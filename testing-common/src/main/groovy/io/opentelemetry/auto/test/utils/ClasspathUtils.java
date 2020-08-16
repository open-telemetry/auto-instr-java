/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.auto.test.utils;

import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.base.StandardSystemProperty.PATH_SEPARATOR;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import io.opentelemetry.auto.test.AgentTestRunner;
import io.opentelemetry.javaagent.tooling.Utils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class ClasspathUtils {
  private static final ClassPath testClasspath = computeTestClasspath();

  public static byte[] convertToByteArray(InputStream resource) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int bytesRead;
    byte[] data = new byte[1024];
    while ((bytesRead = resource.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }

  public static byte[] convertToByteArray(Class<?> clazz) throws IOException {
    InputStream inputStream = null;
    try {
      inputStream =
          clazz.getClassLoader().getResourceAsStream(Utils.getResourceName(clazz.getName()));
      return convertToByteArray(inputStream);
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  /**
   * Create a temporary jar on the filesystem with the bytes of the given classes.
   *
   * <p>The jar file will be removed when the jvm exits.
   *
   * @param loader classloader used to load bytes
   * @param resourceNames names of resources to copy into the new jar
   * @return the location of the newly created jar.
   * @throws IOException
   */
  public static URL createJarWithClasses(ClassLoader loader, String... resourceNames)
      throws IOException {
    File tmpJar = File.createTempFile(UUID.randomUUID().toString() + "", ".jar");
    tmpJar.deleteOnExit();

    Manifest manifest = new Manifest();
    JarOutputStream target = new JarOutputStream(new FileOutputStream(tmpJar), manifest);
    for (String resourceName : resourceNames) {
      InputStream is = null;
      try {
        is = loader.getResourceAsStream(resourceName);
        addToJar(resourceName, convertToByteArray(is), target);
      } finally {
        if (null != is) {
          is.close();
        }
      }
    }
    target.close();

    return tmpJar.toURI().toURL();
  }

  /**
   * Create a temporary jar on the filesystem with the bytes of the given classes.
   *
   * <p>The jar file will be removed when the jvm exits.
   *
   * @param classes classes to package into the jar.
   * @return the location of the newly created jar.
   * @throws IOException
   */
  public static URL createJarWithClasses(Class<?>... classes) throws IOException {
    File tmpJar = File.createTempFile(UUID.randomUUID().toString() + "", ".jar");
    tmpJar.deleteOnExit();

    Manifest manifest = new Manifest();
    JarOutputStream target = new JarOutputStream(new FileOutputStream(tmpJar), manifest);
    for (Class<?> clazz : classes) {
      addToJar(Utils.getResourceName(clazz.getName()), convertToByteArray(clazz), target);
    }
    target.close();

    return tmpJar.toURI().toURL();
  }

  public static URL createJarWithClasses() {

    return null;
  }

  private static void addToJar(
      String resourceName, byte[] bytes, JarOutputStream jarOutputStream)
      throws IOException {
    JarEntry entry = new JarEntry(resourceName);
    jarOutputStream.putNextEntry(entry);
    jarOutputStream.write(bytes, 0, bytes.length);
    jarOutputStream.closeEntry();
  }

  public static ClassPath getTestClasspath() {
    return testClasspath;
  }

  private static ClassPath computeTestClasspath() {
    ClassLoader testClassLoader = AgentTestRunner.class.getClassLoader();
    if (!(testClassLoader instanceof URLClassLoader)) {
      // java9's system loader does not extend URLClassLoader
      // which breaks Guava ClassPath lookup
      testClassLoader = buildJavaClassPathClassLoader();
    }
    try {
      return ClassPath.from(testClassLoader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parse JVM classpath and return ClassLoader containing all classpath entries. Inspired by Guava.
   */
  private static ClassLoader buildJavaClassPathClassLoader() {
    ImmutableList.Builder<URL> urls = ImmutableList.builder();
    for (String entry : Splitter.on(PATH_SEPARATOR.value()).split(JAVA_CLASS_PATH.value())) {
      try {
        try {
          urls.add(new File(entry).toURI().toURL());
        } catch (SecurityException e) { // File.toURI checks to see if the file is a directory
          urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
        }
      } catch (MalformedURLException e) {
        System.err.println(
            String.format(
                "Error injecting bootstrap jar: Malformed classpath entry: %s. %s", entry, e));
      }
    }
    return new URLClassLoader(urls.build().toArray(new URL[0]), null);
  }

  // Moved this to a java class because groovy was adding a hard ref to classLoader
  public static boolean isClassLoaded(String className, ClassLoader classLoader) {
    try {
      Method findLoadedClassMethod =
          ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
      try {
        findLoadedClassMethod.setAccessible(true);
        Class<?> loadedClass = (Class<?>) findLoadedClassMethod.invoke(classLoader, className);
        return null != loadedClass && loadedClass.getClassLoader() == classLoader;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      } finally {
        findLoadedClassMethod.setAccessible(false);
      }
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }
}
