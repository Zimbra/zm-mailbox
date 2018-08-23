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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.XMbxSearchConstants;
import com.zimbra.soap.admin.type.SearchNode;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=XMbxSearchConstants.E_CREATE_XMBX_SEARCH_RESPONSE)
@XmlType(propOrder = {})
public class CreateXMbxSearchResponse {

    /**
     * @zm-api-field-description Search task information
     */
    @XmlElement(name=XMbxSearchConstants.E_SrchTask /* searchtask */, required=true)
    private final SearchNode searchNode;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateXMbxSearchResponse() {
        this((SearchNode) null);
    }

    public CreateXMbxSearchResponse(SearchNode searchNode) {
        this.searchNode = searchNode;
    }

    public SearchNode getSearchNode() { return searchNode; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("searchNode", searchNode);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
