package com.liquidsys.coco.client.soap;

import java.util.HashMap;
import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.liquidsys.coco.service.account.AccountService;
import com.liquidsys.coco.service.ServiceException;


public class LmcGetInfoRequest extends LmcSoapRequest {


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountService.GET_INFO_REQUEST);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        HashMap prefMap = new HashMap();
        LmcGetInfoResponse response = new LmcGetInfoResponse();

        // iterate over all the elements we received
        for (Iterator it = responseXML.elementIterator(); it.hasNext(); ) {
            Element e = (Element) it.next();

            // find out what element it is and go process that
            String elementType = e.getQName().getName();
            if (elementType.equals(AccountService.E_NAME)) {
                response.setAcctName(e.getText());
            } else if (elementType.equals(AccountService.E_LIFETIME)) {
                response.setLifetime(e.getText());
            } else if (elementType.equals(AccountService.E_PREF)) {
                // add this preference to our map
                addPrefToMultiMap(prefMap, e);
            }
        }

        if (!prefMap.isEmpty()) 
            response.setPrefMap(prefMap);

        return response;
    }

}
