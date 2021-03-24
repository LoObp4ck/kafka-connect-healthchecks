package com.loobpack.data.kafka.connect.healthcheck.extension.healthcheck;

import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorHealth;
import com.loobpack.data.kafka.connect.healthcheck.extension.model.ConnectorStatus;
import com.loobpack.data.kafka.connect.healthcheck.extension.service.HealthCheckService;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/healthcheck")
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckResource {

  private final HealthCheckService healthCheckService;

  public HealthCheckResource(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  @GET
  public Response healthcheck() {
    List<ConnectorStatus> status = healthCheckService.checkStatus();

    Status responseCode =
        status.stream()
            .filter(s -> !s.getStatus().isHealthy())
            .findAny()
            .map(s -> Status.INTERNAL_SERVER_ERROR)
            .orElse(Status.OK);

    Map<String, ConnectorHealth> health =
        status.stream()
            .map(s -> new SimpleEntry<>(s.getName(), s.getStatus()))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return Response.status(responseCode).entity(health).build();
  }
}
