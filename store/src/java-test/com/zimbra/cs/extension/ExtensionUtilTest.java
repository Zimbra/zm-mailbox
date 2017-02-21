/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.extension;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;

/**
 * Unit test for {@link ExtensionUtil}.
 *
 * @author ysasaki
 */
public class ExtensionUtilTest {
    private static URL classpath;

    @BeforeClass
    public static void init() throws Exception {
        classpath = new File("build/test-classes").toURI().toURL();
        LC.zimbra_extension_common_directory.setDefault(null);
        LC.zimbra_extension_directory.setDefault(null);
    }

    @Test
    public void simple() throws Exception {
        ExtensionUtil.addClassLoader(new ZimbraExtensionClassLoader(classpath,
                SimpleExtension.class.getName()));
        ExtensionUtil.initAll();
        SimpleExtension ext =
            (SimpleExtension) ExtensionUtil.getExtension("simple");
        Assert.assertNotNull(ext);
        Assert.assertTrue(ext.isInitialized());
        Assert.assertFalse(ext.isDestroyed());
    }

    @Test
    public void resign() throws Exception {
        ExtensionUtil.addClassLoader(new ZimbraExtensionClassLoader(classpath,
                ResignExtension.class.getName()));
        ExtensionUtil.initAll();
        Assert.assertNull(ExtensionUtil.getExtension("resign"));
        Assert.assertTrue(ResignExtension.isDestroyed());
    }

}
