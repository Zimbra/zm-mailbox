/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Map;

/**
 * @author schemers
 */
public class Identity extends Entry implements Comparable {

    private String mName;

    public Identity(String name, Map<String, Object> attrs) {
        super(attrs, null);
        mName = name;
    }

    public String getName() {
        return mName;
    }
    
    public int compareTo(Object obj) {
        if (!(obj instanceof Identity))
            return 0;
        Identity other = (Identity) obj;
        return getName().compareTo(other.getName());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(": { name=").append(getName());
        sb.append(getAttrs().toString());
        sb.append("}");
        return sb.toString();
    }
}


