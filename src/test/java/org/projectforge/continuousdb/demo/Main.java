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

package org.projectforge.continuousdb.demo;

import org.apache.commons.dbcp.BasicDataSource;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.continuousdb.DatabaseUpdateDao;
import org.projectforge.continuousdb.SchemaGenerator;
import org.projectforge.continuousdb.UpdaterConfiguration;
import org.projectforge.continuousdb.demo.entities.Address1DO;
import org.projectforge.continuousdb.demo.entities.Address2DO;
import org.projectforge.continuousdb.demo.entities.GroupDO;
import org.projectforge.continuousdb.demo.entities.UserDO;
import org.projectforge.continuousdb.jdbc.DatabaseExecutorImpl;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Main
{
  public static void main(String[] args)
  {
    Main main = new Main();
    main.createAndUpdateAddressDO();
  }

  private UpdaterConfiguration configuration;

  private DatabaseUpdateDao databaseUpdateDao;

  private Main()
  {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
    dataSource.setUsername("sa");
    // dataSource.setPassword("password");
    dataSource.setUrl("jdbc:hsqldb:testdatabase");
    dataSource.setMaxActive(10);
    dataSource.setMaxIdle(5);
    dataSource.setInitialSize(5);
    dataSource.setValidationQuery("SELECT 1");

    configuration = new UpdaterConfiguration();
    configuration.setDialect(DatabaseDialect.HSQL);
    final DatabaseExecutorImpl databaseExecutor = new DatabaseExecutorImpl();
    databaseExecutor.setDataSource(dataSource);
    configuration.setDatabaseExecutor(databaseExecutor);
    databaseUpdateDao = new DatabaseUpdateDao(configuration);
    // TableAttribute.register(new TableAttributeHookImpl());
    configuration.setDatabaseUpdateDao(databaseUpdateDao);

    // final SortedSet<UpdateEntry> updateEntries = new TreeSet<UpdateEntry>();
    // updateEntries.addAll(DatabaseCoreUpdates.getUpdateEntries(this));
    // getSystemUpdater().setUpdateEntries(updateEntries);

    // Create tables:
    if (databaseUpdateDao.doesEntitiesExist(UserDO.class, GroupDO.class) == false) {
      SchemaGenerator schemaGenerator = configuration.createSchemaGenerator().add(UserDO.class, GroupDO.class);
      schemaGenerator.createSchema();
      configuration.getDatabaseUpdateDao().createMissingIndices();
    }

    // Alter tables
    if (databaseUpdateDao.doesTableAttributesExist(UserDO.class, "username", "password") == false) {
      // username and/or password not yet in table t_user:
      databaseUpdateDao.addTableAttributes(UserDO.class, "username", "password"); // Works also, if one of both attributes does already
                                                                                  // exist.
    }
  }

  private void createAndUpdateAddressDO()
  {
    if (databaseUpdateDao.doesEntitiesExist(Address1DO.class) == false) {
      // Initial creation of t_address because data-base table doesn't yet exist:
      configuration.createSchemaGenerator().add(Address1DO.class).createSchema();
    }
    if (databaseUpdateDao.doesEntitiesExist(Address1DO.class) == false) {
      throw new RuntimeException("What the hell? The table '" + Address1DO.class + "' wasn't created as expected!");
    }
    // Table t_address does now exist.
    if (databaseUpdateDao.doesTableAttributesExist(Address2DO.class, "birthday", "address") == false) {
      // One or both attributes don't yet exist, alter table to add the missing columns now:
      databaseUpdateDao.addTableAttributes(Address2DO.class, "birthday", "address"); // Works also, if one of both attributes does already
      // exist.
    }
    if (databaseUpdateDao.doesTableAttributesExist(Address2DO.class, "birthday", "address") == false) {
      throw new RuntimeException("What the hell? The missing columns 'birthday' and 'address' weren't created as expected!");
    }
  }
}
