package dp.icario.com.utils;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.connector.jdbc.JdbcInputFormat;
import org.apache.flink.connector.jdbc.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import org.apache.flink.connector.jdbc.table.JdbcRowDataInputFormat;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.util.DataFormatConverters;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Mssql {
  private final String jdbcUrl;
  private final String username;
  private final String password;
  private final String schema;

  public Mssql(String jdbcUrl, String username, String password, String schema) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
    this.schema = schema;
  }

  private static DataType getDataType(String columnTypeName) {
    Map<String, DataType> typeMapping = new HashMap<>();
    typeMapping.put("INT", DataTypes.INT());
    typeMapping.put("BIGINT", DataTypes.BIGINT());
    typeMapping.put("TINYINT", DataTypes.SMALLINT());
    typeMapping.put("SMALLINT", DataTypes.SMALLINT());
    typeMapping.put("SMALLDATETIME", DataTypes.TIMESTAMP());
    typeMapping.put("DATETIME", DataTypes.TIMESTAMP());
    typeMapping.put("DATETIME2", DataTypes.TIMESTAMP());
    typeMapping.put("DATE", DataTypes.DATE());
    typeMapping.put("BIT", DataTypes.BOOLEAN());

    return typeMapping.getOrDefault(columnTypeName.toUpperCase(), DataTypes.STRING());
  }

  public class ListType {
    private List<String> fieldNames;
    private List<TypeInformation<?>> fieldTypes;
    private List<DataType> dataType;

    public ListType() {
    }

    public ListType(List<TypeInformation<?>> fieldTypes, List<String> fieldNames, List<DataType> fieldDataTypes) {
      this.fieldNames = fieldNames;
      this.fieldTypes = fieldTypes;
      this.dataType = fieldDataTypes;
    }

    public ListType(List<TypeInformation<?>> fieldTypes, List<String> fieldNames) {
      this.fieldNames = fieldNames;
      this.fieldTypes = fieldTypes;
    }

    public List<String> getFieldNames() {
      return fieldNames;
    }

    public void setFieldNames(List<String> fieldNames) {
      this.fieldNames = fieldNames;
    }

    public List<TypeInformation<?>> getFieldTypes() {
      return fieldTypes;
    }

    public void setFieldTypes(List<TypeInformation<?>> fieldTypes) {
      this.fieldTypes = fieldTypes;
    }

    public List<DataType> getDataType() {
      return dataType;
    }

    public void setDataType(List<DataType> fieldDataTypes) {
      this.dataType = fieldDataTypes;
    }
  }

  public ListType getSchema(String tableName) {
    List<String> fieldNames = new ArrayList<>();
    List<TypeInformation<?>> fieldTypes = new ArrayList<>();
    List<DataType> dataTypeList = new ArrayList<>();
    Logger LOG = LoggerFactory.getLogger(Mssql.class);
    try (Connection connection = DriverManager.getConnection(this.jdbcUrl, this.username, this.password)) {
      Statement stmt = connection.createStatement();
      String sql = "SELECT TOP 1 * FROM " + this.schema + "." + tableName;
      ResultSet rs = stmt.executeQuery(sql);
      ResultSetMetaData rsmd = rs.getMetaData();
      int countColumns = rsmd.getColumnCount();
      for (int i = 1; i <= countColumns; i++) {
        fieldNames.add(rsmd.getColumnName(i));
        fieldTypes.add(getSqlType(rsmd.getColumnTypeName(i)));
        // dataTypeList.add(getDataType(rsmd.getColumnTypeName(i)));
      }
      connection.close();

      return new ListType(fieldTypes, fieldNames);

    } catch (SQLException e) {
      LOG.info("Error SQL is :" + e.getMessage());
      return new ListType();
    }
  }

  public DataStream<Row> queryTable(String tableName, StreamExecutionEnvironment env) {
    ListType listType = getSchema(tableName);
    RowTypeInfo rowTypeInfo = buildRowTypeInfo(listType);
    JdbcInputFormat jdbcInputFormat = buildJdbcInputFormat(rowTypeInfo, tableName);

    return env.createInput(jdbcInputFormat)
            .setParallelism(1)
            .slotSharingGroup(String.format("Source %s",tableName))
            .name(String.format("Source %s",tableName))
            .setDescription(String.format("Get data from table %s",tableName));
  }

  private TypeInformation<?> getSqlType(String columnTypeName) {
    switch (columnTypeName.toUpperCase()) {
      case "DECIMAL":
        return Types.BIG_DEC;
      case "INT":
        return Types.INT;
      case "BIGINT":
        return Types.LONG;
      case "TINYINT":
      case "SMALLINT":
        return Types.SHORT;
      case "DATE":
        return Types.SQL_DATE;
      case "BIT":
        return Types.BOOLEAN;
      case "SMALLDATETIME":
      case "DATETIME":
      case "DATETIME2":
        return Types.SQL_TIMESTAMP;
      default:
        return Types.STRING;
    }
  }

  protected RowTypeInfo buildRowTypeInfo(ListType listType) {
    String[] fieldNamesArray = listType.getFieldNames().toArray(new String[0]);

    // DataType[] fieldDataTypes=listType.getDataType().toArray(new DataType[0]);
    TypeInformation<?>[] fieldTypesArray = listType.getFieldTypes().toArray(new TypeInformation[0]);

    return new RowTypeInfo(fieldTypesArray, fieldNamesArray);
  }

  protected JdbcInputFormat buildJdbcInputFormat(RowTypeInfo rowTypeInfo, String tableName) {
    String query = String.format("Select * from %s.%s", this.schema, tableName);

    return JdbcInputFormat.buildJdbcInputFormat().setDrivername("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .setDBUrl(this.jdbcUrl).setUsername(this.username).setPassword(this.password).setRowTypeInfo(rowTypeInfo)
        .setQuery(query).finish();
  }
}
