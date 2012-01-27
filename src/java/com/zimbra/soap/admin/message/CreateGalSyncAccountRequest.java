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
 * @zm-api-command-description Create Global Address List (GAL) Synchronisation account
 * <br />
 * Notes:
 * <ul>
 * <li> if the referenced account is not found it will be created.
 * <li> the identifier used in name attr is used for SyncGal and SearchGal.
 * <li> name attribute is for the name of the data source.
 * <li> if folder attr is not present it'll default to Contacts folder.
 * <li> passed in attrs in &lt;a/> are used to initialize the gal data source.
 * <li> server is a required parameter and specifies the mailhost on which this account resides.
 * </ul>
 * Example for creating gal sync account for Zimbra LDAP server.
 * <pre>
    &lt;CreateGalSyncAccountRequest name="zimbra" domain="mydomain.com" type="zimbra" server="mailhost.mydomain.com">
      &lt;account by="name">gal@mydomain.com&lt;/account>
    &lt;/CreateGalSyncAccountRequest>
 * </pre>
 * Example for creating domain gal sync account.
 * <pre>
    &lt;CreateGalSyncAccountRequest name="mydomain" domain="mydomain.com" type="ldap" server="mailhost.mydomain.com">
      &lt;account by="name">gal@mydomain.com&lt;/account>
      &lt;a n="zimbraGalSyncLdapURL">ldap://ldap.mydomain.com&lt;/a>
      &lt;a n="zimbraGalSyncLdapStartTlsEnabled">TRUE&lt;/a>
      &lt;a n="zimbraGalSyncLdapSearchBase">cn=users&lt;/a>
      &lt;a n="zimbraGalSyncLdapAuthMech">simple&lt;/a>
      &lt;a n="zimbraGalSyncLdapBindDn">uid=admin,cn=users&lt;/a>
      &lt;a n="zimbraGalSyncLdapBindPassword">password&lt;/a>
      &lt;a n="zimbraGalSyncLdapFilter">(&amp;(mail=*)(objectClass=user))&lt;/a>
      &lt;a n="zimbraGalLdapAttrMap">whenChanged,modifyTimeStamp=modifyTimeStamp&lt;/a>
      &lt;a n="zimbraGalLdapAttrMap">whenCreated,createTimeStamp=createTimeStamp&lt;/a>
      &lt;a n="zimbraGalLdapAttrMap">zimbraMailDeliveryAddress,zimbraMailAlias,mail=email,email2,email3,email4,email5,email6,email7,email8,email9,email10,email11,email12,email13,email14,email15,email16&lt;/a>
    &lt;/CreateGalSyncAccountRequest>
 * </pre>
 * <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_GAL_SYNC_ACCOUNT_REQUEST)
public class CreateGalSyncAccountRequest extends AdminAttrsImpl {

    // AdminService.getAttrs called on server side
    /**
     * @zm-api-field-tag identifier
     * @zm-api-field-description Name of the data source.
     */
    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
    private final String name;

    // Name of pre-existing domain.
    /**
     * @zm-api-field-tag domain-name
     * @zm-api-field-description Domain name
     */
    @XmlAttribute(name=AdminConstants.E_DOMAIN, required=true)
    private final String domain;

    /**
     * @zm-api-field-description GalMode type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final GalMode type;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private final AccountSelector account;

    /**
     * @zm-api-field-tag password
     * @zm-api-field-description
     */
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=false)
    private final String password;

    /**
     * @zm-api-field-tag contact-folder-name
     * @zm-api-field-description Contact folder name
     */
    @XmlAttribute(name=AdminConstants.E_FOLDER, required=false)
    private final String folder;

    /**
     * @zm-api-field-tag server
     * @zm-api-field-description The mailhost on which this account resides
     */
    @XmlAttribute(name=AdminConstants.A_SERVER, required=true)
    private final String mailHost;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateGalSyncAccountRequest() {
        this((String) null, (String) null, (GalMode) null,
                (AccountSelector) null, (String) null, (String) null, (String) null);
    }

    public CreateGalSyncAccountRequest(String name, String domain,
            GalMode type, AccountSelector account, String password,
            String folder, String mailHost) {
        this.name = name;
        this.domain = domain;
        this.type = type;
        this.account = account;
        this.password = password;
        this.folder = folder;
        this.mailHost = mailHost;
    }

    public String getName() { return name; }
    public String getDomain() { return domain; }
    public GalMode getType() { return type; }
    public AccountSelector getAccount() { return account; }
    public String getPassword() { return password; }
    public String getFolder() { return folder; }
    public String getMailHost() { return mailHost; }
}
