/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class CreateCos extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
	    
        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    String name = request.getAttribute(AdminService.E_NAME).toLowerCase();
	    Map attrs = AdminService.getAttrs(request, true);
	    
	    Cos cos = prov.createCos(name, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateCos","name", name}, attrs));         

	    Element response = lc.createElement(AdminService.CREATE_COS_RESPONSE);
	    GetCos.doCos(response, cos);

	    return response;
	}
}