/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Formatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;


public class DbDataSource {

    public static class DataSourceItem {
		public int itemId;
		public String remoteId;
		public Metadata md;
		public DataSourceItem(int i, String r, Metadata m) { itemId = i; remoteId = r; md = m; }
	}
	
    public static final String TABLE_DATA_SOURCE_ITEM = "data_source_item";

    public static void addMapping(DataSource ds, DataSourceItem item) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
        Connection conn = null;
        PreparedStatement stmt = null;
        String dataSourceId = ds.getId();

        if (item.remoteId == null)
        	item.remoteId = "";
        
        ZimbraLog.datasource.debug("Adding mapping for dataSource %s: itemId(%d), remoteId(%s)", ds.getName(), item.itemId, item.remoteId);
        
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ");
            sb.append(getTableName(mbox));
            sb.append(" (");
            sb.append(DbMailItem.MAILBOX_ID);
            sb.append("data_source_id, item_id, remote_id, metadata) VALUES (");
            sb.append(DbMailItem.MAILBOX_ID_VALUE);
            sb.append("?, ?, ?, ?)");
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY)) {
            	sb.append(" ON DUPLICATE KEY UPDATE data_source_id = ?, item_id = ?, remote_id = ?, metadata = ?");
            }
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, dataSourceId);
            stmt.setInt(i++, item.itemId);
            stmt.setString(i++, item.remoteId);
            stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY)) {
                stmt.setString(i++, dataSourceId);
                stmt.setInt(i++, item.itemId);
                stmt.setString(i++, item.remoteId);
                stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
            }
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            if (!Db.supports(Db.Capability.ON_DUPLICATE_KEY) && Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                DbPool.closeStatement(stmt);
                DbPool.quietClose(conn);
            	updateMapping(ds, item);
            } else {
                throw ServiceException.FAILURE("Unable to add mapping for dataSource "+ds.getName(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void updateMapping(DataSource ds, DataSourceItem item) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
        Connection conn = null;
        PreparedStatement stmt = null;
        ZimbraLog.datasource.debug("Updating mapping for dataSource %s: itemId(%d), remoteId(%s)", ds.getName(), item.itemId, item.remoteId);
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(getTableName(mbox));
            sb.append(" SET remote_id = ?, metadata = ? WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append(" item_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            stmt.setString(i++, item.remoteId);
            stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setInt(i++, item.itemId);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to update mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void deleteMappings(DataSource ds, Collection<Integer> itemIds) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
        Connection conn = null;
        PreparedStatement stmt = null;
        ZimbraLog.datasource.debug("Deleting %d mappings for dataSource %s", itemIds.size(), ds.getName());
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append(" data_source_id = ? AND item_id IN ");
            sb.append(DbUtil.suitableNumberOfVariables(itemIds));
            stmt = conn.prepareStatement(sb.toString());
            
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            for (int itemId : itemIds)
            	stmt.setInt(i++, itemId);

            int numRows = stmt.executeUpdate();
            conn.commit();
            ZimbraLog.datasource.debug("Deleted %d mappings for %s", numRows, ds.getName());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void deleteAllMappings(DataSource ds) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
        Connection conn = null;
        PreparedStatement stmt = null;
        ZimbraLog.datasource.debug("Deleting all mappings for dataSource %s", ds.getName());
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append(" data_source_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            int numRows = stmt.executeUpdate();
            conn.commit();
            ZimbraLog.datasource.debug("Deleted %d mappings for %s", numRows, ds.getName());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static boolean hasMapping(DataSource ds, int itemId) throws ServiceException {
    	DataSourceItem item = getMapping(ds, itemId);
    	return item.remoteId != null;
    }
    
    public static Collection<DataSourceItem> getAllMappings(DataSource ds) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
    	ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        ZimbraLog.datasource.debug("Get all mappings for %s", ds.getName());
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, remote_id, metadata FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
            	Metadata md = null;
            	String buf = rs.getString(3);
            	if (buf != null)
            		md = new Metadata(buf);
            	items.add(new DataSourceItem(rs.getInt(1), rs.getString(2), md));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    	
    	return items;
    }
    
    public static Collection<DataSourceItem> getAllMappingsInFolder(DataSource ds, int folderId) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
    	ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        ZimbraLog.datasource.debug("Get all mappings for %s in folder %d", ds.getName(), folderId);
        try {
        	String thisTable = getTableName(mbox);
        	String mailItemTable = DbMailbox.qualifyTableName(mbox, "mail_item");
        	String IN_THIS_MAILBOX_AND = DebugConfig.disableMailboxGroups ? "" : thisTable+".mailbox_id = ? AND ";
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, remote_id, ").append(thisTable).append(".metadata FROM ");
            sb.append(thisTable);
            sb.append(" INNER JOIN ");
            sb.append(mailItemTable);
            sb.append(" ON  ").append(thisTable).append(".item_id = ").append(mailItemTable).append(".id");
            sb.append(" WHERE ");
            sb.append(IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND folder_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            stmt.setInt(i++, folderId);
            rs = stmt.executeQuery();
            while (rs.next()) {
            	Metadata md = null;
            	String buf = rs.getString(3);
            	if (buf != null)
            		md = new Metadata(buf);
            	items.add(new DataSourceItem(rs.getInt(1), rs.getString(2), md));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    	
    	return items;
    }

    public static Collection<DataSourceItem> getAllMappingsForRemoteIdPrefix(DataSource ds, int folderId, String prefix)
        throws ServiceException {
        Mailbox mbox = ds.getMailbox();
    	List<DataSourceItem> items = new ArrayList<DataSourceItem>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String db = DbMailbox.getDatabaseName(mbox);
        String dst = db + ".data_source_item";
        String mit = db + ".mail_item";
        try {
            conn = DbPool.getConnection();
            Formatter fmt = new Formatter();
            fmt.format("SELECT item_id, remote_id, %s.metadata FROM %s", dst, dst);
            fmt.format(" INNER JOIN %s ON %s.item_id = %s.id", mit, dst, mit);
            fmt.format(" WHERE %s.mailbox_id = ?", dst);
            fmt.format(" AND data_source_id = ? AND folder_id = ?");
            if (prefix != null) {
                fmt.format(" AND remote_id LIKE '%s%%'", prefix);
            }
            stmt = conn.prepareStatement(fmt.toString());
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, ds.getId());
            stmt.setInt(3, folderId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                String s = rs.getString(3);
                Metadata md = s != null ? new Metadata(s) : null;
                items.add(new DataSourceItem(rs.getInt(1), rs.getString(2), md));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for data source " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }
    
    public static DataSourceItem getMapping(DataSource ds, int itemId) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        String remoteId = null;
    	Metadata md = null;
    	
        ZimbraLog.datasource.debug("Get mapping for %s, itemId=%d", ds.getName(), itemId);
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT remote_id, metadata FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND item_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            stmt.setInt(i++, itemId);
            rs = stmt.executeQuery();
            if (rs.next()) {
            	remoteId = rs.getString(1);
            	String buf = rs.getString(2);
            	if (buf != null)
            		md = new Metadata(buf);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    	
    	return new DataSourceItem(itemId, remoteId, md);
    }

    public static DataSourceItem getReverseMapping(DataSource ds, String remoteId) throws ServiceException {
    	Mailbox mbox = ds.getMailbox();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        int itemId = 0;
        Metadata md = null;
    	
        ZimbraLog.datasource.debug("Get reverse mapping for %s, remoteId=%s", ds.getName(), remoteId);
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, metadata FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND remote_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            stmt.setString(i++, remoteId);
            rs = stmt.executeQuery();
            if (rs.next()) {
            	itemId = rs.getInt(1);
            	String buf = rs.getString(2);
            	if (buf != null)
            		md = new Metadata(buf);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get reverse mapping for dataSource "+ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    	
    	return new DataSourceItem(itemId, remoteId, md);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_DATA_SOURCE_ITEM);
    }
}
