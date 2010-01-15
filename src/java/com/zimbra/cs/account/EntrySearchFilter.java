/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntrySearchFilter {

    public static enum AndOr {
        and,
        or
    }

    public static enum Operator {
        eq,         // exact match (=val; string or integer)
        has,        // substring (=*val*; string only)
        startswith, // starts with (=val*; string only)
        endswith,   // ends with (=*val; string only)
        ge,         // greater than or equal to (>=val; integer only)
        le,         // less then or equal to (<=val; integer only)
        gt,         // greater than (>val; integer only)
        lt          // less than (<val; integer only)
    }

    public static interface Visitor {
        public void visitSingle(Single term);
        public void enterMulti(Multi term);
        public void leaveMulti(Multi term);
    }

    public static abstract class Term {
        private boolean mNegation;
        public boolean isNegation() { return mNegation; }
        public void setNegation(boolean negation) { mNegation = negation; }
        protected abstract void visit(Visitor visitor);
    }

    public static class Multi extends Term {
        private AndOr mAndOr;
        private List<Term> mTerms;

        public Multi(boolean negation, AndOr andOr) {
            setNegation(negation);
            mAndOr = andOr;
            mTerms = new ArrayList<Term>();
        }

        public Multi(boolean negation, AndOr andOr, Term... terms) {
            this(negation, andOr);
            for (Term t: terms) {
                add(t);
            }
        }

        public List<Term> getTerms() { return mTerms; }
        public boolean isAnd() { return AndOr.and.equals(mAndOr); }
        public void add(Term t) { mTerms.add(t); }

        protected void visit(Visitor visitor) {
            visitor.enterMulti(this);
            for (Term t : mTerms) {
                t.visit(visitor);
            }
            visitor.leaveMulti(this);
        }
    }

    public static class Single extends Term {
        private String mLhs;
        private Operator mOp;
        private String mRhs;

        public Single(boolean negation, String lhs, String op, String rhs)
        throws IllegalArgumentException {
            // throws IllegalArgumentException if comp is unknown
            this(negation, lhs, Operator.valueOf(op), rhs);
        }

        public Single(boolean negation, String lhs, Operator op, String rhs) {
            setNegation(negation);
            mLhs = lhs; mOp = op; mRhs = rhs;
        }

        public String getLhs() { return mLhs; }
        public Operator getOperator()  { return mOp; }
        public String getRhs() { return mRhs; }

        protected void visit(Visitor visitor) {
            visitor.visitSingle(this);
        }
    }

    private static Set<String> sIndexedAttrs = new HashSet<String>();
    static {
        // list all indexed attributes
        sIndexedAttrs.add(Provisioning.A_objectClass.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraForeignPrincipal.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraId.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraMailCatchAllAddress.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraMailDeliveryAddress.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraMailForwardingAddress.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraMailAlias.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraDomainName.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_uid.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_mail.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_cn.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_sn.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_gn.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_displayName.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraCalResSite.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraCalResBuilding.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraCalResFloor.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraCalResRoom.toLowerCase());
        sIndexedAttrs.add(Provisioning.A_zimbraCalResCapacity.toLowerCase());
        //sIndexedAttrs.add(Provisioning.A_street.toLowerCase());
        //sIndexedAttrs.add(Provisioning.A_l.toLowerCase());
        //sIndexedAttrs.add(Provisioning.A_st.toLowerCase());
        //sIndexedAttrs.add(Provisioning.A_postalCode.toLowerCase());
        //sIndexedAttrs.add(Provisioning.A_co.toLowerCase());
    }

    private Term mTerm;

    public EntrySearchFilter(Term term) {
        mTerm = term;
    }

    public void andWith(EntrySearchFilter other) {
        Term otherTerm = other.mTerm;
        if (mTerm instanceof Multi) {
            Multi m = (Multi) mTerm;
            if (m.isAnd()) {
                m.add(otherTerm);
                return;
            }
        }
        Multi multi = new Multi(false, AndOr.and);
        multi.add(mTerm);
        multi.add(otherTerm);
        mTerm = multi;
    }

    public void orWith(EntrySearchFilter other) {
        Term otherTerm = other.mTerm;
        if (mTerm instanceof Multi) {
            Multi m = (Multi) mTerm;
            if (!m.isAnd()) {
                m.add(otherTerm);
                return;
            }
        }
        Multi multi = new Multi(false, AndOr.or);
        multi.add(mTerm);
        multi.add(otherTerm);
        mTerm = multi;
    }

    public void traverse(Visitor visitor) {
        mTerm.visit(visitor);
    }

    public boolean usesIndex() {
        return possiblyUsesIndex(mTerm);
    }

    private static boolean possiblyUsesIndex(Term term) {
        boolean result;
        if (term instanceof Single) {
            Single single = (Single) term;
            result = sIndexedAttrs.contains(single.getLhs().toLowerCase());
        } else {
            assert(term instanceof Multi);
            Multi multi = (Multi) term;
            boolean anding = multi.isAnd();
            List<Term> terms = multi.getTerms();
            if (anding) {
                // If any of the ANDed terms uses an index, the combined
                // search is likely to use an index.
                result = false;
                for (Term t : terms) {
                    if (possiblyUsesIndex(t)) {
                        result = true;
                        break;
                    }
                }
            } else {
                // If any of the ORed terms doesn't use an index, the combined
                // search results in a full scan.
                result = true;
                for (Term t : terms) {
                    if (!possiblyUsesIndex(t)) {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }
}
