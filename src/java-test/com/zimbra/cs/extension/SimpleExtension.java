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
