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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.XMbxSearchConstants;
import com.zimbra.soap.admin.type.SearchID;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-description Attempts to delete a search task.
 * <br />
 * Returns empty <b>&lt;DeleteXMbxSearchResponse/></b> element on success or Fault document on error.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=XMbxSearchConstants.E_DELETE_XMBX_SEARCH_REQUEST)
public class DeleteXMbxSearchRequest {

    /**
     * @zm-api-field-description Search task information
     */
    @XmlElement(name=XMbxSearchConstants.E_SrchTask /* searchtask */, required=true)
    private final SearchID searchTask;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */,  required=false)
    private AccountSelector account;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DeleteXMbxSearchRequest() {
        this((SearchID) null, (AccountSelector)null);
    }

    /*public DeleteXMbxSearchRequest(SearchID searchTask) {
        this.searchTask = searchTask;
    }*/
    public DeleteXMbxSearchRequest(SearchID searchTask, AccountSelector account) {
        this.searchTask = searchTask;
        this.account = account;
    }
    public AccountSelector getAccount() {return account; }
    public SearchID getSearchTask() { return searchTask; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("searchTask", searchTask);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
