/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
