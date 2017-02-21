/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.util.SpoolingCache;

public class SpoolingCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        new File("build/test").mkdirs();
        LC.zimbra_tmp_directory.setDefault("build/test");
    }

    @AfterClass
    public static void destroy() throws Exception {
        new File("build/test").delete();
    }

    private static final String[] STRINGS = new String[] { "foo", "bar", "baz" };

    private void test(SpoolingCache<String> scache, boolean shouldSpool) throws IOException {
        for (String v : STRINGS) {
            scache.add(v);
        }

        Assert.assertEquals("spooled", shouldSpool, scache.isSpooled());
        Assert.assertEquals("entry count matches", STRINGS.length, scache.size());
        int i = 0;
        for (String v : scache) {
            Assert.assertEquals("entry matched: #" + i, STRINGS[i++], v);
        }
        Assert.assertEquals("correct number of items iterated", STRINGS.length, i);

    }

    @Test
    public void memory() throws Exception {
        SpoolingCache<String> scache = new SpoolingCache<String>(STRINGS.length + 3);
        test(scache, false);
        scache.cleanup();
    }

    @Test
    public void disk() throws Exception {
        SpoolingCache<String> scache = new SpoolingCache<String>(0);
        test(scache, true);
        scache.cleanup();
    }

    @Test
    public void both() throws Exception {
        SpoolingCache<String> scache = new SpoolingCache<String>(1);
        test(scache, true);
        scache.cleanup();
    }

}
