/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.account;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.GalContact;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class SearchGal extends DocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        String n = request.getAttribute(AccountService.E_NAME);

        LiquidContext lc = getLiquidContext(context);
        Element response = lc.createElement(AccountService.SEARCH_GAL_RESPONSE);
        Account acct = getRequestedAccount(getLiquidContext(context));

        List contacts = acct.getDomain().searchGal(n);
        for (Iterator it = contacts.iterator(); it.hasNext();) {
            GalContact contact = (GalContact) it.next();
            addContact(response, contact);
        }
        return response;
    }

    public boolean needsAuth(Map context) {
        return true;
    }

    public static void addContact(Element response, GalContact contact) throws ServiceException {
        Element cn = response.addElement(MailService.E_CONTACT);
        cn.addAttribute(MailService.A_ID, contact.getId());
        Map attrs = contact.getAttrs();
        for (Iterator it = attrs.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            cn.addAttribute((String) entry.getKey(), (String) entry.getValue(),
                    Element.DISP_ELEMENT);
        }
    }
}