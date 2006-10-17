/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.mailbox.Identity;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.cs.zimlet.ZimletProperty;
import com.zimbra.cs.zimlet.ZimletUserProperties;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetInfo extends AccountDocumentHandler  {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
		
        Element response = lc.createElement(AccountService.GET_INFO_RESPONSE);
        response.addAttribute(AccountService.E_ID, acct.getId(), Element.DISP_CONTENT);        
        response.addAttribute(AccountService.E_NAME, acct.getName(), Element.DISP_CONTENT);
        long lifetime = lc.getAuthToken().getExpires() - System.currentTimeMillis();
        response.addAttribute(AccountService.E_LIFETIME, lifetime, Element.DISP_CONTENT);
        try {
            response.addAttribute(AccountService.E_QUOTA_USED, getRequestedMailbox(lc).getSize(), Element.DISP_CONTENT);
        } catch (ServiceException e) { }

        Map attrMap = acct.getAttrs();
        // take this out when client is updated
        //doPrefs(response, attrMap);
        Element prefs = response.addUniqueElement(AccountService.E_PREFS);
        doPrefs(prefs, attrMap);
        Element attrs = response.addUniqueElement(AccountService.E_ATTRS);
        doAttrs(attrs, attrMap);
        Element zimlets = response.addUniqueElement(AccountService.E_ZIMLETS);
        doZimlets(zimlets, acct);
        Element props = response.addUniqueElement(AccountService.E_PROPERTIES);
        doProperties(props, acct);
        Element ids = response.addUniqueElement(AccountService.E_IDENTITIES);
        doIdentities(ids, acct, lc);
        GetAccountInfo.addUrls(response, acct);
        
        return response;
    }

    private static void doPrefs(Element prefs, Map attrsMap) {
        for (Iterator mi = attrsMap.entrySet().iterator(); mi.hasNext(); ) {
            Map.Entry entry = (Entry) mi.next();
            String key = (String) entry.getKey();
            if (!key.startsWith("zimbraPref"))
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

    static void doAttrs(Element response, Map attrsMap) throws ServiceException {
        Set<String> attrList = AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.accountInfo);
        for (String key : attrList) {
            Object value = attrsMap.get(key);
            doAttr(response, key, value);
        }
    }
    
    static void doAttr(Element response, String key, Object value) {
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

    private static void doZimlets(Element response, Account acct) {
    	String[] attrList = acct.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
    	List<Zimlet> zimletList = ZimletUtil.orderZimletsByPriority(attrList);
    	int priority = 0;
    	for (Zimlet z : zimletList) {
			if (z.isEnabled() && !z.isExtension()) {
				ZimletUtil.listZimlet(response, z.getName(), priority);
			}
    		priority++;
    	}

    	// load the zimlets in the dev directory and list them
    	ZimletUtil.listDevZimlets(response);
    }

    private static void doProperties(Element response, Account acct) {
    	ZimletUserProperties zp = ZimletUserProperties.getProperties(acct);
    	Set props = zp.getAllProperties();
    	Iterator iter = props.iterator();
    	while (iter.hasNext()) {
    		ZimletProperty prop = (ZimletProperty) iter.next();
    		Element elem = response.addElement(AccountService.E_PROPERTY);
    		elem.addAttribute(AccountService.A_ZIMLET, prop.getZimletName());
    		elem.addAttribute(AccountService.A_NAME, prop.getKey());
    		elem.setText(prop.getValue());
    	}
    }
    
    private static void doIdentities(Element response, Account acct, ZimbraSoapContext lc) {
    	try {
    		List<Identity> identities = Identity.get(acct, lc.getOperationContext());
    		for (Identity i : identities) {
    			ToXML.encodeIdentity(response, i);
    		}
    	} catch (ServiceException se) {
    		ZimbraLog.account.error("can't get identities", se);
    	}
    }
}
