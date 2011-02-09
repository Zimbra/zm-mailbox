/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.gal;

import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.EntrySearchFilter.AndOr;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Term;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter.LdapQueryVisitor;
import com.zimbra.cs.mailbox.Contact;

public class GalExtraSearchFilter {

    public static EntrySearchFilter parseSearchFilter(Element request) throws ServiceException {
        Element filterElem = request.getOptionalElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
        if (filterElem == null) {
            return null;
        }
        
        Element termElem = filterElem.getOptionalElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
        if (termElem == null)
            termElem = filterElem.getElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        Term term = GalExtraSearchFilter.parseFilterTermElem(termElem);
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
    
    public static abstract class GalExtraQueryCallback implements GalSearchQueryCallback {
        protected EntrySearchFilter filter;
        
        protected GalExtraQueryCallback(EntrySearchFilter filter) {
            this.filter = filter;
        }
        
        /*
         * Return an extra query for GAL sync account search
         */
        public String getMailboxSearchQuery() {
            GalExtraSearchFilter.MailboxQueryVisitor visitor = new GalExtraSearchFilter.MailboxQueryVisitor();
            filter.traverse(visitor);
            String query = visitor.getFilter();
            return (StringUtil.isNullOrEmpty(query) ? null : query);
        }
        
        public abstract String getZimbraLdapSearchQuery();
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

    public static abstract class FilteredGalSearchResultCallback extends GalSearchResultCallback {
        private Element mProxiedResponse;
        private Set<String> mAttrs;
        private EntrySearchFilter mFilter;
        
        boolean mPagingSupported; // default to false
        
        public FilteredGalSearchResultCallback(GalSearchParams params, EntrySearchFilter filter, Set<String> attrs) {
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
        public void handleElement(Element e) throws ServiceException {
            // should never be called
            throw ServiceException.FAILURE("internal error, method should not be called", null);
        }
        
        @Override
        public void setQueryOffset(int offset) {
            super.setQueryOffset(offset);
            mPagingSupported = true;
        }
        
        protected Set<String> neededAttrs() {
            return mAttrs;
        }
        
        // not used any more, we no longer need to filter search result for 
        // GAL sync account search, because we've supplied an extra search query,
        // and the result should be correct already.
        protected boolean matched(Contact c) {
            GalExtraSearchFilter.FilterVisitor visitor = new GalExtraSearchFilter.FilterVisitor(c);
            return evaluate(visitor);
        }
        
        protected boolean matched(GalContact c) {
            GalExtraSearchFilter.FilterVisitor visitor = new GalExtraSearchFilter.FilterVisitor(c);
            return evaluate(visitor);
        }
        
        private boolean evaluate(GalExtraSearchFilter.FilterVisitor visitor) {
            if (mFilter == null) {
                return true;
            } else {
                mFilter.traverse(visitor);
                return visitor.getResult();
            }
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
        
        public boolean getResult() {
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
