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

package com.zimbra.soap.adminext.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.AttrsImpl;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminExtConstants.E_GENERATE_BULK_PROV_FROM_LDAP_REQUEST)
public class GenerateBulkProvisionFileFromLDAPRequest extends AttrsImpl {

    @XmlElement(name=AdminExtConstants.A_password
                /* password */, required=false)
    private String password;

    // true if "true" (case insensitive) else false - default value "false"
    @XmlElement(name=AdminExtConstants.A_generatePassword
                /* generatePassword */, required=false)
    private String generatePassword;

    @XmlElement(name=AdminExtConstants.A_genPasswordLength
                /* genPasswordLength */, required=false)
    private Integer genPasswordLength;

    @XmlElement(name=AdminExtConstants.A_fileFormat
                /* fileFormat */, required=false)
    private String fileFormat;

    @XmlElement(name=AdminExtConstants.E_mustChangePassword
                /* mustChangePassword */, required=true)
    private String mustChangePassword;

    @XmlElement(name=AdminExtConstants.A_maxResults
                /* maxResults */, required=false)
    private Integer maxResults;

    @XmlElement(name=AdminExtConstants.E_SMTPHost
                /* SMTPHost */, required=false)
    private String SMTPHost;

    @XmlElement(name=AdminExtConstants.E_SMTPPort
                /* SMTPPort */, required=false)
    private String SMTPPort;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_importMails
                /* importMails */, required=false)
    private String importMails;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_importContacts
                /* importContacts */, required=false)
    private String importContacts;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_importCalendar
                /* importCalendar */, required=false)
    private String importCalendar;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_importTasks
                /* importTasks */, required=false)
    private String importTasks;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_importJunk
                /* importJunk */, required=false)
    private String importJunk;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_importDeletedItems
                /* importDeletedItems */, required=false)
    private String importDeletedItems;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_ignorePreviouslyImported
                /* ignorePreviouslyImported */, required=false)
    private String ignorePreviouslyImported;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_InvalidSSLOk
                /* InvalidSSLOk */, required=false)
    private String invalidSSLOk;

    @XmlElement(name=AdminExtConstants.E_MapiProfile
                /* MapiProfile */, required=false)
    private String mapiProfile;

    @XmlElement(name=AdminExtConstants.E_MapiServer
                /* MapiServer */, required=false)
    private String mapiServer;

    @XmlElement(name=AdminExtConstants.E_MapiLogonUserDN
                /* MapiLogonUserDN */, required=false)
    private String mapiLogonUserDN;

    @XmlElement(name=AdminExtConstants.E_ZimbraAdminLogin
                /* ZimbraAdminLogin */, required=false)
    private String zimbraAdminLogin;

    @XmlElement(name=AdminExtConstants.E_ZimbraAdminPassword
                /* ZimbraAdminPassword */, required=false)
    private String zimbraAdminPassword;

    @XmlElement(name=AdminExtConstants.E_TargetDomainName
                /* TargetDomainName */, required=false)
    private String targetDomainName;

    // true if "TRUE" (case insensitive)
    @XmlElement(name=AdminExtConstants.E_provisionUsers
                /* provisionUsers */, required=false)
    private String provisionUsers;

    public GenerateBulkProvisionFileFromLDAPRequest() {
    }

    public void setPassword(String password) { this.password = password; }
    public void setGeneratePassword(String generatePassword) {
            this.generatePassword = generatePassword;
    }
    public void setGenPasswordLength(Integer genPasswordLength) {
            this.genPasswordLength = genPasswordLength;
    }
    public void setFileFormat(String fileFormat) {
            this.fileFormat = fileFormat;
    }
    public void setMustChangePassword(String mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
    public void setSMTPHost(String SMTPHost) { this.SMTPHost = SMTPHost; }
    public void setSMTPPort(String SMTPPort) { this.SMTPPort = SMTPPort; }
    public void setImportMails(String importMails) {
        this.importMails = importMails;
    }
    public void setImportContacts(String importContacts) {
        this.importContacts = importContacts;
    }
    public void setImportCalendar(String importCalendar) {
        this.importCalendar = importCalendar;
    }
    public void setImportTasks(String importTasks) {
        this.importTasks = importTasks;
    }
    public void setImportJunk(String importJunk) {
        this.importJunk = importJunk;
    }
    public void setImportDeletedItems(String importDeletedItems) {
        this.importDeletedItems = importDeletedItems;
    }
    public void setIgnorePreviouslyImported(String ignorePreviouslyImported) {
        this.ignorePreviouslyImported = ignorePreviouslyImported;
    }
    public void setInvalidSSLOk(String invalidSSLOk) {
        this.invalidSSLOk = invalidSSLOk;
    }
    public void setMapiProfile(String mapiProfile) {
        this.mapiProfile = mapiProfile;
    }
    public void setMapiServer(String mapiServer) {
        this.mapiServer = mapiServer;
    }
    public void setMapiLogonUserDN(String mapiLogonUserDN) {
        this.mapiLogonUserDN = mapiLogonUserDN;
    }
    public void setZimbraAdminLogin(String zimbraAdminLogin) {
        this.zimbraAdminLogin = zimbraAdminLogin;
    }
    public void setZimbraAdminPassword(String zimbraAdminPassword) {
        this.zimbraAdminPassword = zimbraAdminPassword;
    }
    public void setTargetDomainName(String targetDomainName) {
        this.targetDomainName = targetDomainName;
    }
    public void setProvisionUsers(String provisionUsers) {
        this.provisionUsers = provisionUsers;
    }

    public String getPassword() { return password; }
    public String getGeneratePassword() { return generatePassword; }
    public Integer getGenPasswordLength() { return genPasswordLength; }
    public String getFileFormat() { return fileFormat; }
    public String getMustChangePassword() { return mustChangePassword; }
    public Integer getMaxResults() { return maxResults; }
    public String getSMTPHost() { return SMTPHost; }
    public String getSMTPPort() { return SMTPPort; }
    public String getImportMails() { return importMails; }
    public String getImportContacts() { return importContacts; }
    public String getImportCalendar() { return importCalendar; }
    public String getImportTasks() { return importTasks; }
    public String getImportJunk() { return importJunk; }
    public String getImportDeletedItems() { return importDeletedItems; }
    public String getIgnorePreviouslyImported() { return ignorePreviouslyImported; }
    public String getInvalidSSLOk() { return invalidSSLOk; }
    public String getMapiProfile() { return mapiProfile; }
    public String getMapiServer() { return mapiServer; }
    public String getMapiLogonUserDN() { return mapiLogonUserDN; }
    public String getZimbraAdminLogin() { return zimbraAdminLogin; }
    public String getZimbraAdminPassword() { return zimbraAdminPassword; }
    public String getTargetDomainName() { return targetDomainName; }
    public String getProvisionUsers() { return provisionUsers; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("password", password)
            .add("generatePassword", generatePassword)
            .add("genPasswordLength", genPasswordLength)
            .add("fileFormat", fileFormat)
            .add("mustChangePassword", mustChangePassword)
            .add("maxResults", maxResults)
            .add("SMTPHost", SMTPHost)
            .add("SMTPPort", SMTPPort)
            .add("importMails", importMails)
            .add("importContacts", importContacts)
            .add("importCalendar", importCalendar)
            .add("importTasks", importTasks)
            .add("importJunk", importJunk)
            .add("importDeletedItems", importDeletedItems)
            .add("ignorePreviouslyImported", ignorePreviouslyImported)
            .add("invalidSSLOk", invalidSSLOk)
            .add("mapiProfile", mapiProfile)
            .add("mapiServer", mapiServer)
            .add("mapiLogonUserDN", mapiLogonUserDN)
            .add("zimbraAdminLogin", zimbraAdminLogin)
            .add("zimbraAdminPassword", zimbraAdminPassword)
            .add("targetDomainName", targetDomainName)
            .add("provisionUsers", provisionUsers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
