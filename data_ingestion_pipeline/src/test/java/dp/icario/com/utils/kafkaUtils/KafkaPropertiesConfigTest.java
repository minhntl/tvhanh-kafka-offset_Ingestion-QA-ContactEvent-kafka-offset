package dp.icario.com.utils.kafkaUtils;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class KafkaPropertiesConfigTest {

  @Test
  void testGetKafkaConfigScram() {
    String mockUserName = "mockUserName";
    String mockPasword = "mockPassword";

    Properties properties = KafkaPropertiesConfig.getKafkaConfigScram(mockUserName, mockPasword);

    assertEquals(4, properties.size());
    assertEquals("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"mockUserName\" password=\"mockPassword\";", properties.getProperty("sasl.jaas.config"));
    assertEquals("900000", properties.getProperty("transaction.timeout.ms"));
    assertEquals("SCRAM-SHA-512", properties.getProperty("sasl.mechanism"));
    assertEquals("SASL_SSL", properties.getProperty("security.protocol"));
  }

  @Test
  void testGetKafkaConfigLocal() {
    Properties properties = KafkaPropertiesConfig.getKafkaConfigLocal();

    assertEquals(1, properties.size());
    assertEquals("900000", properties.getProperty("transaction.timeout.ms"));
  }
}
