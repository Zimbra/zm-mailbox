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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.zimlet.ZimletProperty;
import com.zimbra.cs.zimlet.ZimletUserProperties;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetInfo extends AccountDocumentHandler  {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
		
        Element response = lc.createElement(AccountConstants.GET_INFO_RESPONSE);
        response.addAttribute(AccountConstants.E_ID, acct.getId(), Element.DISP_CONTENT);
        response.addAttribute(AccountConstants.E_NAME, acct.getName(), Element.DISP_CONTENT);
        long lifetime = lc.getAuthToken().getExpires() - System.currentTimeMillis();
        response.addAttribute(AccountConstants.E_LIFETIME, lifetime, Element.DISP_CONTENT);
        try {
            response.addAttribute(AccountConstants.E_QUOTA_USED, getRequestedMailbox(lc).getSize(), Element.DISP_CONTENT);
        } catch (ServiceException e) { }

        Map attrMap = acct.getAttrs();
        // take this out when client is updated
        //doPrefs(response, attrMap);
        
        Locale locale = Provisioning.getInstance().getLocale(acct);
        
        Element prefs = response.addUniqueElement(AccountConstants.E_PREFS);
        GetPrefs.doPrefs(acct, locale.toString(), prefs, attrMap, null);
        Element attrs = response.addUniqueElement(AccountConstants.E_ATTRS);
        doAttrs(acct, locale.toString(), attrs, attrMap);
        Element zimlets = response.addUniqueElement(AccountConstants.E_ZIMLETS);
        doZimlets(zimlets, acct);
        Element props = response.addUniqueElement(AccountConstants.E_PROPERTIES);
        doProperties(props, acct);
        Element ids = response.addUniqueElement(AccountConstants.E_IDENTITIES);
        doIdentities(ids, acct);
        Element ds = response.addUniqueElement(AccountConstants.E_DATA_SOURCES);
        doDataSources(ds, acct, lc);
        GetAccountInfo.addUrls(response, acct);
        
        return response;
    }

    static void doAttrs(Account acct, String locale, Element response, Map attrsMap) throws ServiceException {
		Set<String> attrList = AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.accountInfo);
        
		if (attrList.contains(Provisioning.A_zimbraLocale)) {
			doAttr(response, Provisioning.A_zimbraLocale, locale);
		}
		for (String key : attrList) {
			Object value = attrsMap.get(key);
			if (!key.equals(Provisioning.A_zimbraLocale))
				doAttr(response, key, value);
		}
	}
    
    static void doAttr(Element response, String key, Object value) {
        if (value instanceof String[]) {
            String sa[] = (String[]) value;
            for (int i = 0; i < sa.length; i++)
                if (sa[i] != null && !sa[i].equals("")) {
                    // FIXME: change to "a"/"n" rather than "attr"/"name"
                    Element pref = response.addElement(AccountConstants.E_ATTR);
                    pref.addAttribute(AccountConstants.A_NAME, key);
                    pref.setText(sa[i]);
                }
        } else {
            if (value != null && !value.equals("")) {
                Element pref = response.addElement(AccountConstants.E_ATTR);
                pref.addAttribute(AccountConstants.A_NAME, key);
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
    		Element elem = response.addElement(AccountConstants.E_PROPERTY);
    		elem.addAttribute(AccountConstants.A_ZIMLET, prop.getZimletName());
    		elem.addAttribute(AccountConstants.A_NAME, prop.getKey());
    		elem.setText(prop.getValue());
    	}
    }
    
    private static void doIdentities(Element response, Account acct) {
    	try {
    		List<Identity> identities = Provisioning.getInstance().getAllIdentities(acct);
    		for (Identity i : identities) {
    			ToXML.encodeIdentity(response, i);
    		}
    	} catch (ServiceException se) {
    		ZimbraLog.account.error("can't get identities", se);
    	}
    }
    
    private static void doDataSources(Element response, Account acct, ZimbraSoapContext zsc) {
        try {
            List<DataSource> dataSources = Provisioning.getInstance().getAllDataSources(acct);
            for (DataSource ds : dataSources) {
                com.zimbra.cs.service.mail.ToXML.encodeDataSource(response, ds);
            }
        } catch (ServiceException se) {
            ZimbraLog.mailbox.error("Unable to get data sources", se);
        }
    }
}
