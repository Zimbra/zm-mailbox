/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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


public class AccountConstants {

    public static final String USER_SERVICE_URI  = "/service/soap/";
    public static final String CONTENT_SERVLET_PATH = "/service/content";

    public static final String NAMESPACE_STR = "urn:zimbraAccount";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    // auth
    public static final String E_AUTH_REQUEST = "AuthRequest";
    public static final String E_AUTH_RESPONSE = "AuthResponse";
    public static final String E_CHANGE_PASSWORD_REQUEST = "ChangePasswordRequest";
    public static final String E_CHANGE_PASSWORD_RESPONSE = "ChangePasswordResponse";
    public static final String E_CLIENT_INFO_REQUEST = "ClientInfoRequest";
    public static final String E_CLIENT_INFO_RESPONSE = "ClientInfoResponse";
    public static final String E_END_SESSION_REQUEST = "EndSessionRequest";
    public static final String E_END_SESSION_RESPONSE = "EndSessionResponse";
    public static final String E_ENABLE_TWO_FACTOR_AUTH_REQUEST = "EnableTwoFactorAuthRequest";
    public static final String E_ENABLE_TWO_FACTOR_AUTH_RESPONSE = "EnableTwoFactorAuthResponse";
    public static final String E_DISABLE_TWO_FACTOR_AUTH_REQUEST = "DisableTwoFactorAuthRequest";
    public static final String E_DISABLE_TWO_FACTOR_AUTH_RESPONSE = "DisableTwoFactorAuthResponse";
    public static final String E_CREATE_APP_SPECIFIC_PASSWORD_REQUEST = "CreateAppSpecificPasswordRequest";
    public static final String E_CREATE_APP_SPECIFIC_PASSWORD_RESPONSE = "CreateAppSpecificPasswordResponse";
    public static final String E_REVOKE_APP_SPECIFIC_PASSWORD_REQUEST = "RevokeAppSpecificPasswordRequest";
    public static final String E_REVOKE_APP_SPECIFIC_PASSWORD_RESPONSE = "RevokeAppSpecificPasswordResponse";
    public static final String E_GET_APP_SPECIFIC_PASSWORDS_REQUEST = "GetAppSpecificPasswordsRequest";
    public static final String E_GET_APP_SPECIFIC_PASSWORDS_RESPONSE = "GetAppSpecificPasswordsResponse";
    public static final String E_GET_SCRATCH_CODES_REQUEST = "GetScratchCodesRequest";
    public static final String E_GET_SCRATCH_CODES_RESPONSE = "GetScratchCodesResponse";
    public static final String E_GENERATE_SCRATCH_CODES_REQUEST = "GenerateScratchCodesRequest";
    public static final String E_GENERATE_SCRATCH_CODES_RESPONSE = "GenerateScratchCodesResponse";
    public static final String E_GET_TRUSTED_DEVICES_REQUEST = "GetTrustedDevicesRequest";
    public static final String E_GET_TRUSTED_DEVICES_RESPONSE = "GetTrustedDevicesResponse";
    public static final String E_REVOKE_TRUSTED_DEVICE_REQUEST = "RevokeTrustedDeviceRequest";
    public static final String E_REVOKE_TRUSTED_DEVICE_RESPONSE = "RevokeTrustedDeviceResponse";
    public static final String E_REVOKE_OTHER_TRUSTED_DEVICES_REQUEST = "RevokeOtherTrustedDevicesRequest";
    public static final String E_REVOKE_OTHER_TRUSTED_DEVICES_RESPONSE = "RevokeOtherTrustedDevicesResponse";
    public static final String E_GET_PREFS_REQUEST = "GetPrefsRequest";
    public static final String E_GET_PREFS_RESPONSE = "GetPrefsResponse";
    public static final String E_GET_INFO_REQUEST = "GetInfoRequest";
    public static final String E_GET_INFO_RESPONSE = "GetInfoResponse";
    public static final String E_GET_ACCOUNT_INFO_REQUEST = "GetAccountInfoRequest";
    public static final String E_GET_ACCOUNT_INFO_RESPONSE = "GetAccountInfoResponse";
    public static final String E_GET_ALL_LOCALES_REQUEST = "GetAllLocalesRequest";
    public static final String E_GET_ALL_LOCALES_RESPONSE = "GetAllLocalesResponse";
    public static final String E_GET_AVAILABLE_LOCALES_REQUEST = "GetAvailableLocalesRequest";
    public static final String E_GET_AVAILABLE_LOCALES_RESPONSE = "GetAvailableLocalesResponse";
    public static final String E_GET_AVAILABLE_SKINS_REQUEST = "GetAvailableSkinsRequest";
    public static final String E_GET_AVAILABLE_SKINS_RESPONSE = "GetAvailableSkinsResponse";
    public static final String E_GET_AVAILABLE_CSV_FORMATS_REQUEST = "GetAvailableCsvFormatsRequest";
    public static final String E_GET_AVAILABLE_CSV_FORMATS_RESPONSE = "GetAvailableCsvFormatsResponse";
    public static final String E_GET_SHARE_INFO_REQUEST = "GetShareInfoRequest";
    public static final String E_GET_SHARE_INFO_RESPONSE = "GetShareInfoResponse";
    public static final String E_GET_WHITE_BLACK_LIST_REQUEST = "GetWhiteBlackListRequest";
    public static final String E_GET_WHITE_BLACK_LIST_RESPONSE = "GetWhiteBlackListResponse";
    public static final String E_MODIFY_PREFS_REQUEST = "ModifyPrefsRequest";
    public static final String E_MODIFY_PREFS_RESPONSE = "ModifyPrefsResponse";
    public static final String E_MODIFY_PROPERTIES_REQUEST = "ModifyPropertiesRequest";
    public static final String E_MODIFY_PROPERTIES_RESPONSE = "ModifyPropertiesResponse";
    public static final String E_MODIFY_WHITE_BLACK_LIST_REQUEST = "ModifyWhiteBlackListRequest";
    public static final String E_MODIFY_WHITE_BLACK_LIST_RESPONSE = "ModifyWhiteBlackListResponse";
    public static final String E_MODIFY_ZIMLET_PREFS_REQUEST = "ModifyZimletPrefsRequest";
    public static final String E_MODIFY_ZIMLET_PREFS_RESPONSE = "ModifyZimletPrefsResponse";

    // GAL
    public static final String E_AUTO_COMPLETE_GAL_REQUEST = "AutoCompleteGalRequest";
    public static final String E_AUTO_COMPLETE_GAL_RESPONSE = "AutoCompleteGalResponse";
    public static final String E_SEARCH_CALENDAR_RESOURCES_REQUEST = "SearchCalendarResourcesRequest";
    public static final String E_SEARCH_CALENDAR_RESOURCES_RESPONSE = "SearchCalendarResourcesResponse";
    public static final String E_SEARCH_GAL_REQUEST = "SearchGalRequest";
    public static final String E_SEARCH_GAL_RESPONSE = "SearchGalResponse";
    public static final String E_SYNC_GAL_REQUEST = "SyncGalRequest";
    public static final String E_SYNC_GAL_RESPONSE = "SyncGalResponse";

    // identities
    public static final String E_CREATE_IDENTITY_REQUEST = "CreateIdentityRequest";
    public static final String E_CREATE_IDENTITY_RESPONSE = "CreateIdentityResponse";
    public static final String E_GET_IDENTITIES_REQUEST = "GetIdentitiesRequest";
    public static final String E_GET_IDENTITIES_RESPONSE = "GetIdentitiesResponse";
    public static final String E_MODIFY_IDENTITY_REQUEST = "ModifyIdentityRequest";
    public static final String E_MODIFY_IDENTITY_RESPONSE = "ModifyIdentityResponse";
    public static final String E_DELETE_IDENTITY_REQUEST = "DeleteIdentityRequest";
    public static final String E_DELETE_IDENTITY_RESPONSE = "DeleteIdentityResponse";

    // signatures
    public static final String E_CREATE_SIGNATURE_REQUEST = "CreateSignatureRequest";
    public static final String E_CREATE_SIGNATURE_RESPONSE = "CreateSignatureResponse";
    public static final String E_GET_SIGNATURES_REQUEST = "GetSignaturesRequest";
    public static final String E_GET_SIGNATURES_RESPONSE = "GetSignaturesResponse";
    public static final String E_MODIFY_SIGNATURE_REQUEST = "ModifySignatureRequest";
    public static final String E_MODIFY_SIGNATURE_RESPONSE = "ModifySignatureResponse";
    public static final String E_DELETE_SIGNATURE_REQUEST = "DeleteSignatureRequest";
    public static final String E_DELETE_SIGNATURE_RESPONSE = "DeleteSignatureResponse";

    // distribution list
    public static final String E_CREATE_DISTRIBUTION_LIST_REQUEST = "CreateDistributionListRequest";
    public static final String E_CREATE_DISTRIBUTION_LIST_RESPONSE = "CreateDistributionListResponse";
    public static final String E_DISTRIBUTION_LIST_ACTION_REQUEST = "DistributionListActionRequest";
    public static final String E_DISTRIBUTION_LIST_ACTION_RESPONSE = "DistributionListActionResponse";
    public static final String E_GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST = "GetAccountDistributionListsRequest";
    public static final String E_GET_ACCOUNT_DISTRIBUTION_LISTS_RESPONSE = "GetAccountDistributionListsResponse";
    public static final String E_GET_DISTRIBUTION_LIST_REQUEST = "GetDistributionListRequest";
    public static final String E_GET_DISTRIBUTION_LIST_RESPONSE = "GetDistributionListResponse";
    public static final String E_GET_DISTRIBUTION_LIST_MEMBERS_REQUEST = "GetDistributionListMembersRequest";
    public static final String E_GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE = "GetDistributionListMembersResponse";
    public static final String E_SUBSCRIBE_DISTRIBUTION_LIST_REQUEST = "SubscribeDistributionListRequest";
    public static final String E_SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE = "SubscribeDistributionListResponse";

    // rights
    public static final String E_CHECK_RIGHTS_REQUEST = "CheckRightsRequest";
    public static final String E_CHECK_RIGHTS_RESPONSE = "CheckRightsResponse";
    public static final String E_DISCOVER_RIGHTS_REQUEST = "DiscoverRightsRequest";
    public static final String E_DISCOVER_RIGHTS_RESPONSE = "DiscoverRightsResponse";
    public static final String E_GET_RIGHTS_REQUEST = "GetRightsRequest";
    public static final String E_GET_RIGHTS_RESPONSE = "GetRightsResponse";
    public static final String E_GRANT_RIGHTS_REQUEST = "GrantRightsRequest";
    public static final String E_GRANT_RIGHTS_RESPONSE = "GrantRightsResponse";
    public static final String E_REVOKE_RIGHTS_REQUEST = "RevokeRightsRequest";
    public static final String E_REVOKE_RIGHTS_RESPONSE = "RevokeRightsResponse";

    // system
    public static final String E_GET_VERSION_INFO_REQUEST = "GetVersionInfoRequest";
    public static final String E_GET_VERSION_INFO_RESPONSE = "GetVersionInfoResponse";

    // SMIME
    public static final String E_GET_SMIME_PUBLIC_CERTS_REQUEST = "GetSMIMEPublicCertsRequest";
    public static final String E_GET_SMIME_PUBLIC_CERTS_RESPONSE = "GetSMIMEPublicCertsResponse";

    // ZimbraLicenseExtension - LicenseService
    public static final String E_CHECK_LICENSE_REQUEST = "CheckLicenseRequest";
    public static final String E_CHECK_LICENSE_RESPONSE = "CheckLicenseResponse";

    // Zimbra Mobile Gateway
    public static final String E_BOOTSTRAP_MOBILE_GATEWAY_APP_REQUEST = "BootstrapMobileGatewayAppRequest";
    public static final String E_BOOTSTRAP_MOBILE_GATEWAY_APP_RESPONSE = "BootstrapMobileGatewayAppResponse";
    public static final String E_RENEW_MOBILE_GATEWAY_APP_TOKEN_REQUEST = "RenewMobileGatewayAppTokenRequest";
    public static final String E_RENEW_MOBILE_GATEWAY_APP_TOKEN_RESPONSE = "RenewMobileGatewayAppTokenResponse";
    public static final String E_REGISTER_MOBILE_GATEWAY_APP_REQUEST = "RegisterMobileGatewayAppRequest";
    public static final String E_REGISTER_MOBILE_GATEWAY_APP_RESPONSE = "RegisterMobileGatewayAppResponse";
    public static final String E_GET_GCM_SENDER_ID_REQUEST = "GetGcmSenderIdRequest";
    public static final String E_GET_GCM_SENDER_ID_RESPONSE = "GetGcmSenderIdResponse";

    //OAuth
    public static final String E_OAUTH_CONSUMER = "OAuthConsumer";
    public static final String E_GET_OAUTH_CONSUMERS_REQUEST = "GetOAuthConsumersRequest";
    public static final String E_GET_OAUTH_CONSUMERS_RESPONSE = "GetOAuthConsumersResponse";
    public static final String E_REVOKE_OAUTH_CONSUMER_REQUEST = "RevokeOAuthConsumerRequest";
    public static final String E_REVOKE_OAUTH_CONSUMER_RESPONSE = "RevokeOAuthConsumerResponse";

    //AddressList
    public static final String E_GET_ALL_ADDRESS_LISTS_REQUEST = "GetAllAddressListsRequest";
    public static final String E_GET_ALL_ADDRESS_LISTS_RESPONSE = "GetAllAddressListsResponse";
    public static final String E_ADDRESS_LISTS = "addressLists";
    public static final String E_ADDRESS_LIST = "addressList";
    public static final String A_DESCRIPTION = "description";
    public static final QName GET_ALL_ADDRESS_LISTS_REQUEST = QName.get(E_GET_ALL_ADDRESS_LISTS_REQUEST, NAMESPACE);
    public static final QName GET_ALL_ADDRESS_LISTS_RESPONSE = QName.get(E_GET_ALL_ADDRESS_LISTS_RESPONSE, NAMESPACE);
    public static final String E_GET_ADDRESS_LIST_MEMBERS_REQUEST = "GetAddressListMembersRequest";
    public static final String E_GET_ADDRESS_LIST_MEMBERS_RESPONSE = "GetAddressListMembersResponse";
    public static final String E_ADDRESS_LIST_MEMBERS = "alm";
    public static final String E_ADDRESS_LIST_MEMBER = "member";
    public static final String A_COUNT_ONLY = "countOnly";

    // auth
    public static final QName AUTH_REQUEST = QName.get(E_AUTH_REQUEST, NAMESPACE);
    public static final QName AUTH_RESPONSE = QName.get(E_AUTH_RESPONSE, NAMESPACE);
    public static final QName CHANGE_PASSWORD_REQUEST = QName.get(E_CHANGE_PASSWORD_REQUEST, NAMESPACE);
    public static final QName CHANGE_PASSWORD_RESPONSE = QName.get(E_CHANGE_PASSWORD_RESPONSE, NAMESPACE);
    public static final QName CLIENT_INFO_REQUEST = QName.get(E_CLIENT_INFO_REQUEST, NAMESPACE);
    public static final QName CLIENT_INFO_RESPONSE = QName.get(E_CLIENT_INFO_RESPONSE, NAMESPACE);
    public static final QName END_SESSION_REQUEST = QName.get(E_END_SESSION_REQUEST, NAMESPACE);
    public static final QName END_SESSION_RESPONSE = QName.get(E_END_SESSION_RESPONSE, NAMESPACE);
    public static final QName ENABLE_TWO_FACTOR_AUTH_REQUEST = QName.get(E_ENABLE_TWO_FACTOR_AUTH_REQUEST, NAMESPACE);
    public static final QName ENABLE_TWO_FACTOR_AUTH_RESPONSE = QName.get(E_ENABLE_TWO_FACTOR_AUTH_RESPONSE, NAMESPACE);
    public static final QName DISABLE_TWO_FACTOR_AUTH_REQUEST = QName.get(E_DISABLE_TWO_FACTOR_AUTH_REQUEST, NAMESPACE);
    public static final QName DISABLE_TWO_FACTOR_AUTH_RESPONSE = QName.get(E_DISABLE_TWO_FACTOR_AUTH_RESPONSE, NAMESPACE);
    public static final QName CREATE_APP_SPECIFIC_PASSWORD_REQUEST = QName.get(E_CREATE_APP_SPECIFIC_PASSWORD_REQUEST, NAMESPACE);
    public static final QName CREATE_APP_SPECIFIC_PASSWORD_RESPONSE = QName.get(E_CREATE_APP_SPECIFIC_PASSWORD_RESPONSE, NAMESPACE);
    public static final QName REVOKE_APP_SPECIFIC_PASSWORD_REQUEST = QName.get(E_REVOKE_APP_SPECIFIC_PASSWORD_REQUEST, NAMESPACE);
    public static final QName REVOKE_APP_SPECIFIC_PASSWORD_RESPONSE = QName.get(E_REVOKE_APP_SPECIFIC_PASSWORD_RESPONSE, NAMESPACE);
    public static final QName GET_APP_SPECIFIC_PASSWORDS_REQUEST = QName.get(E_GET_APP_SPECIFIC_PASSWORDS_REQUEST, NAMESPACE);
    public static final QName GET_APP_SPECIFIC_PASSWORDS_RESPONSE = QName.get(E_GET_APP_SPECIFIC_PASSWORDS_RESPONSE, NAMESPACE);
    public static final QName GET_SCRATCH_CODES_REQUEST = QName.get(E_GET_SCRATCH_CODES_REQUEST, NAMESPACE);
    public static final QName GET_SCRATCH_CODES_RESPONSE = QName.get(E_GET_SCRATCH_CODES_RESPONSE, NAMESPACE);
    public static final QName GENERATE_SCRATCH_CODES_REQUEST = QName.get(E_GENERATE_SCRATCH_CODES_REQUEST, NAMESPACE);
    public static final QName GENERATE_SCRATCH_CODES_RESPONSE = QName.get(E_GENERATE_SCRATCH_CODES_RESPONSE, NAMESPACE);
    public static final QName GET_TRUSTED_DEVICES_REQUEST = QName.get(E_GET_TRUSTED_DEVICES_REQUEST, NAMESPACE);
    public static final QName GET_TRUSTED_DEVICES_RESPONSE = QName.get(E_GET_TRUSTED_DEVICES_RESPONSE, NAMESPACE);
    public static final QName REVOKE_TRUSTED_DEVICE_REQUEST = QName.get(E_REVOKE_TRUSTED_DEVICE_REQUEST, NAMESPACE);
    public static final QName REVOKE_TRUSTED_DEVICE_RESPONSE = QName.get(E_REVOKE_TRUSTED_DEVICE_RESPONSE, NAMESPACE);
    public static final QName REVOKE_OTHER_TRUSTED_DEVICES_REQUEST = QName.get(E_REVOKE_OTHER_TRUSTED_DEVICES_REQUEST, NAMESPACE);
    public static final QName REVOKE_OTHER_TRUSTED_DEVICES_RESPONSE = QName.get(E_REVOKE_OTHER_TRUSTED_DEVICES_RESPONSE, NAMESPACE);

    // prefs
    public static final QName GET_PREFS_REQUEST = QName.get(E_GET_PREFS_REQUEST, NAMESPACE);
    public static final QName GET_PREFS_RESPONSE = QName.get(E_GET_PREFS_RESPONSE, NAMESPACE);
    public static final QName GET_INFO_REQUEST = QName.get(E_GET_INFO_REQUEST, NAMESPACE);
    public static final QName GET_INFO_RESPONSE = QName.get(E_GET_INFO_RESPONSE, NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_REQUEST = QName.get(E_GET_ACCOUNT_INFO_REQUEST, NAMESPACE);
    public static final QName GET_ACCOUNT_INFO_RESPONSE = QName.get(E_GET_ACCOUNT_INFO_RESPONSE, NAMESPACE);
    public static final QName GET_ALL_LOCALES_REQUEST = QName.get(E_GET_ALL_LOCALES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_LOCALES_RESPONSE = QName.get(E_GET_ALL_LOCALES_RESPONSE, NAMESPACE);
    public static final QName GET_AVAILABLE_LOCALES_REQUEST = QName.get(E_GET_AVAILABLE_LOCALES_REQUEST, NAMESPACE);
    public static final QName GET_AVAILABLE_LOCALES_RESPONSE = QName.get(E_GET_AVAILABLE_LOCALES_RESPONSE, NAMESPACE);
    public static final QName GET_AVAILABLE_SKINS_REQUEST = QName.get(E_GET_AVAILABLE_SKINS_REQUEST, NAMESPACE);
    public static final QName GET_AVAILABLE_SKINS_RESPONSE = QName.get(E_GET_AVAILABLE_SKINS_RESPONSE, NAMESPACE);
    public static final QName GET_AVAILABLE_CSV_FORMATS_REQUEST = QName.get(E_GET_AVAILABLE_CSV_FORMATS_REQUEST, NAMESPACE);
    public static final QName GET_AVAILABLE_CSV_FORMATS_RESPONSE = QName.get(E_GET_AVAILABLE_CSV_FORMATS_RESPONSE, NAMESPACE);
    public static final QName GET_SHARE_INFO_REQUEST = QName.get(E_GET_SHARE_INFO_REQUEST, NAMESPACE);
    public static final QName GET_SHARE_INFO_RESPONSE = QName.get(E_GET_SHARE_INFO_RESPONSE, NAMESPACE);
    public static final QName GET_WHITE_BLACK_LIST_REQUEST = QName.get(E_GET_WHITE_BLACK_LIST_REQUEST, NAMESPACE);
    public static final QName GET_WHITE_BLACK_LIST_RESPONSE = QName.get(E_GET_WHITE_BLACK_LIST_RESPONSE, NAMESPACE);
    public static final QName MODIFY_PREFS_REQUEST = QName.get(E_MODIFY_PREFS_REQUEST, NAMESPACE);
    public static final QName MODIFY_PREFS_RESPONSE = QName.get(E_MODIFY_PREFS_RESPONSE, NAMESPACE);
    public static final QName MODIFY_PROPERTIES_REQUEST = QName.get(E_MODIFY_PROPERTIES_REQUEST, NAMESPACE);
    public static final QName MODIFY_PROPERTIES_RESPONSE = QName.get(E_MODIFY_PROPERTIES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_WHITE_BLACK_LIST_REQUEST = QName.get(E_MODIFY_WHITE_BLACK_LIST_REQUEST, NAMESPACE);
    public static final QName MODIFY_WHITE_BLACK_LIST_RESPONSE = QName.get(E_MODIFY_WHITE_BLACK_LIST_RESPONSE, NAMESPACE);
    public static final QName MODIFY_ZIMLET_PREFS_REQUEST = QName.get(E_MODIFY_ZIMLET_PREFS_REQUEST, NAMESPACE);
    public static final QName MODIFY_ZIMLET_PREFS_RESPONSE = QName.get(E_MODIFY_ZIMLET_PREFS_RESPONSE, NAMESPACE);

    // GAL
    public static final QName AUTO_COMPLETE_GAL_REQUEST = QName.get(E_AUTO_COMPLETE_GAL_REQUEST, NAMESPACE);
    public static final QName AUTO_COMPLETE_GAL_RESPONSE = QName.get(E_AUTO_COMPLETE_GAL_RESPONSE, NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_REQUEST = QName.get(E_SEARCH_CALENDAR_RESOURCES_REQUEST, NAMESPACE);
    public static final QName SEARCH_CALENDAR_RESOURCES_RESPONSE = QName.get(E_SEARCH_CALENDAR_RESOURCES_RESPONSE, NAMESPACE);
    public static final QName SEARCH_GAL_REQUEST = QName.get(E_SEARCH_GAL_REQUEST, NAMESPACE);
    public static final QName SEARCH_GAL_RESPONSE = QName.get(E_SEARCH_GAL_RESPONSE, NAMESPACE);
    public static final QName SYNC_GAL_REQUEST = QName.get(E_SYNC_GAL_REQUEST, NAMESPACE);
    public static final QName SYNC_GAL_RESPONSE = QName.get(E_SYNC_GAL_RESPONSE, NAMESPACE);

    // identities
    public static final QName CREATE_IDENTITY_REQUEST = QName.get(E_CREATE_IDENTITY_REQUEST, NAMESPACE);
    public static final QName CREATE_IDENTITY_RESPONSE = QName.get(E_CREATE_IDENTITY_RESPONSE, NAMESPACE);
    public static final QName GET_IDENTITIES_REQUEST = QName.get(E_GET_IDENTITIES_REQUEST, NAMESPACE);
    public static final QName GET_IDENTITIES_RESPONSE = QName.get(E_GET_IDENTITIES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_IDENTITY_REQUEST = QName.get(E_MODIFY_IDENTITY_REQUEST, NAMESPACE);
    public static final QName MODIFY_IDENTITY_RESPONSE = QName.get(E_MODIFY_IDENTITY_RESPONSE, NAMESPACE);
    public static final QName DELETE_IDENTITY_REQUEST = QName.get(E_DELETE_IDENTITY_REQUEST, NAMESPACE);
    public static final QName DELETE_IDENTITY_RESPONSE = QName.get(E_DELETE_IDENTITY_RESPONSE, NAMESPACE);

    // signatures
    public static final QName CREATE_SIGNATURE_REQUEST = QName.get(E_CREATE_SIGNATURE_REQUEST, NAMESPACE);
    public static final QName CREATE_SIGNATURE_RESPONSE = QName.get(E_CREATE_SIGNATURE_RESPONSE, NAMESPACE);
    public static final QName GET_SIGNATURES_REQUEST = QName.get(E_GET_SIGNATURES_REQUEST, NAMESPACE);
    public static final QName GET_SIGNATURES_RESPONSE = QName.get(E_GET_SIGNATURES_RESPONSE, NAMESPACE);
    public static final QName MODIFY_SIGNATURE_REQUEST = QName.get(E_MODIFY_SIGNATURE_REQUEST, NAMESPACE);
    public static final QName MODIFY_SIGNATURE_RESPONSE = QName.get(E_MODIFY_SIGNATURE_RESPONSE, NAMESPACE);
    public static final QName DELETE_SIGNATURE_REQUEST = QName.get(E_DELETE_SIGNATURE_REQUEST, NAMESPACE);
    public static final QName DELETE_SIGNATURE_RESPONSE = QName.get(E_DELETE_SIGNATURE_RESPONSE, NAMESPACE);

    // distribution list
    public static final QName CREATE_DISTRIBUTION_LIST_REQUEST = QName.get(E_CREATE_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName CREATE_DISTRIBUTION_LIST_RESPONSE = QName.get(E_CREATE_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);
    public static final QName DISTRIBUTION_LIST_ACTION_REQUEST = QName.get(E_DISTRIBUTION_LIST_ACTION_REQUEST, NAMESPACE);
    public static final QName DISTRIBUTION_LIST_ACTION_RESPONSE = QName.get(E_DISTRIBUTION_LIST_ACTION_RESPONSE, NAMESPACE);
    public static final QName GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST = QName.get(E_GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST, NAMESPACE);
    public static final QName GET_ACCOUNT_DISTRIBUTION_LISTS_RESPONSE = QName.get(E_GET_ACCOUNT_DISTRIBUTION_LISTS_RESPONSE, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_REQUEST = QName.get(E_GET_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_RESPONSE = QName.get(E_GET_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_MEMBERS_REQUEST = QName.get(E_GET_DISTRIBUTION_LIST_MEMBERS_REQUEST, NAMESPACE);
    public static final QName GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE = QName.get(E_GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE, NAMESPACE);
    public static final QName SUBSCRIBE_DISTRIBUTION_LIST_REQUEST = QName.get(E_SUBSCRIBE_DISTRIBUTION_LIST_REQUEST, NAMESPACE);
    public static final QName SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE = QName.get(E_SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE, NAMESPACE);

    // rights
    public static final QName CHECK_RIGHTS_REQUEST = QName.get(E_CHECK_RIGHTS_REQUEST, NAMESPACE);
    public static final QName CHECK_RIGHTS_RESPONSE = QName.get(E_CHECK_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName DISCOVER_RIGHTS_REQUEST = QName.get(E_DISCOVER_RIGHTS_REQUEST, NAMESPACE);
    public static final QName DISCOVER_RIGHTS_RESPONSE = QName.get(E_DISCOVER_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName GET_RIGHTS_REQUEST = QName.get(E_GET_RIGHTS_REQUEST, NAMESPACE);
    public static final QName GET_RIGHTS_RESPONSE = QName.get(E_GET_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName GRANT_RIGHTS_REQUEST = QName.get(E_GRANT_RIGHTS_REQUEST, NAMESPACE);
    public static final QName GRANT_RIGHTS_RESPONSE = QName.get(E_GRANT_RIGHTS_RESPONSE, NAMESPACE);
    public static final QName REVOKE_RIGHTS_REQUEST = QName.get(E_REVOKE_RIGHTS_REQUEST, NAMESPACE);
    public static final QName REVOKE_RIGHTS_RESPONSE = QName.get(E_REVOKE_RIGHTS_RESPONSE, NAMESPACE);

    // system
    public static final QName GET_VERSION_INFO_REQUEST = QName.get(E_GET_VERSION_INFO_REQUEST, NAMESPACE);
    public static final QName GET_VERSION_INFO_RESPONSE = QName.get(E_GET_VERSION_INFO_RESPONSE, NAMESPACE);

    // SMIME
    public static final QName GET_SMIME_PUBLIC_CERTS_REQUEST = QName.get(E_GET_SMIME_PUBLIC_CERTS_REQUEST, NAMESPACE);
    public static final QName GET_SMIME_PUBLIC_CERTS_RESPONSE = QName.get(E_GET_SMIME_PUBLIC_CERTS_RESPONSE, NAMESPACE);

    // ZimbraLicenseExtension - LicenseService
    public static final QName CHECK_LICENSE_REQUEST = QName.get(E_CHECK_LICENSE_REQUEST, NAMESPACE);
    public static final QName CHECK_LICENSE_RESPONSE = QName.get(E_CHECK_LICENSE_RESPONSE, NAMESPACE);

    // Zimbra Mobile Gateway
    public static final QName BOOTSTRAP_MOBILE_GATEWAY_APP_REQUEST = QName.get(E_BOOTSTRAP_MOBILE_GATEWAY_APP_REQUEST, NAMESPACE);
    public static final QName BOOTSTRAP_MOBILE_GATEWAY_APP_RESPONSE = QName.get(E_BOOTSTRAP_MOBILE_GATEWAY_APP_RESPONSE, NAMESPACE);
    public static final QName RENEW_MOBILE_GATEWAY_APP_TOKEN_REQUEST = QName.get(E_RENEW_MOBILE_GATEWAY_APP_TOKEN_REQUEST, NAMESPACE);
    public static final QName RENEW_MOBILE_GATEWAY_APP_TOKEN_RESPONSE = QName.get(E_RENEW_MOBILE_GATEWAY_APP_TOKEN_RESPONSE, NAMESPACE);
    public static final QName REGISTER_MOBILE_GATEWAY_APP_REQUEST = QName.get(E_REGISTER_MOBILE_GATEWAY_APP_REQUEST, NAMESPACE);
    public static final QName REGISTER_MOBILE_GATEWAY_APP_RESPONSE = QName.get(E_REGISTER_MOBILE_GATEWAY_APP_RESPONSE, NAMESPACE);
    public static final QName GET_GCM_SENDER_ID_REQUEST = QName.get(E_GET_GCM_SENDER_ID_REQUEST, NAMESPACE);
    public static final QName GET_GCM_SENDER_ID_RESPONSE = QName.get(E_GET_GCM_SENDER_ID_RESPONSE, NAMESPACE);

    //OAuth
    public static final QName GET_OAUTH_CONSUMERS_REQUEST = QName.get(E_GET_OAUTH_CONSUMERS_REQUEST, NAMESPACE);
    public static final QName GET_OAUTH_CONSUMERS_RESPONSE = QName.get(E_GET_OAUTH_CONSUMERS_RESPONSE, NAMESPACE);
    public static final QName REVOKE_OAUTH_CONSUMER_REQUEST = QName.get(E_REVOKE_OAUTH_CONSUMER_REQUEST, NAMESPACE);
    public static final QName REVOKE_OAUTH_CONSUMER_RESPONSE = QName.get(E_REVOKE_OAUTH_CONSUMER_RESPONSE, NAMESPACE);

    //HAB
    public static final String E_GET_HAB_REQUEST = "GetHABRequest";
    public static final String E_GET_HAB_RESPONSE = "GetHABResponse";
    public static final QName GET_HAB_REQUEST = QName.get( E_GET_HAB_REQUEST, NAMESPACE);
    public static final QName GET_HAB_RESPONSE = QName.get(E_GET_HAB_RESPONSE, NAMESPACE);

    public static final QName GET_ADDRESS_LIST_MEMBERS_REQUEST = QName.get(E_GET_ADDRESS_LIST_MEMBERS_REQUEST, NAMESPACE);
    public static final QName GET_ADDRESS_LIST_MEMBERS_RESPONSE = QName.get(E_GET_ADDRESS_LIST_MEMBERS_RESPONSE, NAMESPACE);

    public static final String E_ACTION = "action";
    public static final String E_ALIAS = "alias";
    public static final String E_ADMIN_DELEGATED = "adminDelegated";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_TRUSTED_TOKEN = "trustedToken";
    public static final String E_CRUMB = "crumb";
    public static final String E_REFERRAL = "refer";
    public static final String E_LIFETIME = "lifetime";
    public static final String E_TRUST_LIFETIME = "trustLifetime";
    public static final String E_ACCOUNT = "account";
    public static final String E_CALENDAR_RESOURCE = "calresource";
    public static final String E_CERT = "cert";
    public static final String E_CERTS = "certs";
    public static final String E_DL = "dl";
    public static final String E_DL_OWNER = "owner";
    public static final String E_DL_OWNERS = "owners";
    public static final String E_DLM = "dlm";
    public static final String E_DL_SUBS_REQ = "subsReq";
    public static final String E_EMAIL = "email";
    public static final String E_EMAIL2 = "email2";
    public static final String E_EMAIL3 = "email3";
    public static final String E_VERSION = "version";
    public static final String E_NAME = "name";
    public static final String E_NEW_NAME = "newName";
    public static final String E_ID = "id";
    public static final String E_PROFILE_IMAGE_ID = "profileImageId";
    public static final String E_PASSWORD = "password";
    public static final String E_RECOVERY_CODE = "recoveryCode";
    public static final String E_OLD_PASSWORD = "oldPassword";
    public static final String A_SECTIONS = "sections";
    public static final String E_PREF = "pref";
    public static final String E_PREFS = "prefs";
    public static final String E_ATTR = "attr";
    public static final String E_ATTRS = "attrs";
    public static final String E_QUOTA_USED = "used";
    public static final String E_DS_QUOTA = "dsQuota";
    public static final String E_DS_TOTAL_QUOTA = "dsTotalQuota";
    public static final String E_PREVIOUS_SESSION = "prevSession";
    public static final String E_LAST_ACCESS = "accessed";
    public static final String E_RECENT_MSGS = "recent";
    public static final String E_ZIMLET = "zimlet";
    public static final String E_ZIMLETS = "zimlets";
    public static final String E_ZIMLET_CONTEXT = "zimletContext";
    public static final String E_PROPERTY = "prop";
    public static final String E_PROPERTIES = "props";
    public static final String E_SOAP_URL = "soapURL";
    public static final String E_ADMIN_URL = "adminURL";
    public static final String E_PUBLIC_URL = "publicURL";
    public static final String E_COMMUNITY_URL = "communityURL";
    public static final String E_CHANGE_PASSWORD_URL = "changePasswordURL";
    public static final String E_PREAUTH = "preauth";
    public static final String E_A = "a";
    public static final String E_ADDR = "addr";
    public static final String E_ENTRY_SEARCH_FILTER = "searchFilter";
    public static final String E_ENTRY_SEARCH_FILTER_MULTICOND = "conds";
    public static final String E_ENTRY_SEARCH_FILTER_SINGLECOND = "cond";
    public static final String E_LOCALE = "locale";
    public static final String E_VIRTUAL_HOST = "virtualHost";
    public static final String E_SKIN = "skin";
    public static final String E_LICENSE = "license";
    public static final String E_HAB_ROOTS = "habRoots";
    public static final String E_HAB = "hab";
    public static final String E_IDENTITIES = "identities";
    public static final String E_SIGNATURES = "signatures";
    public static final String E_IDENTITY = "identity";
    public static final String E_SIGNATURE = "signature";
    public static final String E_DATA_SOURCES = "dataSources";
    public static final String E_DATA_SOURCE = "dataSource";
    public static final String E_DATA_SOURCE_USAGE = "dataSourceUsage";
    public static final String E_CHILD_ACCOUNTS = "childAccounts";
    public static final String E_CHILD_ACCOUNT = "childAccount";
    public static final String E_CONTENT = "content";
    public static final String E_REQUESTED_SKIN = "requestedSkin";
    public static final String E_REST = "rest";
    public static final String E_CSV = "csv";
    public static final String E_COS = "cos";
    public static final String E_WHITE_LIST = "whiteList";
    public static final String E_BLACK_LIST = "blackList";
    public static final String E_GRANTEE = "grantee";
    public static final String E_OWNER = "owner";
    public static final String E_SENDER = "sender";
    public static final String E_SHARE = "share";
    public static final String E_CONTACT_ID = "cid";
    public static final String E_STORE = "store";
    public static final String E_BOSH_URL = "boshURL";
    public static final String E_SUBSCRIPTION = "sub";
    public static final String E_IS_TRACKING_IMAP = "isTrackingIMAP";
    public static final String E_DRYRUN = "dryRun";
    public static final String E_GET_PASSWORD_RULES = "getPasswordRules";
    public static final String E_CANCEL_RESET_PASSWORD = "cancelResetPassword";

    public static final String A_ACTIVE = "active";
    public static final String A_ATTRS = "attrs";
    public static final String A_ADDR = "addr";
    public static final String A_BCC_OWNERS = "bccOwners";
    public static final String A_DIRECT_ONLY = "directOnly";
    public static final String A_DYNAMIC = "dynamic";
    public static final String A_HOSTNAME = "hostname";
    public static final String A_KEY = "key";
    public static final String A_N = "n";
    public static final String A_NAME = "name";
    public static final String A_ID = "id";
    public static final String A_ID_ONLY = "idOnly";
    public static final String A_IS_DL = "isDL";
    public static final String A_IS_EXTERNAL = "isExternal";
    public static final String A_IS_MEMBER = "isMember";
    public static final String A_IS_OWNER = "isOwner";
    public static final String A_BY = "by";
    public static final String A_TYPE = "type";
    public static final String A_LIMIT = "limit";
    public static final String A_LOCAL_NAME = "localName";
    public static final String A_MEMBER_OF = "memberOf";
    public static final String A_MORE = "more";
    public static final String A_NEED_IS_OWNER = "needIsOwner";
    public static final String A_NEED_IS_MEMBER = "needIsMember";
    public static final String A_NEED_OWNERS = "needOwners";
    public static final String A_NEED_RIGHTS = "needRights";
    public static final String A_OWNER_OF = "ownerOf";
    public static final String A_PERSIST_AUTH_TOKEN_COOKIE = "persistAuthTokenCookie";
    public static final String A_TOTAL = "total";
    public static final String A_ZIMLET = "zimlet";
    public static final String A_ZIMLET_BASE_URL = "baseUrl";
    public static final String A_ZIMLET_PRIORITY = "priority";
    public static final String A_ZIMLET_PRESENCE = "presence";
    public static final String A_TIMESTAMP = "timestamp";
    public static final String A_TOKENIZE_KEY = "tokenizeKey";
    public static final String A_EXPIRES = "expires";
    public static final String A_OP = "op";
    public static final String A_REF = "ref";
    public static final String A_STATUS = "status";
    public static final String A_SORT_BY = "sortBy";
    public static final String A_SORT_ASCENDING = "sortAscending";
    public static final String A_UTF8 = "utf8";
    public static final String A_VISIBLE = "visible";
    public static final String A_PERM_DENIED = "pd";
    public static final String A_EMAIL = "email";
    public static final String A_FIELD = "field";
    public static final String A_STORE = "store";
    public static final String A_SMIME_STORE_LOOKUP_OPT = "storeLookupOpt";
    public static final String A_SMIME_SOURCE_LOOKUP_OPT = "sourceLookupOpt";
    public static final String A_VIA = "via";
    public static final String A_VERIFY_ACCOUNT = "verifyAccount";
    public static final String A_CSRF_SUPPORT = "csrfTokenSecured";
    public static final String A_GET_COUNT = "getCount";
    public static final String A_TOKEN_TYPE = "tokenType";
    public static final String E_JWT_TOKEN = "jwtToken";
    public static final String A_OFFSET = "offset";
    public static final String A_IGNORE_SAME_SITE_COOKIE = "ignoreSameSite";

    // account ACLs
    public static final String A_ACCESSKEY = "key";
    public static final String A_ALLOW = "allow";
    public static final String A_CHECK_GRANTEE_TYPE = "chkgt";
    public static final String A_DENY = "deny";
    public static final String A_DISPLAY = "d";
    public static final String A_GRANT_TYPE = "gt";
    public static final String A_PASSWORD = "pw";
    public static final String A_RIGHT = "right";
    public static final String A_TARGET_BY = "by";
    public static final String A_TARGET_TYPE = "type";
    public static final String A_ZIMBRA_ID = "zid";
    public static final String E_ACE = "ace";
    public static final String E_ACL = "acl";
    public static final String E_GRANT = "grant";
    public static final String E_RIGHT = "right";
    public static final String E_RIGHTS = "rights";
    public static final String E_TARGET = "target";
    public static final String E_TARGETS = "targets";

    // gal
    public static final String A_IS_GROUP = "isGroup";
    public static final String A_EXP = "exp";
    public static final String A_NEED_EXP = "needExp";
    public static final String A_NEED_SMIME_CERTS = "needSMIMECerts";
    public static final String A_GAL_ACCOUNT_ID = "galAcctId";
    public static final String A_GAL_ACCOUNT_PROXIED = "galAcctProxied";
    public static final String A_PAGINATION_SUPPORTED = "paginationSupported";
    public static final String A_OFFSET_INTERNAL = "_offset"; // for server internal use only
    public static final String A_LIMIT_INTERNAL = "_limit";   // for serer internal use only

    // share info
    public static final String A_FOLDER_ID = "folderId";
    public static final String A_FOLDER_UUID = "folderUuid";
    public static final String A_FOLDER_PATH = "folderPath";
    public static final String A_GRANTEE_ID = "granteeId";
    public static final String A_GRANTEE_NAME = "granteeName";
    public static final String A_GRANTEE_DISPLAY_NAME = "granteeDisplayName";
    public static final String A_GRANTEE_TYPE = "granteeType";
    public static final String A_INCLUDE_SELF = "includeSelf";
    public static final String A_INTERNAL = "internal";
    public static final String A_OWNER_ID = "ownerId";
    public static final String A_OWNER_EMAIL = "ownerEmail";
    public static final String A_OWNER_DISPLAY_NAME = "ownerName";
    public static final String A_RIGHTS = "rights";
    public static final String A_MOUNTPOINT_ID = "mid";
    // contact search
    public static final String A_DEPARTMENT = "department";


    // calendar resource search
    public static final String A_ENTRY_SEARCH_FILTER_OR = "or";
    public static final String A_ENTRY_SEARCH_FILTER_NEGATION = "not";
    public static final String A_ENTRY_SEARCH_FILTER_ATTR = "attr";
    public static final String A_ENTRY_SEARCH_FILTER_OP = "op";
    public static final String A_ENTRY_SEARCH_FILTER_VALUE = "value";

    // Version info
    public static final String E_VERSION_INFO_INFO = "info";
    public static final String A_VERSION_INFO_VERSION = "version";
    public static final String A_VERSION_INFO_RELEASE = "release";
    public static final String A_VERSION_INFO_DATE = "buildDate";
    public static final String A_VERSION_INFO_HOST = "host";

    // XMPPComponent APIs
    public static final String E_XMPP_COMPONENT = "xmppcomponent";

    // upload limits
    public static final String A_ATTACHMENT_SIZE_LIMIT = "attSizeLimit";
    public static final String A_DOCUMENT_SIZE_LIMIT = "docSizeLimit";

    //zimbra spellcheck
    public static final String A_IS_SPELL_CHECK_AVAILABLE = "isSpellCheckAvailable";

    //end session
    public static final String A_LOG_OFF = "logoff";
    public static final String A_CLEAR_ALL_SOAP_SESSIONS = "all";
    public static final String A_EXCLUDE_CURRENT_SESSION = "excludeCurrent";

    // Zimbra Mobile Gateway
    public static final String E_APP_ID = "appId";
    public static final String E_APP_KEY = "appKey";
    public static final String A_WANT_APP_TOKEN = "wantAppToken";
    public static final String E_ZMG_DEVICE = "zmgDevice";
    public static final String A_DEVICE_ID = "appId";
    public static final String A_REGISTRATION_ID = "registrationId";
    public static final String A_PUSH_PROVIDER = "pushProvider";
    public static final String A_OS_NAME = "osName";
    public static final String A_OS_VERSION = "osVersion";
    public static final String A_MAX_PAYLOAD_SIZE = "maxPayloadSize";
    public static final String E_GCM_SENDER_ID = "gcmSenderId";
    public static final String A_ZMG_PROXY = "zmgProxy";

    //two-factor auth attributes
    public static final String E_TWO_FACTOR_CODE = "twoFactorCode";
    public static final String E_TWO_FACTOR_SCRATCH_CODE = "scratchCode";
    public static final String E_TWO_FACTOR_SCRATCH_CODES = "scratchCodes";
    public static final String A_TRUSTED_DEVICE = "deviceTrusted";
    public static final String E_TWO_FACTOR_CREDENTIALS = "credentials";
    public static final String E_TWO_FACTOR_SECRET = "secret";
    public static final String E_APP_SPECIFIC_PASSWORD_DATA = "passwordData";
    public static final String E_APP_SPECIFIC_PASSWORDS = "appSpecificPasswords";
    public static final String E_MAX_APP_PASSWORDS = "maxAppPasswords";
    public static final String A_APP_NAME = "appName";
    public static final String A_DATE_CREATED = "created";
    public static final String A_DATE_LAST_USED = "lastUsed";
    public static final String A_THIS_DEVICE_TRUSTED = "thisDeviceTrusted";
    public static final String A_NUM_OTHER_TRUSTED_DEVICES = "nOtherDevices";
    public static final String E_DEVICE_ID = "deviceId";
    public static final String A_GENERATE_DEVICE_ID = "generateDeviceId";
    public static final String E_TWO_FACTOR_AUTH_REQUIRED = "twoFactorAuthRequired";
    public static final String E_TRUSTED_DEVICES_ENABLED = "trustedDevicesEnabled";

    //oauth consumer attributes
    public static final String A_ACCESS_TOKEN = "accessToken";
    public static final String A_CONSUMER_APP_NAME = "appName";
    public static final String A_APPROVED_ON = "approvedOn";
    public static final String A_CONSUMER_DEVICE = "device";

    //ext user prov URL metadata constants
    public static final String P_ACCOUNT_ID = "aid";
    public static final String P_FOLDER_ID = "fid";
    public static final String P_LINK_EXPIRY = "exp";
    public static final String P_EMAIL = "email";
    public static final String P_ADDRESS_VERIFICATION = "address-verification";
    public static final String P_ACCOUNT_VERIFICATION = "account-verification";
    public static final String P_CODE = "recovery_code";

    // Password reset feature
    public static final String E_RESET_PASSWORD_REQUEST = "ResetPasswordRequest";
    public static final String E_RESET_PASSWORD_RESPONSE = "ResetPasswordResponse";
    public static final QName RESET_PASSWORD_REQUEST = QName.get(E_RESET_PASSWORD_REQUEST, NAMESPACE);

    //Hab
    public static final String A_HAB_ROOT_GROUP_ID = "habRootGroupId";
    public static final String E_HAB_GROUP = "habGroup";
    public static final String E_HAB_GROUPS = "habGroups";
    public static final String A_ROOT_HAB_GROUP = "rootHabGroup";
    public static final String A_PARENT_HAB_GROUP_ID = "parentHabGroupId";
    public static final String A_HAB_GROUPS= "habGroups";
    public static final String A_HAB_SENIORITY_INDEX = "seniorityIndex";
    public static final String E_HAB_GROUP_MEMBERS = "groupMembers";
    public static final String E_HAB_GROUP_MEMBER = "groupMember";

    // Session activity feature
    public static final String A_SESSION_ID = "sessionId";

    // Client Info
    public static final String E_SKIN_LOGO_APP_BANNER = "zimbraSkinLogoAppBanner";
    public static final String E_SKIN_LOGO_LOGIN_BANNER = "zimbraSkinLogoLoginBanner";
    public static final String E_SKIN_LOGO_URL = "zimbraSkinLogoURL";
    public static final String E_WEB_CLIENT_LOGIN_URL = "zimbraWebClientLoginURL";
    public static final String E_WEB_CLIENT_LOGOUT_URL = "zimbraWebClientLogoutURL";
    public static final String E_WEB_CLIENT_STAY_SIGNED_IN_DISABLED = "zimbraFeatureStaySignedInDisabled";
    public static final String E_WEB_CLIENT_FEATURE_PASSWORD_RESET = "zimbraFeatureResetPasswordStatus";
    public static final String E_SKIN_BACKGROUND_COLOR = "zimbraSkinBackgroundColor";
    public static final String E_SKIN_FOREGROUND_COLOR = "zimbraSkinForegroundColor";
    public static final String E_SKIN_SECONDARY_COLOR = "zimbraSkinSecondaryColor";
    public static final String E_SKIN_SELECTION_COLOR = "zimbraSkinSelectionColor";
    public static final String E_SKIN_FAVICON = "zimbraSkinFavicon";
    public static final String E_HOSTNAME = "hostname";
    
    public static final String URL = "url";
 	public static final String BUCKET_NAME = "bucketName";
 	public static final String REGION = "region";
 	public static final String ACCESS_KEY = "accessKey";
 	public static final String SECRATE_KEY = "secrateKey";
}
