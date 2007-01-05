package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.im.IMGatewayType;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGatewayList extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element response = lc.createElement(IMService.IM_GATEWAY_LIST_RESPONSE);
        
        Object lock = super.getLock(lc);
        synchronized (lock) {
            IMPersona persona = getRequestedPersona(lc, lock);
            
            IMGatewayType[] types = persona.getAvailableGateways();
            
            if (types != null && types.length > 0) {
                for (IMGatewayType t : types) {
                    Element typeElt = response.addElement("service");
                    typeElt.addAttribute("type", t.getShortName());
                }
            }
        }
        
        return response;
    }
}
