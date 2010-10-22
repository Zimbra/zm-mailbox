package com.zimbra.qa.unittest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.AbstractRetry;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.RetryConnectionFactory;

/**
 * Unit test to exercise busy handler
 * Not added to ZimbraSuite as it is specific to SQLite, and not used in normal ZCS operation
 *
 */
public class TestSQLiteBusyHandler extends TestCase {
    
    public static final int timeout = 120000; //2 minutes is long enough; might not get a busy every time, but should get one most of the time
    
    private final Log log = ZimbraLog.test;
    
    private static final String DB_PATH="data/unittest/sqlite/";
    public Connection createConnect() throws Exception
    {
        Class.forName("org.sqlite.JDBC");
        RetryConnectionFactory factory = new RetryConnectionFactory("jdbc:sqlite:"+DB_PATH+"zimbra.db", null);
        return factory.createConnection();
    }
    
    public String getDatabaseFilename(String dbname) {
        return DB_PATH + dbname + ".db";
    }

    void attachDatabase(Connection conn, String dbname) throws SQLException, ServiceException {
        PreparedStatement stmt = null;

        try {
            boolean autocommit = conn.getAutoCommit();
            if (!autocommit)
                conn.setAutoCommit(true);

            (stmt = conn.prepareStatement("ATTACH DATABASE \"" + getDatabaseFilename(dbname) + "\" AS " + dbname)).execute();
            log.debug("********************* attached %s",dbname);
        } catch (SQLException e) {
            log.error("database " + dbname + " attach failed", e);
            if (!"database is already attached".equals(e.getMessage()))
                throw e;
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
        
    }
    
    void detachDatabase(Connection conn, String dbname) throws SQLException {
        PreparedStatement stmt = null;
        try {
            boolean autocommit = conn.getAutoCommit();
            if (!autocommit)
                conn.setAutoCommit(true);

            (stmt = conn.prepareStatement("DETACH DATABASE " + dbname)).execute();
            log.debug("detached %s",dbname);
        } catch (SQLException e) {
            log.error("database " + dbname + " attach failed", e);
            if (!"database is already attached".equals(e.getMessage()))
                throw e;
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
    }
    
    public Connection connectAndAttach(String dbName) throws SQLException, ServiceException, Exception
    {
        Connection conn = createConnect();
        attachDatabase(conn , dbName);
        return conn;
    }
    
    public boolean checkSubject(String subject, String dbName, Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("select subject from "+dbName+".mail_item where subject='"+subject+"'");
        ResultSet rs = stmt.executeQuery();
        try {
            if (!rs.next()) {
                throw new Exception("no rows returned with subject ["+subject+"]");
            } else {
                if(rs.next()) {
                    throw new Exception("multiple rows returned with subject ["+subject+"]");
                }
            }
            log.debug("check subject OK");
            return true;
        } finally {
            rs.close();
            stmt.close();
        }
    }
    
    public void testUpdate(String dbName, Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("update "+dbName+".mail_item set subject='msg123' where subject='msg'");
        stmt.executeUpdate();
        stmt.close();
        checkSubject("msg123",dbName,conn);
        stmt = conn.prepareStatement("update "+dbName+".mail_item set subject='msg' where subject='msg123'");
        stmt.executeUpdate();
        stmt.close();
        checkSubject("msg",dbName,conn);
        log.debug("updated: %s",dbName);
        conn.commit();
    }
    
    public boolean testEntryName(String name, Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("select entry_name from directory where entry_id=2");
        ResultSet rs = stmt.executeQuery();
        try {
            if (!rs.next()) {
                throw new Exception("no rows returned for entry_id 2");
            } else {
                String checkName = rs.getString(1);
                if (checkName == null || !checkName.equals(name)) {
                    throw new Exception("expected ["+name+"] != db ["+checkName+"]");
                }
            }
            log.debug("check entry OK");
            return true;
        } finally {
            rs.close();
            stmt.close();
        }
    }
    
    
    public void testUpdateZimbra(Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("update directory set entry_name='blah' where entry_id=2");
        stmt.executeUpdate();
        stmt.close();
        testEntryName("blah",conn);
        stmt = conn.prepareStatement("update directory set entry_name='default' where entry_id=2");
        stmt.executeUpdate();
        stmt.close();
        testEntryName("default",conn);
        log.debug("updated: zimbra");
        conn.commit();
    }
    
    
    public void testSelectZimbra(Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement("select * from directory"); 
        ResultSet rs = ps.executeQuery();
        rs.close();
        ps.close();
        log.debug("read: zimbra");
    }
    
    public void integrityCheck(Connection conn, String dbname) throws Exception {
        PreparedStatement stmt = null;
        String prefix = dbname == null || dbname.equals("zimbra") ? "" : dbname + ".";
        stmt = conn.prepareStatement("PRAGMA " + prefix + "integrity_check");
        stmt.execute();
        ResultSet rs = stmt.getResultSet();
        try {
            if (rs.next()) {
                String resp = rs.getString(1);
                if (resp.equals("ok")) {
                    log.debug("integrity check ok");
                    return;
                }
            }
            throw new Exception("Integrity Check Failed");
        } finally {
            rs.close();
            stmt.close();
        }
            
    }

    public static boolean error = false;
    public static boolean timedOut = false;
    
    public void testNoBusy()
    {
        
        try {
            Thread t = new Thread("Update Zimbra") {
                public void run() {
                    try {
                        Connection conn1 = createConnect();
                        conn1.setAutoCommit(false);
                        while (!error && !timedOut) {
                            testUpdateZimbra(conn1);
                        }
                    } catch (Exception e) {
                        error = true;
                        log.error("Exception in thread", e);
                    }
                }
            };
            t.start();

            Thread t2 = new Thread("Attach Test2") {
                public void run() {
                    try {
                        Connection conn2 = createConnect();
                        while (!error && !timedOut) {
                            attachDatabase(conn2, "test2");
                            detachDatabase(conn2, "test2");
                        }
                    } catch (Exception e) {
                        error = true;
                        log.error("Exception in thread", e);
                    }
                }
            };
            t2.start();
            
            Thread t3 = new Thread("Update Test1") {
                public void run() {
                    try {
                        final Connection conn3 = connectAndAttach("test1");
                        attachDatabase(conn3, "test2");
                        conn3.setAutoCommit(false);
                        while (!error && !timedOut) {
                            testUpdate("test1", conn3);
                        }
                    } catch (Exception e) {
                        error = true;
                        log.error("Exception in thread",e);
                    }
                }
            };
            t3.start();
            
            //not normally enabled; integrity check doesn't play well with other concurrent operations
            //uncomment to stress the retry logic; will generally fail eventually with SQLITE_CANTOPEN
//            Thread t4 = new Thread("Integrity Check") {
//                public void run() {
//                    try {
//                        final Connection conn4 = connectAndAttach("test1");
//                        attachDatabase(conn4, "test2");
//                        while (!error && !timedOut) {
//                            integrityCheck(conn4, "test1");
//                            integrityCheck(conn4, "test2");
//                            integrityCheck(conn4, "zimbra");
//                        }
//                    } catch (Exception e) {
//                        error = true;
//                        log.error("Exception in thread",e);
//                    }
//                }
//            };
//            t4.start();
            
            Thread t5 = new Thread("Connection test") {
                public void run() {
                    try {
                        while (!error && !timedOut) {
                            final Connection conn5 = createConnect();
                            conn5.close();
                        }
                    } catch (Exception e) {
                        error = true;
                        log.error("Exception in thread",e);
                    }
                }
            };
            t5.start();

            Thread timeoutThread = new Thread("Timeout") {
                public void run() {
                    try {
                        log.info("running for "+timeout+"ms");
                        Thread.sleep(timeout);
                        timedOut = true;
                    } catch (InterruptedException e) {
                    }
                }
            };
            
            timeoutThread.start();
            boolean joined = false;
            while (!error && !joined) {
                timeoutThread.join(1000);
                if (!timeoutThread.isAlive()) {
                    joined = true;
                }
            }
        }
        catch (Exception e) {
            log.error("Exception",e);
            error = true;
        }
        
        if (error) {
            fail();
        } else {
            log.info("ran for "+timeout+"ms with no exceptions.");
            log.info("Encountered ["+AbstractRetry.getTotalRetries()+"] busy retries");
        }
    }
}
