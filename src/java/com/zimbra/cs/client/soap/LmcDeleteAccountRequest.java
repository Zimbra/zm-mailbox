package com.liquidsys.coco.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.liquidsys.coco.service.admin.AdminService;
import com.zimbra.soap.DomUtil;

public class LmcDeleteAccountRequest extends LmcSoapRequest {
    String mAccountId;
    
    public LmcDeleteAccountRequest(String accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        mAccountId = accountId;
    }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AdminService.DELETE_ACCOUNT_REQUEST);
        DomUtil.add(request, AdminService.E_ID, mAccountId);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) {
        return new LmcDeleteAccountResponse();
    }
}
