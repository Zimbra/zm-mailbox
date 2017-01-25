package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.Factory;

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
                return;
            }
            Factory factory = EphemeralStore.getFactory(backend);
            if (factory == null) {
                throw ServiceException.FAILURE(String.format("unable to modify %s; no factory found for backend '%s'", attrName, backend), null);
            }
            try {
                factory.test(url);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE(String.format("cannot set zimbraEphemeralBackendURL to %s", url), e);
            }
        } else {
            throw ServiceException.FAILURE(String.format("no ephemeral backend specified", attrName), null);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {}

}
