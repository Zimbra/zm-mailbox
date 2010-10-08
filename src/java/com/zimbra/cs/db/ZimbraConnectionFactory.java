package com.zimbra.cs.db;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.PoolConfig;

/**
 * Default ConnectionFactory implementation
 *
 */
public class ZimbraConnectionFactory extends DriverManagerConnectionFactory {
    
    private static ConnectionFactory sConnFactory = null;
    public static ConnectionFactory getConnectionFactory(PoolConfig pconfig) {
        if (sConnFactory == null) {
            String className = LC.zimbra_class_dbconnfactory.value();
            if (className != null && !className.equals("")) {
                try {
                    ZimbraLog.dbconn.info("instantiating DB connection factory class "+className);
                    Class clazz = Class.forName(className);
                    Constructor constructor = clazz.getDeclaredConstructor(String.class, Properties.class);
                    sConnFactory = (ConnectionFactory) constructor.newInstance(pconfig.mConnectionUrl, pconfig.mDatabaseProperties);
                } catch (Exception e) {
                    ZimbraLog.system.error("could not instantiate database connection pool '" + className + "'; defaulting to ZimbraConnectionFactory", e);
                }
            }
            if (sConnFactory == null)
                sConnFactory = new ZimbraConnectionFactory(pconfig.mConnectionUrl, pconfig.mDatabaseProperties);
        }
        return sConnFactory;
    }
    
    ZimbraConnectionFactory(String connectUri, Properties props) {
        super(connectUri, props);
    }
        
    /**
     * Wraps the JDBC connection from the pool with a <tt>DebugConnection</tt>,
     * which does  <tt>sqltrace</tt> logging.
     */
    @Override 
    public java.sql.Connection createConnection() throws SQLException {
        java.sql.Connection conn = super.createConnection();
        Db.getInstance().postCreate(conn);
        return new DebugConnection(conn);   
    }
}