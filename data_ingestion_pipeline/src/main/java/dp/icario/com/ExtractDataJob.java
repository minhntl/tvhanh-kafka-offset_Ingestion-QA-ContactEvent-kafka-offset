package dp.icario.com;

import com.amazonaws.services.schemaregistry.flink.avro.GlueSchemaRegistryAvroSerializationSchema;

import dp.icario.com.utils.SecretManagerUtils;
import dp.icario.com.utils.Mssql;

import dp.icario.com.utils.SchemaRegistryUtils;
import dp.icario.com.utils.kafkaUtils.KafkaPropertiesConfig;
import dp.icario.com.utils.kafkaUtils.MyCustomRowRowConverter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.avro.RowDataToAvroConverters;
import org.apache.flink.formats.avro.utils.AvroKryoSerializerUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.FieldsDataType;
import org.apache.flink.table.types.logical.*;
import org.apache.flink.types.Row;
import java.io.InputStream;
import java.util.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import static dp.icario.com.utils.kafkaUtils.AvroMapperUtils.mapAvroSchemaToRowDataTypes;
import static dp.icario.com.utils.kafkaUtils.AvroMapperUtils.mapSchemaToRowDataType;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class ExtractDataJob {
  private static final Logger LOG = LoggerFactory.getLogger(ExtractDataJob.class);



  public static void main(String[] args) throws Exception {
    // read config in json file
    String jsonFilePath = "template.json";
    InputStream in = ExtractDataJob.class.getClassLoader().getResourceAsStream(jsonFilePath);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
    JsonNode jsonNode = objectMapper.readTree(in);

    // comment it if test at local
    JSONObject secretJson = SecretManagerUtils.getValue(jsonNode.get("DataSource").get("SecretName").asText(), Region.US_EAST_1);

    //get information for source
    JsonNode dataSourceNode = jsonNode.get("DataSource").get("MSSQL");

    JsonNode kafkaTopicNode = jsonNode.get("KafkaTopic");
    JsonNode bucketName = jsonNode.get("Aws").get("Bucket").get("name");
    JsonNode nativePath = jsonNode.get("Aws").get("Bucket").get("Native").get("path");
    String prefixTopic = kafkaTopicNode.get("TopicPrefix").asText();

    // For running in the Amazon Manage service for Apache Flink
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    //		env.getConfig().enableObjectReuse();
    // this is requirement for custom generic records
    AvroKryoSerializerUtils avroKryoSerializerUtil = new AvroKryoSerializerUtils();
    avroKryoSerializerUtil.addAvroSerializersIfRequired(env.getConfig(), GenericData.Record.class);

    if (dataSourceNode.isArray()) {
      for (JsonNode element : dataSourceNode) {

        String dbName = element.get("DatabaseName").asText();
        String username = secretJson.getString(dbName + "_DB_USERNAME");
        String password = secretJson.getString(dbName + "_DB_PASSWORD");
        LOG.info("username " + username + " and password " + password);
        JsonNode tableList = element.get("TableList");
        for (JsonNode table : tableList) {
          String schemaName = table.get("SchemaName").asText();
          String tableNameValue = table.get("TableName").asText();
          LOG.info("schemaname: " + schemaName + " and tablename: " + tableNameValue);
          String jdbcUrl = secretJson.getString("RC1_DB_URL") + "databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true;";
					// String jdbcUrl = jdbc + "databaseName="+dbName+";encrypt=true;trustServerCertificate=true;";
          String avroSchemaFileName = "raw_" + dbName + "_" + schemaName + "_" + tableNameValue;
          String avscFilePath = String.format("%s.avsc", avroSchemaFileName);
          InputStream inputAVSC = ExtractDataJob.class.getClassLoader().getResourceAsStream(avscFilePath);


          String objectNativeDelete = nativePath.asText() + dbName + "_" + schemaName + "_" + tableNameValue + "/";
          // LOG.info("Delete Native table: " + objectNativeDelete);
          // deleteObjectsInBucket(Region.US_EAST_1, bucketName.asText(), objectNativeDelete);

          // Read the Avro schema from the file
          Schema avroSchema = new Schema.Parser().parse(inputAVSC);

          // return Array list of row type
          RowType rowType = mapSchemaToRowDataType(avroSchema);
          List<DataType> dataTypeList = mapAvroSchemaToRowDataTypes(avroSchema);
          FieldsDataType dataType = new FieldsDataType(rowType, dataTypeList);


          Map<String, Object> schemaRegistryConfig = SchemaRegistryUtils.getSchemaRegistryConfig(jsonNode, avroSchemaFileName);

          Mssql mssql = new Mssql(jdbcUrl, username, password, schemaName);
          DataStream<Row> resultStream = mssql.queryTable(tableNameValue, env);

          DataStream<GenericRecord> resultGenStream = resultStream.map(
              row -> {
                MyCustomRowRowConverter customRowRowConverter = MyCustomRowRowConverter.create(dataType);
                RowData rowData = customRowRowConverter.toInternal(row);
                RowDataToAvroConverters.RowDataToAvroConverter rowDataToAvroConverter = RowDataToAvroConverters.createConverter(rowType);
                return (GenericRecord) rowDataToAvroConverter.convert(avroSchema, rowData);
              }
          ).setParallelism(8)
          .slotSharingGroup(String.format("Map-Sink %s",tableNameValue))
          .name(String.format("Map %s",tableNameValue))
          .setDescription(String.format("Map table %s",tableNameValue));

          // Setup Kafka sink
          String brokers = secretJson.getString("BROKERS_URL_SCRAM");
          String brokersUser = secretJson.getString("BROKERS_SCRAM_USER");
          String brokersScramPassword = secretJson.getString("BROKERS_SCRAM_PASSWORD");
          String topicName = prefixTopic + "_" + dbName + "_" + schemaName + "_" + tableNameValue;
          
          KafkaSink<GenericRecord> stringSink = KafkaSink.<GenericRecord>builder()
//								.setKafkaProducerConfig(KafkaPropertiesConfig.getKafkaConfigLocal())
              .setKafkaProducerConfig(KafkaPropertiesConfig.getKafkaConfigScram(brokersUser, brokersScramPassword)) // uncomment it when use on lab env
              .setBootstrapServers(brokers)
              .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                  .setTopic(topicName)
                  .setValueSerializationSchema(GlueSchemaRegistryAvroSerializationSchema.forGeneric(avroSchema, topicName, schemaRegistryConfig))
                  .build()
              ).setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
              .setTransactionalIdPrefix(topicName)
              .build();
          resultGenStream.sinkTo(stringSink)
                  .setParallelism(8)
                  .slotSharingGroup(String.format("Map-Sink %s",tableNameValue))
                  .name(String.format("Sink %s",tableNameValue))
                  .setDescription(String.format("Sink table %s",tableNameValue));
        }
      }
    }
    //execute the program
    env.execute("Extract data");
  }
}
