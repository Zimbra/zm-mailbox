package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 * Does nothing but getting the auth token validated.
 * Used to keep an admin session alive
 */
public class NoOp extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element resp = zsc.createElement(AdminConstants.NO_OP_RESPONSE);
        return resp;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }

}
