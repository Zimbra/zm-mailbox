/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.doc.soap.changelog;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.doc.soap.apidesc.SoapApiCommand;
import com.zimbra.doc.soap.apidesc.SoapApiDescription;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Create an HTML Changelog for the Zimbra SOAP API
 * @author gren
 */
public class SoapApiChangeLog {

    private static final String ARG_OUTPUT_DIR    = "output-dir";
    private static final String ARG_APIDESC_BASELINE_JSON  = "baseline-apidesc";
    private static final String ARG_APIDESC_CURRENT_JSON  = "current-apidesc";
    private static final String ARG_TEMPLATES_DIR    = "templates-dir";

    private static final String CHANGELOG_TEMPLATE_SUBDIR = "changelog";
    public static final String TEMPLATE_FILE = "index.ftl";
    public static final String OUTPUT_FILE = "index.html";

    private String outputDir;
    private String templateDir;
    private final List<SoapApiCommand> newCommands = Lists.newArrayList();
    private final List<SoapApiCommand> deletedCommands = Lists.newArrayList();
    private final List<CommandChanges> modifiedCommands = Lists.newArrayList();
 
    private Template fmTemplate = null;
    private Map<String,Object> changelogDataModel = null;
    private SoapApiDescription baselineDesc;
    private SoapApiDescription currentDesc;
    
    public SoapApiChangeLog() {
    }

    public SoapApiChangeLog(String outputDir, String templateDir) {
        this.outputDir = outputDir;
        this.templateDir = templateDir;
    }

    private Map<String,Object> createChangeLogDataModel() {
        Map<String,Object> root = Maps.newHashMap();
        root.put("baseline", baselineDesc);
        root.put("comparison", currentDesc);
        root.put("addedCommands", newCommands);
        root.put("removedCommands", deletedCommands);
        root.put("modifiedCommands", modifiedCommands);
        return root;
    }

    private void writeChangelog()
    throws IOException, TemplateException {
        File templateDirFile = new File(templateDir, CHANGELOG_TEMPLATE_SUBDIR);
        Configuration config = new Configuration();
        config.setDirectoryForTemplateLoading(templateDirFile);
        config.setObjectWrapper(new BeansWrapper());
        fmTemplate = config.getTemplate(TEMPLATE_FILE);
        File of = new File(outputDir, OUTPUT_FILE);
        FileWriter out = new FileWriter(of);
        try {
            fmTemplate.process(changelogDataModel, out);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                // worst case scenario...clean-up quietly
            }
        }
    }

    public void makeChangeLogDataModel()
    throws JsonParseException, JsonMappingException, IOException, TemplateException {
        Map<String,SoapApiCommand> baselineCmds = Maps.newTreeMap();
        Map<String,SoapApiCommand> currentCmds = Maps.newTreeMap();
        for (SoapApiCommand cmd : currentDesc.getCommands()) {
            currentCmds.put(cmd.getId(), cmd);
        }
        for (SoapApiCommand cmd : baselineDesc.getCommands()) {
            baselineCmds.put(cmd.getId(), cmd);
        }
        for (Entry<String, SoapApiCommand> entry : currentCmds.entrySet()) {
            SoapApiCommand baselineCmd = baselineCmds.get(entry.getKey());
            if (baselineCmd != null) {
                CommandChanges cmd = new CommandChanges(baselineCmd, entry.getValue(),
                        baselineDesc.getTypes(), currentDesc.getTypes());
                if (cmd.hasChanges()) {
                    modifiedCommands.add(cmd);
                }
            } else {
                newCommands.add(entry.getValue());
            }
        }
        for (Entry<String, SoapApiCommand> entry : baselineCmds.entrySet()) {
            if (!currentCmds.containsKey(entry.getKey())) {
                deletedCommands.add(entry.getValue());
            }
        }
        changelogDataModel = createChangeLogDataModel();
    }

    public SoapApiDescription getBaselineDesc() { return baselineDesc; }
    public void setBaselineDesc(SoapApiDescription baselineDesc) { this.baselineDesc = baselineDesc; }
    public SoapApiDescription getCurrentDesc() { return currentDesc; }
    public void setCurrentDesc(SoapApiDescription currentDesc) { this.currentDesc = currentDesc; }

    /**
     * Main
     */
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        Option opt;
        opt = new Option("d", ARG_OUTPUT_DIR, true, "Output directory for changelog information");
        opt.setRequired(true);
        options.addOption(opt);
        opt = new Option("t", ARG_TEMPLATES_DIR, true, "Directory containing Freemarker templates");
        opt.setRequired(true);
        options.addOption(opt);
        opt = new Option("b", ARG_APIDESC_BASELINE_JSON, true, "JSON file - description of baseline SOAP API");
        opt.setRequired(true);
        options.addOption(opt);
        opt = new Option("c", ARG_APIDESC_CURRENT_JSON, true, "JSON file - description of current SOAP API");
        opt.setRequired(true);
        options.addOption(opt);

        CommandLine cl = null;
        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            System.err.println("error: " + pe.getMessage());
            System.exit(2);
        }

        String baselineApiDescriptionJson = cl.getOptionValue('b');
        String currentApiDescriptionJson = cl.getOptionValue('c');
        SoapApiChangeLog clog = new SoapApiChangeLog(cl.getOptionValue('d'), cl.getOptionValue('t'));
        clog.setBaselineDesc(SoapApiDescription.deserializeFromJson(new File(baselineApiDescriptionJson)));
        clog.setCurrentDesc(SoapApiDescription.deserializeFromJson(new File(currentApiDescriptionJson)));
        clog.makeChangeLogDataModel();
        clog.writeChangelog();
    }

    public List<SoapApiCommand> getNewCommands() { return newCommands; }
    public List<SoapApiCommand> getDeletedCommands() { return deletedCommands; }
    public List<CommandChanges> getModifiedCommands() { return modifiedCommands; }
}
