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

/**
 * @zm-api-command-description Add a GalSync data source
 * <br />
 * Access: domain admin sufficient
 * <br />
 * Notes:
 * <ul>
 * <li> Add additional data sources to the existing galsync account.
 * <li> non-existing account causes an exception.
 * <li> name attribute is for the name of the data source.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADD_GAL_SYNC_DATASOURCE_REQUEST)
public class AddGalSyncDataSourceRequest extends AdminAttrsImpl {

    // AdminService.getAttrs called on server side
    /**
     * @zm-api-field-tag datasource-name
     * @zm-api-field-description Name of the data source
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag domain-name
     * @zm-api-field-description Name of pre-existing domain
     */
    @XmlAttribute(name=AdminConstants.E_DOMAIN /* domain */, required=true)
    private final String domain;

    /**
     * @zm-api-field-description GalMode type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final GalMode type;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=true)
    private final AccountSelector account;

    /**
     * @zm-api-field-tag contact-folder-name
     * @zm-api-field-description Contact folder name
     */
    @XmlAttribute(name=AdminConstants.E_FOLDER /* folder */, required=false)
    private final String folder;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AddGalSyncDataSourceRequest() {
        this((String) null, (String) null, (GalMode) null,
                (AccountSelector) null, (String) null, (String) null);
    }

    public AddGalSyncDataSourceRequest(String name, String domain,
            GalMode type, AccountSelector account, String password,
            String folder) {
        this.name = name;
        this.domain = domain;
        this.type = type;
        this.account = account;
        this.folder = folder;
    }

    public String getName() { return name; }
    public String getDomain() { return domain; }
    public GalMode getType() { return type; }
    public AccountSelector getAccount() { return account; }
    public String getFolder() { return folder; }
}
