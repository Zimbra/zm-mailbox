/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
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
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.store.file.Volume;

public class MetadataDump {

    private static final String OPT_MAILBOX_ID = "mailboxId";
    private static final String OPT_ITEM_ID = "itemId";
    private static final String OPT_FILE = "file";
    private static final String OPT_HELP = "h";

    private static Options sOptions = new Options();

    static {
        sOptions.addOption("m", OPT_MAILBOX_ID, true, "mailbox id");
        sOptions.addOption("i", OPT_ITEM_ID, true, "item id (required when --" + OPT_MAILBOX_ID + " is used)");
        sOptions.addOption("f", OPT_FILE, true, "Decode metadata value in a file (other options are ignored)");
        sOptions.addOption(OPT_HELP, "help", false, "Show help (this output)");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            System.err.println(errmsg);
        }
        System.err.println("Usage: zmmetadump -m <mailbox id/email> -i <item id>");
        System.err.println("   or: zmmetadump -f <file containing encoded metadata>");
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

    private static class Row {
        private Map<String, String> mMap = new LinkedHashMap<String, String>();

        public void addColumn(String colName, String value) {
            mMap.put(colName.toLowerCase(), value);
        }

        public Iterator<Entry<String, String>> iterator() {
            return mMap.entrySet().iterator();
        }

        public String get(String colName) {
            return mMap.get(colName.toLowerCase());
        }

        public void print(PrintStream ps) throws ServiceException {
            ps.println("[Database Columns]");
            for (Iterator<Entry<String, String>> iter = iterator(); iter.hasNext(); ) {
                Entry<String, String> entry = iter.next();
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
                short volId = Short.parseShort(mMap.get("volume_id"));
                Volume vol = Volume.getById(volId);
                if (vol != null) {
                    long mboxId = Long.parseLong(mMap.get("mailbox_id"));
                    String itemIdStr = mMap.get("id");
                    if (itemIdStr == null)
                        itemIdStr = mMap.get("item_id");
                    int itemId = Integer.parseInt(itemIdStr);
                    String dir = vol.getBlobDir(mboxId, itemId);
                    String modContent = mMap.get("mod_content");
                    String blobPath = dir + File.separator + itemIdStr + "-" + modContent + ".msg";
                    ps.println("[Blob Path]");
                    ps.println(blobPath);
                    ps.println();
                }
            }
            ps.println("[Metadata]");
            Metadata md = new Metadata(mMap.get(METADATA_COLUMN));
            ps.println(md.prettyPrint());
        }
    }

    private static long getMailboxGroup(Connection conn, long mboxId)
    throws SQLException {
        long gid = 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT group_id FROM mailbox WHERE id = ?");
            stmt.setLong(1, mboxId);
            rs = stmt.executeQuery();
            if (rs.next())
                gid = rs.getLong(1);
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
        return gid;
    }

    private static int lookupMailboxIdFromEmail(Connection conn, String email)
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

    private static Row getItemRow(Connection conn, long groupId, long mboxId, int itemId)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM " + DbMailItem.getMailItemTableName(mboxId, groupId, false) +
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

    private static List<Row> getRevisionRows(Connection conn, long groupId, long mboxId, int itemId)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM " + DbMailItem.getRevisionTableName(mboxId, groupId, false) +
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

    private static String loadFromFile(File file) throws ServiceException {
        try {
            long length = file.length();
            byte[] buf = new byte[(int) length];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                int bytesRead = fis.read(buf);
                if (bytesRead < length)
                    throw ServiceException.FAILURE(
                            "Read " + bytesRead + " bytes when expecting " + length +
                            " bytes, from file " + file.getAbsolutePath(), null);
                return new String(buf, "utf-8");
            } finally {
                if (fis != null)
                    fis.close();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while reading from " + file.getAbsolutePath(), e);
        }
    }

    private static void printBanner(PrintStream ps, String title) {
        ps.println("********************   " + title + "   ********************");
    }

    private static String getTimestampStr(long time) {
        DateFormat fmt = new SimpleDateFormat("EEE yyyy/MM/dd HH:mm:ss z");
        return fmt.format(time);
    }

    public static void main(String[] args) {
        try {
            CliUtil.toolSetup();
            long mboxId = 0;
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
                File file = new File(infileName);
                if (file.exists()) {
                    String encoded = loadFromFile(file);
                    Metadata md = new Metadata(encoded);
                    String pretty = md.prettyPrint();
                    out.println(pretty);
                    return;
                } else {
                    System.err.println("File " + infileName + " does not exist");
                    System.exit(1);
                }
            }
    
            // Get data from db.
            DbPool.startup();
            Connection conn = null;
    
            try {
                String mboxIdStr = cl.getOptionValue(OPT_MAILBOX_ID);
                String itemIdStr = cl.getOptionValue(OPT_ITEM_ID);
                if (mboxIdStr == null || itemIdStr == null) {
                    usage(null);
                    System.exit(1);
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
                long groupId = getMailboxGroup(conn, mboxId);
    
                boolean first = true;
    
                Row item = getItemRow(conn, groupId, mboxId, itemId);
                List<Row> revs = getRevisionRows(conn, groupId, mboxId, itemId);
    
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
