/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.external.ExternalBlobConsistencyChecker;
import com.zimbra.cs.store.external.ExternalStoreManager;
import com.zimbra.cs.store.file.BlobConsistencyChecker;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.ZimbraSoapContext;

public final class CheckBlobConsistency extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);


        // Assemble the list of mailboxes.
        List<Integer> mailboxIds = new ArrayList<Integer>();
        List<Element> mboxElementList = request.listElements(AdminConstants.E_MAILBOX);
        if (mboxElementList.isEmpty()) {
            // Get all mailbox id's.
            for (int mboxId : MailboxManager.getInstance().getMailboxIds()) {
                mailboxIds.add(mboxId);
            }
        } else {
            // Read mailbox id's from the request.
            for (Element mboxEl : mboxElementList) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById((int) mboxEl.getAttributeLong(AdminConstants.A_ID));
                mailboxIds.add(mbox.getId());
            }
        }

        boolean checkSize = request.getAttributeBool(AdminConstants.A_CHECK_SIZE, true);
        boolean reportUsedBlobs = request.getAttributeBool(AdminConstants.A_REPORT_USED_BLOBS, false);

        // Check blobs and assemble response.
        Element response = zsc.createElement(AdminConstants.CHECK_BLOB_CONSISTENCY_RESPONSE);
        List<Element> volumeElementList = request.listElements(AdminConstants.E_VOLUME);
        List<Short> volumeIds = new ArrayList<Short>();
        if (!volumeElementList.isEmpty()) {
            for (Element volumeEl : volumeElementList) {
                short volumeId = (short) volumeEl.getAttributeLong(AdminConstants.A_ID);
                Volume vol = VolumeManager.getInstance().getVolume(volumeId);
                if (vol.getType() == Volume.TYPE_INDEX) {
                    throw ServiceException.INVALID_REQUEST("Index volume " + volumeId + " is not supported", null);
                } else {
                    volumeIds.add(volumeId);
                }
            }
        }

        List<Volume> vols = VolumeManager.getInstance().getAllVolumes();
        Map<StoreType, StoreManager> storeManagerMap = new HashMap<>();
        for (Volume volume: vols) {
            StoreManager sm = StoreManager.getReaderSMInstance(volume.getId());
            if (sm instanceof FileBlobStore && volumeElementList.isEmpty()) {
                storeManagerMap.putIfAbsent(StoreType.INTERNAL, sm);
                switch (volume.getType()) {
                    case Volume.TYPE_MESSAGE:
                    case Volume.TYPE_MESSAGE_SECONDARY:
                        volumeIds.add(volume.getId());
                        break;
                }
            } else if (sm instanceof ExternalStoreManager) {
                storeManagerMap.putIfAbsent(StoreType.EXTERNAL, sm);
            } else {
                storeManagerMap.putIfAbsent(StoreType.NOT_SUPPORTED, sm);
            }
        }
        for (StoreManager sm : storeManagerMap.values()) {
            if (sm instanceof ExternalStoreManager && volumeElementList.isEmpty()) {
                for (int mboxId : mailboxIds) {
                    blobCheckProcessor(null, mboxId, checkSize, reportUsedBlobs, sm, response);
                }
            } else if (sm instanceof FileBlobStore) {
                for (int mboxId : mailboxIds) {
                    blobCheckProcessor(volumeIds, mboxId, checkSize, reportUsedBlobs, sm, response);
                }
            } else {
                ZimbraLog.store.warn(sm.getClass().getName() + " is not supported");
            }
        }

        return response;
    }

    /**
     * This method is used to check internal and external blobs
     * Also process result and add in response
     * @param volumeIds
     * @param mboxId
     * @param checkSize
     * @param reportUsedBlobs
     * @param sm
     * @param response
     * @throws ServiceException
     */
    private void blobCheckProcessor(List<Short> volumeIds, int mboxId, boolean checkSize, boolean reportUsedBlobs, StoreManager sm, Element response) throws ServiceException {
        BlobConsistencyChecker checker = null;
        if (sm instanceof ExternalStoreManager) {
            checker = new ExternalBlobConsistencyChecker();
        } else {
            checker = new BlobConsistencyChecker();
        }
        checker.setStoreManager(sm);
        BlobConsistencyChecker.Results results = checker.check(volumeIds, mboxId, checkSize, reportUsedBlobs);
        if (results.hasInconsistency() || reportUsedBlobs) {
            Element mboxEl = response.addElement(AdminConstants.E_MAILBOX).addAttribute(AdminConstants.A_ID, mboxId);
            results.toElement(mboxEl);
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }

    public enum StoreType {
        INTERNAL(1),
        EXTERNAL(2),
        NOT_SUPPORTED(3);

        private final int storeType;

        StoreType(final int storeType) {
            this.storeType = storeType;
        }
    }

}
