/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
