/*
 * Created on 2005. 1. 26.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DbStatus {

    private static Log mLog = LogFactory.getLog(DbStatus.class);

    public static boolean healthCheck() {
        boolean result = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT 'foo' AS STATUS");
            rs = stmt.executeQuery();
            if (rs.next())
                result = true;
        } catch (SQLException e) {
            mLog.warn("Database health check error", e);
        } catch (ServiceException e) {
            mLog.warn("Database health check error", e);
        } finally {
            try {
                DbPool.closeResults(rs);
            } catch (ServiceException e) {
                mLog.info("Ignoring error while closing database result set during health check", e);
            }
            try {
				DbPool.closeStatement(stmt);
			} catch (ServiceException e) {
                mLog.info("Ignoring error while closing database statement during health check", e);            
            }
            if (conn != null)
                DbPool.quietClose(conn);
        }
        return result;
    }
}
