package com.liquidsys.coco.account.callback;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.account.AttributeCallback;
import com.liquidsys.coco.account.Entry;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.service.ServiceException;

public class MailHost implements AttributeCallback {

    /**
     * check to make sure liquidMailHost points to a valid server liquidServiceHostname
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_liquidMailHost+" is a single-valued attribute", null);
        
        String mailHost = (String) value;

        List servers = Provisioning.getInstance().getAllServers();
        for (Iterator it=servers.iterator(); it.hasNext(); ) {
            Server s = (Server) it.next();
            String serviceName = s.getAttr(Provisioning.A_liquidServiceHostname, null);
            if (mailHost.equalsIgnoreCase(serviceName)) 
                return;
        }
        
        throw ServiceException.INVALID_REQUEST("specified "+Provisioning.A_liquidMailHost+" does not correspond to a valid server service hostname: "+mailHost, null);
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
