package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class EventLoggerCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
    }

    private void validateBackend(Entry entry, String backend, boolean checkExisting) throws ServiceException {
    }

    private void checkExistingHandlers(Entry entry, String newBackendName, String newBackendConfig) throws ServiceException {
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
