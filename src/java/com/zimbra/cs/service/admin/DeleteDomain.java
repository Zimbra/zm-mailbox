/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Domain;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class DeleteDomain extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
	    Map attrs = AdminService.getAttrs(request);

	    Domain domain = prov.getDomainById(id);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(id);
        
        prov.deleteDomain(domain.getId());

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "DeleteDomain","name", domain.getName(), "id", domain.getId()}));

	    Element response = lc.createElement(AdminService.DELETE_DOMAIN_RESPONSE);
	    return response;
	}
}