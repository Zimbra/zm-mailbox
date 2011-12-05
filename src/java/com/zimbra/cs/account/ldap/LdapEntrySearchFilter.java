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

package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Term;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

/*
 * Traverse a EntrySearchFilter.Term tree and convert it to LDAP query
 */
public class LdapEntrySearchFilter {

    public static class LdapQueryVisitor implements Visitor {
        StringBuilder mLdapFilter;

        public LdapQueryVisitor() {
            mLdapFilter = new StringBuilder();
        }

        public String getFilter() {
            return mLdapFilter.toString();
        }

        public void visitSingle(Single term) {
            Operator op = term.getOperator();
            boolean negation = term.isNegation();

            // gt = !le, lt = !ge
            if (op.equals(Operator.gt)) {
                op = Operator.le;
                negation = !negation;
            } else if (op.equals(Operator.lt)) {
                op = Operator.ge;
                negation = !negation;
            }

            if (negation) {
                mLdapFilter.append("(!");
            }
            
            ZLdapFilterFactory filterFactory = ZLdapFilterFactory.getInstance();
            FilterId filterId = FilterId.GAL_SEARCH;
            ZLdapFilter filter;
            
            String attr = term.getLhs();
            String val = getVal(term);
            if (op.equals(Operator.has)) {
                filter = filterFactory.substringFilter(filterId, attr, val);
            } else if (op.equals(Operator.eq)) {
                // there is no presence operator in Single
                if (val.equals("*")) {
                    filter = filterFactory.presenceFilter(filterId, attr);
                } else {
                    filter = filterFactory.equalityFilter(filterId, attr, val);
                }
            } else if (op.equals(Operator.ge)) {
                filter = filterFactory.greaterOrEqualFilter(filterId, attr, val);
            } else if (op.equals(Operator.le)) {
                filter = filterFactory.lessOrEqualFilter(filterId, attr, val);
            } else if (op.equals(Operator.startswith)) {
                filter = filterFactory.startsWithFilter(filterId, attr, val);
            } else if (op.equals(Operator.endswith)) {
                filter = filterFactory.endsWithFilter(filterId, attr, val);
            } else {
                // fallback to EQUALS
                filter = filterFactory.equalityFilter(filterId, attr, val);
            }
            
            mLdapFilter.append(filter.toFilterString());

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
            if (term.getTerms().size() > 1) mLdapFilter.append(')');
            if (term.isNegation()) mLdapFilter.append(')');
        }
        
        protected String getVal(Single term) {
            // return LdapUtil.escapeSearchFilterArg(term.getRhs());
            return term.getRhs();
        }
    }
    
    private static class LdapQueryVisitorIDN extends LdapQueryVisitor implements Visitor {
        protected String getVal(Single term) {
            String rhs = term.getRhs();
            
            AttributeManager attrMgr = null;
            try {
                attrMgr = AttributeManager.getInstance();
            } catch (ServiceException e) {
                ZimbraLog.account.warn("failed to get AttributeManager instance", e);
            }
            
            IDNType idnType = AttributeManager.idnType(attrMgr, term.getLhs());
            rhs = IDNUtil.toAscii(rhs, idnType);
            
            return rhs;
        }
    }

    public static EntrySearchFilter sCalendarResourcesFilter;
    static {
        Single calResType = new Single(
                false,
                Provisioning.A_objectClass,
                Operator.eq,
                AttributeClass.OC_zimbraCalendarResource);
        sCalendarResourcesFilter = new EntrySearchFilter(calResType);
    }

    public static String toLdapIDNFilter(String filterStr) {
        String asciiQuery;
        
        try {
            Term term = LdapFilterParser.parse(filterStr); 
            EntrySearchFilter filter = new EntrySearchFilter(term);
            asciiQuery = toLdapIDNFilter(filter);
            ZimbraLog.account.debug("original query=[" + filterStr + "], converted ascii query=[" + asciiQuery + "]");
        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to convert query to ascii, using original query: " + filterStr, e);
            asciiQuery = filterStr;
        }
        
        return asciiQuery;
    }
    
    /*
     * serialize EntrySearchFilter to LDAP filter string
     */
    private static String toLdapIDNFilter(EntrySearchFilter filter)
    throws ServiceException {
        /*
        if (!filter.usesIndex())
            throw ServiceException.INVALID_REQUEST(
                    "Search referring to no indexed attribute is not allowed: " + filter.toString(), null);
        */
        LdapQueryVisitor visitor = new LdapQueryVisitorIDN();
        filter.traverse(visitor);
        return visitor.getFilter();
    }

    public static String toLdapCalendarResourcesFilter(EntrySearchFilter filter)
    throws ServiceException {
        /* objectClass=calendarResource will be prepended in SearchDirectory
        filter.andWith(sCalendarResourcesFilter);
        if (!filter.usesIndex())
            throw ServiceException.INVALID_REQUEST(
                    "Search referring to no indexed attribute is not allowed: " + filter.toString(), null);
        */
        LdapQueryVisitor visitor = new LdapQueryVisitor();
        filter.traverse(visitor);
        return visitor.getFilter();
    }
}
