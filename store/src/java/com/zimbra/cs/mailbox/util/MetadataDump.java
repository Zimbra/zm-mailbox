/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox.util;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

public final class MetadataDump {

    private static final String OPT_MAILBOX_ID = "mailboxId";
    private static final String OPT_ITEM_ID = "itemId";
    private static final String OPT_DUMPSTER = "dumpster";
    private static final String OPT_FILE = "file";
    private static final String OPT_HELP = "h";
    private static final String OPT_STR = "String";
    public static final String DB_COLS_HDR = "[Database Columns]";
    public static final String METADATA_HDR = "[Metadata]";
    public static final String BLOBPATH_HDR = "[Blob Path]";

    private static Options sOptions = new Options();

    static {
        sOptions.addOption("m", OPT_MAILBOX_ID, true, "mailbox id or email");
        sOptions.addOption("i", OPT_ITEM_ID, true, "item id (required when --" + OPT_MAILBOX_ID + " is used)");
        sOptions.addOption(null, OPT_DUMPSTER, false, "Get data from the dumpster");
        sOptions.addOption("f", OPT_FILE, true, "Decode metadata value in a file (other options are ignored)");
        sOptions.addOption("s", OPT_STR, true, "Decode metadata value from a string (other options are ignored)");
        sOptions.addOption(OPT_HELP, "help", false, "Show help (this output)");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            System.err.println(errmsg);
        }
        System.err.println("Usage: zmmetadump -m <mailbox id/email> -i <item id> [--dumpster]");
        System.err.println("   or: zmmetadump -f <file containing encoded metadata>");
        System.err.println("   or: zmmetadump -s <encoded string>");
    }

    private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(sOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
            System.exit(1);
        }
        return cl;
    }

    private static final String METADATA_COLUMN = "metadata";

    private static class Row implements Iterable<Entry<String, String>> {
        private final Map<String, String> mMap = new LinkedHashMap<String, String>();

        Row()  { }

        void addColumn(String colName, String data) throws ServiceException {
            String key = colName.toLowerCase();
            String value = key.equals(METADATA_COLUMN) ? DbMailItem.decodeMetadata(data) : data;
            mMap.put(key, value);
        }

        @Override public Iterator<Entry<String, String>> iterator() {
            return mMap.entrySet().iterator();
        }

        String get(String colName) {
            return mMap.get(colName.toLowerCase());
        }

        void print(PrintStream ps) throws ServiceException {
            ps.println(DB_COLS_HDR);
            for (Entry<String, String> entry : this) {
                String col = entry.getKey();
                if (!col.equalsIgnoreCase(METADATA_COLUMN)) {
                    String val = entry.getValue();
                    if (col.equalsIgnoreCase("date") || col.equalsIgnoreCase("change_date")) {
                        if (val != null) {
                            long t = Long.parseLong(val) * 1000;
                            val += " (" + getTimestampStr(t) + ")";
                        }
                    }
                    ps.println("  " + col + ": " + (val != null ? val : "<null>"));
                }
            }
            ps.println();
            if (mMap.get("blob_digest") != null) {
                Volume vol = null;
                try {
                    short volId = Short.parseShort(mMap.get("locator"));
                    vol = VolumeManager.getInstance().getVolume(volId);
                } catch (NumberFormatException nfe) {
                    //probably not FileBlobStore
                }
                if (vol != null) {
                    int mboxId = Integer.parseInt(mMap.get("mailbox_id"));
                    String itemIdStr = mMap.get("id");
                    if (itemIdStr == null)
                        itemIdStr = mMap.get("item_id");
                    int itemId = Integer.parseInt(itemIdStr);
                    String dir = vol.getBlobDir(mboxId, itemId);
                    String modContent = mMap.get("mod_content");
                    String blobPath = dir + FileSystems.getDefault().getSeparator() + itemIdStr + "-" + modContent + ".msg";
                    ps.println(BLOBPATH_HDR);
                    ps.println(blobPath);
                    ps.println();
                }
            }
            ps.println(METADATA_HDR);
            Metadata md = new Metadata(mMap.get(METADATA_COLUMN));
            ps.println(md.prettyPrint());
        }
    }

    private static int getMailboxGroup(DbConnection conn, int mboxId)
    throws SQLException {
        int gid = 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT group_id FROM mailbox WHERE id = ?");
            stmt.setInt(1, mboxId);
            rs = stmt.executeQuery();
            if (rs.next())
                gid = rs.getInt(1);
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
        return gid;
    }

    private static int lookupMailboxIdFromEmail(DbConnection conn, String email)
    throws SQLException, ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT id FROM mailbox WHERE comment=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email.toUpperCase());
            rs = stmt.executeQuery();
            if (!rs.next())
                throw ServiceException.INVALID_REQUEST("Account " + email + " not found on this host", null);
            return rs.getInt(1);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static Row getItemRow(DbConnection conn, int groupId, int mboxId, int itemId, boolean fromDumpster)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM " + DbMailItem.getMailItemTableName(groupId, fromDumpster) +
                         " WHERE mailbox_id = " + mboxId + " AND id = " + itemId;
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            if (!rs.next())
                throw ServiceException.INVALID_REQUEST(
                        "No such item: mbox=" + mboxId + ", item=" + itemId,
                        null);
            Row row = new Row();
            ResultSetMetaData rsMeta = rs.getMetaData();
            int cols = rsMeta.getColumnCount();
            for (int i = 1; i <= cols ; i++) {
                String colName = rsMeta.getColumnName(i);
                String colValue = rs.getString(i);
                if (rs.wasNull())
                    colValue = null;
                row.addColumn(colName, colValue);
            }
            return row;
        } catch (SQLException e) {
            throw ServiceException.INVALID_REQUEST("No such item: mbox=" + mboxId + ", item=" + itemId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static List<Row> getRevisionRows(DbConnection conn, int groupId, int mboxId, int itemId, boolean fromDumpster)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM " + DbMailItem.getRevisionTableName(groupId, fromDumpster) +
                         " WHERE mailbox_id = " + mboxId + " AND item_id = " + itemId +
                         " ORDER BY mailbox_id, item_id, version DESC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            List<Row> rows = new ArrayList<Row>();
            while (rs.next()) {
                Row row = new Row();
                ResultSetMetaData rsMeta = rs.getMetaData();
                int cols = rsMeta.getColumnCount();
                for (int i = 1; i <= cols ; i++) {
                    String colName = rsMeta.getColumnName(i);
                    String colValue = rs.getString(i);
                    if (rs.wasNull())
                        colValue = null;
                    row.addColumn(colName, colValue);
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw ServiceException.INVALID_REQUEST("No such item: mbox=" + mboxId + ", item=" + itemId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static String loadFromFile(Path filepath) throws ServiceException {
        try {
            return new String(Files.readAllBytes(filepath));
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while reading from " + filepath.toAbsolutePath().toString(), e);
        }
    }

    private static void printBanner(PrintStream out, String title) {
        out.println("********************   " + title + "   ********************");
    }

    static String getTimestampStr(long time) {
        DateFormat fmt = new SimpleDateFormat("EEE yyyy/MM/dd HH:mm:ss z");
        return fmt.format(time);
    }

    public static void doDump(DbConnection conn, int mboxId, int itemId, boolean fromDumpster, PrintStream out) throws ServiceException {
        try {
            int groupId = getMailboxGroup(conn, mboxId);

            boolean first = true;

            Row item = getItemRow(conn, groupId, mboxId, itemId, fromDumpster);
            List<Row> revs = getRevisionRows(conn, groupId, mboxId, itemId, fromDumpster);

            // main item
            if (!revs.isEmpty())
                printBanner(out, "Current Revision");
            item.print(out);
            first = false;

            // revisions
            for (Row rev : revs) {
                String version = rev.get("version");
                if (!first) {
                    out.println();
                    out.println();
                }
                printBanner(out, "Revision " + version);
                rev.print(out);
                first = false;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("error while getting metadata for item " + itemId + " in mailbox " + mboxId, e);
        }
    }

    public static void main(String[] args) {
        try {
            CliUtil.toolSetup("WARN");
            int mboxId = 0;
            int itemId = 0;

            PrintStream out = new PrintStream(System.out, true, "utf-8");

            CommandLine cl = parseArgs(args);
            if (cl.hasOption(OPT_HELP)) {
                usage(null);
                System.exit(0);
            }

            // Get data from file.
            String infileName = cl.getOptionValue(OPT_FILE);
            if (infileName != null) {
                Path filepath = FileSystems.getDefault().getPath(infileName);
                if (Files.exists(filepath)) {
                    String encoded = loadFromFile(filepath);
                    Metadata md = new Metadata(encoded);
                    String pretty = md.prettyPrint();
                    out.println(pretty);
                    return;
                } else {
                    System.err.println("File " + infileName + " does not exist");
                    System.exit(1);
                }
            }

            // Get data from input String.
            String encoded = cl.getOptionValue(OPT_STR);
            if (!StringUtil.isNullOrEmpty(encoded)) {
                Metadata md = new Metadata(encoded);
                String pretty = md.prettyPrint();
                out.println(pretty);
                return;
            }

            // Get data from db.
            DbPool.startup();
            DbConnection conn = null;

            try {
                boolean fromDumpster = cl.hasOption(OPT_DUMPSTER);
                String mboxIdStr = cl.getOptionValue(OPT_MAILBOX_ID);
                String itemIdStr = cl.getOptionValue(OPT_ITEM_ID);
                if (mboxIdStr == null || itemIdStr == null) {
                    usage(null);
                    System.exit(1);
                    return;
                }
                if (mboxIdStr.matches("\\d+")) {
                    try {
                        mboxId = Integer.parseInt(mboxIdStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid mailbox id " + mboxIdStr);
                        System.exit(1);
                    }
                } else {
                    conn = DbPool.getConnection();
                    mboxId = lookupMailboxIdFromEmail(conn, mboxIdStr);
                }
                try {
                    itemId = Integer.parseInt(itemIdStr);
                } catch (NumberFormatException e) {
                    usage(null);
                    System.exit(1);
                }

                if (conn == null)
                    conn = DbPool.getConnection();
                doDump(conn, mboxId, itemId, fromDumpster, out);
            } finally {
                DbPool.quietClose(conn);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println();
            e.printStackTrace();
            System.exit(1);
        }
    }
}
