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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.Accumulator;

/**
 * Callback <code>Accumulator</code> that returns current values for important
 * database statistics.
 *
 */
class DbStats implements Accumulator {

    private static Log sLog = LogFactory.getLog(DbStats.class);
    private static final Pattern PATTERN_PAGES_RW =
        Pattern.compile("Pages read (\\d+).*written (\\d+)");
    private static final Pattern PATTERN_BP_HIT_RATE = Pattern.compile("hit rate (\\d+)");
    private List mNames = new ArrayList();
    
    DbStats() {
        mNames.add("mysql_opened_tables");
        mNames.add("mysql_slow_queries");
        mNames.add("db_pool_size");
        mNames.add("mysql_threads_connected");
        mNames.add("innodb_pages_read");
        mNames.add("innodb_pages_written");
        mNames.add("innodb_bp_hit_rate");
    }
    
    public List<String> getNames() {
        return mNames;
    }

    public List<Object> getData() {
        List data = new ArrayList();
        // Initialize array, in case db ops fail
        for (int i = 0; i < mNames.size(); i++) {
            data.add(null);
        }

        try {
            data.set(0, getStatus("opened_tables"));
            data.set(1, getStatus("slow_queries"));
            data.set(2, DbPool.getSize());
            data.set(3, getStatus("threads_connected"));
            
            // Parse innodb status output
            DbResults results = DbUtil.executeQuery("SHOW INNODB STATUS");
            BufferedReader r = new BufferedReader(new StringReader(results.getString(1)));
            String line = null;
            while ((line = r.readLine()) != null) {
                Matcher m = PATTERN_PAGES_RW.matcher(line);
                if (m.matches()) {
                    data.set(4, m.group(1));
                    data.set(5, m.group(2));
                }
                m = PATTERN_BP_HIT_RATE.matcher(line);
                if (m.find()) {
                    data.set(6, m.group(1));
                }
            }
        } catch (Exception e) {
            sLog.warn("An error occurred while getting current database stats", e);
        }
        
        return data;
    }

    public void reset() {
    }

    private String getStatus(String variable)
    throws ServiceException {
        DbResults results = DbUtil.executeQuery("SHOW STATUS LIKE '" + variable + "'");
        return results.getString(2);
    }
}
