/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.account.Domain;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetAllDomains extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {
	    
        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

        List domains = prov.getAllDomains();
        
        Element response = lc.createElement(AdminService.GET_ALL_DOMAINS_RESPONSE);        
        for (Iterator it = domains.iterator(); it.hasNext(); )
            GetDomain.doDomain(response, (Domain) it.next());

	    return response;
	}
}
