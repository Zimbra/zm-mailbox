/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 31, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class Sync extends DocumentHandler {

    private static final int DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_ROOT;

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();

        long begin = request.getAttributeLong(MailService.A_TOKEN, 0);

        Element response = lc.createElement(MailService.SYNC_RESPONSE);
        response.addAttribute(MailService.A_CHANGE_DATE, System.currentTimeMillis() / 1000);
        synchronized (mbox) {
            mbox.beginTrackingSync();

            long token = mbox.getLastChangeID();
            response.addAttribute(MailService.A_TOKEN, token);
            if (begin <= 0) {
                Folder folder = mbox.getFolderById(octxt, DEFAULT_FOLDER_ID);
                if (folder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(DEFAULT_FOLDER_ID);
                initialFolderSync(lc, response, mbox, folder);
            } else
                deltaSync(lc, response, mbox, begin);
        }
        return response;
    }

    private void initialFolderSync(ZimbraContext lc, Element response, Mailbox mbox, Folder folder) throws ServiceException {
        boolean isSearch = folder instanceof SearchFolder;
        Mailbox.OperationContext octxt = lc.getOperationContext();
        Element f = ToXML.encodeFolder(response, lc, folder);
        if (!isSearch) {
            if (folder.getId() == Mailbox.ID_FOLDER_TAGS)
                initialTagSync(lc, f, mbox);
            else {
                initialItemSync(f, MailService.E_MSG, mbox.listItemIds(octxt, MailItem.TYPE_MESSAGE, folder.getId()));
                initialItemSync(f, MailService.E_CONTACT, mbox.listItemIds(octxt, MailItem.TYPE_CONTACT, folder.getId()));
                initialItemSync(f, MailService.E_NOTE, mbox.listItemIds(octxt, MailItem.TYPE_NOTE, folder.getId()));
                initialItemSync(f, MailService.E_APPOINTMENT, mbox.listItemIds(octxt, MailItem.TYPE_APPOINTMENT, folder.getId()));
            }
        } else {
            // anything else to be done for searchfolders?
        }

        List subfolders = folder.getSubfolders(octxt);
        if (subfolders != null)
            for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
                Folder subfolder = (Folder) it.next();
                if (subfolder != null)
                    initialFolderSync(lc, f, mbox, subfolder);
        }
    }

    private void initialTagSync(ZimbraContext lc, Element response, Mailbox mbox) throws ServiceException {
        List tags = mbox.getTagList(lc.getOperationContext());
        if (tags != null)
            for (Iterator it = tags.iterator(); it.hasNext(); ) {
                Tag tag = (Tag) it.next();
                if (tag != null && !(tag instanceof Flag))
                    ToXML.encodeTag(response, lc, tag);
            }
    }

    private void initialItemSync(Element f, String ename, int[] items) {
        if (items == null || items.length == 0)
            return;
        Element e = f.addElement(ename);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < items.length; i++)
            sb.append(i == 0 ? "" : ",").append(items[i]);
        e.addAttribute(MailService.A_IDS, sb.toString());
    }

    private static final byte[] SYNC_ORDER = new byte[] { 
        MailItem.TYPE_FOLDER, 
        MailItem.TYPE_TAG, 
        MailItem.TYPE_MESSAGE,
        MailItem.TYPE_CONTACT, 
        MailItem.TYPE_NOTE,
        MailItem.TYPE_APPOINTMENT,
    };
    
    private static final int MUTABLE_FIELDS = Change.MODIFIED_FLAGS  | Change.MODIFIED_TAGS |
                                              Change.MODIFIED_FOLDER | Change.MODIFIED_PARENT |
                                              Change.MODIFIED_NAME   | Change.MODIFIED_QUERY |
                                              Change.MODIFIED_COLOR  | Change.MODIFIED_POSITION |
                                              Change.MODIFIED_VIEW   | Change.MODIFIED_CONFLICT;

    private void deltaSync(ZimbraContext lc, Element response, Mailbox mbox, long begin) throws ServiceException {
        // first, handle deleted items
        String deleted = mbox.getTombstones(begin);
        if (deleted != null && !deleted.equals(""))
            response.addElement(MailService.E_DELETED).addAttribute(MailService.A_IDS, deleted);

        // now, handle created/modified items
        if (begin >= mbox.getLastChangeID())
            return;
        for (int i = 0; i < SYNC_ORDER.length; i++) {
            byte type = SYNC_ORDER[i];
            List changed = mbox.getModifiedItems(type, begin);
            for (Iterator it = changed.iterator(); it.hasNext(); ) {
                MailItem item = (MailItem) it.next();
                
                //
                // For items in the system, if the content has changed since the user last sync'ed 
                // (because it was edited or created), just send back the folder ID and saved date --
                // the client will request the whole object out of band -- potentially using the 
                // content servlet's "include metadata in headers" hack.
                //
                //  If it's just the metadata that changed, send back the set of mutable attributes.
                //
                if (item.getSavedSequence() > begin && type != MailItem.TYPE_FOLDER && type != MailItem.TYPE_TAG)
                    ToXML.encodeItem(response, lc, item, Change.MODIFIED_FOLDER | Change.MODIFIED_CONFLICT);
                else
                    ToXML.encodeItem(response, lc, item, MUTABLE_FIELDS);
            }
        }
    }
}