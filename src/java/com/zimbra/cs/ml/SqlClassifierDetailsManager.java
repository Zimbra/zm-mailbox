package com.zimbra.cs.ml;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
/**
 *
 * @author iraykin
 *
 */
public class SqlClassifierDetailsManager extends ClassifierDetailsManager {
	private String tableName;
	private boolean checkedTable;

	public SqlClassifierDetailsManager(Mailbox mbox) {
		super(mbox);
		this.tableName = DbMailbox.qualifyTableName(mbox.getId(),
				"classification_details_" + String.valueOf(mbox.getId()));
	}

	private void createIfNecessary(DbConnection conn) throws ServiceException {
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
		.append(tableName)
		.append( "(item_id INTEGER UNIQUE, reason TEXT);");
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(sql.toString());
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("could not check or create classification details table", e);
		} finally {
			conn.commit();
		}
	}

	public String getReason(Integer itemId, DbConnection conn) throws ServiceException {
		if (!checkedTable) {
			createIfNecessary(conn);
			checkedTable = true;
		}
		StringBuilder sql = new StringBuilder("SELECT reason FROM ")
		.append(tableName)
		.append(" WHERE item_id=?");
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(sql.toString());
			stmt.setInt(1, itemId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getString("reason");
			} else {
				return "Tagged Manually";
			}
		} catch (SQLException e) {
			throw ServiceException.FAILURE("could not get classification reason from table " + tableName, e);
		}
	}

	@Override
	public String getReason(Integer itemId) throws ServiceException {
		DbConnection conn = DbPool.getConnection();
		if (!checkedTable) {
			createIfNecessary(conn);
			checkedTable = true;
		}
		try {
			return getReason(itemId, conn);
		} finally {
			conn.close();
		}

	}

	public void setReason(Integer itemId, String reason, DbConnection conn) throws ServiceException {
		if (!checkedTable) {
			createIfNecessary(conn);
			checkedTable = true;
		}
		if (reason == null) {return; }
		StringBuilder sql = new StringBuilder("INSERT INTO ")
		.append(tableName)
		.append(" (item_id, reason) ")
		.append("VALUES (?, ?)")
		.append(" ON DUPLICATE KEY UPDATE reason=VALUES(reason);");
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(sql.toString());
			stmt.setInt(1, itemId);
			stmt.setString(2, reason);
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("could not set classification reason to table " + tableName, e);
		} finally {
			conn.commit();
		}
	}

	@Override
	public void setReason(Integer itemId, String reason)
			throws ServiceException {
		if (reason == null) {return; }
		DbConnection conn = DbPool.getConnection();
		setReason(itemId, reason, conn);
		conn.close();
	}

	public void deleteReason(Integer itemId, DbConnection conn) throws ServiceException {
		if (!checkedTable) {
			createIfNecessary(conn);
			checkedTable = true;
		}
		StringBuilder sql = new StringBuilder("DELETE FROM ")
		.append(tableName)
		.append(" WHERE item_id=?;");
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(sql.toString());
			stmt.setInt(1, itemId);
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("could not delete classification reason from table " + tableName, e);
		} finally {
			conn.commit();
		}
	}

	@Override
	public void deleteReason(Integer itemId) throws ServiceException {
		DbConnection conn = DbPool.getConnection();
		deleteReason(itemId, conn);
		conn.close();
	}

}
