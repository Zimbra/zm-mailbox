package com.zimbra.cs.service.im;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.interop.Interop;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGatewayList extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element response = lc.createElement(IMConstants.IM_GATEWAY_LIST_RESPONSE);
        
        Object lock = super.getLock(lc);
        synchronized (lock) {
            IMPersona persona = getRequestedPersona(lc, lock);
            
            List<Interop.ServiceName> types = persona.getAvailableGateways();
            String domain = persona.getDomain();
            
            for (Interop.ServiceName t : types) {
                Element typeElt = response.addElement("service");
                typeElt.addAttribute("type", t.name());
                typeElt.addAttribute("domain", t.name()+"."+domain);
            }
        }
        return response;
    }
}
