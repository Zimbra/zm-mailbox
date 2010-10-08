package com.zimbra.cs.db;

import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;

/**
 * ConnectionFactory implementation which allows for retry on exception 
 *
 */
public class RetryConnectionFactory extends DriverManagerConnectionFactory {

    public RetryConnectionFactory(String connectUri, Properties props) {
        super(connectUri, props);
    }

    @Override 
    public java.sql.Connection createConnection() throws SQLException {
        java.sql.Connection conn = super.createConnection();
        Db.getInstance().postCreate(conn);
        return new RetryConnection(conn);
    }
}
