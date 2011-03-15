/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.OutputFormat;
import org.dom4j.QName;
import org.dom4j.io.XMLWriter;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.soap.JaxbUtil;

/**
 * This class represents a utility to generate the top level WSDL files for
 * Zimbra SOAP interfaces
 * 
 * @author gren
 *
 */
public class WsdlGenerator {

    private    static final String ARG_OUTPUT_DIR = "-output.dir";
    private    static String outputDir = null;

    /**
     * Reads the command line arguments.
     * 
     * @param    args        the arguments
     */
    private static void readArguments(String[] args) {
        int    argPos = 0;

        if (args[argPos].equals(ARG_OUTPUT_DIR)) {
            outputDir = args[++argPos];
            argPos++;
        }
    }

    public static Document makeWsdlDocument(String tns, String serviceName,
            String soapAddress, Iterable<String> requests) {
        Namespace nsTns = new Namespace("tns", tns);
        Namespace nsZimbra = new Namespace("zimbra", ZimbraNamespace.ZIMBRA_STR);
        Namespace nsSoap = new Namespace("soap",
                "http://schemas.xmlsoap.org/wsdl/soap/");
        Namespace nsXsd = new Namespace(
                "xsd", "http://www.w3.org/2001/XMLSchema");
        Namespace nsWsdl = new Namespace("",
                "http://schemas.xmlsoap.org/wsdl/");

        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( "definitions" );
        root.add(nsTns);
        root.add(nsZimbra);
        root.add(nsSoap);
        root.add(nsXsd);
        root.add(nsWsdl);
        root.addAttribute("targetNamespace", tns);
        root.addAttribute("name", serviceName);
        Element typesElem = root.addElement(QName.get("types", nsWsdl));
        Element schemaElem = typesElem.addElement(
                QName.get("schema", nsXsd));
        Element importZimbraElem = schemaElem.addElement(
                QName.get("import", nsXsd));
        importZimbraElem.addAttribute("namespace", ZimbraNamespace.ZIMBRA_STR);
        importZimbraElem.addAttribute("schemaLocation", "zimbra.xsd");
        Element importTnsElem = schemaElem.addElement(
                QName.get("import", nsXsd));
        importTnsElem.addAttribute("namespace", tns);
        String xsdName = tns.substring(4) + ".xsd";
        importTnsElem.addAttribute("schemaLocation", xsdName);

        Element portTypeElem = DocumentHelper.createElement(
                QName.get("portType", nsWsdl));
        portTypeElem.addAttribute("name", serviceName);

        Element bindingElem = DocumentHelper.createElement(
                QName.get("binding", nsWsdl));
        bindingElem.addAttribute("name", serviceName + "PortBinding");
        bindingElem.addAttribute("type", "tns:" + serviceName);
        Element soapBindElem = bindingElem.addElement(
                QName.get("binding", nsSoap));
        soapBindElem.addAttribute("transport",
                "http://schemas.xmlsoap.org/soap/http");
        soapBindElem.addAttribute("style", "document");

        for (String requestName : requests ) {
            String rootName = requestName.substring(0, requestName.length() -7);
            String responseName = rootName + "Response";
            String reqMsgName = requestName.substring(0, 1).toLowerCase() +
                                requestName.substring(1);
            String respMsgName = responseName.substring(0, 1).toLowerCase() +
                                responseName.substring(1);

            Element msgElem = root.addElement(QName.get("message", nsWsdl));
            msgElem.addAttribute("name", reqMsgName);
            Element partElem = msgElem.addElement("part");
            partElem.addAttribute("name", "parameters");
            partElem.addAttribute("element", "tns:" + requestName);
            msgElem = root.addElement(QName.get("message", nsWsdl));
            msgElem.addAttribute("name", respMsgName);
            partElem = msgElem.addElement("part");
            partElem.addAttribute("name", "parameters");
            partElem.addAttribute("element", "tns:" + responseName);

            Element opElem = portTypeElem.addElement(
                    QName.get("operation", nsWsdl));
            opElem.addAttribute("name", reqMsgName);
            Element inElem = opElem.addElement(
                    QName.get("input", nsWsdl));
            inElem.addAttribute("message", "tns:" + reqMsgName);
            Element outElem = opElem.addElement(
                    QName.get("output", nsWsdl));
            outElem.addAttribute("message", "tns:" + respMsgName);

            Element boElem = bindingElem.addElement(
                    QName.get("operation", nsWsdl));
            boElem.addAttribute("name", reqMsgName);
            Element soapOpElem = boElem.addElement(
                    QName.get("operation", nsSoap));
            soapOpElem.addAttribute("soapAction", tns + "/" + rootName);
            soapOpElem.addAttribute("style", "document");
            Element boInElem = boElem.addElement(
                    QName.get("input", nsWsdl));
            Element inSoapBodyElem = boInElem.addElement(
                    QName.get("body", nsSoap));
            inSoapBodyElem.addAttribute("use", "literal");
            Element inSoapHdrElem = boInElem.addElement(
                    QName.get("header", nsSoap));
            inSoapHdrElem.addAttribute("message", "tns:soapHdrContext");
            inSoapHdrElem.addAttribute("part", "context");
            inSoapHdrElem.addAttribute("use", "literal");
            Element boOutElem = boElem.addElement(
                    QName.get("output", nsWsdl));
            Element outSoapBodyElem = boOutElem.addElement(
                    QName.get("body", nsSoap));
            outSoapBodyElem.addAttribute("use", "literal");
        }
        // For Header Context
        Element hdrCntxtMsgElem = root.addElement(QName.get("message", nsWsdl));
        hdrCntxtMsgElem.addAttribute("name", "soapHdrContext");
        Element partElem = hdrCntxtMsgElem.addElement("part");
        partElem.addAttribute("name", "context");
        partElem.addAttribute("element", "zimbra:context");
        root.add(portTypeElem);
        root.add(bindingElem);
        Element svcElem = root.addElement(
                QName.get("service", nsWsdl));
        svcElem.addAttribute("name", serviceName);
        Element svcPortElem = svcElem.addElement(
                QName.get("port", nsWsdl));
        svcPortElem.addAttribute("name", serviceName + "Port");
        svcPortElem.addAttribute("binding", "tns:" + serviceName + "PortBinding");
        Element svcPortAddrElem = svcPortElem.addElement(
                QName.get("address", nsSoap));
        svcPortAddrElem.addAttribute("location", soapAddress);
        return document;
    }

    public static void createWsdlFile(File wsdlFile, String tns,
            String serviceName, String soapAddress, Iterable<String> requests)
    throws IOException {
        Document wsdlDoc = makeWsdlDocument(
                tns, serviceName, soapAddress, requests);

        if (wsdlFile.exists())
            wsdlFile.delete();
        OutputStream xmlOut = new FileOutputStream(wsdlFile);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter( xmlOut, format );
        writer.write(wsdlDoc);
        writer.close();
    }

    /**
     * Main
     * 
     * @param args the utility arguments
     */
    public static void main(String[] args) throws Exception {
        readArguments(args);
        List<String> accountRequests = Lists.newArrayList();
        List<String> adminRequests = Lists.newArrayList();
        List<String> mailRequests = Lists.newArrayList();
        for (Class<?> currClass : JaxbUtil.getJaxbRequestAndResponseClasses()) {
            String className = currClass.getName();
            String requestName = currClass.getSimpleName();
            if (!requestName.endsWith("Request"))
                continue;
            if (className.startsWith("com.zimbra.soap.account.message")) {
                accountRequests.add(requestName);
            } else if (className.startsWith("com.zimbra.soap.admin.message")) {
                adminRequests.add(requestName);
            } else if (className.startsWith("com.zimbra.soap.mail.message")) {
                mailRequests.add(requestName);
            }
        }
        Collections.sort(accountRequests);
        Collections.sort(adminRequests);
        Collections.sort(mailRequests);
        File accountWsdlFile = new File(outputDir, "AccountService.wsdl");
        createWsdlFile(accountWsdlFile,
                AccountConstants.NAMESPACE_STR, "AccountService",
                "http://localhost:7070/service/soap", accountRequests);
        File adminWsdlFile = new File(outputDir, "AdminService.wsdl");
        createWsdlFile(adminWsdlFile,
                AdminConstants.NAMESPACE_STR, "AdminService",
                "https://localhost:7071/service/admin/soap", adminRequests);
        File mailWsdlFile = new File(outputDir, "MailService.wsdl");
        createWsdlFile(mailWsdlFile,
                MailConstants.NAMESPACE_STR, "MailService",
                "http://localhost:7070/service/soap", mailRequests);
    }
} // end WsdlGenerator class
