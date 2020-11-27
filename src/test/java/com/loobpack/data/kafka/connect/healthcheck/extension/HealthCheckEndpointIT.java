package com.loobpack.data.kafka.connect.healthcheck.extension;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.command.CreateContainerCmd;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.UUID;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.okhttp3.MediaType;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.RequestBody;
import org.testcontainers.shaded.okhttp3.Response;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;

@Testcontainers
class HealthCheckEndpointIT {
  private static final Logger logger = LoggerFactory.getLogger(HealthCheckEndpointIT.class);

  private static final String CONFLUENT_VERSION = "5.5.1";
  private static final String PLUGIN_PATH = "/usr/share/java";
  private static final String ARCHIVE_NAME = "kafka-connect-healthcheck-extension";
  private static final String ARCHIVE_NAME_WITH_EXT = ARCHIVE_NAME + ".jar";
  private static final File WORKING_DIR = new File(".");

  private static final UUID TEST_ID = UUID.randomUUID();
  private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";

  private static String baseUrl;
  private static String connectorUrl;
  private static Network network = Network.builder().id(TEST_ID.toString()).build();

  @Container
  private static GenericContainer zookeeper =
      new GenericContainer(appendVersionToContainerName("confluentinc/cp-zookeeper"))
          .withNetwork(network)
          .withCreateContainerCmdModifier(
              cmd ->
                  CreateContainerCmd.class
                      .cast(cmd)
                      .withHostName(zookeeperHost())
                      .withName(zookeeperHost()))
          .withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
          .withEnv("ZOOKEEPER_TICK_TIME", "2000")
          .withExposedPorts(2181);

  @Container
  private static GenericContainer broker =
      new GenericContainer(appendVersionToContainerName("confluentinc/cp-server"))
          .dependsOn(zookeeper)
          .withNetwork(network)
          .withCreateContainerCmdModifier(
              cmd ->
                  CreateContainerCmd.class
                      .cast(cmd)
                      .withHostName(brokerHost())
                      .withName(brokerHost()))
          .withEnv("KAFKA_BROKER_ID", "1")
          .withEnv("KAFKA_ZOOKEEPER_CONNECT", format("%s:2181", zookeeperHost()))
          .withEnv(
              "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
              "PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT")
          .withEnv(
              "KAFKA_ADVERTISED_LISTENERS",
              format("PLAINTEXT://%s:29092,PLAINTEXT_HOST://localhost:9092", brokerHost()))
          .withEnv(
              "KAFKA_METRIC_REPORTERS", "io.confluent.metrics.reporter.ConfluentMetricsReporter")
          .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
          .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
          .withEnv("KAFKA_CONFLUENT_LICENSE_TOPIC_REPLICATION_FACTOR", "1")
          .withEnv("CONFLUENT_METRICS_REPORTER_BOOTSTRAP_SERVERS", format("%s:29092", brokerHost()))
          .withEnv(
              "CONFLUENT_METRICS_REPORTER_ZOOKEEPER_CONNECT", format("%s:2181", zookeeperHost()))
          .withEnv("CONFLUENT_METRICS_REPORTER_TOPIC_REPLICAS", "1")
          .withEnv("CONFLUENT_METRICS_ENABLE", "true")
          .withEnv("CONFLUENT_SUPPORT_CUSTOMER_ID", "anonymous")
          .withExposedPorts(9092);

  @Container
  private static GenericContainer connect =
      new GenericContainer(
              new ImageFromDockerfile()
                  .withFileFromFile(ARCHIVE_NAME_WITH_EXT, getJarFile())
                  .withDockerfileFromBuilder(
                      builder ->
                          builder
                              .from("confluentinc/cp-kafka-connect-base:" + CONFLUENT_VERSION)
                              .copy(ARCHIVE_NAME_WITH_EXT, PLUGIN_PATH)
                              .build()))
          .dependsOn(zookeeper, broker)
          .withNetwork(network)
          .withCreateContainerCmdModifier(
              cmd ->
                  CreateContainerCmd.class
                      .cast(cmd)
                      .withHostName(connectHost())
                      .withName(connectHost()))
          .withEnv("CONNECT_BOOTSTRAP_SERVERS", format("%s:29092", brokerHost()))
          .withEnv("CONNECT_ZOOKEEPER_CONNECT", format("%s:2181", zookeeperHost()))
          .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", connectHost())
          .withEnv("CONNECT_REST_PORT", "8083")
          .withEnv("CONNECT_GROUP_ID", "compose-connect-group")
          .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "connect-configs")
          .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
          .withEnv("CONNECT_OFFSET_FLUSH_INTERVAL_MS", "10000")
          .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "connect-offsets")
          .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
          .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "connect-status")
          .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
          .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
          .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
          .withEnv("CONNECT_PLUGIN_PATH", PLUGIN_PATH)
          .withEnv(
              "CONNECT_REST_EXTENSION_CLASSES", HealthCheckConnectRestExtension.class.getName())
          .withExposedPorts(8083);

  private static String appendVersionToContainerName(String containerName) {
    return format("%s:%s", containerName, CONFLUENT_VERSION);
  }

  private static File getJarFile() {
    File target = new File(WORKING_DIR, "target");
    FileFilter fileFilter = new WildcardFileFilter(format("%s-*.jar", ARCHIVE_NAME));
    File[] jars = target.listFiles(fileFilter);
    assertThat(jars).hasSize(1);
    return jars[0];
  }

  private static String hostnameFor(String name) {
    return format("%s-%s", name, TEST_ID);
  }

  private static String zookeeperHost() {
    return hostnameFor("zookeeper");
  }

  private static String brokerHost() {
    return hostnameFor("broker");
  }

  private static String connectHost() {
    return hostnameFor("connect");
  }

  @NotNull
  private static OkHttpClient newOkHttpClient() {
    return new OkHttpClient.Builder()
        .addInterceptor(
            chain -> {
              Request request = chain.request();
              Response response = chain.proceed(request);

              int tryCount = 1;
              while (!response.isSuccessful() && tryCount <= 3 && response.code() / 100 == 4) {
                response.close();
                try {
                  sleep(SECONDS.toMillis(tryCount));
                } catch (InterruptedException e) {
                  // ignore
                }
                response = chain.proceed(request);
              }

              return response;
            })
        .build();
  }

  private static void waithConnectIsReady() {
    connect
        .waitingFor(Wait.forHealthcheck())
        .waitingFor(Wait.forHttp("/connectors"))
        .waitingFor(Wait.forLogMessage("Finished starting connectors and tasks", 1));
    baseUrl = format("http://localhost:%d", connect.getMappedPort(8083));
    connectorUrl = format("%s/connectors", baseUrl);
  }

  private Closeable registerConnector(OkHttpClient client, String name) throws IOException {
    String connector =
        new JSONObject()
            .put("name", name)
            .put(
                "config",
                new JSONObject()
                    .put("connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector")
                    .put("topics", "source-test-topic")
                    .put("file", format("/tmp/%s.txt", name)))
            .toString();

    Request request =
        new Request.Builder()
            .url(connectorUrl)
            .post(RequestBody.create(MediaType.get(APPLICATION_JSON_CHARSET_UTF_8), connector))
            .build();
    Response response = client.newCall(request).execute();
    assertThat(response.isSuccessful())
        .describedAs(format("Should create sink connector %s", name))
        .isTrue();
    return () -> unregisterConnector(client, name);
  }

  private void unregisterConnector(OkHttpClient client, String name) {
    try {
      String url = format("%s/%s", connectorUrl, name);
      Request request = new Request.Builder().url(url).delete().build();
      Response response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        logger.error("Error deleting connector {}: {}", name, response);
      }
    } catch (Exception e) {
      logger.error("Unable to delete connector {}", name, e);
    }
  }

  @Test
  void shouldReportHealthyConnectorWhenAllConnectorsAreRunning() throws Exception {
    waithConnectIsReady();
    String connectorName = "file-sink-1";

    OkHttpClient client = newOkHttpClient();

    try (Closeable connector = registerConnector(client, connectorName)) {
      Thread.sleep(SECONDS.toMillis(2));
      String healthCheckUrl = format("%s/healthcheck", baseUrl);
      Request request = new Request.Builder().url(healthCheckUrl).get().build();
      Response response = client.newCall(request).execute();

      assertThat(response.isSuccessful()).describedAs("Should get health").isTrue();
      assertThat(response.code()).isEqualTo(HttpStatus.OK_200.getStatusCode());
      JSONObject health = new JSONObject(response.body().string());
      assertThat(health.length()).isEqualTo(1);
      JSONObject connectorHealth = health.getJSONObject(connectorName);
      assertThat(connectorHealth.length()).isEqualTo(1);
      assertThat(connectorHealth.getBoolean("healthy")).isTrue();
    }
  }

  @Test
  void shouldReportUnhealthyConnectorWhenTheConnectorIsNotRunning() throws Exception {
    waithConnectIsReady();
    String connectorName = "file-sink-1";

    OkHttpClient client = newOkHttpClient();

    try (Closeable connector = registerConnector(client, connectorName)) {
      String pauseUrl = format("%s/%s/pause", connectorUrl, connectorName);
      Request request =
          new Request.Builder()
              .url(pauseUrl)
              .put(RequestBody.create(MediaType.get(APPLICATION_JSON_CHARSET_UTF_8), ""))
              .build();
      Response response = client.newCall(request).execute();
      assertThat(response.isSuccessful()).describedAs("Should pause connector").isTrue();

      Thread.sleep(SECONDS.toMillis(2));
      String healthCheckUrl = format("%s/healthcheck", baseUrl);
      request = new Request.Builder().url(healthCheckUrl).get().build();
      response = client.newCall(request).execute();

      assertThat(response.isSuccessful()).describedAs("Should get health").isFalse();
      assertThat(response.code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode());
      JSONObject health = new JSONObject(response.body().string());
      assertThat(health.length()).isEqualTo(1);
      JSONObject connectorHealth = health.getJSONObject(connectorName);
      assertThat(connectorHealth.length()).isEqualTo(2);
      assertThat(connectorHealth.getBoolean("healthy")).isFalse();
      assertThat(connectorHealth.getString("message"))
          .isEqualTo("Connector is not running, state is PAUSED");
    }
  }
}
