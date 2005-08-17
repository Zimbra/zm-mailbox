/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.account;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetInfo extends DocumentHandler  {

	/* (non-Javadoc)
	 * @see com.liquidsys.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
	 */
	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
        Account acct = getRequestedAccount(lc);
		
        Element response = lc.createElement(AccountService.GET_INFO_RESPONSE);
        response.addAttribute(AccountService.E_NAME, acct.getName(), Element.DISP_CONTENT);
        long lifetime = lc.getAuthToken().getExpires() - System.currentTimeMillis();
        response.addAttribute(AccountService.E_LIFETIME, lifetime, Element.DISP_CONTENT);
        try {
            response.addAttribute(AccountService.E_QUOTA_USED, getRequestedMailbox(lc).getSize(), Element.DISP_CONTENT);
        } catch (ServiceException e) { }

        Map attrMap = acct.getAttrs(false, true);
        // take this out when client is updated
        //doPrefs(response, attrMap);
        Element prefs = response.addUniqueElement(AccountService.E_PREFS);
        doPrefs(prefs, attrMap);
        Element attrs = response.addUniqueElement(AccountService.E_ATTRS);
        doAttrs(attrs, attrMap);
        return response;
    }

    private static void doPrefs(Element prefs, Map attrsMap) throws ServiceException {
        for (Iterator mi = attrsMap.entrySet().iterator(); mi.hasNext(); ) {
            Map.Entry entry = (Entry) mi.next();
            String key = (String) entry.getKey();
            if (!key.startsWith("liquidPref"))
                continue;
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++) {
                    Element pref = prefs.addElement(AccountService.E_PREF);
                    pref.addAttribute(AccountService.A_NAME, key);
                    pref.setText(sa[i]);
                }
            } else {
                Element pref = prefs.addElement(AccountService.E_PREF);
                pref.addAttribute(AccountService.A_NAME, key);
                pref.setText((String) value);
            }
        }
    }

    private static void doAttrs(Element response, Map attrsMap) throws ServiceException {
        String[] attrList = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_liquidAccountClientAttr);
        for (int attrIndex = 0; attrIndex < attrList.length; attrIndex++) {
            String key = attrList[attrIndex];
            Object value = attrsMap.get(key);
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++)
                    if (sa[i] != null && !sa[i].equals("")) {
                        // FIXME: change to "a"/"n" rather than "attr"/"name"
                        Element pref = response.addElement(AccountService.E_ATTR);
                        pref.addAttribute(AccountService.A_NAME, key);
                        pref.setText(sa[i]);
                    }
            } else {
                if (value != null && !value.equals("")) {
                    Element pref = response.addElement(AccountService.E_ATTR);
                    pref.addAttribute(AccountService.A_NAME, key);
                    pref.setText((String) value);
                }
            }
        }
    }
}
