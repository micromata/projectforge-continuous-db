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

package org.projectforge.continuousdb.spring;

import javax.sql.DataSource;

import org.projectforge.continuousdb.DatabaseExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Using Spring's JdbcTemplate for executing jdbc commands.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseExecutorImpl implements DatabaseExecutor
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DatabaseExecutorImpl.class);

  private DataSource dataSource;

  @Override
  public DataSource getDataSource()
  {
    return dataSource;
  }

  @Override
  public void setDataSource(DataSource datasource)
  {
    this.dataSource = datasource;
  }

  @SuppressWarnings({ "rawtypes", "unchecked"})
  @Override
  public void execute(final String sql, final boolean ignoreErrors)
  {
    final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(new TransactionCallback() {
      public Object doInTransaction(final TransactionStatus status)
      {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(transactionManager.getDataSource());
        if (ignoreErrors == true) {
          try {
            jdbcTemplate.execute(sql);
          } catch (final Throwable ex) {
            log.info(ex.getMessage(), ex);
            return Boolean.FALSE;
          }
        } else {
          jdbcTemplate.execute(sql);
        }
        return null;
      }
    });
  }

  @Override
  public int queryForInt(String sql, Object... args)
  {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    return jdbc.queryForInt(sql, args);
  }

  @Override
  public int update(String sql, Object... args)
  {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    return jdbc.update(sql, args);
  }
}
