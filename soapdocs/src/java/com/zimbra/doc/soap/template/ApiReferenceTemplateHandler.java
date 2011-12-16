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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;
import com.zimbra.doc.soap.Root;
import com.zimbra.doc.soap.Command;
import com.zimbra.doc.soap.Service;
import com.zimbra.doc.soap.SoapDocException;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * This class represents a template handler for generating the Zimbra SOAP API Reference (in HTML).
 *
 */
public class ApiReferenceTemplateHandler extends TemplateHandler {

    private static final String NAME = "api-reference";

    private static final String TEMPLATE_INDEX = "index.ftl";
    private static final String TEMPLATE_ALL_COMMANDS_FRAME = "allcommands-frame.ftl";
    private static final String TEMPLATE_OVERVIEW_FRAME = "overview-frame.ftl";
    private static final String TEMPLATE_OVERVIEW_SUMMARY = "overview-summary.ftl";
    private static final String TEMPLATE_SERVICE_FRAME = "service/service-frame.ftl";
    private static final String TEMPLATE_SERVICE_SUMMARY = "service/service-summary.ftl";
    private static final String TEMPLATE_SERVICE_COMMAND = "service/command.ftl";

    private static final String OUTPUT_INDEX = "index.html";
    private static final String OUTPUT_ALL_COMMANDS_FRAME = "allcommands-frame.html";
    private static final String OUTPUT_OVERVIEW_FRAME = "overview-frame.html";
    private static final String OUTPUT_OVERVIEW_SUMMARY = "overview-summary.html";

    private static final String OUTPUT_SERVICE_FRAME = "service-frame.html";
    private static final String OUTPUT_SERVICE_SUMMARY = "service-summary.html";

    public static final String KEY_SERVICE = "service";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_ALL_COMMANDS = "allcommands";

    private    Template indexTemplate = null;
    private    Template overviewFrameTemplate = null;
    private    Template overviewSummaryTemplate = null;
    private    Template allCommandsFrameTemplate = null;
    private    Template serviceFrameTemplate = null;
    private    Template serviceSummaryTemplate = null;
    private    Template serviceCommandTemplate = null;

    /**
     * Constructor.
     *
     * @param context the template context properties
     */
    public ApiReferenceTemplateHandler(Properties context)
    throws    IOException {
        super(NAME, context);

        File templatesDirFile = new File(this.templatesDir, NAME);

        Configuration config = new Configuration();
        config.setDirectoryForTemplateLoading(templatesDirFile);
        config.setObjectWrapper(new BeansWrapper());

        // loads the templates
        indexTemplate = config.getTemplate(TEMPLATE_INDEX);
        overviewFrameTemplate = config.getTemplate(TEMPLATE_OVERVIEW_FRAME);
        overviewSummaryTemplate = config.getTemplate(TEMPLATE_OVERVIEW_SUMMARY);
        allCommandsFrameTemplate = config.getTemplate(TEMPLATE_ALL_COMMANDS_FRAME);
        serviceFrameTemplate = config.getTemplate(TEMPLATE_SERVICE_FRAME);
        serviceSummaryTemplate = config.getTemplate(TEMPLATE_SERVICE_SUMMARY);
        serviceCommandTemplate = config.getTemplate(TEMPLATE_SERVICE_COMMAND);

        copyFiles(new String[] { "stylesheet.css" });
    }

    /**
     * Processes the templates for this handler.
     */
    @Override
    public void process(Root root)
    throws IOException, SoapDocException {

        // process index file
        processIndex(root);

        // process overview frame file
        processOverviewFrame(root);

        // process overview summaryfile
        processOverviewSummary(root);

        // process all commands frame file
        processAllCommandsFrame(root);

        // process services
        for (Service svc : root.getServices()) {
            processService(root, svc);
        }
    }

    /**
     * Processes the service page.
     */
    private void processService(Root root, Service svc)
    throws IOException, SoapDocException {
        // make service directory
        File of = getOutputFile(svc.getName());
        of.mkdir();

        Map<String,Service> data = Maps.newHashMap();
        data.put(KEY_SERVICE, svc);

        // write service/service-frame.html
        File sf = new File(of, OUTPUT_SERVICE_FRAME);
        processTemplate(root, data, sf, this.serviceFrameTemplate);

        // write service/service-summary.html
        File ss = new File(of, OUTPUT_SERVICE_SUMMARY);
        processTemplate(root, data, ss, this.serviceSummaryTemplate);

        // write service/commands*.html
        processServiceCommands(root, svc, of);
    }

    /**
     * Processes the index page.
     *
     * @param    root        the root data
     */
    private    void    processIndex(Root root)
    throws    IOException, SoapDocException {
        processTemplate(root, null, OUTPUT_INDEX, this.indexTemplate);
    }

    /**
     * Processes the overview frame page.
     *
     * @param    root        the root data
     */
    private    void    processOverviewFrame(Root root)
    throws    IOException, SoapDocException {
        processTemplate(root, null, OUTPUT_OVERVIEW_FRAME, this.overviewFrameTemplate);
    }

    /**
     * Processes the overview summary page.
     *
     * @param    root        the root data
     */
    private    void    processOverviewSummary(Root root)
    throws    IOException, SoapDocException {
        processTemplate(root, null, OUTPUT_OVERVIEW_SUMMARY, this.overviewSummaryTemplate);
    }

    /**
     * Processes the all commands frame page.
     *
     * @param    root        the root data
     */
    private    void    processAllCommandsFrame(Root root)
    throws    IOException, SoapDocException {
        List<Command>    allCommands = root.getAllCommands();

        Map data = Maps.newHashMap();
        data.put(KEY_ALL_COMMANDS, allCommands);

        processTemplate(root, data, OUTPUT_ALL_COMMANDS_FRAME, this.allCommandsFrameTemplate);
    }

    /**
     * Processes the commands for a given service.
     *
     * @param    root        the root data
     * @param    service        the service
     */
    private    void    processServiceCommands(Root root, Service service, File outputFile)
    throws    IOException, SoapDocException {

        List<Command>    commands = service.getCommands();
        Iterator it = commands.iterator();
        while (it.hasNext()) {
            Command    c = (Command)it.next();

            String    filename = c.getName() + ".html";
            File cf = new File(outputFile, filename);

            Map data = Maps.newHashMap();
            data.put(KEY_COMMAND, c);
            data.put(KEY_SERVICE, service);

            processTemplate(root, data, cf, this.serviceCommandTemplate);
        }

    }
}
