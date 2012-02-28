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

package com.zimbra.doc.soap.doclet;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.sun.javadoc.RootDoc;
import com.zimbra.doc.soap.ApiClassDocumentation;
import com.zimbra.doc.soap.Root;
import com.zimbra.doc.soap.SoapDocException;
import com.zimbra.doc.soap.WsdlDocGenerator;
import com.zimbra.doc.soap.template.ApiReferenceTemplateHandler;
import com.zimbra.doc.soap.template.TemplateHandler;

/**
 * Special purpose doclet used to create documentation for the Zimbra SOAP API
 */
public class ZmApiDoclet {

    private static final String ARG_TEMPLATES_DIR = "--templates-dir";
    private static final String ARG_OUTPUT_DIR    = "--output-dir";
    private static final String ARG_BUILD_VERSION = "--build-version";
    private static final String ARG_BUILD_DATE    = "--build-date";

    private static String templatesDir = null;
    private static String outputDir = null;
    private static String buildVersion = null;
    private static String buildDate = null;
 
    private static final String[] ARG_NAMES = { ARG_TEMPLATES_DIR, ARG_OUTPUT_DIR, ARG_BUILD_VERSION, ARG_BUILD_DATE };
    private static DocletApiListener apiListener;

    public static void setListener(DocletApiListener listener) {
        apiListener = listener;
    }

    /**
     * Starts processing the classes at the root document
     *
     * @param root the root document
     * @throws IOException 
     * @throws SoapDocException 
     */
    public static boolean start(RootDoc root) throws IOException, SoapDocException {
        if (apiListener == null) {
            setListener(new DocletApiListener());
        }
        apiListener.processJavadocResults(root);
        // map between class name and associated documentation
        Map<String, ApiClassDocumentation> javadocInfo = apiListener.getDocMap();
        readOptions(root.options());
        Root soapApiDataModelRoot = WsdlDocGenerator.processJaxbClasses(javadocInfo);

        // process FreeMarker templates
        Properties templateContext = new Properties();
        templateContext.setProperty(TemplateHandler.PROP_TEMPLATES_DIR, templatesDir);
        templateContext.setProperty(TemplateHandler.PROP_OUTPUT_DIR, outputDir);
        templateContext.setProperty(TemplateHandler.PROP_BUILD_VERSION, buildVersion);
        templateContext.setProperty(TemplateHandler.PROP_BUILD_DATE, buildDate);

        // generate the API Reference documentation
        ApiReferenceTemplateHandler templateHandler = new ApiReferenceTemplateHandler(templateContext);
        templateHandler.process(soapApiDataModelRoot);
        return true;
    }

    private static void readOptions(String[][] options) {
        for (String[] opt : options) {
            if (opt[0].equals(ARG_TEMPLATES_DIR)) {
                templatesDir = opt[1];
            } else if (opt[0].equals(ARG_OUTPUT_DIR)) {
                outputDir = opt[1];
            } else if (opt[0].equals(ARG_BUILD_VERSION)) {
                buildVersion = opt[1];
            } else if (opt[0].equals(ARG_BUILD_DATE)) {
                buildDate = opt[1];
            }
        }
    }

    /**
     * Required method as part of the doclet API if you have your own options
     */
    public static int optionLength(String option) {
        for (String argName : ARG_NAMES) {
            if (option.equals(argName)) {
                return 2;
            }
        }
        return 0;
    }

}
