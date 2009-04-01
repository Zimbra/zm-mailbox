package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;

public class CosId extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        // TODO Auto-generated method stub
        
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting())
            return;
        else {
            String cosId = mod.value();
            Provisioning prov = Provisioning.getInstance();
            /*
             * hmm, not sure if YCC(CalendarProvisioning) also requires that 
             * cos must exist when setting a cos id (e.g. zimbraDomainDefaultCOSId)
             * skip for now.  Hack to use idIsUUID() for the check.
             */
            if (prov.idIsUUID()) {
                Cos cos = prov.get(CosBy.id, cosId);
                if (cos == null)
                    throw ServiceException.INVALID_REQUEST("cos id " + cosId + 
                            " does not point to a valid cos", null);
            }
        }
    }
    
    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        // TODO Auto-generated method stub
    }


}
