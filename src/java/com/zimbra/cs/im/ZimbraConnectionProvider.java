package com.zimbra.cs.im;

import java.sql.Connection;
import java.sql.SQLException;

import org.jivesoftware.database.ConnectionProvider;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.service.ServiceException;

public class ZimbraConnectionProvider implements ConnectionProvider {

    public boolean isPooled() {
        return true;
    }

    
    public Connection getConnection() throws SQLException {
        try {
            return DbPool.getConnection().getConnection();
        } catch (ServiceException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SQLException) 
                throw (SQLException)cause;
            throw new SQLException("Caught ServiceException: "+ex.toString());
        }
    }

    public void restart() {
    }
    public void start() {
    }
    public void destroy() {
    }
}
