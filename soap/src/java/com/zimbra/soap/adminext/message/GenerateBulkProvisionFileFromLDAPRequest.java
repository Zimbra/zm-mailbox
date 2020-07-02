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

package com.zimbra.soap.adminext.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.AttrsImpl;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Generate bulk provision file from LDAP
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminExtConstants.E_GENERATE_BULK_PROV_FROM_LDAP_REQUEST)
public class GenerateBulkProvisionFileFromLDAPRequest extends AttrsImpl {

    /**
     * @zm-api-field-tag password
     * @zm-api-field-description Password
     */
    @XmlElement(name=AdminExtConstants.A_password /* password */, required=false)
    private String password;

    /**
     * @zm-api-field-tag generate-password-flag
     * @zm-api-field-description Generate password flag.  True if "true" (case insensitive) else false - default
     * value "false"
     */
    @XmlElement(name=AdminExtConstants.A_generatePassword /* generatePassword */, required=false)
    private String generatePassword;

    /**
     * @zm-api-field-tag gen-password-length
     * @zm-api-field-description Length for generated passwords (Default 8)
     */
    @XmlElement(name=AdminExtConstants.A_genPasswordLength /* genPasswordLength */, required=false)
    private Integer genPasswordLength;

    /**
     * @zm-api-field-tag file-format-preview|csv|bulkxml|migrationxml
     * @zm-api-field-description File format - <b>preview|csv|bulkxml|migrationxml</b>
     */
    @XmlElement(name=AdminExtConstants.A_fileFormat /* fileFormat */, required=false)
    private String fileFormat;

    /**
     * @zm-api-field-tag must-change-password-flag
     * @zm-api-field-description Flag whether must change password
     */
    @XmlElement(name=AdminExtConstants.E_mustChangePassword /* mustChangePassword */, required=true)
    private String mustChangePassword;

    /**
     * @zm-api-field-tag max-results
     * @zm-api-field-description Maximm number of results
     */
    @XmlElement(name=AdminExtConstants.A_maxResults /* maxResults */, required=false)
    private Integer maxResults;

    /**
     * @zm-api-field-tag SMTP-host
     * @zm-api-field-description SMTP host
     */
    @XmlElement(name=AdminExtConstants.E_SMTPHost /* SMTPHost */, required=false)
    private String SMTPHost;

    /**
     * @zm-api-field-tag SMTP-port
     * @zm-api-field-description SMTP port
     */
    @XmlElement(name=AdminExtConstants.E_SMTPPort /* SMTPPort */, required=false)
    private String SMTPPort;

    /**
     * @zm-api-field-tag import-mails-flag
     * @zm-api-field-description Flag whether to import mails.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_importMails /* importMails */, required=false)
    private String importMails;

    /**
     * @zm-api-field-tag import-contacts
     * @zm-api-field-description Flag whether to import contacts.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_importContacts /* importContacts */, required=false)
    private String importContacts;

    /**
     * @zm-api-field-tag import-calendar
     * @zm-api-field-description Flag whether to import calendars.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_importCalendar /* importCalendar */, required=false)
    private String importCalendar;

    /**
     * @zm-api-field-tag import-tasks
     * @zm-api-field-description Flag whether to import tasks.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_importTasks /* importTasks */, required=false)
    private String importTasks;

    /**
     * @zm-api-field-tag import-junk
     * @zm-api-field-description Flag whether to import junk.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_importJunk /* importJunk */, required=false)
    private String importJunk;

    /**
     * @zm-api-field-tag import-deleted-items
     * @zm-api-field-description Flag whether to import deleted items.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_importDeletedItems /* importDeletedItems */, required=false)
    private String importDeletedItems;

    /**
     * @zm-api-field-tag ignore-previously-imported
     * @zm-api-field-description Flag whether to import previosly imported items.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_ignorePreviouslyImported /* ignorePreviouslyImported */, required=false)
    private String ignorePreviouslyImported;

    /**
     * @zm-api-field-tag invalid-ssl-ok
     * @zm-api-field-description Invalid SSL Ok flag.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_InvalidSSLOk /* InvalidSSLOk */, required=false)
    private String invalidSSLOk;

    /**
     * @zm-api-field-tag MAPI-profile
     * @zm-api-field-description MAPI profile
     */
    @XmlElement(name=AdminExtConstants.E_MapiProfile /* MapiProfile */, required=false)
    private String mapiProfile;

    /**
     * @zm-api-field-tag MAPI-server
     * @zm-api-field-description MAPI Server
     */
    @XmlElement(name=AdminExtConstants.E_MapiServer /* MapiServer */, required=false)
    private String mapiServer;

    /**
     * @zm-api-field-tag MAPI-logon-user-DN
     * @zm-api-field-description MAPI logon User DN
     */
    @XmlElement(name=AdminExtConstants.E_MapiLogonUserDN /* MapiLogonUserDN */, required=false)
    private String mapiLogonUserDN;

    /**
     * @zm-api-field-tag zimbra-admin-login
     * @zm-api-field-description Zimbra Admin login
     */
    @XmlElement(name=AdminExtConstants.E_ZimbraAdminLogin /* ZimbraAdminLogin */, required=false)
    private String zimbraAdminLogin;

    /**
     * @zm-api-field-tag zimbra-admin-password
     * @zm-api-field-description Zimbra Admin password
     */
    @XmlElement(name=AdminExtConstants.E_ZimbraAdminPassword /* ZimbraAdminPassword */, required=false)
    private String zimbraAdminPassword;

    /**
     * @zm-api-field-tag target-domain-name
     * @zm-api-field-description Target domain name
     */
    @XmlElement(name=AdminExtConstants.E_TargetDomainName /* TargetDomainName */, required=false)
    private String targetDomainName;

    /**
     * @zm-api-field-tag provision-users-flag
     * @zm-api-field-description Flag whether to provision users.  True if "TRUE" (case insensitive)
     */
    @XmlElement(name=AdminExtConstants.E_provisionUsers /* provisionUsers */, required=false)
    private String provisionUsers;

    public GenerateBulkProvisionFileFromLDAPRequest() {
    }

    public void setPassword(String password) { this.password = password; }
    public void setGeneratePassword(String generatePassword) { this.generatePassword = generatePassword; }
    public void setGenPasswordLength(Integer genPasswordLength) { this.genPasswordLength = genPasswordLength; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public void setMustChangePassword(String mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    public void setSMTPHost(String SMTPHost) { this.SMTPHost = SMTPHost; }
    public void setSMTPPort(String SMTPPort) { this.SMTPPort = SMTPPort; }
    public void setImportMails(String importMails) { this.importMails = importMails; }
    public void setImportContacts(String importContacts) { this.importContacts = importContacts; }
    public void setImportCalendar(String importCalendar) { this.importCalendar = importCalendar; }
    public void setImportTasks(String importTasks) { this.importTasks = importTasks; }
    public void setImportJunk(String importJunk) { this.importJunk = importJunk; }
    public void setImportDeletedItems(String importDeletedItems) { this.importDeletedItems = importDeletedItems; }
    public void setIgnorePreviouslyImported(String ignorePreviouslyImported) {
        this.ignorePreviouslyImported = ignorePreviouslyImported;
    }
    public void setInvalidSSLOk(String invalidSSLOk) { this.invalidSSLOk = invalidSSLOk; }
    public void setMapiProfile(String mapiProfile) { this.mapiProfile = mapiProfile; }
    public void setMapiServer(String mapiServer) { this.mapiServer = mapiServer; }
    public void setMapiLogonUserDN(String mapiLogonUserDN) { this.mapiLogonUserDN = mapiLogonUserDN; }
    public void setZimbraAdminLogin(String zimbraAdminLogin) { this.zimbraAdminLogin = zimbraAdminLogin; }
    public void setZimbraAdminPassword(String zimbraAdminPassword) { this.zimbraAdminPassword = zimbraAdminPassword; }
    public void setTargetDomainName(String targetDomainName) { this.targetDomainName = targetDomainName; }
    public void setProvisionUsers(String provisionUsers) { this.provisionUsers = provisionUsers; }
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
