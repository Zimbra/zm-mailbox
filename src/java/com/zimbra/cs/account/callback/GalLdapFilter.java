package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class GalLdapFilter extends AttributeCallback {

    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting())
            return;
        
        String newValue = mod.value();
        if ("ad".equalsIgnoreCase(newValue))
            attrsToModify.put(Provisioning.A_zimbraGalLdapGroupHandlerClass, com.zimbra.cs.gal.ADGalGroupHandler.class.getCanonicalName());
    }

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
    
}
