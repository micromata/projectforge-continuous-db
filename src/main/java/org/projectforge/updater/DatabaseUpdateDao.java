/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.updater;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.UniqueConstraint;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.projectforge.common.StringHelper;

/**
 * For manipulating the database (patching data etc.)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseUpdateDao
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DatabaseUpdateDao.class);

  private UpdaterConfiguration configuration;

  private DatabaseSupport getDatabaseSupport()
  {
    return configuration.getDatabaseSupport();
  }

  private DatabaseExecutor getDatabaseExecutor()
  {
    return configuration.getDatabaseExecutor();
  }

  private DataSource getDataSource()
  {
    return configuration.getDatabaseExecutor().getDataSource();
  }

  /**
   * Does nothing at default. Override this method for checking the access of the user, e. g. only admin user's should be able to manipulate
   * the data-base.
   * @param writeaccess
   */
  protected void accessCheck(final boolean writeaccess)
  {
    return;
  }

  public boolean doesTableExist(final String table)
  {
    accessCheck(false);
    return internalDoesTableExist(table);
  }

  public boolean doesEntitiesExist(final Class< ? >... entities)
  {
    accessCheck(false);
    for (final Class< ? > entity : entities) {
      if (internalDoesTableExist(new Table(entity).getName()) == false) {
        return false;
      }
    }
    return true;
  }

  public boolean doesExist(final Table... tables)
  {
    accessCheck(false);
    for (final Table table : tables) {
      if (internalDoesTableExist(table.getName()) == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * Without check access.
   * @param table
   * @return
   */
  public boolean internalDoesTableExist(final String table)
  {
    /*
     * try { final ResultSet resultSet = dataSource.getConnection().getMetaData().getTables(CATALOG, SCHEMA_PATTERN, table, new String[] {
     * TABLE_TYPE}); return resultSet.next(); } catch (final SQLException ex) { log.error(ex.getMessage(), ex); throw new
     * InternalErrorException(ex.getMessage()); }
     */
    DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      jdbc.queryForInt("SELECT COUNT(*) FROM " + table);
    } catch (final Exception ex) {
      return false;
    }
    return true;
  }

  public boolean doesTableAttributeExist(final String table, final String attribute)
  {
    accessCheck(false);
    DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      jdbc.queryForInt("SELECT COUNT(" + attribute + ") FROM " + table);
    } catch (final Exception ex) {
      return false;
    }
    return true;
  }

  public boolean doesTableAttributesExist(final Class< ? > entityClass, final String... properties)
  {
    accessCheck(false);
    final Table table = new Table(entityClass);
    return doesTableAttributesExist(table, properties);
  }

  public boolean doesTableAttributesExist(final Table table, final String... properties)
  {
    accessCheck(false);
    for (final String property : properties) {
      final TableAttribute attr = new TableAttribute(table.getEntityClass(), property);
      if (doesTableAttributeExist(table.getName(), attr.getName()) == false) {
        return false;
      }
    }
    return true;
  }

  public boolean isTableEmpty(final String table)
  {
    accessCheck(false);
    return internalIsTableEmpty(table);
  }

  public boolean internalIsTableEmpty(final String table)
  {
    DatabaseExecutor jdbc = getDatabaseExecutor();
    try {
      return jdbc.queryForInt("SELECT COUNT(*) FROM " + table) == 0;
    } catch (final Exception ex) {
      return false;
    }
  }

  /**
   * @param table
   * @return true, if the table is successfully dropped or does not exist.
   */
  public boolean dropTable(final String table)
  {
    accessCheck(true);
    if (doesTableExist(table) == false) {
      // Table is already dropped or does not exist.
      return true;
    }
    if (isTableEmpty(table) == false) {
      // Table is not empty.
      log.warn("Could not drop table '" + table + "' because the table is not empty.");
      return false;
    }
    execute("DROP TABLE " + table);
    return true;
  }

  /**
   * @param table
   * @param attribute
   * @return
   */
  public boolean dropTableAttribute(final String table, final String attribute)
  {
    accessCheck(true);
    execute("ALTER TABLE " + table + " DROP COLUMN " + attribute);
    return true;
  }

  /**
   * @param table
   * @param attribute
   * @param length
   * @return
   */
  public boolean alterTableColumnVarCharLength(final String table, final String attribute, final int length)
  {
    accessCheck(true);
    execute(getDatabaseSupport().alterTableColumnVarCharLength(table, attribute, length), false);
    return true;
  }

  public void buildCreateTableStatement(final StringBuffer buf, final Table table)
  {
    buf.append("CREATE TABLE " + table.getName() + " (\n");
    boolean first = true;
    for (final TableAttribute attr : table.getAttributes()) {
      if (attr.getType().isIn(TableAttributeType.LIST, TableAttributeType.SET) == true) {
        // Nothing to be done here.
        continue;
      }
      if (first == true) {
        first = false;
      } else {
        buf.append(",\n");
      }
      buf.append("  ");
      buildAttribute(buf, attr);
    }
    final TableAttribute primaryKey = table.getPrimaryKey();
    if (primaryKey != null) {
      buf.append(getDatabaseSupport().getPrimaryKeyTableSuffix(primaryKey));
    }
    // Create foreign keys if exist
    for (final TableAttribute attr : table.getAttributes()) {
      if (StringUtils.isNotEmpty(attr.getForeignTable()) == true) {
        // foreign key (user_fk) references t_pf_user(pk)
        buf.append(",\n  FOREIGN KEY (").append(attr.getName()).append(") REFERENCES ").append(attr.getForeignTable()).append("(")
            .append(attr.getForeignAttribute()).append(")");
      }
    }
    final UniqueConstraint[] uniqueConstraints = table.getUniqueConstraints();
    if (uniqueConstraints != null && uniqueConstraints.length > 0) {
      for (final UniqueConstraint uniqueConstraint : uniqueConstraints) {
        final String[] columnNames = uniqueConstraint.columnNames();
        if (columnNames.length > 0) {
          buf.append(",\n  UNIQUE (");
          String separator = "";
          for (final String columnName : columnNames) {
            buf.append(separator).append(columnName);
            separator = ",";
          }
          buf.append(")");
        }
      }
    }
    buf.append("\n);\n");
  }

  private void buildAttribute(final StringBuffer buf, final TableAttribute attr)
  {
    buf.append(attr.getName()).append(" ");
    final Column columnAnnotation = attr.getAnnotation(Column.class);
    if (columnAnnotation != null && StringUtils.isNotEmpty(columnAnnotation.columnDefinition()) == true) {
      buf.append(columnAnnotation.columnDefinition());
    } else {
      buf.append(getDatabaseSupport().getType(attr));
    }
    boolean primaryKeyDefinition = false; // For avoiding double 'not null' definition.
    if (attr.isPrimaryKey() == true) {
      final String suffix = getDatabaseSupport().getPrimaryKeyAttributeSuffix(attr);
      if (StringUtils.isNotEmpty(suffix) == true) {
        buf.append(suffix);
        primaryKeyDefinition = true;
      }
    }
    if (primaryKeyDefinition == false) {
      getDatabaseSupport().addDefaultAndNotNull(buf, attr);
    }
    // if (attr.isNullable() == false) {
    // buf.append(" NOT NULL");
    // }
    // if (StringUtils.isNotBlank(attr.getDefaultValue()) == true) {
    // buf.append(" DEFAULT(").append(attr.getDefaultValue()).append(")");
    // }
  }

  public void buildForeignKeyConstraint(final StringBuffer buf, final String table, final TableAttribute attr)
  {
    buf.append("ALTER TABLE ").append(table).append(" ADD CONSTRAINT ").append(table).append("_").append(attr.getName())
        .append(" FOREIGN KEY (").append(attr.getName()).append(") REFERENCES ").append(attr.getForeignTable()).append("(")
        .append(attr.getForeignAttribute()).append(");\n");
  }

  public boolean createTable(final Table table)
  {
    accessCheck(true);
    if (doesExist(table) == true) {
      log.info("Table '" + table.getName() + "' does already exist.");
      return false;
    }
    final StringBuffer buf = new StringBuffer();
    buildCreateTableStatement(buf, table);
    execute(buf.toString());
    return true;
  }

  public boolean createSequence(final String name, final boolean ignoreErrors)
  {
    accessCheck(true);
    final String sql = getDatabaseSupport().createSequence(name);
    if (sql != null) {
      execute(sql, ignoreErrors);
    }
    return true;
  }

  public void buildAddTableAttributesStatement(final StringBuffer buf, final String table, final TableAttribute... attributes)
  {
    for (final TableAttribute attr : attributes) {
      if (doesTableAttributeExist(table, attr.getName()) == true) {
        buf.append("-- Does already exist: ");
      }
      buf.append("ALTER TABLE ").append(table).append(" ADD COLUMN ");
      buildAttribute(buf, attr);
      buf.append(";\n");
    }
    for (final TableAttribute attr : attributes) {
      if (attr.getForeignTable() != null) {
        if (doesTableAttributeExist(table, attr.getName()) == true) {
          buf.append("-- Column does already exist: ");
        }
        buildForeignKeyConstraint(buf, table, attr);
      }
    }
  }

  public void buildAddTableAttributesStatement(final StringBuffer buf, final String table, final Collection<TableAttribute> attributes)
  {
    buildAddTableAttributesStatement(buf, table, attributes.toArray(new TableAttribute[0]));
  }

  /**
   * @param entityClass
   * @param attributeNames Property names of the attributes to create.
   * @return
   */
  public boolean addTableAttributes(final Class< ? > entityClass, final String... attributeNames)
  {
    return addTableAttributes(new Table(entityClass), attributeNames);
  }

  /**
   * @param table
   * @param attributeNames Property names of the attributes to create.
   * @return
   */
  public boolean addTableAttributes(final Table table, final String... attributeNames)
  {

    final TableAttribute[] attributes = new TableAttribute[attributeNames.length];
    for (int i = 0; i < attributeNames.length; i++) {
      attributes[i] = new TableAttribute(table.getEntityClass(), attributeNames[i]);
    }
    return addTableAttributes(table, attributes);
  }

  public boolean addTableAttributes(final String table, final TableAttribute... attributes)
  {
    final StringBuffer buf = new StringBuffer();
    buildAddTableAttributesStatement(buf, table, attributes);
    execute(buf.toString());
    return true;
  }

  public boolean addTableAttributes(final Table table, final TableAttribute... attributes)
  {
    return addTableAttributes(table.getName(), attributes);
  }

  public boolean addTableAttributes(final String table, final Collection<TableAttribute> attributes)
  {
    final StringBuffer buf = new StringBuffer();
    buildAddTableAttributesStatement(buf, table, attributes);
    execute(buf.toString());
    return true;
  }

  public boolean addTableAttributes(final Table table, final Collection<TableAttribute> attributes)
  {
    return addTableAttributes(table.getName(), attributes);
  }

  public void buildAddUniqueConstraintStatement(final StringBuffer buf, final String table, final String constraintName,
      final String... attributes)
  {
    buf.append("ALTER TABLE ").append(table).append(" ADD CONSTRAINT ").append(constraintName).append(" UNIQUE (");
    buf.append(StringHelper.listToString(", ", attributes));
    buf.append(");\n");
  }

  public boolean renameTableAttribute(final String table, final String oldName, final String newName)
  {
    final String alterStatement = getDatabaseSupport().renameAttribute(table, oldName, newName);
    execute(alterStatement);
    return true;
  }

  public boolean addUniqueConstraint(final Table table, final String constraintName, final String... attributes)
  {
    accessCheck(true);
    return addUniqueConstraint(table.getName(), constraintName, attributes);
  }

  public boolean addUniqueConstraint(final String table, final String constraintName, final String... attributes)
  {
    accessCheck(true);
    final StringBuffer buf = new StringBuffer();
    buildAddUniqueConstraintStatement(buf, table, constraintName, attributes);
    execute(buf.toString());
    return true;
  }

  /**
   * Creates missing data base indices of tables starting with 't_'.
   * @return Number of successful created data base indices.
   */
  public int createMissingIndices()
  {
    accessCheck(true);
    log.info("createMissingIndices called.");
    int counter = 0;
    // For user / time period search:
    log.warn("************** TODO *************");
    // TODO: createIndex("idx_timesheet_user_time", "t_timesheet", "user_id, start_time");
    try {
      final ResultSet reference = getDataSource().getConnection().getMetaData().getCrossReference(null, null, null, null, null, null);
      while (reference.next()) {
        final String fkTable = reference.getString("FKTABLE_NAME");
        final String fkCol = reference.getString("FKCOLUMN_NAME");
        if (fkTable.startsWith("t_") == true) {
          // Table of ProjectForge
          if (createIndex("idx_fk_" + fkTable + "_" + fkCol, fkTable, fkCol) == true) {
            counter++;
          }
        }
      }
    } catch (final SQLException ex) {
      log.error(ex.getMessage(), ex);
    }
    return counter;
  }

  public void insertInto(final String table, final String[] columns, final Object[] values)
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("insert into ").append(table).append(" (").append(StringHelper.listToString(",", columns)).append(") values (");
    boolean first = true;
    for (int i = 0; i < values.length; i++) {
      first = StringHelper.append(buf, first, "?", ",");
    }
    buf.append(")");
    DatabaseExecutor jdbc = getDatabaseExecutor();
    final String sql = buf.toString();
    log.info(sql + "; values = " + StringHelper.listToString(", ", values));
    jdbc.update(sql, values);
  }

  /**
   * @param regionId
   * @param version
   * @return true, if any entry for the given regionId and version is found in the data-base table t_database_update.
   */
  public boolean isVersionUpdated(final String regionId, final String version)
  {
    accessCheck(false);
    DatabaseExecutor jdbc = getDatabaseExecutor();
    final int result = jdbc.queryForInt("select count(*) from t_database_update where region_id=? and version=?", regionId, version);
    return result > 0;
  }

  /**
   * Creates the given data base index if not already exists.
   * @param name
   * @param table
   * @param attributes
   * @return true, if the index was created, false if an error has occured or the index already exists.
   */
  public boolean createIndex(final String name, final String table, final String attributes)
  {
    accessCheck(true);
    try {
      final String jdbcString = "CREATE INDEX " + name + " ON " + table + "(" + attributes + ");";
      execute(jdbcString);
      log.info(jdbcString);
      return true;
    } catch (final Throwable ex) {
      // Index does already exist (or an error has occurred).
      return false;
    }
  }

  /**
   * @param name
   * @param attributes
   * @return true, if the index was dropped, false if an error has occured or the index does not exist.
   */
  public boolean dropIndex(final String name)
  {
    accessCheck(true);
    try {
      execute("DROP INDEX " + name);
      return true;
    } catch (final Throwable ex) {
      // Index does already exist (or an error has occurred).
      return false;
    }
  }

  /**
   * @param jdbcString
   * @see #execute(String, boolean)
   */
  public void execute(final String jdbcString)
  {
    execute(jdbcString, true);
  }

  /**
   * Executes the given String
   * @param jdbcString
   * @param ignoreErrors If true (default) then errors will be caught and logged.
   * @return true if no error occurred (no exception was caught), otherwise false.
   */
  public void execute(final String jdbcString, final boolean ignoreErrors)
  {
    accessCheck(true);
    log.info(jdbcString);
    DatabaseExecutor jdbc = getDatabaseExecutor();
    jdbc.execute(jdbcString, ignoreErrors);
  }

  public int queryForInt(final String jdbcQuery)
  {
    accessCheck(false);
    DatabaseExecutor jdbc = getDatabaseExecutor();
    log.info(jdbcQuery);
    return jdbc.queryForInt(jdbcQuery);
  }

  /**
   * Will be called on shutdown.
   * @see DatabaseSupport#getShutdownDatabaseStatement()
   */
  public void shutdownDatabase()
  {
    final String statement = DatabaseSupport.instance().getShutdownDatabaseStatement();
    if (statement == null) {
      return;
    }
    log.info("Executing data-base shutdown statement: " + statement);
    execute(statement);
  }
}
