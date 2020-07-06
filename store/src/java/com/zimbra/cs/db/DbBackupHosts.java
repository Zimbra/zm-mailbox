package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.redolog.BackupHostManager.BackupHost;
import com.zimbra.cs.redolog.BackupHostManager.BackupHostStatus;

public class DbBackupHosts {

    private static boolean isRegistered(String host) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT created_at FROM backup_hosts WHERE host=?");
            stmt.setString(1, host);
            rs = stmt.executeQuery();
            if(rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error checking whether backup host %s is registered", host), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static boolean registerBackupHost(String host) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            if (isRegistered(host)) {
                return false;
            }
            stmt = conn.prepareStatement("INSERT INTO backup_hosts (host, created_at, flags) VALUES (?, NOW(), 1)");
            stmt.setString(1, host);
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error registering backup host %s", host), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static boolean deleteBackupHost(BackupHost host) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            if (isRegistered(host.getHost())) {
                return false;
            }
            stmt = conn.prepareStatement("DELETE FROM backup_hosts WHERE host = ?");
            stmt.setString(1, host.getHost());
            stmt.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error deleting backup host %s", host.getHost()), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static List<BackupHost> getBackupHosts() throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            List<BackupHost> hosts = new ArrayList<>();
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT id, host, created_at, flags FROM backup_hosts ORDER BY host");
            rs = stmt.executeQuery();
            while (rs.next()) {
                hosts.add(toBackupHost(rs));
            }
            return hosts;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("error getting backup hosts", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static BackupHost getBackupHost(int backupHostId) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT id, host, created_at, flags FROM backup_hosts WHERE id=?");
            stmt.setInt(1, backupHostId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                return toBackupHost(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error getting backup host for id %s", backupHostId), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void setStatus(BackupHost host, BackupHostStatus status) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("UPDATE backup_hosts SET flags=? where id=?");
            stmt.setInt(1, status.getValue());
            stmt.setInt(2, host.getHostId());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error registering backup host %s", host), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }


    public static BackupHostCounts getBackupHostCounts(BackupHost host) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            String sql =
                    "SELECT (SELECT COUNT(account_id) FROM mailbox WHERE backup_host_id=?) AS active, " +
                    "(SELECT COUNT(account_id) FROM pending_backup_host_assignments WHERE backup_host_id=?) AS pending, " +
                    "(SELECT COUNT(account_id) FROM deleted_account where backup_host_id=?) AS deleted;";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, host.getHostId());
            stmt.setInt(2, host.getHostId());
            stmt.setInt(3, host.getHostId());
            rs = stmt.executeQuery();
            rs.next();
            return new BackupHostCounts(host, rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error getting counts for backup host %s", host.getHost()), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static AccountsOnBackupHost getAccountsOnBackupHost(BackupHost host) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            String sql =
                    "SELECT account_id, comment AS account_name, \"active\" AS type FROM mailbox WHERE backup_host_id=? " +
                    "UNION SELECT account_id, email AS account_name, \"pending\" AS type FROM pending_backup_host_assignments WHERE backup_host_id=? " +
                    "UNION SELECT account_id, email AS account_name, \"deleted\" AS type FROM deleted_account WHERE backup_host_id=? ";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, host.getHostId());
            stmt.setInt(2, host.getHostId());
            stmt.setInt(3, host.getHostId());
            rs = stmt.executeQuery();
            AccountsOnBackupHost acctsOnHost = new AccountsOnBackupHost();
            while (rs.next()) {
                acctsOnHost.add(rs.getString("account_id"), rs.getString("account_name"), rs.getString("type"));
            }
            return acctsOnHost;
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error getting accounts on backup host %s", host.getHost()), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static BackupHost toBackupHost(ResultSet rs) throws SQLException {
        int hostId = rs.getInt(1);
        String hostname = rs.getString(2);
        Timestamp timestamp = rs.getTimestamp(3);
        BackupHostStatus status = BackupHostStatus.fromValue(rs.getInt(4));
        return new BackupHost(hostId, hostname, timestamp.getTime(), status);
    }


    public static class BackupHostCounts {
        private BackupHost host;
        private int numActive;
        private int numPending;
        private int numDeleted;

        public BackupHostCounts(BackupHost host, ResultSet rs) throws SQLException {
            this.host = host;
            this.numActive = rs.getInt(1);
            this.numPending = rs.getInt(2);
            this.numDeleted = rs.getInt(3);
        }

        public BackupHost getBackupHost() {
            return host;
        }

        public int getNumActive() {
            return numActive;
        }

        public int getNumPending() {
            return numPending;
        }

        public int getNumDeleted() {
            return numDeleted;
        }
    }

    public static class AccountsOnBackupHost {
        private List<Pair<String, String>> active = new ArrayList<>();
        private List<Pair<String, String>> pending = new ArrayList<>();
        private List<Pair<String, String>> deleted = new ArrayList<>();

        private void add(String accountId, String accountName, String type) {
            Pair<String, String> acctInfo = new Pair<>(accountId, accountName);
            if (type.equals("active")) {
                active.add(acctInfo);
            } else if (type.equals("pending")) {
                pending.add(acctInfo);
            } else if (type.equals("deleted")) {
                deleted.add(acctInfo);
            }
        }

        public List<Pair<String, String>> getActiveAccounts() {
            return active;
        }

        public List<Pair<String, String>> getPendingAccounts() {
            return pending;
        }

        public List<Pair<String, String>> getDeletedAccounts() {
            return deleted;
        }
    }
}