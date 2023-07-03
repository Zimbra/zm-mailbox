/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;

/**
 * Unit test for {@link Version}.
 *
 * @author ysasaki
 */
public class VersionTest {

    @Test
    public void valid() throws Exception {
        new Version("6");
        new Version("6_M1");
        new Version("6_BETA1");
        new Version("6_RC1");
        new Version("6_GA");
        new Version("6.0");
        new Version("6.0_M1");
        new Version("6.0_BETA1");
        new Version("6.0_RC1");
        new Version("6.0_GA");
        new Version("6.0.0");
        new Version("6.0.0_M1");
        new Version("6.0.0_BETA1");
        new Version("6.0.0_RC1");
        new Version("6.0.0_GA");
        new Version("6.0.0_GA1");
    }

    @Test
    public void invalid() throws Exception {
        try {
            new Version("a.0.0");
            Assert.fail();
        } catch (ServiceException expected) {
        }

        try {
            new Version("6.0.0_GB");
            Assert.fail();
        } catch (ServiceException expected) {
        }

        try {
            new Version("5.0.12.1");
            Assert.fail();
        } catch (ServiceException expected) {
        }
    }

    @Test
    public void compareTo() throws Exception {
        Version v1 = new Version("5.0.10");
        Version v2 = new Version("5.0.9");
        Version v3 = new Version("5.0.10");
        Version v4 = new Version("5.0");
        Version future = new Version(Version.FUTURE);

        Assert.assertTrue(v1.compareTo(v2) > 0);
        Assert.assertTrue(v1.compareTo(v3) == 0);
        Assert.assertTrue(v2.compareTo(v1) < 0);
        Assert.assertTrue(v1.compareTo(v4) > 0);

        Assert.assertTrue(v1.compareTo(future) < 0);
        Assert.assertTrue(future.compareTo(v1) > 0);
        Assert.assertTrue(future.compareTo(future) == 0);

        Assert.assertTrue(Version.compare("5.0.10", "5.0.9") > 0);
        Assert.assertTrue(Version.compare("5.0.9", "5.0.10") < 0);
        Assert.assertTrue(Version.compare("5.0.10", "5.0.10") == 0);
    }

    @Test
    public void compare() throws Exception {
        Assert.assertEquals(1, Version.compare("5.0.10", "5.0.9"));
        Assert.assertEquals(0, Version.compare("5.0.10", "5.0.10"));
        Assert.assertEquals(-9, Version.compare("5.0", "5.0.9"));
        Assert.assertEquals(2, Version.compare("5.0.10_RC1", "5.0.10_BETA3"));
        Assert.assertEquals(1, Version.compare("5.0.10_GA", "5.0.10_RC2"));
        Assert.assertEquals(1, Version.compare("5.0.10", "5.0.10_RC2"));

        Assert.assertEquals(0, Version.compare("6.0.0_GA", "6.0.0"));
        Assert.assertEquals(0, Version.compare("6.0.0", "6.0.0_GA"));
        Assert.assertEquals(1, Version.compare("6.0.0_RC1", "6.0.0_RC"));
        Assert.assertEquals(-1, Version.compare("6.0.0_RC", "6.0.0_RC1"));

        Version v1 = new Version("8.0.0.621", false);
        Version v2 = new Version("7.9.16");
        Assert.assertEquals(1, v1.compareTo(v2));

        v1 = new Version("8.0.0.621", false);
        v2 = new Version("8.0.0");
        Assert.assertEquals(0, v1.compareTo(v2));

        v1 = new Version("8.0.0.621", false);
        v2 = new Version("8.0.1");
        Assert.assertEquals(-1, v1.compareTo(v2));

        v1 = new Version("8.0.0.621", false);
        v2 = new Version("9.0.0");
        Assert.assertEquals(-1, v1.compareTo(v2));
    }

    @Test
    public void releases() throws Exception {
        // all real releases we've made or will make
        String[] versions = new String[] {
                // "3.0.M1", this format is not not supported, we should not
                // have any customers upgrading from this version
                "3.0.0_M2", "3.0.0_M3", "3.0.0_M4", "3.0.0_GA", "3.0.1_GA",
                "3.1.0_GA", "3.1.1_GA", "3.1.2_GA", "3.1.3_GA", "3.1.4_GA",
                "3.2.0_M1", "3.2.0_M2", "4.0.0_RC1", "4.0.0_GA", "4.0.1_GA",
                "4.0.2_GA", "4.0.3_GA", "4.0.4_GA", "4.0.5_GA", "4.1.0_BETA1",
                "4.5.0_BETA1", "4.5.0_BETA2", "4.5.0_RC1", "4.5.0_RC2",
                "4.5.0_GA", "4.5.1_GA", "4.5.2_GA", "4.5.3_GA", "4.5.4_GA",
                "4.5.5_GA", "4.5.6_GA", "4.5.7_GA", "4.5.8_GA", "4.5.9_GA",
                "4.5.10_GA", "4.5.11_GA", "5.0.0_BETA1", "5.0.0_BETA2",
                "5.0.0_BETA3", "5.0.0_BETA4", "5.0.0_RC1", "5.0.0_RC2",
                "5.0.0_RC3", "5.0.0_GA", "5.0.1_GA", "5.0.2_GA", "5.0.3_GA",
                "5.0.4_GA", "5.0.5_GA", "5.0.6_GA", "5.0.7_GA", "5.0.8_GA",
                "5.0.9_GA", "5.0.10_GA", "5.0.11_GA", "5.0.12_GA", "6.0.0_BETA1",
                "6.0.0_BETA2", "6.0.0_RC1", "6.0.0_RC2", "6.0.0_GA"
        };

        for (int i = 0; i< versions.length - 1; i++) {
            Assert.assertTrue(Version.compare(versions[i], versions[i]) == 0);
            for (int j = i + 1; j < versions.length; j++) {
                Assert.assertTrue(Version.compare(versions[j], versions[i]) > 0);
            }
        }
    }

    @Test
    public void checkPatchRelease() throws Exception {
        Version currentVersion = new Version("10.0.0_GA_4518");
        Version lastVersion = new Version("10.1.0_GA_4549");
        Assert.assertFalse(currentVersion.isSameMinorRelease(lastVersion));
        currentVersion = new Version("10.0.0_GA_4518");
        lastVersion = new Version("11.0.0_GA_4549");
        Assert.assertFalse(currentVersion.isSameMinorRelease(lastVersion));
        currentVersion = new Version("10.0.0_GA_4518");
        lastVersion = new Version("10.0.1_GA_454");
        Assert.assertTrue(currentVersion.isSameMinorRelease(lastVersion));
    }

}
