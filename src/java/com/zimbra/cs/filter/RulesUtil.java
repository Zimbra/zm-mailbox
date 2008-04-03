/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Jan 7, 2005
 *
 */
package com.zimbra.cs.filter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.dom4j.DocumentException;

import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.soap.*;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;

/**
 * Saves filter rules for an account.
 * 
 * The rules are expressed in XML.
 * 
 * @author kchen
 */
public class RulesUtil {

    private RulesUtil() {
    }
    
    /*
     * The input file is expected to have something like:
     * 
     * <rules>
     *   <r name="rule1">
     *     <g op="anyof">
     *       <c name="header" op=":contains" k0="From" k1="foo@example.zimbra.com"/>
     *       <c ... />
     *     </g>
     *     <action name="fileinto">
     *       <arg>/myfolder</arg>
     *     </action>
     *     <action>...
     *     </action>
     *   </r>
     *   <r ...>
     *     ...
     *   </r>
     * </rules>
     */
    private void usage() {
        System.out.println("Usage: java " + this.getClass().getName() + 
            " <account email addr> <password> <XML rule file path> <mail host> <mail port>");     
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        RulesUtil util = new RulesUtil();
        if (args.length !=5) {
            util.usage();
            return;
        }
        int port = Integer.parseInt(args[4]);
        util.saveRules(args[0], args[1], args[2], args[3], port);
    }
    
    private void saveRules(String acctEmail, String pwd, String path, String mailHost, int port) throws Exception {
        // construct URL to source host
        URL src = new URL("http", mailHost, port, ZimbraServlet.USER_SERVICE_URI);
        SoapTransport trans = new SoapHttpTransport(src.toExternalForm());
        
        // authenticate 
        Element authReq = createAuthRequest(acctEmail, pwd);
        Element authResp = trans.invokeWithoutSession(authReq);
        String authToken = authResp.getAttribute(AdminConstants.E_AUTH_TOKEN);
        String sessionId = authResp.getAttribute(HeaderConstants.E_SESSION, null);
        trans.setAuthToken(authToken);
        if (sessionId != null)
            trans.setSessionId(sessionId);

        // send mailbox export soap command to the source server
        Element reqDoc = createSaveRulesRequest(acctEmail, path);
        trans.invokeWithoutSession(reqDoc);
    }

    /**
     * @param acctEmail
     * @param path
     * @return
     * @throws DocumentException
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    private Element createSaveRulesRequest(String acctEmail, String path) throws DocumentException, UnsupportedEncodingException, IOException {
        String content = new String(ByteUtil.getContent(new File(path)), "utf-8");
        // gonna go with XML here; may want to switch to a binary protocol at some point
        Element rules = Element.parseXML(content);
        Element request = new Element.XMLElement(MailConstants.SAVE_RULES_REQUEST);
        request.addUniqueElement(rules);
        return request;
    }

    /**
     * @param acctEmail
     * @param pwd
     * @return
     */
    private Element createAuthRequest(String acctEmail, String pwd) {
        // gonna go with XML here; may want to switch to a binary protocol at some point
        Element request = new Element.XMLElement(AccountConstants.AUTH_REQUEST);
        request.addAttribute(AccountConstants.E_ACCOUNT, acctEmail, Element.Disposition.CONTENT);
        request.addAttribute(AccountConstants.E_PASSWORD, pwd, Element.Disposition.CONTENT);
        return request;
    }
}
