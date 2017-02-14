/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST)
public class ClearTwoFactorAuthDataRequest {

    @XmlElement(name=AdminConstants.E_COS, required=false)
    private CosSelector cos;

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private AccountSelector account;

    @XmlAttribute(name=AdminConstants.A_LAZY_DELETE, required=false)
    private ZmBoolean lazyDelete;

    public ClearTwoFactorAuthDataRequest() {}

    public void setCos(CosSelector cos) {this.cos = cos; }
    public CosSelector getCos() {return cos; }
    public void setAccount(AccountSelector account) {this.account = account; }
    public AccountSelector getAccount() {return account; }
    public void setLazyDelete(ZmBoolean lazy) {this.lazyDelete = lazy; }
    public ZmBoolean getLazyDelete() {return lazyDelete; }
}
