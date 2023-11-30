package dp.icario.com.utils;

import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

public class SchemaRegistryUtils {
  public static Map<String, Object> getSchemaRegistryConfig(JsonNode jsonNode, String tableNameValue) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AWSSchemaRegistryConstants.AWS_REGION, jsonNode.get("SchemaRegistry").get("AWS_REGION").asText());
    configs.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());
    configs.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, true);
    configs.put(AWSSchemaRegistryConstants.REGISTRY_NAME, jsonNode.get("SchemaRegistry").get("REGISTRY_NAME").asText());
    configs.put(AWSSchemaRegistryConstants.SCHEMA_NAME, tableNameValue + "_schema");
    return configs;
  }
}
