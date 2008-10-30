package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class RevokePermission extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(AdminConstants.REVOKE_PERMISSION_RESPONSE);
        return response;
    }

}
