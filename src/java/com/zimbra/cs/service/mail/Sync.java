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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.soap.ZimbraSoapContext;

public class Sync extends MailDocumentHandler {

	protected static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
	protected String[] getProxiedIdPath(Element request)  { return TARGET_FOLDER_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Mailbox.OperationContext octxt = zsc.getOperationContext();

        String token = request.getAttribute(MailConstants.A_TOKEN, "0");

        Element response = zsc.createElement(MailConstants.SYNC_RESPONSE);
        response.addAttribute(MailConstants.A_CHANGE_DATE, System.currentTimeMillis() / 1000);
        
        int tokenInt;
        int itemCutoff;
        try {
            int delimiter = token.indexOf('-');
            if (delimiter < 1) {
                tokenInt = Integer.parseInt(token);
                itemCutoff = -1;
            } else {
                tokenInt = Integer.parseInt(token.substring(0, delimiter));
                itemCutoff = Integer.parseInt(token.substring(delimiter + 1));
            }
        } catch (NumberFormatException nfe) {
            throw ServiceException.INVALID_REQUEST("malformed sync token: " + token, nfe);
        }
        
        synchronized (mbox) {
            mbox.beginTrackingSync();

            if (tokenInt <= 0) {
                response.addAttribute(MailConstants.A_TOKEN, mbox.getLastChangeID());
                response.addAttribute(MailConstants.A_SIZE, mbox.getSize());
                Folder root = null;
                try {
                    ItemId iidFolder = new ItemId(request.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER_ID + ""), zsc);
                    OperationContext octxtOwner = new OperationContext(mbox.getAccount());
                    root = mbox.getFolderById(octxtOwner, iidFolder.getId());
                } catch (MailServiceException.NoSuchItemException nsie) { }

                Set<Folder> visible = mbox.getVisibleFolders(octxt);
                boolean anyFolders = folderSync(zsc, response, mbox, root, visible, SyncPhase.INITIAL);
                // if no folders are visible, add an empty "<folder/>" as a hint
                if (!anyFolders)
                    response.addElement(MailConstants.E_FOLDER);
            } else {
                boolean typedDeletes = request.getAttributeBool(MailConstants.A_TYPED_DELETES, false);
                String newToken = deltaSync(zsc, response, mbox, tokenInt, typedDeletes, itemCutoff);
                response.addAttribute(MailConstants.A_TOKEN, newToken);
            }
        }
        
        return response;
    }
    
    private static final int DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_ROOT;
    private static enum SyncPhase { INITIAL, DELTA };

    private static boolean folderSync(ZimbraSoapContext zsc, Element response, Mailbox mbox, Folder folder, Set<Folder> visible, SyncPhase phase)
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
        OperationContext octxt = zsc.getOperationContext();
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        boolean initial = phase == SyncPhase.INITIAL;
        Element f = ToXML.encodeFolder(response, ifmt, octxt, folder, Change.ALL_FIELDS);
        if (initial && isVisible) {
            // we're in the middle of an initial sync, so serialize the item ids
            boolean isSearch = folder instanceof SearchFolder;
            if (!isSearch) {
                if (folder.getId() == Mailbox.ID_FOLDER_TAGS) {
                    initialTagSync(zsc, f, mbox);
                } else {
                    initialItemSync(f, MailConstants.E_MSG, mbox.listItemIds(octxt, MailItem.TYPE_MESSAGE, folder.getId()));
                    initialItemSync(f, MailConstants.E_CHAT, mbox.listItemIds(octxt, MailItem.TYPE_CHAT, folder.getId()));
                    initialItemSync(f, MailConstants.E_CONTACT, mbox.listItemIds(octxt, MailItem.TYPE_CONTACT, folder.getId()));
                    initialItemSync(f, MailConstants.E_NOTE, mbox.listItemIds(octxt, MailItem.TYPE_NOTE, folder.getId()));
                    initialItemSync(f, MailConstants.E_APPOINTMENT, mbox.listItemIds(octxt, MailItem.TYPE_APPOINTMENT, folder.getId()));
                    initialItemSync(f, MailConstants.E_TASK, mbox.listItemIds(octxt, MailItem.TYPE_TASK, folder.getId()));
                }
            } else {
                // anything else to be done for searchfolders?
            }
        }

        if (isVisible && visible != null && visible.isEmpty())
            return true;

        // write the subfolders' data to the response
        for (Folder subfolder : subfolders) {
            if (subfolder != null)
                isVisible |= folderSync(zsc, f, mbox, subfolder, visible, phase);
        }

        // if this folder and all its subfolders are not visible (oops!), remove them from the response
        if (!isVisible)
            f.detach();

        return isVisible;
    }

    private static void initialTagSync(ZimbraSoapContext zsc, Element response, Mailbox mbox) throws ServiceException {
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        for (Tag tag : mbox.getTagList(zsc.getOperationContext())) {
            if (tag != null && !(tag instanceof Flag))
                ToXML.encodeTag(response, ifmt, tag, Change.ALL_FIELDS);
        }
    }

    private static void initialItemSync(Element f, String ename, int[] items) {
        if (items == null || items.length == 0)
            return;
        Element e = f.addElement(ename);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++)
            sb.append(i == 0 ? "" : ",").append(items[i]);
        e.addAttribute(MailConstants.A_IDS, sb.toString());
    }

    private static final int FETCH_BATCH_SIZE = 200;
    private static final int MAXIMUM_CHANGE_COUNT = 3990;

    private static final int MUTABLE_FIELDS = Change.MODIFIED_FLAGS  | Change.MODIFIED_TAGS |
                                              Change.MODIFIED_FOLDER | Change.MODIFIED_PARENT |
                                              Change.MODIFIED_NAME   | Change.MODIFIED_CONFLICT |
                                              Change.MODIFIED_COLOR  | Change.MODIFIED_POSITION |
                                              Change.MODIFIED_DATE;

    private static String deltaSync(final ZimbraSoapContext zsc, final Element response, final Mailbox mbox, final int begin, final boolean typedDeletes, int itemCutoff)
    throws ServiceException {
        String newToken = mbox.getLastChangeID() + "";
        if (begin >= mbox.getLastChangeID())
            return newToken;

        OperationContext octxt = zsc.getOperationContext();
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        // first, fetch deleted items
        MailItem.TypedIdList tombstones = mbox.getTombstoneSet(begin);
        Element eDeleted = response.addElement(MailConstants.E_DELETED);

        // then, handle created/modified folders
        if (zsc.isDelegatedRequest()) {
            // first, make sure that something changed...
            if (!mbox.getModifiedFolders(begin).isEmpty()) {
                // special-case the folder hierarchy for delegated delta sync
                Set<Folder> visible = mbox.getVisibleFolders(octxt);
                boolean anyFolders = folderSync(zsc, response, mbox, mbox.getFolderById(null, DEFAULT_FOLDER_ID), visible, SyncPhase.DELTA);
                // if no folders are visible, add an empty "<folder/>" as a hint
                if (!anyFolders)
                    response.addElement(MailConstants.E_FOLDER);
            }
        } else {
            for (Folder folder : mbox.getModifiedFolders(begin))
                ToXML.encodeFolder(response, ifmt, octxt, folder, Change.ALL_FIELDS);
        }

        // next, handle created/modified tags
        for (Tag tag : mbox.getModifiedTags(octxt, begin))
            ToXML.encodeTag(response, ifmt, tag, Change.ALL_FIELDS);

        // finally, handle created/modified "other items"
        int itemCount = 0;
        Pair<List<Integer>,MailItem.TypedIdList> changed = mbox.getModifiedItems(octxt, begin);
        List<Integer> modified = changed.getFirst();
        delta: while (!modified.isEmpty()) {
            List batch = modified.subList(0, Math.min(modified.size(), FETCH_BATCH_SIZE));
            for (MailItem item : mbox.getItemById(octxt, modified, MailItem.TYPE_UNKNOWN)) {
                // detect interrupted sync and resume from the appropriate place
                if (item.getModifiedSequence() == begin + 1 && item.getId() < itemCutoff)
                    continue;

                // if we've overflowed this sync response, set things up so that a subsequent sync starts from where we're cutting off
                if (itemCount >= MAXIMUM_CHANGE_COUNT) {
                    response.addAttribute(MailConstants.A_QUERY_MORE, true);
                    newToken = (item.getModifiedSequence() - 1) + "-" + item.getId();
                    break delta;
                }

                // For items in the system, if the content has changed since the user last sync'ed 
                // (because it was edited or created), just send back the folder ID and saved date --
                // the client will request the whole object out of band -- potentially using the 
                // content servlet's "include metadata in headers" hack.
                // If it's just the metadata that changed, send back the set of mutable attributes.
                boolean created = item.getSavedSequence() > begin;
                ToXML.encodeItem(response, ifmt, octxt, item, created ? Change.MODIFIED_FOLDER | Change.MODIFIED_CONFLICT : MUTABLE_FIELDS);
                itemCount++;
            }
            batch.clear();
        }

        // items that have been altered in non-visible folders will be returned as "deleted" in order to handle moves
        if (changed.getSecond() != null)
            tombstones.add(changed.getSecond());

        // cleanup: only return a <deleted> element if we're sending back deleted item ids
        if (tombstones == null || tombstones.isEmpty()) {
            eDeleted.detach();
        } else {
            StringBuilder deleted = new StringBuilder(), typed = new StringBuilder();
            for (Map.Entry<Byte,List<Integer>> entry : tombstones) {
                typed.setLength(0);
                for (Integer id : entry.getValue()) {
                    deleted.append(deleted.length() == 0 ? "" : ",").append(id);
                    if (typedDeletes)
                        typed.append(typed.length() == 0 ? "" : ",").append(id);
                }
                if (typedDeletes) {
                    // only add typed delete information if the client explicitly requested it
                    String eltName = elementNameForType(entry.getKey());
                    if (eltName != null)
                        eDeleted.addElement(eltName).addAttribute(MailConstants.A_IDS, typed.toString());
                }
            }
            eDeleted.addAttribute(MailConstants.A_IDS, deleted.toString());
        }

        return newToken;
    }

    public static String elementNameForType(byte type) {
        switch (type) {
            case MailItem.TYPE_FOLDER:       return MailConstants.E_FOLDER;
            case MailItem.TYPE_SEARCHFOLDER: return MailConstants.E_SEARCH;
            case MailItem.TYPE_MOUNTPOINT:   return MailConstants.E_MOUNT;
            case MailItem.TYPE_FLAG:
            case MailItem.TYPE_TAG:          return MailConstants.E_TAG;
            case MailItem.TYPE_VIRTUAL_CONVERSATION:
            case MailItem.TYPE_CONVERSATION: return MailConstants.E_CONV;
            case MailItem.TYPE_CHAT:         return MailConstants.E_CHAT;
            case MailItem.TYPE_MESSAGE:      return MailConstants.E_MSG;
            case MailItem.TYPE_CONTACT:      return MailConstants.E_CONTACT;
            case MailItem.TYPE_APPOINTMENT:  return MailConstants.E_APPOINTMENT;
            case MailItem.TYPE_TASK:         return MailConstants.E_TASK;
            case MailItem.TYPE_NOTE:         return MailConstants.E_NOTE;
            case MailItem.TYPE_WIKI:         return MailConstants.E_WIKIWORD;
            case MailItem.TYPE_DOCUMENT:     return MailConstants.E_DOC;
            default:                         return null;
        }
    }

    public static byte typeForElementName(String name) {
        if (name.equals(MailConstants.E_FOLDER))            return MailItem.TYPE_FOLDER;
        else if (name.equals(MailConstants.E_SEARCH))       return MailItem.TYPE_SEARCHFOLDER;
        else if (name.equals(MailConstants.E_MOUNT))        return MailItem.TYPE_MOUNTPOINT;
        else if (name.equals(MailConstants.E_TAG))          return MailItem.TYPE_TAG;
        else if (name.equals(MailConstants.E_CONV))         return MailItem.TYPE_CONVERSATION;
        else if (name.equals(MailConstants.E_MSG))          return MailItem.TYPE_MESSAGE;
        else if (name.equals(MailConstants.E_CHAT))         return MailItem.TYPE_CHAT;
        else if (name.equals(MailConstants.E_CONTACT))      return MailItem.TYPE_CONTACT;
        else if (name.equals(MailConstants.E_APPOINTMENT))  return MailItem.TYPE_APPOINTMENT;
        else if (name.equals(MailConstants.E_TASK))         return MailItem.TYPE_TASK;
        else if (name.equals(MailConstants.E_NOTE))         return MailItem.TYPE_NOTE;
        else if (name.equals(MailConstants.E_WIKIWORD))     return MailItem.TYPE_WIKI;
        else if (name.equals(MailConstants.E_DOC))          return MailItem.TYPE_DOCUMENT;
        else                                                return MailItem.TYPE_UNKNOWN;
    }

}