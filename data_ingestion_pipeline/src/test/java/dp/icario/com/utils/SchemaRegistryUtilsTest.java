package dp.icario.com.utils;

import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaRegistryUtilsTest {

  @Test
  void testGetSchemaRegistryConfig() throws JsonProcessingException {
    // Prepare a sample JSONNode
    String json = "{\"SchemaRegistry\": {\"AWS_REGION\": \"us-east-1\", \"REGISTRY_NAME\": \"my-registry\"}}";
    JsonNode jsonNode = null;
    jsonNode = new ObjectMapper().readTree(json);
    // Call the method to test
    String tableNameValue = "myTable";
    Map<String, Object> result = SchemaRegistryUtils.getSchemaRegistryConfig(jsonNode, tableNameValue);
    // Assert the values in the result map
    assertEquals("us-east-1", result.get(AWSSchemaRegistryConstants.AWS_REGION));
    assertEquals("GENERIC_RECORD", result.get(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE));
    assertEquals(true, result.get(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING));
    assertEquals("my-registry", result.get(AWSSchemaRegistryConstants.REGISTRY_NAME));
    assertEquals("myTable_schema", result.get(AWSSchemaRegistryConstants.SCHEMA_NAME));
  }
}
