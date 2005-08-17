package com.liquidsys.coco.account.callback;

import java.util.Map;

import com.liquidsys.coco.account.AttributeCallback;
import com.liquidsys.coco.account.Entry;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.ServiceException;

public class AccountStatus implements AttributeCallback {

    /**
     * check to make sure liquidMailHost points to a valid server liquidServiceHostname
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_liquidAccountStatus+" is a single-valued attribute", null);
        
        String status = (String) value;

        if (status.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
            attrsToModify.put(Provisioning.A_liquidMailStatus, Provisioning.MAIL_STATUS_DISABLED);
        } else {
            attrsToModify.put(Provisioning.A_liquidMailStatus, Provisioning.MAIL_STATUS_ENABLED);
        }
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
