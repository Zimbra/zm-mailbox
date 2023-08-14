/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2021, 2022, 2023 Synacor, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import java.util.Arrays;
import java.util.List;

public final class AdminConstants {

    public static final String ADMIN_SERVICE_URI = "/service/admin/soap/";

    public static final String NAMESPACE_STR = "urn:zimbraAdmin";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_PING_REQUEST = "PingRequest";
    public static final String E_PING_RESPONSE = "PingResponse";
    public static final String E_CHECK_HEALTH_REQUEST = "CheckHealthRequest";
    public static final String E_CHECK_HEALTH_RESPONSE = "CheckHealthResponse";

    public static final String E_GET_ALL_LOCALES_REQUEST = "GetAllLocalesRequest";
    public static final String E_GET_ALL_LOCALES_RESPONSE = "GetAllLocalesResponse";

    public static final String E_EXPORTMAILBOX_REQUEST = "ExportMailboxRequest";
    public static final String E_EXPORTMAILBOX_RESPONSE = "ExportMailboxResponse";

    public static final String E_AUTH_REQUEST = "AuthRequest";
    public static final String E_AUTH_RESPONSE = "AuthResponse";
    public static final String E_CREATE_ACCOUNT_REQUEST = "CreateAccountRequest";
    public static final String E_CREATE_ACCOUNT_RESPONSE = "CreateAccountResponse";
    public static final String E_CREATE_GAL_SYNC_ACCOUNT_REQUEST = "CreateGalSyncAccountRequest";
    public static final String E_CREATE_GAL_SYNC_ACCOUNT_RESPONSE = "CreateGalSyncAccountResponse";
    public static final String E_ADD_GAL_SYNC_DATASOURCE_REQUEST = "AddGalSyncDataSourceRequest";
    public static final String E_ADD_GAL_SYNC_DATASOURCE_RESPONSE = "AddGalSyncDataSourceResponse";
    public static final String E_DELEGATE_AUTH_REQUEST = "DelegateAuthRequest";
    public static final String E_DELEGATE_AUTH_RESPONSE = "DelegateAuthResponse";
    public static final String E_DELETE_GAL_SYNC_ACCOUNT_REQUEST = "DeleteGalSyncAccountRequest";
    public static final String E_DELETE_GAL_SYNC_ACCOUNT_RESPONSE = "DeleteGalSyncAccountResponse";
    public static final String E_GET_ACCOUNT_REQUEST = "GetAccountRequest";
    public static final String E_GET_ACCOUNT_RESPONSE = "GetAccountResponse";
    public static final String E_GET_ACCOUNT_INFO_REQUEST = "GetAccountInfoRequest";
    public static final String E_GET_ACCOUNT_INFO_RESPONSE = "GetAccountInfoResponse";
    public static final String E_GET_ALL_ACCOUNTS_REQUEST = "GetAllAccountsRequest";
    public static final String E_GET_ALL_ACCOUNTS_RESPONSE = "GetAllAccountsResponse";
    public static final String E_GET_ALL_ADMIN_ACCOUNTS_REQUEST = "GetAllAdminAccountsRequest";
    public static final String E_GET_ALL_ADMIN_ACCOUNTS_RESPONSE = "GetAllAdminAccountsResponse";
    public static final String E_MODIFY_ACCOUNT_REQUEST = "ModifyAccountRequest";
    public static final String E_MODIFY_ACCOUNT_RESPONSE = "ModifyAccountResponse";
    public static final String E_DELETE_ACCOUNT_REQUEST = "DeleteAccountRequest";
    public static final String E_DELETE_ACCOUNT_RESPONSE = "DeleteAccountResponse";
    public static final String E_SET_PASSWORD_REQUEST = "SetPasswordRequest";
    public static final String E_SET_PASSWORD_RESPONSE = "SetPasswordResponse";
    public static final String E_CHECK_PASSWORD_STRENGTH_REQUEST = "CheckPasswordStrengthRequest";
    public static final String E_CHECK_PASSWORD_STRENGTH_RESPONSE = "CheckPasswordStrengthResponse";
    public static final String E_RESET_ACCOUNT_PASSWORD_REQUEST = "ResetAccountPasswordRequest";
    public static final String E_RESET_ACCOUNT_PASSWORD_RESPONSE = "ResetAccountPasswordResponse";

    public static final String E_ADD_ACCOUNT_ALIAS_REQUEST = "AddAccountAliasRequest";
    public static final String E_ADD_ACCOUNT_ALIAS_RESPONSE = "AddAccountAliasResponse";
    public static final String E_REMOVE_ACCOUNT_ALIAS_REQUEST = "RemoveAccountAliasRequest";
    public static final String E_REMOVE_ACCOUNT_ALIAS_RESPONSE = "RemoveAccountAliasResponse";
    public static final String E_SEARCH_ACCOUNTS_REQUEST = "SearchAccountsRequest";
    public static final String E_SEARCH_ACCOUNTS_RESPONSE = "SearchAccountsResponse";
    public static final String E_RENAME_ACCOUNT_REQUEST = "RenameAccountRequest";
    public static final String E_RENAME_ACCOUNT_RESPONSE = "RenameAccountResponse";
    public static final String E_CHANGE_PRIMARY_EMAIL_REQUEST = "ChangePrimaryEmailRequest";
    public static final String E_CHANGE_PRIMARY_EMAIL_RESPONSE = "ChangePrimaryEmailResponse";

    public static final String E_CREATE_DOMAIN_REQUEST = "CreateDomainRequest";
    public static final String E_CREATE_DOMAIN_RESPONSE = "CreateDomainResponse";
    public static final String E_GET_DOMAIN_REQUEST = "GetDomainRequest";
    public static final String E_GET_DOMAIN_RESPONSE = "GetDomainResponse";
    public static final String E_GET_DOMAIN_INFO_REQUEST = "GetDomainInfoRequest";
    public static final String E_GET_DOMAIN_INFO_RESPONSE = "GetDomainInfoResponse";
    public static final String E_MODIFY_DOMAIN_REQUEST = "ModifyDomainRequest";
    public static final String E_MODIFY_DOMAIN_RESPONSE = "ModifyDomainResponse";
    public static final String E_DELETE_DOMAIN_REQUEST = "DeleteDomainRequest";
    public static final String E_DELETE_DOMAIN_RESPONSE = "DeleteDomainResponse";
    public static final String E_GET_ALL_DOMAINS_REQUEST = "GetAllDomainsRequest";
    public static final String E_GET_ALL_DOMAINS_RESPONSE = "GetAllDomainsResponse";

    public static final String E_CREATE_COS_REQUEST = "CreateCosRequest";
    public static final String E_CREATE_COS_RESPONSE = "CreateCosResponse";
    public static final String E_COPY_COS_REQUEST = "CopyCosRequest";
    public static final String E_COPY_COS_RESPONSE = "CopyCosResponse";
    public static final String E_GET_COS_REQUEST = "GetCosRequest";
    public static final String E_GET_COS_RESPONSE = "GetCosResponse";
    public static final String E_MODIFY_COS_REQUEST = "ModifyCosRequest";
    public static final String E_MODIFY_COS_RESPONSE = "ModifyCosResponse";
    public static final String E_DELETE_COS_REQUEST = "DeleteCosRequest";
    public static final String E_DELETE_COS_RESPONSE = "DeleteCosResponse";
    public static final String E_GET_ALL_COS_REQUEST = "GetAllCosRequest";
    public static final String E_GET_ALL_COS_RESPONSE = "GetAllCosResponse";
    public static final String E_RENAME_COS_REQUEST = "RenameCosRequest";
    public static final String E_RENAME_COS_RESPONSE = "RenameCosResponse";

    public static final String E_CREATE_SERVER_REQUEST = "CreateServerRequest";
    public static final String E_CREATE_SERVER_RESPONSE = "CreateServerResponse";
    public static final String E_GET_SERVER_REQUEST = "GetServerRequest";
    public static final String E_GET_SERVER_RESPONSE = "GetServerResponse";

    public static final String E_GET_SERVER_NIFS_REQUEST = "GetServerNIfsRequest";
    public static final String E_GET_SERVER_NIFS_RESPONSE = "GetServerNIfsResponse";

    public static final String E_MODIFY_SERVER_REQUEST = "ModifyServerRequest";
    public static final String E_MODIFY_SERVER_RESPONSE = "ModifyServerResponse";
    public static final String E_DELETE_SERVER_REQUEST = "DeleteServerRequest";
    public static final String E_DELETE_SERVER_RESPONSE = "DeleteServerResponse";
    public static final String E_GET_ALL_SERVERS_REQUEST = "GetAllServersRequest";
    public static final String E_GET_ALL_SERVERS_RESPONSE = "GetAllServersResponse";

    public static final String E_CREATE_ALWAYSONCLUSTER_REQUEST = "CreateAlwaysOnClusterRequest";
    public static final String E_CREATE_ALWAYSONCLUSTER_RESPONSE = "CreateAlwaysOnClusterResponse";
    public static final String E_GET_ALWAYSONCLUSTER_REQUEST = "GetAlwaysOnClusterRequest";
    public static final String E_GET_ALWAYSONCLUSTER_RESPONSE = "GetAlwaysOnClusterResponse";

    public static final String E_MODIFY_ALWAYSONCLUSTER_REQUEST = "ModifyAlwaysOnClusterRequest";
    public static final String E_MODIFY_ALWAYSONCLUSTER_RESPONSE = "ModifyAlwaysOnClusterResponse";
    public static final String E_DELETE_ALWAYSONCLUSTER_REQUEST = "DeleteAlwaysOnClusterRequest";
    public static final String E_DELETE_ALWAYSONCLUSTER_RESPONSE = "DeleteAlwaysOnClusterResponse";
    public static final String E_GET_ALL_ALWAYSONCLUSTERS_REQUEST = "GetAllAlwaysOnClustersRequest";
    public static final String E_GET_ALL_ALWAYSONCLUSTERS_RESPONSE = "GetAllAlwaysOnClustersResponse";

    public static final String E_CREATE_UC_SERVICE_REQUEST = "CreateUCServiceRequest";
    public static final String E_CREATE_UC_SERVICE_RESPONSE = "CreateUCServiceResponse";
    public static final String E_DELETE_UC_SERVICE_REQUEST = "DeleteUCServiceRequest";
    public static final String E_DELETE_UC_SERVICE_RESPONSE = "DeleteUCServiceResponse";
    public static final String E_GET_UC_SERVICE_REQUEST = "GetUCServiceRequest";
    public static final String E_GET_UC_SERVICE_RESPONSE = "GetUCServiceResponse";
    public static final String E_GET_ALL_UC_SERVICES_REQUEST = "GetAllUCServicesRequest";
    public static final String E_GET_ALL_UC_SERVICES_RESPONSE = "GetAllUCServicesResponse";
    public static final String E_MODIFY_UC_SERVICE_REQUEST = "ModifyUCServiceRequest";
    public static final String E_MODIFY_UC_SERVICE_RESPONSE = "ModifyUCServiceResponse";
    public static final String E_RENAME_UC_SERVICE_REQUEST = "RenameUCServiceRequest";
    public static final String E_RENAME_UC_SERVICE_RESPONSE = "RenameUCServiceResponse";

    public static final String E_GET_CONFIG_REQUEST = "GetConfigRequest";
    public static final String E_GET_CONFIG_RESPONSE = "GetConfigResponse";
    public static final String E_MODIFY_CONFIG_REQUEST = "ModifyConfigRequest";
    public static final String E_MODIFY_CONFIG_RESPONSE = "ModifyConfigResponse";
    public static final String E_GET_ALL_CONFIG_REQUEST = "GetAllConfigRequest";
    public static final String E_GET_ALL_CONFIG_RESPONSE = "GetAllConfigResponse";

    public static final String E_GET_SERVICE_STATUS_REQUEST = "GetServiceStatusRequest";
    public static final String E_GET_SERVICE_STATUS_RESPONSE = "GetServiceStatusResponse";

    public static final String E_PURGE_MESSAGES_REQUEST = "PurgeMessagesRequest";
    public static final String E_PURGE_MESSAGES_RESPONSE = "PurgeMessagesResponse";
    public static final String E_DELETE_MAILBOX_REQUEST = "DeleteMailboxRequest";
    public static final String E_DELETE_MAILBOX_RESPONSE = "DeleteMailboxResponse";
    public static final String E_GET_MAILBOX_REQUEST = "GetMailboxRequest";
    public static final String E_GET_MAILBOX_RESPONSE = "GetMailboxResponse";
    public static final String E_LOCKOUT_MAILBOX_REQUEST = "LockoutMailboxRequest";
    public static final String E_LOCKOUT_MAILBOX_RESPONSE = "LockoutMailboxResponse";

    public static final String E_RUN_UNIT_TESTS_REQUEST = "RunUnitTestsRequest";
    public static final String E_RUN_UNIT_TESTS_RESPONSE = "RunUnitTestsResponse";

    public static final String E_CHECK_HOSTNAME_RESOLVE_REQUEST = "CheckHostnameResolveRequest";
    public static final String E_CHECK_HOSTNAME_RESOLVE_RESPONSE = "CheckHostnameResolveResponse";
    public static final String E_CHECK_AUTH_CONFIG_REQUEST = "CheckAuthConfigRequest";
    public static final String E_CHECK_AUTH_CONFIG_RESPONSE = "CheckAuthConfigResponse";
    public static final String E_CHECK_GAL_CONFIG_REQUEST = "CheckGalConfigRequest";
    public static final String E_CHECK_GAL_CONFIG_RESPONSE = "CheckGalConfigResponse";
    public static final String E_CHECK_EXCHANGE_AUTH_REQUEST = "CheckExchangeAuthRequest";
    public static final String E_CHECK_EXCHANGE_AUTH_RESPONSE = "CheckExchangeAuthResponse";
    public static final String E_CHECK_DOMAIN_MX_RECORD_REQUEST = "CheckDomainMXRecordRequest";
    public static final String E_CHECK_DOMAIN_MX_RECORD_RESPONSE = "CheckDomainMXRecordResponse";

    public static final String E_AUTO_COMPLETE_GAL_REQUEST = "AutoCompleteGalRequest";
    public static final String E_AUTO_COMPLETE_GAL_RESPONSE = "AutoCompleteGalResponse";
    public static final String E_SEARCH_GAL_REQUEST = "SearchGalRequest";
    public static final String E_SEARCH_GAL_RESPONSE = "SearchGalResponse";

    public static final String E_CREATE_VOLUME_REQUEST = "CreateVolumeRequest";
    public static final String E_CREATE_VOLUME_RESPONSE = "CreateVolumeResponse";
    public static final String E_GET_VOLUME_REQUEST = "GetVolumeRequest";
    public static final String E_GET_VOLUME_RESPONSE = "GetVolumeResponse";
    public static final String E_MODIFY_VOLUME_REQUEST = "ModifyVolumeRequest";
    public static final String E_MODIFY_VOLUME_RESPONSE = "ModifyVolumeResponse";
    public static final String E_MODIFY_VOLUME_INPLACE_UPGRADE_REQUEST = "ModifyVolumeInplaceUpgradeRequest";
    public static final String E_MODIFY_VOLUME_INPLACE_UPGRADE_RESPONSE = "ModifyVolumeInplaceUpgradeResponse";
    public static final String E_DELETE_VOLUME_REQUEST = "DeleteVolumeRequest";
    public static final String E_DELETE_VOLUME_RESPONSE = "DeleteVolumeResponse";
    public static final String E_GET_ALL_VOLUMES_REQUEST = "GetAllVolumesRequest";
    public static final String E_GET_ALL_VOLUMES_RESPONSE = "GetAllVolumesResponse";
    public static final String E_GET_ALL_VOLUMES_INPLACE_UPGRADE_REQUEST = "GetAllVolumesInplaceUpgradeRequest";
    public static final String E_GET_ALL_VOLUMES_INPLACE_UPGRADE_RESPONSE = "GetAllVolumesInplaceUpgradeResponse";
    public static final String E_GET_CURRENT_VOLUMES_REQUEST = "GetCurrentVolumesRequest";
    public static final String E_GET_CURRENT_VOLUMES_RESPONSE = "GetCurrentVolumesResponse";
    public static final String E_SET_CURRENT_VOLUME_REQUEST = "SetCurrentVolumeRequest";
    public static final String E_SET_CURRENT_VOLUME_RESPONSE = "SetCurrentVolumeResponse";
    public static final String E_CHECK_BLOB_CONSISTENCY_REQUEST = "CheckBlobConsistencyRequest";
    public static final String E_CHECK_BLOB_CONSISTENCY_RESPONSE = "CheckBlobConsistencyResponse";
    public static final String E_EXPORT_AND_DELETE_ITEMS_REQUEST = "ExportAndDeleteItemsRequest";
    public static final String E_EXPORT_AND_DELETE_ITEMS_RESPONSE = "ExportAndDeleteItemsResponse";
    public static final String E_DEDUPE_BLOBS_REQUEST = "DedupeBlobsRequest";
    public static final String E_DEDUPE_BLOBS_RESPONSE = "DedupeBlobsResponse";
    public static final String E_GET_ALL_ACTIVE_SERVERS_REQUEST = "GetAllActiveServersRequest";
    public static final String E_GET_ALL_ACTIVE_SERVERS_RESPONSE = "GetAllActiveServersResponse";
    public static final String E_SET_SERVER_OFFLINE_REQUEST = "SetServerOfflineRequest";
    public static final String E_SET_SERVER_OFFLINE_RESPONSE = "SetServerOfflineResponse";
    public static final String E_SET_LOCAL_SERVER_ONLINE_REQUEST = "SetLocalServerOnlineRequest";
    public static final String E_SET_LOCAL_SERVER_ONLINE_RESPONSE = "SetLocalServerOnlineResponse";

    public static final String E_CREATE_DISTRIBUTION_LIST_REQUEST = "CreateDistributionListRequest";
    public static final String E_CREATE_DISTRIBUTION_LIST_RESPONSE = "CreateDistributionListResponse";
    public static final String E_GET_DISTRIBUTION_LIST_REQUEST = "GetDistributionListRequest";
    public static final String E_GET_DISTRIBUTION_LIST_RESPONSE = "GetDistributionListResponse";
    public static final String E_GET_ALL_DISTRIBUTION_LISTS_REQUEST = "GetAllDistributionListsRequest";
    public static final String E_GET_ALL_DISTRIBUTION_LISTS_RESPONSE = "GetAllDistributionListsResponse";
    public static final String E_ADD_DISTRIBUTION_LIST_MEMBER_REQUEST = "AddDistributionListMemberRequest";
    public static final String E_ADD_DISTRIBUTION_LIST_MEMBER_RESPONSE = "AddDistributionListMemberResponse";
    public static final String E_REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST = "RemoveDistributionListMemberRequest";
    public static final String E_REMOVE_DISTRIBUTION_LIST_MEMBER_RESPONSE = "RemoveDistributionListMemberResponse";
    public static final String E_MODIFY_DISTRIBUTION_LIST_REQUEST = "ModifyDistributionListRequest";
    public static final String E_MODIFY_DISTRIBUTION_LIST_RESPONSE = "ModifyDistributionListResponse";
    public static final String E_DELETE_DISTRIBUTION_LIST_REQUEST = "DeleteDistributionListRequest";
    public static final String E_DELETE_DISTRIBUTION_LIST_RESPONSE = "DeleteDistributionListResponse";
    public static final String E_ADD_DISTRIBUTION_LIST_ALIAS_REQUEST = "AddDistributionListAliasRequest";
    public static final String E_ADD_DISTRIBUTION_LIST_ALIAS_RESPONSE = "AddDistributionListAliasResponse";
    public static final String E_REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST = "RemoveDistributionListAliasRequest";
    public static final String E_REMOVE_DISTRIBUTION_LIST_ALIAS_RESPONSE = "RemoveDistributionListAliasResponse";
    public static final String E_RENAME_DISTRIBUTION_LIST_REQUEST = "RenameDistributionListRequest";
    public static final String E_RENAME_DISTRIBUTION_LIST_RESPONSE = "RenameDistributionListResponse";

    public static final String E_CREATE_HAB_GROUP_REQUEST = "CreateHABGroupRequest";
    public static final String E_CREATE_HAB_GROUP_RESPONSE = "CreateHABGroupResponse";
    public static final String E_MODIFY_HAB_GROUP_REQUEST = "ModifyHABGroupRequest";
    public static final String E_MODIFY_HAB_GROUP_RESPONSE = "ModifyHABGroupResponse";


    public static final String E_GET_VERSION_INFO_REQUEST = "GetVersionInfoRequest";
    public static final String E_GET_VERSION_INFO_RESPONSE = "GetVersionInfoResponse";

    public static final String E_GET_LICENSE_INFO_REQUEST = "GetLicenseInfoRequest";
    public static final String E_GET_LICENSE_INFO_RESPONSE = "GetLicenseInfoResponse";

    public static final String E_GET_ATTRIBUTE_INFO_REQUEST = "GetAttributeInfoRequest";
    public static final String E_GET_ATTRIBUTE_INFO_RESPONSE = "GetAttributeInfoResponse";

    public static final String E_REINDEX_REQUEST = "ReIndexRequest";
    public static final String E_REINDEX_RESPONSE = "ReIndexResponse";
    public static final String E_MANAGE_INDEX_REQUEST = "ManageIndexRequest";
    public static final String E_MANAGE_INDEX_RESPONSE = "ManageIndexResponse";
    public static final String E_COMPACT_INDEX_REQUEST = "CompactIndexRequest";
    public static final String E_COMPACT_INDEX_RESPONSE = "CompactIndexResponse";
    public static final String E_GET_INDEX_STATS_REQUEST = "GetIndexStatsRequest";
    public static final String E_GET_INDEX_STATS_RESPONSE = "GetIndexStatsResponse";
    public static final String E_VERIFY_INDEX_REQUEST = "VerifyIndexRequest";
    public static final String E_VERIFY_INDEX_RESPONSE = "VerifyIndexResponse";
    public static final String E_RECALCULATE_MAILBOX_COUNTS_REQUEST = "RecalculateMailboxCountsRequest";
    public static final String E_RECALCULATE_MAILBOX_COUNTS_RESPONSE = "RecalculateMailboxCountsResponse";

    public static final String E_GET_ZIMLET_REQUEST = "GetZimletRequest";
    public static final String E_GET_ZIMLET_RESPONSE = "GetZimletResponse";
    public static final String E_CREATE_ZIMLET_REQUEST = "CreateZimletRequest";
    public static final String E_CREATE_ZIMLET_RESPONSE = "CreateZimletResponse";
    public static final String E_DELETE_ZIMLET_REQUEST = "DeleteZimletRequest";
    public static final String E_DELETE_ZIMLET_RESPONSE = "DeleteZimletResponse";
    public static final String E_GET_ADMIN_EXTENSION_ZIMLETS_REQUEST = "GetAdminExtensionZimletsRequest";
    public static final String E_GET_ADMIN_EXTENSION_ZIMLETS_RESPONSE = "GetAdminExtensionZimletsResponse";
    public static final String E_GET_ALL_ZIMLETS_REQUEST = "GetAllZimletsRequest";
    public static final String E_GET_ALL_ZIMLETS_RESPONSE = "GetAllZimletsResponse";
    public static final String E_GET_ZIMLET_STATUS_REQUEST = "GetZimletStatusRequest";
    public static final String E_GET_ZIMLET_STATUS_RESPONSE = "GetZimletStatusResponse";
    public static final String E_DEPLOY_ZIMLET_REQUEST = "DeployZimletRequest";
    public static final String E_DEPLOY_ZIMLET_RESPONSE = "DeployZimletResponse";
    public static final String E_UNDEPLOY_ZIMLET_REQUEST = "UndeployZimletRequest";
    public static final String E_UNDEPLOY_ZIMLET_RESPONSE = "UndeployZimletResponse";
    public static final String E_CONFIGURE_ZIMLET_REQUEST = "ConfigureZimletRequest";
    public static final String E_CONFIGURE_ZIMLET_RESPONSE = "ConfigureZimletResponse";
    public static final String E_MODIFY_ZIMLET_REQUEST = "ModifyZimletRequest";
    public static final String E_MODIFY_ZIMLET_RESPONSE = "ModifyZimletResponse";

    public static final String E_CREATE_CALENDAR_RESOURCE_REQUEST = "CreateCalendarResourceRequest";
    public static final String E_CREATE_CALENDAR_RESOURCE_RESPONSE = "CreateCalendarResourceResponse";
    public static final String E_DELETE_CALENDAR_RESOURCE_REQUEST = "DeleteCalendarResourceRequest";
    public static final String E_DELETE_CALENDAR_RESOURCE_RESPONSE = "DeleteCalendarResourceResponse";
    public static final String E_MODIFY_CALENDAR_RESOURCE_REQUEST = "ModifyCalendarResourceRequest";
    public static final String E_MODIFY_CALENDAR_RESOURCE_RESPONSE = "ModifyCalendarResourceResponse";
    public static final String E_RENAME_CALENDAR_RESOURCE_REQUEST = "RenameCalendarResourceRequest";
    public static final String E_RENAME_CALENDAR_RESOURCE_RESPONSE = "RenameCalendarResourceResponse";
    public static final String E_GET_CALENDAR_RESOURCE_REQUEST = "GetCalendarResourceRequest";
    public static final String E_GET_CALENDAR_RESOURCE_RESPONSE = "GetCalendarResourceResponse";
    public static final String E_GET_ALL_CALENDAR_RESOURCES_REQUEST = "GetAllCalendarResourcesRequest";
    public static final String E_GET_ALL_CALENDAR_RESOURCES_RESPONSE = "GetAllCalendarResourcesResponse";
    public static final String E_SEARCH_CALENDAR_RESOURCES_REQUEST = "SearchCalendarResourcesRequest";
    public static final String E_SEARCH_CALENDAR_RESOURCES_RESPONSE = "SearchCalendarResourcesResponse";

    public static final String E_SEARCH_MULTIPLE_MAILBOXES_REQUEST = "SearchMultiMailboxRequest";
    public static final String E_SEARCH_MULTIPLE_MAILBOXES_RESPONSE = "SearchMultiMailboxResponse";

    public static final String E_DUMP_SESSIONS_REQUEST = "DumpSessionsRequest";
    public static final String E_DUMP_SESSIONS_RESPONSE = "DumpSessionsResponse";
    public static final String E_GET_SESSIONS_REQUEST = "GetSessionsRequest";
    public static final String E_GET_SESSIONS_RESPONSE = "GetSessionsResponse";

    public static final String E_GET_QUOTA_USAGE_REQUEST = "GetQuotaUsageRequest";
    public static final String E_GET_QUOTA_USAGE_RESPONSE = "GetQuotaUsageResponse";
    public static final String E_COMPUTE_AGGR_QUOTA_USAGE_REQUEST = "ComputeAggregateQuotaUsageRequest";
    public static final String E_COMPUTE_AGGR_QUOTA_USAGE_RESPONSE = "ComputeAggregateQuotaUsageResponse";
    public static final String E_GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST = "GetAggregateQuotaUsageOnServerRequest";
    public static final String E_GET_AGGR_QUOTA_USAGE_ON_SERVER_RESPONSE = "GetAggregateQuotaUsageOnServerResponse";
    public static final String E_GET_ALL_MAILBOXES_REQUEST = "GetAllMailboxesRequest";
    public static final String E_GET_ALL_MAILBOXES_RESPONSE = "GetAllMailboxesResponse";
    public static final String E_GET_MAILBOX_STATS_REQUEST = "GetMailboxStatsRequest";
    public static final String E_GET_MAILBOX_STATS_RESPONSE = "GetMailboxStatsResponse";

    public static final String E_GET_MAIL_QUEUE_INFO_REQUEST = "GetMailQueueInfoRequest";
    public static final String E_GET_MAIL_QUEUE_INFO_RESPONSE = "GetMailQueueInfoResponse";
    public static final String E_GET_MAIL_QUEUE_REQUEST = "GetMailQueueRequest";
    public static final String E_GET_MAIL_QUEUE_RESPONSE = "GetMailQueueResponse";
    public static final String E_MAIL_QUEUE_ACTION_REQUEST = "MailQueueActionRequest";
    public static final String E_MAIL_QUEUE_ACTION_RESPONSE = "MailQueueActionResponse";
    public static final String E_MAIL_QUEUE_FLUSH_REQUEST = "MailQueueFlushRequest";
    public static final String E_MAIL_QUEUE_FLUSH_RESPONSE = "MailQueueFlushResponse";

    public static final String E_SEARCH_DIRECTORY_REQUEST = "SearchDirectoryRequest";
    public static final String E_SEARCH_DIRECTORY_RESPONSE = "SearchDirectoryResponse";

    public static final String E_GET_ACCOUNT_MEMBERSHIP_REQUEST = "GetAccountMembershipRequest";
    public static final String E_GET_ACCOUNT_MEMBERSHIP_RESPONSE = "GetAccountMembershipResponse";

    public static final String E_GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST = "GetDistributionListMembershipRequest";
    public static final String E_GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE = "GetDistributionListMembershipResponse";

    // data sources
    public static final String E_CREATE_DATA_SOURCE_REQUEST = "CreateDataSourceRequest";
    public static final String E_CREATE_DATA_SOURCE_RESPONSE = "CreateDataSourceResponse";
    public static final String E_GET_DATA_SOURCES_REQUEST = "GetDataSourcesRequest";
    public static final String E_GET_DATA_SOURCES_RESPONSE = "GetDataSourcesResponse";
    public static final String E_MODIFY_DATA_SOURCE_REQUEST = "ModifyDataSourceRequest";
    public static final String E_MODIFY_DATA_SOURCE_RESPONSE = "ModifyDataSourceResponse";
    public static final String E_DELETE_DATA_SOURCE_REQUEST = "DeleteDataSourceRequest";
    public static final String E_DELETE_DATA_SOURCE_RESPONSE = "DeleteDataSourceResponse";

    // calendar time zone fixup
    public static final String E_FIX_CALENDAR_TZ_REQUEST = "FixCalendarTZRequest";
    public static final String E_FIX_CALENDAR_TZ_RESPONSE = "FixCalendarTZResponse";

    // calendar item end time fixup
    public static final String E_FIX_CALENDAR_END_TIME_REQUEST = "FixCalendarEndTimeRequest";
    public static final String E_FIX_CALENDAR_END_TIME_RESPONSE = "FixCalendarEndTimeResponse";

    // calendar item priority fixup
    public static final String E_FIX_CALENDAR_PRIORITY_REQUEST = "FixCalendarPriorityRequest";
    public static final String E_FIX_CALENDAR_PRIORITY_RESPONSE = "FixCalendarPriorityResponse";

    // Admin saved searches
    public static final String E_GET_ADMIN_SAVED_SEARCHES_REQUEST = "GetAdminSavedSearchesRequest";
    public static final String E_GET_ADMIN_SAVED_SEARCHES_RESPONSE = "GetAdminSavedSearchesResponse";
    public static final String E_MODIFY_ADMIN_SAVED_SEARCHES_REQUEST = "ModifyAdminSavedSearchesRequest";
    public static final String E_MODIFY_ADMIN_SAVED_SEARCHES_RESPONSE = "ModifyAdminSavedSearchesResponse";

    public static final String E_CHECK_DIRECTORY_REQUEST = "CheckDirectoryRequest";
    public static final String E_CHECK_DIRECTORY_RESPONSE = "CheckDirectoryResponse";

    public static final String E_FLUSH_CACHE_REQUEST = "FlushCacheRequest";
    public static final String E_FLUSH_CACHE_RESPONSE = "FlushCacheResponse";

    public static final String E_COUNT_ACCOUNT_REQUEST = "CountAccountRequest";
    public static final String E_COUNT_ACCOUNT_RESPONSE = "CountAccountResponse";

    public static final String E_COUNT_OBJECTS_REQUEST = "CountObjectsRequest";
    public static final String E_COUNT_OBJECTS_RESPONSE = "CountObjectsResponse";

    public static final String E_GET_SHARE_INFO_REQUEST = "GetShareInfoRequest";
    public static final String E_GET_SHARE_INFO_RESPONSE = "GetShareInfoResponse";

    // Account loggers
    public static final String E_ADD_ACCOUNT_LOGGER_REQUEST = "AddAccountLoggerRequest";
    public static final String E_ADD_ACCOUNT_LOGGER_RESPONSE = "AddAccountLoggerResponse";
    public static final String E_REMOVE_ACCOUNT_LOGGER_REQUEST = "RemoveAccountLoggerRequest";
    public static final String E_REMOVE_ACCOUNT_LOGGER_RESPONSE = "RemoveAccountLoggerResponse";
    public static final String E_GET_ACCOUNT_LOGGERS_REQUEST = "GetAccountLoggersRequest";
    public static final String E_GET_ACCOUNT_LOGGERS_RESPONSE = "GetAccountLoggersResponse";
    public static final String E_GET_ALL_ACCOUNT_LOGGERS_REQUEST = "GetAllAccountLoggersRequest";
    public static final String E_GET_ALL_ACCOUNT_LOGGERS_RESPONSE = "GetAllAccountLoggersResponse";
    public static final String E_RESET_ALL_LOGGERS_REQUEST = "ResetAllLoggersRequest";
    public static final String E_RESET_ALL_LOGGERS_RESPONSE = "ResetAllLoggersResponse";

    // f/b providers
    public static final String E_GET_ALL_FREE_BUSY_PROVIDERS_REQUEST = "GetAllFreeBusyProvidersRequest";
    public static final String E_GET_ALL_FREE_BUSY_PROVIDERS_RESPONSE = "GetAllFreeBusyProvidersResponse";
    public static final String E_GET_FREE_BUSY_QUEUE_INFO_REQUEST = "GetFreeBusyQueueInfoRequest";
    public static final String E_GET_FREE_BUSY_QUEUE_INFO_RESPONSE = "GetFreeBusyQueueInfoResponse";
    public static final String E_PUSH_FREE_BUSY_REQUEST = "PushFreeBusyRequest";
    public static final String E_PUSH_FREE_BUSY_RESPONSE = "PushFreeBusyResponse";
    public static final String E_PURGE_FREE_BUSY_QUEUE_REQUEST = "PurgeFreeBusyQueueRequest";
    public static final String E_PURGE_FREE_BUSY_QUEUE_RESPONSE = "PurgeFreeBusyQueueResponse";

    // calendar cache
    public static final String E_PURGE_ACCOUNT_CALENDAR_CACHE_REQUEST = "PurgeAccountCalendarCacheRequest";
    public static final String E_PURGE_ACCOUNT_CALENDAR_CACHE_RESPONSE = "PurgeAccountCalendarCacheResponse";

    // admin-version of WaitSetRequest
    public static final String E_ADMIN_CREATE_WAIT_SET_REQUEST = "AdminCreateWaitSetRequest";
    public static final String E_ADMIN_CREATE_WAIT_SET_RESPONSE = "AdminCreateWaitSetResponse";
    public static final String E_ADMIN_WAIT_SET_REQUEST = "AdminWaitSetRequest";
    public static final String E_ADMIN_WAIT_SET_RESPONSE = "AdminWaitSetResponse";
    public static final String E_ADMIN_DESTROY_WAIT_SET_REQUEST = "AdminDestroyWaitSetRequest";
    public static final String E_ADMIN_DESTROY_WAIT_SET_RESPONSE = "AdminDestroyWaitSetResponse";
    public static final String E_QUERY_WAIT_SET_REQUEST = "QueryWaitSetRequest";
    public static final String E_QUERY_WAIT_SET_RESPONSE = "QueryWaitSetResponse";

    // XMPPComponent
    public static final String E_CREATE_XMPPCOMPONENT_REQUEST = "CreateXMPPComponentRequest";
    public static final String E_CREATE_XMPPCOMPONENT_RESPONSE = "CreateXMPPComponentResponse";
    public static final String E_GET_XMPPCOMPONENT_REQUEST = "GetXMPPComponentRequest";
    public static final String E_GET_XMPPCOMPONENT_RESPONSE = "GetXMPPComponentResponse";
    public static final String E_GET_ALL_XMPPCOMPONENTS_REQUEST = "GetAllXMPPComponentsRequest";
    public static final String E_GET_ALL_XMPPCOMPONENTS_RESPONSE = "GetAllXMPPComponentsResponse";
    public static final String E_DELETE_XMPPCOMPONENT_REQUEST = "DeleteXMPPComponentRequest";
    public static final String E_DELETE_XMPPCOMPONENT_RESPONSE = "DeleteXMPPComponentResponse";

    // rights
    public static final String E_GET_RIGHT_REQUEST = "GetRightRequest";
    public static final String E_GET_RIGHT_RESPONSE = "GetRightResponse";
    public static final String E_GET_ADMIN_CONSOLE_UI_COMP_REQUEST = "GetAdminConsoleUICompRequest";
    public static final String E_GET_ADMIN_CONSOLE_UI_COMP_RESPONSE = "GetAdminConsoleUICompResponse";
    public static final String E_GET_ALL_EFFECTIVE_RIGHTS_REQUEST = "GetAllEffectiveRightsRequest";
    public static final String E_GET_ALL_EFFECTIVE_RIGHTS_RESPONSE = "GetAllEffectiveRightsResponse";
    public static final String E_GET_ALL_RIGHTS_REQUEST = "GetAllRightsRequest";
    public static final String E_GET_ALL_RIGHTS_RESPONSE = "GetAllRightsResponse";
    public static final String E_GET_EFFECTIVE_RIGHTS_REQUEST = "GetEffectiveRightsRequest";
    public static final String E_GET_EFFECTIVE_RIGHTS_RESPONSE = "GetEffectiveRightsResponse";
    public static final String E_GET_CREATE_OBJECT_ATTRS_REQUEST = "GetCreateObjectAttrsRequest";
    public static final String E_GET_CREATE_OBJECT_ATTRS_RESPONSE = "GetCreateObjectAttrsResponse";
    public static final String E_GET_GRANTS_REQUEST = "GetGrantsRequest";
    public static final String E_GET_GRANTS_RESPONSE = "GetGrantsResponse";
    public static final String E_GET_RIGHTS_DOC_REQUEST = "GetRightsDocRequest";
    public static final String E_GET_RIGHTS_DOC_RESPONSE = "GetRightsDocResponse";
    public static final String E_GRANT_RIGHT_REQUEST = "GrantRightRequest";
    public static final String E_GRANT_RIGHT_RESPONSE = "GrantRightResponse";
    public static final String E_REVOKE_RIGHT_REQUEST = "RevokeRightRequest";
    public static final String E_REVOKE_RIGHT_RESPONSE = "RevokeRightResponse";
    public static final String E_CHECK_RIGHT_REQUEST = "CheckRightRequest";
    public static final String E_CHECK_RIGHT_RESPONSE = "CheckRightResponse";
    public static final String E_GET_DELEGATED_ADMIN_CONSTRAINTS_REQUEST = "GetDelegatedAdminConstraintsRequest";
    public static final String E_GET_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE = "GetDelegatedAdminConstraintsResponse";
    public static final String E_MODIFY_DELEGATED_ADMIN_CONSTRAINTS_REQUEST = "ModifyDelegatedAdminConstraintsRequest";
    public static final String E_MODIFY_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE = "ModifyDelegatedAdminConstraintsResponse";

    // Monitoring
    public static final String E_GET_SERVER_STATS_REQUEST = "GetServerStatsRequest";
    public static final String E_GET_SERVER_STATS_RESPONSE = "GetServerStatsResponse";

    public static final String E_GET_LOGGER_STATS_REQUEST = "GetLoggerStatsRequest";
    public static final String E_GET_LOGGER_STATS_RESPONSE = "GetLoggerStatsResponse";

    public static final String E_SYNC_GAL_ACCOUNT_REQUEST = "SyncGalAccountRequest";
    public static final String E_SYNC_GAL_ACCOUNT_RESPONSE = "SyncGalAccountResponse";

    // memcached
    public static final String E_RELOAD_MEMCACHED_CLIENT_CONFIG_REQUEST = "ReloadMemcachedClientConfigRequest";
    public static final String E_RELOAD_MEMCACHED_CLIENT_CONFIG_RESPONSE = "ReloadMemcachedClientConfigResponse";
    public static final String E_GET_MEMCACHED_CLIENT_CONFIG_REQUEST = "GetMemcachedClientConfigRequest";
    public static final String E_GET_MEMCACHED_CLIENT_CONFIG_RESPONSE = "GetMemcachedClientConfigResponse";

    // local config
    public static final String E_RELOAD_LOCAL_CONFIG_REQUEST = "ReloadLocalConfigRequest";
    public static final String E_RELOAD_LOCAL_CONFIG_RESPONSE = "ReloadLocalConfigResponse";

    // wiki migration
    public static final String E_MIGRATE_ACCOUNT_REQUEST = "MigrateAccountRequest";
    public static final String E_MIGRATE_ACCOUNT_RESPONSE = "MigrateAccountResponse";

    // noop
    public static final String E_NO_OP_REQUEST = "NoOpRequest";
    public static final String E_NO_OP_RESPONSE = "NoOpResponse";

    // cookie
    public static final String E_CLEAR_COOKIE_REQUEST = "ClearCookieRequest";
    public static final String E_CLEAR_COOKIE_RESPONSE = "ClearCookieResponse";
    public static final String E_REFRESH_REGISTERED_AUTHTOKENS_REQUEST = "RefreshRegisteredAuthTokensRequest";
    public static final String E_REFRESH_REGISTERED_AUTHTOKENS_RESPONSE = "RefreshRegisteredAuthTokensResponse";

    // SMIME config
    public static final String E_GET_SMIME_CONFIG_REQUEST = "GetSMIMEConfigRequest";
    public static final String E_GET_SMIME_CONFIG_RESPONSE = "GetSMIMEConfigResponse";
    public static final String E_MODIFY_SMIME_CONFIG_REQUEST = "ModifySMIMEConfigRequest";
    public static final String E_MODIFY_SMIME_CONFIG_RESPONSE = "ModifySMIMEConfigResponse";

    // Version Check
    public static final String E_VC_REQUEST = "VersionCheckRequest";
    public static final String E_VC_RESPONSE = "VersionCheckResponse";

    // LicenseAdminService
    public static final String E_INSTALL_LICENSE_REQUEST = "InstallLicenseRequest";
    public static final String E_INSTALL_LICENSE_RESPONSE = "InstallLicenseResponse";
    public static final String E_ACTIVATE_LICENSE_REQUEST = "ActivateLicenseRequest";
    public static final String E_ACTIVATE_LICENSE_RESPONSE = "ActivateLicenseResponse";
    public static final String E_GET_LICENSE_REQUEST = "GetLicenseRequest";
    public static final String E_GET_LICENSE_RESPONSE = "GetLicenseResponse";

    // Auto Provision
    public static final String E_AUTO_PROV_ACCOUNT_REQUEST = "AutoProvAccountRequest";
    public static final String E_AUTO_PROV_ACCOUNT_RESPONSE = "AutoProvAccountResponse";
    public static final String E_AUTO_PROV_TASK_CONTROL_REQUEST = "AutoProvTaskControlRequest";
    public static final String E_AUTO_PROV_TASK_CONTROL_RESPONSE = "AutoProvTaskControlResponse";
    public static final String E_SEARCH_AUTO_PROV_DIRECTORY_REQUEST = "SearchAutoProvDirectoryRequest";
    public static final String E_SEARCH_AUTO_PROV_DIRECTORY_RESPONSE = "SearchAutoProvDirectoryResponse";

    // Retention policy
    public static final String E_GET_SYSTEM_RETENTION_POLICY_REQUEST = "GetSystemRetentionPolicyRequest";
    public static final String E_GET_SYSTEM_RETENTION_POLICY_RESPONSE = "GetSystemRetentionPolicyResponse";
    public static final String E_CREATE_SYSTEM_RETENTION_POLICY_REQUEST = "CreateSystemRetentionPolicyRequest";
    public static final String E_CREATE_SYSTEM_RETENTION_POLICY_RESPONSE = "CreateSystemRetentionPolicyResponse";
    public static final String E_MODIFY_SYSTEM_RETENTION_POLICY_REQUEST = "ModifySystemRetentionPolicyRequest";
    public static final String E_MODIFY_SYSTEM_RETENTION_POLICY_RESPONSE = "ModifySystemRetentionPolicyResponse";
    public static final String E_DELETE_SYSTEM_RETENTION_POLICY_REQUEST = "DeleteSystemRetentionPolicyRequest";
    public static final String E_DELETE_SYSTEM_RETENTION_POLICY_RESPONSE = "DeleteSystemRetentionPolicyResponse";

    // StoreManager verification utility
    public static final String E_VERIFY_STORE_MANAGER_REQUEST = "VerifyStoreManagerRequest";
    public static final String E_VERIFY_STORE_MANAGER_RESPONSE = "VerifyStoreManagerResponse";

    // Two-Factor Authentication
    public static final String E_CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST = "ClearTwoFactorAuthDataRequest";
    public static final String E_CLEAR_TWO_FACTOR_AUTH_DATA_RESPONSE = "ClearTwoFactorAuthDataResponse";
    public static final String E_GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_REQUEST = "GetClearTwoFactorAuthDataStatusRequest";
    public static final String E_GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_RESPONSE = "GetClearTwoFactorAuthDataStatusResponse";

    // Skins
    // Skins
    public static final String E_GET_ALL_SKINS_REQUEST = "GetAllSkinsRequest";
    public static final String E_GET_ALL_SKINS_RESPONSE = "GetAllSkinsResponse";
    public static final QName PING_REQUEST = QName.get(E_PING_REQUEST, NAMESPACE);
    public static final QName PING_RESPONSE = QName.get(E_PING_RESPONSE, NAMESPACE);
    public static final QName CHECK_HEALTH_REQUEST = QName.get(E_CHECK_HEALTH_REQUEST, NAMESPACE);
    public static final QName CHECK_HEALTH_RESPONSE = QName.get(E_CHECK_HEALTH_RESPONSE, NAMESPACE);

    public static final QName GET_ALL_LOCALES_REQUEST = QName.get(E_GET_ALL_LOCALES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_LOCALES_RESPONSE = QName.get(E_GET_ALL_LOCALES_RESPONSE, NAMESPACE);

    public static final QName EXPORTMAILBOX_REQUEST = QName.get(E_EXPORTMAILBOX_REQUEST, NAMESPACE);
    public static final QName EXPORTMAILBOX_RESPONSE = QName.get(E_EXPORTMAILBOX_RESPONSE, NAMESPACE);

    public static final QName AUTH_REQUEST = QName.get(E_AUTH_REQUEST, NAMESPACE);
    public static final QName AUTH_RESPONSE = QName.get(E_AUTH_RESPONSE, NAMESPACE);
    public static final QName CREATE_ACCOUNT_REQUEST = QName.get(E_CREATE_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName CREATE_ACCOUNT_RESPONSE = QName.get(E_CREATE_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName CREATE_GAL_SYNC_ACCOUNT_REQUEST = QName.get(E_CREATE_GAL_SYNC_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName CREATE_GAL_SYNC_ACCOUNT_RESPONSE = QName.get(E_CREATE_GAL_SYNC_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName ADD_GAL_SYNC_DATASOURCE_REQUEST = QName.get(E_ADD_GAL_SYNC_DATASOURCE_REQUEST, NAMESPACE);
    public static final QName ADD_GAL_SYNC_DATASOURCE_RESPONSE = QName.get(E_ADD_GAL_SYNC_DATASOURCE_RESPONSE, NAMESPACE);
    public static final QName DELEGATE_AUTH_REQUEST = QName.get(E_DELEGATE_AUTH_REQUEST, NAMESPACE);
    public static final QName DELEGATE_AUTH_RESPONSE = QName.get(E_DELEGATE_AUTH_RESPONSE, NAMESPACE);
    public static final QName DELETE_GAL_SYNC_ACCOUNT_REQUEST = QName.get(E_DELETE_GAL_SYNC_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName DELETE_GAL_SYNC_ACCOUNT_RESPONSE = QName.get(E_DELETE_GAL_SYNC_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName GET_ACCOUNT_REQUEST = QName.get(E_GET_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName GET_ACCOUNT_RESPONSE = QName.get(E_GET_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_REQUEST = QName.get(E_GET_ACCOUNT_INFO_REQUEST, NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_RESPONSE = QName.get(E_GET_ACCOUNT_INFO_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_ACCOUNTS_REQUEST = QName.get(E_GET_ALL_ACCOUNTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ACCOUNTS_RESPONSE = QName.get(E_GET_ALL_ACCOUNTS_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_ADMIN_ACCOUNTS_REQUEST = QName.get(E_GET_ALL_ADMIN_ACCOUNTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ADMIN_ACCOUNTS_RESPONSE = QName.get(E_GET_ALL_ADMIN_ACCOUNTS_RESPONSE, NAMESPACE);
    public static final QName MODIFY_ACCOUNT_REQUEST = QName.get(E_MODIFY_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName MODIFY_ACCOUNT_RESPONSE = QName.get(E_MODIFY_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName DELETE_ACCOUNT_REQUEST = QName.get(E_DELETE_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName DELETE_ACCOUNT_RESPONSE = QName.get(E_DELETE_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName SET_PASSWORD_REQUEST = QName.get(E_SET_PASSWORD_REQUEST, NAMESPACE);
    public static final QName SET_PASSWORD_RESPONSE = QName.get(E_SET_PASSWORD_RESPONSE, NAMESPACE);
    public static final QName CHECK_PASSWORD_STRENGTH_REQUEST = QName.get(E_CHECK_PASSWORD_STRENGTH_REQUEST, NAMESPACE);
    public static final QName CHECK_PASSWORD_STRENGTH_RESPONSE = QName.get(E_CHECK_PASSWORD_STRENGTH_RESPONSE, NAMESPACE);
    public static final QName RESET_ACCOUNT_PASSWORD_REQUEST = QName.get(E_RESET_ACCOUNT_PASSWORD_REQUEST, NAMESPACE);
    public static final QName RESET_ACCOUNT_PASSWORD_RESPONSE = QName.get(E_RESET_ACCOUNT_PASSWORD_RESPONSE, NAMESPACE);

    public static final QName ADD_ACCOUNT_ALIAS_REQUEST = QName.get(E_ADD_ACCOUNT_ALIAS_REQUEST, NAMESPACE);
    public static final QName ADD_ACCOUNT_ALIAS_RESPONSE = QName.get(E_ADD_ACCOUNT_ALIAS_RESPONSE, NAMESPACE);
    public static final QName REMOVE_ACCOUNT_ALIAS_REQUEST = QName.get(E_REMOVE_ACCOUNT_ALIAS_REQUEST, NAMESPACE);
    public static final QName REMOVE_ACCOUNT_ALIAS_RESPONSE = QName.get(E_REMOVE_ACCOUNT_ALIAS_RESPONSE, NAMESPACE);
    public static final QName SEARCH_ACCOUNTS_REQUEST = QName.get(E_SEARCH_ACCOUNTS_REQUEST, NAMESPACE);
    public static final QName SEARCH_ACCOUNTS_RESPONSE = QName.get(E_SEARCH_ACCOUNTS_RESPONSE, NAMESPACE);
    public static final QName RENAME_ACCOUNT_REQUEST = QName.get(E_RENAME_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName RENAME_ACCOUNT_RESPONSE = QName.get(E_RENAME_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName CHANGE_PRIMARY_EMAIL_REQUEST = QName.get(E_CHANGE_PRIMARY_EMAIL_REQUEST, NAMESPACE);
    public static final QName CHANGE_PRIMARY_EMAIL_RESPONSE = QName.get(E_CHANGE_PRIMARY_EMAIL_RESPONSE, NAMESPACE);

    public static final QName CREATE_DOMAIN_REQUEST = QName.get(E_CREATE_DOMAIN_REQUEST, NAMESPACE);
    public static final QName CREATE_DOMAIN_RESPONSE = QName.get(E_CREATE_DOMAIN_RESPONSE, NAMESPACE);
    public static final QName GET_DOMAIN_REQUEST = QName.get(E_GET_DOMAIN_REQUEST, NAMESPACE);
    public static final QName GET_DOMAIN_RESPONSE = QName.get(E_GET_DOMAIN_RESPONSE, NAMESPACE);
    public static final QName GET_DOMAIN_INFO_REQUEST = QName.get(E_GET_DOMAIN_INFO_REQUEST, NAMESPACE);
    public static final QName GET_DOMAIN_INFO_RESPONSE = QName.get(E_GET_DOMAIN_INFO_RESPONSE, NAMESPACE);
    public static final QName MODIFY_DOMAIN_REQUEST = QName.get(E_MODIFY_DOMAIN_REQUEST, NAMESPACE);
    public static final QName MODIFY_DOMAIN_RESPONSE = QName.get(E_MODIFY_DOMAIN_RESPONSE, NAMESPACE);
    public static final QName DELETE_DOMAIN_REQUEST = QName.get(E_DELETE_DOMAIN_REQUEST, NAMESPACE);
    public static final QName DELETE_DOMAIN_RESPONSE = QName.get(E_DELETE_DOMAIN_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_DOMAINS_REQUEST = QName.get(E_GET_ALL_DOMAINS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_DOMAINS_RESPONSE = QName.get(E_GET_ALL_DOMAINS_RESPONSE, NAMESPACE);

    public static final QName CREATE_COS_REQUEST = QName.get(E_CREATE_COS_REQUEST, NAMESPACE);
    public static final QName CREATE_COS_RESPONSE = QName.get(E_CREATE_COS_RESPONSE, NAMESPACE);
    public static final QName COPY_COS_REQUEST = QName.get(E_COPY_COS_REQUEST, NAMESPACE);
    public static final QName COPY_COS_RESPONSE = QName.get(E_COPY_COS_RESPONSE, NAMESPACE);
    public static final QName GET_COS_REQUEST = QName.get(E_GET_COS_REQUEST, NAMESPACE);
    public static final QName GET_COS_RESPONSE = QName.get(E_GET_COS_RESPONSE, NAMESPACE);
    public static final QName MODIFY_COS_REQUEST = QName.get(E_MODIFY_COS_REQUEST, NAMESPACE);
    public static final QName MODIFY_COS_RESPONSE = QName.get(E_MODIFY_COS_RESPONSE, NAMESPACE);
    public static final QName DELETE_COS_REQUEST = QName.get(E_DELETE_COS_REQUEST, NAMESPACE);
    public static final QName DELETE_COS_RESPONSE = QName.get(E_DELETE_COS_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_COS_REQUEST = QName.get(E_GET_ALL_COS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_COS_RESPONSE = QName.get(E_GET_ALL_COS_RESPONSE, NAMESPACE);
    public static final QName RENAME_COS_REQUEST = QName.get(E_RENAME_COS_REQUEST, NAMESPACE);
    public static final QName RENAME_COS_RESPONSE = QName.get(E_RENAME_COS_RESPONSE, NAMESPACE);

    public static final QName CREATE_SERVER_REQUEST = QName.get(E_CREATE_SERVER_REQUEST, NAMESPACE);
    public static final QName CREATE_SERVER_RESPONSE = QName.get(E_CREATE_SERVER_RESPONSE, NAMESPACE);
    public static final QName GET_SERVER_REQUEST = QName.get(E_GET_SERVER_REQUEST, NAMESPACE);
    public static final QName GET_SERVER_RESPONSE = QName.get(E_GET_SERVER_RESPONSE, NAMESPACE);

    public static final QName GET_SERVER_NIFS_REQUEST = QName.get(E_GET_SERVER_NIFS_REQUEST, NAMESPACE);
    public static final QName GET_SERVER_NIFS_RESPONSE = QName.get(E_GET_SERVER_NIFS_RESPONSE, NAMESPACE);

    public static final QName MODIFY_SERVER_REQUEST = QName.get(E_MODIFY_SERVER_REQUEST, NAMESPACE);
    public static final QName MODIFY_SERVER_RESPONSE = QName.get(E_MODIFY_SERVER_RESPONSE, NAMESPACE);
    public static final QName DELETE_SERVER_REQUEST = QName.get(E_DELETE_SERVER_REQUEST, NAMESPACE);
    public static final QName DELETE_SERVER_RESPONSE = QName.get(E_DELETE_SERVER_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_SERVERS_REQUEST = QName.get(E_GET_ALL_SERVERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_SERVERS_RESPONSE = QName.get(E_GET_ALL_SERVERS_RESPONSE, NAMESPACE);

    public static final QName CREATE_ALWAYSONCLUSTER_REQUEST = QName.get(E_CREATE_ALWAYSONCLUSTER_REQUEST, NAMESPACE);
    public static final QName CREATE_ALWAYSONCLUSTER_RESPONSE = QName.get(E_CREATE_ALWAYSONCLUSTER_RESPONSE, NAMESPACE);
    public static final QName GET_ALWAYSONCLUSTER_REQUEST = QName.get(E_GET_ALWAYSONCLUSTER_REQUEST, NAMESPACE);
    public static final QName GET_ALWAYSONCLUSTER_RESPONSE = QName.get(E_GET_ALWAYSONCLUSTER_RESPONSE, NAMESPACE);

    public static final QName MODIFY_ALWAYSONCLUSTER_REQUEST = QName.get(E_MODIFY_ALWAYSONCLUSTER_REQUEST, NAMESPACE);
    public static final QName MODIFY_ALWAYSONCLUSTER_RESPONSE = QName.get(E_MODIFY_ALWAYSONCLUSTER_RESPONSE, NAMESPACE);
    public static final QName DELETE_ALWAYSONCLUSTER_REQUEST = QName.get(E_DELETE_ALWAYSONCLUSTER_REQUEST, NAMESPACE);
    public static final QName DELETE_ALWAYSONCLUSTER_RESPONSE = QName.get(E_DELETE_ALWAYSONCLUSTER_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_ALWAYSONCLUSTERS_REQUEST = QName.get(E_GET_ALL_ALWAYSONCLUSTERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ALWAYSONCLUSTERS_RESPONSE = QName.get(E_GET_ALL_ALWAYSONCLUSTERS_RESPONSE, NAMESPACE);

    public static final QName CREATE_UC_SERVICE_REQUEST = QName.get(E_CREATE_UC_SERVICE_REQUEST, NAMESPACE);
    public static final QName CREATE_UC_SERVICE_RESPONSE = QName.get(E_CREATE_UC_SERVICE_RESPONSE, NAMESPACE);
    public static final QName DELETE_UC_SERVICE_REQUEST = QName.get(E_DELETE_UC_SERVICE_REQUEST, NAMESPACE);
    public static final QName DELETE_UC_SERVICE_RESPONSE = QName.get(E_DELETE_UC_SERVICE_RESPONSE, NAMESPACE);
    public static final QName GET_UC_SERVICE_REQUEST = QName.get(E_GET_UC_SERVICE_REQUEST, NAMESPACE);
    public static final QName GET_UC_SERVICE_RESPONSE = QName.get(E_GET_UC_SERVICE_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_UC_SERVICES_REQUEST = QName.get(E_GET_ALL_UC_SERVICES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_UC_SERVICES_RESPONSE = QName.get(E_GET_ALL_UC_SERVICES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_UC_SERVICE_REQUEST = QName.get(E_MODIFY_UC_SERVICE_REQUEST, NAMESPACE);
    public static final QName MODIFY_UC_SERVICE_RESPONSE = QName.get(E_MODIFY_UC_SERVICE_RESPONSE, NAMESPACE);
    public static final QName RENAME_UC_SERVICE_REQUEST = QName.get(E_RENAME_UC_SERVICE_REQUEST, NAMESPACE);
    public static final QName RENAME_UC_SERVICE_RESPONSE = QName.get(E_RENAME_UC_SERVICE_RESPONSE, NAMESPACE);

    public static final QName GET_CONFIG_REQUEST = QName.get(E_GET_CONFIG_REQUEST, NAMESPACE);
    public static final QName GET_CONFIG_RESPONSE = QName.get(E_GET_CONFIG_RESPONSE, NAMESPACE);
    public static final QName MODIFY_CONFIG_REQUEST = QName.get(E_MODIFY_CONFIG_REQUEST, NAMESPACE);
    public static final QName MODIFY_CONFIG_RESPONSE = QName.get(E_MODIFY_CONFIG_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_CONFIG_REQUEST = QName.get(E_GET_ALL_CONFIG_REQUEST, NAMESPACE);
    public static final QName GET_ALL_CONFIG_RESPONSE = QName.get(E_GET_ALL_CONFIG_RESPONSE, NAMESPACE);

    public static final QName GET_SERVICE_STATUS_REQUEST = QName.get(E_GET_SERVICE_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_SERVICE_STATUS_RESPONSE = QName.get(E_GET_SERVICE_STATUS_RESPONSE, NAMESPACE);

    public static final QName PURGE_MESSAGES_REQUEST = QName.get(E_PURGE_MESSAGES_REQUEST, NAMESPACE);
    public static final QName PURGE_MESSAGES_RESPONSE = QName.get(E_PURGE_MESSAGES_RESPONSE, NAMESPACE);
    public static final QName DELETE_MAILBOX_REQUEST = QName.get(E_DELETE_MAILBOX_REQUEST, NAMESPACE);
    public static final QName DELETE_MAILBOX_RESPONSE = QName.get(E_DELETE_MAILBOX_RESPONSE, NAMESPACE);
    public static final QName GET_MAILBOX_REQUEST = QName.get(E_GET_MAILBOX_REQUEST, NAMESPACE);
    public static final QName GET_MAILBOX_RESPONSE = QName.get(E_GET_MAILBOX_RESPONSE, NAMESPACE);
    public static final QName LOCKOUT_MAILBOX_REQUEST = QName.get(E_LOCKOUT_MAILBOX_REQUEST, NAMESPACE);
    public static final QName LOCKOUT_MAILBOX_RESPONSE = QName.get(E_LOCKOUT_MAILBOX_RESPONSE, NAMESPACE);

    public static final QName RUN_UNIT_TESTS_REQUEST = QName.get(E_RUN_UNIT_TESTS_REQUEST, NAMESPACE);
    public static final QName RUN_UNIT_TESTS_RESPONSE = QName.get(E_RUN_UNIT_TESTS_RESPONSE, NAMESPACE);

    public static final QName CHECK_HOSTNAME_RESOLVE_REQUEST = QName.get(E_CHECK_HOSTNAME_RESOLVE_REQUEST, NAMESPACE);
    public static final QName CHECK_HOSTNAME_RESOLVE_RESPONSE = QName.get(E_CHECK_HOSTNAME_RESOLVE_RESPONSE, NAMESPACE);
    public static final QName CHECK_AUTH_CONFIG_REQUEST = QName.get(E_CHECK_AUTH_CONFIG_REQUEST, NAMESPACE);
    public static final QName CHECK_AUTH_CONFIG_RESPONSE = QName.get(E_CHECK_AUTH_CONFIG_RESPONSE, NAMESPACE);
    public static final QName CHECK_GAL_CONFIG_REQUEST = QName.get(E_CHECK_GAL_CONFIG_REQUEST, NAMESPACE);
    public static final QName CHECK_GAL_CONFIG_RESPONSE = QName.get(E_CHECK_GAL_CONFIG_RESPONSE, NAMESPACE);
    public static final QName CHECK_EXCHANGE_AUTH_REQUEST = QName.get(E_CHECK_EXCHANGE_AUTH_REQUEST, NAMESPACE);
    public static final QName CHECK_EXCHANGE_AUTH_RESPONSE = QName.get(E_CHECK_EXCHANGE_AUTH_RESPONSE, NAMESPACE);
    public static final QName CHECK_DOMAIN_MX_RECORD_REQUEST = QName.get(E_CHECK_DOMAIN_MX_RECORD_REQUEST, NAMESPACE);
    public static final QName CHECK_DOMAIN_MX_RECORD_RESPONSE = QName.get(E_CHECK_DOMAIN_MX_RECORD_RESPONSE, NAMESPACE);

    public static final QName AUTO_COMPLETE_GAL_REQUEST = QName.get(E_AUTO_COMPLETE_GAL_REQUEST, NAMESPACE);
    public static final QName AUTO_COMPLETE_GAL_RESPONSE = QName.get(E_AUTO_COMPLETE_GAL_RESPONSE, NAMESPACE);
    public static final QName SEARCH_GAL_REQUEST = QName.get(E_SEARCH_GAL_REQUEST, NAMESPACE);
    public static final QName SEARCH_GAL_RESPONSE = QName.get(E_SEARCH_GAL_RESPONSE, NAMESPACE);

    public static final QName CREATE_VOLUME_REQUEST = QName.get(E_CREATE_VOLUME_REQUEST, NAMESPACE);
    public static final QName CREATE_VOLUME_RESPONSE = QName.get(E_CREATE_VOLUME_RESPONSE, NAMESPACE);
    public static final QName GET_VOLUME_REQUEST = QName.get(E_GET_VOLUME_REQUEST, NAMESPACE);
    public static final QName GET_VOLUME_RESPONSE = QName.get(E_GET_VOLUME_RESPONSE, NAMESPACE);
    public static final QName MODIFY_VOLUME_REQUEST = QName.get(E_MODIFY_VOLUME_REQUEST, NAMESPACE);
    public static final QName MODIFY_VOLUME_RESPONSE = QName.get(E_MODIFY_VOLUME_RESPONSE, NAMESPACE);
    public static final QName MODIFY_VOLUME_INPLACE_UPGRADE_REQUEST = QName.get(E_MODIFY_VOLUME_INPLACE_UPGRADE_REQUEST, NAMESPACE);
    public static final QName MODIFY_VOLUME_INPLACE_UPGRADE_RESPONSE = QName.get(E_MODIFY_VOLUME_INPLACE_UPGRADE_RESPONSE, NAMESPACE);
    public static final QName DELETE_VOLUME_REQUEST = QName.get(E_DELETE_VOLUME_REQUEST, NAMESPACE);
    public static final QName DELETE_VOLUME_RESPONSE = QName.get(E_DELETE_VOLUME_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_VOLUMES_REQUEST = QName.get(E_GET_ALL_VOLUMES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_VOLUMES_RESPONSE = QName.get(E_GET_ALL_VOLUMES_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_VOLUMES_INPLACE_UPGRADE_REQUEST = QName.get(E_GET_ALL_VOLUMES_INPLACE_UPGRADE_REQUEST, NAMESPACE);
    public static final QName GET_ALL_VOLUMES_INPLACE_UPGRADE_RESPONSE = QName.get(E_GET_ALL_VOLUMES_INPLACE_UPGRADE_RESPONSE, NAMESPACE);
    public static final QName GET_CURRENT_VOLUMES_REQUEST = QName.get(E_GET_CURRENT_VOLUMES_REQUEST, NAMESPACE);
    public static final QName GET_CURRENT_VOLUMES_RESPONSE = QName.get(E_GET_CURRENT_VOLUMES_RESPONSE, NAMESPACE);
    public static final QName SET_CURRENT_VOLUME_REQUEST = QName.get(E_SET_CURRENT_VOLUME_REQUEST, NAMESPACE);
    public static final QName SET_CURRENT_VOLUME_RESPONSE = QName.get(E_SET_CURRENT_VOLUME_RESPONSE, NAMESPACE);
    public static final QName CHECK_BLOB_CONSISTENCY_REQUEST = QName.get(E_CHECK_BLOB_CONSISTENCY_REQUEST, NAMESPACE);
    public static final QName CHECK_BLOB_CONSISTENCY_RESPONSE = QName.get(E_CHECK_BLOB_CONSISTENCY_RESPONSE, NAMESPACE);
    public static final QName EXPORT_AND_DELETE_ITEMS_REQUEST = QName.get(E_EXPORT_AND_DELETE_ITEMS_REQUEST, NAMESPACE);
    public static final QName EXPORT_AND_DELETE_ITEMS_RESPONSE = QName.get(E_EXPORT_AND_DELETE_ITEMS_RESPONSE, NAMESPACE);
    public static final QName DEDUPE_BLOBS_REQUEST = QName.get(E_DEDUPE_BLOBS_REQUEST, NAMESPACE);
    public static final QName DEDUPE_BLOBS_RESPONSE = QName.get(E_DEDUPE_BLOBS_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_ACTIVE_SERVERS_REQUEST = QName.get(E_GET_ALL_ACTIVE_SERVERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ACTIVE_SERVERS_RESPONSE = QName.get(E_GET_ALL_ACTIVE_SERVERS_RESPONSE, NAMESPACE);

    public static final QName SET_SERVER_OFFLINE_REQUEST = QName.get(E_SET_SERVER_OFFLINE_REQUEST, NAMESPACE);
    public static final QName SET_SERVER_OFFLINE_RESPONSE = QName.get(E_SET_SERVER_OFFLINE_RESPONSE, NAMESPACE);
    public static final QName SET_LOCAL_SERVER_ONLINE_REQUEST = QName.get(E_SET_LOCAL_SERVER_ONLINE_REQUEST, NAMESPACE);
    public static final QName SET_LOCAL_SERVER_ONLINE_RESPONSE = QName.get(E_SET_LOCAL_SERVER_ONLINE_RESPONSE, NAMESPACE);

    public static final QName CREATE_DISTRIBUTION_LIST_REQUEST = QName.get(E_CREATE_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName CREATE_DISTRIBUTION_LIST_RESPONSE = QName.get(E_CREATE_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_REQUEST = QName.get(E_GET_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_RESPONSE = QName.get(E_GET_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_DISTRIBUTION_LISTS_REQUEST = QName.get(E_GET_ALL_DISTRIBUTION_LISTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_DISTRIBUTION_LISTS_RESPONSE = QName.get(E_GET_ALL_DISTRIBUTION_LISTS_RESPONSE, NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_MEMBER_REQUEST = QName.get(E_ADD_DISTRIBUTION_LIST_MEMBER_REQUEST, NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_MEMBER_RESPONSE = QName.get(E_ADD_DISTRIBUTION_LIST_MEMBER_RESPONSE, NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST = QName.get(E_REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST, NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_MEMBER_RESPONSE = QName.get(E_REMOVE_DISTRIBUTION_LIST_MEMBER_RESPONSE, NAMESPACE);
    public static final QName MODIFY_DISTRIBUTION_LIST_REQUEST = QName.get(E_MODIFY_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName MODIFY_DISTRIBUTION_LIST_RESPONSE = QName.get(E_MODIFY_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);
    public static final QName DELETE_DISTRIBUTION_LIST_REQUEST = QName.get(E_DELETE_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName DELETE_DISTRIBUTION_LIST_RESPONSE = QName.get(E_DELETE_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_ALIAS_REQUEST = QName.get(E_ADD_DISTRIBUTION_LIST_ALIAS_REQUEST, NAMESPACE);
    public static final QName ADD_DISTRIBUTION_LIST_ALIAS_RESPONSE = QName.get(E_ADD_DISTRIBUTION_LIST_ALIAS_RESPONSE, NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST = QName.get(E_REMOVE_DISTRIBUTION_LIST_ALIAS_REQUEST, NAMESPACE);
    public static final QName REMOVE_DISTRIBUTION_LIST_ALIAS_RESPONSE = QName.get(E_REMOVE_DISTRIBUTION_LIST_ALIAS_RESPONSE, NAMESPACE);
    public static final QName RENAME_DISTRIBUTION_LIST_REQUEST = QName.get(E_RENAME_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName RENAME_DISTRIBUTION_LIST_RESPONSE = QName.get(E_RENAME_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);

    public static final QName CREATE_HAB_GROUP_REQUEST = QName.get(E_CREATE_HAB_GROUP_REQUEST, NAMESPACE);
    public static final QName CREATE_HAB_GROUP_RESPONSE = QName.get(E_CREATE_HAB_GROUP_RESPONSE, NAMESPACE);
    public static final QName MODIFY_HAB_GROUP_REQUEST = QName.get(E_MODIFY_HAB_GROUP_REQUEST, NAMESPACE);
    public static final QName MODIFY_HAB_GROUP_RESPONSE = QName.get(E_MODIFY_HAB_GROUP_RESPONSE, NAMESPACE);

    public static final QName GET_VERSION_INFO_REQUEST = QName.get(E_GET_VERSION_INFO_REQUEST, NAMESPACE);
    public static final QName GET_VERSION_INFO_RESPONSE = QName.get(E_GET_VERSION_INFO_RESPONSE, NAMESPACE);

    public static final QName GET_LICENSE_INFO_REQUEST = QName.get(E_GET_LICENSE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_LICENSE_INFO_RESPONSE = QName.get(E_GET_LICENSE_INFO_RESPONSE, NAMESPACE);

    public static final QName GET_ATTRIBUTE_INFO_REQUEST = QName.get(E_GET_ATTRIBUTE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_ATTRIBUTE_INFO_RESPONSE = QName.get(E_GET_ATTRIBUTE_INFO_RESPONSE, NAMESPACE);

    public static final QName REINDEX_REQUEST = QName.get(E_REINDEX_REQUEST, NAMESPACE);
    public static final QName REINDEX_RESPONSE = QName.get(E_REINDEX_RESPONSE, NAMESPACE);
    public static final QName MANAGE_INDEX_REQUEST = QName.get(E_MANAGE_INDEX_REQUEST, NAMESPACE);
    public static final QName MANAGE_INDEX_RESPONSE = QName.get(E_MANAGE_INDEX_RESPONSE, NAMESPACE);
    public static final QName COMPACT_INDEX_REQUEST = QName.get(E_COMPACT_INDEX_REQUEST, NAMESPACE);
    public static final QName COMPACT_INDEX_RESPONSE = QName.get(E_COMPACT_INDEX_RESPONSE, NAMESPACE);
    public static final QName GET_INDEX_STATS_REQUEST = QName.get(E_GET_INDEX_STATS_REQUEST, NAMESPACE);
    public static final QName GET_INDEX_STATS_RESPONSE = QName.get(E_GET_INDEX_STATS_RESPONSE, NAMESPACE);
    public static final QName VERIFY_INDEX_REQUEST = QName.get(E_VERIFY_INDEX_REQUEST, NAMESPACE);
    public static final QName VERIFY_INDEX_RESPONSE = QName.get(E_VERIFY_INDEX_RESPONSE, NAMESPACE);
    public static final QName RECALCULATE_MAILBOX_COUNTS_REQUEST = QName.get(E_RECALCULATE_MAILBOX_COUNTS_REQUEST, NAMESPACE);
    public static final QName RECALCULATE_MAILBOX_COUNTS_RESPONSE = QName.get(E_RECALCULATE_MAILBOX_COUNTS_RESPONSE, NAMESPACE);

    public static final QName GET_ZIMLET_REQUEST = QName.get(E_GET_ZIMLET_REQUEST, NAMESPACE);
    public static final QName GET_ZIMLET_RESPONSE = QName.get(E_GET_ZIMLET_RESPONSE, NAMESPACE);
    public static final QName CREATE_ZIMLET_REQUEST = QName.get(E_CREATE_ZIMLET_REQUEST, NAMESPACE);
    public static final QName CREATE_ZIMLET_RESPONSE = QName.get(E_CREATE_ZIMLET_RESPONSE, NAMESPACE);
    public static final QName DELETE_ZIMLET_REQUEST = QName.get(E_DELETE_ZIMLET_REQUEST, NAMESPACE);
    public static final QName DELETE_ZIMLET_RESPONSE = QName.get(E_DELETE_ZIMLET_RESPONSE, NAMESPACE);
    public static final QName GET_ADMIN_EXTENSION_ZIMLETS_REQUEST = QName.get(E_GET_ADMIN_EXTENSION_ZIMLETS_REQUEST, NAMESPACE);
    public static final QName GET_ADMIN_EXTENSION_ZIMLETS_RESPONSE = QName.get(E_GET_ADMIN_EXTENSION_ZIMLETS_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_ZIMLETS_REQUEST = QName.get(E_GET_ALL_ZIMLETS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ZIMLETS_RESPONSE = QName.get(E_GET_ALL_ZIMLETS_RESPONSE, NAMESPACE);
    public static final QName GET_ZIMLET_STATUS_REQUEST = QName.get(E_GET_ZIMLET_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_ZIMLET_STATUS_RESPONSE = QName.get(E_GET_ZIMLET_STATUS_RESPONSE, NAMESPACE);
    public static final QName DEPLOY_ZIMLET_REQUEST = QName.get(E_DEPLOY_ZIMLET_REQUEST, NAMESPACE);
    public static final QName DEPLOY_ZIMLET_RESPONSE = QName.get(E_DEPLOY_ZIMLET_RESPONSE, NAMESPACE);
    public static final QName UNDEPLOY_ZIMLET_REQUEST = QName.get(E_UNDEPLOY_ZIMLET_REQUEST, NAMESPACE);
    public static final QName UNDEPLOY_ZIMLET_RESPONSE = QName.get(E_UNDEPLOY_ZIMLET_RESPONSE, NAMESPACE);
    public static final QName CONFIGURE_ZIMLET_REQUEST = QName.get(E_CONFIGURE_ZIMLET_REQUEST, NAMESPACE);
    public static final QName CONFIGURE_ZIMLET_RESPONSE = QName.get(E_CONFIGURE_ZIMLET_RESPONSE, NAMESPACE);
    public static final QName MODIFY_ZIMLET_REQUEST = QName.get(E_MODIFY_ZIMLET_REQUEST, NAMESPACE);
    public static final QName MODIFY_ZIMLET_RESPONSE = QName.get(E_MODIFY_ZIMLET_RESPONSE, NAMESPACE);

    public static final QName CREATE_CALENDAR_RESOURCE_REQUEST = QName.get(E_CREATE_CALENDAR_RESOURCE_REQUEST, NAMESPACE);
    public static final QName CREATE_CALENDAR_RESOURCE_RESPONSE = QName.get(E_CREATE_CALENDAR_RESOURCE_RESPONSE, NAMESPACE);
    public static final QName DELETE_CALENDAR_RESOURCE_REQUEST = QName.get(E_DELETE_CALENDAR_RESOURCE_REQUEST, NAMESPACE);
    public static final QName DELETE_CALENDAR_RESOURCE_RESPONSE = QName.get(E_DELETE_CALENDAR_RESOURCE_RESPONSE, NAMESPACE);
    public static final QName MODIFY_CALENDAR_RESOURCE_REQUEST = QName.get(E_MODIFY_CALENDAR_RESOURCE_REQUEST, NAMESPACE);
    public static final QName MODIFY_CALENDAR_RESOURCE_RESPONSE = QName.get(E_MODIFY_CALENDAR_RESOURCE_RESPONSE, NAMESPACE);
    public static final QName RENAME_CALENDAR_RESOURCE_REQUEST = QName.get(E_RENAME_CALENDAR_RESOURCE_REQUEST, NAMESPACE);
    public static final QName RENAME_CALENDAR_RESOURCE_RESPONSE = QName.get(E_RENAME_CALENDAR_RESOURCE_RESPONSE, NAMESPACE);
    public static final QName GET_CALENDAR_RESOURCE_REQUEST = QName.get(E_GET_CALENDAR_RESOURCE_REQUEST, NAMESPACE);
    public static final QName GET_CALENDAR_RESOURCE_RESPONSE = QName.get(E_GET_CALENDAR_RESOURCE_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_CALENDAR_RESOURCES_REQUEST = QName.get(E_GET_ALL_CALENDAR_RESOURCES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_CALENDAR_RESOURCES_RESPONSE = QName.get(E_GET_ALL_CALENDAR_RESOURCES_RESPONSE, NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_REQUEST = QName.get(E_SEARCH_CALENDAR_RESOURCES_REQUEST, NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_RESPONSE = QName.get(E_SEARCH_CALENDAR_RESOURCES_RESPONSE, NAMESPACE);

    public static final QName SEARCH_MULTIPLE_MAILBOXES_REQUEST = QName.get(E_SEARCH_MULTIPLE_MAILBOXES_REQUEST, NAMESPACE);
    public static final QName SEARCH_MULTIPLE_MAILBOXES_RESPONSE = QName.get(E_SEARCH_MULTIPLE_MAILBOXES_RESPONSE, NAMESPACE);

    public static final QName DUMP_SESSIONS_REQUEST = QName.get(E_DUMP_SESSIONS_REQUEST, NAMESPACE);
    public static final QName DUMP_SESSIONS_RESPONSE = QName.get(E_DUMP_SESSIONS_RESPONSE, NAMESPACE);
    public static final QName GET_SESSIONS_REQUEST = QName.get(E_GET_SESSIONS_REQUEST, NAMESPACE);
    public static final QName GET_SESSIONS_RESPONSE = QName.get(E_GET_SESSIONS_RESPONSE, NAMESPACE);

    public static final QName GET_QUOTA_USAGE_REQUEST = QName.get(E_GET_QUOTA_USAGE_REQUEST, NAMESPACE);
    public static final QName GET_QUOTA_USAGE_RESPONSE = QName.get(E_GET_QUOTA_USAGE_RESPONSE, NAMESPACE);
    public static final QName COMPUTE_AGGR_QUOTA_USAGE_REQUEST = QName.get(E_COMPUTE_AGGR_QUOTA_USAGE_REQUEST, NAMESPACE);
    public static final QName COMPUTE_AGGR_QUOTA_USAGE_RESPONSE = QName.get(E_COMPUTE_AGGR_QUOTA_USAGE_RESPONSE, NAMESPACE);
    public static final QName GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST =
            QName.get(E_GET_AGGR_QUOTA_USAGE_ON_SERVER_REQUEST, NAMESPACE);
    public static final QName GET_AGGR_QUOTA_USAGE_ON_SERVER_RESPONSE =
            QName.get(E_GET_AGGR_QUOTA_USAGE_ON_SERVER_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_MAILBOXES_REQUEST = QName.get(E_GET_ALL_MAILBOXES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_MAILBOXES_RESPONSE = QName.get(E_GET_ALL_MAILBOXES_RESPONSE, NAMESPACE);
    public static final QName GET_MAILBOX_STATS_REQUEST = QName.get(E_GET_MAILBOX_STATS_REQUEST, NAMESPACE);
    public static final QName GET_MAILBOX_STATS_RESPONSE = QName.get(E_GET_MAILBOX_STATS_RESPONSE, NAMESPACE);

    public static final QName GET_MAIL_QUEUE_INFO_REQUEST = QName.get(E_GET_MAIL_QUEUE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_MAIL_QUEUE_INFO_RESPONSE = QName.get(E_GET_MAIL_QUEUE_INFO_RESPONSE, NAMESPACE);
    public static final QName GET_MAIL_QUEUE_REQUEST = QName.get(E_GET_MAIL_QUEUE_REQUEST, NAMESPACE);
    public static final QName GET_MAIL_QUEUE_RESPONSE = QName.get(E_GET_MAIL_QUEUE_RESPONSE, NAMESPACE);
    public static final QName MAIL_QUEUE_ACTION_REQUEST = QName.get(E_MAIL_QUEUE_ACTION_REQUEST, NAMESPACE);
    public static final QName MAIL_QUEUE_ACTION_RESPONSE = QName.get(E_MAIL_QUEUE_ACTION_RESPONSE, NAMESPACE);
    public static final QName MAIL_QUEUE_FLUSH_REQUEST = QName.get(E_MAIL_QUEUE_FLUSH_REQUEST, NAMESPACE);
    public static final QName MAIL_QUEUE_FLUSH_RESPONSE = QName.get(E_MAIL_QUEUE_FLUSH_RESPONSE, NAMESPACE);

    public static final QName SEARCH_DIRECTORY_REQUEST = QName.get(E_SEARCH_DIRECTORY_REQUEST, NAMESPACE);
    public static final QName SEARCH_DIRECTORY_RESPONSE = QName.get(E_SEARCH_DIRECTORY_RESPONSE, NAMESPACE);

    public static final QName GET_ACCOUNT_MEMBERSHIP_REQUEST = QName.get(E_GET_ACCOUNT_MEMBERSHIP_REQUEST, NAMESPACE);
    public static final QName GET_ACCOUNT_MEMBERSHIP_RESPONSE = QName.get(E_GET_ACCOUNT_MEMBERSHIP_RESPONSE, NAMESPACE);

    public static final QName GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST = QName.get(E_GET_DISTRIBUTION_LIST_MEMBERSHIP_REQUEST, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE = QName.get(E_GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE, NAMESPACE);

    // data sources
    public static final QName CREATE_DATA_SOURCE_REQUEST = QName.get(E_CREATE_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName CREATE_DATA_SOURCE_RESPONSE = QName.get(E_CREATE_DATA_SOURCE_RESPONSE, NAMESPACE);
    public static final QName GET_DATA_SOURCES_REQUEST = QName.get(E_GET_DATA_SOURCES_REQUEST, NAMESPACE);
    public static final QName GET_DATA_SOURCES_RESPONSE = QName.get(E_GET_DATA_SOURCES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_REQUEST = QName.get(E_MODIFY_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName MODIFY_DATA_SOURCE_RESPONSE = QName.get(E_MODIFY_DATA_SOURCE_RESPONSE, NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_REQUEST = QName.get(E_DELETE_DATA_SOURCE_REQUEST, NAMESPACE);
    public static final QName DELETE_DATA_SOURCE_RESPONSE = QName.get(E_DELETE_DATA_SOURCE_RESPONSE, NAMESPACE);

    // calendar time zone fixup
    public static final QName FIX_CALENDAR_TZ_REQUEST = QName.get(E_FIX_CALENDAR_TZ_REQUEST, NAMESPACE);
    public static final QName FIX_CALENDAR_TZ_RESPONSE = QName.get(E_FIX_CALENDAR_TZ_RESPONSE, NAMESPACE);

    // calendar item end time fixup
    public static final QName FIX_CALENDAR_END_TIME_REQUEST = QName.get(E_FIX_CALENDAR_END_TIME_REQUEST, NAMESPACE);
    public static final QName FIX_CALENDAR_END_TIME_RESPONSE = QName.get(E_FIX_CALENDAR_END_TIME_RESPONSE, NAMESPACE);

    // calendar item priority fixup
    public static final QName FIX_CALENDAR_PRIORITY_REQUEST = QName.get(E_FIX_CALENDAR_PRIORITY_REQUEST, NAMESPACE);
    public static final QName FIX_CALENDAR_PRIORITY_RESPONSE = QName.get(E_FIX_CALENDAR_PRIORITY_RESPONSE, NAMESPACE);

    // Admin saved searches
    public static final QName GET_ADMIN_SAVED_SEARCHES_REQUEST = QName.get(E_GET_ADMIN_SAVED_SEARCHES_REQUEST, NAMESPACE);
    public static final QName GET_ADMIN_SAVED_SEARCHES_RESPONSE = QName.get(E_GET_ADMIN_SAVED_SEARCHES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_ADMIN_SAVED_SEARCHES_REQUEST = QName.get(E_MODIFY_ADMIN_SAVED_SEARCHES_REQUEST, NAMESPACE);
    public static final QName MODIFY_ADMIN_SAVED_SEARCHES_RESPONSE = QName.get(E_MODIFY_ADMIN_SAVED_SEARCHES_RESPONSE, NAMESPACE);

    public static final QName CHECK_DIRECTORY_REQUEST = QName.get(E_CHECK_DIRECTORY_REQUEST, NAMESPACE);
    public static final QName CHECK_DIRECTORY_RESPONSE = QName.get(E_CHECK_DIRECTORY_RESPONSE, NAMESPACE);

    public static final QName FLUSH_CACHE_REQUEST = QName.get(E_FLUSH_CACHE_REQUEST, NAMESPACE);
    public static final QName FLUSH_CACHE_RESPONSE = QName.get(E_FLUSH_CACHE_RESPONSE, NAMESPACE);

    public static final QName COUNT_ACCOUNT_REQUEST = QName.get(E_COUNT_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName COUNT_ACCOUNT_RESPONSE = QName.get(E_COUNT_ACCOUNT_RESPONSE, NAMESPACE);

    public static final QName COUNT_OBJECTS_REQUEST = QName.get(E_COUNT_OBJECTS_REQUEST, NAMESPACE);
    public static final QName COUNT_OBJECTS_RESPONSE = QName.get(E_COUNT_OBJECTS_RESPONSE, NAMESPACE);

    public static final QName GET_SHARE_INFO_REQUEST = QName.get(E_GET_SHARE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_SHARE_INFO_RESPONSE = QName.get(E_GET_SHARE_INFO_RESPONSE, NAMESPACE);

    // Account loggers
    public static final QName ADD_ACCOUNT_LOGGER_REQUEST = QName.get(E_ADD_ACCOUNT_LOGGER_REQUEST, NAMESPACE);
    public static final QName ADD_ACCOUNT_LOGGER_RESPONSE = QName.get(E_ADD_ACCOUNT_LOGGER_RESPONSE, NAMESPACE);
    public static final QName REMOVE_ACCOUNT_LOGGER_REQUEST = QName.get(E_REMOVE_ACCOUNT_LOGGER_REQUEST, NAMESPACE);
    public static final QName REMOVE_ACCOUNT_LOGGER_RESPONSE = QName.get(E_REMOVE_ACCOUNT_LOGGER_RESPONSE, NAMESPACE);
    public static final QName GET_ACCOUNT_LOGGERS_REQUEST = QName.get(E_GET_ACCOUNT_LOGGERS_REQUEST, NAMESPACE);
    public static final QName GET_ACCOUNT_LOGGERS_RESPONSE = QName.get(E_GET_ACCOUNT_LOGGERS_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_ACCOUNT_LOGGERS_REQUEST = QName.get(E_GET_ALL_ACCOUNT_LOGGERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ACCOUNT_LOGGERS_RESPONSE = QName.get(E_GET_ALL_ACCOUNT_LOGGERS_RESPONSE, NAMESPACE);
    public static final QName RESET_ALL_LOGGERS_REQUEST = QName.get(E_RESET_ALL_LOGGERS_REQUEST, NAMESPACE);
    public static final QName RESET_ALL_LOGGERS_RESPONSE = QName.get(E_RESET_ALL_LOGGERS_RESPONSE, NAMESPACE);

    // f/b providers
    public static final QName GET_ALL_FREE_BUSY_PROVIDERS_REQUEST = QName.get(E_GET_ALL_FREE_BUSY_PROVIDERS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_FREE_BUSY_PROVIDERS_RESPONSE = QName.get(E_GET_ALL_FREE_BUSY_PROVIDERS_RESPONSE, NAMESPACE);
    public static final QName GET_FREE_BUSY_QUEUE_INFO_REQUEST = QName.get(E_GET_FREE_BUSY_QUEUE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_FREE_BUSY_QUEUE_INFO_RESPONSE = QName.get(E_GET_FREE_BUSY_QUEUE_INFO_RESPONSE, NAMESPACE);
    public static final QName PUSH_FREE_BUSY_REQUEST = QName.get(E_PUSH_FREE_BUSY_REQUEST, NAMESPACE);
    public static final QName PUSH_FREE_BUSY_RESPONSE = QName.get(E_PUSH_FREE_BUSY_RESPONSE, NAMESPACE);
    public static final QName PURGE_FREE_BUSY_QUEUE_REQUEST = QName.get(E_PURGE_FREE_BUSY_QUEUE_REQUEST, NAMESPACE);
    public static final QName PURGE_FREE_BUSY_QUEUE_RESPONSE = QName.get(E_PURGE_FREE_BUSY_QUEUE_RESPONSE, NAMESPACE);

    // calendar cache
    public static final QName PURGE_ACCOUNT_CALENDAR_CACHE_REQUEST = QName.get(E_PURGE_ACCOUNT_CALENDAR_CACHE_REQUEST, NAMESPACE);
    public static final QName PURGE_ACCOUNT_CALENDAR_CACHE_RESPONSE = QName.get(E_PURGE_ACCOUNT_CALENDAR_CACHE_RESPONSE, NAMESPACE);

    // admin-version of WaitSetRequest
    public static final QName ADMIN_CREATE_WAIT_SET_REQUEST = QName.get(E_ADMIN_CREATE_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName ADMIN_CREATE_WAIT_SET_RESPONSE = QName.get(E_ADMIN_CREATE_WAIT_SET_RESPONSE, NAMESPACE);
    public static final QName ADMIN_WAIT_SET_REQUEST = QName.get(E_ADMIN_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName ADMIN_WAIT_SET_RESPONSE = QName.get(E_ADMIN_WAIT_SET_RESPONSE, NAMESPACE);
    public static final QName ADMIN_DESTROY_WAIT_SET_REQUEST = QName.get(E_ADMIN_DESTROY_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName ADMIN_DESTROY_WAIT_SET_RESPONSE = QName.get(E_ADMIN_DESTROY_WAIT_SET_RESPONSE, NAMESPACE);
    public static final QName QUERY_WAIT_SET_REQUEST = QName.get(E_QUERY_WAIT_SET_REQUEST, NAMESPACE);
    public static final QName QUERY_WAIT_SET_RESPONSE = QName.get(E_QUERY_WAIT_SET_RESPONSE, NAMESPACE);

    // XMPPComponent
    public static final QName CREATE_XMPPCOMPONENT_REQUEST = QName.get(E_CREATE_XMPPCOMPONENT_REQUEST, NAMESPACE);
    public static final QName CREATE_XMPPCOMPONENT_RESPONSE = QName.get(E_CREATE_XMPPCOMPONENT_RESPONSE, NAMESPACE);
    public static final QName GET_XMPPCOMPONENT_REQUEST = QName.get(E_GET_XMPPCOMPONENT_REQUEST, NAMESPACE);
    public static final QName GET_XMPPCOMPONENT_RESPONSE = QName.get(E_GET_XMPPCOMPONENT_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_XMPPCOMPONENTS_REQUEST = QName.get(E_GET_ALL_XMPPCOMPONENTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_XMPPCOMPONENTS_RESPONSE = QName.get(E_GET_ALL_XMPPCOMPONENTS_RESPONSE, NAMESPACE);
    public static final QName DELETE_XMPPCOMPONENT_REQUEST = QName.get(E_DELETE_XMPPCOMPONENT_REQUEST, NAMESPACE);
    public static final QName DELETE_XMPPCOMPONENT_RESPONSE = QName.get(E_DELETE_XMPPCOMPONENT_RESPONSE, NAMESPACE);

    // rights
    public static final QName GET_RIGHT_REQUEST = QName.get(E_GET_RIGHT_REQUEST, NAMESPACE);
    public static final QName GET_RIGHT_RESPONSE = QName.get(E_GET_RIGHT_RESPONSE, NAMESPACE);
    public static final QName GET_ADMIN_CONSOLE_UI_COMP_REQUEST = QName.get(E_GET_ADMIN_CONSOLE_UI_COMP_REQUEST, NAMESPACE);
    public static final QName GET_ADMIN_CONSOLE_UI_COMP_RESPONSE = QName.get(E_GET_ADMIN_CONSOLE_UI_COMP_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_EFFECTIVE_RIGHTS_REQUEST = QName.get(E_GET_ALL_EFFECTIVE_RIGHTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_EFFECTIVE_RIGHTS_RESPONSE = QName.get(E_GET_ALL_EFFECTIVE_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_RIGHTS_REQUEST = QName.get(E_GET_ALL_RIGHTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_RIGHTS_RESPONSE = QName.get(E_GET_ALL_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName GET_EFFECTIVE_RIGHTS_REQUEST = QName.get(E_GET_EFFECTIVE_RIGHTS_REQUEST, NAMESPACE);
    public static final QName GET_EFFECTIVE_RIGHTS_RESPONSE = QName.get(E_GET_EFFECTIVE_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName GET_CREATE_OBJECT_ATTRS_REQUEST = QName.get(E_GET_CREATE_OBJECT_ATTRS_REQUEST, NAMESPACE);
    public static final QName GET_CREATE_OBJECT_ATTRS_RESPONSE = QName.get(E_GET_CREATE_OBJECT_ATTRS_RESPONSE, NAMESPACE);
    public static final QName GET_GRANTS_REQUEST = QName.get(E_GET_GRANTS_REQUEST, NAMESPACE);
    public static final QName GET_GRANTS_RESPONSE = QName.get(E_GET_GRANTS_RESPONSE, NAMESPACE);
    public static final QName GET_RIGHTS_DOC_REQUEST = QName.get(E_GET_RIGHTS_DOC_REQUEST, NAMESPACE);
    public static final QName GET_RIGHTS_DOC_RESPONSE = QName.get(E_GET_RIGHTS_DOC_RESPONSE, NAMESPACE);
    public static final QName GRANT_RIGHT_REQUEST = QName.get(E_GRANT_RIGHT_REQUEST, NAMESPACE);
    public static final QName GRANT_RIGHT_RESPONSE = QName.get(E_GRANT_RIGHT_RESPONSE, NAMESPACE);
    public static final QName REVOKE_RIGHT_REQUEST = QName.get(E_REVOKE_RIGHT_REQUEST, NAMESPACE);
    public static final QName REVOKE_RIGHT_RESPONSE = QName.get(E_REVOKE_RIGHT_RESPONSE, NAMESPACE);
    public static final QName CHECK_RIGHT_REQUEST = QName.get(E_CHECK_RIGHT_REQUEST, NAMESPACE);
    public static final QName CHECK_RIGHT_RESPONSE = QName.get(E_CHECK_RIGHT_RESPONSE, NAMESPACE);
    public static final QName GET_DELEGATED_ADMIN_CONSTRAINTS_REQUEST = QName.get(E_GET_DELEGATED_ADMIN_CONSTRAINTS_REQUEST, NAMESPACE);
    public static final QName GET_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE = QName.get(E_GET_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE, NAMESPACE);
    public static final QName MODIFY_DELEGATED_ADMIN_CONSTRAINTS_REQUEST = QName.get(E_MODIFY_DELEGATED_ADMIN_CONSTRAINTS_REQUEST, NAMESPACE);
    public static final QName MODIFY_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE = QName.get(E_MODIFY_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE, NAMESPACE);

    // Monitoring
    public static final QName GET_SERVER_STATS_REQUEST = QName.get(E_GET_SERVER_STATS_REQUEST, NAMESPACE);
    public static final QName GET_SERVER_STATS_RESPONSE = QName.get(E_GET_SERVER_STATS_RESPONSE, NAMESPACE);

    public static final QName GET_LOGGER_STATS_REQUEST = QName.get(E_GET_LOGGER_STATS_REQUEST, NAMESPACE);
    public static final QName GET_LOGGER_STATS_RESPONSE = QName.get(E_GET_LOGGER_STATS_RESPONSE, NAMESPACE);

    public static final QName SYNC_GAL_ACCOUNT_REQUEST = QName.get(E_SYNC_GAL_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName SYNC_GAL_ACCOUNT_RESPONSE = QName.get(E_SYNC_GAL_ACCOUNT_RESPONSE, NAMESPACE);

    // memcached
    public static final QName RELOAD_MEMCACHED_CLIENT_CONFIG_REQUEST = QName.get(E_RELOAD_MEMCACHED_CLIENT_CONFIG_REQUEST, NAMESPACE);
    public static final QName RELOAD_MEMCACHED_CLIENT_CONFIG_RESPONSE = QName.get(E_RELOAD_MEMCACHED_CLIENT_CONFIG_RESPONSE, NAMESPACE);
    public static final QName GET_MEMCACHED_CLIENT_CONFIG_REQUEST = QName.get(E_GET_MEMCACHED_CLIENT_CONFIG_REQUEST, NAMESPACE);
    public static final QName GET_MEMCACHED_CLIENT_CONFIG_RESPONSE = QName.get(E_GET_MEMCACHED_CLIENT_CONFIG_RESPONSE, NAMESPACE);

    // local config
    public static final QName RELOAD_LOCAL_CONFIG_REQUEST = QName.get(E_RELOAD_LOCAL_CONFIG_REQUEST, NAMESPACE);
    public static final QName RELOAD_LOCAL_CONFIG_RESPONSE = QName.get(E_RELOAD_LOCAL_CONFIG_RESPONSE, NAMESPACE);

    // wiki migration
    public static final QName MIGRATE_ACCOUNT_REQUEST = QName.get(E_MIGRATE_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName MIGRATE_ACCOUNT_RESPONSE = QName.get(E_MIGRATE_ACCOUNT_RESPONSE, NAMESPACE);

    // noop
    public static final QName NO_OP_REQUEST = QName.get(E_NO_OP_REQUEST, NAMESPACE);
    public static final QName NO_OP_RESPONSE = QName.get(E_NO_OP_RESPONSE, NAMESPACE);

    // cookie
    public static final QName CLEAR_COOKIE_REQUEST = QName.get(E_CLEAR_COOKIE_REQUEST, NAMESPACE);
    public static final QName CLEAR_COOKIE_RESPONSE = QName.get(E_CLEAR_COOKIE_RESPONSE, NAMESPACE);
    public static final QName REFRESH_REGISTERED_AUTHTOKENS_REQUEST = QName.get(E_REFRESH_REGISTERED_AUTHTOKENS_REQUEST, NAMESPACE);
    public static final QName REFRESH_REGISTERED_AUTHTOKENS_RESPONSE = QName.get(E_REFRESH_REGISTERED_AUTHTOKENS_RESPONSE, NAMESPACE);

    public static final QName GET_SMIME_CONFIG_REQUEST = QName.get(E_GET_SMIME_CONFIG_REQUEST, NAMESPACE);
    public static final QName GET_SMIME_CONFIG_RESPONSE = QName.get(E_GET_SMIME_CONFIG_RESPONSE, NAMESPACE);
    public static final QName MODIFY_SMIME_CONFIG_REQUEST = QName.get(E_MODIFY_SMIME_CONFIG_REQUEST, NAMESPACE);
    public static final QName MODIFY_SMIME_CONFIG_RESPONSE = QName.get(E_MODIFY_SMIME_CONFIG_RESPONSE, NAMESPACE);

    // Version Check
    public static final QName VC_REQUEST = QName.get(E_VC_REQUEST, NAMESPACE);
    public static final QName VC_RESPONSE = QName.get(E_VC_RESPONSE, NAMESPACE);

    // LicenseAdminService
    public static final QName INSTALL_LICENSE_REQUEST = QName.get(E_INSTALL_LICENSE_REQUEST, NAMESPACE);
    public static final QName INSTALL_LICENSE_RESPONSE = QName.get(E_INSTALL_LICENSE_RESPONSE, NAMESPACE);
    public static final QName ACTIVATE_LICENSE_REQUEST = QName.get(E_ACTIVATE_LICENSE_REQUEST, NAMESPACE);
    public static final QName ACTIVATE_LICENSE_RESPONSE = QName.get(E_ACTIVATE_LICENSE_RESPONSE, NAMESPACE);
    public static final QName GET_LICENSE_REQUEST = QName.get(E_GET_LICENSE_REQUEST, NAMESPACE);
    public static final QName GET_LICENSE_RESPONSE = QName.get(E_GET_LICENSE_RESPONSE, NAMESPACE);

    // Auto provision
    public static final QName AUTO_PROV_ACCOUNT_REQUEST = QName.get(E_AUTO_PROV_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName AUTO_PROV_ACCOUNT_RESPONSE = QName.get(E_AUTO_PROV_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName AUTO_PROV_TASK_CONTROL_REQUEST = QName.get(E_AUTO_PROV_TASK_CONTROL_REQUEST, NAMESPACE);
    public static final QName AUTO_PROV_TASK_CONTROL_RESPONSE = QName.get(E_AUTO_PROV_TASK_CONTROL_RESPONSE, NAMESPACE);
    public static final QName SEARCH_AUTO_PROV_DIRECTORY_REQUEST = QName.get(E_SEARCH_AUTO_PROV_DIRECTORY_REQUEST, NAMESPACE);
    public static final QName SEARCH_AUTO_PROV_DIRECTORY_RESPONSE = QName.get(E_SEARCH_AUTO_PROV_DIRECTORY_RESPONSE, NAMESPACE);

    // Retention policy
    public static final QName GET_SYSTEM_RETENTION_POLICY_REQUEST = QName.get(E_GET_SYSTEM_RETENTION_POLICY_REQUEST, NAMESPACE);
    public static final QName CREATE_SYSTEM_RETENTION_POLICY_REQUEST = QName.get(E_CREATE_SYSTEM_RETENTION_POLICY_REQUEST, NAMESPACE);
    public static final QName MODIFY_SYSTEM_RETENTION_POLICY_REQUEST = QName.get(E_MODIFY_SYSTEM_RETENTION_POLICY_REQUEST, NAMESPACE);
    public static final QName DELETE_SYSTEM_RETENTION_POLICY_REQUEST = QName.get(E_DELETE_SYSTEM_RETENTION_POLICY_REQUEST, NAMESPACE);

    // Store Manager Verifier
    public static final QName VERIFY_STORE_MANAGER_REQUEST = QName.get(E_VERIFY_STORE_MANAGER_REQUEST, NAMESPACE);
    public static final QName VERIFY_STORE_MANAGER_RESPONSE = QName.get(E_VERIFY_STORE_MANAGER_RESPONSE, NAMESPACE);

    // Skins
    public static final QName GET_ALL_SKINS_REQUEST = QName.get(E_GET_ALL_SKINS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_SKINS_RESPONSE = QName.get(E_GET_ALL_SKINS_RESPONSE, NAMESPACE);

    // Two-Factor Authentication
    public static final QName CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST = QName.get(E_CLEAR_TWO_FACTOR_AUTH_DATA_REQUEST, NAMESPACE);
    public static final QName CLEAR_TWO_FACTOR_AUTH_DATA_RESPONSE = QName.get(E_CLEAR_TWO_FACTOR_AUTH_DATA_RESPONSE, NAMESPACE);
    public static final QName GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_REQUEST = QName.get(E_GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_RESPONSE = QName.get(E_GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_RESPONSE, NAMESPACE);

    public static final String E_FILTER_RULES = "filterRules";
    public static final String E_FILTER_RULE = "filterRule";
    public static final String E_GET_FILTER_RULES_REQUEST = "GetFilterRulesRequest";
    public static final String E_GET_FILTER_RULES_RESPONSE = "GetFilterRulesResponse";
    public static final String E_MODIFY_FILTER_RULES_REQUEST = "ModifyFilterRulesRequest";
    public static final String E_MODIFY_FILTER_RULES_RESPONSE = "ModifyFilterRulesResponse";
    public static final String E_GET_OUTGOING_FILTER_RULES_REQUEST = "GetOutgoingFilterRulesRequest";
    public static final String E_GET_OUTGOING_FILTER_RULES_RESPONSE = "GetOutgoingFilterRulesResponse";
    public static final String E_MODIFY_OUTGOING_FILTER_RULES_REQUEST = "ModifyOutgoingFilterRulesRequest";
    public static final String E_MODIFY_OUTGOING_FILTER_RULES_RESPONSE = "ModifyOutgoingFilterRulesResponse";
    public static final String E_CONTACT_BACKUP_REQUEST = "ContactBackupRequest";
    public static final String E_CONTACT_BACKUP_RESPONSE = "ContactBackupResponse";
    public static final QName GET_FILTER_RULES_REQUEST = QName.get(E_GET_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName GET_FILTER_RULES_RESPONSE = QName.get(E_GET_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_FILTER_RULES_REQUEST = QName.get(E_MODIFY_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName MODIFY_FILTER_RULES_RESPONSE = QName.get(E_MODIFY_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName GET_OUTGOING_FILTER_RULES_REQUEST = QName.get(E_GET_OUTGOING_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName GET_OUTGOING_FILTER_RULES_RESPONSE = QName.get(E_GET_OUTGOING_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_OUTGOING_FILTER_RULES_REQUEST = QName.get(E_MODIFY_OUTGOING_FILTER_RULES_REQUEST, NAMESPACE);
    public static final QName MODIFY_OUTGOING_FILTER_RULES_RESPONSE = QName.get(E_MODIFY_OUTGOING_FILTER_RULES_RESPONSE, NAMESPACE);
    public static final QName CONTACT_BACKUP_REQUEST = QName.get(E_CONTACT_BACKUP_REQUEST, NAMESPACE);
    public static final QName CONTACT_BACKUP_RESPONSE = QName.get(E_CONTACT_BACKUP_RESPONSE, NAMESPACE);

    //HAB
    public static final String E_HAB_ORG_UNIT_REQUEST = "HABOrgUnitRequest";
    public static final String E_HAB_ORG_UNIT_RESPONSE = "HABOrgUnitResponse";
    public static final QName HAB_ORG_UNIT_REQUEST = QName.get(E_HAB_ORG_UNIT_REQUEST, NAMESPACE);
    public static final QName HAB_ORG_UNIT_RESPONSE = QName.get(E_HAB_ORG_UNIT_RESPONSE, NAMESPACE);
    public static final String E_HAB_ORG_UNIT_NAME = "habOrgUnitName";
    public static final String E_HAB_PARENT_GROUP = "parentHABGroup";
    public static final String E_MEMBER = "member";

    //AddressList
    public static final String E_GET_ALL_ADDRESS_LISTS_REQUEST = "GetAllAddressListsRequest";
    public static final String E_GET_ALL_ADDRESS_LISTS_RESPONSE = "GetAllAddressListsResponse";
    public static final QName GET_ALL_ADDRESS_LISTS_REQUEST = QName.get(E_GET_ALL_ADDRESS_LISTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ADDRESS_LISTS_RESPONSE = QName.get(E_GET_ALL_ADDRESS_LISTS_RESPONSE, NAMESPACE);
    public static final String E_DELETE_ADDRESS_LIST_REQUEST = "DeleteAddressListRequest";
    public static final String E_DELETE_ADDRESS_LIST_RESPONSE = "DeleteAddressListResponse";
    public static final QName DELETE_ADDRESS_LIST_REQUEST = QName.get(E_DELETE_ADDRESS_LIST_REQUEST, NAMESPACE);
    public static final QName DELETE_ADDRESS_LIST_RESPONSE = QName.get(E_DELETE_ADDRESS_LIST_RESPONSE, NAMESPACE);
    public static final String E_MODIFY_ADDRESS_LIST_REQUEST = "ModifyAddressListRequest";
    public static final String E_MODIFY_ADDRESS_LIST_RESPONSE = "ModifyAddressListResponse";
    public static final QName MODIFY_ADDRESS_LIST_REQUEST = QName.get(E_MODIFY_ADDRESS_LIST_REQUEST, NAMESPACE);
    public static final QName MODIFY_ADDRESS_LIST_RESPONSE = QName.get(E_MODIFY_ADDRESS_LIST_RESPONSE, NAMESPACE);
    public static final String E_GET_ADDRESS_LIST_INFO_REQUEST = "GetAddressListInfoRequest";
    public static final String E_GET_ADDRESS_LIST_INFO_RESPONSE = "GetAddressListInfoResponse";
    public static final QName GET_ADDRESS_LIST_INFO_REQUEST = QName.get(E_GET_ADDRESS_LIST_INFO_REQUEST, NAMESPACE);
    public static final QName GET_ADDRESS_LIST_INFO_RESPONSE = QName.get(E_GET_ADDRESS_LIST_INFO_RESPONSE, NAMESPACE);


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

    // Dump waitsets
    public static final String A_READY = "ready";
    public static final String A_OWNER = "owner";
    public static final String A_DEFTYPES = "defTypes";
    public static final String A_ACCOUNTS = "accounts";
    public static final String A_CB_SEQ_NO = "cbSeqNo";
    public static final String A_CURRENT_SEQ_NO = "currentSeqNo";
    public static final String A_NEXT_SEQ_NO = "nextSeqNo";
    public static final String A_AID = "aid";
    public static final String A_CID = "cid";
    public static final String E_ERRORS = "errors";

    public static final String E_ACCOUNT = "account";
    public static final String E_CALENDAR_RESOURCE = "calresource";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_NAME = "name";
    public static final String E_NEW_NAME = "newName";
    public static final String E_BINDDN = "bindDn";
    public static final String E_CACHE = "cache";
    public static final String E_CODE = "code";
    public static final String E_CONFIG = "config";
    public static final String E_COOKIE = "cookie";
    public static final String E_COS = "cos";
    public static final String E_CN = "cn";
    public static final String E_DOMAIN = "domain";
    public static final String E_DL = "dl";
    public static final String E_DL_OWNER = "owner";
    public static final String E_DL_OWNERS = "owners";
    public static final String E_DLM = "dlm";
    public static final String E_HOSTNAME = "hostname";
    public static final String E_LIFETIME = "lifetime";
    public static final String E_MESSAGE = "message";
    public static final String E_BY_DEVICEID_ONLY = "bydeviceidonly";
    public static final String E_USERNAME = "username";
    public static final String E_PASSWORD = "password";
    public static final String E_NEW_PASSWORD = "newPassword";
    public static final String E_QUERY = "query";
    public static final String E_QUEUE = "queue";
    public static final String E_ACTION = "action";
    public static final String E_SERVER = "server";
    public static final String E_ALWAYSONCLUSTER = "alwaysOnCluster";
    public static final String E_UC_SERVICE = "ucservice";
    public static final String E_XMPP_COMPONENT = "xmppcomponent";
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
    public static final String E_VOLUME_EXT = "volumeExternalInfo";
    public static final String E_VOLUME_OPENIO_EXT = "volumeExternalOpenIoInfo";
    public static final String E_STORE_MANAGER_RUNTIME_SWITCH_RESULT = "storeManagerRuntimeSwitchResult";
    public static final String E_PROGRESS = "progress";
    public static final String E_SOAP_URL = "soapURL";
    public static final String E_ADMIN_SOAP_URL = "adminSoapURL";
    public static final String E_PUBLIC_MAIL_URL = "publicMailURL";
    public static final String E_SEARCH = "search";
    public static final String E_DIRECTORY = "directory";
    public static final String E_PROVIDER = "provider";
    public static final String E_STATS = "stats";
    public static final String E_FOLDER = "folder";
    public static final String E_OWNER = "owner";
    public static final String E_SHARE = "share";
    public static final String E_DATASOURCE = "datasource";
    public static final String E_ENTRY = "entry";
    public static final String E_KEY = "key";
    public static final String E_PRINCIPAL = "principal";
    public static final String E_SKIN = "skin";
    public static final String E_TOKEN = "token";

    //HAB
    public static final String E_HAB_GROUP_OPERATION ="habGroupOperation";

    public static final String A_HAB_DISPLAY_NAME = "habDisplayName";
    public static final String A_HAB_ORG_UNIT = "habOrgUnit";
    public static final String A_HAB_GROUP_ID ="habGroupId";
    public static final String A_CURRENT_PARENT_HAB_GROUP_ID = "currentParentHabGroupId";
    public static final String A_TARGET_PARENT_HAB_GROUP_ID = "targetParentHabGroupId";

    public static final String A_ACCOUNT = "account";
    public static final String A_CALENDAR_RESOURCE = "calresource";
    public static final String A_COS = "cos";
    public static final String A_DISTRIBUTION_LIST = "distributionlist";
    public static final String A_DOMAIN = "domain";
    public static final String A_SERVER = "server";

    public static final String A_ACTION = "action";
    public static final String A_APPLY_CONFIG = "applyConfig";
    public static final String A_APPLY_COS = "applyCos";
    public static final String A_ID = "id";
    public static final String A_MAX_RESULTS = "maxResults";
    public static final String A_LIMIT = "limit";
    public static final String A_OFFSET = "offset";
    public static final String A_ATTRS = "attrs";
    public static final String A_SEARCH_TOTAL = "searchTotal";
    public static final String A_SORT_BY = "sortBy";
    public static final String A_SORT_ASCENDING = "sortAscending";
    public static final String A_TYPE = "type";
    public static final String A_ONLY_RELATED = "onlyRelated";
    public static final String A_C = "c";
    public static final String A_T = "t";
    public static final String A_NAME = "name";
    public static final String A_NUM = "num";
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
    public static final String A_FOLDER = "l";  // to be consistent with MailConstants.A_FOLDER
    public static final String A_FOLDER_IDS = "folderIds";
    public static final String A_PATH = "path";
    public static final String A_PATH_OR_ID = "pathOrId";
    public static final String A_CREATE = "create";
    public static final String A_EXISTS = "exists";
    public static final String A_IS_DEFAULT_COS = "isDefaultCos";
    public static final String A_IS_DIRECTORY = "isDirectory";
    public static final String A_READABLE = "readable";
    public static final String A_WRITABLE = "writable";
    public static final String A_REVISION = "rev";
    public static final String A_ENTRY_TYPES = "entryTypes";
    public static final String A_DESC = "desc";
    public static final String A_DN = "dn";
    public static final String A_KEYATTR = "keyAttr";
    public static final String A_DYNAMIC = "dynamic";
    public static final String A_PERSIST_AUTH_TOKEN_COOKIE = "persistAuthTokenCookie";
    public static final String A_ALL_SERVERS = "allServers";

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

    public static final String E_TIMEZONE = "timezone";
    public static final String A_TIMEZONE_ID = "id";
    public static final String A_TIMEZONE_DISPLAYNAME = "displayName";

    public static final String A_HEALTHY = "healthy";
    public static final String A_SIZE = "s";
    public static final String A_SERVICE = "service";
    public static final String A_ALWAYSONCLUSTER_ID = "alwaysOnClusterId";
    public static final String A_STATUS = "status";
    public static final String A_TIME = "time";
    public static final String A_TYPES = "types";
    public static final String A_NUM_TABLES = "numTables";


    public static final String A_NUM_EXECUTED = "numExecuted";
    public static final String A_NUM_SUCCEEDED= "numSucceeded";
    public static final String A_NUM_FAILED = "numFailed";
    public static final String A_NUM_SKIPPED = "numSkipped";
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
    public static final String A_VOLUME_CURRENT = "current";
    public static final String A_VOLUME_STORE_TYPE = "storeType";
    public static final String A_VOLUME_STORAGE_TYPE = "storageType";
    public static final String A_VOLUME_VOLUME_PREFIX = "volumePrefix";
    public static final String A_VOLUME_STORE_PROVIDER = "storeProvider";
    public static final String A_VOLUME_GLB_BUCKET_CONFIG_ID = "globalBucketConfigId";
    public static final String A_VOLUME_USE_IN_FREQ_ACCESS = "useInFrequentAccess";
    public static final String A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD = "useInFrequentAccessThreshold";
    public static final String A_VOLUME_USE_INTELLIGENT_TIERING = "useIntelligentTiering";
    public static final String A_VOLUME_URL = "url";
    public static final String A_VOLUME_ACCOUNT = "account";
    public static final String A_VOLUME_NAMESPACE = "nameSpace";
    public static final String A_VOLUME_PROXY_PORT= "proxyPort";
    public static final String A_VOLUME_ACCOUNT_PORT = "accountPort";
    public static final String A_VOLUME_S3 = "S3";
    public static final String A_VOLUME_OPEN_IO = "OPENIO";
    public static final String A_VOLUME_STORE_MANAGER_CLASS = "storeManagerClass";
    public static final String A_VOLUME_BUCKET_ID = "bucketId";
    public static final String A_VOLUME_NAME_SPACE = "nameSpace";
    public static final String A_VOLUME_UNIFIED = "unified";

    // Blob consistency check
    public static final String E_MISSING_BLOBS = "missingBlobs";
    public static final String E_ITEM = "item";
    public static final String A_BLOB_PATH = "blobPath";
    public static final String E_UNEXPECTED_BLOBS = "unexpectedBlobs";
    public static final String E_BLOB = "blob";
    public static final String E_INCORRECT_SIZE = "incorrectSize";
    public static final String E_INCORRECT_REVISION = "incorrectRevision";
    public static final String E_USED_BLOBS = "usedBlobs";
    public static final String A_FILE_DATA_SIZE = "fileDataSize";
    public static final String A_FILE_SIZE = "fileSize";
    public static final String A_CHECK_SIZE = "checkSize";
    public static final String A_REPORT_USED_BLOBS = "reportUsedBlobs";
    public static final String A_VOLUME_ID = "volumeId";
    public static final String A_EXPORT_DIR = "exportDir";
    public static final String A_EXPORT_FILENAME_PREFIX = "exportFilenamePrefix";
    public static final String A_EXTERNAL = "external";

    public static final String A_VERSION_INFO_INFO = "info";
    public static final String A_VERSION_INFO_VERSION = "version";
    public static final String A_VERSION_INFO_PLATFORM = "platform";
    public static final String A_VERSION_INFO_MAJOR = "majorversion";
    public static final String A_VERSION_INFO_MINOR = "minorversion";
    public static final String A_VERSION_INFO_MICRO = "microversion";
    public static final String A_VERSION_INFO_RELEASE = "release";
    public static final String A_VERSION_INFO_DATE = "buildDate";
    public static final String A_VERSION_INFO_HOST = "host";
    public static final String A_VERSION_INFO_TYPE = "type";
    public static final String A_VERSION_INFO_RELCLASS = "relclass";
    public static final String A_VERSION_INFO_RELNUM = "relnum";
    public static final String A_VERSION_INFO_BUILDNUM = "buildnum";

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
    public static final String A_EFFECTIVE_QUOTA = "effectiveQuota";

    public static final String E_TEMPLATE = "template";
    public static final String E_TEST = "test";
    public static final String A_DEST = "dest";

    public static final String A_CONCURRENCY = "concurrency";

    public static final String A_DEPLOYALL = "deployall";
    public static final String A_DEPLOYLOCAL = "deploylocal";

    public static final String A_TZFIXUP_AFTER = "after";
    public static final String A_TZFIXUP_SYNC = "sync";
    public static final String A_FLUSH = "flush";

    // Account loggers
    public static final String E_LOGGER = "logger";
    public static final String E_ACCOUNT_LOGGER = "accountLogger";
    public static final String A_CATEGORY = "category";
    public static final String A_LEVEL = "level";

    public static final String A_PROPAGATE = "propagate";
    public static final String A_START = "start";
    public static final String A_END = "end";
    public static final String A_PREFIX = "prefix";
    public static final String A_QUEUE = "queue";

    // mailbox stats
    public static final String A_NUM_MBOXES = "numMboxes";
    public static final String A_TOTAL_SIZE = "totalSize";

    public static final String A_TOTAL_COUNT = "totalCount";
    public static final String A_PROGRESS = "progress";
    public static final String E_VOLUME_BLOBS_PROGRESS = "volumeBlobsProgress";
    public static final String E_BLOB_DIGESTS_PROGRESS = "blobDigestsProgress";
    // index stats
    public static final String A_MAX_DOCS = "maxDocs";
    public static final String A_DELETED_DOCS = "deletedDocs";

    // mailbox table
    public static final String A_MT_ID               = "id";
    public static final String A_MT_GROUPID          = "groupId";
    public static final String A_MT_ACCOUNTID        = "accountId";
    public static final String A_MT_INDEXVOLUMEID    = "indexVolumeId";
    public static final String A_MT_ITEMIDCHECKPOINT = "itemIdCheckPoint";
    public static final String A_MT_CONTACTCOUNT     = "contactCount";
    public static final String A_MT_SIZECHECKPOINT   = "sizeCheckPoint";
    public static final String A_MT_CHANGECHECKPOINT = "changeCheckPoint";
    public static final String A_MT_TRACKINGSYNC     = "trackingSync";
    public static final String A_MT_TRACKINGIMAP     = "trackingImap";
    public static final String A_MT_LASTBACKUPAT     = "lastbackupat";
    public static final String A_MT_LASTSOAPACCESS   = "lastSoapAccess";
    public static final String A_MT_NEWNESSAGES      = "newMessages";

    // right
    public static final String E_ALL         = "all";
    public static final String E_ATTRS       = "attrs";
    public static final String E_CMD         = "cmd";
    public static final String E_CONSTRAINT  = "constraint";
    public static final String E_DEFAULT     = "default";
    public static final String E_DESC        = "desc";
    public static final String E_ENTRIES     = "entries";
    public static final String E_GET_ATTRS   = "getAttrs";
    public static final String E_GRANT       = "grant";
    public static final String E_GRANTEE     = "grantee";
    public static final String E_IN_DOMAINS  = "inDomains";
    public static final String E_MAX         = "max";
    public static final String E_MIN         = "min";
    public static final String E_NOTE        = "note";
    public static final String E_PACKAGE     = "package";
    public static final String E_RIGHTS      = "rights";
    public static final String E_R           = "r";
    public static final String E_RIGHT       = "right";
    public static final String E_TARGET      = "target";
    public static final String E_SET_ATTRS   = "setAttrs";
    public static final String E_VALUE       = "v";
    public static final String E_VALUES      = "values";
    public static final String E_VIA         = "via";
    public static final String A_ALL         = "all";
    public static final String A_ALLOW       = "allow";
    public static final String A_CAN_DELEGATE= "canDelegate";
    public static final String A_DENY        = "deny";
    public static final String A_DISINHERIT_SUB_GROUPS = "disinheritSubGroups";
    public static final String A_SUB_DOMAIN  = "subDomain";
    public static final String A_EXPAND_ALL_ATTRS = "expandAllAttrs";
    public static final String A_INHERITED   = "inherited";
    public static final String A_RIGHT       = "right";
    public static final String A_RIGHT_CLASS = "rightClass";
    public static final String A_TARGET_TYPE = "targetType";
    public static final String A_SECRET      = "secret";

    public static final String E_WAITSET = "waitSet";

    // Monitoring
    public static final String E_STAT = "stat";
    public static final String A_DESCRIPTION = "description";

    public static final String A_FULLSYNC = "fullSync";
    public static final String A_RESET    = "reset";
    public static final String A_HAS_KEYWORD = "hasKeyword";

    // memcached client
    public static final String A_MEMCACHED_CLIENT_CONFIG_SERVER_LIST = "serverList";
    public static final String A_MEMCACHED_CLIENT_CONFIG_HASH_ALGORITHM = "hashAlgorithm";
    public static final String A_MEMCACHED_CLIENT_CONFIG_BINARY_PROTOCOL = "binaryProtocol";
    public static final String A_MEMCACHED_CLIENT_CONFIG_DEFAULT_EXPIRY_SECONDS = "defaultExpirySeconds";
    public static final String A_MEMCACHED_CLIENT_CONFIG_DEFAULT_TIMEOUT_MILLIS = "defaultTimeoutMillis";

    // CheckExchangeAuth
    public static final String E_AUTH = "auth";
    public static final String A_URL = "url";
    public static final String A_USER = "user";
    public static final String A_PASS = "pass";
    public static final String A_SCHEME = "scheme";

    // flush cache
    public static final String A_ALLSERVERS = "allServers";
    public static final String A_IMAPSERVERS = "imapServers";

    public static final String A_SYNCHRONOUS = "synchronous";

    public static final String E_MIGRATE = "migrate";

    public static final String A_ERROR = "error";

    public static final String OP_MODIFY = "modify";
    public static final String OP_REMOVE = "remove";

    public static final String A_OPERATION = "op";
    // XmlFixupRules (FixCalenderTZ)
    public static final String E_TZFIXUP = "tzfixup";
    public static final String E_FIXUP_RULE = "fixupRule";
    public static final String E_ANY = "any";
    public static final String E_TZID = "tzid";
    public static final String E_NON_DST = "nonDst";
    public static final String E_RULES = "rules";
    public static final String E_DATES = "dates";
    public static final String E_STANDARD = "standard";
    public static final String E_DAYLIGHT = "daylight";
    public static final String E_REPLACE = "replace";
    public static final String E_TOUCH = "touch";
    public static final String E_WELL_KNOWN_TZ = "wellKnownTz";

    public static final String A_STDOFF = "stdoff";
    public static final String A_DAYOFF = "dayoff";
    public static final String A_MON = "mon";
    public static final String A_WEEK = "week";
    public static final String A_WKDAY = "wkday";
    public static final String A_MDAY = "mday";

    // Version Check
    public static final String VERSION_CHECK_STATUS = "status";
    public static final String VERSION_CHECK_CHECK = "check";
    public static final String E_VERSION_CHECK = "versionCheck";
    public static final String A_UPDATE_TYPE = "type";
    public static final String E_UPDATES= "updates";
    public static final String E_UPDATE = "update";
    public static final String A_VERSION_CHECK_STATUS = "status";
    public static final String A_CRITICAL = "critical";
    public static final String A_UPDATE_URL = "updateURL";
    public static final String A_SHORT_VERSION = "shortversion";
    public static final String A_VERSION = "version";
    public static final String A_RELEASE = "release";
    public static final String A_PLATFORM = "platform";
    public static final String A_BUILDTYPE = "buildtype";

    // ZimbraLicenseExtenstion LicenseService and LicenseAdminService
    public static final String E_CONTENT = "content";
    public static final String A_ATTACHMENT_ID = "aid";
    public static final String E_LICENSE = "license";
    public static final String E_ACTIVATION = "activation";
    public static final String E_INFO = "info";
    public static final String A_VALID_FROM = "validFrom";
    public static final String A_VALID_UNTIL = "validUntil";
    public static final String A_SERVER_TIME = "serverTime";
    public static final String A_FEATURE = "feature";

    // Retention policy
    public static final String E_RETENTION_POLICY = "retentionPolicy";
    public static final String E_KEEP = "keep";
    public static final String E_PURGE = "purge";
    public static final String E_POLICY = "policy";

    //StoreManager verification
    public static final String A_CHECK_BLOBS = "checkBlobs";

    public static final String A_COUNT_ONLY = "countOnly";

    // Two-Factor Auth
    public static final String A_LAZY_DELETE = "lazyDelete";

    // Sieve editheader extension constants
    public static final String A_LAST = "last";
    public static final String E_HEADERNAME = "headerName";
    public static final String E_HEADERVALUE = "headerValue";
    public static final String A_COMPARATOR = "comparator";
    public static final String A_MATCHTYPE = "matchType";
    public static final String A_COUNT_COMPARATOR = "countComparator";
    public static final String A_VALUE_COMPARATOR = "valueComparator";
    public static final String A_RELATIONAL_COMPARATOR = "relationalComparator";
    public static final String E_NEW_VALUE = "newValue";

    // contact backup feature
    public static final String E_SERVERS = "servers";

    //HAB
    public static final String A_NEW_NAME = "newName";
    public static final String A_FORCE_DELETE = "forceDelete";
    public static final String E_MEMBERS = "members";
    public static final String A_CASCADE_DELETE = "cascadeDelete";

    // address list
    public static final String E_CREATE_ADDRESS_LIST_REQUEST = "CreateAddressListRequest";
    public static final String E_CREATE_ADDRESS_LIST_RESPONSE = "CreateAddressListResponse";
    public static final QName GET_CREATE_ADDRESS_LIST_REQUEST = QName.get(E_CREATE_ADDRESS_LIST_REQUEST, NAMESPACE);
    public static final QName GET_CREATE_ADDRESS_LIST_RESPONSE = QName.get(E_CREATE_ADDRESS_LIST_RESPONSE, NAMESPACE);
    public static final String E_SEARCH_FILTER = "searchFilter";
    public static final String E_GAL_FILTER = "galFilter";
    public static final String E_LDAP_FILTER = "ldapFilter";
    public static final String A_CLEAR_FILTER = "clearFilter";

    public static final String E_DEVICES = "devices";

    public static final String A_SM_RUNTIME_SWITCH_STATUS = "status";
    public static final String A_SM_RUNTIME_SWITCH_MESSAGE = "message";

    // Global External Store Config
    public static final String E_GET_S3_BUCKET_CONFIG_REQUEST = "GetS3BucketConfigRequest";
    public static final String E_GET_S3_BUCKET_CONFIG_RESPONSE = "GetS3BucketConfigResponse";
    public static final String E_EDIT_S3_BUCKET_CONFIG_REQUEST = "EditS3BucketConfigRequest";
    public static final String E_EDIT_S3_BUCKET_CONFIG_RESPONSE = "EditS3BucketConfigResponse";
    public static final String E_CREATE_S3_BUCKET_CONFIG_REQUEST = "CreateS3BucketConfigRequest";
    public static final String E_CREATE_S3_BUCKET_CONFIG_RESPONSE = "CreateS3BucketConfigResponse";
    public static final String E_DELETE_S3_BUCKET_CONFIG_REQUEST = "DeleteS3BucketConfigRequest";
    public static final String E_DELETE_S3_BUCKET_CONFIG_RESPONSE = "DeleteS3BucketConfigResponse";
    public static final QName GET_S3_BUCKET_CONFIG_REQUEST = QName.get(E_GET_S3_BUCKET_CONFIG_REQUEST, NAMESPACE);
    public static final QName GET_S3_BUCKET_CONFIG_RESPONSE = QName.get(E_GET_S3_BUCKET_CONFIG_RESPONSE, NAMESPACE);
    public static final QName EDIT_S3_BUCKET_CONFIG_REQUEST = QName.get(E_EDIT_S3_BUCKET_CONFIG_REQUEST, NAMESPACE);;
    public static final QName EDIT_S3_BUCKET_CONFIG_RESPONSE = QName.get(E_EDIT_S3_BUCKET_CONFIG_RESPONSE, NAMESPACE);
    public static final QName CREATE_S3_BUCKET_CONFIG_REQUEST = QName.get(E_CREATE_S3_BUCKET_CONFIG_REQUEST, NAMESPACE);
    public static final QName CREATE_S3_BUCKET_CONFIG_RESPONSE = QName.get(E_CREATE_S3_BUCKET_CONFIG_RESPONSE, NAMESPACE);
    public static final QName DELETE_S3_BUCKET_CONFIG_REQUEST = QName.get(E_DELETE_S3_BUCKET_CONFIG_REQUEST, NAMESPACE);
    public static final QName DELETE_S3_BUCKET_CONFIG_RESPONSE = QName.get(E_DELETE_S3_BUCKET_CONFIG_RESPONSE, NAMESPACE);

    // Validate External Config
    public static final String E_VALIDATE_S3_BUCKET_REACHABLE_REQUEST = "ValidateS3BucketReachableRequest";
    public static final String E_VALIDATE_S3_BUCKET_REACHABLE_RESPONSE = "ValidateS3BucketReachableResponse";
    public static final QName VALIDATE_S3_BUCKET_REACHABLE_REQUEST = QName.get(E_VALIDATE_S3_BUCKET_REACHABLE_REQUEST, NAMESPACE);
    public static final QName VALIDATE_S3_BUCKET_REACHABLE_RESPONSE = QName.get(E_VALIDATE_S3_BUCKET_REACHABLE_RESPONSE, NAMESPACE);

    // Removed Zetras zimlet package list
    public static final List<String> ZEXTRAS_PACKAGES_LIST = Arrays.asList("com_ng_auth", "com_zextras_zextras",
            "com_zextras_client", "com_zimbra_connect_classic", "com_zimbra_connect_modern", "com_zextras_docs",
            "com_zimbra_docs_modern", "com_zimbra_drive_modern", "com_zextras_drive", "com_zextras_drive_open",
            "com_zextras_chat_open", "com_zextras_talk", "zimbra-zimlet-briefcase-edit-lool");

}
