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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.common.util.CliUtil;

public class SoapCommandUtil {

    private static final Map<String, Namespace> sTypeToNamespace =
        new HashMap<String, Namespace>();
    private static final Options sOptions = new Options();
    
    private static final String OPT_HELP = "h";
    private static final String OPT_TYPE = "t";
    private static final String OPT_ELEMENT = "e";
    private static final String OPT_USER = "u";
    private static final String OPT_PASSWORD = "p";
    
    static {
        // Namespaces
        sTypeToNamespace.put("mail", Namespace.get("urn:zimbraMail"));
        sTypeToNamespace.put("admin", Namespace.get("urn:zimbraAdmin"));
        sTypeToNamespace.put("account", Namespace.get("urn:zimbraAccount"));
        
        // Options
        Option opt = new Option(OPT_TYPE, "type", true, "SOAP message type (mail, account, admin).");
        opt.setRequired(true);
        sOptions.addOption(opt);
        
        opt = new Option(OPT_USER, "user", true, "Username.");
        opt.setRequired(true);
        sOptions.addOption(opt);
        
        opt = new Option(OPT_PASSWORD, "password", true, "Password.");
        opt.setRequired(true);
        sOptions.addOption(opt);
        
        sOptions.addOption(OPT_ELEMENT, "element", true, "The root element.  Subsequent paths will be relative to this element.");
        sOptions.addOption(OPT_HELP, "help", false, "Display command line help.");
    }
    
    private String mUrl;
    private String mType;
    private Namespace mNamespace;
    private String mUser;
    private String mPassword;
    
    private SoapCommandUtil(String url, String type, String user, String password) {
        mUrl = url;
        mType = type;
        mNamespace = sTypeToNamespace.get(type);
        mUser = user;
        mPassword = password;
    }
    
    private void run(String rootPath, String[] paths)
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(mUrl);
        
        // Create auth element
        Element auth = null;
        
        if (mType.equals("admin")) {
            auth = DocumentHelper.createElement(AdminConstants.AUTH_REQUEST);
            auth.addElement(AdminConstants.E_NAME).setText(mUser);
            auth.addElement(AdminConstants.E_PASSWORD).setText(mPassword);
        } else {
            auth = DocumentHelper.createElement(AccountConstants.AUTH_REQUEST);
            Element account = DomUtil.add(auth, AccountConstants.E_ACCOUNT, mUser);
            account.addAttribute(AdminConstants.A_BY, AdminConstants.BY_NAME);
            auth.addElement(AccountConstants.E_PASSWORD).setText(mPassword);
        }
        
        // Authenticate and get auth token
        com.zimbra.common.soap.Element response = null;
        
        try {
            com.zimbra.common.soap.Element requestElt = com.zimbra.common.soap.Element.convertDOM(auth);
            response = transport.invoke(requestElt);
        } catch (SoapFaultException e) {
            System.err.println("Authentication error: " + e.getMessage());
            System.exit(1);
        }
        
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
        
        // Assemble SOAP request
        Element request = null;
        Element subpathRoot = null;
        
        if (rootPath != null) {
            subpathRoot = processPath(null, rootPath);
        }

        for (int i = 0; i < paths.length; i++) {
            Element e = processPath(subpathRoot, paths[i]);
            if (request == null) {
                request = e;
                while (request.getParent() != null) {
                    request = request.getParent();
                }
            }
        }
        
        if (request == null) {
            System.err.println("No request element specified.");
            System.exit(1);
        }
        
        // Send request and print response
        // System.out.println("Request:\n" + DomUtil.toString(requestRoot, true) + "\n");
        
        try {
            com.zimbra.common.soap.Element requestElt = com.zimbra.common.soap.Element.convertDOM(request);
            response = transport.invoke(requestElt);
        } catch (SoapFaultException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        
        System.out.println(DomUtil.toString(response.toXML(), true));
    }

    /**
     * <p>Processes a path that's relative to the given root.  The path
     * is in an XPath-like format:</p>
     * 
     * <p><tt>element1/element2[/@attr][=value]</tt></p>
     * 
     * If a value is specified, it sets the text of the last element
     * or attribute in the path.
     * 
     * @param root <tt>Element</tt> that the path is relative to
     * @param path an XPath-like path of elements and attributes
     */
    private Element processPath(Element root, String path)
    throws Exception {
        String value = null;
        
        // Parse out value, if it's specified
        if (path.contains("=")) {
            String[] parts = path.split("=");
            path = parts[0];
            value = parts[1];
        }
        
        // Walk parts and implicitly create elements
        String[] parts = path.split("/");
        String part = null;
        for (int i = 0; i < parts.length; i++) {
            part = parts[i];
            if (root == null) {
                QName name = QName.get(part, mNamespace);
                root = DocumentHelper.createElement(name);
            } else if (!(part.startsWith("@"))) {
                root = root.addElement(part);
            }
        }
        
        // Set either element text or attribute value
        if (value != null && part != null) {
            if (part.startsWith("@")) {
                String attrName = part.substring(1);
                root.addAttribute(attrName, value);
            } else {
                root.setText(value);
            }
        }
        return root;
    }
    
    public static void main(String[] args) {
        // Parse command line
        GnuParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(sOptions, args);
        } catch (ParseException e) {
            usage(e.toString());
        }
        
        // Set options and run
        String type = cl.getOptionValue(OPT_TYPE).toLowerCase();
        if (!sTypeToNamespace.containsKey(type)) {
            usage("Invalid type: " + type);
        }
        String url = "http://localhost:7070/service/soap";
        if (type.equals("admin")) {
            url = "https://localhost:7071/service/admin/soap/";
        }
        SoapCommandUtil util = new SoapCommandUtil(
            url, type, cl.getOptionValue(OPT_USER), cl.getOptionValue(OPT_PASSWORD));
        String rootElementPath = cl.getOptionValue(OPT_ELEMENT);
        
        if (url.startsWith("https")) {
            // Set up SSL
            CliUtil.toolSetup();
        }
        
        try {
            util.run(rootElementPath, cl.getArgs());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private static void usage(String error) {
        if (error != null) {
            System.err.println(error + "\n");
        }
        HelpFormatter formatter = new HelpFormatter();
        String syntax = String.format("zmsoap -t <type> -u <user> -p <password> [-e] path1 [... pathN]",
            SoapCommandUtil.class.getSimpleName());
        formatter.printHelp(syntax, sOptions);
        System.exit(1);
    }
}
