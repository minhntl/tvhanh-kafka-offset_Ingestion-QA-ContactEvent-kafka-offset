package dp.icario.com.utils.kafkaUtils;

import org.apache.avro.Schema;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AvroMapperUtilsTest {

  Schema avroSchema;

  @BeforeEach
  void init() throws IOException {
    String fileAVSCtest = "avro_schema_test.avsc";
    InputStream inputAVSC = AvroMapperUtilsTest.class.getClassLoader().getResourceAsStream(fileAVSCtest);

    avroSchema = new Schema.Parser().parse(inputAVSC);
  }

  @Test
  void testMapAvroSchemaToRowType() {
    RowType rowType = AvroMapperUtils.mapAvroSchemaToRowType(avroSchema);

    assertEquals(12, rowType.getFields().size());
  }

  @Test
  void testMapSchemaToRowDataType() {
    RowType rowType = AvroMapperUtils.mapSchemaToRowDataType(avroSchema);

    assertEquals(12, rowType.getFields().size());
  }

  @Test
  void testMapAvroSchemaToRowDataTypes() {
    List<DataType> dataTypes = AvroMapperUtils.mapAvroSchemaToRowDataTypes(avroSchema);

    assertEquals(12, dataTypes.size());
  }
}
