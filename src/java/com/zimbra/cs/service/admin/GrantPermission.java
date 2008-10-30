package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GrantPermission extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(AdminConstants.GRANT_PERMISSION_RESPONSE);
        return response;
    }

}
