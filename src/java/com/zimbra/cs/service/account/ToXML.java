/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.service.account;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.account.Signature.SignatureContent;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.common.util.L10nUtil;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Stack;

public class ToXML {
    
    static Element encodeAccount(Element parent, Account account) {
        return encodeAccount(parent, account, true, null, null);
    }

    static Element encodeAccount(Element parent, Account account, boolean applyCos, 
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element acctElem = parent.addElement(AccountConstants.E_ACCOUNT);
        acctElem.addAttribute(AccountConstants.A_NAME, account.getUnicodeName());
        acctElem.addAttribute(AccountConstants.A_ID, account.getId());
        Map attrs = account.getUnicodeAttrs(applyCos);
        encodeAttrs(acctElem, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
        return acctElem;
    }

    static Element encodeCalendarResource(Element parent, CalendarResource resource) {
        return encodeCalendarResource(parent, resource, false, null, null);
    }
    
    static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos) {
        return encodeCalendarResource(parent, resource, applyCos, null, null);
    }
    
    static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos, 
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element resElem = parent.addElement(AccountConstants.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountConstants.A_NAME, resource.getUnicodeName());
        resElem.addAttribute(AccountConstants.A_ID, resource.getId());
        Map attrs = resource.getUnicodeAttrs(applyCos);
        encodeAttrs(resElem, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
        return resElem;
    }
    
    public static Element encodeCalendarResource(Element parent, String id, String name, Map attrs,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element resElem = parent.addElement(AccountConstants.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountConstants.A_NAME, name);
        resElem.addAttribute(AccountConstants.A_ID, id);
        encodeAttrs(resElem, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
        return resElem;
    }

    static void encodeAttrs(Element e, Map attrs, Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        encodeAttrs(e, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
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
                    encodeAttr(e, name, sv[i], AccountConstants.E_A, key, idnType, allowed);
                }
            } else if (value instanceof String) {
                value = fixupZimbraPrefTimeZoneId(name, (String)value);
                encodeAttr(e, name, (String)value, AccountConstants.E_A, key, idnType, allowed);
            }
        }       
    }

    private static class EntrySearchFilterXmlVisitor implements Visitor {
        Stack<Element> mParentStack;
        Element mRootElement;

        public EntrySearchFilterXmlVisitor(Element parent) {
            mParentStack = new Stack<Element>();
            mParentStack.push(parent);
        }

        public Element getRootElement() { return mRootElement; }

        public void visitSingle(Single term) {
            Element parent = mParentStack.peek();
            Element elem = parent.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
            if (mRootElement == null) mRootElement = elem;
            if (term.isNegation())
                elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, true);
            elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, term.getLhs());
            elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, term.getOperator().toString());
            elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, term.getRhs());
        }

        public void enterMulti(Multi term) {
            Element parent = mParentStack.peek();
            Element elem = parent.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
            if (mRootElement == null) mRootElement = elem;
            if (term.isNegation())
                elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, true);
            if (!term.isAnd())
                elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OR, true);
            mParentStack.push(elem);
        }

        public void leaveMulti(Multi term) {
            mParentStack.pop();
        }
    }

    public static Element encodeEntrySearchFilter(Element parent, EntrySearchFilter filter) {
        EntrySearchFilterXmlVisitor visitor = new EntrySearchFilterXmlVisitor(parent);
        filter.traverse(visitor);
        return visitor.getRootElement();
    }

    public static Element encodeLocale(Element parent, Locale locale, Locale inLocale) {
        Element e = parent.addElement(AccountConstants.E_LOCALE);
		String id = locale.toString();
		String name = L10nUtil.getMessage(L10nUtil.L10N_MSG_FILE_BASENAME, id, Locale.getDefault());
		if (name == null)
		    name = locale.getDisplayName(inLocale);
 
		e.addAttribute(AccountConstants.A_ID, id);
		e.addAttribute(AccountConstants.A_NAME, name != null ? name : id);
        return e;
    }

    public static Element encodeIdentity(Element parent, Identity identity) {
        Element e = parent.addElement(AccountConstants.E_IDENTITY);
        e.addAttribute(AccountConstants.A_NAME, identity.getName());
        e.addAttribute(AccountConstants.A_ID, identity.getId());
        encodeAttrs(e, identity.getUnicodeAttrs(), AccountConstants.A_NAME, null, null);
        return e;
    }
    
    public static Element encodeSignature(Element parent, Signature signature) {
        Element e = parent.addElement(AccountConstants.E_SIGNATURE);
        e.addAttribute(AccountConstants.A_NAME, signature.getName());
        e.addAttribute(AccountConstants.A_ID, signature.getId());
        
        Set<SignatureContent> contents = signature.getContents();
        for (SignatureContent c : contents) {
            e.addElement(AccountConstants.E_CONTENT).addAttribute(AccountConstants.A_TYPE, c.getMimeType()).addText(c.getContent());
        }
        
        String contactId = signature.getAttr(Provisioning.A_zimbraPrefMailSignatureContactId);
        if (contactId != null)
            e.addElement(AccountConstants.E_CONTACT_ID).setText(contactId);
        
        return e;
    }

    public static Element encodeDataSource(Element parent, DataSource ds) {
        Element e = parent.addElement(AccountConstants.E_DATA_SOURCE);
        e.addAttribute(AccountConstants.A_NAME, ds.getName());
        e.addAttribute(AccountConstants.A_ID, ds.getId());
        e.addAttribute(AccountConstants.A_TYPE, ds.getType().name());
        encodeAttrs(e, ds.getUnicodeAttrs(), AccountConstants.A_N, null, null);
        return e;
    }
    
    public static void encodeAttr(Element parent, String key, String value, String eltname, String attrname, 
            IDNType idnType, boolean allowed) {
        
        KeyValuePair kvPair;
        if (allowed) {
            kvPair = parent.addKeyValuePair(key, IDNUtil.toUnicode(value, idnType), eltname, attrname);
        } else {
            kvPair = parent.addKeyValuePair(key, "", eltname, attrname);
            kvPair.addAttribute(AccountConstants.A_PERM_DENIED, true);
        }
    }
    
    /**
     * Fixup for time zone id.  Always use canonical (Olson ZoneInfo) ID.
     */
    public static String fixupZimbraPrefTimeZoneId(String attrName, String attrValue) {
        if (Provisioning.A_zimbraPrefTimeZoneId.equals(attrName))
            return TZIDMapper.canonicalize(attrValue);
        else
            return attrValue;
    }
    
}
