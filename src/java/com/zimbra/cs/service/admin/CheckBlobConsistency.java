/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.BlobConsistencyChecker;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckBlobConsistency extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        
        // Validate the blob store type.
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getName() + " is not supported", null);
        }
        
        // Assemble the list of volumes.
        List<Short> volumeIds = new ArrayList<Short>();
        List<Element> volumeElementList = request.listElements(AdminConstants.E_VOLUME);
        if (volumeElementList.isEmpty()) {
            // Get all message volume id's.
            List<Volume> volumes = Volume.getByType(Volume.TYPE_MESSAGE);
            volumes.addAll(Volume.getByType(Volume.TYPE_MESSAGE_SECONDARY));
            for (Volume vol : volumes) {
                volumeIds.add(vol.getId());
            }
        } else {
            // Read volume id's from the request.
            for (Element volumeEl : volumeElementList) {
                short volumeId = (short) volumeEl.getAttributeLong(AdminConstants.A_ID);
                Volume vol = Volume.getById(volumeId);
                if (vol.getType() == Volume.TYPE_INDEX) {
                    throw ServiceException.INVALID_REQUEST("Index volume " + volumeId + " is not supported", null);
                } else {
                    volumeIds.add(volumeId);
                }
            }
        }
        
        // Assemble the list of mailboxes.
        List<Long> mailboxIds = new ArrayList<Long>();
        List<Element> mboxElementList = request.listElements(AdminConstants.E_MAILBOX);
        if (mboxElementList.isEmpty()) {
            // Get all mailbox id's.
            for (long mboxId : MailboxManager.getInstance().getMailboxIds()) {
                mailboxIds.add(mboxId);
            }
        } else {
            // Read mailbox id's from the request.
            for (Element mboxEl : mboxElementList) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxEl.getAttributeLong(AdminConstants.A_ID));
                mailboxIds.add(mbox.getId());
            }
        }
        
        // Check blobs and assemble response.
        Element response = zsc.createElement(AdminConstants.CHECK_BLOB_CONSISTENCY_RESPONSE);
        Element missing = response.addElement(AdminConstants.E_MISSING_BLOBS);
        Element incorrectSize = response.addElement(AdminConstants.E_INCORRECT_SIZE);
        Element unexpectedBlobs = response.addElement(AdminConstants.E_UNEXPECTED_BLOBS);
        
        for (long mboxId : mailboxIds) {
            BlobConsistencyChecker checker = new BlobConsistencyChecker();
            BlobConsistencyChecker.Results results = checker.check(volumeIds, mboxId, true);

            if (!results.missingBlobs.isEmpty()) {
                Element mboxEl = missing.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
                for (BlobInfo blob : results.missingBlobs) {
                    mboxEl.addElement(AdminConstants.E_ITEM)
                    .addAttribute(AdminConstants.A_ID, blob.itemId)
                    .addAttribute(AdminConstants.A_SIZE, blob.dbSize)
                    .addAttribute(AdminConstants.A_VOLUME_ID, blob.volumeId)
                    .addAttribute(AdminConstants.A_BLOB_PATH, blob.path);
                }
            }
            if (!results.incorrectSize.isEmpty()) {
                Element mboxEl = incorrectSize.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
                for (BlobInfo blob : results.incorrectSize) {
                    Element itemEl = mboxEl.addElement(AdminConstants.E_ITEM)
                        .addAttribute(AdminConstants.A_ID, blob.itemId)
                        .addAttribute(AdminConstants.A_SIZE, blob.dbSize)
                        .addAttribute(AdminConstants.A_VOLUME_ID, blob.volumeId);
                    itemEl.addElement(AdminConstants.E_BLOB)
                        .addAttribute(AdminConstants.A_PATH, blob.path)
                        .addAttribute(AdminConstants.A_SIZE, blob.fileDataSize)
                        .addAttribute(AdminConstants.A_FILE_SIZE, blob.fileSize);
                }
            }
            if (!results.unexpectedFiles.isEmpty()) {
                Element mboxEl = unexpectedBlobs.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
                for (File file : results.unexpectedFiles) {
                    mboxEl.addElement(AdminConstants.E_BLOB)
                    .addAttribute(AdminConstants.A_PATH, file.getAbsolutePath())
                    .addAttribute(AdminConstants.A_SIZE, file.length());
                }
            }
        }
        
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
