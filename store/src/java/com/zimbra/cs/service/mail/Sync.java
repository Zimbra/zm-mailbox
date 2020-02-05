/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.OperationContextData;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.util.PagedDelete;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SyncRequest;

/**
 * @since Aug 31, 2004
 */
public class Sync extends MailDocumentHandler {

    protected static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
    @Override protected String[] getProxiedIdPath(Element request)  { return TARGET_FOLDER_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        SyncRequest syncRequest = zsc.elementToJaxb(request);
        String token = syncRequest.getToken();

        Element response = zsc.createElement(MailConstants.SYNC_RESPONSE);
        response.addAttribute(MailConstants.A_CHANGE_DATE, System.currentTimeMillis() / 1000);

        // the sync token is of the form "last fully synced change id" (e.g. "32425") or
        // last fully synced change id-last item synced in next change id" (e.g. "32425-99213") or
        // last fully synced change id-last item synced in next change id and last fully synced delete change id" (e.g. "32425-99213:d1231232") or
        // last fully synced change id-last item synced in next change id and 
        // last fully synced delete id-last item synced in next delete id (e.g. "32425-99213:d12312-82134")
        SyncToken syncToken = null;
        int tokenInt = 0;
        if (!StringUtil.isNullOrEmpty(token)) {
            syncToken = new SyncToken(token);
            tokenInt = syncToken.getChangeId();
        }
        if (syncToken == null) {
            syncToken = new SyncToken(0);
        }
        int deleleLimit = syncRequest.getDeleteLimit();
        int changeLimit = syncRequest.getChangeLimit();

        // In case client like ZCO does not send @deleteLimit in soap call/request,
        // server can apply delete pagination through debugconfig/localconfig.
        if (deleleLimit <= 0) {
            deleleLimit = DebugConfig.syncMaximumDeleteCount;
        }

        // Client can specify change page limit. If unspecified by client or 
        // client specify more than DebugConfig.syncMaximumChangeCount It will use DebugConfig.syncMaximumChangeCount
        if (changeLimit <= 0 || changeLimit > DebugConfig.syncMaximumChangeCount) {
            changeLimit = DebugConfig.syncMaximumChangeCount;
        }

        boolean initialSync = tokenInt <= 0;

        // permit the caller to restrict initial sync only to calendar items with a recurrence after a given date
        long calendarStart = (syncRequest.getCalendarCutoff() != null) ? syncRequest.getCalendarCutoff() : -1;
        int messageSyncStart  = (syncRequest.getMsgCutoff() != null) ? syncRequest.getMsgCutoff() : -1;

        // if the sync is constrained to a folder subset, we need to first figure out what can be seen
        Folder root = null;
        ItemId iidFolder = null;
        try {
            iidFolder = new ItemId(request.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER_ID + ""), zsc);
            OperationContext octxtOwner = new OperationContext(mbox);
            root = mbox.getFolderById(octxtOwner, iidFolder.getId());
        } catch (MailServiceException.NoSuchItemException nsie) { }

        Set<Folder> visible = octxt.isDelegatedRequest(mbox) ? mbox.getVisibleFolders(octxt) : null;

        FolderNode rootNode = null;
        if (root == null || iidFolder == null) {
            // resolve grantee names of all ACLs on the mailbox
            rootNode = mbox.getFolderTree(octxt, null, true);
        } else {
            // resolve grantee names of all ACLs on all sub-folders of the requested folder
            rootNode = mbox.getFolderTree(octxt, iidFolder, true);
        }
        OperationContextData.addGranteeNames(octxt, rootNode);

        // actually perform the sync
        mbox.lock.lock();
        try {
            mbox.beginTrackingSync();

            if (initialSync) {
                response.addAttribute(MailConstants.A_TOKEN, mbox.getLastChangeID());
                response.addAttribute(MailConstants.A_SIZE, mbox.getSize());

                boolean anyFolders = folderSync(response, octxt, ifmt, mbox, root, visible, calendarStart, messageSyncStart, SyncPhase.INITIAL);
                // if no folders are visible, add an empty "<folder/>" as a hint
                if (!anyFolders) {
                    response.addElement(MailConstants.E_FOLDER);
                }
            } else {
                boolean typedDeletes = request.getAttributeBool(MailConstants.A_TYPED_DELETES, false);
                String newToken = deltaSync(response, octxt, ifmt, mbox, syncToken, deleleLimit, changeLimit, typedDeletes, root, visible, messageSyncStart);
                response.addAttribute(MailConstants.A_TOKEN, newToken);
            }
        } finally {
            mbox.lock.release();
        }

        return response;
    }

    private static final int DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_ROOT;
    private static enum SyncPhase { INITIAL, DELTA };

    /**
     * @param calendarStart start time of range, in milliseconds. {@code -1} means to leave the start time unconstrained.
     */
    private static boolean folderSync(Element response, OperationContext octxt, ItemIdFormatter ifmt, Mailbox mbox, Folder folder,
            Set<Folder> visible, long calendarStart, long messageSyncStart, SyncPhase phase)
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
        Element f = ToXML.encodeFolder(response, ifmt, octxt, folder, Change.ALL_FIELDS);
        if (initial && isVisible && folder.getType() == MailItem.Type.FOLDER) {
            // we're in the middle of an initial sync, so serialize the item ids
            if (folder.getId() == Mailbox.ID_FOLDER_TAGS) {
                initialTagSync(f, octxt, ifmt, mbox);
            } else {
                TypedIdList idlist = mbox.getItemIds(octxt, folder.getId());
                initialMsgSync(f, idlist, octxt, mbox, folder, messageSyncStart);
                initialItemSync(f, MailConstants.E_CHAT, idlist.getIds(MailItem.Type.CHAT));
                initialItemSync(f, MailConstants.E_CONTACT, idlist.getIds(MailItem.Type.CONTACT));
                initialItemSync(f, MailConstants.E_NOTE, idlist.getIds(MailItem.Type.NOTE));
                initialCalendarSync(f, idlist, octxt, mbox, folder, calendarStart);
                initialItemSync(f, MailConstants.E_DOC, idlist.getIds(MailItem.Type.DOCUMENT));
                initialItemSync(f, MailConstants.E_WIKIWORD, idlist.getIds(MailItem.Type.WIKI));
                initialCovSync(f, idlist, octxt, mbox, folder, messageSyncStart);
            }
        }

        if (isVisible && visible != null && visible.isEmpty())
            return true;

        // write the subfolders' data to the response
        for (Folder subfolder : subfolders) {
            if (subfolder != null) {
                isVisible |= folderSync(f, octxt, ifmt, mbox, subfolder, visible, calendarStart, messageSyncStart, phase);
            }
        }

        // if this folder and all its subfolders are not visible (oops!), remove them from the response
        if (!isVisible) {
            f.detach();
        }

        return isVisible;
    }

    private static void initialMsgSync(Element f, TypedIdList idlist, OperationContext octxt, Mailbox mbox, Folder folder, long messageSyncStart) throws ServiceException {
        if (messageSyncStart > 0 && !Collections.disjoint(idlist.types(), EnumSet.of(MailItem.Type.MESSAGE))) {
            idlist = mbox.listItemsForSync(octxt, folder.getId(), MailItem.Type.MESSAGE, messageSyncStart);
        }

        initialItemSync(f, MailConstants.E_MSG, idlist.getIds(MailItem.Type.MESSAGE));
    }

    private static void initialCovSync(Element f, TypedIdList idlist, OperationContext octxt, Mailbox mbox, Folder folder, long messageSyncStart) throws ServiceException {
        if (messageSyncStart > 0 && !Collections.disjoint(idlist.types(), EnumSet.of(MailItem.Type.CONVERSATION))) {
            idlist = mbox.listConvItemsForSync(octxt, folder.getId(), MailItem.Type.CONVERSATION, messageSyncStart);
        }
        initialItemSync(f, MailConstants.E_CONV, idlist.getIds(MailItem.Type.CONVERSATION));
    }

    private static void initialTagSync(Element f, OperationContext octxt, ItemIdFormatter ifmt, Mailbox mbox) throws ServiceException {
        for (Tag tag : mbox.getTagList(octxt)) {
            if (tag != null && !(tag instanceof Flag)) {
                ToXML.encodeTag(f, ifmt, octxt, tag, Change.ALL_FIELDS);
            }
        }
    }

    private static final Set<MailItem.Type> CALENDAR_TYPES = EnumSet.of(MailItem.Type.APPOINTMENT, MailItem.Type.TASK);

    /**
     * @param calendarStart start time of range, in milliseconds. {@code -1} means to leave the start time unconstrained.
     */
    private static void initialCalendarSync(Element f, TypedIdList idlist, OperationContext octxt, Mailbox mbox,
            Folder folder, long calendarStart)
    throws ServiceException {
        if (calendarStart > 0 && !Collections.disjoint(idlist.types(), CALENDAR_TYPES)) {
            idlist = mbox.listCalendarItemsForRange(octxt, MailItem.Type.UNKNOWN, calendarStart, -1, folder.getId());
        }
        initialItemSync(f, MailConstants.E_APPOINTMENT, idlist.getIds(MailItem.Type.APPOINTMENT));
        initialItemSync(f, MailConstants.E_TASK, idlist.getIds(MailItem.Type.TASK));
    }

    private static void initialItemSync(Element f, String ename, List<Integer> items) {
        if (items == null || items.isEmpty())
            return;
        f.addElement(ename).addAttribute(MailConstants.A_IDS, StringUtil.join(",", items));
    }

    private static final int FETCH_BATCH_SIZE = 200;

    private static final int MUTABLE_FIELDS = Change.FLAGS  | Change.TAGS | Change.FOLDER | Change.PARENT |
            Change.NAME | Change.CONFLICT | Change.COLOR  | Change.POSITION | Change.DATE;

    private static final Set<MailItem.Type> FOLDER_TYPES = EnumSet.of(MailItem.Type.FOLDER,
            MailItem.Type.SEARCHFOLDER, MailItem.Type.MOUNTPOINT);


    private static String deltaSync(Element response, OperationContext octxt, ItemIdFormatter ifmt, Mailbox mbox, SyncToken syncToken, int deleteLimit, int changeLimit, boolean typedDeletes, Folder root, Set<Folder> visible, int messageSyncStart)
    throws ServiceException {
        int begin = syncToken.getChangeId();
        int deleteModSeqCutoff = syncToken.getDeleteModSeq();
        deleteModSeqCutoff = deleteModSeqCutoff <= 0 ? begin : deleteModSeqCutoff;
        int mboxLastChangeId = mbox.getLastChangeID();
        SyncToken newSyncToken = new SyncToken(mboxLastChangeId);
        if (begin >= mboxLastChangeId && deleteModSeqCutoff >= mboxLastChangeId) {
            return newSyncToken.toString();
        }
        int changeItemIdCutoff = syncToken.getOffsetInNext();
        int deleteItemIdCutoff = syncToken.getDeleteOffsetInNext();

        // first, fetch deleted items
        TypedIdList tombstones = mbox.getTombstones(deleteModSeqCutoff);
        Element eDeleted = response.addElement(MailConstants.E_DELETED);

        // then, put together the requested folder hierarchy in 2 different flavors
        List<Folder> hierarchy = (root == null || root.getId() == Mailbox.ID_FOLDER_USER_ROOT ? null : root.getSubfolderHierarchy());
        Set<Integer> targetIds = (root != null && root.getId() == Mailbox.ID_FOLDER_USER_ROOT ? null : new HashSet<Integer>(hierarchy == null ? 0 : hierarchy.size()));
        if (hierarchy != null) {
            for (Folder folder : hierarchy) {
                targetIds.add(folder.getId());
            }
        }

        // then, handle created/modified folders
        if (octxt.isDelegatedRequest(mbox)) {
            // first, make sure that something changed...
            if (!mbox.getModifiedFolders(begin).isEmpty() || !Collections.disjoint(tombstones.types(), FOLDER_TYPES)) {
                // special-case the folder hierarchy for delegated delta sync
                boolean anyFolders = folderSync(response, octxt, ifmt, mbox, root, visible, -1, messageSyncStart, SyncPhase.DELTA);
                // if no folders are visible, add an empty "<folder/>" as a hint
                if (!anyFolders) {
                    response.addElement(MailConstants.E_FOLDER);
                }
            }
        } else {
            for (Folder folder : mbox.getModifiedFolders(begin)) {
                // targetIds is null when we are syncing the whole hierarchy, so there is
                // no case of "synthetic tombstone" (item falling out of the tree being synced)
                if (targetIds == null || targetIds.contains(folder.getId())) {
                    ToXML.encodeFolder(response, ifmt, octxt, folder, Change.ALL_FIELDS);
                } else {
                    tombstones.add(folder.getType(), folder.getId(), folder.getUuid(), folder.getModifiedSequence());
                }
            }
        }

        // next, handle created/modified tags
        for (Tag tag : mbox.getModifiedTags(octxt, begin)) {
            ToXML.encodeTag(response, ifmt, octxt, tag, Change.ALL_FIELDS);
        }

        // finally, handle created/modified "other items"
        int itemCount = 0;
        Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, Math.min(begin, deleteModSeqCutoff), messageSyncStart, MailItem.Type.UNKNOWN, targetIds, deleteModSeqCutoff);
        List<Integer> modified = changed.getFirst();

        // items that have been altered in non-visible folders will be returned as "deleted" in order to handle moves
        if (changed.getSecond() != null) {
            tombstones.addAll(changed.getSecond());
        }

        delta: while (!modified.isEmpty()) {
            List<Integer> batch = modified.subList(0, Math.min(modified.size(), FETCH_BATCH_SIZE));
            for (MailItem item : mbox.getItemById(octxt, batch, MailItem.Type.UNKNOWN)) {
                // detect interrupted sync and resume from the appropriate place
                if ((item.getModifiedSequence() == begin + 1 && item.getId() < changeItemIdCutoff) ||
                    item.getModifiedSequence() <= begin) { //if interrupted delete and un-interrupted modifications.
                    continue;
                }

                // if we've overflowed this sync response, set things up so that a subsequent sync starts from where we're cutting off
                if (itemCount >= changeLimit) {
                    response.addAttribute(MailConstants.A_QUERY_MORE, true);
                    newSyncToken.setChangeModSeq((item.getModifiedSequence() - 1));
                    newSyncToken.setChangeItemId(item.getId());
                    newSyncToken.setDeleteModSeq(mboxLastChangeId);
                    break delta;
                }

                // For items in the system, if the content has changed since the user last sync'ed
                // (because it was edited or created), just send back the folder ID and saved date --
                // the client will request the whole object out of band -- potentially using the
                // content servlet's "include metadata in headers" hack.
                // If it's just the metadata that changed, send back the set of mutable attributes.
                boolean created = item.getSavedSequence() > begin;
                ToXML.encodeItem(response, ifmt, octxt, item, created ? Change.FOLDER | Change.CONFLICT | Change.DATE | Change.PARENT : MUTABLE_FIELDS);
                itemCount++;
            }
            batch.clear();
        }

        // cleanup: only return a <deleted> element if we're sending back deleted item ids
        if ((deleteLimit > 0 && tombstones.size() > deleteLimit) ||  deleteItemIdCutoff > 0) {
            PagedDelete pgDel = new PagedDelete(tombstones, typedDeletes);
            pgDel.removeBeforeCutoff(deleteItemIdCutoff, deleteModSeqCutoff);
            if (deleteLimit > 0) {
                pgDel.trimDeletesTillPageLimit(deleteLimit);
            }
            encodePagedDelete(eDeleted, pgDel, newSyncToken, tombstones, typedDeletes);
            if (pgDel.isDeleteOverFlow()) {
                response.addAttribute(MailConstants.A_QUERY_MORE, true);
                response.addAttribute(MailConstants.A_QUERY_MORE, true);
            }
        } else {
            encodeUnpagedDelete(eDeleted, tombstones, typedDeletes);
        }
        return newSyncToken.toString();
    }

    private static void encodeUnpagedDelete(Element eDeleted, TypedIdList tombstones, boolean typedDeletes) {
        if (tombstones.isEmpty()) {
            eDeleted.detach();
        } else {
            StringBuilder deleted = new StringBuilder(), typed = new StringBuilder();
            for (Map.Entry<MailItem.Type, List<TypedIdList.ItemInfo>> entry : tombstones) {
                typed.setLength(0);
                for (TypedIdList.ItemInfo iinfo : entry.getValue()) {
                    deleted.append(deleted.length() == 0 ? "" : ",").append(iinfo.getId());
                    if (typedDeletes) {
                        typed.append(typed.length() == 0 ? "" : ",").append(iinfo.getId());
                    }
                }
                if (typedDeletes) {
                    // only add typed delete information if the client explicitly requested it
                    String eltName = elementNameForType(entry.getKey());
                    if (eltName != null) {
                        eDeleted.addElement(eltName).addAttribute(MailConstants.A_IDS, typed.toString());
                    }
                }
            }
            eDeleted.addAttribute(MailConstants.A_IDS, deleted.toString());
        }
    }

    private static void encodePagedDelete(Element eDeleted, PagedDelete pgDelete, SyncToken newSyncToken, TypedIdList tombstones, boolean typedDeletes) {
        Collection<Integer> itemIds = pgDelete.getAllIds();
        if(itemIds.isEmpty()) {
            eDeleted.detach();
        } else {
            if (typedDeletes) {
                Multimap<MailItem.Type, Integer> type2Id = pgDelete.getTypedItemIds();
                StringBuilder typed = new StringBuilder();
                for (MailItem.Type type : type2Id.keySet()) {
                    String eltName = elementNameForType(type);
                    typed.setLength(0);
                    for (Integer id :type2Id.get(type)) {
                        typed.append(typed.length() == 0 ? "" : ",").append(id);
                    }
                    eDeleted.addElement(eltName).addAttribute(MailConstants.A_IDS, typed.toString());
                }
            }
            StringBuilder deleted = new StringBuilder();
            for (Integer itemId : itemIds) {
                deleted.append(deleted.length() == 0 ? "" : ",").append(itemId);
            }
            eDeleted.addAttribute(MailConstants.A_IDS, deleted.toString());
        }
        if (pgDelete.isDeleteOverFlow()) {
            newSyncToken.setDeleteItemId(pgDelete.getLastItemId());
            newSyncToken.setDeleteModSeq(pgDelete.getCutOffModsequnce()-1);
        }
    }

    public static String elementNameForType(MailItem.Type type) {
        switch (type) {
            case FOLDER:
                return MailConstants.E_FOLDER;
            case SEARCHFOLDER:
                return MailConstants.E_SEARCH;
            case MOUNTPOINT:
                return MailConstants.E_MOUNT;
            case FLAG:
            case TAG:
                return MailConstants.E_TAG;
            case VIRTUAL_CONVERSATION:
            case CONVERSATION:
                return MailConstants.E_CONV;
            case CHAT:
                return MailConstants.E_CHAT;
            case MESSAGE:
                return MailConstants.E_MSG;
            case CONTACT:
                return MailConstants.E_CONTACT;
            case APPOINTMENT:
                return MailConstants.E_APPOINTMENT;
            case TASK:
                return MailConstants.E_TASK;
            case NOTE:
                return MailConstants.E_NOTE;
            case WIKI:
                return MailConstants.E_WIKIWORD;
            case DOCUMENT:
                return MailConstants.E_DOC;
            default:
                return null;
        }
    }

    public static MailItem.Type typeForElementName(String name) {
        if (name.equals(MailConstants.E_FOLDER)) {
            return MailItem.Type.FOLDER;
        } else if (name.equals(MailConstants.E_SEARCH)) {
            return MailItem.Type.SEARCHFOLDER;
        } else if (name.equals(MailConstants.E_MOUNT)) {
            return MailItem.Type.MOUNTPOINT;
        } else if (name.equals(MailConstants.E_TAG)) {
            return MailItem.Type.TAG;
        } else if (name.equals(MailConstants.E_CONV)) {
            return MailItem.Type.CONVERSATION;
        } else if (name.equals(MailConstants.E_MSG)) {
            return MailItem.Type.MESSAGE;
        } else if (name.equals(MailConstants.E_CHAT)) {
            return MailItem.Type.CHAT;
        } else if (name.equals(MailConstants.E_CONTACT)) {
            return MailItem.Type.CONTACT;
        } else if (name.equals(MailConstants.E_APPOINTMENT)) {
            return MailItem.Type.APPOINTMENT;
        } else if (name.equals(MailConstants.E_TASK)) {
            return MailItem.Type.TASK;
        } else if (name.equals(MailConstants.E_NOTE)) {
            return MailItem.Type.NOTE;
        } else if (name.equals(MailConstants.E_WIKIWORD)) {
            return MailItem.Type.WIKI;
        } else if (name.equals(MailConstants.E_DOC)) {
            return MailItem.Type.DOCUMENT;
        } else {
            return MailItem.Type.UNKNOWN;
        }
    }

}
