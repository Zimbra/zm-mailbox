/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetConfig extends AdminDocumentHandler {
    
	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
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
