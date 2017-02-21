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
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Returns admin saved searches.
 * <br />
 * If no <b>&lt;search></b> is present server will return all saved searches.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ADMIN_SAVED_SEARCHES_REQUEST)
public class GetAdminSavedSearchesRequest {

    /**
     * @zm-api-field-description Search information
     */
    @XmlElement(name=AdminConstants.E_SEARCH, required=false)
    private List<NamedElement> searches = Lists.newArrayList();

    public GetAdminSavedSearchesRequest() {
    }

    public void setSearches(Iterable <NamedElement> searches) {
        this.searches.clear();
        if (searches != null) {
            Iterables.addAll(this.searches,searches);
        }
    }

    public GetAdminSavedSearchesRequest addSearch(NamedElement search) {
        this.searches.add(search);
        return this;
    }

    public List<NamedElement> getSearches() {
        return Collections.unmodifiableList(searches);
    }
}
