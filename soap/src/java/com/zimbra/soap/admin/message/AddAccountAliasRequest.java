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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Add an alias for the account
 * <br />
 * Access: domain admin sufficient.
 * <br />
 * Note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADD_ACCOUNT_ALIAS_REQUEST)
public class AddAccountAliasRequest {

    /**
     * @zm-api-field-tag value-of-zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=AdminConstants.E_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag alias
     * @zm-api-field-description Alias
     */
    @XmlAttribute(name=AdminConstants.E_ALIAS /* alias */, required=true)
    private final String alias;



    /**
     * @zm-api-field-tag zimbra alias to be hidden or not
     * @zm-api-field-description isHiddenAlias
     */
    @XmlAttribute(name=AdminConstants.E_IS_HIDDEN_ALIAS /* isHiddenAlias */, required=false)
    private  boolean isHiddenAlias;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AddAccountAliasRequest() {
        this((String)null, (String)null, false);
    }

    public AddAccountAliasRequest(String id, String alias) {
        this.id = id;
        this.alias = alias;
    }
    public AddAccountAliasRequest(String id, String alias, boolean isHiddenAlias) {
        this.id = id;
        this.alias = alias;
        this.isHiddenAlias = isHiddenAlias;
    }

    public String getId() { return id; }
    public String getAlias() { return alias; }

    public boolean isHiddenAlias() { return isHiddenAlias; }
}
