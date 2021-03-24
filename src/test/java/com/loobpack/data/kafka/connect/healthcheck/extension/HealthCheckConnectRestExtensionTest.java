package com.loobpack.data.kafka.connect.healthcheck.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loobpack.data.kafka.connect.healthcheck.extension.healthcheck.HealthCheckResource;

import javax.ws.rs.core.Configurable;
import org.apache.kafka.connect.health.ConnectClusterState;
import org.apache.kafka.connect.rest.ConnectRestExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthCheckConnectRestExtensionTest {

  @Mock private ConnectRestExtensionContext restPluginContext;
  @Mock private ConnectClusterState clusterState;
  @Mock private Configurable configurable;
  @Captor private ArgumentCaptor<HealthCheckResource> healthCheckResourceCaptor;

  private HealthCheckConnectRestExtension underTest = new HealthCheckConnectRestExtension();

  @Test
  void shouldRegisterHealthCheckResource() {
    when(restPluginContext.configurable()).thenReturn(configurable);

    underTest.register(restPluginContext);

    verify(configurable).register(healthCheckResourceCaptor.capture());
    assertThat(healthCheckResourceCaptor.getValue()).isNotNull().hasNoNullFieldsOrProperties();
    verify(restPluginContext).clusterState();
  }
}
