package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.Factory;
import com.zimbra.cs.extension.ExtensionUtil;

public class EphemeralBackendCheck extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName,
            Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        String url = (String) attrValue;
        String[] tokens = url.split(":");
        if (tokens != null && tokens.length > 0) {
            String backend = tokens[0];
            if (backend.equalsIgnoreCase("ldap")) {
                EphemeralStore.clearFactory();
                return;
            }
            Factory factory = EphemeralStore.getFactory(backend);
            if (factory == null) {
                // Probably called from zmprov in LDAP mode, so need to setup any Ephemeral Store extensions
                Level savedEphem = ZimbraLog.ephemeral.getLevel();
                Level savedExten = ZimbraLog.extensions.getLevel();
                try {
                    // suppress logging in zmprov output
                    ZimbraLog.ephemeral.setLevel(Level.error);
                    ZimbraLog.extensions.setLevel(Level.error);
                    ExtensionUtil.initAllMatching(new EphemeralStore.EphemeralStoreMatcher(backend));
                } finally {
                    ZimbraLog.ephemeral.setLevel(savedEphem);
                    ZimbraLog.extensions.setLevel(savedExten);
                }
                factory = EphemeralStore.getFactory(backend);
            }
            if (factory == null) {
                throw ServiceException.FAILURE(String.format(
                        "unable to modify %s; no factory found for backend '%s'", attrName, backend), null);
            }
            try {
                factory.test(url);
                EphemeralStore.clearFactory();
            } catch (ServiceException e) {
                throw ServiceException.FAILURE(String.format("cannot set zimbraEphemeralBackendURL to %s", url), e);
            }
        } else {
            throw ServiceException.FAILURE(String.format(
                    "unable to modify %s; no ephemeral backend specified", attrName), null);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {}

}
