/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.BulkAction;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description SearchAction
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_SEARCH_ACTION_REQUEST)
final public class SearchActionRequest {

    /**
     * @zm-api-field-tag search-request
     * @zm-api-field-description Search request
     */
    @XmlElement(name = MailConstants.E_SEARCH_REQUEST /* SearchRequest */ , required = true)
    private SearchRequest searchRequest;

    /**
     * @zm-api-field-tag bulk-action
     * @zm-api-field-description Bulk action
     */
    @XmlElement(name = MailConstants.E_BULK_ACTION /* BulkAction */, required = true)
    private BulkAction bulkAction;

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(SearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }

    public BulkAction getBulkAction() {
        return bulkAction;
    }

    public void setBulkAction(BulkAction bulkAction) {
        this.bulkAction = bulkAction;
    }
}
