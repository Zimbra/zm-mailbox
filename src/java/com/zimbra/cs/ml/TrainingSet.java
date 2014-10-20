package com.zimbra.cs.ml;

import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
/**
 *
 * @author iraykin
 *
 */
public class TrainingSet {

	private Mailbox mbox;
	private boolean checkedTable = false;
	private List<TrainingItem> loadedItems;

	public TrainingSet(Mailbox mbox) {
		this.mbox = mbox;
	}

	public static String getTableName(Mailbox mbox) {
		return getTableName(mbox.getId());
	}

	public static String getTableName(Integer mboxId) {
		return "training_data_" + String.valueOf(mboxId);
	}

	public void addItem(Integer itemId, InternalLabel label, DbConnection conn, Boolean overwrite) throws ServiceException {
		if (!checkedTable) {
			createTable(mbox.getId(), conn);
			checkedTable = true;
		}
		ZimbraLog.analytics.info("adding item " + String.valueOf(itemId) + " to training set with label " + label.toString());
		StringBuilder sql = new StringBuilder("INSERT")
		.append(overwrite? " INTO ": " IGNORE INTO ")
		.append(DbMailbox.qualifyTableName(mbox.getId(), getTableName(mbox)))
		.append(" (item_id, label, date) ")
		.append("VALUES (?, ?, ?)");
		if (overwrite) {
			sql.append(" ON DUPLICATE KEY UPDATE label=VALUES(label), date=VALUES(date)");
		}
		sql.append(";");
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			stmt.setInt(1, itemId);
			stmt.setInt(2, label.getId());
			stmt.setLong(3, System.currentTimeMillis());
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("error preparing update statement", e);
		} finally {
			conn.commit();
		}
	}

	public List<TrainingItem> getLastLoadedItems() {
		return loadedItems;
	}

	public List<TrainingItem> getItems(DbConnection conn, OperationContext octxt) throws ServiceException {
		StringBuilder sql = new StringBuilder("SELECT item_id, label")
		.append(" FROM ")
		.append(DbMailbox.qualifyTableName(mbox.getId(), getTableName(mbox)));
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			ResultSet rs = stmt.executeQuery();
			List<TrainingItem> trainingItems = new ArrayList<TrainingItem>();
			while (rs.next()) {
				Message message = (Message) mbox.getItemById(octxt, rs.getInt("item_id"), MailItem.Type.MESSAGE);
				InternalLabel label = InternalLabel.fromId(rs.getInt("label"));
				if (label != InternalLabel.UNCLASSIFIED) {
					TrainingItem trainingItem = new TrainingItem(message, label);
					trainingItems.add(trainingItem);
				}
			}
			loadedItems = trainingItems;
			return trainingItems;
		} catch (SQLException e) {
			throw ServiceException.FAILURE("failure getting training items", e);
		}
	}

	public static void createTable(Integer mboxId, DbConnection conn) throws ServiceException {
		String tableName = DbMailbox.qualifyTableName(mboxId, getTableName(mboxId));
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
		.append(tableName)
		.append( "(item_id INTEGER UNIQUE, label INTEGER, date BIGINT UNSIGNED)");
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("SQL error", e);
		} finally {
			conn.commit();
		}
	}

	public static void dropTable(Integer mboxId, DbConnection conn) throws ServiceException {
		StringBuilder sql = new StringBuilder("DROP TABLE ")
		.append(DbMailbox.qualifyTableName(mboxId, getTableName(mboxId)));
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("SQL error", e);
		} finally {
			conn.commit();
		}
	}

	public void removeItem(Integer itemId, DbConnection conn) throws ServiceException {
		if (!checkedTable) {
			createTable(mbox.getId(), conn);
			checkedTable = true;
		}
		ZimbraLog.analytics.info("deleting item " + String.valueOf(itemId) + " from training set");
		StringBuilder sql = new StringBuilder("DELETE FROM ")
		.append(DbMailbox.qualifyTableName(mbox.getId(), getTableName(mbox)))
		.append(" WHERE item_id=?");
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			stmt.setInt(1, itemId);
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("error preparing update statement", e);
		} finally {
			conn.commit();
		}
	}

	public void addItem(MailItem mailItem, InternalLabel label,
			DbConnection conn, boolean overwrite) throws ServiceException {
		if (mailItem instanceof Conversation) {
			List<Message> msgs = ((Conversation) mailItem).getMessages(SortBy.DATE_DESC, -1);
			for (Message msg: msgs) {
				addItem(msg.getId(), label, conn, overwrite);
			}
		} else {
			addItem(mailItem.getId(), label, conn, overwrite);
		}
	}

	public void removeItem(MailItem mailItem, DbConnection conn) throws ServiceException {
		if (mailItem instanceof Conversation) {
			List<Message> msgs = ((Conversation) mailItem).getMessages(SortBy.DATE_DESC, -1);
			for (Message msg: msgs) {
				removeItem(msg.getId(), conn);
			}
		} else {
			removeItem(mailItem.getId(), conn);
		}
	}

	public void deleteAllByLabel(InternalLabel label, DbConnection conn) throws ServiceException {
		StringBuilder sql = new StringBuilder("DELETE FROM ")
		.append(DbMailbox.qualifyTableName(mbox.getId(), getTableName(mbox)))
		.append(" WHERE label=?");
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			stmt.setInt(1, label.getId());
			stmt.executeUpdate();
			conn.commit();
			stmt.close();
			ZimbraLog.analytics.info(String.format("deleted all data with label %s from %s",
					label.toString(), getTableName(mbox.getId())));
		} catch (SQLException e) {
			throw ServiceException.FAILURE("error deleting data", e);
		} finally {
			conn.commit();
		}
	}

	public void outputStats(PrintStream out) throws ServiceException {
		DbConnection conn = DbPool.getConnection();
		StringBuilder sql = new StringBuilder("SELECT label, COUNT(*) FROM ")
		.append(DbMailbox.qualifyTableName(mbox.getId(), getTableName(mbox)))
		.append(" GROUP BY label");
		try {
			PreparedStatement stmt = conn.prepareStatement(sql.toString());
			ResultSet rs = stmt.executeQuery();
			out.println("-------------------------");
			out.println("- Training Data Summary -");
			out.println("-------------------------");
			while (rs.next()) {
				String label = InternalLabel.fromId(rs.getInt(1)).toString();
				Integer count = rs.getInt(2);
				out.println(String.format("%1$-22s", label + ":") + String.valueOf(count));
			}
			stmt.close();
		} catch (SQLException e) {
			throw ServiceException.FAILURE("error getting training set summary", e);
		}
	}
}
