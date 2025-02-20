/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.annotation.Nullable;

/**
 * Classloader used to run the core agent.
 *
 * <p>It is built around the concept of a jar inside another jar. This class loader loads the files
 * of the internal jar to load classes and resources.
 */
public class AgentClassLoader extends URLClassLoader {

  // NOTE it's important not to use logging in this class, because this class is used before logging
  // is initialized

  static {
    ClassLoader.registerAsParallelCapable();
  }

  private static final String AGENT_INITIALIZER_JAR =
      System.getProperty("otel.javaagent.experimental.initializer.jar", "");

  private static final String META_INF = "META-INF/";
  private static final String META_INF_VERSIONS = META_INF + "versions/";

  // multi release jars were added in java 9
  private static final int MIN_MULTI_RELEASE_JAR_JAVA_VERSION = 9;
  // current java version
  private static final int JAVA_VERSION = getJavaVersion();
  private static final boolean MULTI_RELEASE_JAR_ENABLE =
      JAVA_VERSION >= MIN_MULTI_RELEASE_JAR_JAVA_VERSION;

  // Calling java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch
  // adds a jar to the bootstrap class lookup, but not to the resource lookup.
  // As a workaround, we keep a reference to the bootstrap jar
  // to use only for resource lookups.
  private final BootstrapClassLoaderProxy bootstrapProxy;

  private final JarFile jarFile;
  private final URL jarBase;
  private final String jarEntryPrefix;
  private final CodeSource codeSource;
  private final boolean isSecurityManagerSupportEnabled;
  private final Manifest manifest;

  // Used by tests
  public AgentClassLoader(File javaagentFile) {
    this(javaagentFile, "", false);
  }

  /**
   * Construct a new AgentClassLoader.
   *
   * @param javaagentFile Used for resource lookups.
   * @param internalJarFileName File name of the internal jar
   * @param isSecurityManagerSupportEnabled Whether this class loader should define classes with all
   *     permissions
   */
  public AgentClassLoader(
      File javaagentFile, String internalJarFileName, boolean isSecurityManagerSupportEnabled) {
    super(new URL[] {}, getParentClassLoader());
    if (javaagentFile == null) {
      throw new IllegalArgumentException("Agent jar location should be set");
    }
    if (internalJarFileName == null) {
      throw new IllegalArgumentException("Internal jar file name should be set");
    }

    this.isSecurityManagerSupportEnabled = isSecurityManagerSupportEnabled;
    bootstrapProxy = new BootstrapClassLoaderProxy(this);

    jarEntryPrefix =
        internalJarFileName
            + (internalJarFileName.isEmpty() || internalJarFileName.endsWith("/") ? "" : "/");
    try {
      jarFile = new JarFile(javaagentFile, false);
      // base url for constructing jar entry urls
      // we use a custom protocol instead of typical jar:file: because we don't want to be affected
      // by user code disabling URLConnection caching for jar protocol e.g. tomcat does this
      jarBase =
          new URL("x-internal-jar", null, 0, "/", new AgentClassLoaderUrlStreamHandler(jarFile));
      codeSource = new CodeSource(javaagentFile.toURI().toURL(), (Certificate[]) null);
      manifest = jarFile.getManifest();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to open agent jar", e);
    }

    if (!AGENT_INITIALIZER_JAR.isEmpty()) {
      URL url;
      try {
        url = new File(AGENT_INITIALIZER_JAR).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new IllegalStateException(
            "Filename could not be parsed: "
                + AGENT_INITIALIZER_JAR
                + ". Initializer is not installed",
            e);
      }

      addURL(url);
    }
  }

  private static ClassLoader getParentClassLoader() {
    if (JAVA_VERSION > 8) {
      return new PlatformDelegatingClassLoader();
    }
    return null;
  }

  private static int getJavaVersion() {
    String javaSpecVersion = System.getProperty("java.specification.version");
    if ("1.8".equals(javaSpecVersion)) {
      return 8;
    }
    return Integer.parseInt(javaSpecVersion);
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // ContextStorageOverride is meant for library instrumentation we don't want it to apply to our
    // bundled grpc
    if ("io.grpc.override.ContextStorageOverride".equals(name)) {
      throw new ClassNotFoundException(name);
    }

    synchronized (getClassLoadingLock(name)) {
      Class<?> clazz = findLoadedClass(name);
      // first search agent classes
      if (clazz == null) {
        clazz = findAgentClass(name);
      }
      // search from parent and urls added to this loader
      if (clazz == null) {
        clazz = super.loadClass(name, false);
      }
      if (resolve) {
        resolveClass(clazz);
      }

      return clazz;
    }
  }

  private Class<?> findAgentClass(String name) throws ClassNotFoundException {
    AgentJarResource jarResource = findAgentJarResource(name.replace('.', '/') + ".class");
    if (jarResource != null) {
      byte[] bytes;
      try {
        bytes = getJarEntryBytes(jarResource.getJarEntry());
      } catch (IOException exception) {
        throw new ClassNotFoundException(name, exception);
      }

      definePackageIfNeeded(name);
      return defineClass(name, bytes);
    }

    return null;
  }

  public Class<?> defineClass(String name, byte[] bytes) {
    return defineClass(name, bytes, 0, bytes.length, codeSource);
  }

  @Override
  protected PermissionCollection getPermissions(CodeSource codeSource) {
    if (isSecurityManagerSupportEnabled) {
      Permissions permissions = new Permissions();
      permissions.add(new AllPermission());
      return permissions;
    }

    return super.getPermissions(codeSource);
  }

  private byte[] getJarEntryBytes(JarEntry jarEntry) throws IOException {
    int size = (int) jarEntry.getSize();
    byte[] buffer = new byte[size];
    try (InputStream is = jarFile.getInputStream(jarEntry)) {
      int offset = 0;
      int read;

      while (offset < size && (read = is.read(buffer, offset, size - offset)) != -1) {
        offset += read;
      }
    }

    return buffer;
  }

  private void definePackageIfNeeded(String className) {
    String packageName = getPackageName(className);
    if (packageName == null) {
      return;
    }
    if (getPackage(packageName) == null) {
      try {
        definePackage(packageName, manifest, codeSource.getLocation());
      } catch (IllegalArgumentException exception) {
        if (getPackage(packageName) == null) {
          throw new IllegalStateException("Failed to define package", exception);
        }
      }
    }
  }

  private static String getPackageName(String className) {
    int index = className.lastIndexOf('.');
    return index == -1 ? null : className.substring(0, index);
  }

  private AgentJarResource findAgentJarResource(String name) {
    // shading renames .class to .classdata
    boolean isClass = name.endsWith(".class");
    if (isClass) {
      name += getClassSuffix();
    }

    String jarEntryName = jarEntryPrefix + name;
    JarEntry jarEntry = jarFile.getJarEntry(jarEntryName);
    AgentJarResource jarResource = AgentJarResource.create(jarEntryName, jarEntry);
    if (MULTI_RELEASE_JAR_ENABLE) {
      jarResource = findVersionedAgentJarResource(jarResource, name);
    }
    return jarResource;
  }

  // suffix appended to class resource names
  // this is in a protected method so that unit tests could override it
  protected String getClassSuffix() {
    return "data";
  }

  private AgentJarResource findVersionedAgentJarResource(
      AgentJarResource jarResource, String name) {
    // same logic as in JarFile.getVersionedEntry
    if (!name.startsWith(META_INF)) {
      // search for versioned entry by looping over possible versions form high to low
      int version = JAVA_VERSION;
      while (version >= MIN_MULTI_RELEASE_JAR_JAVA_VERSION) {
        String versionedJarEntryName = jarEntryPrefix + META_INF_VERSIONS + version + "/" + name;
        JarEntry versionedJarEntry = jarFile.getJarEntry(versionedJarEntryName);
        if (versionedJarEntry != null) {
          return AgentJarResource.create(versionedJarEntryName, versionedJarEntry);
        }
        version--;
      }
    }

    return jarResource;
  }

  @Override
  public URL getResource(String resourceName) {
    URL bootstrapResource = bootstrapProxy.getResource(resourceName);
    if (null == bootstrapResource) {
      return super.getResource(resourceName);
    } else {
      return bootstrapResource;
    }
  }

  @Override
  public URL findResource(String name) {
    URL url = findJarResource(name);
    if (url != null) {
      return url;
    }

    // find resource from agent initializer jar
    return super.findResource(name);
  }

  private URL findJarResource(String name) {
    AgentJarResource jarResource = findAgentJarResource(name);
    return getAgentJarResourceUrl(jarResource);
  }

  private URL getAgentJarResourceUrl(AgentJarResource jarResource) {
    if (jarResource != null) {
      try {
        return new URL(jarBase, jarResource.getName());
      } catch (MalformedURLException e) {
        throw new IllegalStateException(
            "Failed to construct url for jar entry " + jarResource.getName(), e);
      }
    }

    return null;
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    // find resources from agent initializer jar
    Enumeration<URL> delegate = super.findResources(name);
    // agent jar can have only one resource for given name
    URL url = findJarResource(name);
    if (url != null) {
      return new Enumeration<URL>() {
        boolean first = true;

        @Override
        public boolean hasMoreElements() {
          return first || delegate.hasMoreElements();
        }

        @Override
        public URL nextElement() {
          if (first) {
            first = false;
            return url;
          }
          return delegate.nextElement();
        }
      };
    }

    return delegate;
  }

  public BootstrapClassLoaderProxy getBootstrapProxy() {
    return bootstrapProxy;
  }

  /**
   * A stand-in for the bootstrap class loader. Used to look up bootstrap resources and resources
   * appended by instrumentation.
   *
   * <p>This class is thread safe.
   */
  public static final class BootstrapClassLoaderProxy extends ClassLoader {
    private final AgentClassLoader agentClassLoader;

    static {
      ClassLoader.registerAsParallelCapable();
    }

    public BootstrapClassLoaderProxy(AgentClassLoader agentClassLoader) {
      super(null);
      this.agentClassLoader = agentClassLoader;
    }

    @Override
    public URL getResource(String resourceName) {
      // find resource from boot loader
      URL url = super.getResource(resourceName);
      if (url != null) {
        return url;
      }
      // find from agent jar
      if (agentClassLoader != null) {
        JarEntry jarEntry = agentClassLoader.jarFile.getJarEntry(resourceName);
        AgentJarResource jarResource = AgentJarResource.create(resourceName, jarEntry);
        return agentClassLoader.getAgentJarResourceUrl(jarResource);
      }
      return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      throw new ClassNotFoundException(name);
    }
  }

  private static class AgentJarResource {
    private final String name;
    private final JarEntry jarEntry;

    private AgentJarResource(String name, JarEntry jarEntry) {
      this.name = name;
      this.jarEntry = jarEntry;
    }

    String getName() {
      return name;
    }

    JarEntry getJarEntry() {
      return jarEntry;
    }

    static AgentJarResource create(String name, JarEntry jarEntry) {
      return jarEntry != null ? new AgentJarResource(name, jarEntry) : null;
    }
  }

  private static class AgentClassLoaderUrlStreamHandler extends URLStreamHandler {
    private final JarFile jarFile;

    AgentClassLoaderUrlStreamHandler(JarFile jarFile) {
      this.jarFile = jarFile;
    }

    @Override
    protected URLConnection openConnection(URL url) {
      return new AgentClassLoaderUrlConnection(url, jarFile);
    }
  }

  private static class AgentClassLoaderUrlConnection extends URLConnection {
    private final JarFile jarFile;
    @Nullable private final String entryName;
    @Nullable private JarEntry jarEntry;

    AgentClassLoaderUrlConnection(URL url, JarFile jarFile) {
      super(url);
      this.jarFile = jarFile;
      String path = url.getFile();
      if (path.startsWith("/")) {
        path = path.substring(1);
      }
      if (path.isEmpty()) {
        path = null;
      }
      this.entryName = path;
    }

    @Override
    public void connect() throws IOException {
      if (!connected) {
        if (entryName != null) {
          jarEntry = jarFile.getJarEntry(entryName);
          if (jarEntry == null) {
            throw new FileNotFoundException(
                "JAR entry " + entryName + " not found in " + jarFile.getName());
          }
        }
        connected = true;
      }
    }

    @Override
    public InputStream getInputStream() throws IOException {
      connect();

      if (entryName == null) {
        throw new IOException("no entry name specified");
      } else {
        if (jarEntry == null) {
          throw new FileNotFoundException(
              "JAR entry " + entryName + " not found in " + jarFile.getName());
        }
        return jarFile.getInputStream(jarEntry);
      }
    }

    @Override
    public Permission getPermission() {
      return null;
    }

    @Override
    public long getContentLengthLong() {
      try {
        connect();

        if (jarEntry != null) {
          return jarEntry.getSize();
        }
      } catch (IOException ignored) {
        // Ignore
      }
      return -1;
    }
  }

  // We don't always delegate to platform loader because platform class loader also contains user
  // classes when running a modular application. We don't want these classes interfering with the
  // agent.
  private static class PlatformDelegatingClassLoader extends ClassLoader {

    static {
      // this class loader doesn't load any classes, so this is technically unnecessary,
      // but included for safety, just in case we every change Class.forName() below back to
      // super.loadClass()
      registerAsParallelCapable();
    }

    private final ClassLoader platformClassLoader = getPlatformLoader();

    public PlatformDelegatingClassLoader() {
      super(null);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      // prometheus exporter uses jdk http server, load it from the platform class loader
      // some custom extensions use java.* classes which are not in the boot loader such as
      // java.sql.* and java.net.http.*
      if (name != null
          && (name.startsWith("com.sun.net.httpserver.") || name.startsWith("java."))) {
        return platformClassLoader.loadClass(name);
      }
      return Class.forName(name, false, null);
    }

    private static ClassLoader getPlatformLoader() {
      /*
       Must invoke ClassLoader.getPlatformClassLoader by reflection to remain
       compatible with java 8.
      */
      try {
        Method method = ClassLoader.class.getDeclaredMethod("getPlatformClassLoader");
        return (ClassLoader) method.invoke(null);
      } catch (InvocationTargetException
          | NoSuchMethodException
          | IllegalAccessException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }
}
