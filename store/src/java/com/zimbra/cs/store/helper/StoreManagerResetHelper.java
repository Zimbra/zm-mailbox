package com.zimbra.cs.store.helper;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.soap.admin.type.StoreManagerRuntimeSwitchResult;
import org.dom4j.DocumentException;

import java.io.IOException;

import static com.zimbra.common.localconfig.LC.zimbra_class_store;

public class StoreManagerResetHelper {

    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_NO_OP = "NO_OP";

    public static StoreManagerRuntimeSwitchResult setNewStoreManager(String storeManagerClass) throws ServiceException{
        if (Provisioning.getInstance().getLocalServer().isSMRuntimeSwitchEnabled()) {
            return new StoreManagerRuntimeSwitchResult(STATUS_NO_OP, "StoreManager runtime switch not enabled, enabled SMRunTimeSwitchEnabled ldap attribute");
        }
        if (StringUtil.isNullOrEmpty(storeManagerClass)) {
            String message = "storeManagerClass is empty: ";
            ZimbraLog.store.error(message);
            return new StoreManagerRuntimeSwitchResult(STATUS_FAIL, message);
        }
        if (LC.zimbra_class_store.value().equals(storeManagerClass)) {
            String message = "store_manager class is same as set zimbra_class_store: "+ LC.zimbra_class_store.value();
            ZimbraLog.store.error(message);
            return new StoreManagerRuntimeSwitchResult(STATUS_FAIL, message);
        }
        // if class not exist then throw exception
        if (!ClassHelper.isClassExist(storeManagerClass)) {
            throw ServiceException.OPERATION_DENIED(" store manager class " + storeManagerClass + " not found on classPath");
        }

        // update LC attribute
        try {
            LocalConfig localConfig = new LocalConfig(null);
            localConfig.set(zimbra_class_store.key(), storeManagerClass);
            localConfig.save();
            LC.reload();
        } catch (DocumentException | ConfigException | IOException e) {
            ZimbraLog.store.error("Error while updating LC.zimbra_class_store to StoreManager: %s", storeManagerClass, e);
            return new StoreManagerRuntimeSwitchResult(STATUS_FAIL, "not able to update LC.zimbra_class_store with new StoreManager:"+storeManagerClass);
        }

        try {
            // verify if its set correctly or not
            if (!LC.zimbra_class_store.value().equals(storeManagerClass)) {
                throw ServiceException.OPERATION_DENIED("not able to update StoreManager class " + storeManagerClass + " in cached zimbra_class_store attr");
            }
            StoreManager.resetStoreManager();
        } catch (ServiceException | IOException e) {
            ZimbraLog.store.error("not able to reset StoreManager: %s", storeManagerClass, e);
            return new StoreManagerRuntimeSwitchResult(STATUS_FAIL, "not able to reset StoreManager:"+storeManagerClass);
        }
        return new StoreManagerRuntimeSwitchResult(STATUS_SUCCESS, "StoreManager LC updated with " + storeManagerClass + " and StoreManager.reset called");
    }
}
