package com.zimbra.cs.account.callback;

import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.logger.EventLogger;

public class EventLoggerCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
        if (attrName.equalsIgnoreCase(Provisioning.A_zimbraEventLoggingBackends)) {
            String newBackend = (String) attrValue;
            String[] tokens = newBackend.split(":", 2);
            String newBackendName = tokens[0];
            String newBackendConfig = tokens[1];
            //check that a corresponding EventLogHandler Factory exists
            if (!EventLogger.isFactoryRegistered(newBackendName)) {
                throw ServiceException.FAILURE(String.format("'%s' does not correspond to a registered EventLogHandler Factory", newBackendName), null);
            }
            //check that a handler with the same config is not already registered
            checkExistingHandlers(entry, newBackendName, newBackendConfig);
        }
    }

    private void checkExistingHandlers(Entry entry, String newBackendName, String newBackendConfig) throws ServiceException {
        String[] curBackends = null;
        if (entry instanceof Server) {
            curBackends = ((Server) entry).getEventLoggingBackends();
        } else if (entry instanceof Config) {
            curBackends = ((Config) entry).getEventLoggingBackends();
        }
        if (curBackends != null) {
            for (String curBackend: curBackends) {
                String[] toks = curBackend.split(":", 2);
                if (toks[0].equalsIgnoreCase(newBackendName) && toks[1].equalsIgnoreCase(newBackendConfig)) {
                    String msg = String.format("Event log handler '%s' is already registered with config '%s'", newBackendName, newBackendConfig);
                    throw ServiceException.FAILURE(msg,  null);
                }
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
