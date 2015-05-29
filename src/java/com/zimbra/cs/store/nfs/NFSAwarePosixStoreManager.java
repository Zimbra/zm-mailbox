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
package com.zimbra.cs.store.nfs;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.store.mount.MountedPosixStoreManager;
import com.zimbra.cs.volume.VolumeManager;

/**
 * NFS aware store manager using POSIX file interface.
 * Can generally be left as the default FileBlobStore provider; and will trigger nfs-specific handling when operations require it
 */
public class NFSAwarePosixStoreManager extends MountedPosixStoreManager {

    @Override
    public void startup() throws IOException, ServiceException {
        super.startup();
    }

    @Override
    public boolean supports(StoreFeature feature) {
        //if no volumes are currently using nfs we can support bulk delete
        //this allows us to use this class as the default while avoiding extra overhead for sites which do not use nfs
        switch (feature) {
            case BULK_DELETE:  return !VolumeManager.getInstance().isUsingNfs();
            default:           return super.supports(feature);
        }
    }
}
