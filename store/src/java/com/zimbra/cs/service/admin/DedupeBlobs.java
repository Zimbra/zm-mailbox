/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.BlobDeduper;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.DedupeBlobsRequest;
import com.zimbra.soap.admin.message.DedupeBlobsResponse;
import com.zimbra.soap.admin.message.DedupeBlobsResponse.DedupStatus;
import com.zimbra.soap.admin.type.IntIdAttr;
import com.zimbra.soap.admin.type.VolumeIdAndProgress;

public final class DedupeBlobs extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getName()
                    + " is not supported", null);
        }
        DedupeBlobsRequest req = zsc.elementToJaxb(request);
        BlobDeduper deduper = BlobDeduper.getInstance();
        DedupeBlobsResponse resp = new DedupeBlobsResponse();
        // Assemble the list of volumes.
        List<IntIdAttr> volumeList = req.getVolumes();
        List<Short> volumeIds = new ArrayList<Short>();
        if ((req.getAction() == DedupeBlobsRequest.DedupAction.start) ||
                (req.getAction() == DedupeBlobsRequest.DedupAction.reset)) {
            if (volumeList.isEmpty()) {
                // Get all message volume id's.
                for (Volume vol : VolumeManager.getInstance().getAllVolumes()) {
                    switch (vol.getType()) {
                    case Volume.TYPE_MESSAGE:
                    case Volume.TYPE_MESSAGE_SECONDARY:
                        volumeIds.add(vol.getId());
                        break;
                    }
                }
            } else {
                // Read volume id's from the request.
                for (IntIdAttr attr : volumeList) {
                    short volumeId = (short) attr.getId();
                    Volume vol = VolumeManager.getInstance().getVolume(volumeId);
                    if (vol.getType() == Volume.TYPE_INDEX) {
                        throw ServiceException.INVALID_REQUEST("Index volume " + volumeId + " is not supported", null);
                    } else {
                        volumeIds.add(volumeId);
                    }
                }
            }
        }
        if (req.getAction() == DedupeBlobsRequest.DedupAction.start) {
                try {
                    deduper.process(volumeIds);
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error while deduping", e);
                }
        } else if (req.getAction() == DedupeBlobsRequest.DedupAction.stop) {
            deduper.stopProcessing();
        } else if (req.getAction() == DedupeBlobsRequest.DedupAction.reset) {
            if (volumeList.isEmpty()) {
                deduper.resetVolumeBlobs(new ArrayList<Short>());
            } else {
                deduper.resetVolumeBlobs(volumeIds);
            }
        }
        // return the stats for all actions.
        boolean isRunning = deduper.isRunning();
        if (isRunning) {
            resp.setStatus(DedupStatus.running);
        } else {
            resp.setStatus(DedupStatus.stopped);
        }
        Map<Short, String> volumeBlobsProgress = deduper.getVolumeBlobsProgress();
        VolumeIdAndProgress[] blobProgress = new VolumeIdAndProgress[volumeBlobsProgress.size()];
        int i=0;
        for (Map.Entry<Short, String> entry : volumeBlobsProgress.entrySet()) {
        	blobProgress[i++] = new VolumeIdAndProgress(String.valueOf(entry.getKey()), entry.getValue());
        }
        resp.setVolumeBlobsProgress(blobProgress);
        Map<Short, String> blobDigestsProgress = deduper.getBlobDigestsProgress();
        VolumeIdAndProgress[] digestProgress = new VolumeIdAndProgress[blobDigestsProgress.size()];
        i=0;
        for (Map.Entry<Short, String> entry : blobDigestsProgress.entrySet()) {
        	digestProgress[i++] = new VolumeIdAndProgress(String.valueOf(entry.getKey()), entry.getValue());
        }
        resp.setBlobDigestsProgress(digestProgress);
        Pair<Integer, Long> pair = deduper.getCountAndSize();
        resp.setTotalCount(pair.getFirst());
        resp.setTotalSize(pair.getSecond());
        return zsc.jaxbToElement(resp);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
