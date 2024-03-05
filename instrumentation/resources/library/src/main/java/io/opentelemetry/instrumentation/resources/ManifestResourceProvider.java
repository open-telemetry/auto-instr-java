/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.resources;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.Optional;
import java.util.jar.Manifest;

/**
 * A {@link ResourceProvider} that will attempt to detect the <code>service.name</code> and <code>
 * service.version</code> from META-INF/MANIFEST.MF.
 */
@AutoService(ResourceProvider.class)
public final class ManifestResourceProvider extends AttributeResourceProvider<Manifest> {

  @SuppressWarnings("unused") // SPI
  public ManifestResourceProvider() {
    this(new JarPathFinder());
  }

  // Visible for testing
  ManifestResourceProvider(JarPathFinder jarPathFinder) {
    super(
        new AttributeProvider<Manifest>() {
          @Override
          public Optional<Manifest> readData() {
            return jarPathFinder.getManifestFromJarFile();
          }

          @Override
          public void registerAttributes(Builder<Manifest> builder) {
            builder
                .add(
                    ResourceAttributes.SERVICE_NAME,
                    manifest -> {
                      String serviceName =
                          manifest.getMainAttributes().getValue("Implementation-Title");
                      return Optional.ofNullable(serviceName);
                    })
                .add(
                    ResourceAttributes.SERVICE_VERSION,
                    manifest -> {
                      String serviceVersion =
                          manifest.getMainAttributes().getValue("Implementation-Version");
                      return Optional.ofNullable(serviceVersion);
                    });
          }
        });
  }

  @Override
  public int order() {
    // make it run later than SpringBootServiceNameDetector
    return 300;
  }
}
