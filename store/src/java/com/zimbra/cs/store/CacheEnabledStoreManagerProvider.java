/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
package com.zimbra.cs.store;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.helper.ClassHelper;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

import java.io.IOException;

/**
 * Cache enable store manager provier
 */
public class CacheEnabledStoreManagerProvider {

    /**
     * StoreManager cache
     */
    private static final LoadingCache<Short, StoreManager> volumeStoreManagerCacheLoader = CacheBuilder.newBuilder()
            .build(new CacheLoader<Short, StoreManager>() {
                @Override
                public StoreManager load(final Short volumeId) throws Exception {
                    return getStoreManagerForVolume(volumeId);
                }
            });

    /**
     * Return StoreManager from cache if skiCache false otherwise cache object will be returned
     * @param volumeId
     * @param skipCache
     * @return
     * @throws Exception
     */
    public static StoreManager getStoreManagerForVolume(Short volumeId, boolean skipCache) throws Exception {
        if (!skipCache) {
            ZimbraLog.store.trace("Store Manager cached for %s", volumeId);
            return volumeStoreManagerCacheLoader.get(volumeId);
        }
        return getStoreManagerForVolume(volumeId);
    }

    private static StoreManager getStoreManagerForVolume(Short volumeId) throws ServiceException {
        // load volume
        Volume volume = VolumeManager.getInstance().getVolume(volumeId);
        String className = volume.getStoreManagerClass();
        StoreManager storeManager = null;
        try {
            ZimbraLog.store.debug("loading Store Manager: %s for %s", className, volumeId);
            storeManager = (StoreManager) ClassHelper.getZimbraClassInstanceBy(className);
            ZimbraLog.store.debug("StoreManager loaded, starting up");
            storeManager.startup();
        } catch (ReflectiveOperationException e) {
            String msg = "error while loading StoreManager class: " + className;
            ZimbraLog.store.error(msg, e);
            throw ServiceException.FAILURE(msg, e);
        } catch (IOException e) {
            String msg = "error while StoreManager.startup() for class: " + className;
            ZimbraLog.store.error(msg, e);
            throw ServiceException.FAILURE(msg, e);
        }
        return storeManager;
    }

    protected static void refreshCache(short volumeId) {
        volumeStoreManagerCacheLoader.refresh(volumeId);
    }
}