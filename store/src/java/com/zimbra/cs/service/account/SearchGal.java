/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.gal.GalExtraSearchFilter;
import com.zimbra.cs.gal.GalExtraSearchFilter.GalExtraQueryCallback;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.gal.GalSearchConfig.GalType;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchQueryCallback;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.MemberOfSelector;
import com.zimbra.soap.type.GalSearchType;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class SearchGal extends GalDocumentHandler {

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
    
    private static Element searchGal(ZimbraSoapContext zsc, Account account, Element request) throws ServiceException {
        
        // if search by ref is requested, honor it
        String ref = request.getAttribute(AccountConstants.A_REF, null);
        
        String galSearchQuery = null;
        
        String name = null;
        
	if (ref == null) {
		galSearchQuery = request.getAttribute(MailConstants.A_QUERY, null);
		name = request.getAttribute(AccountConstants.E_NAME, null);
		if (galSearchQuery != null && name != null) {
			throw ServiceException.INVALID_REQUEST("only one of the following attributes are allowed: "
				+ MailConstants.A_QUERY + " or " + AccountConstants.E_NAME, null);
		}
		if (StringUtil.isNullOrEmpty(galSearchQuery) && name == null) {
			throw ServiceException.INVALID_REQUEST("missing one of the required attributes: "
				+ AccountConstants.A_REF + ", " + MailConstants.A_QUERY + " or " + AccountConstants.E_NAME,
				null);
		}
	}
        
        EntrySearchFilter filter = GalExtraSearchFilter.parseSearchFilter(request);
                
        String typeStr = request.getAttribute(AccountConstants.A_TYPE, "all");
        GalSearchType type = GalSearchType.fromString(typeStr);
        boolean needCanExpand = request.getAttributeBool(AccountConstants.A_NEED_EXP, false);
        boolean needIsOwner = request.getAttributeBool(AccountConstants.A_NEED_IS_OWNER, false);
        MemberOfSelector needIsMember = MemberOfSelector.fromString(
                request.getAttribute(AccountConstants.A_NEED_IS_MEMBER, MemberOfSelector.none.name()));
        
        // internal attr, for proxied GSA search from GetSMIMEPublicCerts only
        boolean needSMIMECerts = request.getAttributeBool(AccountConstants.A_NEED_SMIME_CERTS, false);
        
        String galAcctId = request.getAttribute(AccountConstants.A_GAL_ACCOUNT_ID, null);
        
        GalSearchParams params = new GalSearchParams(account, zsc);
        
        if (ref != null) {
            // search GAL by ref, which is a dn
            params.setSearchEntryByDn(ref);
        } else if (!StringUtil.isNullOrEmpty(galSearchQuery)) {
        	// search GAL by query
        	params.setGalSearchQuery(galSearchQuery);
        } else if (name != null) {
        	// search GAL by name
        	params.setQuery(name);
        }
        params.setType(type);
        params.setRequest(request);
        params.setNeedCanExpand(needCanExpand);
        params.setNeedIsOwner(needIsOwner);
        params.setNeedIsMember(needIsMember);
        params.setNeedSMIMECerts(needSMIMECerts);
        params.setResponseName(AccountConstants.SEARCH_GAL_RESPONSE);

        if (galAcctId != null) {
            params.setGalSyncAccount(Provisioning.getInstance().getAccountById(galAcctId));
        }
        
        if (filter != null) {
            params.setExtraQueryCallback(new SearchGalExtraQueryCallback(filter));
        }
        
        // limit for GAL sync account search is taken from the request
        // set a default if it is not specified in the request
        // otherwise mailbox search defaults to 30
        // also note that mailbox search has a hard limit of 1000
        if (request.getAttribute(MailConstants.A_QUERY_LIMIT, null) == null) {
            request.addAttribute(MailConstants.A_QUERY_LIMIT, 100);
        }
        
        /* do not support specified attrs yet
        String attrsStr = request.getAttribute(AccountConstants.A_ATTRS, null);
        String[] attrs = attrsStr == null ? null : attrsStr.split(",");
        Set<String> attrsSet = attrs == null ? null : new HashSet<String>(Arrays.asList(attrs));
        */
        
        params.setResultCallback(new SearchGalResultCallback(params, filter, null));

        GalSearchControl gal = new GalSearchControl(params);
        gal.search();
        return params.getResultCallback().getResponse();
    }
    
    private static class SearchGalResultCallback extends GalExtraSearchFilter.FilteredGalSearchResultCallback {
        
        private SearchGalResultCallback(GalSearchParams params, EntrySearchFilter filter, Set<String> attrs) {
            super(params, filter, attrs);
        }
    }
    
    private static class SearchGalExtraQueryCallback extends GalExtraQueryCallback implements GalSearchQueryCallback {

        public SearchGalExtraQueryCallback(EntrySearchFilter filter) {
            super(filter);
        }
        
        /*
         * Return an extra query for Zimbra GAL LDAP search
         * 
         * Each terminal term in the filter is mapped to a query in the generated LDAP query as:
         * term.getLhs() => a named filter in globalconfig zimbraGalLdapFilterDef
         * term.getRhs() => value that will replace the %s in the named filter
         * 
         * To use this method, getRhs() of each terminal term in the filter must be 
         * a named filter in globalconfig zimbraGalLdapFilterDef.
         */
        public String getZimbraLdapSearchQuery() {
            GenLdapQueryByNamedFilterVisitor visitor = new GenLdapQueryByNamedFilterVisitor();
            filter.traverse(visitor);
            String query = visitor.getFilter();
            return (StringUtil.isNullOrEmpty(query) ? null : query);
        }
    }
    
    private static class GenLdapQueryByNamedFilterVisitor implements Visitor {
        StringBuilder mLdapFilter;
        boolean mEncounteredError;

        public GenLdapQueryByNamedFilterVisitor() {
            mLdapFilter = new StringBuilder();
        }

        public String getFilter() {
            return mEncounteredError ? null : mLdapFilter.toString();
        }

        public void visitSingle(Single term) {
            
            Operator op = term.getOperator();
            String namedFilter = term.getLhs();
            String value = term.getRhs();
            
            namedFilter = namedFilter + "_" + op.name();
            String filter = null;
            try {
                String filterDef = GalSearchConfig.getFilterDef(namedFilter);
                if (filterDef != null) {
                    filter = GalUtil.expandFilter(null, filterDef, value, null);
                }
            } catch (ServiceException e) { 
                ZimbraLog.gal.warn("cannot find filter def " + namedFilter, e);
            }
            
            if (filter == null) {
                // mark the filter invalid, will return null regardless what other terms are evaluated to.
                mEncounteredError = true;
                return;
            }
                
            boolean negation = term.isNegation();
                
            if (negation) {
                mLdapFilter.append("(!");
            }
            
            mLdapFilter.append(filter);
            
            if (negation) {
                mLdapFilter.append(')');
            }
        }

        
        public void enterMulti(Multi term) {
            if (term.isNegation()) mLdapFilter.append("(!");
            if (term.getTerms().size() > 1) {
                if (term.isAnd())
                    mLdapFilter.append("(&");
                else
                    mLdapFilter.append("(|");
            }
        }

        public void leaveMulti(Multi term) {
            if (term.getTerms().size() > 1) {
                mLdapFilter.append(')');
            }
            if (term.isNegation()) {
                mLdapFilter.append(')');
            }
        }
    }

    public static Map<String, String> getQueries(Domain domain, GalSearchParams params, GalSearchType type, EntrySearchFilter filter, ZimbraSoapContext zsc) throws ServiceException {
        // set default values in params
        params.setType(type);
        params.setQuery("");
        params.setOp(GalOp.search);
        params.setNeedCanExpand(true);
        params.setNeedIsMember(MemberOfSelector.none);
        params.setNeedIsOwner(false);
        params.setNeedSMIMECerts(false);
        params.setWildCardSearch(true);
        params.setQuery(domain.getName());
        params.setToken(domain.getGalTokenizeSearchKeyAsString());
        if (filter != null) {
            params.setExtraQueryCallback(new SearchGalExtraQueryCallback(filter));
        }

        Map<String, String> map = new HashMap<String, String>();
        GalSearchControl galSearchControl = new GalSearchControl(params);

        String galQuery = galSearchControl.getGalQuery();
        if (!StringUtil.isNullOrEmpty(galQuery)) {
            ZimbraLog.addresslist.debug("Gal Query: %s", galQuery);
            map.put(Provisioning.A_zimbraAddressListGalFilter, galQuery);
        }

        params.createSearchConfig(GalType.zimbra);
        String ldapQuery = params.generateLdapQuery();
        if (!StringUtil.isNullOrEmpty(ldapQuery)) {
            ZimbraLog.addresslist.debug("Ldap Query: %s", ldapQuery);
            map.put(Provisioning.A_zimbraAddressListLdapFilter, ldapQuery);
        }

        return map;
    }
}
