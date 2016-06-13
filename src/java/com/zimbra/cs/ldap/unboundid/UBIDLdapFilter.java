/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap.unboundid;

import com.unboundid.ldap.sdk.Filter;

import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public class UBIDLdapFilter extends ZLdapFilter {

    private Filter filter;

    UBIDLdapFilter(FilterId filterId, Filter filter) {
        super(filterId);
        this.filter = filter;
    }

    @Override
    public void debug() {
    }

    Filter getNative() {
        return filter;
    }

    @Override
    public String toFilterString() {
        // cannot use this one, assertion values are all turned to lower case
        // return getNative().toNormalizedString();
        return getNative().toString();
    }
}
