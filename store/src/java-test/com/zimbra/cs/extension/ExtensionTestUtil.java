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
package com.zimbra.cs.extension;

import com.zimbra.common.localconfig.LC;

import java.io.File;
import java.net.URL;

/**
 * Utility Class for Unit Test with Extension.
 * When unit test is executed, custom classes in java-test/com/zimbra/extensions is compiled
 * and copied to build/test/extensions/com/zimbra/extensions
 */
public class ExtensionTestUtil {
    private static URL classpath;

    public static void init() throws Exception {
        classpath = new File("store/build/test/extensions").toURI().toURL();
        LC.zimbra_extension_common_directory.setDefault(null);
        LC.zimbra_extension_directory.setDefault(null);
    }

    public static void registerExtension(String extensionClassName) throws Exception {
        ExtensionUtil.addClassLoader(new ZimbraExtensionClassLoader(classpath,extensionClassName));
    }


}
