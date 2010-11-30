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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
import com.zimbra.cs.account.EntrySearchFilter.AndOr;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Term;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter.LdapQueryVisitor;
import com.zimbra.cs.gal.GalSearchQueryCallback;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
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
    
    private static Element searchGal(ZimbraSoapContext zsc, Account account, Element request) throws ServiceException {
        
        Element name = request.getOptionalElement(AccountConstants.E_NAME);

        EntrySearchFilter filter = parseSearchFilter(request);
        
        GalSearchParams params = new GalSearchParams(account, zsc);
        params.setQuery(name == null ? null : name.getText());
        params.setExtraQueryCallback(new CalendarResourceQueryCallback(filter));
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
    
    private static class CalendarResourceGalSearchResultCallback extends GalSearchResultCallback {
        Element mProxiedResponse;
        Set<String> mAttrs;
        EntrySearchFilter mFilter;
        
        boolean mPagingSupported; // default to false
        
        public CalendarResourceGalSearchResultCallback(GalSearchParams params, EntrySearchFilter filter, Set<String> attrs) {
            super(params);
            mAttrs = attrs;
            mFilter = filter;
        }

        @Override
        public boolean passThruProxiedGalAcctResponse() {
            return true;
        }
        
        @Override
        public void handleProxiedResponse(Element resp) {
            mProxiedResponse = resp;
            mProxiedResponse.detach();
        }
        
        @Override
        public Element getResponse() {
            if (mProxiedResponse != null)
                return mProxiedResponse;
            else {
                Element resp = super.getResponse();
                resp.addAttribute(AccountConstants.A_PAGINATION_SUPPORTED, mPagingSupported);
                return super.getResponse();
            }
        }
        
        @Override
        public Element handleContact(Contact contact) throws ServiceException {
            com.zimbra.cs.service.account.ToXML.encodeCalendarResource(getResponse(), 
                    mFormatter.formatItemId(contact), contact.get(ContactConstants.A_email), 
                    contact.getAllFields(), mAttrs, null);
            return null; // return null because we don't want the sort field (sf) attr added to each hit
        }
        
        @Override
        public void handleContact(GalContact galContact) throws ServiceException {
            /*
             * entries found in Zimbra GAL is already filtered by the extra search query
             * entries found in external GAL needs to be filtered for the exta crteria
             */
            if (galContact.isZimbraGal() || matched(galContact)) {
                com.zimbra.cs.service.account.ToXML.encodeCalendarResource(getResponse(), 
                        galContact.getId(), galContact.getSingleAttr(ContactConstants.A_email), 
                        galContact.getAttrs(), mAttrs, null);
            }
        }
        
        @Override
        public void handleElement(Element e) throws ServiceException {
            // should never be called
        }
        
        
        @Override
        public void setQueryOffset(int offset) {
            super.setQueryOffset(offset);
            mPagingSupported = true;
        }
        
        // not used any more, we no longer need to filter search result for 
        // GAL sync account search, because we've supplied an extra search query,
        // and the result should be correct already.
        private boolean matched(Contact c) {
            FilterVisitor visitor = new FilterVisitor(c);
            return evaluate(visitor);
        }
        
        private boolean matched(GalContact c) {
            FilterVisitor visitor = new FilterVisitor(c);
            return evaluate(visitor);
        }
        
        private boolean evaluate(FilterVisitor visitor) {
            mFilter.traverse(visitor);
            return visitor.getResult();
        }
    }
    
    private static class CalendarResourceQueryCallback implements GalSearchQueryCallback {
        private EntrySearchFilter filter;
        
        private CalendarResourceQueryCallback(EntrySearchFilter filter) {
            this.filter = filter;
        }
        
        /*
         * Supply an extra query for GAL sync account search
         */
        public String getMailboxSearchQuery() {
            MailboxQueryVisitor visitor = new MailboxQueryVisitor();
            filter.traverse(visitor);
            String query = visitor.getFilter();
            return (StringUtil.isNullOrEmpty(query) ? null : query);
        }
        
        /*
         * Supply an extra query for Zimbra GAL LDAP search
         */
        public String getZimbraLdapSearchQuery() {
            LdapQueryVisitor visitor = new LdapQueryVisitor();
            filter.traverse(visitor);
            String query = visitor.getFilter();
            return (StringUtil.isNullOrEmpty(query) ? null : query);
        }
    }
    
    private static class MailboxQueryVisitor implements Visitor {
        private Stack<Multi> parentTerms;
        private StringBuilder query;

        public MailboxQueryVisitor() {
            parentTerms = new Stack<Multi>();
            query = new StringBuilder();
        }

        public String getFilter() {
            return query.toString();
        }

        public void visitSingle(Single term) {
            Multi parent = parentTerms.peek();
            if (parent != null && parent.getTerms().size() > 1) {
                // add logical operator if this is not the first child term
                if (!parent.getTerms().get(0).equals(term)) {
                    if (parent.isAnd())
                        query.append(" AND ");
                    else
                        query.append(" OR ");
                }
            }
                
            Operator op = term.getOperator();
            boolean negation = term.isNegation();

            if (negation) query.append("-");

            String attr = term.getLhs();
            String val = term.getRhs();
            if (op.equals(Operator.has)) {
                query.append('#').append(attr).append(":(*").append(val).append("*)");
            } else if (op.equals(Operator.eq)) {
                query.append('#').append(attr).append(":(").append(val).append(")");
            } else if (op.equals(Operator.ge)) {
                query.append('#').append(attr).append(":>=").append(val);
            } else if (op.equals(Operator.le)) {
                query.append('#').append(attr).append(":<=").append(val);
            } else if (op.equals(Operator.gt)) {
                query.append('#').append(attr).append(":>").append(val);
            } else if (op.equals(Operator.lt)) {
                query.append('#').append(attr).append(":<").append(val);
            } else if (op.equals(Operator.startswith)) {
                query.append('#').append(attr).append(":(").append(val).append("*)");
            } else if (op.equals(Operator.endswith)) {
                query.append('#').append(attr).append(":(*").append(val);
            } else {
                // fallback to EQUALS
                query.append('#').append(attr).append(":(").append(val).append(")");
            }
        }

        public void enterMulti(Multi term) {
            parentTerms.push(term);
            if (term.isNegation()) 
                query.append("-(");
        }

        public void leaveMulti(Multi term) {
            if (term.isNegation()) 
                query.append(')');
            
            parentTerms.remove(term);
        }
    }
    
    private static class FilterVisitor implements Visitor {
        private static interface KeyValue {
            // returns a String or String[]
            public Object get(String key);
        }
        
        private static class ContactKV implements KeyValue {
            
            Contact mContact;
            
            private ContactKV(Contact contact) {
                mContact = contact;
            }
            
            public Object get(String key) {
                return mContact.get(key);
            }
        }
        
        private static class GalContactKV implements KeyValue {
            
            GalContact mGalContact;
            
            private GalContactKV(GalContact galContact) {
                mGalContact = galContact;
            }
            
            public Object get(String key) {
                return mGalContact.getAttrs().get(key);
            }
        }
        
        private static class Result {
            Multi mTerm;
            Boolean mCurResult;
            
            private Result(Multi term) {
                mTerm = term;
            }

            private Result(boolean result) {
                setResult(result);
            }
            
            Multi getTerm() {
                return mTerm;
            }
            private Boolean getResult() {
                return mCurResult;
            }
            
            private void setResult(boolean result) {
                mCurResult = result;
            }
            
            private void negateResult() {
                if (mCurResult != null)
                    mCurResult = !mCurResult;
            }
            
        }
        
        KeyValue mContact;
        Stack<Result> mParentResult;
        
        private FilterVisitor(Contact contact) {
            mContact = new ContactKV(contact);
            mParentResult = new Stack<Result>();
        }
        
        private FilterVisitor(GalContact galContact) {
            mContact = new GalContactKV(galContact);
            mParentResult = new Stack<Result>();
        }
        
        boolean getResult() {
            // there should one and only one item in the stack
            return mParentResult.pop().getResult().booleanValue();
        }
        @Override
        public void enterMulti(Multi term) {
            mParentResult.push(new Result(term));
        }

        @Override
        public void leaveMulti(Multi term) {
            // we must have a result by now
            Result thisTerm = mParentResult.pop(); // this is us
            if (thisTerm.getTerm().isNegation()) {
                thisTerm.negateResult();
            }
                
            // propagate this Term's result to its parent if there is one
            if (!mParentResult.empty()) {
                // have a parent
                Result parent = mParentResult.peek();
                Boolean parentResult = parent.getResult();
                if (parentResult == null || // we are the first child
                    (parentResult == Boolean.TRUE  && parent.getTerm().isAnd()) ||
                    (parentResult == Boolean.FALSE && !parent.getTerm().isAnd())) {
                    parent.setResult(thisTerm.getResult());
                }
            } else {
                // we are the top, push it back on the stack
                mParentResult.push(thisTerm);
            }
        }

        @Override
        public void visitSingle(Single term) {
            if (!mParentResult.empty()) {
                // have a parent
                Result parent = mParentResult.peek();
                Boolean parentResult = parent.getResult();
                if (parentResult == null || // we are the first child
                    (parentResult == Boolean.TRUE  && parent.getTerm().isAnd()) ||
                    (parentResult == Boolean.FALSE && !parent.getTerm().isAnd())) {
                    parent.setResult(evaluate(term));
                }
                // short-circuit it, no need to evaluate this single term, 
                // since it cannot affect the final result if we are here
            } else {
                // no parent, we are the only Term, evaluate and 
                // remember the result (push to the stack)
                mParentResult.push(new Result(evaluate(term)));
            }
        }
        
        private boolean evaluate(Single term) {
            String opAttr = term.getLhs();
            
            Object value = mContact.get(opAttr);
            if (value instanceof String[]) {
                for (String v : (String[])value) {
                    if (shouldInclude(term, v))
                        return true;
                }
                return false;
            } else if (value != null) {
                return shouldInclude(term, value.toString());
            } else {
                return false;
            }
        }
        
        private boolean shouldInclude(Single term, String value) {
            Operator op = term.getOperator();
            String opVal = term.getRhs();
            boolean result = true;
            
            if (op.equals(Operator.has)) {
                result = (value == null) ? false : value.toLowerCase().contains(opVal.toLowerCase());
            } else if (op.equals(Operator.eq)) {
                result = (value == null) ? false : value.toLowerCase().equals(opVal.toLowerCase());
            } else if (op.equals(Operator.ge)) {
                // always use number comparison
                result = (value == null) ? false : Integer.valueOf(value) >= Integer.valueOf(opVal);
            } else if (op.equals(Operator.le)) {
                // always use number comparison
                result = (value == null) ? false : Integer.valueOf(value) <= Integer.valueOf(opVal);
            } else if (op.equals(Operator.startswith)) {
                result = (value == null) ? false : value.toLowerCase().startsWith(opVal.toLowerCase());
            } else if (op.equals(Operator.endswith)) {
                result = (value == null) ? false : value.toLowerCase().endsWith(opVal.toLowerCase());
            } else {
                // fallback to EQUALS
                result = (value == null) ? false : value.toLowerCase().equals(opVal.toLowerCase());
            }

            if (term.isNegation()) 
                return !result;
            else
                return result;
        }
    }

}
