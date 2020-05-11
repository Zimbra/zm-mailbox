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
package com.zimbra.cs.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;

public class AutoDiscoverServlet extends ZimbraServlet {

    private static final long serialVersionUID = 1921058393177879727L;
    private static final Log log = LogFactory.getLog(AutoDiscoverServlet.class);

    private static final String BASIC_AUTH_HEADER = "Authorization";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String WWW_AUTHENTICATE_VALUE = "Basic realm=\"Zimbra\"";
    private static final String NTLM = "NTLM";
    private static final String NEGOTIATE = "Negotiate";

    private static final String MS_ACTIVESYNC_PATH = "/Microsoft-Server-ActiveSync";
    private static final String EWS_SERVICE_PATH = "/ews/Exchange.asmx";

    private static final String NS_MOBILE = "http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006";
    private static final String NS_OUTLOOK = "http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a";
    private static final String NS        = "http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006";
    private static final String XSI_NS    = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSD_NS    = "http://www.w3.org/2001/XMLSchema";

    @Override
    public void init() throws ServletException {
        log.info("Starting up");
        super.init();
    }

    @Override
    public void destroy() {
        log.info("Shutting down");
        super.destroy();
    }

    public static String getServiceUrl(Account acct, String client) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getServer(acct);
        Domain domain = prov.getDomain(acct);
        String serviceUrl = "";

        if (!isEwsClient(client)) {
	        if (LC.zimbra_activesync_autodiscover_use_service_url.booleanValue()) {
	            serviceUrl = URLUtil.getServiceURL(server, AutoDiscoverServlet.MS_ACTIVESYNC_PATH, true);
	        } else {
	            serviceUrl = URLUtil.getPublicURLForDomain(server, domain, AutoDiscoverServlet.MS_ACTIVESYNC_PATH, true);
	        }
        } else {

            if (LC.zimbra_ews_autodiscover_use_service_url.booleanValue()) {
                serviceUrl = URLUtil.getServiceURL(server, AutoDiscoverServlet.EWS_SERVICE_PATH, true);
            } else {
                serviceUrl = URLUtil.getPublicURLForDomain(server, domain, AutoDiscoverServlet.EWS_SERVICE_PATH, true);
            }
        }

        //fix for bug 83212 suppress port number, if default port is used as per the protocol
        if (serviceUrl.toLowerCase().startsWith(URLUtil.PROTO_HTTPS)) {
            serviceUrl = serviceUrl.replace(":" + URLUtil.DEFAULT_HTTPS_PORT + "/","/");
        } else if (serviceUrl.toLowerCase().startsWith(URLUtil.PROTO_HTTP)) {
            serviceUrl = serviceUrl.replace(":" + URLUtil.DEFAULT_HTTP_PORT + "/","/");
        }

        return serviceUrl;
    }

    //FIXME for windows phone
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userAgent = req.getHeader("User-Agent");
        if (userAgent != null && (userAgent.contains("PocketPC") || userAgent.contains("SmartPhone"))) {
            if (log.isDebugEnabled()) {
                Enumeration<String> enm = req.getHeaderNames();
                while (enm.hasMoreElements()) {
                    String header = enm.nextElement();
                    log.debug("GET header: %s", header + ":" + req.getHeader(header));
                }
            }
            if (req.isSecure()) {
                Account acct = authenticate(req, resp, NS_MOBILE);
                if (acct == null) {
                    return;
                }
            } else {
                resp.sendRedirect(LC.zimbra_activesync_autodiscover_url.value());
            }
        }
    }

    //Headers:
    //Authorization: Basic dXNlcjJAc3VkaXB0by1tcHJvLmxvY2FsOnRlc3QxMjM=
    //MS-ASProtocolVersion: 2.5
    //Connection: keep-alive
    //User-Agent: Android/0.3
    //Content-Type: text/xml
    //Content-Length: 379
    //Host: autodiscover.sudipto-mpro.local
    //
    //<?xml version='1.0' encoding='UTF-8' standalone='no' ?>
    //  <Autodiscover xmlns="http://schemas.microsoft.com/exchange/autodiscover/mobilesync/requestschema/2006">
    //    <Request>
    //      <EMailAddress>user2@sudipto-mpro.local</EMailAddress>
    //      <AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006</AcceptableResponseSchema>
    //    </Request>
    //  </Autodiscover>
    //
    //
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);

        log.info("Handling autodiscover request...");

        byte[] reqBytes = null;
        reqBytes = ByteUtil.getContent(req.getInputStream(), req.getContentLength());
        if (reqBytes == null) {
            log.warn("No content found in the request");
            sendError(resp, 600, "No content found in the request");
            return;
        }
        String content = new String(reqBytes, "UTF-8");
        log.debug("Request before auth: %s", content);

        if (log.isDebugEnabled()) {
            Enumeration<String> enm = req.getHeaderNames();
            while (enm.hasMoreElements()) {
                String header = enm.nextElement();
                log.info("POST header: %s", header + ":" + req.getHeader(header));
            }
        }



        String email = null;
        String responseSchema = null;

        try {
            DocumentBuilderFactory docBuilderFactory = makeDocumentBuilderFactory();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(content)));
            NodeList nList = doc.getElementsByTagName("Request");

            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    email = getTagValue("EMailAddress", element);
                    responseSchema = getTagValue("AcceptableResponseSchema", element);

                    if (email != null)
                        break;
                }
            }
        } catch (Exception e) {
            log.warn("cannot parse body: %s", content);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Body cannot be parsed");
            return;
        }

        //Return an error if there's no email address specified!
        if (email == null || email.length() == 0) {
            log.warn("No Email address is specified in the Request, %s", content);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No Email address is specified in the Request");
            return;
        }

        //Return an error if the response schema doesn't match ours!
        if (responseSchema != null && responseSchema.length() > 0) {

            if (!(responseSchema.equals(NS_MOBILE) || responseSchema.equals(NS_OUTLOOK))) {
                log.warn("Requested response schema not available " + responseSchema);
                sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Requested response schema not available " + responseSchema);
                return;
            }
        }

        log.debug("Authenticating user");
        Account acct = authenticate(req, resp, responseSchema);
        if (acct == null) {
            return;
        }
        log.debug("Authentication finished successfully");

        log.debug("content length: %d, content type: %s", req.getContentLength(), req.getContentType());
        if (req.getContentLength() == 0 || req.getContentType() == null) {
            log.warn("No suitable content found in the request");
            sendError(resp, 600, "No suitable content found in the request");
            return;
        }

        try {
            if (! (AccountUtil.addressMatchesAccount(acct, email) ||
                acct.isAvailabilityServiceProvider())) { //Exchange server sends dummy email address from service account.
                log.warn(email + " doesn't match account addresses for user " + acct.getName());
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        email + " doesn't match account addresses");
                return;
            }
        } catch (ServiceException e) {
            log.warn("Account access error; user=" + acct.getName(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Account access error; user=" + acct.getName());
            return;
        }

        String respDoc = null;
        try {
            String serviceUrl = getServiceUrl(acct, responseSchema);
            String displayName = acct.getDisplayName() == null ? email : acct.getDisplayName();
            if (displayName.contains("@")) {
                displayName = displayName.substring(0, displayName.indexOf("@"));
            }
            log.debug("displayName: %s, email: %s, serviceUrl: %s", displayName, email, serviceUrl);
            if (isEwsClient(responseSchema)) {
            	respDoc = createResponseDocForEws(displayName, email, serviceUrl, acct);
            } else {
            	respDoc = createResponseDoc(displayName, email, serviceUrl);
            }
        } catch (Exception e) {
            log.warn(e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        log.info("Response: %s", respDoc);
        log.debug("response length: %d", respDoc.length());

        try {
            ByteUtil.copy(new ByteArrayInputStream(respDoc.getBytes("UTF-8")), true, resp.getOutputStream(), false);
        } catch (IOException e) {
            log.error("copy response error", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        log.debug("setting content type to text/xml");
        resp.setContentType("text/xml");
        log.info("sending autodiscover response...");
    }

    private Account authenticate(HttpServletRequest req, HttpServletResponse resp, String responseSchema) throws ServletException, IOException {
        //The basic auth header looks like this:
        //Authorization: Basic emltYnJhXFx1c2VyMTp0ZXN0MTIz
        //The base64 encoded credentials can be either <domain>\<user>:<pass> or just <user>:<pass>
        String auth = req.getHeader(BASIC_AUTH_HEADER);
        log.debug("auth header: %s", auth);

        if (auth == null || !auth.toLowerCase().startsWith("basic ")) {
            log.warn("No basic auth header in the request");
            resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
            //            resp.addHeader(WWW_AUTHENTICATE_HEADER, NTLM);
            //            resp.addHeader(WWW_AUTHENTICATE_HEADER, NEGOTIATE);
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return null;
        }

        // 6 comes from "Basic ".length();
        String cred = new String(Base64.decodeBase64(auth.substring(6).getBytes()));

        int bslash = cred.indexOf('\\');

        int colon = cred.indexOf(':');
        if (colon == -1 || colon <= bslash + 1) {
            log.warn("Invalid basic auth credentials");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid basic auth credentials");
            return null;
        }

        String domain = bslash > 0 ? cred.substring(0, bslash) : "";
        String userPassedIn = cred.substring(bslash + 1, colon);
        String user = cred.substring(bslash + 1, colon);
        String pass = cred.substring(colon + 1);

        log.debug("user=%s, domain=%s", user, domain);
        if (pass.length() == 0) {
            log.warn("Empty password");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Empty password");
            return null;
        }

        if (domain.length() > 0 && user.indexOf('@') == -1) {
            if (domain.charAt(0) != '@') {
                user += '@';
            }
            user += domain;
        }

        try {
            Provisioning prov = Provisioning.getInstance();
            if (user.indexOf('@') == -1) {
                String defaultDomain = prov.getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
                if (defaultDomain == null) {
                    log.warn("Ldap access error; user=" + user);
                    sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ldap access error");
                    return null;
                }
                user = user + "@" + defaultDomain;
            }

            Account account = prov.get(AccountBy.name, user);
            if (account == null) {
                log.warn("User not found; user=" + user);
                resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                return null;
            }

            try {
                Map<String, Object> authCtxt = new HashMap<String, Object>();
                authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
                authCtxt.put(AuthContext.AC_REMOTE_IP, ZimbraServlet.getClientIp(req));
                authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, userPassedIn);
                authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
                prov.authAccount(account, pass, AuthContext.Protocol.zsync, authCtxt);
            } catch (ServiceException x) {
                log.warn("User password mismatch; user=" + user);
                resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                return null;
            }

            if (isEwsClient(responseSchema) && !account.getBooleanAttr(Provisioning.A_zimbraFeatureEwsEnabled, false)) {
            	log.info("User account not enabled for ZimbraEWS; user=" + user);
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Account not enabled for ZimbraEWS");
                return null;
            }

            if (!isEwsClient(responseSchema) && !account.getBooleanAttr(Provisioning.A_zimbraFeatureMobileSyncEnabled, false)) {
                log.info("User account not enabled for ZimbraSync; user=" + user);
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Account not enabled for ZimbraSync");
                return null;
            }


            return account;
        } catch (ServiceException x) {
            log.warn("Account access error; user=" + user, x);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Account access error; user=" + user);
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static List<Element> getChildren(Element e) {
        NodeList nl = e.getChildNodes();
        List<Element> nodes = new ArrayList<Element>(nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                nodes.add((Element) node);
            }
        }
        return nodes;
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nlList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }

    private static void sendError(HttpServletResponse resp, int errCode, String reason) throws IOException {
        log.warn("HTTP/1.1 " + errCode + " " + reason);
        resp.sendError(errCode, reason);
    }

    //<Autodiscover xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006">
    //  <Response xmlns="http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006">
    //    <Culture>en:en</Culture>
    //    <User>
    //      <DisplayName>Demo User One</DisplayName>
    //      <EMailAddress>user1@sudipto-mpro.local</EMailAddress>
    //    </User>
    //    <Action>
    //      <Settings>
    //        <Server>
    //          <Type>MobileSync</Type>
    //          <Url>https://sudipto-mpro.local/Microsoft-Server-ActiveSync</Url>
    //          <Name>https://sudipto-mpro.local/Microsoft-Server-ActiveSync</Name>
    //        </Server>
    //      </Settings>
    //    </Action>
    //  </Response>
    //</Autodiscover>
    //
    //
    //Error Case:
    //<Autodiscover xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006">
    //  <Response xmlns="http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006">
    //    <Culture>en:en</Culture>
    //    <User>
    //      <DisplayName>Demo User One</DisplayName>
    //      <EMailAddress>user1@sudipto-mpro.local</EMailAddress>
    //    </User>
    //    <Action>
    //      <Error>
    //        <Status>1</Status>
    //        <Message>Active Directory currently not available</Message>
    //        <DebugData>UserMailbox</DebugData>
    //      </Error>
    //    </Action>
    //  </Response>
    //</Autodiscover>
    //
    private static String createResponseDoc(String displayName, String email, String serviceUrl) throws Exception {
        DocumentBuilderFactory factory = makeDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.newDocument();

        Element root = xmlDoc.createElementNS(NS, "Autodiscover");
        root.setAttribute("xmlns", NS);
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttribute("xmlns:xsd", XSD_NS);
        xmlDoc.appendChild(root);

        //Add the response element.
        Element response =  xmlDoc.createElementNS(NS_MOBILE, "Response");
        root.appendChild(response);

        //Add culture to to response
        Element culture = xmlDoc.createElement("Culture");
        culture.appendChild(xmlDoc.createTextNode("en:en"));
        response.appendChild(culture);

        //User
        Element user = xmlDoc.createElement("User");
        Element displayNameElm = xmlDoc.createElement("DisplayName");
        displayNameElm.appendChild(xmlDoc.createTextNode(displayName));
        user.appendChild(displayNameElm);
        Element emailAddr = xmlDoc.createElement("EMailAddress");
        emailAddr.appendChild(xmlDoc.createTextNode(email));
        user.appendChild(emailAddr);
        response.appendChild(user);

        //Action
        Element action = xmlDoc.createElement("Action");
        Element settings = xmlDoc.createElement("Settings");
        Element server = xmlDoc.createElement("Server");

        Element type = xmlDoc.createElement("Type");
        type.appendChild(xmlDoc.createTextNode("MobileSync"));
        server.appendChild(type);

        Element url = xmlDoc.createElement("Url");
        url.appendChild(xmlDoc.createTextNode(serviceUrl));
        server.appendChild(url);

        Element name = xmlDoc.createElement("Name");
        name.appendChild(xmlDoc.createTextNode(serviceUrl));
        server.appendChild(name);

        settings.appendChild(server);
        action.appendChild(settings);
        response.appendChild(action);

        TransformerFactory transformerFactory = makeTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(xmlDoc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        writer.flush();
        String xml = writer.toString();
        writer.close();

        //manually generate xmlns for Autodiscover and Response element, this works
        //for testexchangeconnectivity.com, but iOS and Android don't like Response's xmlns
        //        StringBuilder str = new StringBuilder();
        //        str.append("<?xml version=\"1.0\"?>\n");
        //        str.append("<Autodiscover xmlns:xsd=\"").append(XSD_NS).append("\"");
        //        str.append(" xmlns:xsi=\"").append(XSI_NS).append("\"");
        //        str.append(" xmlns=\"").append(NS).append("\">\n");
        //        int respIndex = xml.indexOf("<Response>");
        //        str.append("<Response xmlns=\"").append(NS_MOBILE).append("\">");
        //        str.append(xml.substring(respIndex + "<Response>".length(), xml.length()));
        //        return str.toString();
        return "<?xml version=\"1.0\"?>\n" + xml;
    }


    private static String createResponseDocForEws(String displayName, String email, String serviceUrl, Account acct) throws Exception {

    	Provisioning prov = Provisioning.getInstance();
        Server server = prov.getServer(acct);

        String cn = server.getCn();
        String name = server.getName();
        String acctId = acct.getId();


        DocumentBuilderFactory factory = makeDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.newDocument();

        Element root = xmlDoc.createElementNS(NS, "Autodiscover");
        root.setAttribute("xmlns", NS);
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttribute("xmlns:xsd", XSD_NS);
        xmlDoc.appendChild(root);

        //Add the response element.
        Element response = xmlDoc.createElementNS(NS_OUTLOOK, "Response");
        root.appendChild(response);

        //User
        Element user = xmlDoc.createElement("User");
        Element displayNameElm = xmlDoc.createElement("DisplayName");
        displayNameElm.appendChild(xmlDoc.createTextNode(displayName));
        user.appendChild(displayNameElm);
        Element emailAddr = xmlDoc.createElement("EmailAddress");
        emailAddr.appendChild(xmlDoc.createTextNode(email));
        user.appendChild(emailAddr);

        Element depId = xmlDoc.createElement("DeploymentId");
        depId.appendChild(xmlDoc.createTextNode(acctId));
        user.appendChild(depId);


        response.appendChild(user);

        //Action
        Element account = xmlDoc.createElement("Account");
        Element acctType = xmlDoc.createElement("AccountType");
        acctType.appendChild(xmlDoc.createTextNode("email"));
        account.appendChild(acctType);
        response.appendChild(account);


        Element action = xmlDoc.createElement("Action");
        action.appendChild(xmlDoc.createTextNode("settings"));
        account.appendChild(action);


        Element protocol = xmlDoc.createElement("Protocol");
        account.appendChild(protocol);

        Element type = xmlDoc.createElement("Type");
        type.appendChild(xmlDoc.createTextNode("EXCH"));
        protocol.appendChild(type);

        Element ews = xmlDoc.createElement("EwsUrl");
        protocol.appendChild(ews);
        ews.appendChild(xmlDoc.createTextNode(serviceUrl));

        Element as = xmlDoc.createElement("ASUrl");
        protocol.appendChild(as);
        as.appendChild(xmlDoc.createTextNode(serviceUrl));

        TransformerFactory transformerFactory = makeTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(xmlDoc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        writer.flush();
        String xml = writer.toString();
        writer.close();
        return "<?xml version=\"1.0\"?>\n" + xml;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(createResponseDoc("test user", "b@c.com", "http://mail.com/MS-ActiveSync"));
    }

    public static boolean isEwsClient(String responseSchema) {
    	boolean ewsClient = false;
    	if (responseSchema.equals(NS_OUTLOOK) ) {
    	    ewsClient = true;
    	}

    	return ewsClient;

    }

    public static DocumentBuilderFactory makeDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // XXE attack prevention
        factory.setFeature(Constants.DISALLOW_DOCTYPE_DECL, true);
        factory.setFeature(Constants.EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(Constants.EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(Constants.LOAD_EXTERNAL_DTD, false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    public static TransformerFactory makeTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }
}
