package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.logger.EventLogger;

public class EventLoggerCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
        if (attrName.equalsIgnoreCase(Provisioning.A_zimbraEventLoggingBackends)) {
            String backend = (String) attrValue;
            String[] tokens = backend.split(":");
            String backendName = tokens[0];
            if (!EventLogger.isFactoryRegistered(backendName)) {
                throw ServiceException.FAILURE(String.format("'%s' does not correspond to a registered EventLogHandler Factory", backendName), null);
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        if (attrName.equalsIgnoreCase(Provisioning.A_zimbraEventLoggingBackends) ||
                attrName.equalsIgnoreCase(Provisioning.A_zimbraEventLoggingNumThreads)) {
            EventLogger.getEventLogger().restartEventNotifierExecutor();
        }
    }

}
