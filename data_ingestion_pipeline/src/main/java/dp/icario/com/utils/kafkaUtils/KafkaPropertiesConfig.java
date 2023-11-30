package dp.icario.com.utils.kafkaUtils;

import java.util.Properties;

public class KafkaPropertiesConfig {

  public static Properties getKafkaConfigScram(String username, String password) {
    Properties kafkaProps = new Properties();
    kafkaProps.setProperty("transaction.timeout.ms", "900000");
    kafkaProps.setProperty("sasl.jaas.config", String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";", username, password));
    kafkaProps.setProperty("sasl.mechanism", "SCRAM-SHA-512");
    kafkaProps.setProperty("security.protocol", "SASL_SSL");
    return kafkaProps;
  }

  public static Properties getKafkaConfigLocal() {
    Properties kafkaProps = new Properties();
    kafkaProps.setProperty("transaction.timeout.ms", "900000");
    return kafkaProps;
  }
}
