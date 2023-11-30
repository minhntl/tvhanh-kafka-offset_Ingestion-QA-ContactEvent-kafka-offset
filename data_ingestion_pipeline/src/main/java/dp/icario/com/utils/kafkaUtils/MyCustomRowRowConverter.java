package dp.icario.com.utils.kafkaUtils;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.data.conversion.DataStructureConverter;
import org.apache.flink.table.data.conversion.DataStructureConverters;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.getFieldNames;

/** Converter for {@link RowType} of {@link Row} external type. */
@Internal
public class MyCustomRowRowConverter implements DataStructureConverter<RowData, Row> {

  private static final long serialVersionUID = 1L;

  private final DataStructureConverter<Object, Object>[] fieldConverters;

  private final RowData.FieldGetter[] fieldGetters;

  private final LinkedHashMap<String, Integer> positionByName;

  private MyCustomRowRowConverter(
      DataStructureConverter<Object, Object>[] fieldConverters,
      RowData.FieldGetter[] fieldGetters,
      LinkedHashMap<String, Integer> positionByName) {
    this.fieldConverters = fieldConverters;
    this.fieldGetters = fieldGetters;
    this.positionByName = positionByName;
  }

  @Override
  public void open(ClassLoader classLoader) {
    for (DataStructureConverter<Object, Object> fieldConverter : fieldConverters) {
      fieldConverter.open(classLoader);
    }
  }

  @Override
  public RowData toInternal(Row external) {
    final int length = fieldConverters.length;
    final GenericRowData genericRow = new GenericRowData(external.getKind(), length);

    final Set<String> fieldNames = external.getFieldNames(false);

    // position-based field access
    if (fieldNames == null) {
      for (int pos = 0; pos < length; pos++) {
        final Object value = external.getField(pos);
        if (value != null) {
          if (value.getClass().equals(Date.class)) {
            Date mydate = (Date) value;
            genericRow.setField(pos, (int) mydate.toLocalDate().toEpochDay());
          } else if (value.getClass().equals(Timestamp.class)) {
            Timestamp timestamp = (Timestamp) value;
            TimestampData timestampData = TimestampData.fromTimestamp(timestamp);
            genericRow.setField(pos, timestampData);
          } else if (value.getClass().equals(Short.class)) {
            Short shortValue = (Short) value;
            genericRow.setField(pos, shortValue.intValue());
          } else if (value.getClass().equals(BigDecimal.class)) {
            BigDecimal bigDecimalValue = (BigDecimal) value;
            genericRow.setField(pos, bigDecimalValue.doubleValue());
          } else
            genericRow.setField(pos, fieldConverters[pos].toInternalOrNull(value));
        } else
          genericRow.setField(pos, fieldConverters[pos].toInternalOrNull(value));
      }
    }
    // name-based field access
    else {
      for (String fieldName : fieldNames) {
        final Integer targetPos = positionByName.get(fieldName);
        if (targetPos == null) {
          throw new IllegalArgumentException(
              String.format(
                  "Unknown field name '%s' for mapping to a row position. "
                      + "Available names are: %s",
                  fieldName, positionByName.keySet()));
        }
        final Object value = external.getField(fieldName);
        if (value != null) {
          if (value.getClass().equals(Date.class)) {
            Date mydate = (Date) value;
            genericRow.setField(targetPos, (int) mydate.toLocalDate().toEpochDay());
          } else if (value.getClass().equals(Timestamp.class)) {
            Timestamp timestamp = (Timestamp) value;
            TimestampData timestampData = TimestampData.fromTimestamp(timestamp);
            genericRow.setField(targetPos, timestampData);
          } else if (value.getClass().equals(Short.class)) {
            Short shortValue = (Short) value;
            genericRow.setField(targetPos, shortValue.intValue());
          } else
            genericRow.setField(targetPos, fieldConverters[targetPos].toInternalOrNull(value));
        } else
          genericRow.setField(targetPos, fieldConverters[targetPos].toInternalOrNull(value));
      }
    }

    return genericRow;
  }

  @Override
  public Row toExternal(RowData internal) {
    final int length = fieldConverters.length;
    final Object[] fieldByPosition = new Object[length];
    for (int pos = 0; pos < length; pos++) {
      final Object value = fieldGetters[pos].getFieldOrNull(internal);
      fieldByPosition[pos] = fieldConverters[pos].toExternalOrNull(value);
    }
    return RowUtils.createRowWithNamedPositions(
        internal.getRowKind(), fieldByPosition, positionByName);
  }

  // --------------------------------------------------------------------------------------------
  // Factory method
  // --------------------------------------------------------------------------------------------

  @SuppressWarnings({ "unchecked", "Convert2MethodRef" })
  public static MyCustomRowRowConverter create(DataType dataType) {
    final List<DataType> fields = dataType.getChildren();
    final DataStructureConverter<Object, Object>[] fieldConverters = fields.stream()
        .map(dt -> DataStructureConverters.getConverter(dt))
        .toArray(DataStructureConverter[]::new);
    final RowData.FieldGetter[] fieldGetters = IntStream.range(0, fields.size())
        .mapToObj(
            pos -> RowData.createFieldGetter(
                fields.get(pos).getLogicalType(), pos))
        .toArray(RowData.FieldGetter[]::new);
    final List<String> fieldNames = getFieldNames(dataType.getLogicalType());
    final LinkedHashMap<String, Integer> positionByName = new LinkedHashMap<>();
    for (int i = 0; i < fieldNames.size(); i++) {
      positionByName.put(fieldNames.get(i), i);
    }
    return new MyCustomRowRowConverter(fieldConverters, fieldGetters, positionByName);
  }
}
