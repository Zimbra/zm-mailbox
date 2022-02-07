/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.fb;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import com.microsoft.schemas.exchange.services._2006.messages.CreateItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.CreateItemType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeService;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services._2006.messages.FindFolderResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services._2006.messages.FolderInfoResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.FreeBusyResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.GetFolderResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.GetFolderType;
import com.microsoft.schemas.exchange.services._2006.messages.GetUserAvailabilityRequestType;
import com.microsoft.schemas.exchange.services._2006.messages.GetUserAvailabilityResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.UpdateItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.UpdateItemType;
import com.microsoft.schemas.exchange.services._2006.types.ArrayOfCalendarEvent;
import com.microsoft.schemas.exchange.services._2006.types.ArrayOfMailboxData;
import com.microsoft.schemas.exchange.services._2006.types.BaseFolderType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarEvent;
import com.microsoft.schemas.exchange.services._2006.types.CalendarEventDetails;
import com.microsoft.schemas.exchange.services._2006.types.ConflictResolutionType;
import com.microsoft.schemas.exchange.services._2006.types.ConstantValueType;
import com.microsoft.schemas.exchange.services._2006.types.ContainmentModeType;
import com.microsoft.schemas.exchange.services._2006.types.ContainsExpressionType;
import com.microsoft.schemas.exchange.services._2006.types.DateTimePrecisionType;
import com.microsoft.schemas.exchange.services._2006.types.DayOfWeekType;
import com.microsoft.schemas.exchange.services._2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.Duration;
import com.microsoft.schemas.exchange.services._2006.types.EmailAddress;
import com.microsoft.schemas.exchange.services._2006.types.ExchangeVersionType;
import com.microsoft.schemas.exchange.services._2006.types.ExtendedPropertyType;
import com.microsoft.schemas.exchange.services._2006.types.FieldURIOrConstantType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.FolderQueryTraversalType;
import com.microsoft.schemas.exchange.services._2006.types.FolderResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.FolderType;
import com.microsoft.schemas.exchange.services._2006.types.FreeBusyViewOptionsType;
import com.microsoft.schemas.exchange.services._2006.types.IsEqualToType;
import com.microsoft.schemas.exchange.services._2006.types.ItemChangeType;
import com.microsoft.schemas.exchange.services._2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services._2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.ItemType;
import com.microsoft.schemas.exchange.services._2006.types.LegacyFreeBusyType;
import com.microsoft.schemas.exchange.services._2006.types.MailboxCultureType;
import com.microsoft.schemas.exchange.services._2006.types.MailboxData;
import com.microsoft.schemas.exchange.services._2006.types.ManagementRoleType;
import com.microsoft.schemas.exchange.services._2006.types.MapiPropertyTypeType;
import com.microsoft.schemas.exchange.services._2006.types.MeetingAttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.MessageDispositionType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAllItemsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfItemChangeDescriptionsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfItemChangesType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfPropertyValuesType;
import com.microsoft.schemas.exchange.services._2006.types.PathToExtendedFieldType;
import com.microsoft.schemas.exchange.services._2006.types.PathToUnindexedFieldType;
import com.microsoft.schemas.exchange.services._2006.types.PostItemType;
import com.microsoft.schemas.exchange.services._2006.types.RequestServerVersion;
import com.microsoft.schemas.exchange.services._2006.types.ResponseClassType;
import com.microsoft.schemas.exchange.services._2006.types.RestrictionType;
import com.microsoft.schemas.exchange.services._2006.types.SearchExpressionType;
import com.microsoft.schemas.exchange.services._2006.types.SerializableTimeZone;
import com.microsoft.schemas.exchange.services._2006.types.SerializableTimeZoneTime;
import com.microsoft.schemas.exchange.services._2006.types.ServerVersionInfo;
import com.microsoft.schemas.exchange.services._2006.types.SetItemFieldType;
import com.microsoft.schemas.exchange.services._2006.types.TargetFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.TimeZoneContextType;
import com.microsoft.schemas.exchange.services._2006.types.TimeZoneDefinitionType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.AuthScheme;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.ExchangeUserResolver;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.ServerInfo;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;

public class ExchangeEWSFreeBusyProvider extends FreeBusyProvider {
    public static final String TYPE_EWS = "ews";
    private ExchangeServicePortType service = null;
    private static ExchangeService factory = null;

    static {
        ZimbraLog.fb.debug("Setting MailcapCommandMap handlers back to default");
        MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
        mc.addMailcap("application/xml;;x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/xml;;x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;;x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("xml/x-zimbra-share;;x-java-content-handler=com.sun.mail.handlers.text_plain");
        CommandMap.setDefaultCommandMap(mc);
        ZimbraLog.fb.debug("Done Setting MailcapCommandMap handlers");

        URL wsdlUrl = ExchangeService.class.getResource("/EWS.wsdl");
        ZimbraLog.fb.debug("EWS Wsdl URL = %s", wsdlUrl);
        factory = new ExchangeService(wsdlUrl,
                new QName("http://schemas.microsoft.com/exchange/services/2006/messages",
                    "ExchangeService"));
    }

    boolean initService(ServerInfo info) throws MalformedURLException {
        service = factory.getExchangeServicePort();

        ((BindingProvider)service).getRequestContext()
            .put(BindingProvider.USERNAME_PROPERTY, info.authUsername);
        ((BindingProvider)service).getRequestContext()
            .put(BindingProvider.PASSWORD_PROPERTY, info.authPassword);
        ((BindingProvider)service).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, info.url);

        // TODO: make sure we're passing authentication
        return true;
    }

    private static TrustManager[] trustAllCerts =
        new TrustManager[] { new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {

            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {

            }

        } };

    private static HostnameVerifier hv = new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {

            return true;// accept all
        }
    };

    /* Enable and call the following static block in initService() to accept self signed certificates */
    /* private static void setSSLConfig() throws Exception {
         SSLContext context = SSLContext.getInstance("SSL");
         context.init(null, trustAllCerts, new SecureRandom());
         HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
         HttpsURLConnection.setDefaultHostnameVerifier(hv);
     }
    */

    private static class BasicUserResolver implements ExchangeUserResolver {
        @Override
        public ServerInfo getServerInfo(String emailAddr) {
            String url =
                getAttr(Provisioning.A_zimbraFreebusyExchangeURL, emailAddr);
            String user =
                getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername,
                    emailAddr);
            String pass =
                getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword,
                    emailAddr);
            String scheme =
                getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme,
                    emailAddr);
            if (url == null || user == null || pass == null || scheme == null)
                return null;

            ServerInfo info = new ServerInfo();
            info.url = url;
            info.authUsername = user;
            info.authPassword = pass;
            info.scheme = AuthScheme.valueOf(scheme);
            info.org =
                getAttr(Provisioning.A_zimbraFreebusyExchangeUserOrg, emailAddr);
            try {
                Account acct =
                    Provisioning.getInstance().get(AccountBy.name, emailAddr);
                if (acct != null) {
                    String fps[] =
                        acct.getMultiAttr(Provisioning.A_zimbraForeignPrincipal);
                    if (fps != null && fps.length > 0) {
                        for (String fp : fps) {
                            if (fp.startsWith(Provisioning.FP_PREFIX_AD)) {
                                int idx = fp.indexOf(':');
                                if (idx != -1) {
                                    info.cn = fp.substring(idx + 1);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (ServiceException se) {
                info.cn = null;
            }
			String exchangeType = getAttr(Provisioning.A_zimbraFreebusyExchangeServerType, emailAddr);
			info.enabled = TYPE_EWS.equals(exchangeType);
            return info;
        }

        // first lookup account/cos, then domain, then globalConfig.
        static String getAttr(String attr, String emailAddr) {
            String val = null;
            if (attr == null)
                return val;
            try {
                Provisioning prov = Provisioning.getInstance();
                if (emailAddr != null) {
                    Account acct = prov.get(AccountBy.name, emailAddr);
                    if (acct != null) {
                        val = acct.getAttr(attr, null);
                        if (val != null)
                            return val;
                        Domain dom = prov.getDomain(acct);
                        if (dom != null)
                            val = dom.getAttr(attr, null);
                        if (val != null)
                            return val;
                    }
                }
                val = prov.getConfig().getAttr(attr, null);
            } catch (ServiceException se) {
                ZimbraLog.fb.error("can't get attr " + attr, se);
            }
            return val;
        }
    }

    public ExchangeEWSFreeBusyProvider() {
        mRequests = new HashMap<String, ArrayList<Request>>();
    }

    @Override
    public boolean registerForMailboxChanges() {
        return registerForMailboxChanges(null);
    }

    BaseFolderType bindFolder(
        DistinguishedFolderIdNameType distinguishedFolderId,
        DefaultShapeNamesType shapeType) {
        final DistinguishedFolderIdType distinguishedFolderIdType =
            new DistinguishedFolderIdType();
        distinguishedFolderIdType.setId(distinguishedFolderId);
        final NonEmptyArrayOfBaseFolderIdsType nonEmptyArrayOfBaseFolderIdsType =
            new NonEmptyArrayOfBaseFolderIdsType();
        nonEmptyArrayOfBaseFolderIdsType.getFolderIdOrDistinguishedFolderId()
            .add(distinguishedFolderIdType);
        GetFolderType getFolderRequest = new GetFolderType();
        getFolderRequest.setFolderIds(nonEmptyArrayOfBaseFolderIdsType);
        FolderResponseShapeType stResp = new FolderResponseShapeType();
        stResp.setBaseShape(shapeType);
        getFolderRequest.setFolderShape(stResp);
        RequestServerVersion serverVersion = new RequestServerVersion();
        serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
        Holder<ServerVersionInfo> gfversionInfo =
            new Holder<ServerVersionInfo>();
        Holder<GetFolderResponseType> gfresponseHolder =
            new Holder<GetFolderResponseType>();
        MailboxCultureType mct = new MailboxCultureType();
        mct.setValue("EN");
        TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
        tzdt.setId("Greenwich Standard Time");
        TimeZoneContextType tzct = new TimeZoneContextType();
        tzct.setTimeZoneDefinition(tzdt);
        service.getFolder(getFolderRequest,
                mct, serverVersion,
                tzct, gfresponseHolder,
                gfversionInfo);
        FolderInfoResponseMessageType firmtResp =
            (FolderInfoResponseMessageType)gfresponseHolder.value.getResponseMessages()
                .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                .get(0)
                .getValue();

        if (firmtResp.getFolders()
            .getFolderOrCalendarFolderOrContactsFolder()
            .size() > 0) {
            return firmtResp.getFolders()
                .getFolderOrCalendarFolderOrContactsFolder()
                .get(0);
        } else {
            ZimbraLog.fb.error("Could not find the folder in Exchange : " + distinguishedFolderId.toString());
        }

        return null;
    }

    List<BaseFolderType> findFolderByProp(FolderIdType id,
        UnindexedFieldURIType prop, String val) {
        FindFolderType findFolderRequest = new FindFolderType();

        findFolderRequest.setTraversal(FolderQueryTraversalType.SHALLOW);
        final NonEmptyArrayOfBaseFolderIdsType ffEmptyArrayOfBaseFolderIdsType =
            new NonEmptyArrayOfBaseFolderIdsType();
        ffEmptyArrayOfBaseFolderIdsType.getFolderIdOrDistinguishedFolderId()
            .add(id);
        FolderResponseShapeType stResp = new FolderResponseShapeType();
        stResp.setBaseShape(DefaultShapeNamesType.ID_ONLY);
        findFolderRequest.setParentFolderIds(ffEmptyArrayOfBaseFolderIdsType);
        findFolderRequest.setFolderShape(stResp);

        RestrictionType rtRestriction = new RestrictionType();

        IsEqualToType ieq = new IsEqualToType();

        PathToUnindexedFieldType pix = new PathToUnindexedFieldType();
        pix.setFieldURI(prop);
        ieq.setPath(new JAXBElement<PathToUnindexedFieldType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
            "FieldURI"),
            PathToUnindexedFieldType.class,
            pix));

        FieldURIOrConstantType ct = new FieldURIOrConstantType();
        ConstantValueType cv = new ConstantValueType();
        cv.setValue(val);
        ct.setConstant(cv);

        ieq.setFieldURIOrConstant(ct);

        rtRestriction.setSearchExpression(new JAXBElement<SearchExpressionType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
            "IsEqualTo"),
            SearchExpressionType.class,
            ieq));

        findFolderRequest.setRestriction(rtRestriction);

        Holder<FindFolderResponseType> findFolderResponse =
            new Holder<FindFolderResponseType>();
        RequestServerVersion serverVersion = new RequestServerVersion();
        serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
        Holder<ServerVersionInfo> gfversionInfo =
            new Holder<ServerVersionInfo>();

        MailboxCultureType mct = new MailboxCultureType();
        mct.setValue("EN");
        TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
        tzdt.setId("Greenwich Standard Time");
        TimeZoneContextType tzct = new TimeZoneContextType();
        tzct.setTimeZoneDefinition(tzdt);
        ManagementRoleType role = null;
        service.findFolder(findFolderRequest,
                mct, serverVersion,
                tzct, role, findFolderResponse,
                gfversionInfo);
        FindFolderResponseMessageType ffRespMessage =
            (FindFolderResponseMessageType)findFolderResponse.value.getResponseMessages()
                .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                .get(0)
                .getValue();
        if (ResponseClassType.SUCCESS == ffRespMessage.getResponseClass()) {
            return ffRespMessage.getRootFolder()
                .getFolders()
                .getFolderOrCalendarFolderOrContactsFolder();
        }
        ZimbraLog.fb.warn("findFolderByProp " + ffRespMessage.getResponseCode());
        return null;
    }

    List<BaseFolderType> findFolderByPartialProp(FolderIdType id,
        UnindexedFieldURIType prop, String val) {
        FindFolderType findFolderRequest = new FindFolderType();

        findFolderRequest.setTraversal(FolderQueryTraversalType.SHALLOW);
        final NonEmptyArrayOfBaseFolderIdsType ffEmptyArrayOfBaseFolderIdsType =
            new NonEmptyArrayOfBaseFolderIdsType();
        ffEmptyArrayOfBaseFolderIdsType.getFolderIdOrDistinguishedFolderId()
            .add(id);
        FolderResponseShapeType stResp = new FolderResponseShapeType();
        stResp.setBaseShape(DefaultShapeNamesType.ID_ONLY);
        findFolderRequest.setParentFolderIds(ffEmptyArrayOfBaseFolderIdsType);
        findFolderRequest.setFolderShape(stResp);

        RestrictionType rtRestriction = new RestrictionType();

        ContainsExpressionType contains = new ContainsExpressionType();
        PathToUnindexedFieldType pix = new PathToUnindexedFieldType();
        pix.setFieldURI(prop);
        contains.setPath(new JAXBElement<PathToUnindexedFieldType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
            "FieldURI"),
            PathToUnindexedFieldType.class,
            pix));

        FieldURIOrConstantType ct = new FieldURIOrConstantType();
        ConstantValueType cv = new ConstantValueType();
        cv.setValue(val);
        ct.setConstant(cv);

        contains.setConstant(cv);
        contains.setContainmentMode(ContainmentModeType.SUBSTRING);

        rtRestriction.setSearchExpression(new JAXBElement<SearchExpressionType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
            "Contains"),
            SearchExpressionType.class,
            contains));

        findFolderRequest.setRestriction(rtRestriction);

        Holder<FindFolderResponseType> findFolderResponse =
            new Holder<FindFolderResponseType>();
        RequestServerVersion serverVersion = new RequestServerVersion();
        serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
        Holder<ServerVersionInfo> gfversionInfo =
            new Holder<ServerVersionInfo>();

        MailboxCultureType mct = new MailboxCultureType();
        mct.setValue("EN");
        TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
        tzdt.setId("Greenwich Standard Time");
        TimeZoneContextType tzct = new TimeZoneContextType();
        tzct.setTimeZoneDefinition(tzdt);
        ManagementRoleType role = null;
        service.findFolder(findFolderRequest,
                mct, serverVersion,
                tzct, role, findFolderResponse,
                gfversionInfo);
        FindFolderResponseMessageType ffRespMessage =
            (FindFolderResponseMessageType)findFolderResponse.value.getResponseMessages()
                .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                .get(0)
                .getValue();
        if (ResponseClassType.SUCCESS == ffRespMessage.getResponseClass()) {
            return ffRespMessage.getRootFolder()
                .getFolders()
                .getFolderOrCalendarFolderOrContactsFolder();
        }
        ZimbraLog.fb.warn("findFolderByPartialProp " +
            ffRespMessage.getResponseCode());
        return null;
    }

    List<ItemType> findItemByProp(FolderIdType id, UnindexedFieldURIType prop,
        String val, DefaultShapeNamesType shapeType) {
        FindItemType findItemRequest = new FindItemType();

        RestrictionType rtRestriction = new RestrictionType();

        IsEqualToType ieq = new IsEqualToType();

        PathToUnindexedFieldType pix = new PathToUnindexedFieldType();
        pix.setFieldURI(prop);
        ieq.setPath(new JAXBElement<PathToUnindexedFieldType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
            "FieldURI"),
            PathToUnindexedFieldType.class,
            pix));

        FieldURIOrConstantType ct = new FieldURIOrConstantType();
        ConstantValueType cv = new ConstantValueType();
        cv.setValue(val);
        ct.setConstant(cv);

        ieq.setFieldURIOrConstant(ct);

        rtRestriction.setSearchExpression(new JAXBElement<SearchExpressionType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
            "IsEqualTo"),
            SearchExpressionType.class,
            ieq));

        findItemRequest.setRestriction(rtRestriction);

        ItemResponseShapeType stShape = new ItemResponseShapeType();
        stShape.setBaseShape(shapeType);
        findItemRequest.setItemShape(stShape);
        NonEmptyArrayOfBaseFolderIdsType ids =
            new NonEmptyArrayOfBaseFolderIdsType();
        ids.getFolderIdOrDistinguishedFolderId().add(id);
        findItemRequest.setParentFolderIds(ids);
        findItemRequest.setTraversal(ItemQueryTraversalType.SHALLOW);

        RequestServerVersion serverVersion = new RequestServerVersion();
        serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);

        Holder<FindItemResponseType> fiResponse =
            new Holder<FindItemResponseType>();
        Holder<ServerVersionInfo> gfversionInfo =
            new Holder<ServerVersionInfo>();
        MailboxCultureType mct = new MailboxCultureType();
        mct.setValue("EN");
        TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
        tzdt.setId("Greenwich Standard Time");
        TimeZoneContextType tzct = new TimeZoneContextType();
        tzct.setTimeZoneDefinition(tzdt);
        ManagementRoleType role = null;
        service.findItem(findItemRequest,
                mct, serverVersion,
                tzct, DateTimePrecisionType.MILLISECONDS, role, fiResponse,
                gfversionInfo);

        FindItemResponseMessageType fiRespMessage =
            (FindItemResponseMessageType)fiResponse.value.getResponseMessages()
                .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                .get(0)
                .getValue();

        if (ResponseClassType.SUCCESS == fiRespMessage.getResponseClass()) {
            return fiRespMessage.getRootFolder()
                .getItems()
                .getItemOrMessageOrCalendarItem();
        }
        ZimbraLog.fb.warn("findItemByProp " + fiRespMessage.getResponseCode());
        return null;
    }

    @Override
    public boolean handleMailboxChange(String accountId) {
        ZimbraLog.fb.debug("Entering handleMailboxChange() for account : " + accountId);
        String email = getEmailAddress(accountId);
		ServerInfo serverInfo = getServerInfo(email);
		if (email == null || !serverInfo.enabled) {
		    ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
			return true;  // no retry
		}

        FreeBusy fb;
        try {
            fb = getFreeBusy(accountId, FreeBusyQuery.CALENDAR_FOLDER_ALL);
        } catch (ServiceException se) {
            ZimbraLog.fb.warn("can't get freebusy for account " + accountId, se);
            ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
            // retry the request if it's receivers fault.
            return !se.isReceiversFault();
        }
        if (email == null || fb == null) {
            ZimbraLog.fb.warn("account not found / incorrect / wrong host: " +
                accountId);
            ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
            return true; // no retry
        }

        if (serverInfo == null || serverInfo.org == null ||
            serverInfo.cn == null) {
            ZimbraLog.fb.warn("no exchange server info for user " + email);
            ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
            return true; // no retry
        }
        if (null == service) {
            try {
                if (!initService(serverInfo)) {
                    ZimbraLog.fb.error("failed to initialize exchange service object " +
                        serverInfo.url);
                    ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
                    return true;
                }
            } catch (MalformedURLException e) {
                ZimbraLog.fb.error("exception while trying to initialize exchange service object " +
                    serverInfo.url);
                ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
                return true;
            }
        }
        ExchangeEWSMessage msg =
            new ExchangeEWSMessage(serverInfo.org, serverInfo.cn, email);

        try {
            FolderType publicFolderRoot =
                (FolderType)bindFolder(DistinguishedFolderIdNameType.PUBLICFOLDERSROOT,
                    DefaultShapeNamesType.ALL_PROPERTIES);
            if (publicFolderRoot == null) {
                ZimbraLog.fb.error("Could not find the public root folder on exchange");
                return true;
            }
            List<BaseFolderType> resultsNonIpm =
                findFolderByProp(publicFolderRoot.getParentFolderId(),
                    UnindexedFieldURIType.FOLDER_DISPLAY_NAME,
                    "NON_IPM_SUBTREE");

            if (resultsNonIpm != null && resultsNonIpm.size() > 0) {
                FolderType folderNonIPM = (FolderType)resultsNonIpm.get(0);

                List<BaseFolderType> resultSchedulePlus =
                    findFolderByProp(folderNonIPM.getFolderId(),
                        UnindexedFieldURIType.FOLDER_DISPLAY_NAME,
                        "SCHEDULE+ FREE BUSY");
                if (resultSchedulePlus != null && resultSchedulePlus.size() > 0) {
                    FolderType folderSchedulePlus =
                        (FolderType)resultSchedulePlus.get(0);

                    List<BaseFolderType> resultFBFolder =
                        findFolderByPartialProp(folderSchedulePlus.getFolderId(),
                            UnindexedFieldURIType.FOLDER_DISPLAY_NAME,
                            serverInfo.org);// TODO: check here for partial name
                    // search
                    if (resultFBFolder != null && resultFBFolder.size() > 0) {
                        FolderType folderFB = (FolderType)resultFBFolder.get(0);

                        List<ItemType> resultMessage =
                            findItemByProp(folderFB.getFolderId(),
                                UnindexedFieldURIType.ITEM_SUBJECT,
                                "USER-/CN=RECIPIENTS/CN=" +
                                getForeignPrincipal(accountId),
                                DefaultShapeNamesType.ALL_PROPERTIES);
                        if (resultMessage != null && resultMessage.size() > 0) {
                            // edit message
                            ItemType itemMessage = resultMessage.get(0);

                            Map<PathToExtendedFieldType, NonEmptyArrayOfPropertyValuesType> props =
                                msg.GetFreeBusyProperties(fb);

                            final NonEmptyArrayOfItemChangeDescriptionsType cdExPropArr =
                                new NonEmptyArrayOfItemChangeDescriptionsType();
                            for (PathToExtendedFieldType pathExProp : props.keySet()) {
                                ItemType itemEmptyMessage = new ItemType();
                                SetItemFieldType sifItem =
                                    new SetItemFieldType();
                                sifItem.setPath(new JAXBElement<PathToExtendedFieldType>(new QName("http://schemas.microsoft.com/exchange/services/2006/types",
                                    "Path"),
                                    PathToExtendedFieldType.class,
                                    pathExProp));
                                ExtendedPropertyType exProp =
                                    new ExtendedPropertyType();
                                exProp.setExtendedFieldURI(pathExProp);
                                if (pathExProp.getPropertyType() == MapiPropertyTypeType.APPLICATION_TIME_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.BINARY_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.CLSID_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.CURRENCY_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.DOUBLE_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.FLOAT_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.INTEGER_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.LONG_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.OBJECT_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.SHORT_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.STRING_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.SYSTEM_TIME_ARRAY) {
                                    exProp.setValues(props.get(pathExProp));
                                } else {
                                    if (props.get(pathExProp).getValue().size() > 0) {
                                        exProp.setValue(props.get(pathExProp)
                                            .getValue()
                                            .get(0));
                                    }
                                }
                                itemEmptyMessage.getExtendedProperty()
                                    .add(exProp);
                                sifItem.setItem(itemEmptyMessage);
                                cdExPropArr.getAppendToItemFieldOrSetItemFieldOrDeleteItemField()
                                    .add(sifItem);

                            }
                            UpdateItemType updateItemRequest =
                                new UpdateItemType();
                            updateItemRequest.setMessageDisposition(MessageDispositionType.SAVE_ONLY);
                            updateItemRequest.setConflictResolution(ConflictResolutionType.ALWAYS_OVERWRITE);
                            RequestServerVersion serverVersion =
                                new RequestServerVersion();
                            serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);

                            ItemChangeType itemExpropChange =
                                new ItemChangeType();
                            itemExpropChange.setItemId(itemMessage.getItemId());
                            itemExpropChange.setUpdates(cdExPropArr);
                            final NonEmptyArrayOfItemChangesType ctExPropArr =
                                new NonEmptyArrayOfItemChangesType();
                            ctExPropArr.getItemChange().add(itemExpropChange);
                            updateItemRequest.setItemChanges(ctExPropArr);

                            Holder<UpdateItemResponseType> updateItemResponse =
                                new Holder<UpdateItemResponseType>();
                            Holder<ServerVersionInfo> gfversionInfo =
                                new Holder<ServerVersionInfo>();
                            MailboxCultureType mct = new MailboxCultureType();
                            mct.setValue("EN");
                            TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
                            tzdt.setId("Greenwich Standard Time");
                            TimeZoneContextType tzct = new TimeZoneContextType();
                            tzct.setTimeZoneDefinition(tzdt);
                            service.updateItem(updateItemRequest,
                                mct, serverVersion,
                                tzct, updateItemResponse,
                                gfversionInfo);
                            ResponseMessageType updateItemResponseMessage =
                                updateItemResponse.value.getResponseMessages()
                                    .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                                    .get(0)
                                    .getValue();

                        } else {
                            // create message
                            PostItemType itemMessage = new PostItemType();

                            itemMessage.setSubject("USER-/CN=RECIPIENTS/CN=" +
                            	getForeignPrincipal(accountId));
                            itemMessage.setItemClass("IPM.Post");

                            Map<PathToExtendedFieldType, NonEmptyArrayOfPropertyValuesType> props =
                                msg.GetFreeBusyProperties(fb);

                            for (PathToExtendedFieldType pathExProp : props.keySet()) {
                                ExtendedPropertyType exProp =
                                    new ExtendedPropertyType();
                                exProp.setExtendedFieldURI(pathExProp);
                                if (pathExProp.getPropertyType() == MapiPropertyTypeType.APPLICATION_TIME_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.BINARY_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.CLSID_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.CURRENCY_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.DOUBLE_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.FLOAT_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.INTEGER_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.LONG_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.OBJECT_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.SHORT_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.STRING_ARRAY ||
                                    pathExProp.getPropertyType() == MapiPropertyTypeType.SYSTEM_TIME_ARRAY) {
                                    exProp.setValues(props.get(pathExProp));
                                } else {
                                    if (props.get(pathExProp).getValue().size() > 0) {
                                        exProp.setValue(props.get(pathExProp)
                                            .getValue()
                                            .get(0));
                                    }
                                }
                                itemMessage.getExtendedProperty().add(exProp);
                            }

                            CreateItemType createItemRequest =
                                new CreateItemType();
                            RequestServerVersion serverVersion =
                                new RequestServerVersion();
                            serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
                            createItemRequest.setMessageDisposition(MessageDispositionType.SAVE_ONLY);
                            TargetFolderIdType idTargetFolder =
                                new TargetFolderIdType();
                            idTargetFolder.setFolderId(folderFB.getFolderId());
                            createItemRequest.setSavedItemFolderId(idTargetFolder);
                            NonEmptyArrayOfAllItemsType createItems =
                                new NonEmptyArrayOfAllItemsType();
                            createItems.getItemOrMessageOrCalendarItem()
                                .add(itemMessage);
                            createItemRequest.setItems(createItems);
                            Holder<CreateItemResponseType> createItemResponse =
                                new Holder<CreateItemResponseType>();
                            Holder<ServerVersionInfo> gfversionInfo =
                                new Holder<ServerVersionInfo>();
                            MailboxCultureType mct = new MailboxCultureType();
                            mct.setValue("EN");
                            TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
                            tzdt.setId("Greenwich Standard Time");
                            TimeZoneContextType tzct = new TimeZoneContextType();
                            tzct.setTimeZoneDefinition(tzdt);
                            service.createItem(createItemRequest,
                                mct, serverVersion,
                                tzct, createItemResponse,
                                gfversionInfo);
                            ResponseMessageType createItemResponseMessage =
                                createItemResponse.value.getResponseMessages()
                                    .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                                    .get(0)
                                    .getValue();

                        }
                    } else {
                        ZimbraLog.fb.error("Could not find the Exchange folder containing '" + serverInfo.org +
                                "'. Make sure zimbraFreebusyExchangeUserOrg is configured correctly and it exists on Exchange");
                    }
                } else {
                    ZimbraLog.fb.error("Could not find the Exchange folder 'SCHEDULE+ FREE BUSY'");
                }
            } else {
                ZimbraLog.fb.error("Could not find the Exchange folder 'NON_IPM_SUBTREE'");
            }

            return true;

        } catch (Exception e) {
            ZimbraLog.fb.error("error communicating with " + serverInfo.url, e);
        } finally {
            ZimbraLog.fb.debug("Exiting handleMailboxChange() for account : " + accountId);
        }

        return false;// retry
    }

    public List<FreeBusy>
        getFreeBusyForHost(String host, ArrayList<Request> req)
            throws IOException {
        int fb_interval = LC.exchange_free_busy_interval_min.intValueWithinRange(5, 1444);
        List<FreeBusyResponseType> results = null;
        ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();

		Request r = req.get(0);
        long start = Request.offsetInterval(req.get(0).start, fb_interval);
		ServerInfo serverInfo = (ServerInfo) r.data;
		if (serverInfo == null) {
			ZimbraLog.fb.warn("no exchange server info for user "+r.email);
			return ret;
		}

		if (!serverInfo.enabled)
			return ret;

        ArrayOfMailboxData attendees = new ArrayOfMailboxData();

        for (Request request : req) {
            EmailAddress email = new EmailAddress();
            email.setAddress(request.email);
            MailboxData mailbox = new MailboxData();
            mailbox.setEmail(email);
            mailbox.setAttendeeType(MeetingAttendeeType.REQUIRED);
            attendees.getMailboxData().add(mailbox);
        }
        try {
            Duration duration = new Duration();
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

            GregorianCalendar gregorianCalStart = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            gregorianCalStart.setTimeInMillis(start);
            duration.setStartTime(datatypeFactory.newXMLGregorianCalendar(gregorianCalStart));

            GregorianCalendar gregorianCalEnd = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            gregorianCalEnd.setTimeInMillis(req.get(0).end);
            duration.setEndTime(datatypeFactory.newXMLGregorianCalendar(gregorianCalEnd));

            FreeBusyViewOptionsType availabilityOpts =
                new FreeBusyViewOptionsType();
            availabilityOpts.setMergedFreeBusyIntervalInMinutes(fb_interval);

            // Request for highest hierarchy view. The rest hierarchy will be Detailed->FreeBusy->MergedOnly->None
            availabilityOpts.getRequestedView().add("DetailedMerged");
            availabilityOpts.setTimeWindow(duration);
            GetUserAvailabilityRequestType availabilityRequest = new GetUserAvailabilityRequestType();
            // TODO: check if we need to set request timezone
            SerializableTimeZone timezone = new SerializableTimeZone();
            timezone.setBias(0);
            SerializableTimeZoneTime standardTime = new SerializableTimeZoneTime();
            standardTime.setTime("00:00:00");
            standardTime.setDayOrder((short)1);
            standardTime.setDayOfWeek(DayOfWeekType.SUNDAY);
            timezone.setStandardTime(standardTime);
            timezone.setDaylightTime(standardTime);
            availabilityRequest.setTimeZone(timezone);
            availabilityRequest.setFreeBusyViewOptions(availabilityOpts);
            availabilityRequest.setMailboxDataArray(attendees);
            
            RequestServerVersion serverVersion = new RequestServerVersion();
            serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
            Holder<GetUserAvailabilityResponseType> availabilityResponse =
                new Holder<GetUserAvailabilityResponseType>();
            Holder<ServerVersionInfo> gfversionInfo =
                new Holder<ServerVersionInfo>();

            TimeZoneDefinitionType tzdt = new TimeZoneDefinitionType();
            tzdt.setId("Greenwich Standard Time");
            TimeZoneContextType tzct = new TimeZoneContextType();
            tzct.setTimeZoneDefinition(tzdt);

            service.getUserAvailability(availabilityRequest, tzct, serverVersion,
                availabilityResponse,
                gfversionInfo);
            results = availabilityResponse.value.getFreeBusyResponseArray()
                    .getFreeBusyResponse();

        } catch (DatatypeConfigurationException dce) {
            ZimbraLog.fb.warn("getFreeBusyForHost DatatypeConfiguration failure",
                dce);
            return getEmptyList(req);
        } catch (Exception e) {
            ZimbraLog.fb.warn("getFreeBusyForHost failure", e);
            return getEmptyList(req);
        }

        for (Request re : req) {
            
            int i = 0;
            long startTime = req.get(0).start;
            long endTime = req.get(0).end;
            for (FreeBusyResponseType attendeeAvailability : results) {
                if (attendeeAvailability.getFreeBusyView() != null) {
                    	String fbResponseViewType = attendeeAvailability.getFreeBusyView().getFreeBusyViewType().get(0);
                    	String emailAddress = attendees.getMailboxData().get(i).getEmail().getAddress();
                    	ZimbraLog.fb.debug("For user :%s free busy response type received is : %s", emailAddress, fbResponseViewType);
                	
                    if (re.email == emailAddress) {
                        if (ResponseClassType.ERROR == attendeeAvailability.getResponseMessage().getResponseClass()) {
                            ZimbraLog.fb.debug("Unable to fetch free busy for %s  error code %s :: %s",
                                emailAddress,
                                attendeeAvailability.getResponseMessage().getResponseCode(),
                                attendeeAvailability.getResponseMessage().getMessageText());
                           
                            FreeBusy npFreeBusy = FreeBusy.nodataFreeBusy(emailAddress, startTime, endTime);
                            ret.add(npFreeBusy);
                            if (attendeeAvailability.getResponseMessage().getResponseCode().equals(ResponseCodeType.ERROR_NO_FREE_BUSY_ACCESS)) {
                                	npFreeBusy.mList.getHead().hasPermission = false;
                            }
                            ZimbraLog.fb.info("Error in response. continuing to next one sending nodata as response");
                            i++;
                            continue;
                        }
                        String fb = attendeeAvailability.getFreeBusyView().getMergedFreeBusy();
                        ZimbraLog.fb.info("Merged view Free Busy info received for user:%s is %s: ", emailAddress, fb);
                        ArrayList<FreeBusy> userIntervals = new ArrayList<FreeBusy>();
                        
                        if (fb == null) {
                            ZimbraLog.fb.warn("Merged view Free Busy info not avaiable for the user");
                            fb = "";  //Avoid NPE.
                        } else {
                            userIntervals.add(new ExchangeFreeBusyProvider.ExchangeUserFreeBusy(fb,
                                re.email, fb_interval, startTime, endTime));
                            ret.addAll(userIntervals);
                        }

                        // Parsing Detailed fb view response
                        if ("DetailedMerged".equals(fbResponseViewType) || "FreeBusyMerged".equals(fbResponseViewType)) {

                            parseDetailedFreeBusyResponse(emailAddress, startTime, endTime, attendeeAvailability, userIntervals);
                            ret.addAll(userIntervals);
                        } else {  // No FreeBusy view information available. returning nodata freebusy in response

                        	     ZimbraLog.fb.debug("No Free Busy view info avaiable for [%s], free busy view type from response : %s", emailAddress, fbResponseViewType);
                        	     ret.add(FreeBusy.nodataFreeBusy(emailAddress, startTime, endTime));
                        }
                    }
                    i++;
            	}
            }
        }
        return ret;
    }
    
   /*
    * This method parse the Detailed and FreeBusy view response information for each individual user,
    * who has those view information.
    */
    private static void parseDetailedFreeBusyResponse(String name, long start, long end, FreeBusyResponseType freeBusyResponse, ArrayList<FreeBusy> ret) {
        	ArrayOfCalendarEvent arrayOfCalendarEvent = null;
        	List<CalendarEvent> calendarEvents  = null;
        if (freeBusyResponse.getFreeBusyView() != null) {
            arrayOfCalendarEvent = freeBusyResponse.getFreeBusyView().getCalendarEventArray();

            if (arrayOfCalendarEvent != null) {
                calendarEvents = arrayOfCalendarEvent.getCalendarEvent();
                LegacyFreeBusyType legacyType;

                if (calendarEvents != null && calendarEvents.size() > 0) {
    
                    for (CalendarEvent event : calendarEvents) {
                        legacyType = event.getBusyType();
                        FreeBusy.Interval interval = getFreeBusyInterval(ret, event);
                        ZimbraLog.fb.debug(
                            "For user %s FB data received is: legacyType : %s, startTime : %s, "
                                + "endTime : %s",name, legacyType, event.getStartTime(), event.getEndTime());
                        if (event.getCalendarEventDetails() != null && interval != null) {
    
                            CalendarEventDetails calendarEventDetails = event.getCalendarEventDetails();
                            interval.id = calendarEventDetails.getID();
                            interval.location = calendarEventDetails.getLocation();
                            interval.subject = calendarEventDetails.getSubject();
                            interval.isMeeting = calendarEventDetails.isIsMeeting();
                            interval.isRecurring = calendarEventDetails.isIsRecurring();
                            interval.isException = calendarEventDetails.isIsException();
                            interval.isReminderSet = calendarEventDetails.isIsReminderSet();
                            interval.isPrivate = calendarEventDetails.isIsPrivate();
                            interval.detailsExist = true;
                            ZimbraLog.fb.debug("eventID : %s, location : %s, subject : %s, isMeeting : %b, "
                            + "isRecurring : %b, isException : %b, isReminderSet : %b, isPrivate : %b", 
                            interval.id, interval.location, interval.subject, interval.isMeeting,
                            interval.isRecurring, interval.isException, interval.isReminderSet, 
                            interval.isPrivate);
                    } else {
                        ZimbraLog.fb.debug("Calendar Event details not found for the user %s",
                            name);
                    }
                }
            } else {
                    ZimbraLog.fb.debug("No Calendar Information available for the user : %s", name);
                }
            }
        }
    }

    /**
     * @param ret
     * @param event
     * @return
     */
    private static FreeBusy.Interval getFreeBusyInterval(ArrayList<FreeBusy> ret, CalendarEvent event) {

        int tzOffset = TimeZone.getTimeZone("Z").getRawOffset();
        event.getStartTime().setTimezone(tzOffset);
        event.getEndTime().setTimezone(tzOffset);
        long startTime = event.getStartTime().toGregorianCalendar().getTimeInMillis();
        long endTime = event.getEndTime().toGregorianCalendar().getTimeInMillis();
        for (FreeBusy.Interval fbI : ret.get(0).mList ){
            if (fbI.mStart == startTime && fbI.mEnd == endTime) {
                return fbI;
            }
        }
        return null;
    }

    public static int checkAuth(ServerInfo info, Account requestor)
        throws ServiceException, IOException {
        ExchangeEWSFreeBusyProvider provider = new ExchangeEWSFreeBusyProvider();
        provider.initService(info);
        FolderType publicFolderRoot =
            (FolderType)provider.bindFolder(DistinguishedFolderIdNameType.PUBLICFOLDERSROOT,
                DefaultShapeNamesType.ALL_PROPERTIES);
        if (publicFolderRoot == null) {
            return 400;
        }
        return 200;
    }

    @Override
    public ExchangeEWSFreeBusyProvider getInstance() {
        return new ExchangeEWSFreeBusyProvider();
    }

    @Override
    public void addFreeBusyRequest(Request req) throws FreeBusyUserNotFoundException {
        ServerInfo info = null;
        for (ExchangeUserResolver resolver : sRESOLVERS) {
            String email = req.email;
            if (req.requestor != null)
                email = req.requestor.getName();
            info = resolver.getServerInfo(email);
            if (info != null) {
                if (!info.enabled)
                    throw new FreeBusyUserNotFoundException();
                if (null == service) {
                    try {
                        initService(info);
                    } catch (MalformedURLException e) {
                        ZimbraLog.fb.warn("failed to initialize provider", e);
                    }
                }
                break;
            }
        }
        if (info == null)
			throw new FreeBusyUserNotFoundException();
        addRequest(info, req);
    }

    public static void registerResolver(ExchangeUserResolver r, int priority) {
        synchronized (sRESOLVERS) {
            sRESOLVERS.ensureCapacity(priority + 1);
            sRESOLVERS.add(priority, r);
        }
    }

    private static ArrayList<ExchangeUserResolver> sRESOLVERS;
    static {
        sRESOLVERS = new ArrayList<ExchangeUserResolver>();

        registerResolver(new BasicUserResolver(), 0);
        register(new ExchangeEWSFreeBusyProvider());
    }

    private final HashMap<String, ArrayList<Request>> mRequests;

    @Override
    public List<FreeBusy> getResults() {
        ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
        for (Map.Entry<String, ArrayList<Request>> entry : mRequests.entrySet()) {
            try {
                ret.addAll(this.getFreeBusyForHost(entry.getKey(),
                    entry.getValue()));
            } catch (IOException e) {
                ZimbraLog.fb.error("error communicating with " + entry.getKey(),
                    e);
            }
        }
        return ret;
    }

    protected void addRequest(ServerInfo info, Request req) {
        ArrayList<Request> r = mRequests.get(info.url);
        if (r == null) {
            r = new ArrayList<Request>();
            mRequests.put(info.url, r);
        }
        req.data = info;
        r.add(req);
    }

    public ServerInfo getServerInfo(String emailAddr) {
        ServerInfo serverInfo = null;
        for (ExchangeUserResolver r : sRESOLVERS) {
            serverInfo = r.getServerInfo(emailAddr);
            if (serverInfo != null)
                break;
        }
        return serverInfo;
    }

    private static final String EXCHANGE_EWS = "EXCHANGE2010";

    @Override
    public String getName() {
        return EXCHANGE_EWS;
    }

    @Override
    public Set<Type> registerForItemTypes() {
        return EnumSet.of(MailItem.Type.APPOINTMENT);
    }

    private long getTimeInterval(String attr, String accountId, long defaultValue) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (accountId != null) {
            Account acct = prov.get(AccountBy.id, accountId);
            if (acct != null) {
                return acct.getTimeInterval(attr, defaultValue);
            }
        }
        return prov.getConfig().getTimeInterval(attr, defaultValue);
    }

    @Override
    public long cachedFreeBusyStartTime(String accountId) {
        Calendar cal = GregorianCalendar.getInstance();
        int curYear = cal.get(Calendar.YEAR);
        try {
            long dur = getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, accountId, 0);
            cal.setTimeInMillis(System.currentTimeMillis() - dur);
        } catch (ServiceException se) {
            // set to 1 week ago
            cal.setTimeInMillis(System.currentTimeMillis() -
                Constants.MILLIS_PER_WEEK);
        }
        // normalize the time to 00:00:00
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.get(Calendar.YEAR) < curYear) {
            // Exchange accepts FB info for only one calendar year. If the start date falls in the previous year
            // change it to beginning of the current year.
            cal.set(curYear, 0, 1);
        }
        return cal.getTimeInMillis();
    }

    @Override
    public long cachedFreeBusyEndTime(String accountId) {
        long duration = Constants.MILLIS_PER_MONTH * 2;
        Calendar cal = GregorianCalendar.getInstance();
        try {
            duration = getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, accountId, duration);
        } catch (ServiceException se) {}
        cal.setTimeInMillis(cachedFreeBusyStartTime(accountId) + duration);
        // normalize the time to 00:00:00
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    public long cachedFreeBusyStartTime() {
        return cachedFreeBusyStartTime(null);
    }

    @Override
    public long cachedFreeBusyEndTime() {
        return cachedFreeBusyEndTime(null);
    }

    @Override
    public String foreignPrincipalPrefix() {
        return Provisioning.FP_PREFIX_AD;
    }

    protected String getForeignPrincipal(String accountId) throws ServiceException {
    	String ret = null;
        Account acct =
            Provisioning.getInstance()
                .get(Key.AccountBy.id, accountId);
        if (acct == null)
            return null;
        String[] fps = acct.getForeignPrincipal();
        for (String fp : fps) {
            if (fp.startsWith(Provisioning.FP_PREFIX_AD)) {
                int idx = fp.indexOf(':');
                if (idx != -1) {
                    ret = fp.substring(idx + 1);
                    break;
                }
            }
        }
        return ret;
    }

    @Override
    public boolean registerForMailboxChanges(String accountId) {
        if (sRESOLVERS.size() > 1)
            return true;
        String email = null;
        try {
            Account account = null;
            if (accountId != null)
                account = Provisioning.getInstance().getAccountById(accountId);
            if (account != null)
                email = account.getName();
        } catch (ServiceException se) {
            ZimbraLog.fb.warn("cannot fetch account", se);
        }
        return getServerInfo(email) != null;
    }
}
