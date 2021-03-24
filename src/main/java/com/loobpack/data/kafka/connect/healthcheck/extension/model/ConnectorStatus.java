package com.loobpack.data.kafka.connect.healthcheck.extension.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConnectorStatus {

  private final String name;
  private final ConnectorHealth status;
}
