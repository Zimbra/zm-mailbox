/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.AndOr;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Term;
import com.zimbra.soap.ZimbraSoapContext;

public class SearchCalendarResources extends AccountDocumentHandler {

    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(AccountConstants.SEARCH_CALENDAR_RESOURCES_RESPONSE);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        String sortBy = request.getAttribute(AccountConstants.A_SORT_BY, null);
        boolean sortAscending = request.getAttributeBool(AccountConstants.A_SORT_ASCENDING, true);
        String attrsStr = request.getAttribute(AccountConstants.A_ATTRS, null);
        String[] attrs = attrsStr == null ? null : attrsStr.split(",");

        EntrySearchFilter filter = parseSearchFilter(request);
        filter.andWith(sFilterActiveResourcesOnly);

        Provisioning prov = Provisioning.getInstance();
        List resources = prov.searchCalendarResources(prov.getDomain(account), filter, attrs, sortBy, sortAscending);
        for (Iterator iter = resources.iterator(); iter.hasNext(); ) {
            CalendarResource resource = (CalendarResource) iter.next();
            ToXML.encodeCalendarResource(response, resource);
        }
        return response;
    }

    public static EntrySearchFilter parseSearchFilter(Element request) throws ServiceException {
        Element filterElem = request.getElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        Element termElem = filterElem.getOptionalElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
        if (termElem == null)
            termElem = filterElem.getElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        Term term = parseFilterTermElem(termElem);
        EntrySearchFilter filter = new EntrySearchFilter(term);

        return filter;
    }

    private static Term parseFilterTermElem(Element termElem)
    throws ServiceException {
        Term term;
        String elemName = termElem.getName();
        boolean negation = termElem.getAttributeBool(AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, false);
        if (elemName.equals(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND)) {
            boolean or = termElem.getAttributeBool(AccountConstants.A_ENTRY_SEARCH_FILTER_OR, false);
            Multi multiTerm = new Multi(negation, or ? AndOr.or : AndOr.and);
            for (Iterator<Element> iter = termElem.elementIterator();
                 iter.hasNext(); ) {
                Term child = parseFilterTermElem(iter.next());
                multiTerm.add(child);
            }
            term = multiTerm;
        } else if (elemName.equals(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND)) {
            String attr = termElem.getAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR);
            if (attr == null)
                throw ServiceException.INVALID_REQUEST("Missing search term attr", null);
            String op = termElem.getAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP);
            if (op == null)
                throw ServiceException.INVALID_REQUEST("Missing search term op", null);
            String value = termElem.getAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE);
            if (value == null)
                throw ServiceException.INVALID_REQUEST("Missing search term value", null);
            term = new Single(negation, attr, op, value);
        } else {
            throw ServiceException.INVALID_REQUEST(
                    "Unknown element <" + elemName + "> in search filter",
                    null);
        }
        return term;
    }

    // When end-user client searches resources, expose only those resources
    // that are in active or maintenance status.  Hide locked and closed
    // resources.
    private static EntrySearchFilter sFilterActiveResourcesOnly;
    static {
        Single active = new Single(
                false,
                Provisioning.A_zimbraAccountStatus,
                Operator.eq,
                Provisioning.ACCOUNT_STATUS_ACTIVE);
        Single maint = new Single(
                false,
                Provisioning.A_zimbraAccountStatus,
                Operator.eq,
                Provisioning.ACCOUNT_STATUS_MAINTENANCE);
        Multi activeOrMaint = new Multi(false, AndOr.or, active, maint);
        sFilterActiveResourcesOnly = new EntrySearchFilter(activeOrMaint);
    }
}
