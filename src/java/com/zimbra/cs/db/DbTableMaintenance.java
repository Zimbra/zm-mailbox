/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/**
 * @author bburtin
 */
package com.zimbra.cs.db;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class DbTableMaintenance {
    
    public static int runMaintenance()
    throws ServiceException {
        if (!(Db.getInstance() instanceof MySQL)) {
            ZimbraLog.mailbox.warn("Table maintenance only supported for MySQL.");
            return 0;
        }
        
        int numTables = 0;
        DbResults results = DbUtil.executeQuery(
            "SELECT table_schema, table_name " +
            "FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE table_schema = 'zimbra' " +
            "OR table_schema LIKE '" + DbMailbox.DB_PREFIX_MAILBOX_GROUP + "%'");

        while (results.next()) {
            String dbName = results.getString("TABLE_SCHEMA");
            String tableName = results.getString("TABLE_NAME");
            String sql = String.format("ANALYZE TABLE %s.%s", dbName, tableName); 
            ZimbraLog.mailbox.info("Running %s", sql);
            DbUtil.executeUpdate(sql);
            numTables++;
        }
        
        return numTables;
    }
}
