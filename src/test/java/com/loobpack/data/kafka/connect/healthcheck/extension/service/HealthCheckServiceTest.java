package com.loobpack.data.kafka.connect.healthcheck.extension.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorHealth;
import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorStatus;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.health.ConnectClusterState;
import org.apache.kafka.connect.health.ConnectorState;
import org.apache.kafka.connect.health.ConnectorType;
import org.apache.kafka.connect.health.TaskState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

  @Mock private ConnectClusterState clusterState;

  private HealthCheckService underTest;

  @BeforeEach
  void init() {
    underTest = new HealthCheckService(clusterState);
  }

  @Test
  void shouldReturnEmptyListWhenThereAreNoConnectorsConfigured() {
    when(clusterState.connectors()).thenReturn(Collections.emptyList());

    List<ConnectorStatus> status = underTest.checkStatus();

    assertThat(status).isEmpty();
  }

  @Test
  void shouldReportHealthyWhenAllConnectorsAndTasksAreRunning() {
    when(clusterState.connectors()).thenReturn(Arrays.asList("a", "b"));
    when(clusterState.connectorHealth("a"))
        .thenReturn(newConnectorHealth("a", "RUNNING", "RUNNING", "RUNNING"));
    when(clusterState.connectorHealth("b"))
        .thenReturn(newConnectorHealth("b", "RUNNING", "RUNNING", "RUNNING"));

    List<ConnectorStatus> status = underTest.checkStatus();

    assertThat(status)
        .hasSize(2)
        .contains(ConnectorStatus.builder().name("a").status(ConnectorHealth.healthy()).build())
        .contains(ConnectorStatus.builder().name("b").status(ConnectorHealth.healthy()).build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"UNASSIGNED", "PAUSED", "FAILED"})
  void shouldReportUnhealthyWhenAtLeastOneConnectorIsNotRunning(String connectorState) {
    when(clusterState.connectors()).thenReturn(Arrays.asList("a", "b"));
    when(clusterState.connectorHealth("a"))
        .thenReturn(newConnectorHealth("a", connectorState, "ignore"));
    when(clusterState.connectorHealth("b"))
        .thenReturn(newConnectorHealth("b", "RUNNING", "RUNNING"));

    List<ConnectorStatus> status = underTest.checkStatus();

    assertThat(status)
        .hasSize(2)
        .contains(
            ConnectorStatus.builder()
                .name("a")
                .status(
                    ConnectorHealth.unhealthy(
                        "Connector is not running, state is " + connectorState))
                .build())
        .contains(ConnectorStatus.builder().name("b").status(ConnectorHealth.healthy()).build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"UNASSIGNED", "PAUSED", "FAILED"})
  void shouldReportUnhealthyWhenAllConnectorsAreRunningAndAtLeastOneTaskIsNotRunning(
      String taskState) {
    when(clusterState.connectors()).thenReturn(Arrays.asList("a", "b"));
    when(clusterState.connectorHealth("a"))
        .thenReturn(newConnectorHealth("a", "RUNNING", "RUNNING"));
    when(clusterState.connectorHealth("b"))
        .thenReturn(newConnectorHealth("b", "RUNNING", "RUNNING", taskState, "RUNNING"));

    List<ConnectorStatus> status = underTest.checkStatus();

    assertThat(status)
        .hasSize(2)
        .contains(ConnectorStatus.builder().name("a").status(ConnectorHealth.healthy()).build())
        .contains(
            ConnectorStatus.builder()
                .name("b")
                .status(
                    ConnectorHealth.unhealthy(
                        "Task 1 is not running, current state is " + taskState))
                .build());
  }

  private org.apache.kafka.connect.health.ConnectorHealth newConnectorHealth(
      String connectorName, String connectorState, String... taskStates) {
    Map<Integer, TaskState> taskStateMap = new HashMap<>();
    for (int i = 0; i < taskStates.length; ++i) {
      taskStateMap.put(i, new TaskState(i, taskStates[i], "W" + i, "taskTrace" + i));
    }
    return new org.apache.kafka.connect.health.ConnectorHealth(
        connectorName,
        new ConnectorState(connectorState, "W1", "connectorTrace"),
        taskStateMap,
        ConnectorType.UNKNOWN);
  }
}
