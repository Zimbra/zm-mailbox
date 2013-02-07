/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 VMware, Inc.
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

package com.zimbra.cs.service.admin;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.db.DbBlobConsistency;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

public class ExportAndDeleteItems extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);

        // Parse request.
        Element mboxEl = request.getElement(AdminConstants.E_MAILBOX);
        int mboxId = (int) mboxEl.getAttributeLong(AdminConstants.A_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        if (mbox == null) {
            throw ServiceException.INVALID_REQUEST("empty mbox id", null);
        }
        Multimap<Integer, Integer> idRevs = HashMultimap.create();
        for (Element itemEl : mboxEl.listElements(AdminConstants.E_ITEM)) {
            idRevs.put((int) itemEl.getAttributeLong(AdminConstants.A_ID), (int) itemEl.getAttributeLong(AdminConstants.A_VERSION_INFO_VERSION));
        }
        String dirPath = request.getAttribute(AdminConstants.A_EXPORT_DIR, null);
        String prefix = request.getAttribute(AdminConstants.A_EXPORT_FILENAME_PREFIX, null);

        // Synchronize on the mailbox, to make sure that another thread doesn't
        // modify the items we're exporting/deleting.
       
        synchronized (mbox) {
        	Connection conn = null;

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
                                list = mbox.getAllRevisions(null, itemId, MailItem.TYPE_UNKNOWN);
                            } catch (NoSuchItemException ex) {
                                // exception happens when we try to delete a mail_item which is already in mail_item_dumpster
                                continue;
                            }

                            for (MailItem item : list) {
                                if (item.getType() == MailItem.TYPE_DOCUMENT) {
                                    mbox.purgeRevision(null, itemId, item.getVersion(), false);
                                }
                            }
                            mbox.delete(null, itemId, MailItem.TYPE_UNKNOWN, null);
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
    
    private void export(Connection conn, Mailbox mbox, String tableName, String idColName,
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
