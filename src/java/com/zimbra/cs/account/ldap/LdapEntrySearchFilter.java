/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Term;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;

public class LdapEntrySearchFilter {

    private static class LdapQueryVisitor implements Visitor {
        StringBuilder mLdapFilter;

        public LdapQueryVisitor() {
            mLdapFilter = new StringBuilder();
        }

        public String getFilter() {
            return mLdapFilter.toString();
        }

        private static StringBuilder addCond(StringBuilder sb,
                                             String lhs,
                                             String op,
                                             String rhs) {
            sb.append('(');
            sb.append(lhs).append(op).append(rhs);
            sb.append(')');
            return sb;
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

            if (negation) mLdapFilter.append("(!");

            String attr = term.getLhs();
            String val = getVal(term);
            if (op.equals(Operator.has)) {
                mLdapFilter.append('(').append(attr);
                mLdapFilter.append("=*").append(val).append("*)");
            } else if (op.equals(Operator.eq)) {
                addCond(mLdapFilter, attr, "=", val);
            } else if (op.equals(Operator.ge)) {
                addCond(mLdapFilter, attr, ">=", val);
            } else if (op.equals(Operator.le)) {
                addCond(mLdapFilter, attr, "<=", val);
            } else if (op.equals(Operator.startswith)) {
                mLdapFilter.append('(').append(attr);
                mLdapFilter.append('=').append(val).append("*)");
            } else if (op.equals(Operator.endswith)) {
                mLdapFilter.append('(').append(attr);
                mLdapFilter.append("=*").append(val);
                mLdapFilter.append(')');
            } else {
                // fallback to EQUALS
                addCond(mLdapFilter, attr, "=", val);
            }

            if (negation) mLdapFilter.append(')');
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
            return LdapUtil.escapeSearchFilterArg(term.getRhs());
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
            if (attrMgr != null && attrMgr.isEmailOrIDN(term.getLhs()))
                rhs = IDNUtil.toAscii(rhs);
            
            return rhs;
        }
    }

    public static EntrySearchFilter sCalendarResourcesFilter;
    static {
        Single calResType = new Single(
                false,
                Provisioning.A_objectClass,
                Operator.eq,
                LdapProvisioning.C_zimbraCalendarResource);
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
        filter.andWith(sCalendarResourcesFilter);
        if (!filter.usesIndex())
            throw ServiceException.INVALID_REQUEST(
                    "Search referring to no indexed attribute is not allowed: " + filter.toString(), null);
        LdapQueryVisitor visitor = new LdapQueryVisitor();
        filter.traverse(visitor);
        return visitor.getFilter();
    }
}
