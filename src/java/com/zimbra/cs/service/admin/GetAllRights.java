package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllRights extends RightDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        
        String targetType = request.getAttribute(AdminConstants.A_TARGET_TYPE, null);
        boolean expandAllAtrts = request.getAttributeBool(AdminConstants.A_EXPAND_ALL_ATRTS, false);
        List<Right> rights = RightCommand.getAllRights(targetType);
        
        Element response = lc.createElement(AdminConstants.GET_ALL_RIGHTS_RESPONSE);
        for (Right right : rights)
            RightCommand.rightToXML(response, right, expandAllAtrts);
        
        return response;
    }

    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(sDocRightNotesAllowAllAdmins);
    }
}
