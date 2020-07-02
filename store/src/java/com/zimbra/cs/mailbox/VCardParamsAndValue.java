/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.List;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

public final class VCardParamsAndValue {
    private final Set<String> params = Sets.newHashSet();
    // Technically, a property can have multiple values but we treat it as one value
    private final String value;

    public VCardParamsAndValue(String value) {
        this.value = value;
    }

    public VCardParamsAndValue(String value, Set<String> params) {
        this.value = value;
        if (params != null) {
            this.params.addAll(params);
        }
    }

    public String getValue() {
        return value;
    }

    public Set<String> getParams() {
        return params;
    }

    public static String getFirstValue(String key, ListMultimap<String, VCardParamsAndValue> mmap) {
        List<VCardParamsAndValue> vals = mmap.get(key);
        if (vals == null || vals.isEmpty()) {
            return null;
        }
        return vals.get(0).getValue();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("params", params).add("value",value).toString();
    }
}
