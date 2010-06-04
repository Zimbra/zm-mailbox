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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.PublishShareInfoAction;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class PublishShareInfo extends ShareInfoHandler {

    private static final String[] OWNER_ACCOUNT_PATH = new String[] { AdminConstants.E_SHARE, AdminConstants.E_OWNER};
    protected String[] getProxiedAccountElementPath()  { return OWNER_ACCOUNT_PATH; }
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();
        
        // entry to modify the share info for 
        NamedEntry publishingOnEntry = getPublishableTargetEntry(zsc, request, prov);
        
        Element eShare = request.getElement(AdminConstants.E_SHARE);
        PublishShareInfoAction action = PublishShareInfoAction.fromString(eShare.getAttribute(AdminConstants.A_ACTION));
        
        // check if the authed admin has right to change(publish/unpublish) share info on this DL
        checkDistributionListRight(zsc, (DistributionList)publishingOnEntry, Admin.R_publishDistributionListShareInfo);
            
        Element eFolder = eShare.getElement(AdminConstants.E_FOLDER);
        String folderPath = eFolder.getAttribute(AdminConstants.A_PATH, null);
        String folderId = eFolder.getAttribute(AdminConstants.A_FOLDER, null);
        String folderIdOrPath = eFolder.getAttribute(AdminConstants.A_PATH_OR_ID, null);
        
        if (folderPath != null)
            ensureOtherFolderDescriptorsAreNotPresent(folderId, folderIdOrPath);
        else if (folderId != null)
            ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderIdOrPath);
        else if (folderIdOrPath != null)
            ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderId);

            
        if (action == PublishShareInfoAction.add) {
            // adding (publishing)
            
            Account ownerAcct = getOwner(zsc, eShare, prov, true);
            
            // we will need to access the owner's mailbox, check if the admin can 
            // access the owner account's mailbox
            checkAdminLoginAsRight(zsc, prov, ownerAcct);
            
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ownerAcct, false);

            if (mbox == null) {
                /*
                 * bug 42416
                 * instead of throwing, just do nothing.  The mailbox does not exist, there is any
                 * share info anyway. 
                 */
                // throw ServiceException.FAILURE("mailbox not found for account " + ownerAcct.getId(), null);
            } else {
            
                Folder folder = null;
                    
                if (folderPath != null)
                    folder = getFolderByPath(octxt, mbox, folderPath);
                else if (folderId != null)
                    folder = getFolderByPath(octxt, mbox, folderId);
                else if (folderIdOrPath != null)
                    folder = getFolder(octxt, mbox, folderIdOrPath);
                else {
                    // no folder is given, iterate through all folders
                    folder = null;
                }
                
                ShareInfo.Publishing.publish(prov, octxt, publishingOnEntry, ownerAcct, folder);
            }
            
        } else {
            // removing (unpublishing)
            
            /*
             * do not have to check admin login as right on the owner account because
             * we will be just going through published data on the DL.
             */ 
            
            //
            // owner account
            //
            boolean allOwners = false;
            String ownerAcctId = null;
            String ownerAcctEmail = null;
            
            Element eOwner = eShare.getOptionalElement(AdminConstants.E_OWNER);
            if (eOwner == null)
                allOwners = true;
            else {
                allOwners = false;
                String key = eOwner.getAttribute(AdminConstants.A_BY);
                AccountBy by = AccountBy.fromString(key);
                
                if (by == AccountBy.id)
                    ownerAcctId = eOwner.getText();
                else if (by == AccountBy.name)
                    ownerAcctEmail = eOwner.getText();
                else
                    throw ServiceException.INVALID_REQUEST("invalid owner key: " + key, null);
            }
            
            //
            // folder
            //
            if (folderIdOrPath != null) {
                if (folderIdOrPath.charAt(0) == '/')
                    folderPath = folderIdOrPath;
                else
                    folderId = folderIdOrPath;
            }
            
            boolean allFolders = (folderId == null && folderPath == null);
            
            ShareInfo.Published.unpublish(prov, (DistributionList)publishingOnEntry, 
                    ownerAcctId, ownerAcctEmail, allOwners, folderId, folderPath, allFolders);
        }
        
        Element response = zsc.createElement(AdminConstants.PUBLISH_SHARE_INFO_RESPONSE);
        return response;
    }
    
    // Folder returned is guaranteed to be not null
    private  Folder getFolder(OperationContext octxt, Mailbox mbox, String folderIdOrPath) 
        throws ServiceException {

        // try to get by path first
        try {
            return getFolderByPath(octxt, mbox, folderIdOrPath);
        } catch (MailServiceException e) {
            if (MailServiceException.NO_SUCH_FOLDER.equals(e.getCode())) {
                // folder not found by path, try getting it by id
                return getFolderById(octxt, mbox, folderIdOrPath);
            } else
                throw e;
        }
    }
    
    private Folder getFolderById(OperationContext octxt, Mailbox mbox, String folderId) throws ServiceException {
        
        int fid;
        try {
            fid = Integer.parseInt(folderId);
        } catch (NumberFormatException e) {
            throw MailServiceException.NO_SUCH_FOLDER(folderId);
        }
        
        Folder folder = mbox.getFolderById(octxt, fid);
        
        if (folder == null)
            throw MailServiceException.NO_SUCH_FOLDER(folderId);
        
        return folder;
    }
    
    private Folder getFolderByPath(OperationContext octxt, Mailbox mbox, String folderPath) throws ServiceException {
        Folder folder = mbox.getFolderByPath(octxt, folderPath);
        
        if (folder == null)
            throw MailServiceException.NO_SUCH_FOLDER(folderPath);
        
        return folder;
    }
    
    private void ensureOtherFolderDescriptorsAreNotPresent(String other1, String other2) throws ServiceException {
        if (other1 != null || other2 != null)
            throw ServiceException.INVALID_REQUEST("can only specify one of " + 
                                                   AdminConstants.A_PATH + " or " +
                                                   AdminConstants.A_FOLDER + 
                                                   AdminConstants.A_PATH_OR_ID, null);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_adminLoginAs);
        relatedRights.add(Admin.R_adminLoginCalendarResourceAs);
        relatedRights.add(Admin.R_publishDistributionListShareInfo);
        
        notes.add("Needs the " + Admin.R_publishDistributionListShareInfo.getName() + 
                " right on the distribution list entry to publish; and the " + 
                Admin.R_adminLoginAs.getName() + "/" + Admin.R_adminLoginCalendarResourceAs.getName()
                + " right on the owner account/calendar resource.");
        
    }
}
