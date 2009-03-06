package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

public class ServerConfig extends AttributeCallback {

    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

}
