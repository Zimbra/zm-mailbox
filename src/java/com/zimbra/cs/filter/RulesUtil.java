/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;

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
        Zimbra.toolSetup();
        RulesUtil util = new RulesUtil();
        RuleManager mgr = RuleManager.getInstance();
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
        String authToken = authResp.getAttribute(AdminService.E_AUTH_TOKEN);
        String sessionId = authResp.getAttribute(ZimbraSoapContext.E_SESSION_ID, null);
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
        Element request = new Element.XMLElement(MailService.SAVE_RULES_REQUEST);
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
        Element request = new Element.XMLElement(AccountService.AUTH_REQUEST);
        request.addAttribute(AccountService.E_ACCOUNT, acctEmail, Element.DISP_CONTENT);
        request.addAttribute(AccountService.E_PASSWORD, pwd, Element.DISP_CONTENT);
        return request;
    }
}
