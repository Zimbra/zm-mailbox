package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

import java.util.HashMap;
import java.util.Map;

public class BackupCSDReset extends AttributeCallback {
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {

    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Map<String, String> attrs = new HashMap<String, String>(1);
            attrs.put(Provisioning.A_zimbraBackupCSDReset, "TRUE");
            if (entry instanceof Server) {
                Server server = (Server) entry;
                prov.modifyAttrs(server, attrs);
            } else if (entry instanceof Config) {
                Config config = prov.getConfig();
                prov.modifyAttrs(config, attrs);
            }
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("unable to fetch the local server", e);
        }
    }
}
