/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.account.GalContact;
import com.liquidsys.coco.account.ldap.Check;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.account.SearchGal;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class CheckGalConfig extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);

        Element q = request.getElement(AdminService.E_QUERY);
        String query = q.getText();
        long limit = q.getAttributeLong(AdminService.A_LIMIT, 10);
	    Map attrs = AdminService.getAttrs(request, true);


        Element response = lc.createElement(AdminService.CHECK_GAL_CONFIG_RESPONSE);
        Check.Result r = Check.checkGalConfig(attrs, query, (int)limit);
        
        response.addElement(AdminService.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminService.E_MESSAGE).addText(message);

        List contacts = r.getContacts();
        if (contacts != null) {
            for (Iterator it = contacts.iterator(); it.hasNext();) {
                GalContact contact = (GalContact) it.next();
                SearchGal.addContact(response, contact);
            }
        }
	    return response;
	}
}
