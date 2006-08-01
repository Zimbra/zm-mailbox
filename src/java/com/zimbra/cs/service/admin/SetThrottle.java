package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.operation.Scheduler;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class SetThrottle extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
                throws ServiceException, SoapFaultException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        String concurStr = request.getAttribute(AdminService.A_CONCURRENCY, null);
        
        if (concurStr != null) {
            int[] params = Scheduler.readOpsFromString(concurStr);
            if (params == null) 
                throw ServiceException.INVALID_REQUEST("Could not parse concurrency string: "+concurStr, null);
            Scheduler.setConcurrency(params);    
        }
        
        Element response = lc.createElement(AdminService.SET_THROTTLE_RESPOSNE);
        
        Scheduler s = Scheduler.get(null);
        concurStr = "";
        int[] concur = s.getMaxOps();
        for (int i = 0; i < concur.length; i++) {
            if (i > 0)
                concurStr+=",";
            concurStr += concur[i];
        }
        response.addAttribute(AdminService.A_CONCURRENCY, concurStr);
        
        return response;
    }
    
}
