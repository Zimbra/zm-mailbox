/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.google.common.base.Joiner;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;

public class MySQL extends Db {

    private Map<Db.Error, Integer> mErrorCodes;

    MySQL() {
        mErrorCodes = new HashMap<Db.Error, Integer>(6);
        //see SQLExceptionMapper or MysqlErrorNumbers from MySQL Connector-J for listing of error codes
        mErrorCodes.put(Db.Error.DEADLOCK_DETECTED,        1213); //MysqlErrorNumbers.ER_LOCK_DEADLOCK
        mErrorCodes.put(Db.Error.DUPLICATE_ROW,            1062); //MysqlErrorNumbers.ER_DUP_ENTRY
        mErrorCodes.put(Db.Error.FOREIGN_KEY_NO_PARENT,    1216); //MysqlErrorNumbers.ER_NO_REFERENCED_ROW
        mErrorCodes.put(Db.Error.FOREIGN_KEY_CHILD_EXISTS, 1451); //
        mErrorCodes.put(Db.Error.NO_SUCH_DATABASE,         1146); //MysqlErrorNumbers.ER_NO_SUCH_TABLE
        mErrorCodes.put(Db.Error.NO_SUCH_TABLE,            1146); //MysqlErrorNumbers.ER_NO_SUCH_TABLE
        mErrorCodes.put(Db.Error.TABLE_FULL,               1114); //MysqlErrorNumbers.ER_RECORD_FILE_FULL
    }

    @Override
    boolean supportsCapability(Db.Capability capability) {
        switch (capability) {
            case AVOID_OR_IN_WHERE_CLAUSE:   return false;
            case BITWISE_OPERATIONS:         return true;
            case BOOLEAN_DATATYPE:           return true;
            case CASE_SENSITIVE_COMPARISON:  return false;
            case CAST_AS_BIGINT:             return false;
            case CLOB_COMPARISON:            return true;
            case DISABLE_CONSTRAINT_CHECK:   return true;
            case FILE_PER_DATABASE:          return false;
            case LIMIT_CLAUSE:               return true;
            case MULTITABLE_UPDATE:          return true;
            case NON_BMP_CHARACTERS:         return false;
            case ON_DUPLICATE_KEY:           return true;
            case ON_UPDATE_CASCADE:          return true;
            case READ_COMMITTED_ISOLATION:   return true;
            case REPLACE_INTO:               return true;
            case REQUEST_UTF8_UNICODE_COLLATION:  return true;
            case ROW_LEVEL_LOCKING:          return true;
            case UNIQUE_NAME_INDEX:          return true;
            case SQL_PARAM_LIMIT:            return false;
            case DUMPSTER_TABLES:            return true;
        }
        return false;
    }

    @Override
    boolean compareError(SQLException e, Db.Error error) {
        Integer code = mErrorCodes.get(error);
        return (code != null && e.getErrorCode() == code);
    }

    @Override
    String forceIndexClause(String index) {
        return " FORCE INDEX (" + index + ')';
    }

    @Override
    String getIFNULLClause(String expr1, String expr2) {
        return "IFNULL(" + expr1 + ", " + expr2 + ")";
    }

    @Override
    public String bitAND(String expr1, String expr2) {
        return expr1 + " & " + expr2;
    }

    @Override
    public String bitANDNOT(String expr1, String expr2) {
        return expr1 + " & ~" + expr2;
    }

    @Override
    DbPool.PoolConfig getPoolConfig() {
        return new MySQLConfig();
    }

    @Override
    public boolean databaseExists(DbConnection conn, String dbname) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int numSchemas = 0;

        try {
            stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA " +
                "WHERE schema_name = ?");
            stmt.setString(1, dbname);
            rs = stmt.executeQuery();
            rs.next();
            numSchemas = rs.getInt(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to determine whether database exists", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return (numSchemas > 0);
    }

    @Override
    public void enableStreaming(Statement stmt)
    throws SQLException {
        if (LC.jdbc_results_streaming_enabled.booleanValue()) {
            stmt.setFetchSize(Integer.MIN_VALUE);
        }
    }

    protected class MySQLConfig extends DbPool.PoolConfig {
        MySQLConfig() {
            mDriverClassName = getDriverClassName();
            mPoolSize = 100;
            mRootUrl = getRootUrl();
            mConnectionUrl = mRootUrl + "zimbra";
            mLoggerUrl = null;
            mSupportsStatsCallback = true;
            mDatabaseProperties = getDBProperties();

            // override pool size if specified in prefs
            String maxActive = (String) mDatabaseProperties.get("maxActive");
            if (maxActive != null) {
                try {
                    mPoolSize = Integer.parseInt(maxActive);
                } catch (NumberFormatException nfe) {
                    ZimbraLog.system.warn("exception parsing 'maxActive' pref; defaulting pool size to " + mPoolSize, nfe);
                }
            }
            ZimbraLog.misc.debug("Setting connection pool size to " + mPoolSize);
        }

        protected String getDriverClassName() {
            return "com.mysql.jdbc.Driver";
        }

        protected String getRootUrl() {
            return "jdbc:mysql://address=(protocol=tcp)(host=" + LC.mysql_bind_address.value() + ")(port=" + LC.mysql_port.value() + ")/";
        }

        protected Properties getDBProperties() {
            Properties props = new Properties();

            props.put("cacheResultSetMetadata", "true");
            props.put("cachePrepStmts", "true");
            // props.put("cacheCallableStmts", "true");
            props.put("prepStmtCacheSize", "25");
            // props.put("prepStmtCacheSqlLmiit", "256");
            props.put("autoReconnect", "true");
            props.put("useUnicode", "true");
            props.put("characterEncoding", "UTF-8");
            props.put("dumpQueriesOnException", "true");

            // props.put("connectTimeout", "0");    // connect timeout in msecs
            // props.put("initialTimeout", "2");    // time to wait between re-connects
            // props.put("maxReconnects", "3"");    // max number of reconnects to attempt

            // Set/override MySQL Connector/J connection properties from localconfig.
            // Localconfig keys with "zimbra_mysql_connector_" prefix are used.
            final String prefix = "zimbra_mysql_connector_";
            for (String key : LC.getAllKeys()) {
                if (!key.startsWith(prefix))
                    continue;
                String prop = key.substring(prefix.length());
                if (prop.length() > 0 && !prop.equalsIgnoreCase("logger")) {
                    props.put(prop, LC.get(key));
                    ZimbraLog.system.info("Setting mysql connector property: " + prop + "=" + LC.get(key));
                }
            }

            // These properties cannot be set with "zimbra_mysql_connector_" keys.
            props.put("user", LC.zimbra_mysql_user.value());
            props.put("password", LC.zimbra_mysql_password.value());

            return props;
        }
    }

    @Override
    public String toString() {
        return "MySQL";
    }

    private final String sTableName = "zimbra.flush_enforcer";
    private final String sCreateTable =
        "CREATE TABLE IF NOT EXISTS " + sTableName + " (dummy_column INTEGER) ENGINE = InnoDB";
    private final String sDropTable = "DROP TABLE IF EXISTS " + sTableName;

    @Override
    public synchronized void flushToDisk() {
        // Create a table and then drop it.  We take advantage of the fact that innodb will call
        // log_buffer_flush_to_disk() during CREATE TABLE or DELETE TABLE.
        DbConnection conn = null;
        PreparedStatement createStmt = null;
        PreparedStatement dropStmt = null;
        boolean success = false;
        try {
            try {
                conn = DbPool.getMaintenanceConnection();
                createStmt = conn.prepareStatement(sCreateTable);
                dropStmt = conn.prepareStatement(sDropTable);
                createStmt.executeUpdate();
                dropStmt.executeUpdate();
                success = true;
            } finally {
                DbPool.quietCloseStatement(createStmt);
                DbPool.quietCloseStatement(dropStmt);
                if (conn != null)
                    conn.commit();
                DbPool.quietClose(conn);
            }
        } catch (SQLException e) {
            // If there's an error, let's just log it but not bubble up the exception.
            ZimbraLog.dbconn.warn("ignoring error while forcing mysql to flush innodb log to disk", e);
        } catch (ServiceException e) {
            // If there's an error, let's just log it but not bubble up the exception.
            ZimbraLog.dbconn.warn("ignoring error while forcing mysql to flush innodb log to disk", e);
        } finally {
            if (!success) {
                // There was an error.
                // The whole point of this method is to force innodb to flush its log.  Innodb is
                // supposed to be flushing roughly every second in its master thread, so let's simply
                // wait a few seconds to give the master thread a chance.
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {}
            }
        }
    }


    public static void main(String args[]) {
        // command line argument parsing
        Options options = new Options();
        CommandLine cl = Versions.parseCmdlineArgs(args, options);

        String outputDir = cl.getOptionValue("o");
        File outFile = new File(outputDir, "versions-init.sql");
        outFile.delete();

        try {
            String redoVer = com.zimbra.cs.redolog.Version.latest().toString();
            /**
             * Insert the following config table values in a way that does not generate
             * an error if the key (name) already exists. NOTE: We do not want to use
             * "ON DUPLICATE KEY UPDATE."  If previous values already exist, it will be
             * the job of the upgrade scripts to handle updating version information.
             */
            String outStr = "-- AUTO-GENERATED .SQL FILE - Generated by the MySQL versions tool\n" +
                    "USE zimbra;\n" +
                    "INSERT IGNORE INTO zimbra.config(name, value, description)\n" +
                    "VALUES ('db.version', '" + Versions.DB_VERSION + "', 'db schema version');\n" +
                    "INSERT IGNORE INTO zimbra.config(name, value, description)\n" +
                    "VALUES ('index.version', '" + Versions.INDEX_VERSION + "', 'index version');\n" +
                    "INSERT IGNORE INTO zimbra.config(name, value, description)\n" +
                    "VALUES ('redolog.version', '" + redoVer + "', 'redolog version');\n" +
                    "\nCOMMIT;\n";

            Writer output = new BufferedWriter(new FileWriter(outFile));
            output.write(outStr);
            if (output != null)
                output.close();
        } catch (IOException e){
            System.out.println("ERROR - caught exception at\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public String concat(String... fieldsToConcat) {
        Joiner joiner = Joiner.on(", ").skipNulls();
        return "CONCAT(" + joiner.join(fieldsToConcat) + ")";
    }

    @Override
    public String sign(String field) {
        return "SIGN(" + field + ")";
    }

    @Override
    public String lpad(String field, int padSize, String padString) {
        return "LPAD(" + field + ", " + padSize + ", '" + padString + "')";
    }

    @Override
    public String limit(int offset, int limit) {
        return "LIMIT " + offset + "," + limit;
    }
}
