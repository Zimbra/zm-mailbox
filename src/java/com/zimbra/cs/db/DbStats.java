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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Callback <code>Accumulator</code> that returns current values for important
 * database statistics.
 *
 */
class DbStats implements RealtimeStatsCallback {

    private static Log sLog = LogFactory.getLog(DbStats.class);
    private static final Pattern PATTERN_PAGES_RW =
        Pattern.compile("Pages read (\\d+).*written (\\d+)");
    private static final Pattern PATTERN_BP_HIT_RATE = Pattern.compile("hit rate (\\d+)");
    
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();

        try {
            data.put(ZimbraPerf.RTS_DB_POOL_SIZE, DbPool.getSize());
            data.put(ZimbraPerf.RTS_MYSQL_OPENED_TABLES, getStatus("opened_tables"));
            data.put(ZimbraPerf.RTS_MYSQL_SLOW_QUERIES, getStatus("slow_queries"));
            data.put(ZimbraPerf.RTS_MYSQL_THREADS_CONNECTED, getStatus("threads_connected"));
            
            // Parse innodb status output
            DbResults results = DbUtil.executeQuery("SHOW INNODB STATUS");
            BufferedReader r = new BufferedReader(new StringReader(results.getString(1)));
            String line = null;
            while ((line = r.readLine()) != null) {
                Matcher m = PATTERN_PAGES_RW.matcher(line);
                if (m.matches()) {
                    data.put(ZimbraPerf.RTS_INNODB_PAGES_READ, m.group(1));
                    data.put(ZimbraPerf.RTS_INNODB_PAGES_WRITTEN, m.group(2));
                } else {
                    m = PATTERN_BP_HIT_RATE.matcher(line);
                    if (m.find()) {
                        data.put(ZimbraPerf.RTS_INNODB_BP_HIT_RATE, m.group(1));
                    }
                }
            }
        } catch (Exception e) {
            sLog.warn("An error occurred while getting current database stats", e);
        }
        
        return data;
    }

    private String getStatus(String variable)
    throws ServiceException {
        DbResults results = DbUtil.executeQuery("SHOW STATUS LIKE '" + variable + "'");
        return results.getString(2);
    }
}
