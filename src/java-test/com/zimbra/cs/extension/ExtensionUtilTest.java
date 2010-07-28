/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.extension;

import java.io.File;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

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
        LC.zimbra_extensions_common_directory.setDefault(null);
        LC.zimbra_extensions_directory.setDefault(null);
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
