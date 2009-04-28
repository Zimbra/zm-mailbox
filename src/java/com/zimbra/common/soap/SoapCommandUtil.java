/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.cli2.validation.EnumValidator;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element.ElementFactory;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;

public class SoapCommandUtil implements SoapTransport.DebugListener {

    private static final Map<String, Namespace> sTypeToNamespace =
        new HashMap<String, Namespace>();

    private static final String DEFAULT_ADMIN_URL = String.format("https://%s:%d/service/admin/soap",
        LC.zimbra_zmprov_default_soap_server.value(),
        LC.zimbra_admin_service_port.intValue());
    private static final String DEFAULT_URL = "http://" + LC.zimbra_zmprov_default_soap_server.value() + "/service/soap";

    private static final String TYPE_MAIL = "mail";
    private static final String TYPE_ADMIN = "admin";
    private static final String TYPE_ACCOUNT = "account";
    private static final String TYPE_IM = "im";
    private static final String TYPE_MOBILE = "mobile";
    
    static {
        // Namespaces
        sTypeToNamespace.put(TYPE_MAIL, Namespace.get("urn:zimbraMail"));
        sTypeToNamespace.put(TYPE_ADMIN, Namespace.get("urn:zimbraAdmin"));
        sTypeToNamespace.put(TYPE_ACCOUNT, Namespace.get("urn:zimbraAccount"));
        sTypeToNamespace.put(TYPE_IM, Namespace.get("urn:zimbraIM"));
        sTypeToNamespace.put(TYPE_MOBILE, Namespace.get("urn:zimbraSync"));
    }
    
    private Group mOptions;
    private String mUrl;
    private String mType;
    private Namespace mNamespace;
    private String mMailboxName;
    private String mAdminAccountName;
    private String mTargetAccountName;
    private String mPassword;
    private List<String> mPaths;
    private String mAuthToken;
    private int mVerbose = 0;
    private boolean mUseSession = false;
    private boolean mNoOp = false;
    private String mSelect;
    
    @SuppressWarnings("unchecked")
    private void parseCommandLine(String[] args) {
        
        DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
        ArgumentBuilder abuilder = new ArgumentBuilder();
        Argument name = abuilder.withName("name").withMinimum(1).withMaximum(1).create();
        Argument noArgs = abuilder.withMinimum(0).withMaximum(0).create();
        
        // Initialize options
        Option help = obuilder 
            .withLongName("help").withShortName("h").withDescription("Print usage information.").create();
        Option mailbox = obuilder
            .withLongName("mailbox").withShortName("m")
            .withDescription("Mailbox account name.  mail and account requests are sent to this account.  " +
                "Also used for authentication if -a and -z are not specified.")
            .withArgument(name).create();
        Option target = obuilder
            .withLongName("target").withArgument(name)
            .withDescription("Target account name to which requests will be sent.  Only used for non-admin sessions.").create();
        Option admin = obuilder
            .withLongName("admin").withShortName("a").withDescription("Admin account name to authenticate as.")
            .withArgument(name).create();
        Option password = obuilder
            .withLongName("password").withShortName("p").withDescription("Password.")
            .withArgument(abuilder.withMinimum(1).withMaximum(1).withName("pass").create()).create();
        Option passFile = obuilder
            .withLongName("passfile").withShortName("P").withDescription("Read password from file.")
            .withArgument(abuilder.withName("path").withMinimum(1).withMaximum(1).create()).create();
        Option url = obuilder
            .withLongName("url").withShortName("u").withDescription("Server hostname and optional port.")
            .withArgument(abuilder.withName("http[s]://...").withMinimum(1).withMaximum(1).create()).create();
        Option zadmin = obuilder
            .withLongName("zadmin").withShortName("z").withArgument(noArgs)
            .withDescription("Authenticate with zimbra admin name/password from localconfig.").create();
        Option verbose = obuilder
            .withLongName("verbose").withShortName("v").withArgument(noArgs)
            .withDescription("Print the SOAP request and other status information. Specify twice for fully verbose output").create();
        Option noOp = obuilder
            .withLongName("no-op").withShortName("n").withArgument(noArgs)
            .withDescription("Print the SOAP request only.  Don't send it.").create();
        Option select = obuilder
            .withLongName("select").withDescription("Select element(s) or an attribute from the response.")
            .withArgument(abuilder.withMinimum(1).withMaximum(1).withName("path").create()).create();
        Option paths = abuilder.withName("path").withMinimum(1)
            .withDescription("Element or attribute path and value.  Roughly follows XPath syntax: " +
                "[/]element1[/element2][/..][/@attr][=value].").create();
        
        // Types option
        Set<String> validTypes = new HashSet<String>();
        validTypes.add("mail"); validTypes.add("account"); validTypes.add("admin"); validTypes.add("im"); validTypes.add("mobile");
        Argument typeArg = abuilder
            .withName("type").withValidator(new EnumValidator(validTypes))
            .withMinimum(1).withMaximum(1).create();
        Option type = obuilder
            .withLongName("type").withShortName("t")
            .withArgument(typeArg)
            .withDescription("SOAP request type (mail, account, admin, im, mobile).  Default is admin.")
            .create();
        
        // Initialize option group
        GroupBuilder gbuilder = new GroupBuilder();
        mOptions = gbuilder
            .withName("options")
            .withOption(help)
            .withOption(mailbox)
            .withOption(target)
            .withOption(admin)
            .withOption(zadmin)
            .withOption(password)
            .withOption(passFile)
            .withOption(type)
            .withOption(url)
            .withOption(verbose)
            .withOption(noOp)
            .withOption(paths)
            .withOption(select)
            .create();

        // Parse command line
        Parser parser = new Parser();
        parser.setGroup(mOptions);
        parser.setHelpOption(help);
        CommandLine cl = null;
        
        try {
            cl = parser.parse(args);
        } catch (OptionException e) {
            usage(null);
        }
        if (cl == null) {
            usage(null);
        }
        
        // Set member variables
        if (cl.hasOption(passFile)) {
            String path = (String) cl.getValue(passFile);
            try {
                mPassword = StringUtil.readSingleLineFromFile(path);
            } catch (IOException e) {
                usage("Cannot read password from file: " + e.getMessage());
            }
        }
        if (cl.hasOption(password)) {
            mPassword = (String) cl.getValue(password);
        }
        mMailboxName = (String) cl.getValue(mailbox);
        mAdminAccountName = (String) cl.getValue(admin);

        if (!cl.hasOption(admin) && cl.hasOption(zadmin)) {
            mAdminAccountName = LC.zimbra_ldap_user.value();
            if (!cl.hasOption(password)) {
                mPassword = LC.zimbra_ldap_password.value();
            }
        }
        mMailboxName = (String) cl.getValue(mailbox);
        
        if (mMailboxName == null && mAdminAccountName == null) {
            usage("Authentication account not specified.");
        }
        
        if (cl.hasOption(type)) {
            mType = (String) cl.getValue(type);
            if ((mType.equals(TYPE_MAIL) || mType.equals(TYPE_ACCOUNT) || mType.equals(TYPE_IM)) && mMailboxName == null) {
                usage("Mailbox must be specified for mail or account requests.");
            }
        } else {
            if (mMailboxName != null) {
                mType = TYPE_MAIL;
            } else {
                mType = "admin";
            }
        }
        mNamespace = sTypeToNamespace.get(mType);
        
        if (cl.hasOption(url)) {
            mUrl = (String) cl.getValue(url);
        } else {
            if (mAdminAccountName != null) {
                mUrl = DEFAULT_ADMIN_URL;  
            } else {
                mUrl = DEFAULT_URL;
            }
        }

        if (cl.hasOption(target)) {
            if (mAdminAccountName != null) {
                usage("--target option cannot be used with admin authentication.");
            }
            mTargetAccountName = (String) cl.getValue(target);
        }
        
        mPaths = cl.getValues(paths);
        mVerbose = cl.getOptionCount(verbose);
        mNoOp = cl.hasOption(noOp);
        mSelect = (String) cl.getValue(select);
    }
    
    public void sendSoapMessage(com.zimbra.common.soap.Element envelope) {
        if (mVerbose > 1) {
            System.out.println(DomUtil.toString(envelope.toXML(), true));
        }
    }
    
    public void receiveSoapMessage(com.zimbra.common.soap.Element envelope) {
        if (mVerbose > 1) {
            System.out.println(DomUtil.toString(envelope.toXML(), true));
        }
    }
    
    private void usage(String errorMsg) {
        if (errorMsg != null) {
            System.err.format("%s\n\n", errorMsg);
        }
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setGroup(mOptions);
        helpFormatter.setShellCommand("zmsoap");
        helpFormatter.getFullUsageSettings().remove(DisplaySetting.DISPLAY_GROUP_EXPANDED);
        try {
            helpFormatter.print();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        System.exit(1);
    }
    
    private void adminAuth()
    throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(mUrl);
        transport.setDebugListener(this);
        
        // Create auth element
        Element auth = DocumentHelper.createElement(AdminConstants.AUTH_REQUEST);
        auth.addElement(AdminConstants.E_NAME).setText(mAdminAccountName);
        auth.addElement(AdminConstants.E_PASSWORD).setText(mPassword);
        
        // Authenticate and get auth token
        com.zimbra.common.soap.Element response = null;
        com.zimbra.common.soap.Element request = null;
        
        if (mVerbose > 0) {
            System.out.println("Sending admin auth request to " + mUrl);
        }
        
        try {
            request = com.zimbra.common.soap.Element.convertDOM(auth);
            response = transport.invoke(request, false, !mUseSession, null);
        } catch (SoapFaultException e) {
            System.err.format("Authentication error: %s\n", e.getMessage());
            System.exit(1);
        } catch (ConnectException e) {
            System.err.format("Unable to connect to %s: %s\n", mUrl, e.getMessage());
        }
        mAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(mAuthToken);
        
        // Do delegate auth if this is a mail or account service request
        if (mType.equals(TYPE_MAIL) || mType.equals(TYPE_ACCOUNT) || mType.equals(TYPE_IM)) {
            Element getInfo = DocumentHelper.createElement(AdminConstants.GET_ACCOUNT_INFO_REQUEST);
            Element account = DomUtil.add(getInfo, AccountConstants.E_ACCOUNT, mMailboxName);
            account.addAttribute(AdminConstants.A_BY, AdminConstants.BY_NAME);
            try {
                request = com.zimbra.common.soap.Element.convertDOM(getInfo);
                response = transport.invoke(request, false, !mUseSession, null);
            } catch (SoapFaultException e) {
                System.err.format("Cannot access account: %s\n", e.getMessage());
                System.exit(1);
            }
            mUrl = response.getElement(AdminConstants.E_SOAP_URL).getText();
            
            // Get delegate auth token
            Element delegateAuth = DocumentHelper.createElement(AdminConstants.DELEGATE_AUTH_REQUEST);
            account = DomUtil.add(delegateAuth, AccountConstants.E_ACCOUNT, mMailboxName);
            account.addAttribute(AdminConstants.A_BY, AdminConstants.BY_NAME);
            try {
                request = com.zimbra.common.soap.Element.convertDOM(delegateAuth);
                response = transport.invoke(request, false, !mUseSession, null);
            } catch (SoapFaultException e) {
                System.err.format("Cannot do delegate auth: %s\n", e.getMessage());
            }
            mAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        }
    }
    
    private void mailboxAuth()
    throws Exception {
        if (mVerbose > 0) {
            System.out.println("Sending auth request to " + mUrl);
        }
        
        SoapHttpTransport transport = new SoapHttpTransport(mUrl);
        transport.setDebugListener(this);
        
        // Create auth element
        Element auth = DocumentHelper.createElement(AccountConstants.AUTH_REQUEST);
        Element account = DomUtil.add(auth, AccountConstants.E_ACCOUNT, mMailboxName);
        account.addAttribute(AdminConstants.A_BY, AdminConstants.BY_NAME);
        auth.addElement(AccountConstants.E_PASSWORD).setText(mPassword);
        
        // Authenticate and get auth token
        com.zimbra.common.soap.Element response = null;
        
        try {
            com.zimbra.common.soap.Element requestElt = com.zimbra.common.soap.Element.convertDOM(auth);
            response = transport.invoke(requestElt, false, !mUseSession, null);
        } catch (SoapFaultException e) {
            System.err.println("Authentication error: " + e.getMessage());
            System.exit(1);
        } catch (ConnectException e) {
            System.err.format("Unable to connect to %s: %s\n", mUrl, e.getMessage());
            System.exit(1);
        }
        mAuthToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
    }
    
    private void run()
    throws Exception {
        // Assemble SOAP request.
        Element element = null;
        
        for (int i = 0; i < mPaths.size(); i++) {
            element = processPath(element, mPaths.get(i));
        }
        
        // Find the root.
        Element request = element;
        if (request == null) {
            System.err.println("No request element specified.");
            System.exit(1);
        }
        while (request.getParent() != null) {
            request = request.getParent();
        }
        
        if (mVerbose == 1 || mNoOp) {
            System.out.println(DomUtil.toString(request, true));
        }
        if (mNoOp) {
            return;
        }

        // Authenticate
        if (mAdminAccountName != null) {
            adminAuth();
        } else {
            mailboxAuth();
        }
        
        // Send request and print response.
        SoapHttpTransport transport = new SoapHttpTransport(mUrl);
        transport.setDebugListener(this);
        
        transport.setAuthToken(mAuthToken);
        if (!mType.equals(TYPE_ADMIN) && mTargetAccountName != null) {
            transport.setTargetAcctName(mTargetAccountName);
        }
        com.zimbra.common.soap.Element response = null;
        try {
            com.zimbra.common.soap.Element requestElt = com.zimbra.common.soap.Element.convertDOM(request);
            response = transport.invoke(requestElt, false, !mUseSession, null);
        } catch (SoapFaultException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Select result.
        List<com.zimbra.common.soap.Element> results = null;
        String resultString = null;
        
        if (mSelect != null) {
            // Create bogus root element, to allow us to find the first element in the path. 
            com.zimbra.common.soap.Element root = response.getFactory().createElement("root");
            response.detach();
            root.addElement(response);
            
            String[] parts = mSelect.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                if (lastPart.startsWith("@")) {
                    parts[parts.length - 1] = lastPart.substring(1);
                    resultString = root.getPathAttribute(parts);
                } else {
                    results = root.getPathElementList(parts);
                }
            }
        } else {
            results = new ArrayList<com.zimbra.common.soap.Element>();
            results.add(response);
        }
        
        if (mVerbose <= 1) {
            if (resultString == null && results != null) {
                StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (com.zimbra.common.soap.Element e : results) {
                    if (first) {
                        first = false; 
                    } else {
                        buf.append('\n');
                    }
                    buf.append(DomUtil.toString(e.toXML(), true));
                }
                resultString = buf.toString();
            }
            if (resultString == null) {
                resultString = "";
            }
            System.out.println(resultString);
        }
    }

    /**
     * Processes a path that's relative to the given root.  The path
     * is in an XPath-like format:
     * 
     * <p><tt>element1/element2[/@attr][=value]</tt></p>
     * 
     * If a value is specified, it sets the text of the last element
     * or attribute in the path.
     * 
     * @param start <tt>Element</tt> that the path is relative to, or <tt>null</tt>
     * for the root
     * @param path an XPath-like path of elements and attributes
     */
    private Element processPath(Element start, String path)
    throws Exception {
        String value = null;
        
        // Parse out value, if it's specified.
        if (path.contains("=")) {
            String[] parts = path.split("=");
            path = parts[0];
            value = parts[1];
        }
        
        // Find the first element.
        Element element = start;
        
        // Walk parts and implicitly create elements.
        String[] parts = path.split("/");
        String part = null;
        for (int i = 0; i < parts.length; i++) {
            part = parts[i];
            if (element == null) {
                QName name = QName.get(part, mNamespace);
                element = DocumentHelper.createElement(name);
            } else if (part.equals("..")) {
                element = element.getParent();
            } else if (!(part.startsWith("@"))) {
                element = element.addElement(part);
            }
        }
        
        // Set either element text or attribute value
        if (value != null && part != null) {
            if (part.startsWith("@")) {
                String attrName = part.substring(1);
                element.addAttribute(attrName, value);
            } else {
                element.setText(value);
            }
        }
        return element;
    }
    
    public static void main(String[] args) {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmsoap", null);
        SoapCommandUtil app = new SoapCommandUtil();
        app.parseCommandLine(args);
        
        try {
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
