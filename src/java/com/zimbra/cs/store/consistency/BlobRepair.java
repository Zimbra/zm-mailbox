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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class BlobRepair implements Runnable {
    private final String mysqlPasswd;
    private final File reportFile;

    private final static String JDBC_URL =
            "jdbc:mysql://localhost:7306/mboxgroup";
    private final static String WRONG_VOLUME_UPDATE_REV =
            "UPDATE revision SET volume_id = ?" +
            " WHERE mailbox_id = ? AND item_id = ?" +
            " AND version = ? AND volume_id = ?";
    private final static String WRONG_VOLUME_UPDATE_MB =
            "UPDATE mail_item SET volume_id = ?" +
            " WHERE mailbox_id = ? AND id = ? AND volume_id = ?";
    private final static String DELETE_MISSING_BLOB_MB =
            "DELETE FROM mail_item WHERE mailbox_id = ? AND id = ?";
    private final static String DELETE_MISSING_BLOB_REV =
            "DELETE FROM revision" +
            " WHERE mailbox_id = ? AND item_id = ? AND version = ?";

    public BlobRepair(String mysqlPasswd, File reportFile) {
        this.mysqlPasswd = mysqlPasswd;
        this.reportFile = reportFile;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        try {
            ObjectInputStream in = null;
            Connection c = null;
            try {
                in = new ObjectInputStream(new FileInputStream(reportFile));
                Map<Byte,Volume> volumes = (Map) in.readObject();
                List<ItemFault> faults = (List) in.readObject();
                int lastGroup = -1;
                for (ItemFault fault : faults) {
                    if (lastGroup != fault.item.group) {
                        if (c != null)
                            c.close();
                        c = DriverManager.getConnection(
                                JDBC_URL + fault.item.group,
                                BlobConsistencyCheck.ZIMBRA_USER, mysqlPasswd);
                    }
                    StatementExecutor e = new StatementExecutor(c);
                    String msg = ReportDisplay.getFaultMessage(volumes, fault);
                    boolean fixed = fixFault(e, volumes, fault);
                    String prefix = fixed ? "(FIXED) " : "(SKIPPED) ";
                    System.out.println(prefix + msg);
                    lastGroup = fault.item.group;
                }
            }
            finally {
                if (c  != null) c.close();
                if (in != null) in.close();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static boolean fixFault(StatementExecutor e,
            Map<Byte,Volume> volumes, ItemFault fault) throws SQLException {
        boolean isRev = fault.faultRevision != null;

        int updatedRows = 0;
        switch (fault.faultCode) {
        case NOT_FOUND:
            if (isRev) {
                updatedRows = e.update(DELETE_MISSING_BLOB_REV, new Object[] {
                    fault.item.mailboxId,
                    fault.item.id,
                    fault.faultRevision.version
                });
            } else {
                updatedRows = e.update(DELETE_MISSING_BLOB_MB, new Object[] {
                    fault.item.mailboxId, fault.item.id
                });
            }
            break;
        case WRONG_VOLUME:
            if (isRev) {
                updatedRows = e.update(WRONG_VOLUME_UPDATE_REV, new Object[] {
                    fault.volumeId,
                    fault.item.mailboxId,
                    fault.item.id,
                    fault.faultRevision.version,
                    fault.faultRevision.volumeId
                });
            } else {
                updatedRows = e.update(WRONG_VOLUME_UPDATE_MB, new Object[] {
                    fault.volumeId,
                    fault.item.mailboxId,
                    fault.item.id,
                    fault.item.volumeId
                });
            }
            break;
        case WRONG_SIZE:
            // we can't do anything about it
            break;
        }
        return updatedRows > 0;
    }
}
