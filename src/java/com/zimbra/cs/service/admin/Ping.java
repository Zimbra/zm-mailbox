/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class Ping extends AdminDocumentHandler {

	/* (non-Javadoc)
	 * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
	 */
	public Element handle(Element document, Map context)
			throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
		Element response = lc.createElement(AdminService.PING_RESPONSE);
		return response;
	}
	
	public boolean needsAuth(Map context) {
        // return false because this may be called from Perl which
        // doesn't have auth token
		return false;
	}

    public boolean needsAdminAuth(Map context) {
        // return false because this may be called from Perl which
        // doesn't have auth token
    	return false;
    }
}
