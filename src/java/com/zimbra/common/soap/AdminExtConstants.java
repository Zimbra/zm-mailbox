/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class AdminExtConstants {
    public static final String NAMESPACE_STR = "urn:zimbraAdminExt";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_BULK_IMPORT_ACCOUNTS_REQUEST = "BulkImportAccountsRequest";
    public static final String E_BULK_IMPORT_ACCOUNTS_RESPONSE = "BulkImportAccountsResponse";
    public static final String E_GENERATE_BULK_PROV_FROM_LDAP_REQUEST = "GenerateBulkProvisionFileFromLDAPRequest";
    public static final String E_GENERATE_BULK_PROV_FROM_LDAP_RESPONSE = "GenerateBulkProvisionFileFromLDAPResponse";
    public static final String E_BULK_IMAP_DATA_IMPORT_REQUEST = "BulkIMAPDataImportRequest";
    public static final String E_BULK_IMAP_DATA_IMPORT_RESPONSE = "BulkIMAPDataImportResponse";
    public static final String E_GET_BULK_IMAP_IMPORT_TASK_REQUEST = "GetBulkIMAPImportTaskRequest";
    public static final String E_GET_BULK_IMAP_IMPORT_TASK_RESPONSE = "GetBulkIMAPImportTaskResponse";
    public static final String E_GET_BULK_IMAP_IMPORT_TASKLIST_REQUEST = "GetBulkIMAPImportTaskListRequest";
    public static final String E_GET_BULK_IMAP_IMPORT_TASKLIST_RESPONSE = "GetBulkIMAPImportTaskListResponse";
    public static final String E_PURGE_BULK_IMAP_IMPORT_TASKS_REQUEST = "PurgeBulkIMAPImportTasksRequest";
    public static final String E_PURGE_BULK_IMAP_IMPORT_TASKS_RESPONSE = "PurgeBulkIMAPImportTasksResponse";

    public static final QName BULK_IMPORT_ACCOUNTS_REQUEST = QName.get(E_BULK_IMPORT_ACCOUNTS_REQUEST, NAMESPACE);
    public static final QName BULK_IMPORT_ACCOUNTS_RESPONSE = QName.get(E_BULK_IMPORT_ACCOUNTS_RESPONSE, NAMESPACE);

    public static final QName GENERATE_BULK_PROV_FROM_LDAP_REQUEST = QName.get(E_GENERATE_BULK_PROV_FROM_LDAP_REQUEST, NAMESPACE);
    public static final QName GENERATE_BULK_PROV_FROM_LDAP_RESPONSE = QName.get(E_GENERATE_BULK_PROV_FROM_LDAP_RESPONSE, NAMESPACE);

    public static final QName BULK_IMAP_DATA_IMPORT_REQUEST = QName.get(E_BULK_IMAP_DATA_IMPORT_REQUEST, NAMESPACE);
    public static final QName BULK_IMAP_DATA_IMPORT_RESPONSE = QName.get(E_BULK_IMAP_DATA_IMPORT_RESPONSE, NAMESPACE);

    public static final QName GET_BULK_IMAP_IMPORT_TASK_REQUEST = QName.get(E_GET_BULK_IMAP_IMPORT_TASK_REQUEST, NAMESPACE);
    public static final QName GET_BULK_IMAP_IMPORT_TASK_RESPONSE = QName.get(E_GET_BULK_IMAP_IMPORT_TASK_RESPONSE, NAMESPACE);

    public static final QName GET_BULK_IMAP_IMPORT_TASKLIST_REQUEST = QName.get(E_GET_BULK_IMAP_IMPORT_TASKLIST_REQUEST, NAMESPACE);
    public static final QName GET_BULK_IMAP_IMPORT_TASKLIST_RESPONSE = QName.get(E_GET_BULK_IMAP_IMPORT_TASKLIST_RESPONSE, NAMESPACE);

    public static final QName PURGE_BULK_IMAP_IMPORT_TASKS_REQUEST = QName.get(E_PURGE_BULK_IMAP_IMPORT_TASKS_REQUEST, NAMESPACE);
    public static final QName PURGE_BULK_IMAP_IMPORT_TASKS_RESPONSE = QName.get(E_PURGE_BULK_IMAP_IMPORT_TASKS_RESPONSE, NAMESPACE);

    // General Bulk provisioning constants
    public static final String A_password = "password";
    public static final String A_generatePassword = "generatePassword";
    public static final String A_genPasswordLength = "genPasswordLength";
    public static final String A_fileFormat = "fileFormat";
    public static final String A_maxResults = "maxResults";
    public static final String A_setMustChangePwd = "setMustChangePwd";
    public static final String A_op = "op";
    public static final String A_sourceType = "sourceType";
    public static final String A_owner = "owner";
    public static final String A_totalTasks = "totalTasks";
    public static final String A_finishedTasks = "finishedTasks";
    public static final String A_failedTasks = "failedTasks";

    public static final String E_Task = "task";
    public static final String E_User = "User";
    public static final String E_ExchangeMail = "ExchangeMail";
    public static final String E_remoteEmail = "RemoteEmailAddress";
    public static final String E_remoteIMAPLogin = "RemoteIMAPLogin";
    public static final String E_localEmail = "LocalEmailAddress";
    public static final String E_remoteIMAPPassword = "remoteIMAPPassword";
    public static final String E_ZCSImport = "ZCSImport";
    public static final String E_ImportUsers = "ImportUsers";
    public static final String E_useAdminLogin = "UseAdminLogin";
    public static final String E_IMAPAdminLogin = "IMAPAdminLogin";
    public static final String E_IMAPAdminPassword = "IMAPAdminPassword";
    public static final String E_connectionType = "ConnectionType";
    public static final String E_SMTPHost = "SMTPHost";
    public static final String E_SMTPPort = "SMTPPort";
    public static final String E_IMAPHost = "IMAPHost";
    public static final String E_IMAPPort = "IMAPPort";
    public static final String E_attachmentID = "aid";
    public static final String E_idleCount = "idleCount";
    public static final String E_runningCount = "runningCount";
    public static final String E_finishedCount = "finishedCount";
    public static final String E_runningAccounts = "runningAccounts";
    public static final String E_serverName = "serverName";
    public static final String E_port = "port";
    public static final String E_adminUserName = "adminUserName";
    public static final String E_indexBatchSize = "indexBatchSize";
    public static final String E_skippedAccountCount = "skippedAccountCount";
    public static final String E_sourceServerType = "sourceServerType";
    public static final String E_mustChangePassword = "mustChangePassword";

    // used by BulkImportAccounts
    public static final String E_status = "status";
    public static final String E_provisionedCount = "provisionedCount";
    public static final String E_skippedCount = "skippedCount";
    public static final String E_totalCount = "totalCount";
    public static final String E_reportFileToken = "fileToken";
    public static final String E_errorCount = "errorCount";
    public static final String E_createDomains = "createDomains";

    // used by GenerateBulkProvisionFileFromLDAP
    public static final String E_fileToken = "fileToken";
    public static final String E_MapiProfile = "MapiProfile";
    public static final String E_TargetDomainName = "TargetDomainName";
    public static final String E_ZimbraAdminLogin = "ZimbraAdminLogin";
    public static final String E_ZimbraAdminPassword = "ZimbraAdminPassword";
    public static final String E_provisionUsers = "provisionUsers";
    public static final String E_MapiServer = "MapiServer";
    public static final String E_MapiLogonUserDN = "MapiLogonUserDN";
    public static final String E_importMails = "importMails";
    public static final String E_importContacts = "importContacts";
    public static final String E_importTasks = "importTasks";
    public static final String E_importCalendar = "importCalendar";
    public static final String E_importDeletedItems = "importDeletedItems";
    public static final String E_importJunk = "importJunk";
    public static final String E_ignorePreviouslyImported = "ignorePreviouslyImported";
    public static final String E_InvalidSSLOk = "InvalidSSLOk";
    public static final String E_domainCount = "domainCount";
    public static final String E_skippedDomainCount = "skippedDomainCount";
}
