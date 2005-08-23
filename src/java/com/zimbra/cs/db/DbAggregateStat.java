/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Apr 8, 2004
 *
 * 
 */
package com.zimbra.cs.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 */
public class DbAggregateStat {

    private static Log mLog = LogFactory.getLog(DbAggregateStat.class);
    
    private static final String CN_TIME = "time";
    private static final String CN_NAME = "name";
    private static final String CN_VALUE = "value";
    private static final String CN_PERIOD = "period";

    // these MUST be kept in sync with the database
    private static final int CI_TIME = 1;
    private static final int CI_NAME = 2;
    private static final int CI_VALUE = 3;
    private static final int CI_PERIOD = 4;

    private long mDbTime;
    private String mDbName;
    private String mDbValue;
    private int mDbPeriod;
    
    public static List getStats(Connection conn, 
            String name, long startTime, long endTime, int period) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList result = new ArrayList();
        try {
            stmt = conn.prepareStatement(
                    "SELECT * FROM aggregate_stat WHERE name LIKE ? AND period = ? AND time > FROM_UNIXTIME(?) AND time <= FROM_UNIXTIME(?) ORDER BY TIME ASC");
            stmt.setString(1, name);
            stmt.setInt(2, period);
            stmt.setLong(3, startTime);
            stmt.setLong(4, endTime);            
            rs = stmt.executeQuery();
            while (rs.next()) {
                DbAggregateStat dot = constructObjectType(rs);
                result.add(dot);
            }
        } catch (SQLException e) {
        	    throw ServiceException.FAILURE("getting aggregate_stat", e);
        } finally {
        	DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }
    
    private static DbAggregateStat constructObjectType(ResultSet rs) throws SQLException {
        DbAggregateStat result = new DbAggregateStat();
        result.mDbTime = rs.getTimestamp(CI_TIME).getTime()/1000;
        result.mDbName = rs.getString(CI_NAME); 
        result.mDbValue = rs.getString(CI_VALUE);
        result.mDbPeriod = rs.getInt(CI_PERIOD);
        return result;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("aggregate_stat: {");
        sb.append(CN_TIME).append(": ").append(mDbTime).append(", ");
        sb.append(CN_NAME).append(": ").append(mDbName).append(", ");
        sb.append(CN_VALUE).append(": ").append(mDbValue).append(", ");
        sb.append(CN_PERIOD).append(": ").append(mDbPeriod);
        sb.append("}");
        return sb.toString();
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return mDbName;
    }

    /**
     * @return Returns the period.
     */
    public int getPeriod() {
        return mDbPeriod;
    }
    /**
     * @return Returns the time, in msecs since 1970.
     */
    public long getTime() {
        return mDbTime;
    }
    /**
     * @return Returns the value.
     */
    public String getValue() {
        return mDbValue;
    }

    public static void main(String args[]) throws ServiceException {
        Connection conn = DbPool.getConnection().getConnection();
        final long SPH = 60*60;
        final long SPD = SPH*24;
        long current = System.currentTimeMillis()/1000;
        List stats = getStats(conn, "test", current-SPD*4, current+SPH, 1);
        for (Iterator it=stats.iterator(); it.hasNext();) {
            DbAggregateStat stat = (DbAggregateStat) it.next();
            System.out.println(stat);
        }
    }
}
