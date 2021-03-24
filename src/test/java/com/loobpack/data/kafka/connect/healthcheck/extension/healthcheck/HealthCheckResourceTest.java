package com.loobpack.data.kafka.connect.healthcheck.extension.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorHealth;
import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorStatus;
import com.loobpack.data.kafka.connect.healthcheck.extension.service.HealthCheckService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

public class HealthCheckResourceTest extends JerseyTest {

  private HealthCheckService healthCheckService;

  @Override
  protected Application configure() {
    healthCheckService = mock(HealthCheckService.class);
    HealthCheckResource underTest = new HealthCheckResource(healthCheckService);
    return new ResourceConfig().register(underTest);
  }

  @Test
  public void shouldReportHealthyWhenThereAreNoConnectorsDeployed() {
    when(healthCheckService.checkStatus()).thenReturn(Collections.emptyList());

    Response response = target("/healthcheck").request().get();

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    HashMap<String, Object> entity =
        response.readEntity(new GenericType<HashMap<String, Object>>() {});
    assertThat(entity).isEmpty();
  }

  @Test
  public void shouldReportHealthyWhenAllConnectorsAreHealthy() {
    when(healthCheckService.checkStatus())
        .thenReturn(
            Arrays.asList(
                ConnectorStatus.builder().name("a").status(ConnectorHealth.healthy()).build(),
                ConnectorStatus.builder().name("b").status(ConnectorHealth.healthy()).build(),
                ConnectorStatus.builder().name("c").status(ConnectorHealth.healthy()).build()));

    Response response = target("/healthcheck").request().get();

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    HashMap<String, Object> entity =
        response.readEntity(new GenericType<HashMap<String, Object>>() {});
    assertThat(entity)
        .hasSize(3)
        .containsEntry("a", healthyMap())
        .containsEntry("b", healthyMap())
        .containsEntry("c", healthyMap());
  }

  @Test
  public void shouldReportUnhealthyWhenAtLeastOneConnectorIsNotHealthy() {
    when(healthCheckService.checkStatus())
        .thenReturn(
            Arrays.asList(
                ConnectorStatus.builder().name("a").status(ConnectorHealth.healthy()).build(),
                ConnectorStatus.builder()
                    .name("b")
                    .status(ConnectorHealth.unhealthy("Ill"))
                    .build(),
                ConnectorStatus.builder().name("c").status(ConnectorHealth.healthy()).build()));

    Response response = target("/healthcheck").request().get();

    assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    HashMap<String, Object> entity =
        response.readEntity(new GenericType<HashMap<String, Object>>() {});
    assertThat(entity)
        .hasSize(3)
        .containsEntry("a", healthyMap())
        .containsEntry("b", unhealthyMap("Ill"))
        .containsEntry("c", healthyMap());
  }

  private HashMap<String, Object> healthyMap() {
    return new HashMap<String, Object>() {
      {
        put("healthy", true);
      }
    };
  }

  private HashMap<String, Object> unhealthyMap(String message) {
    return new HashMap<String, Object>() {
      {
        put("healthy", false);
        put("message", message);
      }
    };
  }
}
