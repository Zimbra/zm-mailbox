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
import com.zimbra.cs.ldap.ZLdapFilterFactory;

/*
 * Traverse a EntrySearchFilter.Term tree and convert it to LDAP query
 */
public class LdapEntrySearchFilter {

    
    public static class LdapQueryVisitor implements Visitor {
        private StringBuilder mLdapFilter;
        
        /*
         * Whether values in the EntrySearchFilter.Term tree is raw and therefore
         * should be escaped per RFC 2254.  
         *     raw    escaped
         *    ---------------
         *      *     0x2a
         *      (     0x28
         *      )     0x29
         *      \     0x5c
         *
         * Set to true if value in each node is already escaped.
         * Set to false if value in each node is not yet escaped.
         * 
         * Callsites taking RFC 2254 LDAP search filter as input should 
         * set this to false.  e.g. LdapQueryVisitorIDN used by SearchDirectory SOAP 
         * handler and "zmprov sa", because values are already escaped.
         * 
         * Callsites takes raw search value should set this to true, so values in 
         * the output will be properly escaped.  e.g. callsites that build ldap 
         * filter from SOAP <searchFilter> elements.
         * 
         */
        private boolean valueIsRaw = true;

        public LdapQueryVisitor() {
            this(true);
        }
        
        public LdapQueryVisitor(boolean valueIsRaw) {
            mLdapFilter = new StringBuilder();
            this.valueIsRaw = valueIsRaw;
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
            String filter = null;
            
            String attr = term.getLhs();
            String val = getVal(term);
            if (op.equals(Operator.has)) {
                filter = filterFactory.substringFilter(attr, val, valueIsRaw);
            } else if (op.equals(Operator.eq)) {
                // there is no presence operator in Single
                if (val.equals("*")) {
                    filter = filterFactory.presenceFilter(attr);
                } else {
                    filter = filterFactory.equalityFilter(attr, val, valueIsRaw);
                }
            } else if (op.equals(Operator.ge)) {
                filter = filterFactory.greaterOrEqualFilter(attr, val, valueIsRaw);
            } else if (op.equals(Operator.le)) {
                filter = filterFactory.lessOrEqualFilter(attr, val, valueIsRaw);
            } else if (op.equals(Operator.startswith)) {
                filter = filterFactory.startsWithFilter(attr, val, valueIsRaw);
            } else if (op.equals(Operator.endswith)) {
                filter = filterFactory.endsWithFilter(attr, val, valueIsRaw);
            } else {
                // fallback to EQUALS
                filter = filterFactory.equalityFilter(attr, val, valueIsRaw);
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
            if (term.getTerms().size() > 1) mLdapFilter.append(')');
            if (term.isNegation()) mLdapFilter.append(')');
        }
        
        protected String getVal(Single term) {
            // return LdapUtil.escapeSearchFilterArg(term.getRhs());
            return term.getRhs();
        }
    }
    
    private static class LdapQueryVisitorIDN extends LdapQueryVisitor implements Visitor {
        
        private LdapQueryVisitorIDN() {
            super(false);
        }
        
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

    /**
     * Takes a RFC 2254 filter and converts assertions value from unicode to ACE 
     * for IDN attributes.  IDN attributes are those storing the ACE representation 
     * of the unicode.   For non-IDN attributes, assertion values are just passed through. 
     * 
     * e.g.
     * (zimbraMailDeliveryAddress=*@test.\u4e2d\u6587.com) will be converted to
     * (zimbraMailDeliveryAddress=*@test.xn--fiq228c.com)
     * because zimbraMailDeliveryAddress is an IDN attribute.
     * 
     * (zimbraDomainName=*\u4e2d\u6587*) will remain the same because zimbraDomainName 
     * is not an IDN attribute.
     *   
     * @param filterStr a RFC 2254 filter (assertion values must be already RFC 2254 escaped)
     * @return
     */
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
        /* 
        if (!filter.usesIndex())
            throw ServiceException.INVALID_REQUEST(
                    "Search referring to no indexed attribute is not allowed: " + filter.toString(), null);
        */
        LdapQueryVisitor visitor = new LdapQueryVisitor();
        filter.traverse(visitor);
        return visitor.getFilter();
    }
}
