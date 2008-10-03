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
package com.zimbra.cs.store.consistency;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;


public class ReportGenerator implements Runnable {

    // the cast will wrap to the correct signedness
    private final static byte[] GZIP_MAGIC = { 0x1f, (byte) 0x8b };
    private final boolean checkCompressed;
    private boolean skipBlobStore;
    private final String mysqlPasswd;
    private final File reportFile;
    private final int mailboxId;
    private final int mboxGroupId;
    final static String JDBC_URL = "jdbc:mysql://localhost:7306/";
    private final static char[] SPINNER = { '|', '/', '-', '\\' };
    
    /**
     * type IN (store, secondary-store).
     */
    private final static String VOLUME_QUERY =
            "SELECT id, path, file_bits, file_group_bits," +
            " mailbox_bits, mailbox_group_bits, compress_blobs" +
            " FROM volume WHERE type IN (1,2)";
    private final static String QUERY_FOR_FILE = "SELECT i.id" +
    		" FROM mail_item i LEFT OUTER JOIN revision r" +
    		" ON i.id = r.item_id AND i.mailbox_id = r.mailbox_id" +
    		" WHERE i.mailbox_id = ? AND i.id = ?" +
    		" AND (i.mod_content = ? OR r.mod_content = ?)";
    /**
     * has blob (blob_digest is not null)
     */
    private final static String ITEM_QUERY =
            "SELECT id, type, i.mailbox_id," +
            " i.volume_id, i.size, i.blob_digest, i.mod_content," +
            " r.version, r.size, r.volume_id, r.blob_digest, r.mod_content" +
            " FROM mail_item i LEFT OUTER JOIN revision r" +
            " ON i.id = r.item_id AND i.mailbox_id = r.mailbox_id" +
            " WHERE (i.blob_digest is not null or r.blob_digest is not null)" +
            " AND (i.volume_id is not null or r.volume_id is not null)";
    
    private final static String MBOX_ITEM_QUERY = ITEM_QUERY + " AND i.mailbox_id = ?";

    public ReportGenerator(String mysqlPasswd, File reportFile,
            boolean checkCompressed, boolean skipBlobStore,
            int mailboxId, int mboxGroupId) {
        this.mysqlPasswd = mysqlPasswd;
        this.reportFile = reportFile;
        this.checkCompressed = checkCompressed;
        this.skipBlobStore = skipBlobStore;
        this.mailboxId = mailboxId;
        this.mboxGroupId = mboxGroupId;
    }
    private Map<Byte,Volume> volumes;

    public void run() {
        List<ItemFault> faults = null;
        try {
            Connection c = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            File tmpFile = null;
            try {
                c = DriverManager.getConnection(JDBC_URL + "zimbra",
                        BlobConsistencyCheck.ZIMBRA_USER, mysqlPasswd);
                StatementExecutor e = new StatementExecutor(c);
                volumes = getVolumeInfo(e);
                
                List<Integer> mboxGroups = null;
                if (mailboxId == -1)
                    mboxGroups = getMailboxGroupList(e);
                c.close();

                tmpFile = File.createTempFile("mailitems", ".lst");
                out = new ObjectOutputStream(
                        new FileOutputStream(tmpFile, true));
                System.out.println("Spooling item list to " + tmpFile);
                int items = 0;

                if (mailboxId != -1 && mboxGroupId != -1) {
                    skipBlobStore = true; // TODO implement the blob store search as well?
                    String mboxgroup = "mboxgroup" + mboxGroupId;
                    System.out.println("Retrieving items from " +
                            mboxgroup + " mailbox " + mailboxId);
                    c = DriverManager.getConnection(JDBC_URL + mboxgroup,
                            BlobConsistencyCheck.ZIMBRA_USER, mysqlPasswd);
                    e = new StatementExecutor(c);
                    items += getMailItems(mailboxId, mboxGroupId, e, out);
                    c.close();
                } else {
                    for (int group : mboxGroups) {
                        String mboxgroup = "mboxgroup" + group;
                        System.out.println("Retrieving items from " + mboxgroup);
                        c = DriverManager.getConnection(JDBC_URL + mboxgroup,
                                BlobConsistencyCheck.ZIMBRA_USER, mysqlPasswd);
                        e = new StatementExecutor(c);
                        items += getMailItems(group, e, out);
                        c.close();
                    }
                }

                out.close();

                long start = System.currentTimeMillis();
                in = new ObjectInputStream(
                        new FileInputStream(tmpFile));

                c = DriverManager.getConnection(JDBC_URL + "zimbra",
                        BlobConsistencyCheck.ZIMBRA_USER, mysqlPasswd);
                e = new StatementExecutor(c);
                
                try {
                    System.err.print(" "); // skip blank space for spinner in cacheFileName
                    faults = determineFaults(volumes, in, items, e);
                    System.err.println();
                }
                catch (SQLException ex) {
                    ex.printStackTrace();
                    IOException ioe = new IOException();
                    ioe.initCause(ex);
                    throw ioe;
                }
                
                if (!skipBlobStore) {
                    System.err.print("Processing BLOB store\n ");
                    walkStore(volumes, faults, e);
                    System.err.println();
                }
                
                c.close();

                System.out.println(tmpFile + ": size " + tmpFile.length());
                long elapsed = System.currentTimeMillis() - start;
                System.out.printf("Processed %d items in %dms\n",
                        items, elapsed);
            }
            finally {
                if (c   != null) c.close();
                if (out != null) out.close();
                if (in  != null) in.close();

                if (tmpFile != null) tmpFile.delete();
            }

            saveAndDisplayReport(faults, volumes);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    private void walkStore(Map<Byte,Volume> volumes,
            List<ItemFault> faults, StatementExecutor e) throws SQLException {
        for (Volume volume : volumes.values()) {
            File f = new File(volume.path);
            if (!f.isDirectory()) {
                System.err.println(f + ": is not a directory!");
            } else {
                walkAndProcess(f, faults, e);
            }
        }
    }
    private Connection walkConnection = null;
    private StatementExecutor walkExecutor = null;
    private String walkMboxGroup = null;
    private void walkAndProcess(File dir,
            List<ItemFault> faults, StatementExecutor e) throws SQLException {
        File[] files = dir.listFiles();
        if (files == null) {
            System.err.println(dir + ": unable to list files");
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                walkAndProcess(f, faults, e);
            } else {
                System.err.print("\b" +
                        SPINNER[Math.abs(++spinnerCounter % SPINNER.length)]);
                String[] file = f.toString().split("/");
                
                Object r = null;
                if (file.length >= 5 && f.toString().endsWith(".msg")) {
                    // groupMask/mboxId/msg/itemMask/itemId-content.msg
                    String itemIdAndModContent = file[file.length - 1];
                    String mboxIdStr = file[file.length - 4];
                    int mboxId = Integer.parseInt(mboxIdStr);
                    int mboxGroup = mboxId % 100;
                    if (mboxGroup == 0) mboxGroup = 100;
                    itemIdAndModContent.substring(0, itemIdAndModContent.indexOf("."));
                    String itemIdStr = itemIdAndModContent.substring(
                            0, itemIdAndModContent.indexOf("-"));
                    String modContentStr = itemIdAndModContent.substring(
                            itemIdAndModContent.indexOf("-") + 1, itemIdAndModContent.indexOf("."));
                    int itemId = Integer.parseInt(itemIdStr);
                    int modContent = Integer.parseInt(modContentStr);
                    String mboxGroupName = "mboxgroup" + mboxGroup;
                    if (!mboxGroupName.equals(walkMboxGroup)) {
                        if (walkConnection != null)
                            walkConnection.close();
                        
                        walkConnection = DriverManager.getConnection(
                                JDBC_URL + mboxGroupName,
                                BlobConsistencyCheck.ZIMBRA_USER, mysqlPasswd);
                        walkMboxGroup = mboxGroupName;
                        walkExecutor = new StatementExecutor(walkConnection);
                    }
                    r = walkExecutor.query(QUERY_FOR_FILE, new Object[] {
                                    mboxId, itemId, modContent, modContent });

                }
                if (r == null) {
                    addAndPrintFault(faults, new ItemFault(null, null, null,
                            ItemFault.Code.NO_METADATA, (byte) 0, 0, f));
                }
            }
        }
    }
    private void addAndPrintFault(List<ItemFault> faults, ItemFault fault) {
        faults.add(fault);
        System.err.println();
        ReportDisplay.printFault(volumes, fault);
        System.err.print(" ");
    }
    
    private void saveAndDisplayReport(List<ItemFault> faults,
            Map<Byte,Volume> volumes) throws IOException {
        if (faults.size() == 0) {
            reportFile.delete();
            System.out.println("No inconsistencies found");
        } else {
            String inconsistency =
                    faults.size() == 1 ? " inconsistency" : " inconsistencies";
            System.out.println(faults.size() + inconsistency + " found");
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(
                        new FileOutputStream(reportFile));
                oos.writeObject(volumes);
                oos.writeInt(faults.size());
                int count = 0;
                for (ItemFault fault : faults) {
                    count++;
                    oos.writeObject(fault);
                    if (count % 10000 == 0)
                        oos.reset();
                }
            }
            finally {
                if (oos != null) oos.close();
            }

            faults = null; // allow gc since we're not leaving ReportGenerator prior to running ReportDisplay
            //new ReportDisplay(reportFile).run();
            System.out.println("Report saved to: " + reportFile);
        }
        
    }
    
    private int spinnerCounter = 0;
    private List<ItemFault> determineFaults(Map<Byte,Volume> volumes,
            ObjectInputStream in, int items, StatementExecutor e)
            throws IOException, SQLException {
        ArrayList<ItemFault> faults = new ArrayList<ItemFault>();
        try {
            for (int i = 0; i < items; i++) {
                System.err.print("\b" +
                        SPINNER[Math.abs(++spinnerCounter % SPINNER.length)]);
                Object o = in.readObject();
                Item item = (Item) o;

                Volume v = volumes.get(item.volumeId);
                if (v == null) {
                    addAndPrintFault(faults, new ItemFault(item, item, null,
                            ItemFault.Code.NOT_FOUND,
                            (byte) 0, 0, null));
                    continue;
                }
                File f = v.getItemFile(item);
                if (!f.exists()) {
                    boolean found = false;
                    byte foundId = 0;
                    for (Volume vol : volumes.values()) {
                        f = vol.getItemFile(item);
                        if (f.exists()) {
                            found = true;
                            foundId = vol.id;
                        }
                    }
                    if (found) {
                        addAndPrintFault(faults, new ItemFault(item, item, null,
                                ItemFault.Code.WRONG_VOLUME,
                                foundId, 0, null));
                    } else {
                        addAndPrintFault(faults, new ItemFault(item, item, null,
                                ItemFault.Code.NOT_FOUND,
                                (byte) 0, 0, null));
                    }
                } else if (f.length() != item.size) {
                    FileInputStream fin = null;
                    BufferedInputStream bin = null;
                    try {
                        fin = new FileInputStream(f);
                        bin = new BufferedInputStream(fin);
                        bin.mark(2);
                        byte[] magic = new byte[2];
                        bin.read(magic);
                        if (checkCompressed && Arrays.equals(GZIP_MAGIC, magic)) {
                            bin.reset();
                            GZIPInputStream gzin = null;
                            try {
                                gzin = new GZIPInputStream(bin);
                                int read = 0;
                                int len = 0;
                                byte[] buf = new byte[16384];
                                while ((read = gzin.read(buf)) != -1)
                                    len += read;
                                if (item.size != len) {
                                    addAndPrintFault(faults, new ItemFault(item, item, null,
                                            ItemFault.Code.WRONG_SIZE,
                                            (byte) 0, f.length(), null));
                                }
                            }
                            catch (IOException ioe) {
                                addAndPrintFault(faults, new ItemFault(item, item, null,
                                        ItemFault.Code.GZIP_CORRUPT,
                                        (byte) 0, f.length(), null));
                            }
                            finally {
                                if (gzin != null) gzin.close();
                            }
                        }
                        if (!Arrays.equals(GZIP_MAGIC, magic)) {
                            addAndPrintFault(faults, new ItemFault(item, item, null,
                                    ItemFault.Code.WRONG_SIZE,
                                    (byte) 0, f.length(), null));
                        }
                    }
                    finally {
                        if (bin != null) bin.close();
                        if (fin != null) fin.close();
                    }
                }

                for (Item.Revision rev : item.revisions) {
                    v = volumes.get(rev.volumeId);
                    f = v.getItemRevisionFile(item, rev);
                    if (!f.exists()) {
                        boolean found = false;
                        byte foundId = 0;
                        for (Volume vol : volumes.values()) {
                            f = vol.getItemRevisionFile(item, rev);
                            if (f.exists()) {
                                found = true;
                                foundId = vol.id;
                            }
                        }
                        if (found) {
                            addAndPrintFault(faults, new ItemFault(item, null, rev,
                                    ItemFault.Code.WRONG_VOLUME,
                                    foundId, 0, null));
                        } else {
                            addAndPrintFault(faults, new ItemFault(item, null, rev,
                                    ItemFault.Code.NOT_FOUND,
                                    (byte) 0, 0, null));
                        }
                    } else if (f.length() != rev.size) {
                        
                        // TODO refactor copy/paste code
                        FileInputStream fin = null;
                        BufferedInputStream bin = null;
                        try {
                            fin = new FileInputStream(f);
                            bin = new BufferedInputStream(fin);
                            bin.mark(2);
                            byte[] magic = new byte[2];
                            bin.read(magic);
                            if (checkCompressed && Arrays.equals(GZIP_MAGIC, magic)) {
                                bin.reset();
                                GZIPInputStream gzin = null;
                                try {
                                    gzin = new GZIPInputStream(bin);
                                    int read = 0;
                                    int len = 0;
                                    byte[] buf = new byte[16384];
                                    while ((read = gzin.read(buf)) != -1)
                                        len += read;
                                    if (item.size != len) {
                                        addAndPrintFault(faults, new ItemFault(item, null, rev,
                                                ItemFault.Code.WRONG_SIZE,
                                                (byte) 0, f.length(), null));
                                    }
                                }
                                catch (IOException ioe) {
                                    addAndPrintFault(faults, new ItemFault(item, null, rev,
                                            ItemFault.Code.GZIP_CORRUPT,
                                            (byte) 0, f.length(), null));
                                }
                                finally {
                                    if (gzin != null) gzin.close();
                                }
                            }
                            if (!Arrays.equals(GZIP_MAGIC, magic)) {
                                addAndPrintFault(faults, new ItemFault(item, null, rev,
                                        ItemFault.Code.WRONG_SIZE,
                                        (byte) 0, f.length(), null));
                            }
                        }
                        catch (IOException ex) {
                            addAndPrintFault(faults, new ItemFault(item, null, rev,
                                    ItemFault.Code.IO_EXCEPTION,
                                    (byte) 0, f.length(), null));
                        }
                        finally {
                            if (bin != null) bin.close();
                            if (fin != null) fin.close();
                        }
                    }
                }
            }
        }
        catch (ClassNotFoundException ex) {
            IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
        return faults;
        
    }

    private Map<Byte,Volume> getVolumeInfo(StatementExecutor e)
    throws SQLException {
        
        System.out.println("Retrieving volume information");
        final HashMap<Byte,Volume> m = new HashMap<Byte,Volume>();
        e.query(VOLUME_QUERY, new StatementExecutor.ObjectMapper() {
            public void mapRow(ResultSet rs) throws SQLException {
                byte id = rs.getByte("id");
                Volume v = new Volume(id,
                        rs.getString("path"),
                        rs.getShort("file_bits"),
                        rs.getShort("file_group_bits"),
                        rs.getShort("mailbox_bits"),
                        rs.getShort("mailbox_group_bits"),
                        rs.getBoolean("compress_blobs"));
                m.put(id, v);
            }
        });
        return m;
    }

    private List<Integer> getMailboxGroupList(StatementExecutor e)
    throws SQLException {
        System.out.println("Retrieving mboxgroup list");
        final ArrayList<Integer> l = new ArrayList<Integer>();
        e.query("SHOW DATABASES", new StatementExecutor.ObjectMapper() {
            public void mapRow(ResultSet rs) throws SQLException {
                String name = rs.getString(1);
                // add number only
                if (name != null && name.startsWith("mboxgroup"))
                    l.add(Integer.parseInt(name.substring(9)));
            }
        });
        Collections.sort(l);
        return l;
    }

    private int getMailItems(int group, StatementExecutor e,
            ObjectOutputStream oos)
    throws SQLException, IOException {
        return getMailItems(-1, group, e, oos);
    }
    private int getMailItems(final int mboxId, final int group,
            StatementExecutor e, final ObjectOutputStream oos)
    throws SQLException, IOException {
        // lazy, fake pointer
        final int[] countref = new int[1];
        final Item[] lastItem = new Item[1];

        StatementExecutor.ObjectMapper mapper = new StatementExecutor.ObjectMapper() {
            private void serialize(Item o) throws SQLException {
                Collections.sort(o.revisions);
                try {
                    oos.writeObject(o);
                    // help avoid OOME
                    if (countref[0] % 500 == 0)
                        oos.reset();
                    countref[0]++;
                }
                catch (IOException e) {
                    SQLException x = new SQLException();
                    x.initCause(e);
                    throw x;
                }
            }

            public void mapRow(ResultSet rs) throws SQLException {
                int id = rs.getInt("id");
                int mailboxId = rs.getInt("i.mailbox_id");

                if (lastItem[0] != null &&
                        lastItem[0].id == id && lastItem[0].mailboxId == mailboxId) {
                    Item.Revision rev = new Item.Revision(
                            rs.getInt("r.version"),
                            rs.getByte("r.volume_id"),
                            rs.getLong("r.size"),
                            rs.getString("r.blob_digest"),
                            rs.getInt("r.mod_content"));
                    lastItem[0].revisions.add(rev);
                } else {
                    // new item; serialize previous
                    if (lastItem[0] != null)
                        serialize(lastItem[0]);

                    Item item = new Item(id, group, mailboxId,
                            rs.getByte("type"),
                            rs.getByte("i.volume_id"),
                            rs.getLong("i.size"),
                            rs.getString("i.blob_digest"),
                            rs.getInt("i.mod_content"));
                    lastItem[0] = item;
                }


            }
        };
        if (mboxId == -1)
            e.query(ITEM_QUERY, mapper);
        else
            e.query(MBOX_ITEM_QUERY, new Object[] { mboxId }, mapper);

        if (lastItem[0] != null) {
            Collections.sort(lastItem[0].revisions);
            oos.writeObject(lastItem[0]);
            oos.reset();
            countref[0]++;
        }
        return countref[0];
    }

}
