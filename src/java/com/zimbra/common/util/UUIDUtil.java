/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.util.UUID;

// just a wrapper to call UUID.randomUUID()
// so we can use another UUID generator if we want to (highly unlikely though)
public class UUIDUtil {

    /**
     * Returns a new UUID.
     * @return
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
