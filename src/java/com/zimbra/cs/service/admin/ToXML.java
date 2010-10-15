/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.AttributeManager.IDNType;

public class ToXML {
    public static Element encodeAccount(Element parent, Account account) {
        return encodeAccount(parent, account, true, null, null);
    }
    
    public static Element encodeAccount(Element parent, Account account, boolean applyCos) {
        return encodeAccount(parent, account, applyCos, null, null);
    }

    public static Element encodeAccount(Element parent, Account account, boolean applyCos, 
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        return encodeAccount(parent, account, 
                applyCos, false, reqAttrs,  attrRightChecker);
    }
    
    public static Element encodeAccount(Element parent, Account account, 
            boolean applyCos, boolean needsExternalIndicator,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element acctElem = parent.addElement(AccountConstants.E_ACCOUNT);
        acctElem.addAttribute(AccountConstants.A_NAME, account.getUnicodeName());
        acctElem.addAttribute(AccountConstants.A_ID, account.getId());
        
        if (needsExternalIndicator) {
            try {
                boolean isExternal = account.isAccountExternal();
                acctElem.addAttribute(AccountConstants.A_isExternal, isExternal);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to determine if account is external", e);
            }
        }
        Map attrs = account.getUnicodeAttrs(applyCos);
        encodeAttrs(acctElem, attrs, AdminConstants.A_N, reqAttrs, attrRightChecker);
        return acctElem;
    }

    public static Element encodeCalendarResource(Element parent, CalendarResource resource) {
        return encodeCalendarResource(parent, resource, false, null, null);
    }
    
    public static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos) {
        return encodeCalendarResource(parent, resource, applyCos, null, null);
    }
    
    public static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos, 
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element resElem = parent.addElement(AccountConstants.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountConstants.A_NAME, resource.getUnicodeName());
        resElem.addAttribute(AccountConstants.A_ID, resource.getId());
        Map attrs = resource.getUnicodeAttrs(applyCos);
        encodeAttrs(resElem, attrs, AdminConstants.A_N, reqAttrs, attrRightChecker);
        return resElem;
    }

    public static void encodeAttrs(Element e, Map attrs, Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        encodeAttrs(e, attrs, AdminConstants.A_N, reqAttrs, attrRightChecker);
    }
    
    private static void encodeAttrs(Element e, Map attrs, String key, Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        AttributeManager attrMgr = null;
        try {
            attrMgr = AttributeManager.getInstance();
        } catch (ServiceException se) {
            ZimbraLog.account.warn("failed to get AttributeManager instance", se);
        }
        
        for (Iterator iter = attrs.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Entry) iter.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            // Never return data source passwords
            if (name.equalsIgnoreCase(Provisioning.A_zimbraDataSourcePassword))
                continue;

            // Never return password.
            if (name.equalsIgnoreCase(Provisioning.A_userPassword))
                value = "VALUE-BLOCKED";
            
            // only returns requested attrs
            if (reqAttrs != null && !reqAttrs.contains(name))
                continue;
            
            boolean allowed = attrRightChecker == null ? true : attrRightChecker.allowAttr(name);
            
            IDNType idnType = AttributeManager.idnType(attrMgr, name);

            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    encodeAttr(e, name, sv[i], AdminConstants.E_A, key, idnType, allowed);
                }
            } else if (value instanceof String) {
                value = com.zimbra.cs.service.account.ToXML.fixupZimbraPrefTimeZoneId(name, (String)value);
                encodeAttr(e, name, (String)value, AdminConstants.E_A, key, idnType, allowed);
            }
        }       
    }

    public static void encodeAttr(Element parent, String key, String value, String eltname, String attrname, 
            IDNType idnType, boolean allowed) {
        
        Element e = parent.addElement(eltname);
        e.addAttribute(attrname, key);
        
        if (allowed) {
            e.setText(IDNUtil.toUnicode(value, idnType));
        } else {
            e.addAttribute(AccountConstants.A_PERM_DENIED, true);
        }
    }
}
