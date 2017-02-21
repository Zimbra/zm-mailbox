/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Callback <code>Accumulator</code> that returns current values for important
 * database statistics.
 */
class DbStats implements RealtimeStatsCallback {

    private static Log sLog = LogFactory.getLog(DbStats.class);
    private static final Pattern PATTERN_BP_HIT_RATE = Pattern.compile("hit rate (\\d+)");
    
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();

        try {
            data.put(ZimbraPerf.RTS_DB_POOL_SIZE, DbPool.getSize());
            
            // Parse innodb status output
            DbResults results = DbUtil.executeQuery("SHOW ENGINE INNODB STATUS");
            Integer hitRate = parseBufferPoolHitRate(results.getString("Status"));
            if (hitRate != null) {
                data.put(ZimbraPerf.RTS_INNODB_BP_HIT_RATE, hitRate);
            }
        } catch (Exception e) {
            sLog.warn("An error occurred while getting current database stats", e);
        }
        
        return data;
    }
    
    private static Integer parseBufferPoolHitRate(String innodbStatus)
    throws IOException {
        ZimbraLog.perf.debug("InnoDB status output:\n%s", innodbStatus);
        BufferedReader r = new BufferedReader(new StringReader(innodbStatus));
        String line = null;
        while ((line = r.readLine()) != null) {
            Matcher m = PATTERN_BP_HIT_RATE.matcher(line);
            if (m.find()) {
                String hitRate = m.group(1);
                ZimbraLog.perf.debug("Parsed hit rate: %s", hitRate);
                return Integer.parseInt(hitRate);
            }
        }
        return null;
    }
}
