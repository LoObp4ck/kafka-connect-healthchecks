package com.loobpack.data.kafka.connect.healthcheck.extension;

import com.jcabi.manifests.Manifests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManifestReader {
  public static final String LOOBPACK_MODULE_ATTRIBUTE = "X-Loobpack-Module";
  public static final String LOOBPACK_VERSION_ATTRIBUTE = "X-Loobpack-Version";
  private static final Logger logger = LoggerFactory.getLogger(ManifestReader.class);

  public String getAttribute(String name) {
    try {
      return Manifests.read(name);
    } catch (Exception e) {
      logger.error("Unable to find attribute {}", name, e);
    }
    return null;
  }
}
