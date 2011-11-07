package com.zimbra.cs.account.callback;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

public class MtaRelayHost extends AttributeCallback {

    /**
     * ensure there can be only one value on zimbraMtaRelayHost
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {
        
        MultiValueMod mod = multiValueMod(attrsToModify, attrName);
        Set<String> valuesToBe = newValuesToBe(mod, entry, attrName);
        
        if (valuesToBe.size() > 1) {
            throw ServiceException.INVALID_REQUEST(attrName + " can only have one value", null);
        }
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
