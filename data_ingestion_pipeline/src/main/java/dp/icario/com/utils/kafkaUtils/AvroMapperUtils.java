package dp.icario.com.utils.kafkaUtils;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import org.apache.flink.formats.avro.typeutils.AvroSchemaConverter;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.*;
import org.joda.time.LocalDate;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AvroMapperUtils {

  private static LogicalType mapAvroTypeToLogicalType(Schema avroType) {
    if (avroType.getType() == Schema.Type.UNION) {
      for (Schema type : avroType.getTypes()) {
        if (type.getType() != Schema.Type.NULL) {
          return mapAvroTypeToLogicalType(type);
        }
      }
    } else {
      if (avroType.getLogicalType() != null) {
        if (avroType.getLogicalType() == LogicalTypes.date()) {
          return DataTypes.DATE().getLogicalType();
        } else {
          return DataTypes.TIMESTAMP(9).getLogicalType();
        }
      } else {
        switch (avroType.getType()) {
          case DOUBLE:
            return new DoubleType();
          case NULL:
            return new NullType();
          case LONG:
            return new BigIntType();
          case STRING:
            return new VarCharType(250);
          case INT:
            return new IntType();
          case BOOLEAN:
            return new BooleanType();
          // Add more cases for other Avro types as needed
          default:
            throw new IllegalArgumentException("Unsupported Avro type: " + avroType.getType());
        }
      }

    }
    return null;
  }

  private static RowType.RowField mapAvroFieldToRowField(Schema.Field avroField) {
    LogicalType logicalType = mapAvroTypeToLogicalType(avroField.schema());
    return new RowType.RowField(avroField.name(), logicalType);
  }

  private static RowType.RowField mapLogicalTypeToRowData(Schema.Field avroField) {
    LogicalType logicalType = mapAvroTypeToLogicalType(avroField.schema());
    return new RowType.RowField(avroField.name(), logicalType);
  }

  public static RowType mapAvroSchemaToRowType(Schema avroSchema) {
    RowType.RowField[] rowFields = avroSchema.getFields()
        .stream()
        .map(AvroMapperUtils::mapAvroFieldToRowField)
        .toArray(RowType.RowField[]::new);
    return new RowType(Arrays.asList(rowFields));
  }

  public static RowType mapSchemaToRowDataType(Schema avroSchema) {
    RowType.RowField[] rowFields = avroSchema.getFields()
        .stream()
        .map(AvroMapperUtils::mapLogicalTypeToRowData)
        .toArray(RowType.RowField[]::new);
    return new RowType(Arrays.asList(rowFields));
  }

  private static DataType mapRowToRowDataTypeFields(Schema.Field avroField) {
    return mapAvroTypeToDataType(avroField.schema());
  }

  private static DataType mapAvroTypeToDataType(Schema avroType) {

    if (avroType.getType() == Schema.Type.UNION) {
      for (Schema type : avroType.getTypes()) {
        if (type.getType() != Schema.Type.NULL) {
          // This is the non-null type in the union
          return mapAvroTypeToDataType(type);
        }
      }
    } else {
      if (avroType.getLogicalType() != null) {
        if (avroType.getLogicalType() == LogicalTypes.date()) {
          // Handle logical type "date" by returning DataTypes.DATE()
          return DataTypes.DATE();
        } else
          return DataTypes.TIMESTAMP();
      } else {
        switch (avroType.getType()) {
          case DOUBLE:
            return DataTypes.DOUBLE();
          case FLOAT:
            return DataTypes.FLOAT();
          case STRING:
            return DataTypes.STRING();
          case BYTES:
            return DataTypes.BYTES();
          case LONG:
            return DataTypes.BIGINT();
          case INT:
            return DataTypes.INT();
          case BOOLEAN:
            return DataTypes.BOOLEAN();
          // Add more cases for other Avro types as needed
          default:
            throw new IllegalArgumentException("Unsupported Avro type: " + avroType.getType());
        }
      }
    }
    return null;
  }

  public static List<DataType> mapAvroSchemaToRowDataTypes(Schema avroSchema) {
    return avroSchema.getFields()
        .stream()
        .map(AvroMapperUtils::mapRowToRowDataTypeFields).collect(Collectors.toList());
  }
}
