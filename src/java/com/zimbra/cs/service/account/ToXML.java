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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class ToXML {

    public static Element encodeAccount(
        Element parent,
        Account account,
        boolean applyCos)
    throws ServiceException {
        Element acctElem = parent.addElement(AccountService.E_ACCOUNT);
        acctElem.addAttribute(AccountService.A_NAME, account.getName());
        acctElem.addAttribute(AccountService.A_ID, account.getId());        
        Map attrs = account.getAttrs(applyCos);
        addAccountAttrs(acctElem, attrs);
        return acctElem;
    }

    public static Element encodeAccount(
        Element parent,
        Account account)
    throws ServiceException {
        return encodeAccount(parent, account, true);
    }

    public static Element encodeCalendarResource(
        Element parent,
        CalendarResource resource,
        boolean applyCos)
    throws ServiceException {
        Element resElem = parent.addElement(AccountService.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountService.A_NAME, resource.getName());
        resElem.addAttribute(AccountService.A_ID, resource.getId());        
        Map attrs = resource.getAttrs(applyCos);
        addAccountAttrs(resElem, attrs);
        return resElem;
    }

    public static Element encodeCalendarResource(
        Element parent,
        CalendarResource resource)
    throws ServiceException {
        return encodeCalendarResource(parent, resource, false);
    }

    private static void addAccountAttrs(Element e, Map attrs) {
        for (Iterator iter = attrs.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Entry) iter.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            // Never return password.
            if (name.equals(Provisioning.A_userPassword))
                value = "VALUE-BLOCKED";

            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    Element pref = e.addElement(AccountService.E_A);
                    pref.addAttribute(AccountService.A_N, name);
                    pref.setText(sv[i]);
                }
            } else if (value instanceof String) {
                Element pref = e.addElement(AccountService.E_A);
                pref.addAttribute(AccountService.A_N, name);
                pref.setText((String) value);
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
            Element elem = parent.addElement(AccountService.E_ENTRY_SEARCH_FILTER_SINGLECOND);
            if (mRootElement == null) mRootElement = elem;
            if (term.isNegation())
                elem.addAttribute(AccountService.A_ENTRY_SEARCH_FILTER_NEGATION, true);
            elem.addAttribute(AccountService.A_ENTRY_SEARCH_FILTER_ATTR, term.getLhs());
            elem.addAttribute(AccountService.A_ENTRY_SEARCH_FILTER_OP, term.getOperator().toString());
            elem.addAttribute(AccountService.A_ENTRY_SEARCH_FILTER_VALUE, term.getRhs());
        }

        public void enterMulti(Multi term) {
            Element parent = mParentStack.peek();
            Element elem = parent.addElement(AccountService.E_ENTRY_SEARCH_FILTER_MULTICOND);
            if (mRootElement == null) mRootElement = elem;
            if (term.isNegation())
                elem.addAttribute(AccountService.A_ENTRY_SEARCH_FILTER_NEGATION, true);
            if (!term.isAnd())
                elem.addAttribute(AccountService.A_ENTRY_SEARCH_FILTER_OR, true);
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

    public static Element encodeLocale(Element parent, Locale locale) {
        Element e = parent.addElement(AccountService.E_LOCALE);
        // Always use US English for locale's display name.
        e.addAttribute(AccountService.A_NAME, locale.getDisplayName(Locale.US));
        e.addAttribute(AccountService.A_ID, locale.toString());
        return e;
    }
}
