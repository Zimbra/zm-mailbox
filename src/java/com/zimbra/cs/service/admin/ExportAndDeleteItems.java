/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.db.DbBlobConsistency;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailItem;
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
        List<Integer> itemIds = new ArrayList<Integer>();
        for (Element itemEl : mboxEl.listElements(AdminConstants.E_ITEM)) {
            itemIds.add((int) itemEl.getAttributeLong(AdminConstants.A_ID));
        }
        String dirPath = request.getAttribute(AdminConstants.A_EXPORT_DIR, null);
        String prefix = request.getAttribute(AdminConstants.A_EXPORT_FILENAME_PREFIX, null);

        // Synchronize on the mailbox, to make sure that another thread doesn't
        // modify the items we're exporting/deleting.
        
        synchronized (mbox) {
            // Export items to SQL files.
            if (dirPath != null) {
                File exportDir = new File(dirPath);
                if (!exportDir.isDirectory()) {
                    throw ServiceException.INVALID_REQUEST(dirPath + " is not a directory", null);
                }

                Connection conn = null;
                try {
                    conn = DbPool.getConnection();
                    String filePath = makePath(dirPath, DbMailItem.TABLE_MAIL_ITEM, prefix);
                    export(conn, mbox, DbMailItem.TABLE_MAIL_ITEM, "id", itemIds, filePath);
                    filePath = makePath(dirPath, DbMailItem.TABLE_MAIL_ITEM, prefix);
                    export(conn, mbox, DbMailItem.TABLE_MAIL_ITEM_DUMPSTER, "id", itemIds, filePath);

                    filePath = makePath(dirPath, DbMailItem.TABLE_REVISION, prefix);
                    export(conn, mbox, DbMailItem.TABLE_REVISION, "item_id", itemIds, filePath);
                    filePath = makePath(dirPath, DbMailItem.TABLE_REVISION, prefix);
                    export(conn, mbox, DbMailItem.TABLE_REVISION_DUMPSTER, "item_id", itemIds, filePath);

                    filePath = makePath(dirPath, DbMailItem.TABLE_APPOINTMENT, prefix);
                    export(conn, mbox, DbMailItem.TABLE_APPOINTMENT, "item_id", itemIds, filePath);
                    filePath = makePath(dirPath, DbMailItem.TABLE_APPOINTMENT, prefix);
                    export(conn, mbox, DbMailItem.TABLE_APPOINTMENT_DUMPSTER, "item_id", itemIds, filePath);
                } finally {
                    DbPool.quietClose(conn);
                }
            }

            // Delete items.
            int[] idArray = new int[itemIds.size()];
            for (int i = 0; i < itemIds.size(); i++) {
                idArray[i] = itemIds.get(i);
            }
            mbox.delete(null, idArray, MailItem.TYPE_UNKNOWN, null);
        }
        
        return zsc.createElement(AdminConstants.EXPORT_AND_DELETE_ITEMS_RESPONSE);
    }
    
    private void export(Connection conn, Mailbox mbox, String tableName, String idColName,
                        Collection<Integer> itemIds, String filePath)
    throws ServiceException {
        if (DbBlobConsistency.getNumRows(conn, mbox, tableName, idColName, itemIds) > 0) {
            DbBlobConsistency.export(conn, mbox, tableName, idColName, itemIds, filePath);
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
