/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc.store;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.HARD_EXPIRY_DEFAULT;

/**
 * {@link IRopcTokenStore} backed by the Zimbra (boot) database.
 *
 * <p>Refresh and access tokens are AES-encrypted with {@link DataSource#encryptData(String, String)}
 * (keyed by account id) before storage and decrypted on read; only ciphertext is written.
 *
 * <p>Schema: see docs/idp-mfa-schema.sql — global table {@code mfa_oauth_token}
 * (PK account_id, provider).
 */
public final class DbRopcTokenStore implements IRopcTokenStore {

    private static final String TABLE_ROPC_TOKEN_STORE = "ropc_token_store";

    @Override
    public List<IRopcSessionRecord> find(Account account, String username, String userAgent, String provider,
                                         String protocol, String deviceId) throws ServiceException {
        if (username == null || provider == null || deviceId == null) {
            return null;
        }
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<IRopcSessionRecord> results = new ArrayList<>();
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);
            stmt = conn.prepareStatement(
                    "SELECT id, username, device_id, user_agent, ip, provider, protocol, refresh_token, " +
                            "id_token, password, created_at, updated_at"
                            + " FROM " + getTableName(mbox) + " WHERE username = ? AND provider = ? AND protocol = ? " +
                            "AND user_agent = ? AND device_id = ? ORDER BY created_at DESC");
            stmt.setString(1, username);
            stmt.setString(2, provider);
            stmt.setString(3, protocol);
            stmt.setString(4, userAgent);
            stmt.setString(5, deviceId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                IRopcSessionRecord record = new IRopcSessionRecord.Builder()
                        .id(rs.getLong(1))
                        .username(rs.getString(2))
                        .deviceId(rs.getString(3))
                        .userAgent(rs.getString(4))
                        .ip(rs.getString(5))
                        .provider(rs.getString(6))
                        .protocol(rs.getString(7))
                        .refreshToken(rs.getString(8))
                        .idToken(rs.getString(9))
                        .passwordHash(rs.getString(10))
                        .createdAt(rs.getLong(11))
                        .lastUpdatedAt(rs.getLong(12))
                        .build();
                results.add(record);

            }

            return results.isEmpty() ? null : results;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ROPC token lookup failed for " + username, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    @Override
    public List<IRopcSessionRecord>  findByIp(Account account, String username, String userAgent, String provider,
                                              String protocol, String ip) throws ServiceException {
        if (username == null || provider == null || ip == null) {
            return null;
        }
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<IRopcSessionRecord> results = new ArrayList<>();
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);
            stmt = conn.prepareStatement(
                    "SELECT id, username, device_id, user_agent, ip, provider, protocol, refresh_token, " +
                            "id_token, password, created_at, updated_at"
                            + " FROM " + getTableName(mbox) + " WHERE username = ? AND provider = ? AND protocol = ? " +
                            "AND user_agent = ? AND ip = ? ORDER BY created_at DESC");
            stmt.setString(1, username);
            stmt.setString(2, provider);
            stmt.setString(3, protocol);
            stmt.setString(4, userAgent);
            stmt.setString(5, ip);
            rs = stmt.executeQuery();

            while (rs.next()) {
                IRopcSessionRecord record = new IRopcSessionRecord.Builder()
                        .id(rs.getLong(1))
                        .username(rs.getString(2))
                        .deviceId(rs.getString(3))
                        .userAgent(rs.getString(4))
                        .ip(rs.getString(5))
                        .provider(rs.getString(6))
                        .protocol(rs.getString(7))
                        .refreshToken(rs.getString(8))
                        .idToken(rs.getString(9))
                        .passwordHash(rs.getString(10))
                        .createdAt(rs.getLong(11))
                        .lastUpdatedAt(rs.getLong(12))
                        .build();
                results.add(record);

            }

            return results.isEmpty() ? null : results;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ROPC token lookup failed for " + username, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    @Override
    public List<IRopcSessionRecord> findByDeviceIdAndUsername(Account account, String deviceId)
            throws ServiceException {
        if (account == null || deviceId == null) {
            return null;
        }
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<IRopcSessionRecord> results = new ArrayList<>();
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);
            stmt = conn.prepareStatement(
                    "SELECT id, username, device_id, user_agent, ip, provider, protocol, refresh_token, " +
                            "id_token, password, created_at, updated_at"
                            + " FROM " + getTableName(mbox) + " WHERE device_id = ? AND username = ?");
            stmt.setString(1, deviceId);
            stmt.setString(2, account.getName());
            rs = stmt.executeQuery();

            while (rs.next()) {
                IRopcSessionRecord record = new IRopcSessionRecord.Builder()
                        .id(rs.getLong(1))
                        .username(rs.getString(2))
                        .deviceId(rs.getString(3))
                        .userAgent(rs.getString(4))
                        .ip(rs.getString(5))
                        .provider(rs.getString(6))
                        .protocol(rs.getString(7))
                        .refreshToken(rs.getString(8))
                        .idToken(rs.getString(9))
                        .passwordHash(rs.getString(10))
                        .createdAt(rs.getLong(11))
                        .lastUpdatedAt(rs.getLong(12))
                        .build();
                results.add(record);

            }

            return results.isEmpty() ? null : results;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ROPC token lookup failed for " + account.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }

    }

    @Override
    public void upsert(Account account, IRopcSessionRecord session) throws ServiceException {
        if (session == null) {
            return;
        }

        DbConnection conn = null;
        PreparedStatement stmt = null;

        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);
            if (session.getId() == null) {
                // new login, insert the record
                stmt = conn.prepareStatement(
                        "INSERT INTO " + getTableName(mbox) + " " +
                                "(username, device_id, user_agent, ip, provider, protocol, refresh_token, " +
                                "id_token, password, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                                "On DUPLICATE KEY UPDATE " +
                                "refresh_token = VALUES(refresh_token), " +
                                "id_token = VALUES(id_token), " +
                                "password = VALUES(password), " +
                                "ip = VALUES(ip), " +
                                "created_at = VALUES(created_at), " +
                                "updated_at = VALUES(updated_at)");
                stmt.setString(1, session.getUsername());
                stmt.setString(2, session.getDeviceId());
                stmt.setString(3, session.getUserAgent());
                stmt.setString(4, session.getIp());
                stmt.setString(5, session.getProvider());
                stmt.setString(6, session.getProtocol());
                stmt.setString(7, session.getRefreshToken());
                stmt.setString(8, session.getIdToken());
                stmt.setString(9, session.getPasswordHash());
                stmt.setLong(10, session.getCreatedAt());
                stmt.setLong(11, session.getLastUpdatedAt());
                stmt.executeUpdate();
            } else {
                // its a token update : Update by id
                stmt = conn.prepareStatement(
                        "UPDATE " + getTableName(mbox) + " " +
                                "SET refresh_token = ?, id_token = ?, updated_at = ? " +
                                "WHERE id = ?");
                stmt.setString(1, session.getRefreshToken());
                stmt.setString(2, session.getIdToken());
                stmt.setLong(3, session.getLastUpdatedAt());
                stmt.setLong(4, session.getId());
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            DbPool.quietRollback(conn);
            throw ServiceException.FAILURE("Failed to upsert ROPC session for " + session.getUsername(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    @Override
    public void updateDeviceId(Account account, IRopcSessionRecord session)
            throws ServiceException {
        if (session == null) {
            return;
        }

        DbConnection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement deleteStmt = null;

        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);
            stmt = conn.prepareStatement(
                    "UPDATE " + getTableName(mbox) + " " +
                            "SET device_id = ?, updated_at = ? WHERE id = ?");
            stmt.setString(1, session.getDeviceId());
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setLong(3, session.getId());
            try {
                stmt.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException e) {
                // fallback
                deleteStmt = conn.prepareStatement(
                        "DELETE FROM " + getTableName(mbox) + " " +
                                "WHERE username = ? AND provider = ? AND protocol = ? AND " +
                                "device_id = ? AND user_agent = ? AND id != ?"
                );
                deleteStmt.setString(1, session.getUsername());
                deleteStmt.setString(2, session.getProvider());
                deleteStmt.setString(3, session.getProtocol());
                deleteStmt.setString(4, session.getDeviceId());
                deleteStmt.setString(5, session.getUserAgent());
                deleteStmt.setLong(6, session.getId());
                deleteStmt.executeUpdate();

                stmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            DbPool.quietRollback(conn);
            throw ServiceException.FAILURE("Failed to update ROPC session for " + session.getUsername(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    @Override
    public void delete(Account account, Long id, String username, String userAgent, String provider, String protocol,
                       String deviceId) throws ServiceException {
        if (id == null) {
            return;
        }
        DbConnection conn = null;
        PreparedStatement stmt = null;

        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);

            // delete only the exact session , matching the id!
            stmt = conn.prepareStatement(
                    "DELETE FROM " + getTableName(mbox) + " " + "WHERE id = ?");
            stmt.setLong(1, id);
            stmt.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            DbPool.quietRollback(conn);
            throw ServiceException.FAILURE("Failed to delete ROPC session for " + username, e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void deleteExpiredTokens() throws ServiceException {
        DbConnection conn = null;
        PreparedStatement findSchemasStmt = null;
        PreparedStatement deleteStmt = null;
        ResultSet resultSet = null;

        try {
            conn = DbPool.getConnection();
            long maxAgeMillis = TimeUnit.DAYS.toMillis(LC.mfa_idp_hard_reauth_in_days.intValue());
            if (maxAgeMillis <= 0) {
                maxAgeMillis = TimeUnit.DAYS.toMillis(HARD_EXPIRY_DEFAULT);
            }
            long expirationThresholdMillis = System.currentTimeMillis() - maxAgeMillis;
            int totalDeleted = 0;

            String findSchemasSql = "SELECT TABLE_SCHEMA FROM information_schema.TABLES WHERE TABLE_NAME = ?";
            findSchemasStmt = conn.prepareStatement(findSchemasSql);
            findSchemasStmt.setString(1, TABLE_ROPC_TOKEN_STORE);
            resultSet = findSchemasStmt.executeQuery();

            while (resultSet.next()) {
                try {
                    String schemaName = resultSet.getString("TABLE_SCHEMA");
                    String fullTableName = schemaName + "." + TABLE_ROPC_TOKEN_STORE;
                    // delete anything created before the threshold
                    deleteStmt = conn.prepareStatement(
                            "DELETE FROM " + fullTableName + " WHERE created_at <= ?");
                    deleteStmt.setLong(1, expirationThresholdMillis);
                    totalDeleted += deleteStmt.executeUpdate();

                } catch (SQLException e) {
                    ZimbraLog.account.error("Failed to execute cleanup of expired ROPC tokens", e);
                } finally {
                    DbPool.closeStatement(deleteStmt);
                }
            }
            conn.commit();

            if (totalDeleted > 0) {
                ZimbraLog.account.debug("Successfully purged %d expired ROPC sessions from the database", totalDeleted);
            }
        } catch (Exception e) {
            DbPool.quietRollback(conn);
            ZimbraLog.account.error("Failed to execute cleanup of expired ROPC tokens", e);
        } finally {
            DbPool.closeResults(resultSet);
            DbPool.closeStatement(findSchemasStmt);
            DbPool.quietClose(conn);
        }
    }

    @Override
    public void deleteByDeviceIdAndUsername(Account account, String deviceId) throws ServiceException {
        if (account == null || deviceId == null) {
            return;
        }
        DbConnection conn = null;
        PreparedStatement stmt = null;

        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            conn = DbPool.getConnection(mbox);

            stmt = conn.prepareStatement(
                    "DELETE FROM " + getTableName(mbox) + " " + "WHERE device_id = ? AND username = ?");
            stmt.setString(1, deviceId);
            stmt.setString(2, account.getName());
            stmt.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            DbPool.quietRollback(conn);
            throw ServiceException.FAILURE("Failed to delete ROPC session for " + account.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_ROPC_TOKEN_STORE);
    }
}
