/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store.mount;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.MailboxBlob.MailboxBlobInfo;
import com.zimbra.cs.store.file.FileBlobStore;

/**
 * FileBlobStore implementation which handles all volumes as mounted paths
 * Technically can be used for any mounted filesystem; although official support is only added for a few specific filesystem types
 *
 */
public class MountedPosixStoreManager extends FileBlobStore {

    @Override
    public boolean supports(StoreFeature feature) {
        //we set BULK_DELETE to false so we get a list of blobs in deleteStore; to be used to clear FD cache
        switch (feature) {
            case BULK_DELETE:  return false;
            default:           return super.supports(feature);
        }
    }

    @Override
    public boolean deleteStore(MailboxData mboxData,
            Iterable<MailboxBlobInfo> blobs) throws IOException,
            ServiceException {
        //remove the fd cache entries which may still be open
        //could still leave open files if another server/process has opened them
        //however, they will eventually be cleared once that process is done
        if (blobs != null) {
            for (MailboxBlobInfo info : blobs) {
                short volumeId = Short.valueOf(info.locator);
                String path = getBlobPath(mboxData, info.itemId, info.revision, volumeId);
                BlobInputStream.getFileDescriptorCache().remove(path);
            }
        }
        return super.deleteStore(mboxData, null);
    }
}
