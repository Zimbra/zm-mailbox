/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
        classpath = new File("build/test/extensions").toURI().toURL();
        LC.zimbra_extension_common_directory.setDefault(null);
        LC.zimbra_extension_directory.setDefault(null);
    }

    public static void registerExtension(String extensionClassName) throws Exception {
        ExtensionUtil.addClassLoader(new ZimbraExtensionClassLoader(classpath,extensionClassName));
    }


}
