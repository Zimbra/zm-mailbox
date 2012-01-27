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
