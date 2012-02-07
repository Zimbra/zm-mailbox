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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.XMbxSearchConstants;
import com.zimbra.soap.admin.type.SearchNode;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=XMbxSearchConstants.E_GET_XMBX_SEARCHES_RESPONSE)
@XmlType(propOrder = {})
public class GetXMbxSearchesListResponse {

    /**
     * @zm-api-field-description Search task information
     */
    @XmlElement(name=XMbxSearchConstants.E_SrchTask /* searchtask */, required=false)
    private List<SearchNode> searchNodes = Lists.newArrayList();

    public GetXMbxSearchesListResponse() {
    }

    public void setSearchNodes(Iterable <SearchNode> searchNodes) {
        this.searchNodes.clear();
        if (searchNodes != null) {
            Iterables.addAll(this.searchNodes,searchNodes);
        }
    }

    public void addSearchNode(SearchNode searchNode) {
        this.searchNodes.add(searchNode);
    }

    public List<SearchNode> getSearchNodes() {
        return Collections.unmodifiableList(searchNodes);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("searchNodes", searchNodes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
