/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedValue;
import com.zimbra.common.util.ZimbraLog;
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ADMIN_SAVED_SEARCHES_RESPONSE)
public class GetAdminSavedSearchesResponse {

    /**
     * @zm-api-field-description Information on saved searches
     */
    @XmlElement(name=AdminConstants.E_SEARCH, required=false)
    private List<NamedValue> searches = Lists.newArrayList();

    public GetAdminSavedSearchesResponse() {
    }

    public void setSearches(Iterable <NamedValue> searches) {
        this.searches.clear();
        if (searches != null) {
            Iterables.addAll(this.searches,searches);
        }
    }

    public GetAdminSavedSearchesResponse addSearch(NamedValue search) {

        this.searches.add(search);
        ZimbraLog.mailbox.info("----------- >>>>>>>>>"+  this.getSearches());
        return this;
    }

    public List<NamedValue> getSearches() {
        return Collections.unmodifiableList(searches);
    }
}
