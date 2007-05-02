package com.zimbra.cs.service.im;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.interop.Interop.ServiceName;
import com.zimbra.cs.im.interop.Interop.UserStatus;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGatewayList extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element response = lc.createElement(IMConstants.IM_GATEWAY_LIST_RESPONSE);
        
        Object lock = super.getLock(lc);
        synchronized (lock) {
            IMPersona persona = getRequestedPersona(lc, lock);
            
            List<Pair<ServiceName, UserStatus>> types = persona.getAvailableGateways();
            String domain = persona.getDomain();
            
            for (Pair<ServiceName, UserStatus> p : types) {
                Element typeElt = response.addElement("service");
                typeElt.addAttribute("type", p.getFirst().name());
                typeElt.addAttribute("domain", p.getFirst().name()+"."+domain);
                if (p.getSecond() != null) {
                    Element rElt = typeElt.addElement("registration");
                    rElt.addAttribute("name", p.getSecond().username);
                    rElt.addAttribute("state", p.getSecond().state.name().toLowerCase());
                    if (p.getSecond().nextConnectAttemptTime > 0) {
                        rElt.addAttribute("timeUntilNextConnect", p.getSecond().nextConnectAttemptTime-System.currentTimeMillis());
                    }
                } 
            }
        }
        return response;
    }
}
