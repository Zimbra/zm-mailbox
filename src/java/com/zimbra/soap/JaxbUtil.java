/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010-2011 Zimbra, Inc.
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

package com.zimbra.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ZimbraLog;

public final class JaxbUtil {

    private static final Class<?>[] MESSAGE_CLASSES;
    private static JAXBContext JAXB_CONTEXT;

    static {
        MESSAGE_CLASSES = new Class<?>[] {
            // zimbraAccount
            com.zimbra.soap.account.message.AuthRequest.class,
            com.zimbra.soap.account.message.AuthResponse.class,
            com.zimbra.soap.account.message.ChangePasswordRequest.class,
            com.zimbra.soap.account.message.ChangePasswordResponse.class,
            com.zimbra.soap.account.message.EndSessionRequest.class,
            com.zimbra.soap.account.message.EndSessionResponse.class,
            com.zimbra.soap.account.message.GetIdentitiesRequest.class,
            com.zimbra.soap.account.message.GetIdentitiesResponse.class,
            com.zimbra.soap.account.message.GetInfoRequest.class,
            com.zimbra.soap.account.message.GetInfoResponse.class,
            com.zimbra.soap.account.message.GetPrefsRequest.class,
            com.zimbra.soap.account.message.GetPrefsResponse.class,
            com.zimbra.soap.account.message.GetSignaturesRequest.class,
            com.zimbra.soap.account.message.GetSignaturesResponse.class,
            com.zimbra.soap.account.message.ModifyPropertiesRequest.class,
            com.zimbra.soap.account.message.ModifyPropertiesResponse.class,

            // zimbraMail
            com.zimbra.soap.mail.message.ExportContactsRequest.class,
            com.zimbra.soap.mail.message.ExportContactsResponse.class,
            com.zimbra.soap.mail.message.GetDataSourcesRequest.class,
            com.zimbra.soap.mail.message.GetDataSourcesResponse.class,
            com.zimbra.soap.mail.message.GetFolderRequest.class,
            com.zimbra.soap.mail.message.GetFolderResponse.class,
            com.zimbra.soap.mail.message.ImportContactsRequest.class,
            com.zimbra.soap.mail.message.ImportContactsResponse.class,

            // zimbraAdmin
            com.zimbra.soap.admin.message.AddAccountAliasRequest.class,
            com.zimbra.soap.admin.message.AddAccountAliasResponse.class,
            com.zimbra.soap.admin.message.AddAccountLoggerRequest.class,
            com.zimbra.soap.admin.message.AddAccountLoggerResponse.class,
            com.zimbra.soap.admin.message.AddDistributionListAliasRequest.class,
            com.zimbra.soap.admin.message.AddDistributionListAliasResponse.class,
            com.zimbra.soap.admin.message.AddDistributionListMemberRequest.class,
            com.zimbra.soap.admin.message.AddDistributionListMemberResponse.class,
            com.zimbra.soap.admin.message.AdminCreateWaitSetRequest.class,
            com.zimbra.soap.admin.message.AdminCreateWaitSetResponse.class,
            com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest.class,
            com.zimbra.soap.admin.message.AdminDestroyWaitSetResponse.class,
            com.zimbra.soap.admin.message.AdminWaitSetRequest.class,
            com.zimbra.soap.admin.message.AdminWaitSetResponse.class,
            com.zimbra.soap.admin.message.AuthRequest.class,
            com.zimbra.soap.admin.message.AuthResponse.class,
            com.zimbra.soap.admin.message.CheckAuthConfigRequest.class,
            com.zimbra.soap.admin.message.CheckAuthConfigResponse.class,
            com.zimbra.soap.admin.message.CheckDirectoryRequest.class,
            com.zimbra.soap.admin.message.CheckDirectoryResponse.class,
            com.zimbra.soap.admin.message.CheckDomainMXRecordRequest.class,
            com.zimbra.soap.admin.message.CheckDomainMXRecordResponse.class,
            com.zimbra.soap.admin.message.CheckGalConfigRequest.class,
            com.zimbra.soap.admin.message.CheckGalConfigResponse.class,
            com.zimbra.soap.admin.message.CheckHealthRequest.class,
            com.zimbra.soap.admin.message.CheckHealthResponse.class,
            com.zimbra.soap.admin.message.CheckHostnameResolveRequest.class,
            com.zimbra.soap.admin.message.CheckHostnameResolveResponse.class,
            com.zimbra.soap.admin.message.CheckPasswordStrengthRequest.class,
            com.zimbra.soap.admin.message.CheckPasswordStrengthResponse.class,
            com.zimbra.soap.admin.message.CheckRightRequest.class,
            com.zimbra.soap.admin.message.CheckRightResponse.class,
            com.zimbra.soap.admin.message.ConfigureZimletRequest.class,
            com.zimbra.soap.admin.message.ConfigureZimletResponse.class,
            com.zimbra.soap.admin.message.CopyCosRequest.class,
            com.zimbra.soap.admin.message.CopyCosResponse.class,
            com.zimbra.soap.admin.message.CountAccountRequest.class,
            com.zimbra.soap.admin.message.CountAccountResponse.class,
            com.zimbra.soap.admin.message.CreateAccountRequest.class,
            com.zimbra.soap.admin.message.CreateAccountResponse.class,
            com.zimbra.soap.admin.message.CreateCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.CreateCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.CreateCosRequest.class,
            com.zimbra.soap.admin.message.CreateCosResponse.class,
            com.zimbra.soap.admin.message.CreateDataSourceRequest.class,
            com.zimbra.soap.admin.message.CreateDataSourceResponse.class,
            com.zimbra.soap.admin.message.CreateDistributionListRequest.class,
            com.zimbra.soap.admin.message.CreateDistributionListResponse.class,
            com.zimbra.soap.admin.message.CreateDomainRequest.class,
            com.zimbra.soap.admin.message.CreateDomainResponse.class,
            com.zimbra.soap.admin.message.CreateGalSyncAccountRequest.class,
            com.zimbra.soap.admin.message.CreateGalSyncAccountResponse.class,
            com.zimbra.soap.admin.message.CreateServerRequest.class,
            com.zimbra.soap.admin.message.CreateServerResponse.class,
            com.zimbra.soap.admin.message.CreateVolumeRequest.class,
            com.zimbra.soap.admin.message.CreateVolumeResponse.class,
            com.zimbra.soap.admin.message.CreateXMPPComponentRequest.class,
            com.zimbra.soap.admin.message.CreateXMPPComponentResponse.class,
            com.zimbra.soap.admin.message.CreateZimletRequest.class,
            com.zimbra.soap.admin.message.CreateZimletResponse.class,
            com.zimbra.soap.admin.message.DelegateAuthRequest.class,
            com.zimbra.soap.admin.message.DelegateAuthResponse.class,
            com.zimbra.soap.admin.message.DeleteAccountRequest.class,
            com.zimbra.soap.admin.message.DeleteAccountResponse.class,
            com.zimbra.soap.admin.message.DeleteCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.DeleteCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.DeleteCosRequest.class,
            com.zimbra.soap.admin.message.DeleteCosResponse.class,
            com.zimbra.soap.admin.message.DeleteDataSourceRequest.class,
            com.zimbra.soap.admin.message.DeleteDataSourceResponse.class,
            com.zimbra.soap.admin.message.DeleteDistributionListRequest.class,
            com.zimbra.soap.admin.message.DeleteDistributionListResponse.class,
            com.zimbra.soap.admin.message.DeleteDomainRequest.class,
            com.zimbra.soap.admin.message.DeleteDomainResponse.class,
            com.zimbra.soap.admin.message.DeleteGalSyncAccountRequest.class,
            com.zimbra.soap.admin.message.DeleteGalSyncAccountResponse.class,
            com.zimbra.soap.admin.message.DeleteMailboxRequest.class,
            com.zimbra.soap.admin.message.DeleteMailboxResponse.class,
            com.zimbra.soap.admin.message.DeleteServerRequest.class,
            com.zimbra.soap.admin.message.DeleteServerResponse.class,
            com.zimbra.soap.admin.message.DeleteVolumeRequest.class,
            com.zimbra.soap.admin.message.DeleteVolumeResponse.class,
            com.zimbra.soap.admin.message.DeleteXMPPComponentRequest.class,
            com.zimbra.soap.admin.message.DeleteXMPPComponentResponse.class,
            com.zimbra.soap.admin.message.DeleteZimletRequest.class,
            com.zimbra.soap.admin.message.DeleteZimletResponse.class,
            com.zimbra.soap.admin.message.DeployZimletRequest.class,
            com.zimbra.soap.admin.message.DeployZimletResponse.class,
            com.zimbra.soap.admin.message.ExportMailboxRequest.class,
            com.zimbra.soap.admin.message.ExportMailboxResponse.class,
            com.zimbra.soap.admin.message.FixCalendarEndTimeRequest.class,
            com.zimbra.soap.admin.message.FixCalendarEndTimeResponse.class,
            com.zimbra.soap.admin.message.FixCalendarPriorityRequest.class,
            com.zimbra.soap.admin.message.FixCalendarPriorityResponse.class,
            com.zimbra.soap.admin.message.FlushCacheRequest.class,
            com.zimbra.soap.admin.message.FlushCacheResponse.class,
            com.zimbra.soap.admin.message.GetAccountInfoRequest.class,
            com.zimbra.soap.admin.message.GetAccountInfoResponse.class,
            com.zimbra.soap.admin.message.GetAccountLoggersRequest.class,
            com.zimbra.soap.admin.message.GetAccountLoggersResponse.class,
            com.zimbra.soap.admin.message.GetAccountMembershipRequest.class,
            com.zimbra.soap.admin.message.GetAccountMembershipResponse.class,
            com.zimbra.soap.admin.message.GetAccountRequest.class,
            com.zimbra.soap.admin.message.GetAccountResponse.class,
            com.zimbra.soap.admin.message.GetAdminConsoleUICompRequest.class,
            com.zimbra.soap.admin.message.GetAdminConsoleUICompResponse.class,
            com.zimbra.soap.admin.message.GetAllAccountLoggersRequest.class,
            com.zimbra.soap.admin.message.GetAllAccountLoggersResponse.class,
            com.zimbra.soap.admin.message.GetAllAccountsRequest.class,
            com.zimbra.soap.admin.message.GetAllAccountsResponse.class,
            com.zimbra.soap.admin.message.GetAllAdminAccountsRequest.class,
            com.zimbra.soap.admin.message.GetAllAdminAccountsResponse.class,
            com.zimbra.soap.admin.message.GetAllCalendarResourcesRequest.class,
            com.zimbra.soap.admin.message.GetAllCalendarResourcesResponse.class,
            com.zimbra.soap.admin.message.GetAllConfigRequest.class,
            com.zimbra.soap.admin.message.GetAllConfigResponse.class,
            com.zimbra.soap.admin.message.GetAllCosRequest.class,
            com.zimbra.soap.admin.message.GetAllCosResponse.class,
            com.zimbra.soap.admin.message.GetAllDistributionListsRequest.class,
            com.zimbra.soap.admin.message.GetAllDistributionListsResponse.class,
            com.zimbra.soap.admin.message.GetAllDomainsRequest.class,
            com.zimbra.soap.admin.message.GetAllDomainsResponse.class,
            com.zimbra.soap.admin.message.GetAllEffectiveRightsRequest.class,
            com.zimbra.soap.admin.message.GetAllEffectiveRightsResponse.class,
            com.zimbra.soap.admin.message.GetAllFreeBusyProvidersRequest.class,
            com.zimbra.soap.admin.message.GetAllFreeBusyProvidersResponse.class,
            com.zimbra.soap.admin.message.GetAllLocalesRequest.class,
            com.zimbra.soap.admin.message.GetAllLocalesResponse.class,
            com.zimbra.soap.admin.message.GetAllMailboxesRequest.class,
            com.zimbra.soap.admin.message.GetAllMailboxesResponse.class,
            com.zimbra.soap.admin.message.GetAllRightsRequest.class,
            com.zimbra.soap.admin.message.GetAllRightsResponse.class,
            com.zimbra.soap.admin.message.GetAllServersRequest.class,
            com.zimbra.soap.admin.message.GetAllServersResponse.class,
            com.zimbra.soap.admin.message.GetAllVolumesRequest.class,
            com.zimbra.soap.admin.message.GetAllVolumesResponse.class,
            com.zimbra.soap.admin.message.GetAllXMPPComponentsRequest.class,
            com.zimbra.soap.admin.message.GetAllXMPPComponentsResponse.class,
            com.zimbra.soap.admin.message.GetAllZimletsRequest.class,
            com.zimbra.soap.admin.message.GetAllZimletsResponse.class,
            com.zimbra.soap.admin.message.GetCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.GetCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.GetConfigRequest.class,
            com.zimbra.soap.admin.message.GetConfigResponse.class,
            com.zimbra.soap.admin.message.GetCosRequest.class,
            com.zimbra.soap.admin.message.GetCosResponse.class,
            com.zimbra.soap.admin.message.GetCreateObjectAttrsRequest.class,
            com.zimbra.soap.admin.message.GetCreateObjectAttrsResponse.class,
            com.zimbra.soap.admin.message.GetCurrentVolumesRequest.class,
            com.zimbra.soap.admin.message.GetCurrentVolumesResponse.class,
            com.zimbra.soap.admin.message.GetDataSourcesRequest.class,
            com.zimbra.soap.admin.message.GetDataSourcesResponse.class,
            com.zimbra.soap.admin.message.GetDelegatedAdminConstraintsRequest.class,
            com.zimbra.soap.admin.message.GetDelegatedAdminConstraintsResponse.class,
            com.zimbra.soap.admin.message.GetDistributionListMembershipRequest.class,
            com.zimbra.soap.admin.message.GetDistributionListMembershipResponse.class,
            com.zimbra.soap.admin.message.GetDistributionListRequest.class,
            com.zimbra.soap.admin.message.GetDistributionListResponse.class,
            com.zimbra.soap.admin.message.GetDomainInfoRequest.class,
            com.zimbra.soap.admin.message.GetDomainInfoResponse.class,
            com.zimbra.soap.admin.message.GetDomainRequest.class,
            com.zimbra.soap.admin.message.GetDomainResponse.class,
            com.zimbra.soap.admin.message.GetEffectiveRightsRequest.class,
            com.zimbra.soap.admin.message.GetEffectiveRightsResponse.class,
            com.zimbra.soap.admin.message.GetFreeBusyQueueInfoRequest.class,
            com.zimbra.soap.admin.message.GetFreeBusyQueueInfoResponse.class,
            com.zimbra.soap.admin.message.GetGrantsRequest.class,
            com.zimbra.soap.admin.message.GetGrantsResponse.class,
            com.zimbra.soap.admin.message.GetLicenseInfoRequest.class,
            com.zimbra.soap.admin.message.GetLicenseInfoResponse.class,
            com.zimbra.soap.admin.message.GetLoggerStatsRequest.class,
            com.zimbra.soap.admin.message.GetLoggerStatsResponse.class,
            com.zimbra.soap.admin.message.GetMailQueueInfoRequest.class,
            com.zimbra.soap.admin.message.GetMailQueueInfoResponse.class,
            com.zimbra.soap.admin.message.GetMailQueueRequest.class,
            com.zimbra.soap.admin.message.GetMailQueueResponse.class,
            com.zimbra.soap.admin.message.GetMailboxRequest.class,
            com.zimbra.soap.admin.message.GetMailboxResponse.class,
            com.zimbra.soap.admin.message.GetMailboxStatsRequest.class,
            com.zimbra.soap.admin.message.GetMailboxStatsResponse.class,
            com.zimbra.soap.admin.message.GetMemcachedClientConfigRequest.class,
            com.zimbra.soap.admin.message.GetMemcachedClientConfigResponse.class,
            com.zimbra.soap.admin.message.GetPublishedShareInfoRequest.class,
            com.zimbra.soap.admin.message.GetPublishedShareInfoResponse.class,
            com.zimbra.soap.admin.message.GetQuotaUsageRequest.class,
            com.zimbra.soap.admin.message.GetQuotaUsageResponse.class,
            com.zimbra.soap.admin.message.GetRightRequest.class,
            com.zimbra.soap.admin.message.GetRightResponse.class,
            com.zimbra.soap.admin.message.GetRightsDocRequest.class,
            com.zimbra.soap.admin.message.GetRightsDocResponse.class,
            com.zimbra.soap.admin.message.GetServerNIfsRequest.class,
            com.zimbra.soap.admin.message.GetServerNIfsResponse.class,
            com.zimbra.soap.admin.message.GetServerRequest.class,
            com.zimbra.soap.admin.message.GetServerResponse.class,
            com.zimbra.soap.admin.message.GetServerStatsRequest.class,
            com.zimbra.soap.admin.message.GetServerStatsResponse.class,
            com.zimbra.soap.admin.message.GetServiceStatusRequest.class,
            com.zimbra.soap.admin.message.GetServiceStatusResponse.class,
            com.zimbra.soap.admin.message.GetSessionsRequest.class,
            com.zimbra.soap.admin.message.GetSessionsResponse.class,
            com.zimbra.soap.admin.message.GetShareInfoRequest.class,
            com.zimbra.soap.admin.message.GetShareInfoResponse.class,
            com.zimbra.soap.admin.message.GetVersionInfoRequest.class,
            com.zimbra.soap.admin.message.GetVersionInfoResponse.class,
            com.zimbra.soap.admin.message.GetVolumeRequest.class,
            com.zimbra.soap.admin.message.GetVolumeResponse.class,
            com.zimbra.soap.admin.message.GetXMPPComponentRequest.class,
            com.zimbra.soap.admin.message.GetXMPPComponentResponse.class,
            com.zimbra.soap.admin.message.GetZimletRequest.class,
            com.zimbra.soap.admin.message.GetZimletResponse.class,
            com.zimbra.soap.admin.message.GetZimletStatusRequest.class,
            com.zimbra.soap.admin.message.GetZimletStatusResponse.class,
            com.zimbra.soap.admin.message.GrantRightRequest.class,
            com.zimbra.soap.admin.message.GrantRightResponse.class,
            com.zimbra.soap.admin.message.MailQueueActionRequest.class,
            com.zimbra.soap.admin.message.MailQueueActionResponse.class,
            com.zimbra.soap.admin.message.MailQueueFlushRequest.class,
            com.zimbra.soap.admin.message.MailQueueFlushResponse.class,
            com.zimbra.soap.admin.message.ModifyAccountRequest.class,
            com.zimbra.soap.admin.message.ModifyAccountResponse.class,
            com.zimbra.soap.admin.message.ModifyCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.ModifyCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.ModifyConfigRequest.class,
            com.zimbra.soap.admin.message.ModifyConfigResponse.class,
            com.zimbra.soap.admin.message.ModifyCosRequest.class,
            com.zimbra.soap.admin.message.ModifyCosResponse.class,
            com.zimbra.soap.admin.message.ModifyDataSourceRequest.class,
            com.zimbra.soap.admin.message.ModifyDataSourceResponse.class,
            com.zimbra.soap.admin.message.ModifyDelegatedAdminConstraintsRequest.class,
            com.zimbra.soap.admin.message.ModifyDelegatedAdminConstraintsResponse.class,
            com.zimbra.soap.admin.message.ModifyDistributionListRequest.class,
            com.zimbra.soap.admin.message.ModifyDistributionListResponse.class,
            com.zimbra.soap.admin.message.ModifyDomainRequest.class,
            com.zimbra.soap.admin.message.ModifyDomainResponse.class,
            com.zimbra.soap.admin.message.ModifyServerRequest.class,
            com.zimbra.soap.admin.message.ModifyServerResponse.class,
            com.zimbra.soap.admin.message.ModifyVolumeRequest.class,
            com.zimbra.soap.admin.message.ModifyVolumeResponse.class,
            com.zimbra.soap.admin.message.ModifyZimletRequest.class,
            com.zimbra.soap.admin.message.ModifyZimletResponse.class,
            com.zimbra.soap.admin.message.NoOpRequest.class,
            com.zimbra.soap.admin.message.NoOpResponse.class,
            com.zimbra.soap.admin.message.PingRequest.class,
            com.zimbra.soap.admin.message.PingResponse.class,
            com.zimbra.soap.admin.message.PublishShareInfoRequest.class,
            com.zimbra.soap.admin.message.PublishShareInfoResponse.class,
            com.zimbra.soap.admin.message.PurgeAccountCalendarCacheRequest.class,
            com.zimbra.soap.admin.message.PurgeAccountCalendarCacheResponse.class,
            com.zimbra.soap.admin.message.PurgeFreeBusyQueueRequest.class,
            com.zimbra.soap.admin.message.PurgeFreeBusyQueueResponse.class,
            com.zimbra.soap.admin.message.PurgeMessagesRequest.class,
            com.zimbra.soap.admin.message.PurgeMessagesResponse.class,
            com.zimbra.soap.admin.message.PushFreeBusyRequest.class,
            com.zimbra.soap.admin.message.PushFreeBusyResponse.class,
            com.zimbra.soap.admin.message.QueryWaitSetRequest.class,
            com.zimbra.soap.admin.message.QueryWaitSetResponse.class,
            com.zimbra.soap.admin.message.ReIndexRequest.class,
            com.zimbra.soap.admin.message.ReIndexResponse.class,
            com.zimbra.soap.admin.message.RecalculateMailboxCountsRequest.class,
            com.zimbra.soap.admin.message.RecalculateMailboxCountsResponse.class,
            com.zimbra.soap.admin.message.ReloadLocalConfigRequest.class,
            com.zimbra.soap.admin.message.ReloadLocalConfigResponse.class,
            com.zimbra.soap.admin.message.ReloadMemcachedClientConfigRequest.class,
            com.zimbra.soap.admin.message.ReloadMemcachedClientConfigResponse.class,
            com.zimbra.soap.admin.message.RemoveAccountAliasRequest.class,
            com.zimbra.soap.admin.message.RemoveAccountAliasResponse.class,
            com.zimbra.soap.admin.message.RemoveAccountLoggerRequest.class,
            com.zimbra.soap.admin.message.RemoveAccountLoggerResponse.class,
            com.zimbra.soap.admin.message.RemoveDistributionListAliasRequest.class,
            com.zimbra.soap.admin.message.RemoveDistributionListAliasResponse.class,
            com.zimbra.soap.admin.message.RemoveDistributionListMemberRequest.class,
            com.zimbra.soap.admin.message.RemoveDistributionListMemberResponse.class,
            com.zimbra.soap.admin.message.RenameAccountRequest.class,
            com.zimbra.soap.admin.message.RenameAccountResponse.class,
            com.zimbra.soap.admin.message.RenameCalendarResourceRequest.class,
            com.zimbra.soap.admin.message.RenameCalendarResourceResponse.class,
            com.zimbra.soap.admin.message.RenameCosRequest.class,
            com.zimbra.soap.admin.message.RenameCosResponse.class,
            com.zimbra.soap.admin.message.RenameDistributionListRequest.class,
            com.zimbra.soap.admin.message.RenameDistributionListResponse.class,
            com.zimbra.soap.admin.message.RevokeRightRequest.class,
            com.zimbra.soap.admin.message.RevokeRightResponse.class,
            com.zimbra.soap.admin.message.RunUnitTestsRequest.class,
            com.zimbra.soap.admin.message.RunUnitTestsResponse.class,
            com.zimbra.soap.admin.message.SearchCalendarResourcesRequest.class,
            com.zimbra.soap.admin.message.SearchCalendarResourcesResponse.class,
            com.zimbra.soap.admin.message.SearchDirectoryRequest.class,
            com.zimbra.soap.admin.message.SearchDirectoryResponse.class,
            com.zimbra.soap.admin.message.SetCurrentVolumeRequest.class,
            com.zimbra.soap.admin.message.SetCurrentVolumeResponse.class,
            com.zimbra.soap.admin.message.SetPasswordRequest.class,
            com.zimbra.soap.admin.message.SetPasswordResponse.class,
            com.zimbra.soap.admin.message.UndeployZimletRequest.class,
            com.zimbra.soap.admin.message.UndeployZimletResponse.class,
            com.zimbra.soap.admin.message.VerifyIndexRequest.class,
            com.zimbra.soap.admin.message.VerifyIndexResponse.class
        };

        try {
            JAXB_CONTEXT = JAXBContext.newInstance(MESSAGE_CLASSES);
        } catch (JAXBException e) {
            ZimbraLog.soap.error("Unable to initialize JAXB", e);
        }
    }

    private JaxbUtil() {
    }

    public static Class<?>[] getJaxbRequestAndResponseClasses() {
        return MESSAGE_CLASSES;
    }

    /**
     * @param o
     * @param factory - e.g. XmlElement.mFactory or JSONElement.mFactory 
     * @return
     * @throws ServiceException
     */
    public static Element jaxbToElement(Object o, Element.ElementFactory factory)
    throws ServiceException {
        try {
            Marshaller marshaller = getContext().createMarshaller();
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            DocumentResult dr = new DocumentResult();
            marshaller.marshal(o, dr); 
            Document theDoc = dr.getDocument();
            org.dom4j.Element rootElem = theDoc.getRootElement();
            return Element.convertDOM(rootElem, factory);
        } catch (Exception e) {
            throw ServiceException.FAILURE("Unable to convert " +
                    o.getClass().getName() + " to Element", e);
        }
    }

    public static Element jaxbToElement(Object o)
    throws ServiceException {
        return jaxbToElement(o, XMLElement.mFactory);
    }

    //  This appears to be safe but is fairly slow.
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxbUsingByteArray(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.dom4j.Element rootElem = e.toXML();
            return (T) unmarshaller.unmarshal(new ByteArrayInputStream(rootElem.asXML().getBytes("utf-8")));
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        } catch (UnsupportedEncodingException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
		}
    }

    // This appears to work if e is an XMLElement but sometimes fails badly if
    // e is a JSONElement - get:
    // javax.xml.bind.UnmarshalException: Namespace URIs and local names
    //      to the unmarshaller needs to be interned.
    // and that seems to make the unmarshaller unstable from then on :-(
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxbUsingDom4j(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.dom4j.Element rootElem = e.toXML();
            DocumentSource docSrc = new DocumentSource(rootElem);
            return (T) unmarshaller.unmarshal(docSrc);
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
    }

    /**
     * Return a JAXB object.  This implementation uses a org.w3c.dom.Document 
     * as an intermediate representation.  This appears to be more reliable
     * than using a DocumentSource based on org.dom4j.Element
     */
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxb(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.w3c.dom.Document doc = e.toW3cDom();
            // ZimbraLog.soap.warn("Dom to Xml:\n" + domToString(doc));
            return (T) unmarshaller.unmarshal(doc);
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
    }

    public static String domToString(org.w3c.dom.Document document) {
        try {
            Source xmlSource = new DOMSource(document);
            StreamResult result = new StreamResult(new ByteArrayOutputStream());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes"); //Java XML Indent
            transformer.transform(xmlSource, result);
            return result.getOutputStream().toString();
        } catch (TransformerFactoryConfigurationError factoryError) {
            ZimbraLog.soap.error("Error creating TransformerFactory", factoryError);
        } catch (TransformerException transformerError) {
            ZimbraLog.soap.error( "Error transforming document", transformerError);
        }
        return null;
    }

    private static JAXBContext getContext()
    throws ServiceException {
        if (JAXB_CONTEXT == null) {
            throw ServiceException.FAILURE("JAXB has not been initialized", null);
        }
        return JAXB_CONTEXT;
    }
}
