package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class DisposableAddressBaseNameCheck extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        if (entry instanceof Account) { 
            Account accnt = (Account)  entry;
            String currentValue = accnt.getDisposableAddressBaseName();
            if (!StringUtil.isNullOrEmpty(currentValue)) {
                throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraDisposableAddressBaseName + " value can not be modified, once it's value is set", null);
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // empty method
    }
}
