package com.zimbra.cs.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
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
    private static final String WWW_AUTHENTICATE_VALUE = "BASIC realm=\"Zimbra\"";
    
    private static final String MS_ACTIVESYNC_PATH = "/Microsoft-Server-ActiveSync";
    
    private static final String SC = "http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006";
    private static final String NS = "http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";
    
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
    
    public static String getServiceUrl(Account acct) throws ServiceException {
        String url = LC.zimbra_activesync_autodiscover_url.value();
        if (url != null && url.length() > 0)
            return url;
        
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getServer(acct);
        Domain domain = prov.getDomain(acct);
        
        if (LC.zimbra_activesync_autodiscover_use_service_url.booleanValue())
            return URLUtil.getServiceURL(server, AutoDiscoverServlet.MS_ACTIVESYNC_PATH, true);
        
        return URLUtil.getPublicURLForDomain(server, domain, AutoDiscoverServlet.MS_ACTIVESYNC_PATH, true);
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
        
        Account acct = authenticate(req, resp);
        if (acct == null)
            return;
        
        if (req.getContentLength() == 0 || req.getContentType() == null || !req.getContentType().toLowerCase().equals("text/xml")) {
            log.warn("No suitable content found in the request");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No suitable content found in the request");
            return;
        }
        
        byte[] reqBytes = null;
        reqBytes = ByteUtil.getContent(req.getInputStream(), req.getContentLength());
        if (reqBytes == null) {
            log.warn("No content found in the request");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No content found in the request");
            return;
        }
        
        String email = null;
        String responseSchema = null;
        String content = new String(reqBytes, "UTF-8");
        
        log.info("Request: " + content);
        
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(content)));
            NodeList nList = doc.getElementsByTagName("Request");
                
            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);    
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    email = getTagValue("EMailAddress",element);
                    responseSchema = getTagValue("AcceptableResponseSchema",element);
                    
                    if (email != null)
                        break;
                }
            }
        } catch (Exception e) {
            log.warn("cannot parse body " + content);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Body cannot be parsed");
            return;
        }
            
        //Return an error if there's no email address specified!
        if (email == null || email.length() == 0) {
            log.warn("No Email address is specified in the Request " + content);
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No Email address is specified in the Request");
            return;
        }
            
        //Return an error if the response schema doesn't match ours!
        if (responseSchema != null && responseSchema.length() > 0 && !responseSchema.equals(SC)) {
            log.warn("Requested response schema not available " + responseSchema);
            sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Requested response schema not available " + responseSchema);
            return;
        }
        
        try {
            if (!AccountUtil.addressMatchesAccount(acct, email)) {
                log.warn(email + " doesn't match account addresses for user " + acct.getName());
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, email + " doesn't match account addresses");
                return;
            }
        } catch (ServiceException e) {
            log.warn("Account access error; user=" + acct.getName(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Account access error; user=" + acct.getName());
            return;
        }
        
        String respDoc = null;
        try {
            String serviceUrl = getServiceUrl(acct);
            String displayName = acct.getDisplayName() == null ? email : acct.getDisplayName();
            respDoc = createResponseDoc(displayName, email, serviceUrl);
        } catch (Exception e) {
            log.warn(e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }
        
        log.info("Response: " + respDoc);
        
        resp.setContentType("application/xml");
        resp.setContentLength(respDoc.length());
        ByteUtil.copy(new ByteArrayInputStream(respDoc.getBytes("UTF-8")), true, resp.getOutputStream(), false);
    }
    
    private Account authenticate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //The basic auth header looks like this:
        //Authorization: Basic emltYnJhXFx1c2VyMTp0ZXN0MTIz
        //The base64 encoded credentials can be either <domain>\<user>:<pass> or just <user>:<pass>
        String auth = req.getHeader(BASIC_AUTH_HEADER);

        if (auth == null || !auth.toLowerCase().startsWith("basic ")) {
            log.warn("No basic auth header in the request");
            resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return null;
        }
        
        // 6 comes from "Basic ".length();
        String cred = new String(Base64.decodeBase64(auth.substring(6).getBytes()));
        
        int bslash = cred.indexOf('\\');
        
        int colon = cred.indexOf(':');
        if (colon == -1 || colon <= bslash+1) {
            log.warn("Invalid basic auth credentials");
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid basic auth credentials");
            return null;
        }

        String domain = bslash > 0 ? cred.substring(0, bslash) : "";
        String userPassedIn = cred.substring(bslash+1, colon);
        String user = cred.substring(bslash+1, colon);
        String pass = cred.substring(colon+1);
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
                authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, userPassedIn);
                authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
                prov.authAccount(account, pass, AuthContext.Protocol.zsync, authCtxt);
            } catch (ServiceException x) {
                log.warn("User password mismatch; user=" + user);
                resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
                sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                return null;
            }
            
            if (!account.getBooleanAttr(Provisioning.A_zimbraFeatureMobileSyncEnabled, false)) {
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
    
    private static String getTagValue(String tag, Element element){
        NodeList nlList= element.getElementsByTagName(tag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0); 
     
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation domImpl = builder.getDOMImplementation();
        
        //create a new doc
        Document xmlDoc = domImpl.createDocument(NS,"Autodiscover",null);

        //Add namespace declarations to the root element.
        Element root = xmlDoc.getDocumentElement();
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttribute("xmlns:xsd", XSD_NS);
        root.setAttribute("xmlns", NS);

        //Add the response element.
        Element response = xmlDoc.createElementNS(NS,"Response");
        response.setAttribute("xmlns", NS);       
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
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(xmlDoc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        writer.flush();
        return writer.toString();
    }

}
