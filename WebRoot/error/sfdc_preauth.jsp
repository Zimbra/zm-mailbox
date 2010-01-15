<!--
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
-->
<%@ page import="org.apache.commons.httpclient.methods.PostMethod" %>
<%@ page import="org.apache.commons.httpclient.methods.StringRequestEntity" %>
<%@ page import="org.apache.commons.httpclient.HttpClient" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="javax.crypto.Mac" %>
<%@ page import="java.security.NoSuchAlgorithmException" %>
<%@ page import="java.security.InvalidKeyException" %>
<%@ page import="javax.crypto.SecretKey" %>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="java.io.UnsupportedEncodingException"%>
<%--taglib prefix="z" uri="/WEB-INF/zimbra.tld" %>
<z:zimletconfig var="config" action="list" zimlet="com_zimbra_sforce"/>

    Map zConfig = (Map) request.getAttribute("config");
    String domain_key = (String) ((Map) zConfig.get("global")).get("preauthDomainKey");
    String redirect_user = (String) ((Map) zConfig.get("global")).get("redirectUser");
--%>
<%!
    public static String generateRedirect(HttpServletRequest request, String name, String domain_key) throws UnsupportedEncodingException {
        HashMap<String, String> params = new HashMap<String, String>();
        String ts = System.currentTimeMillis() + "";
        params.put("account", name);
        params.put("by", "name"); // needs to be part of hmac
        params.put("timestamp", ts);
        params.put("expires", "0"); // means use the default

        String preAuth = computePreAuth(params, domain_key);
        String preAuthURL= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() +
                "/service/preauth/?" +
                "account=" + name +
                "&by=name" +
                "&timestamp=" + ts +
                "&expires=0" +
                "&preauth=" + preAuth;
        String redirectURL = "/service/zimlet/com_zimbra_sforce/welcome.jsp?url=" + URLEncoder.encode(URLEncoder.encode(preAuthURL,"UTF-8"),"UTF-8");
        return preAuthURL + "&redirectURL=" + redirectURL;
    }

    public static String computePreAuth(Map<String, String> params, String key) {
        TreeSet<String> names = new TreeSet<String>(params.keySet());
        StringBuffer sb = new StringBuffer();
        for (Object name : names) {
            if (sb.length() > 0) sb.append('|');
            sb.append(params.get(name));
        }
        return getHmac(sb.toString(), key.getBytes());
    }

    private static String getHmac(String data, byte[] key) {
        try {
            ByteKey bk = new ByteKey(key);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(bk);
            return toHex(mac.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal error", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("fatal error", e);
        }
    }

    static class ByteKey implements SecretKey {
        private byte[] mKey;

        ByteKey(byte[] key) {
            mKey = (byte[]) key.clone();
        }

        public byte[] getEncoded() {
            return mKey;
        }

        public String getAlgorithm() {
            return "HmacSHA1";
        }

        public String getFormat() {
            return "RAW";
        }
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte aData : data) {
            sb.append(hex[(aData & 0xf0) >>> 4]);
            sb.append(hex[aData & 0x0f]);
        }
        return sb.toString();
    }

    private static final char[] hex =
            {'0', '1', '2', '3', '4', '5', '6', '7',
                    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
%>
<%
    String server = request.getParameter("server");
    String user = request.getParameter("userid");
    String sess = request.getParameter("session");

    // Make sure we've got all the required params
    if (server == null || user == null || sess == null) {
        out.println("<h1>Error! A required field sever, userid, or session is NULL!</h1>");
        return;
    }

    // Make sure the server is a salesforce.com server
    if (!server.startsWith("https://") || server.indexOf("/", 8) < 0) {
        out.println("<h1>Error! Not https or not valid path after server</h1>");
        return;
    }

    if (!server.substring(0, server.indexOf("/", 8)).endsWith("salesforce.com")) {
        out.println("<h1>Error! Not a salesforce.com server!</h1>");
        return;
    }

    // Quick SOAP generator
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "                  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header>\n" +
            "        <ns1:SessionHeader soapenv:mustUnderstand=\"0\" xmlns:ns1=\"urn:enterprise.soap.sforce.com\">\n" +
            "            <ns2:sessionId xmlns:ns2=\"urn:enterprise.soap.sforce.com\">" + sess + "</ns2:sessionId>\n" +
            "        </ns1:SessionHeader>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <query xmlns=\"urn:enterprise.soap.sforce.com\">\n" +
            "            <queryString>select Email from User where id='" + user + "'</queryString>\n" +
            "        </query>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    // Send it off to SFDC to validate the userid and get the email address
    PostMethod post = new PostMethod(server);
    post.setRequestEntity(new StringRequestEntity(xml));
    post.setRequestHeader("Content-type", "text/xml; charset=utf-8");
    post.setRequestHeader("SOAPAction", "m");
    HttpClient httpclient = new HttpClient();
    int result = httpclient.executeMethod(post);
    String email;
    if (result != 200) {
        out.println("<h1>Error! Failed to validate userid</h1><br/>");
        out.println("<pre>");
        out.println(post.getResponseBodyAsString());
        out.println("</pre>");
        return;
    } else {
        // Got a good login
        String xmlResult = post.getResponseBodyAsString();
        // Quick SOAP parser
        int start = xmlResult.indexOf("<sf:Email>") + 10;
        int end = xmlResult.indexOf("</sf:Email>");
        email = xmlResult.substring(start, end);
    }
    post.releaseConnection();
    // Allow global config to hard-code a user for testing/demo use
    // Hack hack hack
    String redirect_user = "sample@roadshow.zimbra.com";
    String domain_key = "3f22c69abb92ce8d0b5bd30bd4cc3acd4ecd535783490e8722f9903ced6c61ee";
    if (redirect_user != null) {
        email = redirect_user;
    }
    String redirect = generateRedirect(request, email, domain_key);
    response.sendRedirect(redirect);
%>