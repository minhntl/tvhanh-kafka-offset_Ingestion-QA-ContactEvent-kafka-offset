package dp.icario.com.utils.kafkaUtils;

import org.apache.avro.Schema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.FieldsDataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static dp.icario.com.utils.kafkaUtils.AvroMapperUtils.mapAvroSchemaToRowDataTypes;
import static dp.icario.com.utils.kafkaUtils.AvroMapperUtils.mapSchemaToRowDataType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyCustomRowRowConverterTest {

  FieldsDataType dataType;

  @BeforeEach
  void init() throws IOException {
    String fileAVSCtest = "row_converter_schema_test.avsc";
    InputStream inputAVSC = AvroMapperUtilsTest.class.getClassLoader().getResourceAsStream(fileAVSCtest);
    Schema avroSchema = new Schema.Parser().parse(inputAVSC);
    RowType rowType = mapSchemaToRowDataType(avroSchema);
    List<DataType> dataTypeList = mapAvroSchemaToRowDataTypes(avroSchema);

    dataType = new FieldsDataType(rowType, dataTypeList);
  }

  @Test
  void testOpen() {
    ClassLoader mockClassLoader = mock(ClassLoader.class);
    MyCustomRowRowConverter myCustomRowRowConverter = mock(MyCustomRowRowConverter.class);

    myCustomRowRowConverter.open(mockClassLoader);

    verify(myCustomRowRowConverter, times(1)).open(mockClassLoader);
  }

  @Test
  void testToInternal() {
    MyCustomRowRowConverter myCustomRowRowConverter = MyCustomRowRowConverter.create(dataType);
    Row external = Row.of("USERID", "FIRSTNAME");

    RowData rowData = myCustomRowRowConverter.toInternal(external);

    assertEquals(RowKind.INSERT, rowData.getRowKind());
    assertEquals(2, rowData.getArity());
  }

  @Test
  void testToExternal() {
    MyCustomRowRowConverter myCustomRowRowConverter = MyCustomRowRowConverter.create(dataType);
    RowData internal = mock(RowData.class);
    when(internal.getRowKind()).thenReturn(RowKind.INSERT);

    Row row = myCustomRowRowConverter.toExternal(internal);

    assertEquals(2, row.getFieldNames(true).size());
    assertEquals(RowKind.INSERT, row.getKind());
  }

  @Test
  void testCreate() {
    try (MockedConstruction<MyCustomRowRowConverter> mockMyCustomRowRowConverter = mockConstruction(MyCustomRowRowConverter.class)) {
      MyCustomRowRowConverter.create(dataType);

      assertEquals(1, mockMyCustomRowRowConverter.constructed().size());
    }
  }
}
