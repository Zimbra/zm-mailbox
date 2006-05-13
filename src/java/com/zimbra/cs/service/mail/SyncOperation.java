/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Set;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.operation.Scheduler;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * This implementation is in service.mail b/c there is no intermediate (non-XML) format for
 * the sync response right now
 */
public class SyncOperation extends Operation {

    private static int LOAD = 2;
    static {
        Operation.Config c = loadConfig(SyncOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }

    private static final int DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_ROOT;
    private static enum SyncPhase { INITIAL, DELTA };

    ZimbraSoapContext mZsc;
    Element mRequest;
    Element mResponse;
    long mBegin;

    public SyncOperation(Session session, OperationContext oc, Mailbox mbox, 
                ZimbraSoapContext zsc, Element request, Element response, long begin) {
        super(session, oc, mbox, Requester.SOAP, Scheduler.Priority.BATCH, LOAD);

        mZsc = zsc;
        mRequest = request;
        mResponse = response;
        mBegin = begin;
    }

    protected void callback() throws ServiceException {
        Mailbox mbox = getMailbox();
        synchronized (mbox) {
            mbox.beginTrackingSync(null);

            mResponse.addAttribute(MailService.A_TOKEN, mbox.getLastChangeID());
            if (mBegin <= 0) {
                mResponse.addAttribute(MailService.A_SIZE, mbox.getSize());
                Folder root = null;
                try {
                    int folderId = (int) mRequest.getAttributeLong(MailService.A_FOLDER, DEFAULT_FOLDER_ID);
                    OperationContext octxtOwner = new OperationContext(mbox.getAccount());
                    root = mbox.getFolderById(octxtOwner, folderId);
                } catch (MailServiceException.NoSuchItemException nsie) { }

                Set<Folder> visible = mbox.getVisibleFolders(getOpCtxt());
                boolean anyFolders = folderSync(mZsc, mResponse, mbox, root, visible, SyncPhase.INITIAL);
                // if no folders are visible, add an empty "<folder/>" as a hint
                if (!anyFolders)
                    mResponse.addElement(MailService.E_FOLDER);
            } else
                deltaSync(mZsc, mResponse, mbox, mBegin);
        }
    }

    private boolean folderSync(ZimbraSoapContext zsc, Element response, Mailbox mbox, Folder folder, Set<Folder> visible, SyncPhase phase)
    throws ServiceException {
        if (folder == null)
            return false;
        if (visible != null && visible.isEmpty())
            return false;
        boolean isVisible = visible == null || visible.remove(folder);

        // short-circuit if we know that this won't be in the output
        List<Folder> subfolders = folder.getSubfolders(null);
        if (!isVisible && subfolders.isEmpty())
            return false;

        // write this folder's data to the response
        boolean initial = phase == SyncPhase.INITIAL;
        Element f = ToXML.encodeFolder(response, zsc, folder, initial ? ToXML.NOTIFY_FIELDS : Change.ALL_FIELDS);
        if (initial && isVisible) {
            // we're in the middle of an initial sync, so serialize the item ids
            boolean isSearch = folder instanceof SearchFolder;
            Mailbox.OperationContext octxt = zsc.getOperationContext();
            if (!isSearch) {
                if (folder.getId() == Mailbox.ID_FOLDER_TAGS)
                    initialTagSync(zsc, f, mbox);
                else {
                    initialItemSync(f, MailService.E_MSG, mbox.listItemIds(octxt, MailItem.TYPE_MESSAGE, folder.getId()));
                    initialItemSync(f, MailService.E_CONTACT, mbox.listItemIds(octxt, MailItem.TYPE_CONTACT, folder.getId()));
                    initialItemSync(f, MailService.E_NOTE, mbox.listItemIds(octxt, MailItem.TYPE_NOTE, folder.getId()));
                    initialItemSync(f, MailService.E_APPOINTMENT, mbox.listItemIds(octxt, MailItem.TYPE_APPOINTMENT, folder.getId()));
                }
            } else {
                // anything else to be done for searchfolders?
            }
        }

        if (isVisible && visible != null && visible.isEmpty())
            return true;

        // write the subfolders' data to the response
        for (Folder subfolder : subfolders)
            if (subfolder != null)
                isVisible |= folderSync(zsc, f, mbox, subfolder, visible, phase);

        // if this folder and all its subfolders are not visible (oops!), remove them from the response
        if (!isVisible)
            f.detach();

        return isVisible;
    }

    private void initialTagSync(ZimbraSoapContext zsc, Element response, Mailbox mbox) throws ServiceException {
        for (Tag tag : mbox.getTagList(zsc.getOperationContext()))
            if (tag != null && !(tag instanceof Flag))
                ToXML.encodeTag(response, zsc, tag);
    }

    private void initialItemSync(Element f, String ename, int[] items) {
        if (items == null || items.length == 0)
            return;
        Element e = f.addElement(ename);
        StringBuilder sb = new StringBuilder();
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
    Change.MODIFIED_NAME   | Change.MODIFIED_CONFLICT |
    Change.MODIFIED_COLOR  | Change.MODIFIED_POSITION;

    private void deltaSync(ZimbraSoapContext zsc, Element response, Mailbox mbox, long begin) throws ServiceException {
        if (begin >= mbox.getLastChangeID())
            return;

        // first, handle deleted items
        List<Integer> tombstones = mbox.getTombstones(begin);
        if (tombstones != null && !tombstones.isEmpty()) {
            StringBuilder deleted = new StringBuilder();
            for (Integer id : tombstones)
                deleted.append(deleted.equals("") ? "" : ",").append(id);
            response.addElement(MailService.E_DELETED).addAttribute(MailService.A_IDS, deleted.toString());
        }

        //  now, handle created/modified items
        OperationContext octxt = zsc.getOperationContext();
        for (byte type : SYNC_ORDER) {
            if (type == MailItem.TYPE_FOLDER && zsc.isDelegatedRequest()) {
                //  first, make sure that something changed...
                OperationContext octxtOwner = new OperationContext(mbox.getAccount());
                if (mbox.getModifiedItems(octxtOwner, type, begin).isEmpty())
                    continue;
                // special-case the folder hierarchy for delegated delta sync
                Set<Folder> visible = mbox.getVisibleFolders(octxt);
                boolean anyFolders = folderSync(zsc, response, mbox, mbox.getFolderById(null, DEFAULT_FOLDER_ID), visible, SyncPhase.DELTA);
                // if no folders are visible, add an empty "<folder/>" as a hint
                if (!anyFolders)
                    response.addElement(MailService.E_FOLDER);
                continue;
            }

            for (MailItem item : mbox.getModifiedItems(octxt, type, begin)) {
                //      For items in the system, if the content has changed since the user last sync'ed 
                //      (because it was edited or created), just send back the folder ID and saved date --
                //      the client will request the whole object out of band -- potentially using the 
                //      content servlet's "include metadata in headers" hack.

                //      If it's just the metadata that changed, send back the set of mutable attributes.

                boolean isMetadataOnly = type == MailItem.TYPE_FOLDER || type == MailItem.TYPE_TAG;
                if (!isMetadataOnly && item.getSavedSequence() > begin)
                    ToXML.encodeItem(response, zsc, item, Change.MODIFIED_FOLDER | Change.MODIFIED_CONFLICT);
                else
                    ToXML.encodeItem(response, zsc, item, isMetadataOnly ? Change.ALL_FIELDS : MUTABLE_FIELDS);
            }
        }
    }
}

