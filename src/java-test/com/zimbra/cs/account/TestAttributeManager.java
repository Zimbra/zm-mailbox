/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Version;
import com.zimbra.cs.account.callback.AvailableZimlets;


public class TestAttributeManager  {

    private AttributeManager am = null;

    public static final String ATTR_TWO_SINCE = "twoSinceAttr";
    public static final String ATTR_MULTI_SINCE = "multiSinceAttr";
    public static final String ATTR_FUTURE = "futureAttr";
    public static final String ATTR_ZIMBRAID = "zimbraId";
    public static final String ATTR_ZIMLETDOMAIN = "zimbraZimletDomainAvailableZimlets";



    @Before
    public void setup() throws ServiceException {
        am = new AttributeManager();

        Set<AttributeClass> requiredIn = Sets.newHashSet(
                AttributeClass.account,
                AttributeClass.alias,
                AttributeClass.distributionList,
                AttributeClass.domain,
                AttributeClass.server,
                AttributeClass.alwaysOnCluster,
                AttributeClass.ucService,
                AttributeClass.cos,
                AttributeClass.xmppComponent,
                AttributeClass.group,
                AttributeClass.groupDynamicUnit,
                AttributeClass.groupStaticUnit);
        Set<AttributeFlag> flags = Sets.newHashSet(AttributeFlag.accountInfo);
        AttributeInfo ai = new AttributeInfo("zimbraId", 1, null, 0, null, AttributeType.TYPE_ID, null, "", true, null, null, AttributeCardinality.single, requiredIn, null, flags, null, null, null, null, null, "Zimbra Systems Unique ID", null, null, null);
        am.addAttribute(ai);

        requiredIn = null;
        Set<AttributeClass> optionalIn = Sets.newHashSet(AttributeClass.domain,AttributeClass.globalConfig);
        flags = Sets.newHashSet(AttributeFlag.domainInherited);
        List<Version> since = Lists.newArrayList(new Version("5.0.10"));
        ai = new AttributeInfo("zimbraZimletDomainAvailableZimlets", 710, null, 0, new AvailableZimlets(), AttributeType.TYPE_STRING, null, "", false, null, "256", AttributeCardinality.multi, requiredIn, optionalIn, flags, null, null, null, null, null, "List of Zimlets available to this domain.", null, since, null);
        am.addAttribute(ai);

        requiredIn = null;
        optionalIn = null;
        flags = null;
        since = Lists.newArrayList(new Version("8.0.8"), new Version("8.5.1"));
        ai = new AttributeInfo(ATTR_TWO_SINCE, 99996, null, 0, null, AttributeType.TYPE_STRING, null, "", false, null, null, AttributeCardinality.multi, requiredIn, optionalIn, flags, null, null, null, null, null, "test two since", null, since, null);
        am.addAttribute(ai);;

        since = Lists.newArrayList(new Version("9.0.0"), new Version("8.0.8"), new Version("7.2.8"), new Version("8.5.2"));
        //out of order intentionally; attributeinfo class should handle that so we don't have bugs if someone no-brains this
        ai = new AttributeInfo(ATTR_MULTI_SINCE, 99997, null, 0, null, AttributeType.TYPE_STRING, null, "", false, null, null, AttributeCardinality.multi, requiredIn, optionalIn, flags, null, null, null, null, null, "test multi since", null, since, null);
        am.addAttribute(ai);;

        since = Lists.newArrayList(new Version(Version.FUTURE));
        ai = new AttributeInfo(ATTR_FUTURE, 99998, null, 0, null, AttributeType.TYPE_STRING, null, "", false, null, null, AttributeCardinality.single, requiredIn, optionalIn, flags, null, null, null, null, null, "test future", null, since, null);
        am.addAttribute(ai);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInVersion() throws Exception {

        Assert.assertTrue(am.inVersion(ATTR_ZIMBRAID, "0"));
        Assert.assertTrue(am.inVersion(ATTR_ZIMBRAID, "5.0.10"));

        Assert.assertFalse(am.inVersion(ATTR_ZIMLETDOMAIN, "0"));
        Assert.assertFalse(am.inVersion(ATTR_ZIMLETDOMAIN, "5.0.9"));

        Assert.assertTrue(am.inVersion(ATTR_ZIMLETDOMAIN, "5.0.10"));
        Assert.assertTrue(am.inVersion(ATTR_ZIMLETDOMAIN, "5.0.11"));
        Assert.assertTrue(am.inVersion(ATTR_ZIMLETDOMAIN, "5.5"));
        Assert.assertTrue(am.inVersion(ATTR_ZIMLETDOMAIN, "6"));

        Assert.assertTrue(am.inVersion(ATTR_TWO_SINCE, "8.0.8"));
        Assert.assertTrue(am.inVersion(ATTR_TWO_SINCE, "8.0.9"));
        Assert.assertTrue(am.inVersion(ATTR_TWO_SINCE, "8.5.1"));
        Assert.assertTrue(am.inVersion(ATTR_TWO_SINCE, "8.5.2"));
        Assert.assertTrue(am.inVersion(ATTR_TWO_SINCE, "9.0"));

        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "0"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "0.0"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "6"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "7"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "7.2"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "7.2.7"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "7.1.9"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "8.5.0"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "7.2.5"));
        Assert.assertFalse(am.inVersion(ATTR_TWO_SINCE, "7.2.7"));

        assertFuzzyMaintenaceReleaseCase(ATTR_TWO_SINCE, "8.4.1", true);
        assertFuzzyMaintenaceReleaseCase(ATTR_TWO_SINCE, "8.2.2", true);

        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "7.2.8"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "7.2.9"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "8.0.8"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "8.0.9"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "8.5.2"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "9.0"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "9.0.0"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "9.0.1"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "9.1.1"));
        Assert.assertTrue(am.inVersion(ATTR_MULTI_SINCE, "10.0.0"));

        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "0"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "0.0"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "6"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "7"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "7.2"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "7.1.9"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "8.5.0"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "8.5.1"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "7.2.5"));
        Assert.assertFalse(am.inVersion(ATTR_MULTI_SINCE, "7.2.7"));

        assertFuzzyMaintenaceReleaseCase(ATTR_MULTI_SINCE, "8.4.1", true);
    }

    @Test
    public void testBeforeVersion() throws Exception {

        Assert.assertTrue(am.beforeVersion(ATTR_ZIMBRAID , "0"));
        Assert.assertTrue(am.beforeVersion(ATTR_ZIMBRAID , "5.0.10"));

        Assert.assertFalse(am.beforeVersion(ATTR_ZIMLETDOMAIN, "0"));
        Assert.assertFalse(am.beforeVersion(ATTR_ZIMLETDOMAIN, "5.0.9"));
        Assert.assertFalse(am.beforeVersion(ATTR_ZIMLETDOMAIN, "5.0.10"));

        Assert.assertTrue(am.beforeVersion(ATTR_ZIMLETDOMAIN, "5.0.11"));
        Assert.assertTrue(am.beforeVersion(ATTR_ZIMLETDOMAIN, "5.5"));
        Assert.assertTrue(am.beforeVersion(ATTR_ZIMLETDOMAIN, "6"));

        Assert.assertTrue(am.beforeVersion(ATTR_TWO_SINCE, "8.0.9"));
        Assert.assertTrue(am.beforeVersion(ATTR_TWO_SINCE, "8.5.2"));
        Assert.assertTrue(am.beforeVersion(ATTR_TWO_SINCE, "9.0"));

        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "8.0.8"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "8.5.1"));

        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "0"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "0.0"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "6"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "7"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "7.2"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "7.2.7"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "7.1.9"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "8.5.0"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "7.2.5"));
        Assert.assertFalse(am.beforeVersion(ATTR_TWO_SINCE, "7.2.7"));

        assertFuzzyMaintenaceReleaseCase(ATTR_TWO_SINCE, "8.4.1", false);

        Assert.assertTrue(am.beforeVersion(ATTR_MULTI_SINCE, "7.2.9"));
        Assert.assertTrue(am.beforeVersion(ATTR_MULTI_SINCE, "8.0.9"));
        Assert.assertTrue(am.beforeVersion(ATTR_MULTI_SINCE, "9.0.1"));
        Assert.assertTrue(am.beforeVersion(ATTR_MULTI_SINCE, "9.1.1"));
        Assert.assertTrue(am.beforeVersion(ATTR_MULTI_SINCE, "10.0.0"));

        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "7.2.8"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "8.0.8"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "8.5.2"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "9.0"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "9.0.0"));

        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "0"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "0.0"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "6"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "7"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "7.2"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "7.1.9"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "8.5.0"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "8.5.1"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "7.2.5"));
        Assert.assertFalse(am.beforeVersion(ATTR_MULTI_SINCE, "7.2.7"));

        assertFuzzyMaintenaceReleaseCase(ATTR_MULTI_SINCE, "8.4.1", false);

    }

    private void assertFuzzyMaintenaceReleaseCase(String attrName, String version, boolean in) throws ServiceException {
        //this case is fuzzy, but we have to have it one way or the other
        //i.e. if attr is added in 8.0.8 and 8.5.1 does it appear in 8.4.1?
        //that depends if 8.4.1 is a predecessor of 8.5 or a successor of 8.0.8 chronologically and branchwise
        //but we can make a rule here; and it should be rare
        //if attr was added in 8.0.8 and 8.5.1, then later we had a 8.4.1 we have to assume it is a successor of 8.0.8
        //otherwise the attr needs to list 8.4.x specifically
        boolean check = in ? am.inVersion(attrName, version) : am.beforeVersion(attrName, version);
        Assert.assertTrue(check);
    }

    @Test
    public void testIsFuture() {
        Assert.assertTrue(am.isFuture(ATTR_FUTURE));
        Assert.assertFalse(am.isFuture(ATTR_MULTI_SINCE));
        Assert.assertFalse(am.isFuture(ATTR_TWO_SINCE));
        Assert.assertFalse(am.isFuture(ATTR_ZIMBRAID));
        Assert.assertFalse(am.isFuture(ATTR_ZIMLETDOMAIN));
    }

    @Test
    public void testAddedIn() throws ServiceException {
        Assert.assertTrue(am.addedIn(ATTR_ZIMLETDOMAIN, "5.0.10"));

        Assert.assertTrue(am.addedIn(ATTR_TWO_SINCE, "8.0.8"));
        Assert.assertTrue(am.addedIn(ATTR_TWO_SINCE, "8.5.1"));

        Assert.assertTrue(am.addedIn(ATTR_MULTI_SINCE, "9.0.0"));
        Assert.assertTrue(am.addedIn(ATTR_MULTI_SINCE, "8.0.8"));
        Assert.assertTrue(am.addedIn(ATTR_MULTI_SINCE, "7.2.8"));
        Assert.assertTrue(am.addedIn(ATTR_MULTI_SINCE, "8.5.2"));

        Assert.assertFalse(am.addedIn(ATTR_ZIMLETDOMAIN, "5.0.11"));
        Assert.assertFalse(am.addedIn(ATTR_ZIMLETDOMAIN, "5.0.9"));
        Assert.assertFalse(am.addedIn(ATTR_ZIMLETDOMAIN, "5.0"));
        Assert.assertFalse(am.addedIn(ATTR_ZIMLETDOMAIN, "6.0"));
        Assert.assertFalse(am.addedIn(ATTR_ZIMLETDOMAIN, "7.0"));

        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.5.3"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.5.1"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "9.0.1"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.0.7"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.0.9"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "7.2.7"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "7.2.9"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.5.7"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.5.3"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.5.1"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.0"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "8.1"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "7.1"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "7.2"));
        Assert.assertFalse(am.addedIn(ATTR_MULTI_SINCE, "10.0"));

    }
}
