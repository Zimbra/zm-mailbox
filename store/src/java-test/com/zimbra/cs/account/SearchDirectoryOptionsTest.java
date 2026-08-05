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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link SearchDirectoryOptions} — a pure options holder with
 * enum helpers, setters/getters, and a deep equals().
 */
public class SearchDirectoryOptionsTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    private static Domain createDomain(String name) throws Exception {
        return Provisioning.getInstance().createDomain(name, new HashMap<String, Object>());
    }

    @Test
    public void getFlagEachObjectTypeMatchesProvisioningFlag() {
        // Arrange / Act / Assert
        assertEquals(Provisioning.SD_ACCOUNT_FLAG, ObjectType.accounts.getFlag());
        assertEquals(Provisioning.SD_ALIAS_FLAG, ObjectType.aliases.getFlag());
        assertEquals(Provisioning.SD_DOMAIN_FLAG, ObjectType.domains.getFlag());
        assertEquals(Provisioning.SD_SERVER_FLAG, ObjectType.servers.getFlag());
    }

    @Test
    public void getAllTypesFlagsCombinesEveryTypeIntoOrMask() {
        // Arrange
        int expected = 0;
        for (ObjectType t : ObjectType.values()) {
            expected |= t.getFlag();
        }

        // Act
        int allFlags = ObjectType.getAllTypesFlags();

        // Assert
        assertEquals(expected, allFlags);
        assertTrue("must include account flag",
                (allFlags & Provisioning.SD_ACCOUNT_FLAG) == Provisioning.SD_ACCOUNT_FLAG);
    }

    @Test
    public void getFlagsSetOfTypesOrsTheirFlags() {
        // Arrange
        Set<ObjectType> types = new HashSet<ObjectType>();
        types.add(ObjectType.accounts);
        types.add(ObjectType.domains);

        // Act
        int flags = ObjectType.getFlags(types);

        // Assert
        assertEquals(Provisioning.SD_ACCOUNT_FLAG | Provisioning.SD_DOMAIN_FLAG, flags);
    }

    @Test
    public void fromCSVStringValidTrimmedTokensParsesAllTypes() throws Exception {
        // Act
        Set<ObjectType> types = ObjectType.fromCSVString("accounts, domains ,servers");

        // Assert
        assertEquals(3, types.size());
        assertTrue(types.contains(ObjectType.accounts));
        assertTrue(types.contains(ObjectType.domains));
        assertTrue(types.contains(ObjectType.servers));
    }

    @Test
    public void fromCSVStringUnknownTokenThrowsServiceException() {
        try {
            ObjectType.fromCSVString("accounts,bogusType");
            fail("expected ServiceException for unknown type");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("unknown type"));
        }
    }

    @Test
    public void fromStringUnknownValueThrowsInvalidRequest() {
        try {
            ObjectType.fromString("notAType");
            fail("expected ServiceException for unknown type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void toCSVStringSingleTypeRoundTripsThroughFromCSVString() throws Exception {
        // Arrange
        Set<ObjectType> types = new HashSet<ObjectType>();
        types.add(ObjectType.coses);

        // Act
        String csv = ObjectType.toCSVString(types);
        Set<ObjectType> back = ObjectType.fromCSVString(csv);

        // Assert
        assertEquals("coses", csv);
        assertEquals(types, back);
    }

    @Test
    public void setTypesVarargsThenGetTypesAsFlagsReflectsTypes() throws Exception {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();

        // Act
        opts.setTypes(ObjectType.accounts, ObjectType.servers);

        // Assert
        assertEquals(2, opts.getTypes().size());
        assertEquals(Provisioning.SD_ACCOUNT_FLAG | Provisioning.SD_SERVER_FLAG,
                opts.getTypesAsFlags());
    }

    @Test
    public void addTypeOnNullTypesCreatesSetAndAdds() {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();
        assertNull(opts.getTypes());

        // Act
        opts.addType(ObjectType.domains);
        opts.addType(ObjectType.domains);

        // Assert
        assertEquals(1, opts.getTypes().size());
        assertEquals(Provisioning.SD_DOMAIN_FLAG, opts.getTypesAsFlags());
    }

    @Test
    public void getTypesAsFlagsNullTypesReturnsZero() {
        // Act / Assert
        assertEquals(0, SearchDirectoryOptions.getTypesAsFlags(null));
        assertEquals(0, new SearchDirectoryOptions().getTypesAsFlags());
    }

    @Test
    public void setResultPageSizeNegativeResetsToDefaultLimit() throws Exception {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();
        opts.setResultPageSize(50);
        assertEquals(50, opts.getResultPageSize());

        // Act
        opts.setResultPageSize(-5);

        // Assert
        assertEquals(SearchDirectoryOptions.DEFAULT_LIMIT, opts.getResultPageSize());
    }

    @Test
    public void setSortOptNullRevertsToDefaultSortOpt() {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();
        opts.setSortOpt(SortOpt.SORT_DESCENDING);
        assertEquals(SortOpt.SORT_DESCENDING, opts.getSortOpt());

        // Act
        opts.setSortOpt(null);

        // Assert
        assertEquals(SearchDirectoryOptions.DEFAULT_SORT_OPT, opts.getSortOpt());
    }

    @Test
    public void settersAndGettersScalarFieldsRoundTrip() {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();

        // Act
        opts.setOnMaster(true);
        opts.setMaxResults(123);
        opts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        opts.setSortAttr("displayName");
        opts.setConvertIDNToAscii(true);
        opts.setManageDSAit(true);
        opts.setUseControl(false);
        opts.setLimit(7);
        opts.setOffset(3);
        opts.setHabRootGroupDn("ou=people");

        // Assert
        assertTrue(opts.getOnMaster());
        assertEquals(123, opts.getMaxResults());
        assertEquals(MakeObjectOpt.NO_DEFAULTS, opts.getMakeObjectOpt());
        assertEquals("displayName", opts.getSortAttr());
        assertTrue(opts.getConvertIDNToAscii());
        assertTrue(opts.isManageDSAit());
        assertFalse(opts.isUseControl());
        assertEquals(7, opts.getLimit());
        assertEquals(3, opts.getOffset());
        assertEquals("ou=people", opts.getHabRootGroupDn());
        assertTrue("useConnPool is hardcoded true", opts.getUseConnPool());
    }

    @Test
    public void equalsSameScalarConfigurationIsTrueAndDiffersWhenChanged() {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setMaxResults(10);
        b.setMaxResults(10);

        // Assert — equal
        assertTrue(a.equals(b));
        assertTrue(a.equals(a));

        // Act — mutate one
        b.setMaxResults(11);

        // Assert — no longer equal
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsNonOptionsObjectIsFalse() {
        // Act / Assert
        assertFalse(new SearchDirectoryOptions().equals("not an options object"));
    }

    @Test
    public void constructorWithReturnAttrsStoresAttrs() {
        // Arrange
        String[] attrs = new String[] {"cn", "mail"};

        // Act
        SearchDirectoryOptions opts = new SearchDirectoryOptions(attrs);

        // Assert
        assertEquals(2, opts.getReturnAttrs().length);
        assertEquals("cn", opts.getReturnAttrs()[0]);
    }

    @Test
    public void constructorWithDomainSetsDomain() throws Exception {
        // Arrange
        Domain domain = createDomain("sdo-ctor-domain.example.com");

        // Act
        SearchDirectoryOptions opts = new SearchDirectoryOptions(domain);

        // Assert
        assertSame(domain, opts.getDomain());
        // default return attrs is ALL_ATTRS (null)
        assertNull(opts.getReturnAttrs());
    }

    @Test
    public void constructorWithDomainAndReturnAttrsSetsBoth() throws Exception {
        // Arrange
        Domain domain = createDomain("sdo-ctor-domain2.example.com");
        String[] attrs = new String[] {"cn", "zimbraId"};

        // Act
        SearchDirectoryOptions opts = new SearchDirectoryOptions(domain, attrs);

        // Assert
        assertSame(domain, opts.getDomain());
        assertEquals(2, opts.getReturnAttrs().length);
        assertEquals("zimbraId", opts.getReturnAttrs()[1]);
    }

    @Test
    public void setTypesFromCSVStringParsesCSVSetsTypesAsFlags() throws Exception {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();

        // Act — the soap entry point that parses a CSV string
        opts.setTypes("accounts,domains");

        // Assert
        assertEquals(2, opts.getTypes().size());
        assertEquals(Provisioning.SD_ACCOUNT_FLAG | Provisioning.SD_DOMAIN_FLAG,
                opts.getTypesAsFlags());
    }

    @Test
    public void setFilterStringThenGettersReturnFilterIdAndStr() {
        // Arrange
        SearchDirectoryOptions opts = new SearchDirectoryOptions();

        // Act — store a raw filter string with no FilterId (null is allowed)
        opts.setFilterString(null, "(objectClass=*)");

        // Assert
        assertNull(opts.getFilterId());
        assertEquals("(objectClass=*)", opts.getFilterString());
        assertNull("no ZLdapFilter object was set", opts.getFilter());
    }

    @Test
    public void equalsSameDomainIsTrueAndDiffersForDifferentDomain() throws Exception {
        // Arrange — two options pointing at the same domain are equal on the domain branch
        Domain domain1 = createDomain("sdo-eq-domain1.example.com");
        Domain domain2 = createDomain("sdo-eq-domain2.example.com");
        SearchDirectoryOptions a = new SearchDirectoryOptions(domain1);
        SearchDirectoryOptions b = new SearchDirectoryOptions(domain1);

        // Assert — equal when same domain id
        assertTrue(a.equals(b));

        // Act — point b at a different domain
        SearchDirectoryOptions c = new SearchDirectoryOptions(domain2);

        // Assert — domain-id mismatch makes them unequal
        assertFalse(a.equals(c));
    }

    @Test
    public void equalsDomainSetOnlyOnOneIsFalseBothDirections() throws Exception {
        // Arrange
        Domain domain = createDomain("sdo-eq-onesided.example.com");
        SearchDirectoryOptions withDomain = new SearchDirectoryOptions(domain);
        SearchDirectoryOptions withoutDomain = new SearchDirectoryOptions();

        // Assert — domain on this but null on other -> false (the L237/L239 branch)
        assertFalse(withDomain.equals(withoutDomain));
        // domain null on this but set on other -> false (the L246 branch)
        assertFalse(withoutDomain.equals(withDomain));
    }

    @Test
    public void equalsDifferingResultPageSizeIsFalse() throws Exception {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setResultPageSize(25);
        b.setResultPageSize(50);

        // Assert — the resultPageSize branch (L233)
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferingOnMasterIsFalse() {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setOnMaster(true);

        // Assert — the onMaster branch (L221)
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferingFilterStringIsFalseAndOneSidedNullIsFalse() {
        // Arrange — same filter string is equal
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setFilterString(null, "(cn=a)");
        b.setFilterString(null, "(cn=a)");
        assertTrue(a.equals(b));

        // Act — different filter string (L275 branch)
        b.setFilterString(null, "(cn=b)");

        // Assert
        assertFalse(a.equals(b));

        // Arrange — filterStr set on this, null on other (L270/L272 branch)
        SearchDirectoryOptions noFilter = new SearchDirectoryOptions();
        assertFalse(a.equals(noFilter));
        // filterStr null on this, set on other (L280 branch)
        assertFalse(noFilter.equals(a));
    }

    @Test
    public void equalsDifferingReturnAttrsIsFalseAndOneSidedNullIsFalse() {
        // Arrange — same attrs (order-insensitive) are equal
        SearchDirectoryOptions a = new SearchDirectoryOptions(new String[] {"cn", "mail"});
        SearchDirectoryOptions b = new SearchDirectoryOptions(new String[] {"mail", "cn"});
        assertTrue("return attrs compared as a set, order independent", a.equals(b));

        // Act — differing attrs (L296 branch)
        SearchDirectoryOptions c = new SearchDirectoryOptions(new String[] {"cn"});

        // Assert
        assertFalse(a.equals(c));

        // attrs set on this, null on other (L289/L291 branch)
        SearchDirectoryOptions allAttrs = new SearchDirectoryOptions();
        assertFalse(a.equals(allAttrs));
        // attrs null on this, set on other (L301 branch)
        assertFalse(allAttrs.equals(a));
    }

    @Test
    public void equalsDifferingTypesFlagsIsFalse() throws Exception {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setTypes(ObjectType.accounts);
        b.setTypes(ObjectType.servers);

        // Assert — the getTypesAsFlags branch (L285)
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferingMakeObjectOptIsFalse() {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);

        // Assert — the makeObjOpt branch (L306)
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferingSortOptIsFalse() {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setSortOpt(SortOpt.SORT_ASCENDING);

        // Assert — the sortOpt branch (L310)
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsDifferingSortAttrIsFalseAndOneSidedNullIsFalse() {
        // Arrange — same sort attr is equal
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setSortAttr("cn");
        b.setSortAttr("cn");
        assertTrue(a.equals(b));

        // Act — different sort attr (L319 branch)
        b.setSortAttr("mail");

        // Assert
        assertFalse(a.equals(b));

        // sortAttr set on this, null on other (L314/L316 branch)
        SearchDirectoryOptions noSort = new SearchDirectoryOptions();
        assertFalse(a.equals(noSort));
        // sortAttr null on this, set on other (L324 branch)
        assertFalse(noSort.equals(a));
    }

    @Test
    public void equalsDifferingConvertIDNToAsciiIsFalse() {
        // Arrange
        SearchDirectoryOptions a = new SearchDirectoryOptions();
        SearchDirectoryOptions b = new SearchDirectoryOptions();
        a.setConvertIDNToAscii(true);

        // Assert — the convertIDNToAscii branch (L329)
        assertFalse(a.equals(b));
    }

    @Test
    public void equalsFullyConfiguredIdenticalOptionsIsTrue() throws Exception {
        // Arrange — exercise the all-branches-pass path of equals()
        Domain domain = createDomain("sdo-eq-full.example.com");
        SearchDirectoryOptions a = new SearchDirectoryOptions(domain, new String[] {"cn"});
        SearchDirectoryOptions b = new SearchDirectoryOptions(domain, new String[] {"cn"});
        for (SearchDirectoryOptions o : new SearchDirectoryOptions[] {a, b}) {
            o.setOnMaster(true);
            o.setMaxResults(99);
            o.setResultPageSize(33);
            o.setFilterString(null, "(objectClass=zimbraAccount)");
            o.setTypes(ObjectType.accounts);
            o.setMakeObjectOpt(MakeObjectOpt.NO_SECONDARY_DEFAULTS);
            o.setSortOpt(SortOpt.SORT_DESCENDING);
            o.setSortAttr("displayName");
            o.setConvertIDNToAscii(true);
        }

        // Assert — every branch matches, equals returns true
        assertTrue(a.equals(b));
    }
}
