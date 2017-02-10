package com.zimbra.cs.account.callback;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.Factory;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.admin.type.CacheEntryType;

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
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        ExecutorService executor = newCachedThreadPool(newDaemonThreadFactory("ClearEphemeralConfigCache"));
        List<Server> servers = null;
        try {
            servers = Provisioning.getInstance().getAllMailClientServers();
        } catch (ServiceException e) {
            ZimbraLog.account.warn("cannot fetch list of servers");
            return;
        }
        for (Server server: servers) {
            try {
                if (server.isLocalServer()) {
                    // don't need to flush cache on this server
                    continue;
                }
            } catch (ServiceException e2) {
                ZimbraLog.ephemeral.warn("error determining if server %s is local server", server.getServiceHostname());
            }
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    SoapProvisioning soapProv = new SoapProvisioning();
                    try {
                        String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI, true);
                        soapProv.soapSetURI(adminUrl);
                    } catch (ServiceException e1) {
                        ZimbraLog.ephemeral.warn("could not get admin URL for server %s during ephemeral backend change", e1);
                        return;
                    }

                    try {
                        soapProv.soapZimbraAdminAuthenticate();
                        soapProv.flushCache(CacheEntryType.config, null);
                        ZimbraLog.ephemeral.info("sent FlushCache request to server %s", server.getServiceHostname());

                    } catch (ServiceException e) {
                        ZimbraLog.ephemeral.warn("cannot send FlushCache request to server %s", server.getServiceHostname(), e);
                    }
                }

            });

        }

    }

}
