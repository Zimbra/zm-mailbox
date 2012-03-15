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

import java.util.Map;
import java.util.Map.Entry;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.common.soap.AppBlastConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ReplicationConstants;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.util.JaxbInfo;
import javax.xml.bind.annotation.XmlSchema;

/**
 * Helper class for ZmApiDoclet intended to facilitate the generation of documentation for the Zimbra SOAP API.
 */
public class WsdlDocGenerator {

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

    private static void updateElementDescriptionWithApiClassDocumentation(
                        XmlElementDescription elem, ApiClassDocumentation doc) {
        if (doc == null) {
            return;
        }
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
            if (elem.getValueFieldName() != null) {
                if (elem.getValueFieldName().equals(entry.getKey())) {
                    elem.setValueFieldDescription(entry.getValue());
                }
            }
            // public JaxbValueInfo getElementValue() { return elementValue; }
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

    private static void populateWithJavadocInfo(DescriptionNode node,
            Map<String,ApiClassDocumentation> javadocInfo) {
        if (node instanceof XmlElementDescription) {
            XmlElementDescription elem = (XmlElementDescription) node;
            Class<?> jaxbClass = elem.getJaxbClass();
            // Loop over class and superclasses to ensure we get all documentation
            while ((jaxbClass != null) && (jaxbClass.getName().startsWith("com.zimbra"))) {
                ApiClassDocumentation doc = (jaxbClass != null) ? javadocInfo.get(jaxbClass.getName()) : null;
                updateElementDescriptionWithApiClassDocumentation(elem, doc);
                jaxbClass = jaxbClass.getSuperclass();
            }
        }
        for (DescriptionNode childNode : node.getChildren()) {
            populateWithJavadocInfo(childNode, javadocInfo);
        }
    }

    private static void populateWithJavadocInfo(Root root, Map<String,ApiClassDocumentation> javadocInfo) {
        for (Command cmd : root.getAllCommands()) {
            String reqClass = cmd.getRequest().getJaxbClass().getName();
            ApiClassDocumentation doc = javadocInfo.get(reqClass);
            if ((doc != null) && (!Strings.isNullOrEmpty(doc.getCommandDescription()))) {
                cmd.setDescription(doc.getCommandDescription());
                cmd.setNetworkEdition(doc.isNetworkEdition());
                cmd.setDeprecation(doc.getDeprecationDescription());
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

    /**
     * Traverse description tree, creating pointers from duplicate elements to an original, so that we can
     * reduce the amount of repetition in document
     */
    private static void markupDuplicateElements(XmlElementDescription elemDesc) {
        Map<Class<?>,XmlElementDescription> primaryDescriptions = Maps.newHashMap();
        elemDesc.markupDuplicateElements(primaryDescriptions);
    }

    public static Root processJaxbClasses(Map<String,ApiClassDocumentation> javadocInfo) {
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
                desc = XmlElementDescription.createTopLevel(jaxbInfo, namespace, jaxbInfo.getRootElementName());
                markupDuplicateElements(desc);
                cmd.setDescription(desc.getDescription());
                cmd.setRootRequestElement(desc);
                // getJavaDocInfoForCommand(cmd, jaxbClass);
            } else if (className.endsWith("Response")) {
                cmdName = className.substring(0, className.lastIndexOf("Response"));
                cmd = svc.getCommand(namespace, cmdName);
                if (cmd == null) {
                    cmd = svc.addCommand(new Command(svc, cmdName, namespace));
                }
                desc = XmlElementDescription.createTopLevel(jaxbInfo, namespace, jaxbInfo.getRootElementName());
                markupDuplicateElements(desc);
                cmd.setRootResponseElement(desc);
            }
        }
        populateWithJavadocInfo(root, javadocInfo);
        return root;
    }
}
