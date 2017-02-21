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

import com.zimbra.cs.extension.ZimbraExtension;

/**
 * Simple extension for testing.
 *
 * @author ysasaki
 */
public class SimpleExtension implements ZimbraExtension {
    private boolean initialized = false;
    private boolean destroyed = false;

    public String getName() {
        return "simple";
    }

    public void init() {
        initialized = true;
    }

    public void destroy() {
        destroyed = true;
    }

    boolean isInitialized() {
        return initialized;
    }

    boolean isDestroyed() {
        return destroyed;
    }

}
