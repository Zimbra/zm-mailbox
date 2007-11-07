/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;


public class AdminConstants {
    public static final String NAMESPACE_STR = "urn:zimbraAdmin";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final QName PING_REQUEST = QName.get("PingRequest", NAMESPACE);
    public static final QName PING_RESPONSE = QName.get("PingResponse", NAMESPACE);
    public static final QName CHECK_HEALTH_REQUEST = QName.get("CheckHealthRequest", NAMESPACE);
    public static final QName CHECK_HEALTH_RESPONSE = QName.get("CheckHealthResponse", NAMESPACE);

    public static final QName EXPORTMAILBOX_REQUEST = QName.get("ExportMailboxRequest", NAMESPACE);
    public static final QName EXPORTMAILBOX_RESPONSE = QName.get("ExportMailboxResponse", NAMESPACE);

    public static final QName AUTH_REQUEST = QName.get("AuthRequest", NAMESPACE);
    public static final QName AUTH_RESPONSE = QName.get("AuthResponse", NAMESPACE);
    public static final QName CREATE_ACCOUNT_REQUEST = QName.get("CreateAccountRequest", NAMESPACE);
    public static final QName CREATE_ACCOUNT_RESPONSE = QName.get("CreateAccountResponse", NAMESPACE);
    public static final QName CREATE_ADMIN_ACCOUNT_REQUEST = QName.get("CreateAdminAccountRequest", NAMESPACE);
    public static final QName CREATE_ADMIN_ACCOUNT_RESPONSE = QName.get("CreateAdminAccountResponse", NAMESPACE);
    public static final QName DELEGATE_AUTH_REQUEST = QName.get("DelegateAuthRequest", NAMESPACE);
    public static final QName DELEGATE_AUTH_RESPONSE = QName.get("DelegateAuthResponse", NAMESPACE);
    public static final QName GET_ACCOUNT_REQUEST = QName.get("GetAccountRequest", NAMESPACE);
    public static final QName GET_ACCOUNT_RESPONSE = QName.get("GetAccountResponse", NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_REQUEST = QName.get("GetAccountInfoRequest", NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_RESPONSE = QName.get("GetAccountInfoResponse", NAMESPACE);
    public static final QName GET_ALL_ACCOUNTS_REQUEST = QName.get("GetAllAccountsRequest", NAMESPACE);
    public static final QName GET_ALL_ACCOUNTS_RESPONSE = QName.get("GetAllAccountsResponse", NAMESPACE);
    public static final QName GET_ALL_ADMIN_ACCOUNTS_REQUEST = QName.get("GetAllAdminAccountsRequest", NAMESPACE);
    public static final QName GET_ALL_ADMIN_ACCOUNTS_RESPONSE = QName.get("GetAllAdminAccountsResponse", NAMESPACE);
    public static final QName MODIFY_ACCOUNT_REQUEST = QName.get("ModifyAccountRequest", NAMESPACE);
    public static final QName MODIFY_ACCOUNT_RESPONSE = QName.get("ModifyAccountResponse", NAMESPACE);
    public static final QName DELETE_ACCOUNT_REQUEST = QName.get("DeleteAccountRequest", NAMESPACE);
    public static final QName DELETE_ACCOUNT_RESPONSE = QName.get("DeleteAccountResponse", NAMESPACE);
    public static final QName SET_PASSWORD_REQUEST = QName.get("SetPasswordRequest", NAMESPACE);
    public static final QName SET_PASSWORD_RESPONSE = QName.get("SetPasswordResponse", NAMESPACE);
    public static final QName CHECK_PASSWORD_STRENGTH_REQUEST = QName.get("CheckPasswordStrengthRequest", NAMESPACE);
    public static final QName CHECK_PASSWORD_STRENGTH_RESPONSE = QName.get("CheckPasswordStrengthResponse", NAMESPACE);
    public static final QName ADD_ACCOUNT_ALIAS_REQUEST = QName.get("AddAccountAliasRequest", NAMESPACE);
    public static final QName ADD_ACCOUNT_ALIAS_RESPONSE = QName.get("AddAccountAliasResponse", NAMESPACE);
    public static final QName REMOVE_ACCOUNT_ALIAS_REQUEST = QName.get("RemoveAccountAliasRequest", NAMESPACE);
    public static final QName REMOVE_ACCOUNT_ALIAS_RESPONSE = QName.get("RemoveAccountAliasResponse", NAMESPACE);
    public static final QName SEARCH_ACCOUNTS_REQUEST = QName.get("SearchAccountsRequest", NAMESPACE);
    public static final QName SEARCH_ACCOUNTS_RESPONSE = QName.get("SearchAccountsResponse", NAMESPACE);
    public static final QName RENAME_ACCOUNT_REQUEST = QName.get("RenameAccountRequest", NAMESPACE);
    public static final QName RENAME_ACCOUNT_RESPONSE = QName.get("RenameAccountResponse", NAMESPACE);

    public static final QName CREATE_DOMAIN_REQUEST = QName.get("CreateDomainRequest", NAMESPACE);
    public static final QName CREATE_DOMAIN_RESPONSE = QName.get("CreateDomainResponse", NAMESPACE);
    public static final QName GET_DOMAIN_REQUEST = QName.get("GetDomainRequest", NAMESPACE);
    public static final QName GET_DOMAIN_RESPONSE = QName.get("GetDomainResponse", NAMESPACE);
    public static final QName GET_DOMAIN_INFO_REQUEST = QName.get("GetDomainInfoRequest", NAMESPACE);
    public static final QName GET_DOMAIN_INFO_RESPONSE = QName.get("GetDomainInfoResponse", NAMESPACE);
    public static final QName MODIFY_DOMAIN_REQUEST = QName.get("ModifyDomainRequest", NAMESPACE);
    public static final QName MODIFY_DOMAIN_RESPONSE = QName.get("ModifyDomainResponse", NAMESPACE);
    public static final QName DELETE_DOMAIN_REQUEST = QName.get("DeleteDomainRequest", NAMESPACE);
    public static final QName DELETE_DOMAIN_RESPONSE = QName.get("DeleteDomainResponse", NAMESPACE);
    public static final QName GET_ALL_DOMAINS_REQUEST = QName.get("GetAllDomainsRequest", NAMESPACE);
    public static final QName GET_ALL_DOMAINS_RESPONSE = QName.get("GetAllDomainsResponse", NAMESPACE);

    public static final QName CREATE_COS_REQUEST = QName.get("CreateCosRequest", NAMESPACE);
    public static final QName CREATE_COS_RESPONSE = QName.get("CreateCosResponse", NAMESPACE);
    public static final QName GET_COS_REQUEST = QName.get("GetCosRequest", NAMESPACE);
    public static final QName GET_COS_RESPONSE = QName.get("GetCosResponse", NAMESPACE);
    public static final QName MODIFY_COS_REQUEST = QName.get("ModifyCosRequest", NAMESPACE);
    public static final QName MODIFY_COS_RESPONSE = QName.get("ModifyCosResponse", NAMESPACE);
    public static final QName DELETE_COS_REQUEST = QName.get("DeleteCosRequest", NAMESPACE);
    public static final QName DELETE_COS_RESPONSE = QName.get("DeleteCosResponse", NAMESPACE);
    public static final QName GET_ALL_COS_REQUEST = QName.get("GetAllCosRequest", NAMESPACE);
    public static final QName GET_ALL_COS_RESPONSE = QName.get("GetAllCosResponse", NAMESPACE);
    public static final QName RENAME_COS_REQUEST = QName.get("RenameCosRequest", NAMESPACE);
    public static final QName RENAME_COS_RESPONSE = QName.get("RenameCosResponse", NAMESPACE);

    public static final QName CREATE_SERVER_REQUEST = QName.get("CreateServerRequest", NAMESPACE);
    public static final QName CREATE_SERVER_RESPONSE = QName.get("CreateServerResponse", NAMESPACE);
    public static final QName GET_SERVER_REQUEST = QName.get("GetServerRequest", NAMESPACE);
    public static final QName GET_SERVER_RESPONSE = QName.get("GetServerResponse", NAMESPACE);
    
    public static final QName GET_SERVER_NIFS_REQUEST = QName.get("GetServerNIfsRequest", NAMESPACE);
    public static final QName GET_SERVER_NIFS_RESPONSE = QName.get("GetServerNIfsResponse", NAMESPACE);    
    
    public static final QName MODIFY_SERVER_REQUEST = QName.get("ModifyServerRequest", NAMESPACE);
    public static final QName MODIFY_SERVER_RESPONSE = QName.get("ModifyServerResponse", NAMESPACE);
    public static final QName DELETE_SERVER_REQUEST = QName.get("DeleteServerRequest", NAMESPACE);
    public static final QName DELETE_SERVER_RESPONSE = QName.get("DeleteServerResponse", NAMESPACE);
    public static final QName GET_ALL_SERVERS_REQUEST = QName.get("GetAllServersRequest", NAMESPACE);
    public static final QName GET_ALL_SERVERS_RESPONSE = QName.get("GetAllServersResponse", NAMESPACE);

    public static final QName GET_CONFIG_REQUEST = QName.get("GetConfigRequest", NAMESPACE);
    public static final QName GET_CONFIG_RESPONSE = QName.get("GetConfigResponse", NAMESPACE);
    public static final QName MODIFY_CONFIG_REQUEST = QName.get("ModifyConfigRequest", NAMESPACE);
    public static final QName MODIFY_CONFIG_RESPONSE = QName.get("ModifyConfigResponse", NAMESPACE);
    public static final QName GET_ALL_CONFIG_REQUEST = QName.get("GetAllConfigRequest", NAMESPACE);
    public static final QName GET_ALL_CONFIG_RESPONSE = QName.get("GetAllConfigResponse", NAMESPACE);

    public static final QName GET_SERVICE_STATUS_REQUEST = QName.get("GetServiceStatusRequest", NAMESPACE);
    public static final QName GET_SERVICE_STATUS_RESPONSE = QName.get("GetServiceStatusResponse", NAMESPACE);

    public static final QName PURGE_MESSAGES_REQUEST = QName.get("PurgeMessagesRequest", NAMESPACE);
    public static final QName PURGE_MESSAGES_RESPONSE= QName.get("PurgeMessagesResponse", NAMESPACE);
    public static final QName DELETE_MAILBOX_REQUEST = QName.get("DeleteMailboxRequest", NAMESPACE);
    public static final QName DELETE_MAILBOX_RESPONSE= QName.get("DeleteMailboxResponse", NAMESPACE);
    public static final QName GET_MAILBOX_REQUEST = QName.get("GetMailboxRequest", NAMESPACE);
    public static final QName GET_MAILBOX_RESPONSE= QName.get("GetMailboxResponse", NAMESPACE);

    public static final QName MAINTAIN_TABLES_REQUEST = QName.get("MaintainTablesRequest", NAMESPACE);
    public static final QName MAINTAIN_TABLES_RESPONSE = QName.get("MaintainTablesResponse", NAMESPACE);

    public static final QName RUN_UNIT_TESTS_REQUEST = QName.get("RunUnitTestsRequest", NAMESPACE);
    public static final QName RUN_UNIT_TESTS_RESPONSE = QName.get("RunUnitTestsResponse", NAMESPACE);

    public static final QName CHECK_HOSTNAME_RESOLVE_REQUEST = QName.get("CheckHostnameResolveRequest", NAMESPACE);
    public static final QName CHECK_HOSTNAME_RESOLVE_RESPONSE = QName.get("CheckHostnameResolveResponse", NAMESPACE);
    public static final QName CHECK_AUTH_CONFIG_REQUEST = QName.get("CheckAuthConfigRequest", NAMESPACE);
    public static final QName CHECK_AUTH_CONFIG_RESPONSE = QName.get("CheckAuthConfigResponse", NAMESPACE);
    public static final QName CHECK_GAL_CONFIG_REQUEST = QName.get("CheckGalConfigRequest", NAMESPACE);
    public static final QName CHECK_GAL_CONFIG_RESPONSE = QName.get("CheckGalConfigResponse", NAMESPACE);

    public static final QName AUTO_COMPLETE_GAL_REQUEST = QName.get("AutoCompleteGalRequest", NAMESPACE);
    public static final QName AUTO_COMPLETE_GAL_RESPONSE = QName.get("AutoCompleteGalResponse", NAMESPACE);
    public static final QName SEARCH_GAL_REQUEST = QName.get("SearchGalRequest", NAMESPACE);
    public static final QName SEARCH_GAL_RESPONSE = QName.get("SearchGalResponse", NAMESPACE);

    public static final QName CREATE_VOLUME_REQUEST = QName.get("CreateVolumeRequest", NAMESPACE);
    public static final QName CREATE_VOLUME_RESPONSE = QName.get("CreateVolumeResponse", NAMESPACE);
    public static final QName GET_VOLUME_REQUEST = QName.get("GetVolumeRequest", NAMESPACE);
    public static final QName GET_VOLUME_RESPONSE = QName.get("GetVolumeResponse", NAMESPACE);
    public static final QName MODIFY_VOLUME_REQUEST = QName.get("ModifyVolumeRequest", NAMESPACE);
    public static final QName MODIFY_VOLUME_RESPONSE = QName.get("ModifyVolumeResponse", NAMESPACE);
    public static final QName DELETE_VOLUME_REQUEST = QName.get("DeleteVolumeRequest", NAMESPACE);
    public static final QName DELETE_VOLUME_RESPONSE = QName.get("DeleteVolumeResponse", NAMESPACE);
    public static final QName GET_ALL_VOLUMES_REQUEST = QName.get("GetAllVolumesRequest", NAMESPACE);
    public static final QName GET_ALL_VOLUMES_RESPONSE = QName.get("GetAllVolumesResponse", NAMESPACE);
    public static final QName GET_CURRENT_VOLUMES_REQUEST = QName.get("GetCurrentVolumesRequest", NAMESPACE);
    public static final QName GET_CURRENT_VOLUMES_RESPONSE = QName.get("GetCurrentVolumesResponse", NAMESPACE);
    public static final QName SET_CURRENT_VOLUME_REQUEST = QName.get("SetCurrentVolumeRequest", NAMESPACE);
    public static final QName SET_CURRENT_VOLUME_RESPONSE = QName.get("SetCurrentVolumeResponse", NAMESPACE);

    public static final QName CREATE_DISTRIBUTION_LIST_REQUEST = QName.get("CreateDistributionListRequest", NAMESPACE);
    public static final QName CREATE_DISTRIBUTION_LIST_RESPONSE = QName.get("CreateDistributionListResponse", NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_REQUEST = QName.get("GetDistributionListRequest", NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_RESPONSE = QName.get("GetDistributionListResponse", NAMESPACE);
    public static final QName GET_ALL_DISTRIBUTION_LISTS_REQUEST = QName.get("GetAllDistributionListsRequest", NAMESPACE);
    public static final QName GET_ALL_DISTRIBUTION_LISTS_RESPONSE = QName.get("GetAllDistributionListsResponse", NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_MEMBER_REQUEST = QName.get("AddDistributionListMemberRequest", NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_MEMBER_RESPONSE = QName.get("AddDistributionListMemberResponse", NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST = QName.get("RemoveDistributionListMemberRequest", NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_MEMBER_RESPONSE = QName.get("RemoveDistributionListMemberResponse", NAMESPACE);
    public static final QName MODIFY_DISTRIBUTION_LIST_REQUEST = QName.get("ModifyDistributionListRequest", NAMESPACE);
    public static final QName MODIFY_DISTRIBUTION_LIST_RESPONSE = QName.get("ModifyDistributionListResponse", NAMESPACE);
    public static final QName DELETE_DISTRIBUTION_LIST_REQUEST = QName.get("DeleteDistributionListRequest", NAMESPACE);
    public static final QName DELETE_DISTRIBUTION_LIST_RESPONSE = QName.get("DeleteDistributionListResponse", NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_ALIAS_REQUEST = QName.get("AddDistributionListAliasRequest", NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_ALIAS_RESPONSE = QName.get("AddDistributionListAliasResponse", NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST = QName.get("RemoveDistributionListAliasRequest", NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_ALIAS_RESPONSE = QName.get("RemoveDistributionListAliasResponse", NAMESPACE);
    public static final QName RENAME_DISTRIBUTION_LIST_REQUEST = QName.get("RenameDistributionListRequest", NAMESPACE);
    public static final QName RENAME_DISTRIBUTION_LIST_RESPONSE = QName.get("RenameDistributionListResponse", NAMESPACE);

    public static final QName GET_VERSION_INFO_REQUEST = QName.get("GetVersionInfoRequest", NAMESPACE);
    public static final QName GET_VERSION_INFO_RESPONSE = QName.get("GetVersionInfoResponse", NAMESPACE);

    public static final QName GET_LICENSE_INFO_REQUEST = QName.get("GetLicenseInfoRequest", NAMESPACE);
    public static final QName GET_LICENSE_INFO_RESPONSE = QName.get("GetLicenseInfoResponse", NAMESPACE);

    public static final QName REINDEX_REQUEST = QName.get("ReIndexRequest", NAMESPACE);
    public static final QName REINDEX_RESPONSE = QName.get("ReIndexResponse", NAMESPACE);

    public static final QName GET_ZIMLET_REQUEST = QName.get("GetZimletRequest", NAMESPACE);
    public static final QName GET_ZIMLET_RESPONSE = QName.get("GetZimletResponse", NAMESPACE);
    public static final QName CREATE_ZIMLET_REQUEST = QName.get("CreateZimletRequest", NAMESPACE);
    public static final QName CREATE_ZIMLET_RESPONSE = QName.get("CreateZimletResponse", NAMESPACE);
    public static final QName DELETE_ZIMLET_REQUEST = QName.get("DeleteZimletRequest", NAMESPACE);
    public static final QName DELETE_ZIMLET_RESPONSE = QName.get("DeleteZimletResponse", NAMESPACE);
    public static final QName GET_ADMIN_EXTENSION_ZIMLETS_REQUEST = QName.get("GetAdminExtensionZimletsRequest", NAMESPACE);
    public static final QName GET_ADMIN_EXTENSION_ZIMLETS_RESPONSE = QName.get("GetAdminExtensionZimletsResponse", NAMESPACE);
    public static final QName GET_ALL_ZIMLETS_REQUEST = QName.get("GetAllZimletsRequest", NAMESPACE);
    public static final QName GET_ALL_ZIMLETS_RESPONSE = QName.get("GetAllZimletsResponse", NAMESPACE);
    public static final QName GET_ZIMLET_STATUS_REQUEST = QName.get("GetZimletStatusRequest", NAMESPACE);
    public static final QName GET_ZIMLET_STATUS_RESPONSE = QName.get("GetZimletStatusResponse", NAMESPACE);
    public static final QName DEPLOY_ZIMLET_REQUEST = QName.get("DeployZimletRequest", NAMESPACE);
    public static final QName DEPLOY_ZIMLET_RESPONSE = QName.get("DeployZimletResponse", NAMESPACE);
    public static final QName UNDEPLOY_ZIMLET_REQUEST = QName.get("UndeployZimletRequest", NAMESPACE);
    public static final QName UNDEPLOY_ZIMLET_RESPONSE = QName.get("UndeployZimletResponse", NAMESPACE);
    public static final QName CONFIGURE_ZIMLET_REQUEST = QName.get("ConfigureZimletRequest", NAMESPACE);
    public static final QName CONFIGURE_ZIMLET_RESPONSE = QName.get("ConfigureZimletResponse", NAMESPACE);
    public static final QName MODIFY_ZIMLET_REQUEST = QName.get("ModifyZimletRequest", NAMESPACE);
    public static final QName MODIFY_ZIMLET_RESPONSE = QName.get("ModifyZimletResponse", NAMESPACE);

    public static final QName CREATE_CALENDAR_RESOURCE_REQUEST    = QName.get("CreateCalendarResourceRequest",   NAMESPACE);
    public static final QName CREATE_CALENDAR_RESOURCE_RESPONSE   = QName.get("CreateCalendarResourceResponse",  NAMESPACE);
    public static final QName DELETE_CALENDAR_RESOURCE_REQUEST    = QName.get("DeleteCalendarResourceRequest",   NAMESPACE);
    public static final QName DELETE_CALENDAR_RESOURCE_RESPONSE   = QName.get("DeleteCalendarResourceResponse",  NAMESPACE);
    public static final QName MODIFY_CALENDAR_RESOURCE_REQUEST    = QName.get("ModifyCalendarResourceRequest",   NAMESPACE);
    public static final QName MODIFY_CALENDAR_RESOURCE_RESPONSE   = QName.get("ModifyCalendarResourceResponse",  NAMESPACE);
    public static final QName RENAME_CALENDAR_RESOURCE_REQUEST    = QName.get("RenameCalendarResourceRequest",   NAMESPACE);
    public static final QName RENAME_CALENDAR_RESOURCE_RESPONSE   = QName.get("RenameCalendarResourceResponse",  NAMESPACE);
    public static final QName GET_CALENDAR_RESOURCE_REQUEST       = QName.get("GetCalendarResourceRequest",      NAMESPACE);
    public static final QName GET_CALENDAR_RESOURCE_RESPONSE      = QName.get("GetCalendarResourceResponse",     NAMESPACE);
    public static final QName GET_ALL_CALENDAR_RESOURCES_REQUEST  = QName.get("GetAllCalendarResourcesRequest",  NAMESPACE);
    public static final QName GET_ALL_CALENDAR_RESOURCES_RESPONSE = QName.get("GetAllCalendarResourcesResponse", NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_REQUEST   = QName.get("SearchCalendarResourcesRequest",  NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_RESPONSE  = QName.get("SearchCalendarResourcesResponse", NAMESPACE);

    public static final QName SEARCH_MULTIPLE_MAILBOXES_REQUEST = QName.get("SearchMultiMailboxRequest", NAMESPACE);
    public static final QName SEARCH_MULTIPLE_MAILBOXES_RESPONSE = QName.get("SearchMultiMailboxResponse", NAMESPACE);

    public static final QName DUMP_SESSIONS_REQUEST = QName.get("DumpSessionsRequest", NAMESPACE);
    public static final QName DUMP_SESSIONS_RESPONSE = QName.get("DumpSessionsResponse", NAMESPACE);
    public static final QName GET_SESSIONS_REQUEST = QName.get("GetSessionsRequest", NAMESPACE);
    public static final QName GET_SESSIONS_RESPONSE = QName.get("GetSessionsResponse", NAMESPACE);

    public static final QName GET_QUOTA_USAGE_REQUEST = QName.get("GetQuotaUsageRequest", NAMESPACE);
    public static final QName GET_QUOTA_USAGE_RESPONSE = QName.get("GetQuotaUsageResponse", NAMESPACE);

    public static final QName GET_MAIL_QUEUE_INFO_REQUEST = QName.get("GetMailQueueInfoRequest", NAMESPACE);
    public static final QName GET_MAIL_QUEUE_INFO_RESPONSE = QName.get("GetMailQueueInfoResponse", NAMESPACE);
    public static final QName GET_MAIL_QUEUE_REQUEST = QName.get("GetMailQueueRequest", NAMESPACE);
    public static final QName GET_MAIL_QUEUE_RESPONSE = QName.get("GetMailQueueResponse", NAMESPACE);
    public static final QName MAIL_QUEUE_ACTION_REQUEST = QName.get("MailQueueActionRequest", NAMESPACE);
    public static final QName MAIL_QUEUE_ACTION_RESPONSE = QName.get("MailQueueActionResponse", NAMESPACE);
    public static final QName MAIL_QUEUE_FLUSH_REQUEST = QName.get("MailQueueFlushRequest", NAMESPACE);
    public static final QName MAIL_QUEUE_FLUSH_RESPONSE = QName.get("MailQueueFlushResponse", NAMESPACE);

    public static final QName SEARCH_DIRECTORY_REQUEST = QName.get("SearchDirectoryRequest", NAMESPACE);
    public static final QName SEARCH_DIRECTORY_RESPONSE = QName.get("SearchDirectoryResponse", NAMESPACE);

    public static final QName GET_ACCOUNT_MEMBERSHIP_REQUEST = QName.get("GetAccountMembershipRequest", NAMESPACE);
    public static final QName GET_ACCOUNT_MEMBERSHIP_RESPONSE = QName.get("GetAccountMembershipResponse", NAMESPACE);

    public static final QName GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST = QName.get("GetDistributionListMembershipRequest", NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE = QName.get("GetDistributionListMembershipResponse", NAMESPACE);

    public static final QName INIT_NOTEBOOK_REQUEST  = QName.get("InitNotebookRequest",  NAMESPACE);
    public static final QName INIT_NOTEBOOK_RESPONSE = QName.get("InitNotebookResponse", NAMESPACE);

    public static final QName SET_THROTTLE_REQUEST = QName.get("SetThrottleRequest", NAMESPACE);
    public static final QName SET_THROTTLE_RESPOSNE = QName.get("SetThrottleResponse", NAMESPACE);

    // data sources
    public static final QName CREATE_DATA_SOURCE_REQUEST = QName.get("CreateDataSourceRequest", NAMESPACE);
    public static final QName CREATE_DATA_SOURCE_RESPONSE = QName.get("CreateDataSourceResponse", NAMESPACE);
    public static final QName GET_DATA_SOURCES_REQUEST = QName.get("GetDataSourcesRequest", NAMESPACE);
    public static final QName GET_DATA_SOURCES_RESPONSE = QName.get("GetDataSourcesResponse", NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_REQUEST = QName.get("ModifyDataSourceRequest", NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_RESPONSE = QName.get("ModifyDataSourceResponse", NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_REQUEST = QName.get("DeleteDataSourceRequest", NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_RESPONSE = QName.get("DeleteDataSourceResponse", NAMESPACE);

    // calendar time zone fixup
    public static final QName FIX_CALENDAR_TIME_ZONE_REQUEST = QName.get("FixCalendarTimeZoneRequest", NAMESPACE);
    public static final QName FIX_CALENDAR_TIME_ZONE_RESPONSE = QName.get("FixCalendarTimeZoneResponse", NAMESPACE);
    // calendar item end time fixup
    public static final QName FIX_CALENDAR_END_TIME_REQUEST = QName.get("FixCalendarEndTimeRequest", NAMESPACE);
    public static final QName FIX_CALENDAR_END_TIME_RESPONSE = QName.get("FixCalendarEndTimeResponse", NAMESPACE);
    
    // Admin saved searches
    public static final QName GET_ADMIN_SAVED_SEARCHES_REQUEST = QName.get("GetAdminSavedSearchesRequest", NAMESPACE);
    public static final QName GET_ADMIN_SAVED_SEARCHES_RESPONSE = QName.get("GetAdminSavedSearchesResponse", NAMESPACE);
    public static final QName MODIFY_ADMIN_SAVED_SEARCHES_REQUEST = QName.get("ModifyAdminSavedSearchesRequest", NAMESPACE);
    public static final QName MODIFY_ADMIN_SAVED_SEARCHES_RESPONSE = QName.get("ModifyAdminSavedSearchesResponse", NAMESPACE);

    public static final QName CHECK_DIRECTORY_REQUEST = QName.get("CheckDirectoryRequest", NAMESPACE);
    public static final QName CHECK_DIRECTORY_RESPONSE = QName.get("CheckDirectoryResponse", NAMESPACE);
    
    public static final QName FLUSH_CACHE_REQUEST = QName.get("FlushCacheRequest", NAMESPACE);
    public static final QName FLUSH_CACHE_RESPONSE = QName.get("FlushCacheResponse", NAMESPACE);

    // Account loggers
    public static final QName ADD_ACCOUNT_LOGGER_REQUEST = QName.get("AddAccountLoggerRequest", NAMESPACE);
    public static final QName ADD_ACCOUNT_LOGGER_RESPONSE = QName.get("AddAccountLoggerResponse", NAMESPACE);
    public static final QName REMOVE_ACCOUNT_LOGGER_REQUEST = QName.get("RemoveAccountLoggerRequest", NAMESPACE);
    public static final QName REMOVE_ACCOUNT_LOGGER_RESPONSE = QName.get("RemoveAccountLoggerResponse", NAMESPACE);
    public static final QName GET_ACCOUNT_LOGGERS_REQUEST = QName.get("GetAccountLoggersRequest", NAMESPACE);
    public static final QName GET_ACCOUNT_LOGGERS_RESPONSE = QName.get("GetAccountLoggersResponse", NAMESPACE);
    public static final QName GET_ALL_ACCOUNT_LOGGERS_REQUEST = QName.get("GetAllAccountLoggersRequest", NAMESPACE);
    public static final QName GET_ALL_ACCOUNT_LOGGERS_RESPONSE = QName.get("GetAllAccountLoggersResponse", NAMESPACE);

    // DumpSessions
    public static final String E_SESSION = "session";
    public static final String A_ZIMBRA_ID = "zid";
    public static final String A_SESSION_ID = "sid";
    public static final String A_LIST_SESSIONS = "listSessions";
    public static final String A_GROUP_BY_ACCOUNT = "groupByAccount";
    public static final String A_ACTIVE_ACCOUNTS = "activeAccounts";
    public static final String A_ACTIVE_SESSIONS = "activeSessions";
    public static final String A_CREATED_DATE = "cd";
    public static final String A_LAST_ACCESSED_DATE = "ld";

    public static final String E_ACCOUNT = "account";
    public static final String E_CALENDAR_RESOURCE = "calresource";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_NAME = "name";
    public static final String E_NEW_NAME = "newName";
    public static final String E_BINDDN = "bindDn";
    public static final String E_CACHE = "cache";
    public static final String E_CODE = "code";
    public static final String E_COS = "cos";
    public static final String A_COS = "cos";
    public static final String E_CN = "cn";
    public static final String E_DOMAIN = "domain";
    public static final String E_DL = "dl";
    public static final String E_DLM = "dlm";
    public static final String E_ENTRY = "entry";
    public static final String E_HOSTNAME = "hostname";
    public static final String E_LIFETIME = "lifetime";
    public static final String E_MESSAGE = "message";
    public static final String E_PASSWORD = "password";
    public static final String E_NEW_PASSWORD = "newPassword";
    public static final String E_QUERY = "query";
    public static final String E_QUEUE = "queue";
    public static final String E_ACTION = "action";
    public static final String E_SERVER = "server";
    public static final String E_STATUS = "status";
    public static final String E_END_TIME = "endTime";
    public static final String E_START_TIME = "startTime";
    public static final String E_STAT_NAME = "statName";
    public static final String E_PERIOD = "period";
    public static final String E_A = "a";
    public static final String E_S = "s";
    public static final String E_ALIAS = "alias";
    public static final String E_ID = "id";
    public static final String E_PAGE_NUMBER = "pagenum";
    public static final String E_ORDER_BY = "orderby";
    public static final String E_IS_ASCENDING = "isascending";
    public static final String E_RESULTS_PERPAGE = "pageresultsnum";
    public static final String E_ATTRS_TO_GET = "attrstoget";
    public static final String E_MAX_SEARCH_RESULTS = "maxsearchresults";
    public static final String E_MAILBOX = "mbox";
    public static final String E_NI = "ni";
    public static final String E_NUM_OF_PAGES = "numpages";
    public static final String E_VOLUME = "volume";
    public static final String E_PROGRESS = "progress";
    public static final String E_SOAP_URL = "soapURL";
    public static final String E_ADMIN_SOAP_URL = "adminSoapURL";
    public static final String E_SEARCH = "search";
    public static final String E_DIRECTORY = "directory";

    public static final String A_ACTION = "action";
    public static final String A_APPLY_CONFIG = "applyConfig";
    public static final String A_APPLY_COS = "applyCos";
    public static final String A_ID = "id";
    public static final String A_MAX_RESULTS = "maxResults";
    public static final String A_LIMIT = "limit";
    public static final String A_OFFSET = "offset";
    public static final String A_DOMAIN = "domain";
    public static final String A_ATTRS = "attrs";
    public static final String A_SEARCH_TOTAL = "searchTotal";
    public static final String A_SORT_BY = "sortBy";
    public static final String A_SORT_ASCENDING = "sortAscending";
    public static final String A_TYPE = "type";
    public static final String A_C = "c";
    public static final String A_T = "t";
    public static final String A_NAME = "name";
    public static final String A_MORE = "more";
    public static final String A_BY = "by";
    public static final String A_N = "n";
    public static final String A_HOSTNAME = "hn";
    public static final String A_ACCOUNTID = "id";
    public static final String A_MAILBOXID = "mbxid";
    public static final String A_TOTAL = "total";
    public static final String A_TOKEN = "token";
    public static final String A_VIA = "via";
    public static final String A_EXCLUDE = "exclude";
    public static final String A_REFRESH = "refresh";
    public static final String A_TARGETNAME = "targetName";
    public static final String A_PATH = "path";
    public static final String A_CREATE = "create";
    public static final String A_EXISTS = "exists";
    public static final String A_IS_DIRECTORY = "isDirectory";
    public static final String A_READABLE = "readable";
    public static final String A_WRITABLE = "writable";

    public static final String BY_ID = "id";
    public static final String BY_QUERY = "query";
    public static final String BY_NAME = "name";

    public static final String E_FIELD = "field";
    public static final String E_MATCH = "match";
    public static final String A_SCAN = "scan";
    public static final String A_WAIT = "wait";
    public static final String A_QUEUE_SUMMARY = "qs";
    public static final String A_QUEUE_SUMMARY_ITEM = "qsi";
    public static final String A_QUEUE_ITEM = "qi";
    public static final String A_OP = "op";

    public static final String A_HEALTHY = "healthy";
    public static final String A_SIZE = "s";
    public static final String A_SERVICE = "service";
    public static final String A_SERVER = "server";
    public static final String A_STATUS = "status";
    public static final String A_TIME = "time";
    public static final String A_TYPES = "types";
    public static final String A_NUM_TABLES = "numTables";

    public static final String A_NUM_EXECUTED = "numExecuted";
    public static final String A_NUM_SUCCEEDED= "numSucceeded";
    public static final String A_NUM_FAILED = "numFailed";
    public static final String A_NUM_REMAINING = "numRemaining";
    public static final String A_DURATION = "duration";

    public static final String A_VOLUME_TYPE = "type";
    public static final String A_VOLUME_NAME = "name";
    public static final String A_VOLUME_ROOTPATH = "rootpath";
    public static final String A_VOLUME_MGBITS = "mgbits";
    public static final String A_VOLUME_MBITS = "mbits";
    public static final String A_VOLUME_FGBITS = "fgbits";
    public static final String A_VOLUME_FBITS = "fbits";
    public static final String A_VOLUME_COMPRESS_BLOBS = "compressBlobs";
    public static final String A_VOLUME_COMPRESSION_THRESHOLD = "compressionThreshold";
    public static final String A_VOLUME_IS_CURRENT = "isCurrent";

    public static final String A_VERSION_INFO_INFO = "info";
    public static final String A_VERSION_INFO_VERSION = "version";
    public static final String A_VERSION_INFO_RELEASE = "release";
    public static final String A_VERSION_INFO_DATE = "buildDate";
    public static final String A_VERSION_INFO_HOST = "host";

    public static final String E_LICENSE_EXPIRATION = "expiration";
    public static final String A_LICENSE_EXPIRATION_DATE = "date";

    public static final String E_ZIMLET = "zimlet";
    public static final String E_ACL = "acl";
    public static final String E_PRIORITY = "priority";
    public static final String A_EXTENSION = "extension";
    public static final String A_MAIL = "mail";
    public static final String A_VALUE = "value";
    public static final String A_PRIORITY = "priority";
    public static final String A_ACL = "acl";
    public static final String A_NONE = "none";

    public static final String A_QUOTA_USED = "used";
    public static final String A_QUOTA_LIMIT = "limit";

    public static final String E_TEMPLATE = "template";
    public static final String E_TEST = "test";
    public static final String A_DEST = "dest";

    public static final String A_CONCURRENCY = "concurrency";

    public static final String A_DEPLOYALL = "deployall";
    public static final String A_DEPLOYLOCAL = "deploylocal";

    public static final String A_COUNTRY = "country";
    public static final String A_TZFIXUP_AFTER = "after";
    public static final String A_TZFIXUP_SYNC = "sync";
    
    // Account loggers
    public static final String E_LOGGER = "logger";
    public static final String E_ACCOUNT_LOGGER = "accountLogger";
    public static final String A_CATEGORY = "category";
    public static final String A_LEVEL = "level";

}
