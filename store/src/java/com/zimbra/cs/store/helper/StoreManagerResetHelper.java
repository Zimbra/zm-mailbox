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
package com.zimbra.cs.store.helper;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.soap.admin.enums.Status;
import com.zimbra.soap.admin.type.StoreManagerRuntimeSwitchResult;
import org.dom4j.DocumentException;

import java.io.IOException;

public class StoreManagerResetHelper {

    /**
     * In Zimbra Platform StoreManager object used to work with stores(current primary and current secondary)
     * And current store manager class name is available as part of LocalConfig attribute `zimbra_class_store`.
     * Instance of current StoreManger is being maintained as singleton and can be accessed via
     * {@link StoreManager} static method getInstance(), check its implementation.
     * Now as per new design StoreManager class can be mapped to associated Volume in database
     * column volume.store_manager_class(string) and instance of new StoreManager can be used without restarting mailboxd
     * just by resetting singleton object and overriding LocalConfig attribute `zimbra_class_store`.
     * Following method performs part of reset process
     *      1. New StoreManager class validation
     *      2. Update local config attribute `zimbra_class_store`
     *      3. Override LocalConfig and save it.
     *      4. Reload LocalConfig cache.
     *      5. Initiate Singleton reset {@link StoreManager}.getInstance()
     *
     * @param storeManagerClass
     * @return
     * @throws ServiceException
     */
    public static StoreManagerRuntimeSwitchResult setNewStoreManager(String storeManagerClass) throws ServiceException{
        if (Provisioning.getInstance().getLocalServer().isSMRuntimeSwitchEnabled()) {
            return new StoreManagerRuntimeSwitchResult(Status.NO_OPERATION, "StoreManager runtime switch not enabled, enabled SMRunTimeSwitchEnabled ldap attribute");
        }
        if (StringUtil.isNullOrEmpty(storeManagerClass)) {
            String message = "storeManagerClass is empty: ";
            ZimbraLog.store.error(message);
            return new StoreManagerRuntimeSwitchResult(Status.FAIL, message);
        }
        if (LC.zimbra_class_store.value().equals(storeManagerClass)) {
            String message = "store_manager class is same as set zimbra_class_store: "+ LC.zimbra_class_store.value();
            ZimbraLog.store.debug(message);
            return new StoreManagerRuntimeSwitchResult(Status.FAIL, message);
        }
        // if class not exist then throw exception
        if (!ClassHelper.isClassExist(storeManagerClass)) {
            throw ServiceException.OPERATION_DENIED(" store manager class " + storeManagerClass + " not found on classPath");
        }

        // update LC attribute
        try {
            LocalConfig localConfig = new LocalConfig(null);
            localConfig.set(LC.zimbra_class_store.key(), storeManagerClass);
            localConfig.save();
            LC.reload();
        } catch (DocumentException | ConfigException | IOException e) {
            ZimbraLog.store.error("Error while updating LC.zimbra_class_store to StoreManager: %s", storeManagerClass, e);
            return new StoreManagerRuntimeSwitchResult(Status.FAIL, "not able to update LC.zimbra_class_store with new StoreManager:" + storeManagerClass);
        }

        try {
            // verify if its set correctly or not
            if (!LC.zimbra_class_store.value().equals(storeManagerClass)) {
                throw ServiceException.OPERATION_DENIED("not able to update StoreManager class " + storeManagerClass + " in cached zimbra_class_store attr");
            }
            StoreManager.resetStoreManager();
        } catch (ServiceException | IOException e) {
            ZimbraLog.store.error("not able to reset StoreManager: %s", storeManagerClass, e);
            return new StoreManagerRuntimeSwitchResult(Status.FAIL, "not able to reset StoreManager:" + storeManagerClass);
        }
        return new StoreManagerRuntimeSwitchResult(Status.SUCCESS, "StoreManager LC updated with " + storeManagerClass + " and StoreManager.reset called");
    }
}
