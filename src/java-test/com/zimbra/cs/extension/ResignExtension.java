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

import com.zimbra.cs.extension.ExtensionException;
import com.zimbra.cs.extension.ZimbraExtension;

/**
 * Test extension that lets the framework disable this extension.
 *
 * @author ysasaki
 */
public class ResignExtension implements ZimbraExtension {

    private static boolean destroyed = false;

    public String getName() {
        return "resign";
    }

    public void init() throws ExtensionException {
        destroyed = false;
        throw new ExtensionException("voluntarily resigned");
    }

    public void destroy() {
        destroyed = true;
    }

    static boolean isDestroyed() {
        return destroyed;
    }

}
