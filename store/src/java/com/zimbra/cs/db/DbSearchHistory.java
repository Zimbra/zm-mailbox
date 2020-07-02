package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.Db.Capability;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryEntry;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryMetadataParams;
import com.zimbra.cs.mailbox.Mailbox;

public final class DbSearchHistory {

    private static final String TABLE_SEARCHES = "searches";
    private static final String TABLE_SEARCH_HISTORY = "search_history";
    private final Mailbox mailbox;

    public DbSearchHistory(Mailbox mbox) {
        mailbox = mbox;
    }

    private String getSearchesTableName() {
        return DbMailbox.qualifyTableName(mailbox, TABLE_SEARCHES);
    }

    private String getSearchHistoryTableName() {
        return DbMailbox.qualifyTableName(mailbox, TABLE_SEARCH_HISTORY);
    }

    public void createNewSearch(DbConnection conn, int id, String searchString) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(getSearchesTableName())
                    .append("(").append(DbMailItem.MAILBOX_ID).append(" id, search)")
                    .append(" VALUES (").append(DbMailItem.MAILBOX_ID_VALUE).append("?, ?)");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setInt(pos++, id);
            stmt.setString(pos++, searchString);
            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE("failed to add search history entry", null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("Failed to create search history entry '%s' (id=%d)", searchString, id), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private int getSearchStringId(DbConnection conn, String searchString) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM " + getSearchesTableName() +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + " search=?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setString(pos++, searchString);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw ServiceException.NOT_FOUND(String.format("ID of search string '%s' not found in the database", searchString));
            } else {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error looking up ID of search string '%s'", searchString), e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    private void updateLastSearchDate(DbConnection conn, String searchString, int id, Timestamp timestamp) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sb = new StringBuilder("UPDATE ").append(getSearchesTableName())
                    .append(" SET last_search_date=? WHERE ")
                    .append(DbMailItem.IN_THIS_MAILBOX_AND)
                    .append(" id=?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            stmt.setTimestamp(pos++, timestamp);
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setInt(pos++, id);
            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE(String.format("failed to update last search time for '%s'", searchString), null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to update last search time for '%s'", searchString), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public void logSearch(DbConnection conn, String searchString, long timestampMillis) throws ServiceException {
        int id = getSearchStringId(conn, searchString);
        PreparedStatement stmt = null;
        try {
            //this operation involves two parts: adding a row to search_history and updating the latest search column in searches
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(getSearchHistoryTableName())
                    .append("(").append(DbMailItem.MAILBOX_ID).append(" search_id, date) VALUES (")
                    .append(DbMailItem.MAILBOX_ID_VALUE).append("?, ?)");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setInt(pos++, id);
            Timestamp timestamp = new Timestamp(timestampMillis);
            stmt.setTimestamp(pos++, timestamp);
            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE(String.format("failed to add search string '%s' to history", searchString), null);
            }
            updateLastSearchDate(conn, searchString, id, timestamp);
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to add search string '%s' to history", searchString), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public int getCount(DbConnection conn, String searchString, long maxAgeMillis) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int id = getSearchStringId(conn, searchString);
            StringBuilder sb = new StringBuilder("SELECT COUNT(search_id) FROM ").append(getSearchHistoryTableName()).append(" WHERE ")
                    .append(DbMailItem.IN_THIS_MAILBOX_AND).append(" search_id=?");
            if (maxAgeMillis > 0) {
                sb.append(" AND date>=?");
            }
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setInt(pos++, id);
            if (maxAgeMillis > 0) {
                stmt.setTimestamp(pos++, new Timestamp(System.currentTimeMillis() - maxAgeMillis));
            }
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw ServiceException.FAILURE(String.format("failed to get number of occurrences of '%s' in search history", searchString), null);
            } else {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to get number of occurrences of '%s' in search history", searchString), e);
        } catch (ServiceException se) {
            if (se.getCode().equals(ServiceException.NOT_FOUND)) {
                //id not found; search doesn't exist in history
                return 0;
            } else {
                throw se;
            }
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public boolean isRegistered(DbConnection conn, String searchString) throws ServiceException {
        try {
            getSearchStringId(conn, searchString);
            return true;
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.NOT_FOUND)) {
                return false;
            } else {
                throw e;
            }
        }
    }

    public List<SearchHistoryEntry> search(DbConnection conn, SearchHistoryMetadataParams params) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder sb = new StringBuilder("SELECT id, search FROM ").append(getSearchesTableName()).append(" WHERE ");
            if (params.hasIds() || params.hasMaxAge()) {
                sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
                boolean needAnd = false;
                if (params.getMaxAge() > 0) {
                    sb.append(" last_search_date>=?");
                    needAnd = true;
                }
                if (params.hasIds()) {
                    sb.append(needAnd ? " AND " : "");
                    sb.append(DbUtil.whereIn("id", params.getIds().size()));
                }
            } else {
                sb.append(DebugConfig.disableMailboxGroups ? "" : "mailbox_id = ? ");
            }
            if (!params.hasIds()) {
                //no need to sort/limit otherwise, since results will be re-sorted according to relevance
                sb.append(" ORDER BY last_search_date DESC ");
                if (params.getNumResults() > 0 && Db.supports(Capability.LIMIT_CLAUSE)) {
                    sb.append(" ").append(Db.getInstance().limit(params.getNumResults()));
                }
            }
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            if (params.getMaxAge() > 0) {
                stmt.setTimestamp(pos++, new Timestamp(System.currentTimeMillis() - params.getMaxAge()));
            }
            if (params.hasIds()) {
                for (int id: params.getIds()) {
                    stmt.setInt(pos++, id);
                }
            }
            rs = stmt.executeQuery();
            List<SearchHistoryEntry> results = new ArrayList<SearchHistoryEntry>();
            while (rs.next()) {
                SearchHistoryEntry entry = new SearchHistoryEntry(rs.getInt(1), rs.getString(2));
                results.add(entry);
            }
            return results;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("failed to search in search history", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    private List<Integer> getIdsToDelete(DbConnection conn, Timestamp timestamp) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Integer> results;
        StringBuilder sb = new StringBuilder("SELECT id FROM ").append(getSearchesTableName())
                .append(" WHERE ")
                .append(DbMailItem.IN_THIS_MAILBOX_AND)
                .append(" last_search_date < ?");
        try {
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setTimestamp(pos++, timestamp);
            rs = stmt.executeQuery();
            results = new ArrayList<Integer>();
            while (rs.next()) {
                results.add(rs.getInt(1));
            }
            return results;
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    private void deleteFromSearchesTable(DbConnection conn, Timestamp timestamp) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sb = new StringBuilder("DELETE FROM ").append(getSearchesTableName())
                    .append(" WHERE ")
                    .append(DbMailItem.IN_THIS_MAILBOX_AND)
                    .append(" last_search_date < ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setTimestamp(pos++, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to delete search history entries with max age %sms", timestamp.getTime()), e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    private void deleteFromSearchHistoryTable(DbConnection conn, Timestamp timestamp) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sb = new StringBuilder("DELETE FROM ").append(getSearchHistoryTableName())
                    .append(" WHERE ")
                    .append(DbMailItem.IN_THIS_MAILBOX_AND)
                    .append(" date < ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setTimestamp(pos++, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to delete search history entries with max age %sms", timestamp.getTime()), e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public Collection<Integer> delete(DbConnection conn, long maxAgeMillis) throws ServiceException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - maxAgeMillis);
        List<Integer> toDelete;
        try {
            toDelete = getIdsToDelete(conn, timestamp);
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to find search history entries with last search older than %sms", maxAgeMillis), e);
        }
        deleteFromSearchesTable(conn, timestamp);
        deleteFromSearchHistoryTable(conn, timestamp);
        return toDelete;
    }

    public void deleteAll(DbConnection conn) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sb = new StringBuilder("DELETE FROM ").append(getSearchesTableName())
                    .append(DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to delete search history for mailbox %d", mailbox.getId()), e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public SavedSearchStatus getSavedSearchStatus(DbConnection conn, String searchString) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder sb = new StringBuilder("SELECT status FROM ").append(getSearchesTableName())
                    .append(" WHERE ")
                    .append(DbMailItem.IN_THIS_MAILBOX_AND)
                    .append(" search = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setString(pos++, searchString);
            rs = stmt.executeQuery();
            if(!rs.next()) {
                return SavedSearchStatus.NOT_PROMPTED; //if search isn't registered, no need to throw error
            } else {
                return SavedSearchStatus.of(rs.getShort(1));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to updated prompt status for search string '%s' in mailbox %s", searchString, mailbox.getId()), e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public void setSavedSearchStatus(DbConnection conn, String searchString, SavedSearchStatus status) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sb = new StringBuilder("UPDATE ").append(getSearchesTableName())
                    .append(" SET status = ? WHERE ")
                    .append(DbMailItem.IN_THIS_MAILBOX_AND)
                    .append(" search = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            stmt.setShort(pos++, status.getId());
            pos = DbMailItem.setMailboxId(stmt, mailbox, pos);
            stmt.setString(pos++, searchString);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("failed to update prompt status for search string '%s' in mailbox %s", searchString, mailbox.getId()), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
}
