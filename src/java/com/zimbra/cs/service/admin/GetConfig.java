/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetConfig extends AdminDocumentHandler {
    
	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();

        Element a = request.getElement(AdminService.E_A);
	    String name = a.getAttribute(AdminService.A_N);

        String value[] = prov.getConfig().getMultiAttr(name);

	    Element response = lc.createElement(AdminService.GET_CONFIG_RESPONSE);
        doConfig(response, name, value);

	    return response;
	}

	public static void doConfig(Element e, String name, String[] value) throws ServiceException {
	    if (value == null)
	        return;
	    for (int i = 0; i < value.length; i++)
            e.addAttribute(name, value[i], Element.DISP_ELEMENT);
    }

	public static void doConfig(Element e, String name, String value) throws ServiceException {
	    if (value == null)
	        return;
        e.addAttribute(name, value, Element.DISP_ELEMENT);
    }
}
