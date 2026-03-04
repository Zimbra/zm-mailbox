package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

import java.util.Map;

public class MailQuotaSoftLimitCallback extends AttributeCallback {
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {

        int value = Integer.parseInt((String) attrValue);

        if (value > 120) {
            throw ServiceException.INVALID_REQUEST("Soft limit cannot be more than 120% of the mail quota", null);
       }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {

    }
}
