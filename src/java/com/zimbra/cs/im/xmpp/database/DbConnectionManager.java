package com.zimbra.cs.im.xmpp.database;

import java.sql.*;

public class DbConnectionManager {

    public static Connection getConnection() throws SQLException {
    	return null;
    }
    
    public static Connection getTransactionConnection() throws SQLException {
    	return null;
    }

    public static void closeTransactionConnection(Connection conn, boolean abort) {}

    public static ConnectionProvider getConnectionProvider() {
    	return null;
    }
    
    public static void setFetchSize(ResultSet rs, int fetchSize) {
    }
    
    public static PreparedStatement createScrollablePreparedStatement(Connection con, String sql) {
    	return null;
    }

    public static void scrollResultSet(ResultSet rs, int rowNumber) throws SQLException {
    }
    
    
}
