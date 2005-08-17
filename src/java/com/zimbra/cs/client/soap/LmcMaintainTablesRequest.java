package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.cs.service.admin.AdminService;


public class LmcMaintainTablesRequest extends LmcSoapRequest {

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AdminService.MAINTAIN_TABLES_REQUEST);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
    {
        String s = responseXML.attributeValue(AdminService.A_NUM_TABLES);
        int numTables = Integer.parseInt(s);
        return new LmcMaintainTablesResponse(numTables);
    }
}
