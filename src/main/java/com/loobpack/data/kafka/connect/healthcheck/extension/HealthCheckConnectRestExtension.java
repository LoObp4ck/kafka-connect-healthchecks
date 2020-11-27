package com.loobpack.data.kafka.connect.healthcheck.extension;

import com.loobpack.data.kafka.connect.healthcheck.extension.healthcheck.HealthCheckResource;
import com.loobpack.data.kafka.connect.healthcheck.extension.service.HealthCheckService;
import java.util.Map;
import org.apache.kafka.connect.rest.ConnectRestExtension;
import org.apache.kafka.connect.rest.ConnectRestExtensionContext;

public class HealthCheckConnectRestExtension implements ConnectRestExtension {

  private String version;
  private final ManifestReader manifestReader = new ManifestReader();

  @Override
  public void register(ConnectRestExtensionContext restPluginContext) {
    restPluginContext
        .configurable()
        .register(
            new HealthCheckResource(new HealthCheckService(restPluginContext.clusterState())));
  }

  @Override
  public void close() {
    // Nothing to do here
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // Nothing to do here
  }

  @Override
  public String version() {
    if (version == null) {
      Object v = manifestReader.getAttribute(ManifestReader.LOOBPACK_VERSION_ATTRIBUTE);
      version = v == null ? "unknown" : v.toString();
    }
    return version;
  }
}
