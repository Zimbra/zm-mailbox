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
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.OutputFormat;
import org.dom4j.QName;
import org.dom4j.io.XMLWriter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.common.soap.AppBlastConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ReplicationConstants;
import com.zimbra.common.soap.SyncConstants;
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

    public static Document makeWsdlDocument(String svcNsString, String xsdNsString,
            String serviceName, String soapAddress, Iterable<String> requests) {
        String xsdPrefix = xsdNsString.replaceFirst("urn:", "");
        final String svcPrefix = "svc";
        Namespace nsTns = new Namespace(xsdPrefix, xsdNsString);
        Namespace nsSvc = new Namespace(svcPrefix, svcNsString);
        Namespace nsZimbra = new Namespace("zimbra", ZimbraNamespace.ZIMBRA_STR);
        Namespace nsSoap = new Namespace("soap",
                "http://schemas.xmlsoap.org/wsdl/soap/");
        Namespace nsXsd = new Namespace(
                "xsd", "http://www.w3.org/2001/XMLSchema");
        Namespace nsWsdl = new Namespace("wsdl",
                "http://schemas.xmlsoap.org/wsdl/");
        final QName svcTypes = QName.get("types", nsSvc);
        final QName xsdSchema = QName.get("schema", nsXsd);
        final QName xsdImport = QName.get("import", nsXsd);
        final QName soapBinding = QName.get("binding", nsSoap);
        final QName wsdlMessage = QName.get("message", nsWsdl);
        final QName wsdlBinding = QName.get("binding", nsWsdl);
        final QName wsdlOperation = QName.get("operation", nsWsdl);
        final QName soapOperation = QName.get("operation", nsSoap);
        final QName portType = QName.get("portType", nsWsdl);
        final QName part = QName.get("part", nsWsdl);
        final QName input = QName.get("input", nsWsdl);
        final QName output = QName.get("output", nsWsdl);
        final QName body = QName.get("body", nsSoap);
        final QName header = QName.get("header", nsSoap);
        final QName service = QName.get("service", nsWsdl);
        final QName port = QName.get("port", nsWsdl);
        final QName address = QName.get("address", nsSoap);
        String xsdName = xsdNsString.substring(4) + ".xsd";

        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(QName.get("definitions", nsWsdl));
        root.add(nsSvc);
        root.add(nsTns);
        root.add(nsZimbra);
        root.add(nsSoap);
        root.add(nsXsd);
        root.add(nsWsdl);
        root.addAttribute("targetNamespace", svcNsString);
        root.addAttribute("name", serviceName);
        // wsdl:definitions/svc:types
        Element typesElem = root.addElement(svcTypes);
        // wsdl:definitions/svc:types/xsd:schema
        Element schemaElem = typesElem.addElement(xsdSchema);
        // wsdl:definitions/svc:types/xsd:schema/xsd:import
        Element importZimbraElem = schemaElem.addElement(xsdImport);
        importZimbraElem.addAttribute("namespace", ZimbraNamespace.ZIMBRA_STR);
        importZimbraElem.addAttribute("schemaLocation", "zimbra.xsd");

        Element importTnsElem = schemaElem.addElement(xsdImport);
        importTnsElem.addAttribute("namespace", xsdNsString);
        importTnsElem.addAttribute("schemaLocation", xsdName);

        // wsdl:definitions/wsdl:portType
        Element portTypeElem = DocumentHelper.createElement(portType);
        portTypeElem.addAttribute("name", serviceName);

        // wsdl:definitions/wsdl:binding
        Element bindingElem = DocumentHelper.createElement(wsdlBinding);
        bindingElem.addAttribute("name", serviceName + "PortBinding");
        bindingElem.addAttribute("type", svcPrefix + ":" + serviceName);
        // wsdl:definitions/wsdl:binding/soap:binding
        Element soapBindElem = bindingElem.addElement(soapBinding);
        soapBindElem.addAttribute("transport",
                "http://schemas.xmlsoap.org/soap/http");
        soapBindElem.addAttribute("style", "document");

        for (String requestName : requests ) {
            String rootName = requestName.substring(0, requestName.length() -7);
            String responseName = rootName + "Response";
            String reqOpName = requestName.substring(0, 1).toLowerCase() +
                                requestName.substring(1);
            String respOpName = responseName.substring(0, 1).toLowerCase() +
                                responseName.substring(1);
            String reqMsgName = reqOpName + "Message";
            String respMsgName = respOpName + "Message";

            // wsdl:definitions/wsdl:message - for request
            Element msgElem = root.addElement(wsdlMessage);
            msgElem.addAttribute("name", reqMsgName);
            // wsdl:definitions/wsdl:message/wsdl:part
            Element partElem = msgElem.addElement(part);
            partElem.addAttribute("name", "parameters");
            partElem.addAttribute("element", xsdPrefix + ":" + requestName);
            // wsdl:definitions/wsdl:message - for response
            msgElem = root.addElement(wsdlMessage);
            msgElem.addAttribute("name", respMsgName);
            // wsdl:definitions/wsdl:message/wsdl:part
            partElem = msgElem.addElement(part);
            partElem.addAttribute("name", "parameters");
            partElem.addAttribute("element", xsdPrefix + ":" + responseName);

            // wsdl:definitions/wsdl:portType/wsdl:operation
            Element opElem = portTypeElem.addElement(wsdlOperation);
            opElem.addAttribute("name", reqOpName);
            // wsdl:definitions/wsdl:portType/wsdl:operation/wsdl:input
            Element inElem = opElem.addElement(input);
            inElem.addAttribute("message", svcPrefix + ":" + reqMsgName);
            // wsdl:definitions/wsdl:portType/wsdl:operation/wsdl:output
            Element outElem = opElem.addElement(output);
            outElem.addAttribute("message", svcPrefix + ":" + respMsgName);

            // wsdl:definitions/wsdl:binding/wsdl:operation
            Element boElem = bindingElem.addElement(wsdlOperation);
            boElem.addAttribute("name", reqOpName);
            // wsdl:definitions/wsdl:binding/wsdl:operation/soap:operation
            Element soapOpElem = boElem.addElement(soapOperation);
            soapOpElem.addAttribute("soapAction", xsdNsString + "/" + rootName);
            soapOpElem.addAttribute("style", "document");
            // wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:input
            Element boInElem = boElem.addElement(input);
            // wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:input/soap:body
            Element inSoapBodyElem = boInElem.addElement(body);
            inSoapBodyElem.addAttribute("use", "literal");
            // wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:input/soap:header
            Element inSoapHdrElem = boInElem.addElement(header);
            inSoapHdrElem.addAttribute("message", svcPrefix + ":soapHdrContext");
            inSoapHdrElem.addAttribute("part", "context");
            inSoapHdrElem.addAttribute("use", "literal");
            // wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:output
            Element boOutElem = boElem.addElement(output);
            // wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:output/soap:body
            Element outSoapBodyElem = boOutElem.addElement(body);
            outSoapBodyElem.addAttribute("use", "literal");
        }
        // For Header Context
        // wsdl:definitions/wsdl:message
        Element hdrCntxtMsgElem = root.addElement(wsdlMessage);
        hdrCntxtMsgElem.addAttribute("name", "soapHdrContext");
        // wsdl:definitions/wsdl:message/wsdl:part
        Element partElem = hdrCntxtMsgElem.addElement(part);
        partElem.addAttribute("name", "context");
        partElem.addAttribute("element", "zimbra:context");

        root.add(portTypeElem);
        root.add(bindingElem);

        // wsdl:definitions/wsdl:service
        Element svcElem = root.addElement(service);
        svcElem.addAttribute("name", serviceName);
        // wsdl:definitions/wsdl:service/wsdl:port
        Element svcPortElem = svcElem.addElement(port);
        svcPortElem.addAttribute("name", serviceName + "Port");
        svcPortElem.addAttribute("binding", svcPrefix + ":" + serviceName + "PortBinding");
        // wsdl:definitions/wsdl:service/wsdl:port/soap:address
        Element svcPortAddrElem = svcPortElem.addElement(address);
        svcPortAddrElem.addAttribute("location", soapAddress);
        return document;
    }

    public static void createWsdlFile(File wsdlFile, String xsdNs,
            String serviceName, String soapAddress, Iterable<String> requests)
    throws IOException {
        String wsdlNs = "http://www.zimbra.com/wsdl/" + wsdlFile.getName();
        Document wsdlDoc = makeWsdlDocument(
                wsdlNs, xsdNs, serviceName, soapAddress, requests);

        if (wsdlFile.exists())
            wsdlFile.delete();
        OutputStream xmlOut = new FileOutputStream(wsdlFile);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter( xmlOut, format );
        writer.write(wsdlDoc);
        writer.close();
    }

    public static void createWsdl(String wsdlFileName, String xsdNs,
            String serviceName, String soapAddress, Iterable<String> requests)
    throws IOException {
        File wsdlFile = new File(outputDir, wsdlFileName);
        createWsdlFile(wsdlFile, xsdNs, serviceName, soapAddress, requests);
    }

    private static Map<String,List<String>> getRequestLists() {
        Map<String,List<String>> requestLists = Maps.newHashMap();
        for (Class<?> currClass : JaxbUtil.getJaxbRequestAndResponseClasses()) {
            String requestName = currClass.getSimpleName();
            if (!requestName.endsWith("Request"))
                continue;
            String pkgName = currClass.getPackage().getName();
            List<String> reqList;
            if (requestLists.containsKey(pkgName)) {
                reqList = requestLists.get(pkgName);
            } else {
                reqList = Lists.newArrayList();
                requestLists.put(pkgName, reqList);
            }
            reqList.add(requestName);
        }
        for (String key :requestLists.keySet()) {
            Collections.sort(requestLists.get(key));
        }
        return requestLists;
    }

    /**
     * Main
     * 
     * @param args the utility arguments
     */
    public static void main(String[] args) throws Exception {
        readArguments(args);
        Map<String,List<String>> requestLists = getRequestLists();
        createWsdl("AccountService.wsdl",
                AccountConstants.NAMESPACE_STR, "AccountService",
                "http://localhost:7070/service/soap",
                requestLists.get("com.zimbra.soap.account.message"));
        createWsdl("AdminService.wsdl",
                AdminConstants.NAMESPACE_STR, "AdminService",
                "https://localhost:7071/service/admin/soap",
                requestLists.get("com.zimbra.soap.admin.message"));
        createWsdl("AdminExtService.wsdl",
                AdminExtConstants.NAMESPACE_STR, "AdminExtService",
                "https://localhost:7071/service/admin/soap",
                requestLists.get("com.zimbra.soap.adminext.message"));
        createWsdl("MailService.wsdl",
                MailConstants.NAMESPACE_STR, "MailService",
                "http://localhost:7070/service/soap",
                requestLists.get("com.zimbra.soap.mail.message"));
        createWsdl("ReplicationService.wsdl",
                ReplicationConstants.NAMESPACE_STR, "ReplicationService",
                "http://localhost:7070/service/soap",
                requestLists.get("com.zimbra.soap.replication.message"));
        createWsdl("SyncService.wsdl",
                SyncConstants.NAMESPACE_STR, "SyncService",
                "http://localhost:7070/service/soap",
                requestLists.get("com.zimbra.soap.sync.message"));
        createWsdl("AppblastService.wsdl",
                AppBlastConstants.NAMESPACE_STR, "AppblastService",
                "http://localhost:7070/service/soap",
                requestLists.get("com.zimbra.soap.appblast.message"));
    }
} // end WsdlGenerator class
