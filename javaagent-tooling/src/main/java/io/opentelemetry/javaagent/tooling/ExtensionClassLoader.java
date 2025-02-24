/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;

/**
 * This class creates a class loader which encapsulates arbitrary extensions for Otel Java
 * instrumentation agent. Such extensions may include SDK components (exporters or propagators) and
 * additional instrumentations. They have to be isolated and shaded to reduce interference with the
 * user application and to make it compatible with shaded SDK used by the agent. Thus each extension
 * jar gets a separate class loader and all of them are aggregated with the help of {@link
 * MultipleParentClassLoader}.
 */
// TODO find a way to initialize logging before using this class
@SuppressWarnings("SystemOut")
public class ExtensionClassLoader extends URLClassLoader {
  public static final String EXTENSIONS_CONFIG = "otel.javaagent.extensions";

  private final boolean isSecurityManagerSupportEnabled;

  // this class is used early, and must not use logging in most of its methods
  // in case the extension config can not be found, we save the warning
  // message and log it later, when the logging subsystem is initialized
  @Nullable private static String extensionClassLoadWarningMessage;

  // NOTE it's important not to use logging in this class, because this class is used before logging
  // is initialized

  static {
    ClassLoader.registerAsParallelCapable();
  }

  public static ClassLoader getInstance(
      ClassLoader parent,
      File javaagentFile,
      boolean isSecurityManagerSupportEnabled,
      EarlyInitAgentConfig earlyConfig) {
    List<URL> extensions = new ArrayList<>();

    includeEmbeddedExtensionsIfFound(extensions, javaagentFile);

    String externalExtensionsLocation = earlyConfig.getString(EXTENSIONS_CONFIG);
    if (externalExtensionsLocation != null && !externalExtensionsLocation.isEmpty()) {
      List<URL> externalExtensions = parseLocation(externalExtensionsLocation, javaagentFile);
      if (externalExtensions.isEmpty()) {
        extensionClassLoadWarningMessage =
            "No external extensions found for configured location \""
                + externalExtensionsLocation
                + "\"";
      } else {
        extensions.addAll(externalExtensions);
      }
    }

    // TODO when logging is configured add warning about deprecated property
    if (extensions.isEmpty()) {
      return parent;
    }

    List<ClassLoader> delegates = new ArrayList<>(extensions.size());
    for (URL url : extensions) {
      delegates.add(getDelegate(parent, url, isSecurityManagerSupportEnabled));
    }
    return new MultipleParentClassLoader(parent, delegates);
  }

  static void logWarningIfAny() {
    if (extensionClassLoadWarningMessage != null) {
      Logger.getLogger(ExtensionClassLoader.class.getName())
          .log(WARNING, extensionClassLoadWarningMessage);
    }
  }

  private static void includeEmbeddedExtensionsIfFound(List<URL> extensions, File javaagentFile) {
    try (JarFile jarFile = new JarFile(javaagentFile, false)) {
      Enumeration<JarEntry> entryEnumeration = jarFile.entries();
      String prefix = "extensions/";
      File tempDirectory = null;
      while (entryEnumeration.hasMoreElements()) {
        JarEntry jarEntry = entryEnumeration.nextElement();
        String name = jarEntry.getName();

        if (name.startsWith(prefix) && !jarEntry.isDirectory()) {
          tempDirectory = ensureTempDirectoryExists(tempDirectory);

          File tempFile = new File(tempDirectory, name.substring(prefix.length()));
          // reject extensions that would be extracted outside of temp directory
          // https://security.snyk.io/research/zip-slip-vulnerability
          if (!tempFile
              .getCanonicalFile()
              .toPath()
              .startsWith(tempDirectory.getCanonicalFile().toPath())) {
            throw new IllegalStateException("Invalid extension " + name);
          }
          if (tempFile.createNewFile()) {
            tempFile.deleteOnExit();
            extractFile(jarFile, jarEntry, tempFile);
            addFileUrl(extensions, tempFile);
          } else {
            System.err.println("Failed to create temp file " + tempFile);
          }
        }
      }
    } catch (IOException ex) {
      System.err.println("Failed to open embedded extensions " + ex.getMessage());
    }
  }

  private static File ensureTempDirectoryExists(File tempDirectory) throws IOException {
    if (tempDirectory == null) {
      tempDirectory = Files.createTempDirectory("otel-extensions").toFile();
      tempDirectory.deleteOnExit();
    }
    return tempDirectory;
  }

  private static URLClassLoader getDelegate(
      ClassLoader parent, URL extensionUrl, boolean isSecurityManagerSupportEnabled) {
    return new ExtensionClassLoader(extensionUrl, parent, isSecurityManagerSupportEnabled);
  }

  // visible for testing
  static List<URL> parseLocation(@Nullable String locationName, File javaagentFile) {
    if (locationName == null) {
      return Collections.emptyList();
    }

    List<URL> result = new ArrayList<>();
    for (String location : locationName.split(",")) {
      parseLocation(location, javaagentFile, result);
    }

    return result;
  }

  private static void parseLocation(String locationName, File javaagentFile, List<URL> locations) {
    if (locationName.isEmpty()) {
      return;
    }

    File location = new File(locationName);
    if (isJar(location)) {
      addFileUrl(locations, location);
    } else if (location.isDirectory()) {
      File[] files = location.listFiles(ExtensionClassLoader::isJar);
      if (files != null) {
        for (File file : files) {
          if (isJar(file) && !file.getAbsolutePath().equals(javaagentFile.getAbsolutePath())) {
            addFileUrl(locations, file);
          }
        }
      }
    }
  }

  private static boolean isJar(File f) {
    return f.isFile() && f.getName().endsWith(".jar");
  }

  private static void addFileUrl(List<URL> result, File file) {
    try {
      // skip shading extension classes if opentelemetry-api is not shaded (happens when using
      // disableShadowRelocate=true)
      if (Context.class.getName().contains(".shaded.")) {
        URL wrappedUrl = new URL("otel", null, -1, "/", new RemappingUrlStreamHandler(file));
        result.add(wrappedUrl);
      } else {
        result.add(file.toURI().toURL());
      }
    } catch (MalformedURLException ignored) {
      System.err.println("Ignoring " + file);
    }
  }

  private static void extractFile(JarFile jarFile, JarEntry jarEntry, File outputFile)
      throws IOException {
    try (InputStream in = jarFile.getInputStream(jarEntry);
        ReadableByteChannel rbc = Channels.newChannel(in);
        FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
  }

  @Override
  protected PermissionCollection getPermissions(CodeSource codesource) {
    if (isSecurityManagerSupportEnabled) {
      Permissions permissions = new Permissions();
      permissions.add(new AllPermission());
      return permissions;
    }
    return super.getPermissions(codesource);
  }

  private ExtensionClassLoader(
      URL url, ClassLoader parent, boolean isSecurityManagerSupportEnabled) {
    super(new URL[] {url}, parent);
    this.isSecurityManagerSupportEnabled = isSecurityManagerSupportEnabled;
  }
}
