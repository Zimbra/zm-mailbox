/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.db.DbBlobConsistency;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ExportAndDeleteItemsRequest;
import com.zimbra.soap.admin.type.ExportAndDeleteItemSpec;
import com.zimbra.soap.admin.type.ExportAndDeleteMailboxSpec;

public class ExportAndDeleteItems extends AdminDocumentHandler {

    @Override
    public Element handle(Element requst, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);

        // Parse request.
        ExportAndDeleteItemsRequest req = zsc.elementToJaxb(requst);
        ExportAndDeleteMailboxSpec mailbox = req.getMailbox();
        if (mailbox == null) {
            throw ServiceException.INVALID_REQUEST("empty mbox id", null);
        }
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailbox.getId());
        Multimap<Integer, Integer> idRevs = HashMultimap.create();
        for (ExportAndDeleteItemSpec item : mailbox.getItems()) {
            idRevs.put(item.getId(), item.getVersion());
        }
        String dirPath = req.getExportDir();
        String prefix = req.getExportFilenamePrefix();

        // Lock the mailbox, to make sure that another thread doesn't modify the items we're exporting/deleting.
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            DbConnection conn = null;

            try {
                conn = DbPool.getConnection();
                if (dirPath != null) {
                    File exportDir = new File(dirPath);
                    if (!exportDir.isDirectory()) {
                        DbPool.quietClose(conn);
                        throw ServiceException.INVALID_REQUEST(dirPath + " is not a directory", null);
                    }

                    String filePath = makePath(dirPath, DbMailItem.TABLE_MAIL_ITEM, prefix);
                    export(conn, mbox, DbMailItem.TABLE_MAIL_ITEM, "id", idRevs, filePath);
                    filePath = makePath(dirPath, DbMailItem.TABLE_MAIL_ITEM_DUMPSTER, prefix);
                    export(conn, mbox, DbMailItem.TABLE_MAIL_ITEM_DUMPSTER, "id", idRevs, filePath);

                    filePath = makePath(dirPath, DbMailItem.TABLE_REVISION, prefix);
                    export(conn, mbox, DbMailItem.TABLE_REVISION, "item_id", idRevs, filePath);
                    filePath = makePath(dirPath, DbMailItem.TABLE_REVISION_DUMPSTER, prefix);
                    export(conn, mbox, DbMailItem.TABLE_REVISION_DUMPSTER, "item_id", idRevs, filePath);

                    filePath = makePath(dirPath, DbMailItem.TABLE_APPOINTMENT, prefix);
                    export(conn, mbox, DbMailItem.TABLE_APPOINTMENT, "item_id", idRevs, filePath);
                    filePath = makePath(dirPath, DbMailItem.TABLE_APPOINTMENT_DUMPSTER, prefix);
                    export(conn, mbox, DbMailItem.TABLE_APPOINTMENT_DUMPSTER, "item_id", idRevs, filePath);
                }

                // delete item from mail_item and revision table
                for (Integer itemId : idRevs.keySet()) {
                    Collection<Integer> revs = idRevs.get(itemId);
                    for (int rev : revs) {
                        if (rev == 0) {
                            // delete all revisions to make sure we delete all blobs
                            List<MailItem> list = null;
                            try {
                                list = mbox.getAllRevisions(null, itemId, MailItem.Type.UNKNOWN);
                            } catch (NoSuchItemException ex) {
                                // exception happens when we try to delete a mail_item which is already in mail_item_dumpster
                                continue;
                            }

                            for (MailItem item : list) {
                                if (item.getType() == MailItem.Type.DOCUMENT) {
                                    mbox.purgeRevision(null, itemId, item.getVersion(), false);
                                }
                            }
                            mbox.delete(null, itemId, MailItem.Type.UNKNOWN, null);
                            break;
                        } else if (!revs.contains(0)) {
                            try {
                                mbox.purgeRevision(null, itemId, rev, false);
                            } catch (NoSuchItemException ex) {
                                // exception happens when we try to delete a revision which is already in revision_dumpster
                                continue;
                            }
                        }
                    }
                }
                // Delete items from mail_item_dumpster & revision_dumpster tables just
                // incase moved to dumpster tables
                DbBlobConsistency.delete(conn, mbox, idRevs);
            } finally {
                conn.commit();
                DbPool.quietClose(conn);
            }
        }

        return zsc.createElement(AdminConstants.EXPORT_AND_DELETE_ITEMS_RESPONSE);
    }

    private void export(DbConnection conn, Mailbox mbox, String tableName, String idColName,
                        Multimap<Integer, Integer> idRevs, String filePath)
    throws ServiceException {
        if (DbBlobConsistency.getNumRows(conn, mbox, tableName, idColName, idRevs) > 0) {
            DbBlobConsistency.export(conn, mbox, tableName, idColName, idRevs, filePath);
        }
    }

    String makePath(String dirPath, String tableName, String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        return dirPath + "/" + prefix + tableName + ".txt";
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
