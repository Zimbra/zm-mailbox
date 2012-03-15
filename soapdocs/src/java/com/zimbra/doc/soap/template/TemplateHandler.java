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

package com.zimbra.doc.soap.template;

import java.io.*;
import java.util.*;

import com.google.common.collect.Maps;
import com.zimbra.doc.soap.*;
import freemarker.template.*;

/**
 * This class represents the base class for all template handlers.
 *
 */
public abstract class TemplateHandler {

    public static final String PROP_TEMPLATES_DIR = "templates-dir";
    public static final String PROP_OUTPUT_DIR = "output-dir";
    public static final String PROP_BUILD_VERSION = "build-version";
    public static final String PROP_BUILD_DATE = "build-date";

    public static final String DEFAULT_BUILD_VERSION = "0.0.0";

    public static final String KEY_BUILD_INFO = "build";
    public static final String KEY_BUILD_INFO_VERSION = "version";
    public static final String KEY_BUILD_INFO_DATE = "date";

    public static final String KEY_SOAP_ROOT = "root";

    protected String templatesDir= null;
    protected String outputDir = null;
    protected String buildVersion = null;
    protected String buildDate = null;

    protected String name = null;

    /**
     * Constructor.
     *
     * @param name    the handler name
     * @param context the context properties for this handler
     */
    public TemplateHandler(String name, Properties context)
    throws IOException {
        this.name = name;

        this.templatesDir = getTemplatesDirProp(context);
        this.outputDir = getOutputDirProp(context);
        this.buildVersion = getBuildVersionProp(context);
        this.buildDate = getBuildDateProp(context);

        // initialize output dir
        File od = new File(this.outputDir, getName());
        od.mkdir();
    }

    private String getTemplatesDirProp(Properties context) {
        String obj = context.getProperty(PROP_TEMPLATES_DIR);
        if (obj == null)
            throw new IllegalArgumentException("must specify the template directory");
        return obj;
    }

    private String getOutputDirProp(Properties context) {
        String obj = context.getProperty(PROP_OUTPUT_DIR);
        if (obj == null)
            throw new IllegalArgumentException("must specify the output directory");
        return obj;
    }

    private String getBuildVersionProp(Properties context) {
        String obj = context.getProperty(PROP_BUILD_VERSION);
        if (obj == null || obj.length() <= 0)
            return    DEFAULT_BUILD_VERSION;
        return obj;
    }

    private String getBuildDateProp(Properties context) {
        String obj = context.getProperty(PROP_BUILD_DATE);
        if (obj == null || obj.length() <= 0) {
            Calendar cal = Calendar.getInstance();
            Date    now = cal.getTime();
            return    now.toString();
        }
        return obj;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Gets the data model.
     *
     * @param    data        the data
     * @return    the data model
     */
    private Map getDataModel(Map data) {
        Map dataModel = Maps.newHashMap();
        Map<String,String> buildInfo = getBuildInfo();

        dataModel.put(KEY_BUILD_INFO, buildInfo);

        Iterator it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            dataModel.put(entry.getKey(), entry.getValue());
        }

        return    dataModel;
    }

    protected Map<String,String> getBuildInfo() {
        Map<String,String>    buildInfo = new HashMap<String,String>();

        buildInfo.put(KEY_BUILD_INFO_VERSION, this.buildVersion);
        buildInfo.put(KEY_BUILD_INFO_DATE, this.buildDate);

        return    buildInfo;
    }

    protected File getOutputFile(String file)
    throws IOException {
        File od = new File(this.outputDir, getName());
        File of = new File(od, file);
        return    of;
    }

    /**
     * Processes the template.
     *
     * @param    root        the root data
     * @param    service        the service or <code>null</code> for none
     * @param    data        any additional data or <code>null</code> for none
     * @param    outputFile    the output file name
     * @param    template    the template to process
     */
    protected    void    processTemplate(Root root, Map data, String outputFile, Template template)
    throws    IOException, SoapDocException {
        File    of = getOutputFile(outputFile);

        processTemplate(root, data, of, template);
    }

    /**
     * Processes the template.
     *
     * @param    root        the root data
     * @param    service        the service or <code>null</code> for none
     * @param    data        any additional data or <code>null</code> for none
     * @param    outputFile    the output file
     * @param    template    the template to process
     */
    protected    void    processTemplate(Root root, Map data, File outputFile, Template template)
    throws    IOException, SoapDocException {
        FileWriter out = new FileWriter(outputFile);

        if (data == null)
            data = new HashMap();

        data.put(KEY_SOAP_ROOT, root);

        try {
            Map    dataModel = getDataModel(data);
               template.process(dataModel, out);
        } catch(TemplateException te) {
            throw new SoapDocException(te);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                // clean-up quietly
            }
        }
    }

    /**
     * Copies the given files from the templates directory to the output directory.
     *
     * @param    filesnames        an array of file names
     */
    protected void copyFiles(String[] filenames)
    throws java.io.FileNotFoundException, IOException {
        for (int i=0; i < filenames.length; i++) {
            File tmp = new File(templatesDir, getName());
            File fromFile = new File(tmp, filenames[i]);

            File toFile = getOutputFile(filenames[i]);

            FileInputStream from = null;
            FileOutputStream to = null;

            try {
                from = new FileInputStream(fromFile);
                to = new FileOutputStream(toFile);
                byte[] buffer = new byte[4096];
                int bytesRead = -1;

                while ((bytesRead = from.read(buffer)) != -1)
                    to.write(buffer, 0, bytesRead); // write
            } finally {
                if (from != null) {
                    try {
                        from.close();
                    } catch (IOException e) {
                        // clean-up quietly
                    }
                }
                if (to != null) {
                    try {
                        to.close();
                    } catch (IOException e) {
                        // clean-up quietly
                    }
                }
            }
        }

    }

    /**
     * Processes the template.
     *
     * @param    root        the root data
     */
    public abstract void process(Root root) throws IOException, SoapDocException;

} // end TemplateHandler class
