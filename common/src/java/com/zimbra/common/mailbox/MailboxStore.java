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
     */
    public void copyItemAction(OpContext ctxt, String authenticatedAcctId, ItemIdentifier targetFolder,
            List<Integer> idlist) throws ServiceException;
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
}
