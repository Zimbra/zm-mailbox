/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.ldap;

import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public abstract class ZLdapFilter extends ZLdapElement {
    
    private FilterId filterId;
    
    protected ZLdapFilter(FilterId filterId) {
        this.filterId = filterId;
    }
    
    public abstract String toFilterString();
    
    public String getStatString() {
        // TODO: mandate factory
        return filterId == null ? "" : filterId.getStatString();
        // return filterId.name();
    }

    
}
