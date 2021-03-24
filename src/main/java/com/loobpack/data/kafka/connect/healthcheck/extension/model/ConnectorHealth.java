package com.loobpack.data.kafka.connect.healthcheck.extension.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Value;

@Value
@JsonInclude(Include.NON_NULL)
public class ConnectorHealth {

  private final boolean healthy;
  private final String message;

  public static ConnectorHealth healthy() {
    return new ConnectorHealth(true, null);
  }

  public static ConnectorHealth unhealthy(String message) {
    return new ConnectorHealth(false, message);
  }
}
