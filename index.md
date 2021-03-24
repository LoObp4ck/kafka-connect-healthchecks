# kafka-connect-healthchecks

This is [Kafka Connect Extension](https://cwiki.apache.org/confluence/display/KAFKA/KIP-285%3A+Connect+Rest+Extension+Plugin) that exposes a `/healthcheck` endpoint that can used to retrieve the health status of each connector running in the cluster.

For now, it will check that, for all connectors that are part of your Kafka Connect cluster, all tasks are in RUNNING state ( we'll make that more tunable soon).

## How to install

Copy the JAR file into the `plugin.path` (or `CONNECT_PLUGIN_PATH` if you are using env vars) directory and the set the property `rest.extension.classes` (or `CONNECT_REST_EXTENSION_CLASSES` env var) with the full qulified name of the extension class, e.g. `CONNECT_REST_EXTENSION_CLASSES=com.loobpack.data.kafka.connect.healthcheck.extension.HealthCheckConnectRestExtension`.

Note that, as per the official documentation, you can configure more than one extension if you specify the classes with comma-separated values.

Finally you can also access the JAR from maven central repository

```
<dependency>
  <groupId>net.loobpack.kafka-connect-healthchecks</groupId>
  <artifactId>kafka-connect-healthcheck-extension</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Special thanks

He did not have the time to commit by himself, but most of the initial work here has been made by @ddcprg, thx =)

Thx Vonage/Nexmo (initially intented for internal uses) as well for letting us publish this piece of work
