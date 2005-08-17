/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetAccount extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    public static final String BY_ADMIN_NAME = "adminName";
    
	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyCos = request.getAttributeBool(AdminService.A_APPLY_COS, true);        
        Element a = request.getElement(AdminService.E_ACCOUNT);
	    String key = a.getAttribute(AdminService.A_BY);
        String value = a.getText();

	    Account account = null;

        if (key.equals(BY_NAME)) {
            account = prov.getAccountByName(value);
        } else if (key.equals(BY_ID)) {
            account = prov.getAccountById(value);
        } else if (key.equals(BY_ADMIN_NAME)) {
            account = prov.getAdminAccountByName(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

	    Element response = lc.createElement(AdminService.GET_ACCOUNT_RESPONSE);
        doAccount(response, account, applyCos);

	    return response;
	}

    public static void doAccount(Element e, Account a) throws ServiceException {
        doAccount(e, a, true);
    }
    
    public static void doAccount(Element e, Account a, boolean applyCos) throws ServiceException {
        Element account = e.addElement(AdminService.E_ACCOUNT);
        account.addAttribute(AdminService.A_NAME, a.getName());
        account.addAttribute(AdminService.A_ID, a.getId());        
        Map attrs = a.getAttrs(false, applyCos);
        doAttrs(account, attrs);
    }
    
    private static void doAttrs(Element e, Map attrs) throws ServiceException {
         for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            // TODO: might be being too paranoid, but there doesn't seem like a good reason to return this
            if (name.equals(Provisioning.A_userPassword))
                value = "VALUE-BLOCKED";
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    e.addAttribute(name, sv[i], Element.DISP_ELEMENT);
            } else if (value instanceof String)
                e.addAttribute(name, (String) value, Element.DISP_ELEMENT);
        }       
    }
}