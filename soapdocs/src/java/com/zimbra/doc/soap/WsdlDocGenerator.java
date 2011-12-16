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

package com.zimbra.doc.soap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.common.soap.AppBlastConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ReplicationConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.doc.soap.doclet.DocletApiListener;
import com.zimbra.doc.soap.doclet.ZmApiDoclet;
import com.zimbra.doc.soap.template.ApiReferenceTemplateHandler;
import com.zimbra.doc.soap.template.TemplateHandler;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.util.JaxbInfo;
import javax.xml.bind.annotation.XmlSchema;

/**
 * This class represents a utility to generate all documentation for the Zimbra SOAP API.
 *
 */
public class WsdlDocGenerator {
    private static final String ARG_JAXB_SRC_DIR  = "-jaxb.src.dir";
    private static final String ARG_TEMPLATES_DIR = "-templates.dir";
    private static final String ARG_OUTPUT_DIR    = "-output.dir";
    private static final String ARG_BUILD_VERSION = "-build.version";
    private static final String ARG_BUILD_DATE    = "-build.date";

    private static String jaxbSrcDir = null;
    private static String templatesDir = null;
    private static String outputDir = null;
    private static String buildVersion = null;
    private static String buildDate = null;

    private static final Map<String,String> serviceDescriptions;
    static {
        serviceDescriptions = Maps.newHashMap();
        serviceDescriptions.put(AccountConstants.NAMESPACE_STR, "The Account Service includes commands for retrieving, storing and managing user account information.");
        serviceDescriptions.put(AdminConstants.NAMESPACE_STR, "The Admin Service includes commands for administering Zimbra.");
        serviceDescriptions.put(AdminExtConstants.NAMESPACE_STR, "The Admin Extension Service includes additional commands for administering Zimbra.");
        serviceDescriptions.put(AppBlastConstants.NAMESPACE_STR, "The AppBlast Service includes commands related to application delivery.");
        serviceDescriptions.put(MailConstants.NAMESPACE_STR, "The Mail Service includes commands for managing mail and calendar information.");
        serviceDescriptions.put(ReplicationConstants.NAMESPACE_STR, "The zimbraRepl Service includes commands for managing Zimbra Server replication.");
        serviceDescriptions.put(SyncConstants.NAMESPACE_STR, "The zimbraSync Service includes commands for managing devices using Synchronization.");
    }

    /**
     * Reads the command line arguments.
     *
     * @param args the arguments
     */
    private static void readArguments(String[] args) {
        int    argPos = 0;

        if (args[argPos].equals(ARG_JAXB_SRC_DIR)) {
            jaxbSrcDir = args[++argPos];
            argPos++;
        }

        if (args[argPos].equals(ARG_TEMPLATES_DIR)) {
            templatesDir = args[++argPos];
            argPos++;
        }

        if (args[argPos].equals(ARG_OUTPUT_DIR)) {
            outputDir = args[++argPos];
            argPos++;
        }

        if (args[argPos].equals(ARG_BUILD_VERSION)) {
            buildVersion = args[++argPos];
            argPos++;
        }

        if (args[argPos].equals(ARG_BUILD_DATE)) {
            buildDate = args[++argPos];
            argPos++;
        }
    }

    public static String buildSourcePath(String className) {
        StringBuilder buf = new StringBuilder();
        
        String classFilePath = className.replaceAll("\\.", "/");
        
        buf.append(jaxbSrcDir);
        buf.append("/"); // it's OK to use "/" since that's what javadoc is expecting in src files list
        buf.append(classFilePath);
        buf.append(".java");

        return  buf.toString();
    }

    /**
     * @return map between class name and associated documentation
     */
    private static Map<String,ApiClassDocumentation> getJavaDocInfoForClasses(Collection<String> pkgs) {
        DocletApiListener listener = new DocletApiListener();
        ZmApiDoclet.setListener(listener);
        
        List<String> argList = Lists.newArrayList();
        argList.add("-doclet");
        argList.add(ZmApiDoclet.class.getName());
        argList.add("-sourcepath");
        argList.add(jaxbSrcDir);
        argList.add("-private");  // needed to get private field annotations
        argList.addAll(pkgs);
        // argList.add("com.zimbra.soap.account.message");
        // argList.add("com.zimbra.soap.account.type");
        com.sun.tools.javadoc.Main.execute(argList.toArray(new String[0]));
        return listener.getDocMap();
    }

    private static Collection<String> getJaxbPackages(Class<?>[] jaxbClasses) {
        Set<String> pkgs = Sets.newHashSet();
        pkgs.add("com.zimbra.soap.base");
        pkgs.add("com.zimbra.soap.header");
        pkgs.add("com.zimbra.soap.type");
        for (Class<?> jaxbClass : jaxbClasses) {
            String msgPkgName = jaxbClass.getPackage().getName();
            String typePkgName = msgPkgName.replace(".message", ".type");
            pkgs.add(msgPkgName);
            pkgs.add(typePkgName);
        }
        return pkgs;
    }

    private static String getNamespace(Class<?> jaxbClass, Map<Package,String> pkgToNamespace) {
        Package pkg = jaxbClass.getPackage();
        String namespace = pkgToNamespace.get(pkg);
        if (namespace == null) {
            XmlSchema schemaAnnot = pkg.getAnnotation(XmlSchema.class);
            if (schemaAnnot != null) {
                namespace = schemaAnnot.namespace();
                // XmlNs[] xmlns = schemaAnnot.xmlns();  Useful if we need prefix for namespace
                pkgToNamespace.put(pkg, namespace);
            }
        }
        return namespace;
    }

    private static void populateWithJavadocInfo(DescriptionNode node,
            Map<String,ApiClassDocumentation> javadocInfo) {
        if (node instanceof XmlElementDescription) {
            XmlElementDescription elem = (XmlElementDescription) node;
            Class<?> jaxbClass = elem.getJaxbClass();
            ApiClassDocumentation doc = (jaxbClass != null) ? javadocInfo.get(jaxbClass.getName()) : null;
            if (doc != null) {
                String classDesc = doc.getClassDescription();
                if (!Strings.isNullOrEmpty(classDesc)) {
                    elem.setDescription(classDesc);
                }
                // Field Tags
                for (Entry<String, String> entry : doc.getFieldTag().entrySet()) {
                    for (XmlAttributeDescription attr : elem.getAttribs()) {
                        if (entry.getKey().equals(attr.getFieldName())) {
                            attr.setFieldTag(entry.getValue());
                        }
                    }
                    for (DescriptionNode childNode : elem.getChildren()) {
                        if (childNode instanceof XmlElementDescription) {
                            XmlElementDescription childElem = (XmlElementDescription) childNode;
                            if (entry.getKey().equals(childElem.getFieldName())) {
                                childElem.setFieldTag(entry.getValue());
                            }
                        }
                    }
                }
                // Field Descriptions
                for (Entry<String, String> entry : doc.getFieldDescription().entrySet()) {
                    for (XmlAttributeDescription attr : elem.getAttribs()) {
                        if (entry.getKey().equals(attr.getFieldName())) {
                            attr.setDescription(entry.getValue());
                        }
                    }
                    for (DescriptionNode childNode : elem.getChildren()) {
                        if (childNode instanceof XmlElementDescription) {
                            XmlElementDescription childElem = (XmlElementDescription) childNode;
                            if (entry.getKey().equals(childElem.getFieldName())) {
                                childElem.setDescription(entry.getValue());
                            }
                        }
                    }
                }
            }
        }
        for (DescriptionNode childNode : node.getChildren()) {
            populateWithJavadocInfo(childNode, javadocInfo);
        }
    }

    private static void populateWithJavadocInfo(Root root) {
        Class<?>[] jaxbClasses = JaxbUtil.getJaxbRequestAndResponseClasses();
        // map between class name and associated documentation
        Map<String,ApiClassDocumentation> javadocInfo = getJavaDocInfoForClasses(getJaxbPackages(jaxbClasses));

        for (Command cmd : root.getAllCommands()) {
            String reqClass = cmd.getRequest().getJaxbClass().getName();
            ApiClassDocumentation doc = javadocInfo.get(reqClass);
            if ((doc != null) && (!Strings.isNullOrEmpty(doc.getCommandDescription()))) {
                cmd.setDescription(doc.getCommandDescription());
            } else {
                String respClass = cmd.getResponse().getJaxbClass().getName();
                doc = javadocInfo.get(respClass);
                if ((doc != null) && (!Strings.isNullOrEmpty(doc.getCommandDescription()))) {
                    cmd.setDescription(doc.getCommandDescription());
                }
            }
            populateWithJavadocInfo(cmd.getRequest(), javadocInfo);
            populateWithJavadocInfo(cmd.getResponse(), javadocInfo);
        }
    }

    private static Root processJaxbClasses() {
        Class<?>[] jaxbClasses = JaxbUtil.getJaxbRequestAndResponseClasses();
        Map<Package,String> pkgToNamespace = Maps.newHashMap();
        Root root = new Root();
        for (Class<?> jaxbClass : jaxbClasses) {
            JaxbInfo jaxbInfo = JaxbInfo.getFromCache(jaxbClass);
            String namespace = getNamespace(jaxbClass, pkgToNamespace);
            Service svc = root.getServiceForNamespace(namespace);
            if (svc == null) {
                svc = root.addService(new Service(namespace.replaceFirst("urn:", ""), namespace));
                String svcDesc = serviceDescriptions.get(namespace);
                if (svcDesc == null) {
                    throw new RuntimeException("No service description exists for namespace " + namespace);
                } else {
                    svc.setDescription(svcDesc);
                }
            }
            String cmdName;
            Command cmd;
            XmlElementDescription desc;
            String className = jaxbClass.getName();
            className = jaxbInfo.getRootElementName();
            if (className.endsWith("Request")) {
                cmdName = className.substring(0, className.lastIndexOf("Request"));
                cmd = svc.getCommand(namespace, cmdName);
                if (cmd == null) {
                    cmd = svc.addCommand(new Command(svc, cmdName, namespace));
                }
                cmd.setDescription(cmdName + " Description");
                desc = XmlElementDescription.createTopLevel(jaxbInfo, namespace, jaxbInfo.getRootElementName());
                cmd.setRootRequestElement(desc);
                // getJavaDocInfoForCommand(cmd, jaxbClass);
            } else if (className.endsWith("Response")) {
                cmdName = className.substring(0, className.lastIndexOf("Response"));
                cmd = svc.getCommand(namespace, cmdName);
                if (cmd == null) {
                    cmd = svc.addCommand(new Command(svc, cmdName, namespace));
                }
                desc = XmlElementDescription.createTopLevel(jaxbInfo, namespace, jaxbInfo.getRootElementName());
                cmd.setRootResponseElement(desc);
            }
        }
        populateWithJavadocInfo(root);
        return root;
    }

    /**
     * Main
     */
    public static void main(String[] args) throws Exception {

        readArguments(args);

        Root root = processJaxbClasses();
        // Root root = processWsdl();

        // Useful for debug?
        // root.dump();

        // process FreeMarker templates
        Properties templateContext = new Properties();
        templateContext.setProperty(TemplateHandler.PROP_TEMPLATES_DIR, templatesDir);
        templateContext.setProperty(TemplateHandler.PROP_OUTPUT_DIR, outputDir);
        templateContext.setProperty(TemplateHandler.PROP_BUILD_VERSION, buildVersion);
        templateContext.setProperty(TemplateHandler.PROP_BUILD_DATE, buildDate);

        // generate the API Reference documentation
        ApiReferenceTemplateHandler templateHandler = new ApiReferenceTemplateHandler(templateContext);
        templateHandler.process(root);
    }
}
