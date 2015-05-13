/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.extension;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link ExtensionManager}.
 *
 * @author ysasaki
 */
public class ExtensionManagerTest {
    private static URL classpath;

    @BeforeClass
    public static void init() throws Exception {
        classpath = new File("build/test-classes").toURI().toURL();
        LC.zimbra_extension_common_directory.setDefault(null);
        LC.zimbra_extension_directory.setDefault(null);
        MailboxTestUtil.initServer();
    }

    @Test
    public void simple() throws Exception {
        ExtensionManager extensionManager = ExtensionManager.getInstance();
        extensionManager.addClassLoader(new ZimbraExtensionClassLoader(classpath, SimpleExtension.class.getName()));
        extensionManager.initAll();
        SimpleExtension ext = (SimpleExtension) extensionManager.getExtension("simple");
        Assert.assertNotNull(ext);
        Assert.assertTrue(ext.isInitialized());
        Assert.assertFalse(ext.isDestroyed());
    }

    @Test
    public void resign() throws Exception {
        ExtensionManager extensionManager = ExtensionManager.getInstance();
        extensionManager.addClassLoader(new ZimbraExtensionClassLoader(classpath, ResignExtension.class.getName()));
        extensionManager.initAll();
        Assert.assertNull(extensionManager.getExtension("resign"));
        Assert.assertTrue(ResignExtension.isDestroyed());
    }

    @Test
    public void hiddenDefaultPorts() throws Exception {
        ExtensionManager extensionManager = ExtensionManager.getInstance();
        extensionManager.addClassLoader(new ZimbraExtensionClassLoader(classpath, HiddenDefaultPortsExtension.class.getName()));
        extensionManager.initAll();
        ZimbraExtension ext = extensionManager.getExtension("hiddenDefaultPorts");
        Assert.assertTrue(ext.getClass().isAnnotationPresent(ZimbraExtension.HideFromDefaultPorts.class));
    }
}
