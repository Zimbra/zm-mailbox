/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_FETCH_ALL_REMOTE_ACCOUNTS_RESPONSE)
public class FetchAllRemoteAccountsResponse {

    /**
     * @zm-api-field-description Information on accounts
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private List <AccountInfo> accountList = Lists.newArrayList();

    public FetchAllRemoteAccountsResponse() {
    }

    public void setAccountList(Iterable <AccountInfo> accounts) {
        this.accountList.clear();
        if (accounts != null) {
            Iterables.addAll(this.accountList, accounts);
        }
    }

    public void addAccount(AccountInfo account ) {
        this.accountList.add(account);
    }

    public List <AccountInfo> getAccountList() {
        return Collections.unmodifiableList(accountList);
    }
}
