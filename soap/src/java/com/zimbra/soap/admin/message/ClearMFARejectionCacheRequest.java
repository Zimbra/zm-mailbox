/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Clear the MFA rejection cache for a specific account or all accounts.
 * <br />
 * This operation is restricted to global admins only.
 * <br />
 * e.g. Clear for a specific account:
 * <pre>
 *     &lt;ClearMFARejectionCacheRequest>
 *        &lt;account by="name">user@domain.com&lt;/account>
 *     &lt;/ClearMFARejectionCacheRequest>
 * </pre>
 * e.g. Clear for all accounts:
 * <pre>
 *     &lt;ClearMFARejectionCacheRequest/>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_CLEAR_MFA_REJECTION_CACHE_REQUEST)
@XmlType(propOrder = {})
public class ClearMFARejectionCacheRequest {

    /**
     * @zm-api-field-tag account
     * @zm-api-field-description Account to clear MFA rejection cache for.
     * If not specified, clears cache for all accounts.
     */
    @XmlElement(name = AdminConstants.E_ACCOUNT, required = false)
    private AccountSelector account;

    /**
     * no-argument constructor wanted by JAXB.
     */
    public ClearMFARejectionCacheRequest() {
        this(null);
    }

    public ClearMFARejectionCacheRequest(AccountSelector account) {
        this.account = account;
    }

    public AccountSelector getAccount() {
        return account;
    }
}
