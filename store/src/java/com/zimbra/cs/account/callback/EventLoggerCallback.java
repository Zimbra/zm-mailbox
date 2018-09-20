package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.logger.EventLogger;

public class EventLoggerCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
        if (attrName.equalsIgnoreCase(Provisioning.A_zimbraEventLoggingBackends)) {
            MultiValueMod mod = multiValueMod(attrsToModify, Provisioning.A_zimbraEventLoggingBackends);
            if (mod.adding() || mod.replacing()) {
                if (attrValue instanceof String) {
                    validateBackend(entry, (String) attrValue, mod.adding());
                } else if (attrValue instanceof String[]) {
                    for (String backend: (String[]) attrValue) {
                        validateBackend(entry, backend, mod.adding());
                    }
                }
            }
        }
    }

    private void validateBackend(Entry entry, String backend, boolean checkExisting) throws ServiceException {
        String[] tokens = backend.split(":", 2);
        if (tokens.length < 2) {
            throw ServiceException.FAILURE("zimbraEventLoggingBackends values must be of the form backend:config", null);
        }
        String newBackendName = tokens[0];
        String newBackendConfig = tokens[1];
        //if we're adding handlers to an existing list, check that a handler with the same config is not already registered
        if (checkExisting) {
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
        if (attrName.equals(Provisioning.A_zimbraEventLoggingEnabled)) {
            try {
                boolean isEnabled = Provisioning.getInstance().getLocalServer().isEventLoggingEnabled();
                EventLogger.getEventLogger().setEnabled(isEnabled);
            } catch (ServiceException e) {
                ZimbraLog.event.error("unable to determine zimbraEventLoggingEnabled value", e);
            }
        } else if (attrName.equals(Provisioning.A_zimbraEventBackendURL)) {
            EventStore.clearFactory();
        } else {
            try {
                EventLogger.getEventLogger().restartEventNotifierExecutor();
            } catch (ServiceException e) {
                ZimbraLog.event.error("unable to restart event notifier executor", e);
            }
        }
    }
}
