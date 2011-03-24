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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.admin.type.GalMode;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_CREATE_GAL_SYNC_ACCOUNT_REQUEST)
public class CreateGalSyncAccountRequest extends AdminAttrsImpl {

    // AdminService.getAttrs called on server side
    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
    private final String name;

    // Name of pre-existing domain.
    @XmlAttribute(name=AdminConstants.E_DOMAIN, required=true)
    private final String domain;

    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final GalMode type;

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private final AccountSelector account;

    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=false)
    private final String password;

    @XmlAttribute(name=AdminConstants.E_FOLDER, required=false)
    private final String folder;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateGalSyncAccountRequest() {
        this((String) null, (String) null, (GalMode) null,
                (AccountSelector) null, (String) null, (String) null);
    }

    public CreateGalSyncAccountRequest(String name, String domain,
            GalMode type, AccountSelector account, String password,
            String folder) {
        this.name = name;
        this.domain = domain;
        this.type = type;
        this.account = account;
        this.password = password;
        this.folder = folder;
    }

    public String getName() { return name; }
    public String getDomain() { return domain; }
    public GalMode getType() { return type; }
    public AccountSelector getAccount() { return account; }
    public String getPassword() { return password; }
    public String getFolder() { return folder; }
}
