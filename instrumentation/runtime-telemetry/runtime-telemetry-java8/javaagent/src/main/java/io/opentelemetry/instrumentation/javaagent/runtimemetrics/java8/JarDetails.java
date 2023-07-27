/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.javaagent.runtimemetrics.java8;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * For a given URL representing a Jar directly on the file system or embedded within another
 * archive, this class provides methods which expose useful information about it.
 */
class JarDetails {
  private static final Map<String, String> EMBEDDED_FORMAT_TO_EXTENSION =
      Stream.of("ear", "war", "jar")
          .collect(
              collectingAndThen(
                  toMap(ext -> ('.' + ext + "!/"), identity()),
                  Collections::<String, String>unmodifiableMap));
  private static final ThreadLocal<MessageDigest> SHA1 =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
              throw new IllegalStateException(e);
            }
          });
  private static final ThreadLocal<MessageDigest> SHA512 =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
              throw new IllegalStateException(e);
            }
          });

  private final URL url;
  protected final JarFile jarFile;
  private final Properties pom;
  private final Manifest manifest;

  private JarDetails(URL url, JarFile jarFile) {
    this.url = url;
    this.jarFile = jarFile;
    this.pom = getPom();
    this.manifest = getManifest();
  }

  static JarDetails forUrl(URL url) throws IOException {
    if (url.getProtocol().equals("jar")) {
      String urlString = url.toExternalForm();
      String urlLower = urlString.toLowerCase(Locale.ROOT);
      for (Map.Entry<String, String> entry : EMBEDDED_FORMAT_TO_EXTENSION.entrySet()) {
        int index = urlLower.indexOf(entry.getKey());
        if (index > 0) {
          String targetEntry = urlString.substring(index + entry.getKey().length());
          JarFile jarFile =
              new JarFile(
                  urlString.substring("jar:file:".length(), index + 1 + entry.getValue().length()));
          JarEntry jarEntry = jarFile.getJarEntry(targetEntry);
          return new Embedded(url, jarFile, jarEntry);
        }
      }
    }
    return new JarDetails(url, new JarFile(url.getFile()));
  }

  /** Returns the archive file name, e.g. {@code jackson-datatype-jsr310-2.15.2.jar}. */
  String packagePath() {
    String path = url.getFile();
    int start = path.lastIndexOf(File.separator);
    if (start > -1) {
      return path.substring(start + 1);
    }
    return null;
  }

  /** Returns the extension of the archive, e.g. {@code jar}. */
  String packageType() {
    String path = url.getFile();
    int extensionStart = path.lastIndexOf(".");
    if (extensionStart > -1) {
      return path.substring(extensionStart + 1);
    }
    return null;
  }

  @Nullable
  String packageName() {
    if (pom == null) {
      return null;
    }
    String groupId = pom.getProperty("groupId");
    String artifactId = pom.getProperty("artifactId");
    if (groupId != null && !groupId.isEmpty() && artifactId != null && !artifactId.isEmpty()) {
      return groupId + ":" + artifactId;
    }
    return null;
  }

  @Nullable
  String version() {
    if (pom == null) {
      return null;
    }
    String version = pom.getProperty("version");
    if (version != null && !version.isEmpty()) {
      return version;
    }
    return null;
  }

  /**
   * Returns the package description from the jar manifest "{Implementation-Title} by
   * {Implementation-Vendor}", e.g. {@code Jackson datatype: JSR310 by FasterXML}.
   */
  @Nullable
  String packageDescription() {
    if (manifest == null) {
      return null;
    }

    java.util.jar.Attributes mainAttributes = manifest.getMainAttributes();
    String name = mainAttributes.getValue(java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE);
    String description =
        mainAttributes.getValue(java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR);

    String packageDescription = name;
    if (description != null && !description.isEmpty()) {
      packageDescription += " by " + description;
    }
    return packageDescription;
  }

  @Nullable
  String computeSha1() {
    return computeDigest(SHA1.get());
  }

  @Nullable
  String computeSha512() {
    return computeDigest(SHA512.get());
  }

  private String computeDigest(MessageDigest md) {
    try (InputStream inputStream = getInputStream()) {
      DigestInputStream dis = new DigestInputStream(inputStream, md);
      byte[] buffer = new byte[8192];
      while (dis.read(buffer) != -1) {}
      byte[] digest = md.digest();
      return new BigInteger(1, digest).toString(16);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Open an input stream for the given url. If the url points to a jar within a jar, return an
   * input stream starting at the embedded jar.
   */
  protected InputStream getInputStream() throws IOException {
    return url.openStream();
  }

  @Nullable
  protected Manifest getManifest() {
    try {
      return jarFile.getManifest();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Returns the values from pom.properties if this file is found. If multiple pom.properties files
   * are found or there is an error reading the file, return null.
   */
  @Nullable
  protected Properties getPom() {
    Properties pom = null;
    try {
      for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
        JarEntry jarEntry = entries.nextElement();
        if (jarEntry.getName().startsWith("META-INF/maven")
            && jarEntry.getName().endsWith("pom.properties")) {
          if (pom != null) {
            // we've found multiple pom files. bail!
            return null;
          }
          Properties props = new Properties();
          props.load(jarFile.getInputStream(jarEntry));
          pom = props;
        }
      }
    } catch (IOException e) {
      return null;
    }
    return pom;
  }

  private static class Embedded extends JarDetails {

    private final JarEntry jarEntry;

    private Embedded(URL url, JarFile jarFile, JarEntry jarEntry) {
      super(url, jarFile);
      this.jarEntry = jarEntry;
    }

    /**
     * Open an input stream for the given url. If the url points to a jar within a jar, return an
     * input stream starting at the embedded jar.
     */
    @Override
    protected InputStream getInputStream() throws IOException {
      return jarFile.getInputStream(jarEntry);
    }

    @Override
    protected Manifest getManifest() {
      try (JarInputStream jarFile = new JarInputStream(getInputStream())) {
        return jarFile.getManifest();
      } catch (IOException e) {
        return null;
      }
    }

    /**
     * Returns the values from pom.properties if this file is found. If multiple pom.properties
     * files are found, return null.
     */
    @Override
    @Nullable
    protected Properties getPom() {
      Properties pom = null;
      // Need to navigate inside the embedded jar which can't be done via random access.
      try (JarInputStream jarFile = new JarInputStream(getInputStream())) {
        for (JarEntry entry = jarFile.getNextJarEntry();
            entry != null;
            entry = jarFile.getNextJarEntry()) {
          if (entry.getName().startsWith("META-INF/maven")
              && entry.getName().endsWith("pom.properties")) {
            if (pom != null) {
              // we've found multiple pom files. bail!
              return null;
            }
            Properties props = new Properties();
            props.load(jarFile);
            pom = props;
          }
        }
        return pom;
      } catch (IOException e) {
        return null;
      }
    }
  }
}
