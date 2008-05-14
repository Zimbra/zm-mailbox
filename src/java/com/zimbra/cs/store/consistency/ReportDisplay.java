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
import java.util.List;
import java.util.Map;

public class ReportDisplay implements Runnable {
    private final File reportFile;

    public ReportDisplay(File reportFile) {
        this.reportFile = reportFile;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        try {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(reportFile));
                Map<Byte,Volume> volumes = (Map) in.readObject();
                List<ItemFault> faults = (List) in.readObject();

                for (ItemFault fault : faults) {
                    printFault(volumes, fault);
                }
                String f = faults.size() == 1 ? " fault " : " faults ";
                System.out.println(" *** " + faults.size() + f + "found");
            }
            finally {
                if (in != null) in.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String getFaultMessage(
            Map<Byte,Volume> volumes, ItemFault fault) {
        String itemName = null;
        Volume v;
        File file = null;
        long size = -1;

        if (fault.faultCode != ItemFault.Code.NO_METADATA) {
            itemName = String.format(
                    " * MailboxGroup=%d, mailbox=%d, item=%d",
                    fault.item.group, fault.item.mailboxId, fault.item.id);
            if (fault.faultRevision != null) {
                itemName += ", version=" + fault.faultRevision.version;
                v = volumes.get(fault.faultRevision.volumeId);
                file = v.getItemRevisionFile(fault.item, fault.faultRevision);
                size = fault.faultRevision.size;
            } else {
                v = volumes.get(fault.item.volumeId);
                file = v.getItemFile(fault.item);
                size = fault.item.size;
            }
        }

        String msg = "UNKNOWN ERRORCODE: " + fault.faultCode;
        switch (fault.faultCode) {
        case NOT_FOUND:
            if (fault.faultItem != null && fault.item.revisions.size() > 0) {
                msg = String.format(": file not found: %s" +
                		" (revert to previous revision %d)",
                        file, fault.item.revisions.get(0));
            } else {
                msg = ": file not found: " + file +
                        " (delete associated metadata)";
            }
            break;
        case WRONG_VOLUME:
            Volume fv = volumes.get(fault.volumeId);
            File ff = fault.faultItem != null ?
                    fv.getItemFile(fault.faultItem) :
                    fv.getItemRevisionFile(fault.item, fault.faultRevision);
            msg = String.format(": wrong volume, expected at %s, found at %s" +
                    " (move to correct volume)", file, ff);
            break;
        case WRONG_SIZE:
            msg = String.format(": wrong size: expected %d, was %s: %s" +
                    " (no action, unrecoverable)", size, fault.size, file);
            break;
        case NO_METADATA:
            msg = String.format(" * %s: no associated metadata (delete blob)",
                    fault.faultFile);
        }
        return itemName != null ? itemName + msg : msg;
    }
    private static void printFault(Map<Byte,Volume> volumes, ItemFault fault) {
        System.out.println(getFaultMessage(volumes, fault));
    }
}
