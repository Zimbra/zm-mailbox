package com.zimbra.cs.account.callback;

import java.util.Date;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class GalCallback extends AttributeCallback {
    
    private String oldValue = null;
    private String newValue = null;

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {
        
        oldValue = entry.getAttr(attrName, true);
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        newValue = mod.value();
        if (attrName.equals("zimbraGalLdapFilter")) {
            if (mod.unsetting())
                return;
            if ("ad".equalsIgnoreCase(newValue)) {
                attrsToModify.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, 
                        com.zimbra.cs.account.grouphandler.ADGroupHandler.class.getCanonicalName());
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        if (entry instanceof Domain) {
            try {
                if (!StringUtil.equal(oldValue, newValue)) {
                    ((Domain) entry).setGalDefinitionLastModifiedTime(new Date());
                }
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("Unable to set zimbraGalDefinitionLastModifiedTime " + e);
            }
        }
    }
    
}
