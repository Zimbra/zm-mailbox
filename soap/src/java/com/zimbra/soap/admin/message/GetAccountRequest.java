/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get attributes related to an account
 * <br />
 * <b>{request-attrs}</b> - comma-seperated list of attrs to return
 * <br />
 * <br />
 * Note: this request is by default proxied to the account's home server
 * <br />
 * <br />
 * <b>Access</b>: domain admin sufficient
 */
@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_REQUEST)
public class GetAccountRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-tag apply-cos
     * @zm-api-field-description Flag whether or not to apply class of service (COS) rules
     * <table>
     * <tr> <td> <b>1 (true) [default]</b> </td> <td> COS rules apply and unset attrs on an account will get their
     *                                                value from the COS </td> </tr>
     * <tr> <td> <b>0 (false)</b> </td> <td> only attributes directly set on the account will be returned </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_APPLY_COS, required=false)
    private ZmBoolean applyCos = ZmBoolean.ONE /* true */;

    /**
     * @zm-api-field-tag effectiveQuota
     * @zm-api-field-description Flag whether or not to get effective value (minimum of zimbraMailQuota and zimbraMailDomainQuota)
     * <table>
     * <tr> <td> <b>1 (true)</b> </td> <td> zimbraMailQuota attribute will contain effective value
     * <tr> <td> <b>0 (false)[default]</b> </td> <td> zimbraMailQuota attribute will contain actual ldap value set </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_EFFECTIVE_QUOTA, required=false)
    private ZmBoolean effectiveQuota;
    
    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT)
    private AccountSelector account;

    public GetAccountRequest() {
    }

    public GetAccountRequest(AccountSelector account) {
        this(account, (Boolean) null, (Iterable<String>) null);
    }

    public GetAccountRequest(AccountSelector account, Boolean applyCos) {
        this(account, applyCos, (Iterable<String>) null);
    }

    public GetAccountRequest(AccountSelector account, Boolean applyCos,
            Iterable<String> attrs) {
        this(account, applyCos, false, attrs);
    }

    public GetAccountRequest(AccountSelector account, Boolean applyCos,
            Boolean effectiveQuota, Iterable<String> attrs) {
        super(attrs);
        this.account = account;
        this.applyCos = ZmBoolean.fromBool(applyCos);
        this.effectiveQuota = ZmBoolean.fromBool(effectiveQuota);
    }

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public void setApplyCos(Boolean applyCos) {
        this.applyCos = ZmBoolean.fromBool(applyCos);
    }

    public void setEffectiveQuota(Boolean effectiveQuota) {
        this.effectiveQuota = ZmBoolean.fromBool(effectiveQuota);
    }

    public AccountSelector getAccount() { return account; }
    public Boolean isApplyCos() { return ZmBoolean.toBool(applyCos); }
    public Boolean isEffectiveQuota() { return ZmBoolean.toBool(effectiveQuota); }
}
