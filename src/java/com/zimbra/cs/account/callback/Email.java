package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.AttributeInfo;

/**
 * Callback for validating attributes that should've been declared
 * email in zimbra-attrs.xml but had been declared as string.  
 * 
 * To avoid LDAP upgrade complication, we use this callback for
 * validating the format.  If the attr had been declared as 
 * email, the validation would have happened in AttributeInfo.checkValue. 
 *
 */
public class Email extends AttributeCallback {


    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting())
            return;
        
        AttributeInfo.validEmailAddress(mod.value(), false);
    }
    
    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        // TODO Auto-generated method stub

    }


}
