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
public class DbServiceStatus {

    private static Log mLog = LogFactory.getLog(DbServiceStatus.class);
    
    private static final String CN_SERVER = "server";
    private static final String CN_SERVICE = "service";
    private static final String CN_TIME = "time";
    private static final String CN_STATUS = "status";

    // these MUST be kept in sync with the database
    private static final int CI_SERVER = 1;
    private static final int CI_SERVICE = 2;
    private static final int CI_TIME = 3;    
    private static final int CI_STATUS = 4;

    private long mDbTime;
    private String mDbServer;
    private String mDbService;
    private int mDbStatus;
    
    public static List getStatus(Connection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList result = new ArrayList();
        try {
            stmt = conn.prepareStatement("SELECT * FROM service_status");
            rs = stmt.executeQuery();
            while (rs.next()) {
                DbServiceStatus dot = constructObjectType(rs);
                result.add(dot);
            }
        } catch (SQLException e) {
        	    throw ServiceException.FAILURE("getting service status", e);
        } finally {
        	DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }
    
    private static DbServiceStatus constructObjectType(ResultSet rs) throws SQLException {
        DbServiceStatus result = new DbServiceStatus();
        result.mDbTime = rs.getTimestamp(CI_TIME).getTime()/1000;
        result.mDbServer = rs.getString(CI_SERVER); 
        result.mDbService = rs.getString(CI_SERVICE);
        result.mDbStatus = rs.getInt(CI_STATUS);
        return result;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("service_status: {");
        sb.append(CN_SERVER).append(": ").append(mDbServer).append(", ");
        sb.append(CN_SERVICE).append(": ").append(mDbService).append(", ");
        sb.append(CN_TIME).append(": ").append(mDbTime).append(", ");
        sb.append(CN_STATUS).append(": ").append(mDbStatus);
        sb.append("}");
        return sb.toString();
    }

    /**
     * @return Returns the server.
     */
    public String getServer() {
        return mDbServer;
    }

    /**
     * @return Returns the service.
     */
    public String getService() {
        return mDbService;
    }
    /**
     * @return Returns the time, in msecs since 1970.
     */
    public long getTime() {
        return mDbTime;
    }
    /**
     * @return Returns the status
     */
    public int getStatus() {
        return mDbStatus;
    }

    public static void main(String args[]) throws ServiceException {
        Connection conn = DbPool.getConnection().getConnection();
        List stats = getStatus(conn);
        for (Iterator it=stats.iterator(); it.hasNext();) {
            DbServiceStatus stat = (DbServiceStatus) it.next();
            System.out.println(stat);
        }
    }
}
