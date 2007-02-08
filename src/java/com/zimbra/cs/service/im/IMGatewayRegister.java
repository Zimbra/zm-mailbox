package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.interop.Interop;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGatewayRegister extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        Element response = lc.createElement(IMConstants.IM_GATEWAY_REGISTER_RESPONSE);
        
        Object lock = super.getLock(lc);
        synchronized (lock) {
            IMPersona persona = getRequestedPersona(lc, lock);
            String op = request.getAttribute("op");
            boolean retVal = false;
            
            if (op.equals("unreg")) {
                String serviceStr = request.getAttribute("service");
                persona.gatewayUnRegister(Interop.ServiceName.valueOf(serviceStr));
            } else {
                String serviceStr = request.getAttribute("service");
                String nameStr = request.getAttribute("name");
                String pwStr = request.getAttribute("password");
                
                persona.gatewayRegister(Interop.ServiceName.valueOf(serviceStr), nameStr, pwStr);
            }
            response.addAttribute("result", true);
        }
        
        return response;
    }
}
