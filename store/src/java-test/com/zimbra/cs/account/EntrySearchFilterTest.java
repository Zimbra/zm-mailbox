/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.EntrySearchFilter.AndOr;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.gal.GalExtraSearchFilter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link EntrySearchFilter} — term construction, AND/OR combination,
 * traversal via the visitor, and index-usage analysis. Pure logic, no harness needed.
 */
public class EntrySearchFilterTest {

    /** A real recording visitor — captures the traversal order and structure. */
    private static class RecordingVisitor implements Visitor {
        private final List<String> events = new ArrayList<String>();

        @Override
        public void visitSingle(Single term) {
            events.add("single:" + term.getLhs() + term.getOperator() + term.getRhs());
        }

        @Override
        public void enterMulti(Multi term) {
            events.add("enter:" + (term.isAnd() ? "and" : "or"));
        }

        @Override
        public void leaveMulti(Multi term) {
            events.add("leave:" + (term.isAnd() ? "and" : "or"));
        }
    }

    @Test
    public void singleStringOpConstructorParsesOperatorEnum() {
        Single single = new Single(false, "displayName", "eq", "Bob");

        assertEquals("displayName", single.getLhs());
        assertEquals(Operator.eq, single.getOperator());
        assertEquals("Bob", single.getRhs());
        assertFalse("negation default should be false", single.isNegation());
    }

    @Test
    public void singleUnknownOperatorStringThrowsIllegalArgument() {
        try {
            new Single(false, "displayName", "bogusOp", "Bob");
            fail("expected IllegalArgumentException for unknown operator");
        } catch (IllegalArgumentException e) {
            assertTrue("message should reference the bad op",
                    e.getMessage().contains("bogusOp"));
        }
    }

    @Test
    public void termSetNegationTogglesNegationState() {
        Single single = new Single(true, "uid", Operator.eq, "u1");

        assertTrue("ctor negation should be honored", single.isNegation());
        single.setNegation(false);
        assertFalse("setNegation(false) should clear it", single.isNegation());
    }

    @Test
    public void multiVarargsConstructorAddsAllTermsAndReportsAnd() {
        Single a = new Single(false, "uid", Operator.eq, "u1");
        Single b = new Single(false, "cn", Operator.has, "smith");

        Multi multi = new Multi(false, AndOr.and, a, b);

        assertTrue("AndOr.and should report isAnd true", multi.isAnd());
        assertEquals(2, multi.getTerms().size());
        assertSame(a, multi.getTerms().get(0));
        assertSame(b, multi.getTerms().get(1));
    }

    @Test
    public void multiOrConstructorIsAndReportsFalse() {
        Multi multi = new Multi(false, AndOr.or);
        multi.add(new Single(false, "uid", Operator.eq, "u1"));

        assertFalse("AndOr.or should report isAnd false", multi.isAnd());
        assertEquals(1, multi.getTerms().size());
    }

    @Test
    public void traverseNestedMultiVisitsInDepthFirstOrder() {
        Single inner1 = new Single(false, "uid", Operator.eq, "u1");
        Single inner2 = new Single(false, "cn", Operator.has, "smith");
        Multi multi = new Multi(false, AndOr.and, inner1, inner2);
        EntrySearchFilter filter = new EntrySearchFilter(multi);
        RecordingVisitor visitor = new RecordingVisitor();

        filter.traverse(visitor);

        assertEquals(4, visitor.events.size());
        assertEquals("enter:and", visitor.events.get(0));
        assertEquals("single:uideqU1".toLowerCase(), visitor.events.get(1).toLowerCase());
        assertEquals("leave:and", visitor.events.get(3));
    }

    @Test
    public void traverseSingleTermVisitsSingleOnly() {
        Single single = new Single(false, "displayName", Operator.eq, "Bob");
        EntrySearchFilter filter = new EntrySearchFilter(single);
        RecordingVisitor visitor = new RecordingVisitor();

        filter.traverse(visitor);

        assertEquals(1, visitor.events.size());
        assertTrue(visitor.events.get(0).startsWith("single:"));
    }

    @Test
    public void usesIndexSingleIndexedAttrReturnsTrue() {
        // mail is in the indexed-attribute set
        Single single = new Single(false, Provisioning.A_mail, Operator.eq, "x@y.com");
        EntrySearchFilter filter = new EntrySearchFilter(single);

        assertTrue("mail is an indexed attribute", filter.usesIndex());
    }

    @Test
    public void usesIndexSingleNonIndexedAttrReturnsFalse() {
        Single single = new Single(false, "description", Operator.eq, "hello");
        EntrySearchFilter filter = new EntrySearchFilter(single);

        assertFalse("description is not indexed", filter.usesIndex());
    }

    @Test
    public void usesIndexAndWithOneIndexedTermReturnsTrue() {
        Single indexed = new Single(false, Provisioning.A_uid, Operator.eq, "u1");
        Single notIndexed = new Single(false, "description", Operator.eq, "x");
        Multi multi = new Multi(false, AndOr.and, indexed, notIndexed);
        EntrySearchFilter filter = new EntrySearchFilter(multi);

        assertTrue("AND with at least one indexed term uses index", filter.usesIndex());
    }

    @Test
    public void usesIndexOrWithOneNonIndexedTermReturnsFalse() {
        Single indexed = new Single(false, Provisioning.A_uid, Operator.eq, "u1");
        Single notIndexed = new Single(false, "description", Operator.eq, "x");
        Multi multi = new Multi(false, AndOr.or, indexed, notIndexed);
        EntrySearchFilter filter = new EntrySearchFilter(multi);

        assertFalse("OR where one term is unindexed forces a full scan", filter.usesIndex());
    }

    @Test
    public void andWithExistingAndMultiAppendsToSameMulti() throws Exception {
        Single a = new Single(false, Provisioning.A_uid, Operator.eq, "u1");
        Single b = new Single(false, Provisioning.A_mail, Operator.eq, "x@y.com");
        Multi rootAnd = new Multi(false, AndOr.and, a);
        EntrySearchFilter root = new EntrySearchFilter(rootAnd);
        EntrySearchFilter other = new EntrySearchFilter(b);

        root.andWith(other);

        // The other term should have been folded directly into the existing AND multi.
        assertEquals(2, rootAnd.getTerms().size());
        assertSame(b, rootAnd.getTerms().get(1));
    }

    @Test
    public void orWithTopLevelAndMultiWrapsInNewOrMulti() {
        Single a = new Single(false, Provisioning.A_uid, Operator.eq, "u1");
        Single b = new Single(false, Provisioning.A_mail, Operator.eq, "x@y.com");
        Multi rootAnd = new Multi(false, AndOr.and, a);
        EntrySearchFilter root = new EntrySearchFilter(rootAnd);
        EntrySearchFilter other = new EntrySearchFilter(b);
        RecordingVisitor visitor = new RecordingVisitor();

        root.orWith(other);
        root.traverse(visitor);

        // Because the root was an AND, orWith must create a NEW or-multi wrapping both.
        assertEquals("enter:or", visitor.events.get(0));
        assertTrue("nested AND multi must still be traversed",
                visitor.events.contains("enter:and"));
    }

    @Test
    public void orWithExistingOrMultiAppendsToSameMulti() {
        Single a = new Single(false, Provisioning.A_uid, Operator.eq, "u1");
        Single b = new Single(false, Provisioning.A_mail, Operator.eq, "x@y.com");
        Multi rootOr = new Multi(false, AndOr.or, a);
        EntrySearchFilter root = new EntrySearchFilter(rootOr);
        EntrySearchFilter other = new EntrySearchFilter(b);

        root.orWith(other);

        assertEquals(2, rootOr.getTerms().size());
        assertSame(b, rootOr.getTerms().get(1));
    }

    @Test
    public void andWithTopLevelSingleWrapsInNewAndMulti() {
        Single a = new Single(false, Provisioning.A_uid, Operator.eq, "u1");
        Single b = new Single(false, Provisioning.A_mail, Operator.eq, "x@y.com");
        EntrySearchFilter root = new EntrySearchFilter(a);
        EntrySearchFilter other = new EntrySearchFilter(b);
        RecordingVisitor visitor = new RecordingVisitor();

        root.andWith(other);
        root.traverse(visitor);

        assertEquals("enter:and", visitor.events.get(0));
        assertEquals("leave:and", visitor.events.get(visitor.events.size() - 1));
    }

    // ------------------------------------------------------------------
    // parseSearchFilter -- build the filter the same way production does, from a SOAP
    // <searchFilter> Element (GalExtraSearchFilter.parseSearchFilter), then assert the
    // resulting term tree via the visitor. Pure XML parsing, no harness/LDAP required.
    // ------------------------------------------------------------------

    /* Builds {@code <request><searchFilter>...</searchFilter></request>} and returns the request. */
    private static Element requestWithFilter() {
        Element request = new Element.XMLElement("SearchGalRequest");
        return request.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
    }

    @Test
    public void parseSearchFilterSingleCondBuildsSingleTerm() throws Exception {
        // Arrange -- a single <cond attr=.. op=.. value=..> under <searchFilter>.
        Element filterElem = requestWithFilter();
        Element cond = filterElem.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, "displayName");
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
        cond.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "Bob");

        // Act -- parse via the real production entry point (parent of the <searchFilter> elem).
        EntrySearchFilter filter = GalExtraSearchFilter.parseSearchFilter(filterElem.getParent());

        // Assert -- one Single term with the parsed attr/op/value.
        RecordingVisitor visitor = new RecordingVisitor();
        filter.traverse(visitor);
        assertEquals(1, visitor.events.size());
        assertEquals("single:displayNameeqBob", visitor.events.get(0));
    }

    @Test
    public void parseSearchFilterOrMultiCondBuildsOrMultiWithChildren() throws Exception {
        // Arrange -- <conds or="1"> wrapping two <cond> terms.
        Element filterElem = requestWithFilter();
        Element conds = filterElem.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
        conds.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OR, true);
        Element c1 = conds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        c1.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, "uid");
        c1.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "eq");
        c1.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "u1");
        Element c2 = conds.addElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
        c2.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, "cn");
        c2.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, "has");
        c2.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, "smith");

        // Act
        EntrySearchFilter filter = GalExtraSearchFilter.parseSearchFilter(filterElem.getParent());

        // Assert -- an OR multi wrapping the two singles, in document order.
        RecordingVisitor visitor = new RecordingVisitor();
        filter.traverse(visitor);
        assertEquals(4, visitor.events.size());
        assertEquals("enter:or", visitor.events.get(0));
        assertEquals("single:uideqU1".toLowerCase(), visitor.events.get(1).toLowerCase());
        assertEquals("single:cnhassmith", visitor.events.get(2));
        assertEquals("leave:or", visitor.events.get(3));
    }
}
