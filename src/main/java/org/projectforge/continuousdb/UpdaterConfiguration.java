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

package org.projectforge.continuousdb;

import org.projectforge.common.DatabaseDialect;

/**
 * Main class for configuration of this module.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UpdaterConfiguration
{
  private DatabaseExecutor databaseExecutor;

  private DatabaseSupport databaseSupport;

  private DatabaseDialect dialect;

  private SystemUpdater systemUpdater;

  private DatabaseUpdateDao databaseUpdateDao;

  public void setDatabaseExecutor(DatabaseExecutor databaseExecutor)
  {
    this.databaseExecutor = databaseExecutor;
  }
  
  public void setDatabaseUpdateDao(DatabaseUpdateDao databaseUpdateDao)
  {
    this.databaseUpdateDao = databaseUpdateDao;
  }
  
  public DatabaseUpdateDao getDatabaseUpdateDao()
  {
    return databaseUpdateDao;
  }

  public void setDialect(DatabaseDialect dialect)
  {
    this.dialect = dialect;
    this.databaseSupport = null;
  }

  public DatabaseExecutor getDatabaseExecutor()
  {
    return databaseExecutor;
  }

  public DatabaseSupport getDatabaseSupport()
  {
    if (databaseSupport == null) {
      databaseSupport = new DatabaseSupport(dialect);
    }
    return databaseSupport;
  }

  public SystemUpdater getSystemUpdater()
  {
    if (systemUpdater == null) {
      systemUpdater = new SystemUpdater(this);
    }
    return systemUpdater;
  }
}
