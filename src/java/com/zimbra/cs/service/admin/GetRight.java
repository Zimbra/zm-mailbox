package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.soap.ZimbraSoapContext;


public class GetRight extends RightDocumentHandler {

public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        // String rightBy = eRight.getAttribute(AdminConstants.A_BY);
        String value = eRight.getText();

        Right right = RightManager.getInstance().getRight(value);
        
        if (right == null)
            throw AccountServiceException.NO_SUCH_RIGHT(value);
        
        Element response = zsc.createElement(AdminConstants.GET_RIGHT_RESPONSE);
        RightCommand.rightToXML(response, right);

        return response;
    }

    
}
