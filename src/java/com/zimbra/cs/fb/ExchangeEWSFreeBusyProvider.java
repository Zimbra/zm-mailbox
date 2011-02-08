package com.zimbra.cs.fb;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import com.microsoft.schemas.exchange.services._2006.messages.CreateItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.CreateItemType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeWebService;
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
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.UpdateItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.UpdateItemType;
import com.microsoft.schemas.exchange.services._2006.types.*;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.AuthScheme;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.ExchangeUserResolver;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.ServerInfo;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;

public class ExchangeEWSFreeBusyProvider extends FreeBusyProvider {
    public static final int FB_INTERVAL = 30;
    static ExchangeServicePortType service = null;

    boolean Initialize(ServerInfo info) throws MalformedURLException {
        URL wsdlUrl = ExchangeWebService.class.getResource("/Services.wsdl");
        ExchangeWebService factory =
            new ExchangeWebService(wsdlUrl,
                new QName("http://schemas.microsoft.com/exchange/services/2006/messages",
                    "ExchangeWebService"));
        service = factory.getExchangeWebPort();

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

    /* Enable the following static block to accept self signed certificates */
    // private static void setSSLConfig() throws Exception {
    // SSLContext context = SSLContext.getInstance("SSL");
    // context.init(null, trustAllCerts, new SecureRandom());
    // HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    // HttpsURLConnection.setDefaultHostnameVerifier(hv);
    //
    // }

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

    public boolean registerForMailboxChanges() {
        if (sRESOLVERS.size() > 1)
            return true;
        Config config = null;
        try {
            config = Provisioning.getInstance().getConfig();
        } catch (ServiceException se) {
            ZimbraLog.fb.warn("cannot fetch config", se);
            return false;
        }
        String url =
            config.getAttr(Provisioning.A_zimbraFreebusyExchangeURL, null);
        String user =
            config.getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername,
                null);
        String pass =
            config.getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword,
                null);
        String scheme =
            config.getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme,
                null);
        return (url != null && user != null && pass != null && scheme != null);
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
        service.getFolder(getFolderRequest,
            serverVersion,
            gfresponseHolder,
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
            (SearchExpressionType)ieq));

        findFolderRequest.setRestriction(rtRestriction);

        Holder<FindFolderResponseType> findFolderResponse =
            new Holder<FindFolderResponseType>();
        RequestServerVersion serverVersion = new RequestServerVersion();
        serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
        Holder<ServerVersionInfo> gfversionInfo =
            new Holder<ServerVersionInfo>();

        service.findFolder(findFolderRequest,
            serverVersion,
            findFolderResponse,
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
            (SearchExpressionType)contains));

        findFolderRequest.setRestriction(rtRestriction);

        Holder<FindFolderResponseType> findFolderResponse =
            new Holder<FindFolderResponseType>();
        RequestServerVersion serverVersion = new RequestServerVersion();
        serverVersion.setVersion(ExchangeVersionType.EXCHANGE_2010_SP_1);
        Holder<ServerVersionInfo> gfversionInfo =
            new Holder<ServerVersionInfo>();

        service.findFolder(findFolderRequest,
            serverVersion,
            findFolderResponse,
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
            (SearchExpressionType)ieq));

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
        service.findItem(findItemRequest,
            serverVersion,
            fiResponse,
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

    public boolean handleMailboxChange(String accountId) {
        String email;
        FreeBusy fb;
        try {
            email = getEmailAddress(accountId);
            fb = getFreeBusy(accountId, FreeBusyQuery.CALENDAR_FOLDER_ALL);
        } catch (ServiceException se) {
            ZimbraLog.fb.warn("can't get freebusy for account " + accountId, se);
            // retry the request if it's receivers fault.
            return !se.isReceiversFault();
        }
        if (email == null || fb == null) {
            ZimbraLog.fb.warn("account not found / incorrect / wrong host: " +
                accountId);
            return true; // no retry
        }
        ServerInfo serverInfo = getServerInfo(email);
        if (serverInfo == null || serverInfo.org == null ||
            serverInfo.cn == null) {
            ZimbraLog.fb.warn("no exchange server info for user " + email);
            return true; // no retry
        }
        if (null == service) {
            try {
                if (!Initialize(serverInfo)) {
                    ZimbraLog.fb.error("failed to initialize exchange service object " +
                        serverInfo.url);
                    return true;
                }
            } catch (MalformedURLException e) {
                ZimbraLog.fb.error("exception while trying to initialize exchange service object " +
                    serverInfo.url);
                return true;
            }
        }
        ExchangeEWSMessage msg =
            new ExchangeEWSMessage(serverInfo.org, serverInfo.cn, email);
        String url = serverInfo.url + msg.getUrl();

        try {
            ZimbraLog.fb.debug("POST " + url);
            FolderType publicFolderRoot =
                (FolderType)bindFolder(DistinguishedFolderIdNameType.PUBLICFOLDERSROOT,
                    DefaultShapeNamesType.ALL_PROPERTIES);
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
                            service.updateItem(updateItemRequest,
                                serverVersion,
                                updateItemResponse,
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
                            service.createItem(createItemRequest,
                                serverVersion,
                                createItemResponse,
                                gfversionInfo);
                            ResponseMessageType createItemResponseMessage =
                                createItemResponse.value.getResponseMessages()
                                    .getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage()
                                    .get(0)
                                    .getValue();

                        }

                    }
                }

            }

            return true;

        } catch (Exception e) {
            ZimbraLog.fb.error("error commucating to " + serverInfo.url, e);
        }

        return false;// retry
    }

    public List<FreeBusy>
        getFreeBusyForHost(String host, ArrayList<Request> req)
            throws IOException {
        ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();

        ArrayOfMailboxData attendees = new ArrayOfMailboxData();

        for (Request r : req) {
            EmailAddress email = new EmailAddress();
            email.setAddress(r.email);
            MailboxData mailbox = new MailboxData();
            mailbox.setEmail(email);
            mailbox.setAttendeeType(MeetingAttendeeType.REQUIRED);
            attendees.getMailboxData().add(mailbox);
        }
        try {
            Duration duration = new Duration();
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

            Date start = new Date(req.get(0).start);
            GregorianCalendar gregorianCalStart = new GregorianCalendar();
            gregorianCalStart.setTime(start);
            duration.setStartTime(datatypeFactory.newXMLGregorianCalendar(gregorianCalStart));

            Date end = new Date(req.get(0).end);
            GregorianCalendar gregorianCalEnd = new GregorianCalendar();
            gregorianCalEnd.setTime(end);
            duration.setEndTime(datatypeFactory.newXMLGregorianCalendar(gregorianCalEnd));

            FreeBusyViewOptionsType availabilityOpts =
                new FreeBusyViewOptionsType();
            availabilityOpts.setMergedFreeBusyIntervalInMinutes(FB_INTERVAL);

            availabilityOpts.getRequestedView().add("MergedOnly");
            availabilityOpts.setTimeWindow(duration);
            GetUserAvailabilityRequestType availabilityRequest =
                new GetUserAvailabilityRequestType();
            // TODO: check if we need to set request timezone
            SerializableTimeZone timezone = new SerializableTimeZone();
            timezone.setBias(TimeZone.getDefault().getRawOffset() / 1000 / 60 *
                -1);
            SerializableTimeZoneTime standardTime =
                new SerializableTimeZoneTime();
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

            service.getUserAvailability(availabilityRequest,
                serverVersion,
                availabilityResponse,
                gfversionInfo);
            List<FreeBusyResponseType> results =
                availabilityResponse.value.getFreeBusyResponseArray()
                    .getFreeBusyResponse();

            int i = 0;
            for (FreeBusyResponseType attendeeAvailability : results) {
                if (ResponseClassType.SUCCESS != attendeeAvailability.getResponseMessage()
                    .getResponseClass()) {
                    ZimbraLog.fb.warn("Error in response. continuing to next one");
                    continue;
                }
                ZimbraLog.fb.debug("Availability for " +
                    attendees.getMailboxData().get(i).getEmail().getAddress() +
                    " [" +
                    attendeeAvailability.getFreeBusyView().getMergedFreeBusy() +
                    "]");
                String fb =
                    attendeeAvailability.getFreeBusyView().getMergedFreeBusy();
                ret.add(new ExchangeFreeBusyProvider.ExchangeUserFreeBusy(fb,
                    attendees.getMailboxData().get(i).getEmail().getAddress(),
                    FB_INTERVAL,
                    req.get(0).start,
                    req.get(0).end));
                i++;
            }
        } catch (DatatypeConfigurationException dce) {
            ZimbraLog.fb.warn("getFreeBusyForHost DatatypeConfiguration failure",
                dce);
        } catch (Exception e) {
            ZimbraLog.fb.warn("getFreeBusyForHost failure", e);
        }

        return ret;
    }

    public static int checkAuth(ServerInfo info, Account requestor)
        throws ServiceException, IOException {
        // TODO make a dummy call to check we're passing
        if (null != service) {
            return 200;
        }
        return 400;
    }

    public ExchangeEWSFreeBusyProvider getInstance() {
        return new ExchangeEWSFreeBusyProvider();
    }

    public void addFreeBusyRequest(Request req) {
        ServerInfo info = null;
        for (ExchangeUserResolver resolver : sRESOLVERS) {
            String email = req.email;
            if (req.requestor != null)
                email = req.requestor.getName();
            info = resolver.getServerInfo(email);
            if (info != null) {
                if (null == service) {
                    try {
                        Initialize(info);
                    } catch (MalformedURLException e) {
                        ZimbraLog.fb.warn("failed to initialize provider", e);
                    }
                }
                break;
            }
        }
        addRequest(info, req);
    }

    public static void registerResolver(ExchangeUserResolver r, int priority) {
        synchronized (sRESOLVERS) {
            ZimbraLog.fb.error("entering registerResolver");
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

    private HashMap<String, ArrayList<Request>> mRequests;

    public List<FreeBusy> getResults() {
        ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
        for (Map.Entry<String, ArrayList<Request>> entry : mRequests.entrySet()) {
            try {
                ret.addAll(this.getFreeBusyForHost(entry.getKey(),
                    entry.getValue()));
            } catch (IOException e) {
                ZimbraLog.fb.error("error communicating to " + entry.getKey(),
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
        ZimbraLog.fb.error("entering getServerInfo " + emailAddr);
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

    @Override
    public long cachedFreeBusyStartTime() {
        Calendar cal = GregorianCalendar.getInstance();
        try {
            Config config = Provisioning.getInstance().getConfig();
            long dur =
                config.getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart,
                    0);
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
        return cal.getTimeInMillis();
    }

    @Override
    public long cachedFreeBusyEndTime() {
        long duration = Constants.MILLIS_PER_MONTH * 2;
        Calendar cal = GregorianCalendar.getInstance();
        try {
            Config config = Provisioning.getInstance().getConfig();
            duration =
                config.getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval,
                    duration);
        } catch (ServiceException se) {}
        cal.setTimeInMillis(cachedFreeBusyStartTime() + duration);
        // normalize the time to 00:00:00
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    public String foreignPrincipalPrefix() {
        return Provisioning.FP_PREFIX_AD;
    }

    protected String getForeignPrincipal(String accountId) throws ServiceException {
    	String ret = null;
        Account acct =
            Provisioning.getInstance()
                .get(Provisioning.AccountBy.id, accountId);
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
        return registerForMailboxChanges();
    }
}
