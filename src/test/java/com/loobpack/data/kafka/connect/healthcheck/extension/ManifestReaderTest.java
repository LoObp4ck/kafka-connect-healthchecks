package com.loobpack.data.kafka.connect.healthcheck.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ManifestReaderTest {

  private ManifestReader underTest = new ManifestReader();

  private static Stream<Arguments> attributesAndValues() {
    return Stream.of(
        Arguments.of(ManifestReader.LOOBPACK_MODULE_ATTRIBUTE, "kafka-connect-healthcheck-extension"),
        Arguments.of(ManifestReader.LOOBPACK_VERSION_ATTRIBUTE, "0.0.1"));
  }

  @ParameterizedTest
  @MethodSource("attributesAndValues")
  void shouldReturnModuleNameFromManifest(String name, String expectedValue) {
    String value = underTest.getAttribute(name);

    assertThat(value).isEqualTo(expectedValue);
  }

  @Test
  void shouldReturnNullWhenAttributeNotFoundInManifest() {
    String value = underTest.getAttribute("SOME-UNKNOWN-ATTRIBUTE");

    assertThat(value).isNull();
  }
}
