package com.loobpack.data.kafka.connect.healthcheck.extension.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorHealth;
import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorStatus;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.kafka.connect.health.ConnectClusterState;
import org.apache.kafka.connect.health.TaskState;

public class HealthCheckService {

  private static final String RUNNING_STATE = "RUNNING";

  private final ConnectClusterState clusterState;

  public HealthCheckService(ConnectClusterState clusterState) {
    this.clusterState = clusterState;
  }

  public List<ConnectorStatus> checkStatus() {
    return clusterState.connectors().stream()
        .map(
            name -> ConnectorStatus.builder().name(name).status(checkConnectorHealth(name)).build())
        .collect(toList());
  }

  private ConnectorHealth checkConnectorHealth(String connectorName) {
    org.apache.kafka.connect.health.ConnectorHealth connectorHealth =
        clusterState.connectorHealth(connectorName);
    if (isNotRunningState(connectorHealth.connectorState().state())) {
      return ConnectorHealth.unhealthy(
          format(
              "Connector is not running, state is %s", connectorHealth.connectorState().state()));
    }
    String message =
        connectorHealth.tasksState().entrySet().stream()
            .filter(this::isTaskNotRunning)
            .map(
                e ->
                    format(
                        "Task %d is not running, current state is %s",
                        e.getKey(), e.getValue().state()))
            .collect(Collectors.joining(", "));
    if (!message.isEmpty()) {
      return ConnectorHealth.unhealthy(message);
    }

    return ConnectorHealth.healthy();
  }

  private boolean isNotRunningState(String stateName) {
    return !RUNNING_STATE.equalsIgnoreCase(stateName);
  }

  private boolean isTaskNotRunning(Entry<Integer, TaskState> entry) {
    return isNotRunningState(entry.getValue().state());
  }
}
