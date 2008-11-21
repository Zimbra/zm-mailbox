package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.RightBy;
import com.zimbra.cs.account.Right;
import com.zimbra.soap.ZimbraSoapContext;

public class DeleteRight extends RightDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);
        
        Right right = prov.get(RightBy.id, id);
        if (right == null)
            throw AccountServiceException.NO_SUCH_COS(id);
        
        prov.deleteCos(right.getId());

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "DeleteCos","name", right.getName(), "id", right.getId()}));

        Element response = lc.createElement(AdminConstants.DELETE_RIGHT_RESPONSE);
        return response;
    }

}
