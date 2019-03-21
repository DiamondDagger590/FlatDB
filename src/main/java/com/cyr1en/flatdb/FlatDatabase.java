/*
 * MIT License
 *
 * Copyright (c) 2019 Ethan Bacurio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cyr1en.flatdb;

import com.cyr1en.flatdb.annotations.processor.TableProcessor;
import lombok.Getter;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FlatDatabase implements Database {

  @Getter private String db_prefix;
  @Getter private Connection connection;
  @Getter private Set<FlatTable> tables;
  private Statement statement;

  FlatDatabase(DatabaseBuilder builder) throws SQLException {
    this.connection = DriverManager.getConnection(builder.getConnectionURL());
    this.statement = connection.createStatement();
    this.db_prefix = builder.getDatabasePrefix();
    initializeTables(builder.getTables());
  }

  private void initializeTables(List<Class> tableClasses) {
    TableProcessor processor = new TableProcessor(this);
    for (Class c : tableClasses)
      processor.process(c);
  }

  @Override
  public Optional<ResultSet> executeQuery(@Language("SQL") String query, String... replacements) {
    try {
      Statement statement = connection.createStatement();
      String fQuery = String.format(query, (Object[]) replacements);
      return Optional.of(statement.executeQuery(fQuery));
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (!statement.isClosed()) {
          statement.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  @Override
  public int executeUpdate(@Language("SQL") String sql, String... replacements) {
    try {
      Statement statement = connection.createStatement();
      String fSql = String.format(sql, (Object[]) replacements);
      return statement.executeUpdate(fSql);
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (!statement.isClosed()) {
          statement.close();
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return 0;
  }

  @Override
  public Optional<DatabaseMetaData> getMetaData() {
    try {
      return Optional.of(connection.getMetaData());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  public boolean tableExists(String tableName) {
    System.out.println("Checking if " + tableName + " exists!");
    List<String> tableNames = new ArrayList<>();
    getMetaData().ifPresent(dmd -> {
      try {
        ResultSet rs = dmd.getTables(null, null, null, new String[]{"TABLE"});
        while (rs.next())
          tableNames.add(rs.getString("TABLE_NAME"));
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
    return tableNames.stream().anyMatch(s -> s.equalsIgnoreCase(tableName));
  }

}