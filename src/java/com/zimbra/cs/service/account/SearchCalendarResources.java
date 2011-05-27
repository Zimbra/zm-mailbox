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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter.LdapQueryVisitor;
import com.zimbra.cs.gal.GalExtraSearchFilter;
import com.zimbra.cs.gal.GalExtraSearchFilter.FilteredGalSearchResultCallback;
import com.zimbra.cs.gal.GalExtraSearchFilter.GalExtraQueryCallback;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchQueryCallback;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.soap.ZimbraSoapContext;

public class SearchCalendarResources extends GalDocumentHandler {

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        return searchGal(zsc, account, request);
    }

    private static Element searchGal(ZimbraSoapContext zsc, Account account, Element request) 
    throws ServiceException {
        
        Element name = request.getOptionalElement(AccountConstants.E_NAME);

        EntrySearchFilter filter = GalExtraSearchFilter.parseSearchFilter(request);
        
        GalSearchParams params = new GalSearchParams(account, zsc);
        params.setQuery(name == null ? null : name.getText());
        
        if (filter != null) {
            params.setExtraQueryCallback(new CalendarResourceExtraQueryCallback(filter));
        }
        
        params.setType(Provisioning.GalSearchType.resource);
        params.setRequest(request);
        params.setResponseName(AccountConstants.SEARCH_CALENDAR_RESOURCES_RESPONSE);
        
        // limit for GAL sync account search is taken from the request
        // set a default if it is not specified in the request
        // otherwise mailbox search defaults to 30
        // also note that mailbox search has a hard limit of 1000
        if (request.getAttribute(MailConstants.A_QUERY_LIMIT, null) == null)
            request.addAttribute(MailConstants.A_QUERY_LIMIT, 100);
        
        // set limit for the LDAP search
        // paging is not supported for LDAP search, set a high limit
        params.setLimit(LC.calendar_resource_ldap_search_maxsize.intValue());

        String attrsStr = request.getAttribute(AccountConstants.A_ATTRS, null);
        String[] attrs = attrsStr == null ? null : attrsStr.split(",");
        Set<String> attrsSet = attrs == null ? null : new HashSet<String>(Arrays.asList(attrs));

        params.setResultCallback(new CalendarResourceGalSearchResultCallback(params, filter, attrsSet));

        GalSearchControl gal = new GalSearchControl(params);
        gal.search();
        return params.getResultCallback().getResponse();
    }
    
    private static class CalendarResourceExtraQueryCallback 
    extends GalExtraQueryCallback implements GalSearchQueryCallback {
        public CalendarResourceExtraQueryCallback(EntrySearchFilter filter) {
            super(filter);
        }
        
        /*
         * Return an extra query for Zimbra GAL LDAP search
         * 
         * Each terminal term in the filter is mapped to a term in the generated LDAP query as:
         * ({term.getLhs()} op {term.getRhs()}) 
         * 
         * To use this method, getRhs() of each terminal term in the filter must be 
         * an actual LDAP attribute name 
         */
        public String getZimbraLdapSearchQuery() {
            LdapQueryVisitor visitor = new LdapQueryVisitor();
            filter.traverse(visitor);
            String query = visitor.getFilter();
            return (StringUtil.isNullOrEmpty(query) ? null : query);
        }
    }
    
    private static class CalendarResourceGalSearchResultCallback extends FilteredGalSearchResultCallback {
        
        private CalendarResourceGalSearchResultCallback(GalSearchParams params, EntrySearchFilter filter, Set<String> attrs) {
            super(params, filter, attrs);
        }
        
        @Override
        public Element handleContact(Contact contact) throws ServiceException {
            com.zimbra.cs.service.account.ToXML.encodeCalendarResource(getResponse(), 
                    mFormatter.formatItemId(contact), contact.get(ContactConstants.A_email), 
                    contact.getAllFields(), neededAttrs(), null);
            return null; // return null because we don't want the sort field (sf) attr added to each hit
        }
        
        @Override
        public void handleContact(GalContact galContact) throws ServiceException {
            /*
             * entries found in Zimbra GAL is already filtered by the extra search query
             * entries found in external GAL needs to be filtered for the extra criteria
             */
            if (galContact.isZimbraGal() || matched(galContact)) {
                com.zimbra.cs.service.account.ToXML.encodeCalendarResource(getResponse(), 
                        galContact.getId(), galContact.getSingleAttr(ContactConstants.A_email), 
                        galContact.getAttrs(), neededAttrs(), null);
            }
        }
    }

}
