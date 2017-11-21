/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.common.mailbox;

import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;

public interface MailboxStore {
    public String getAccountId() throws ServiceException;
    public long getSize() throws ServiceException;
    public FolderStore getFolderByPath(OpContext octxt, String path) throws ServiceException;
    public FolderStore getFolderById(OpContext octxt, String id) throws ServiceException;
    public ExistingParentFolderStoreAndUnmatchedPart getParentFolderStoreAndUnmatchedPart(OpContext octxt, String path)
    throws ServiceException;
    /**
     * Copies the items identified in {@link idlist} to folder {@link targetFolder}
     * @param idlist - list of item ids for items to copy
     * @param targetFolder - Destination folder
     * @return Item IDs of successfully copied items
     */
    public List<String> copyItemAction(OpContext ctxt, ItemIdentifier targetFolder, List<ItemIdentifier> idlist)
            throws ServiceException;
    public void createFolderForMsgs(OpContext octxt, String path) throws ServiceException;
    public void renameFolder(OpContext octxt, FolderStore folder, String path) throws ServiceException;
    public void deleteFolder(OpContext octxt, String itemId) throws ServiceException;
    public void emptyFolder(OpContext octxt, String folderId, boolean removeSubfolders) throws ServiceException;
    public void flagFolderAsSubscribed(OpContext ctxt, FolderStore folder) throws ServiceException;
    public void flagFolderAsUnsubscribed(OpContext ctxt, FolderStore folder) throws ServiceException;
    public List<FolderStore> getUserRootSubfolderHierarchy(OpContext ctxt) throws ServiceException;
    public void modifyFolderGrant(OpContext ctxt, FolderStore folder, GrantGranteeType granteeType, String granteeId,
            String perms, String args) throws ServiceException;
    public void modifyFolderRevokeGrant(OpContext ctxt, String folderId, String granteeId) throws ServiceException;
    /**
     * Delete <tt>MailItem</tt>s with given ids.  If there is no <tt>MailItem</tt> for a given id, that id is ignored.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param nonExistingItems If not null, This gets populated with the item IDs of nonExisting items
     */
    public void delete(OpContext octxt, List<Integer> itemIds, List<Integer> nonExistingItems) throws ServiceException;
    /** Resets the mailbox's "recent message count" to 0.  A message is considered "recent" if:
     *     (a) it's not a draft or a sent message, and
     *     (b) it was added since the last write operation associated with any SOAP session. */
    public void resetRecentMessageCount(OpContext octxt) throws ServiceException;
    /** Acquire an in process lock relevant for this type of MailboxStore */
    public void lock(boolean write);
    /** Release an in process lock relevant for this type of MailboxStore */
    public void unlock();
    /** Returns the IDs of all items modified since a given change number.
     *  Will not return modified folders or tags; for these you need to call
     * @return a List of IDs of all caller-visible MailItems of the given type modified since the checkpoint
     */
    public List<Integer> getIdsOfModifiedItemsInFolder(OpContext octxt, int lastSync, int folderId)
            throws ServiceException;
    /**
     * @return the item with the specified ID.
     */
    public ZimbraMailItem getItemById(OpContext octxt, ItemIdentifier id, MailItemType type) throws ServiceException;
    public void flagItemAsRead(OpContext octxt, ItemIdentifier itemId, MailItemType type) throws ServiceException;
    public List<ZimbraMailItem> getItemsById(OpContext octxt, Collection<ItemIdentifier> ids) throws ServiceException;
    public void alterTag(OpContext octxt, Collection<ItemIdentifier> ids, String tagName, boolean addTag)
            throws ServiceException;
    public void setTags(OpContext octxt, Collection<ItemIdentifier> itemIds, int flags, Collection<String> tags)
            throws ServiceException;
    public ZimbraSearchParams createSearchParams(String queryString);
    public ZimbraQueryHitResults searchImap(OpContext octx, ZimbraSearchParams params) throws ServiceException;
    /**
     * Returns the change sequence number for the most recent transaction.  This will be either the change number
     * for the current transaction or, if no database changes have yet been made in this transaction, the sequence
     * number for the last committed change.
     */
    public int getLastChangeID();
    public List<Integer> resetImapUid(OpContext octxt, List<Integer> itemIds) throws ServiceException;
    public void noOp() throws ServiceException;
    public void markMsgSeen(OpContext octxt, ItemIdentifier msgId) throws ServiceException;
}
