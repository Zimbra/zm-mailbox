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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Remove Account Alias
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REMOVE_ACCOUNT_ALIAS_REQUEST)
@XmlType(propOrder = {})
public class RemoveAccountAliasRequest {

    /**
     * @zm-api-field-tag value-of-zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=false)
    private final String id;

    /**
     * @zm-api-field-description Alias
     */
    @XmlAttribute(name=AdminConstants.E_ALIAS, required=true)
    private final String alias;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private RemoveAccountAliasRequest() {
        this(null, null);
    }

    public RemoveAccountAliasRequest(String id, String alias) {
        this.id = id;
        this.alias = alias;
    }

    public String getId() { return id; }
    public String getAlias() { return alias; }
}
