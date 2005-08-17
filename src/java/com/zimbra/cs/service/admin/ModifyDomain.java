/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class ModifyDomain extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
	    Map attrs = AdminService.getAttrs(request);
	    
	    Domain domain = prov.getDomainById(id);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(id);
        
        // pass in true to checkImmutable
        domain.modifyAttrs(attrs, true);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "ModifyDomain","name", domain.getName()}, attrs));	    

        Element response = lc.createElement(AdminService.MODIFY_DOMAIN_RESPONSE);
	    GetDomain.doDomain(response, domain);
	    return response;
	}
}