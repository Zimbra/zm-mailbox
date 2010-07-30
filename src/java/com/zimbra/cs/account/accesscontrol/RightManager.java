/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.FileGenUtil;
import com.zimbra.cs.account.accesscontrol.Right.RightType;


public class RightManager {
    
    private static final String E_A            = "a";
    private static final String E_ATTRS        = "attrs";
    private static final String E_DEFAULT      = "default";
    private static final String E_DESC         = "desc";
    private static final String E_DOC          = "doc";
    private static final String E_INCLUDE      = "include";
    private static final String E_R            = "r";
    private static final String E_RIGHTS       = "rights";
    private static final String E_RIGHT        = "right";
    
    private static final String A_FALLBACK     = "fallback";
    private static final String A_FILE         = "file";
    private static final String A_LIMIT        = "l";
    private static final String A_N            = "n";
    private static final String A_NAME         = "name";
    private static final String A_TARGET_TYPE  = "targetType";
    private static final String A_TYPE         = "type";
    private static final String A_USER_RIGHT   = "userRight";
    
    private static final String TARGET_TYPE_DELIMITER   = ",";
    
    private static RightManager mInstance;
    
    // keep the map sorted so "zmmailbox lp" can display in alphabetical order 
    private Map<String, UserRight> sUserRights = new TreeMap<String, UserRight>();  
    private Map<String, AdminRight> sAdminRights = new TreeMap<String, AdminRight>();  

    static private class CoreRightDefFiles {
        private static final HashSet<String> sCoreRightDefFiles = new HashSet<String>();
        
        static {
            sCoreRightDefFiles.add("zimbra-rights.xml");
            sCoreRightDefFiles.add("zimbra-user-rights.xml");
            // sCoreRightDefFiles.add("rights-unittest.xml");
        }
        
        static boolean isCoreRightFile(File file) {
            return sCoreRightDefFiles.contains(file.getName());
        }
        
        static String listCoreDefFiles() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String file : sCoreRightDefFiles) {
                if (!first)
                    sb.append(", ");
                else
                    first = false;
                sb.append(file);
            }
            return sb.toString();
        }
    }
    
    public static synchronized RightManager getInstance() throws ServiceException {
        if (mInstance != null) {
            return mInstance;
        }
        String dir = LC.zimbra_rights_directory.value();
        mInstance = new RightManager(dir);
        
        try {
            Right.init(mInstance);
        } catch (ServiceException e) {
            ZimbraLog.acl.error("failed to initialize known right from: " + dir, e);
            throw e;
        }
        return mInstance;
    }
    
    private RightManager(String dir) throws ServiceException {
        File fdir = new File(dir);
        if (!fdir.exists()) {
            throw ServiceException.FAILURE("rights directory does not exists: " + dir, null);
        }
        if (!fdir.isDirectory()) {
            throw ServiceException.FAILURE("rights directory is not a directory: " + dir, null);
        }
                
        File[] files = fdir.listFiles();
        List<File> yetToProcess = new ArrayList<File>(Arrays.asList(files));
        List<File> processed = new ArrayList<File>();
        
        while (!yetToProcess.isEmpty()) { 
            File file = yetToProcess.get(0);
            
            if (!file.getPath().endsWith(".xml")) {
                ZimbraLog.acl.warn("while loading rights, ignoring none .xml file: " + file);
                yetToProcess.remove(file);
                continue;
            }
            if (!file.isFile()) {
                ZimbraLog.acl.warn("while loading rights, ignored non-file: " + file);
            }
            try {
                boolean done = loadSystemRights(file, processed);
                if (done) {
                    processed.add(file);
                    yetToProcess.remove(file);
                } else {
                    // move this file to the end
                    yetToProcess.remove(file);
                    yetToProcess.add(file);
                }
            } catch (DocumentException de) {
                throw ServiceException.PARSE_ERROR("error loading rights file: " + file, de);
            }
        }
    }
    
    private boolean getBoolean(String value) throws ServiceException {
        if ("1".equals(value))
            return true;
        else if ("0".equals(value))
            return false;
        else
            throw ServiceException.PARSE_ERROR("invalid value:" + value, null);
    }
    
    private boolean getBooleanAttr(Element elem, String attr) throws ServiceException {
        String value = elem.attributeValue(attr);
        if (value == null)
            throw ServiceException.PARSE_ERROR("missing required attribute: " + attr, null);
        return getBoolean(value);
    }
    
    private boolean getBooleanAttr(Element elem, String attr, boolean defaultValue) throws ServiceException {
        String value = elem.attributeValue(attr);
        if (value == null)
            return defaultValue;
        return getBoolean(value);
    }
    
    private void parseDesc(Element eDesc, Right right) throws ServiceException {
        if (right.getDesc() != null)
            throw ServiceException.PARSE_ERROR("multiple " + E_DESC, null);
        right.setDesc(eDesc.getText());
    }
    
    private void parseDoc(Element eDoc, Right right) throws ServiceException {
        if (right.getDoc() != null)
            throw ServiceException.PARSE_ERROR("multiple " + E_DOC, null);
        right.setDoc(eDoc.getText());
    }
    
    private void parseDefault(Element eDefault, Right right) throws ServiceException {
        String defaultValue = eDefault.getText();
        if ("allow".equalsIgnoreCase(defaultValue))
            right.setDefault(Boolean.TRUE);
        else if ("deny".equalsIgnoreCase(defaultValue))
            right.setDefault(Boolean.FALSE);
        else
            throw ServiceException.PARSE_ERROR("invalid default value: " + defaultValue, null);
    }
    
    private void parseAttr(Element eAttr, AttrRight right) throws ServiceException {
        String attrName = eAttr.attributeValue(A_N);
        if (attrName == null)
            throw ServiceException.PARSE_ERROR("missing attr name", null);   
        
        right.validateAttr(attrName);
        right.addAttr(attrName);
    }
    
    private void parseAttrs(Element eAttrs, Right right) throws ServiceException {
        if (!(right instanceof AttrRight))
            throw ServiceException.PARSE_ERROR(E_ATTRS + " is only allowed for admin getAttrs or setAttrs right", null);
        
        AttrRight attrRight = (AttrRight)right;
        for (Iterator elemIter = eAttrs.elementIterator(); elemIter.hasNext();) {
            Element elem = (Element)elemIter.next();
            if (elem.getName().equals(E_A))
                parseAttr(elem, attrRight);
            else
                throw ServiceException.PARSE_ERROR("invalid element: " + elem.getName(), null);   
        }
    }
    
    private void parseRight(Element eAttr, ComboRight right) throws ServiceException {
        String rightName = eAttr.attributeValue(A_N);
        if (rightName == null)
            throw ServiceException.PARSE_ERROR("missing right name", null);   
            
        Right r = getRight(rightName); // getRight will throw if the right does not exist.
        // combo right can only contain admin rights
        if (r.isUserRight())
            throw ServiceException.PARSE_ERROR(r.getName() + " is an user right, combo right " +
                    "can only contain admin rights.", null);   
            
        right.addRight(r);
    }
    
    private void parseRights(Element eAttrs, Right right) throws ServiceException {
        if (!(right instanceof ComboRight))
            throw ServiceException.PARSE_ERROR(E_RIGHTS + " is only allowed for admin combo right", null);
        
        ComboRight comboRight = (ComboRight)right;
        
        for (Iterator elemIter = eAttrs.elementIterator(); elemIter.hasNext();) {
            Element elem = (Element)elemIter.next();
            if (elem.getName().equals(E_R))
                parseRight(elem, comboRight);
            else
                throw ServiceException.PARSE_ERROR("invalid element: " + elem.getName(), null);   
        }
    }
    
    private Right parseRight(Element eRight) throws ServiceException {
        String name = eRight.attributeValue(A_NAME);
        
        // system define rights cannot contain a ".".  "." is the separator for inline attr right
        if (name.contains("."))
            throw ServiceException.PARSE_ERROR("righ name cannot contain dot(.): " + name, null);
        
        boolean userRight = getBooleanAttr(eRight, A_USER_RIGHT, false);
        
        // System.out.println("Parsing right " + "(" +  (userRight?"user":"admin") + ") " + name);
        Right right;
        
        AdminRight.RightType rightType = null;
        String targetTypeStr = eRight.attributeValue(A_TARGET_TYPE, null);
        
        if (userRight) {
            TargetType targetType;
            if (targetTypeStr != null)
                targetType = TargetType.fromCode(targetTypeStr);
            else
                targetType = TargetType.account;  // default target type for user right is account
            
            right = new UserRight(name);
            right.setTargetType(targetType);
            
            String fallback = eRight.attributeValue(A_FALLBACK, null);
            if (fallback != null) {
                CheckRightFallback fb = loadFallback(fallback, right);
                right.setFallback(fb);
            }
            
        } else {
            String rt = eRight.attributeValue(A_TYPE);
            if (rt == null)
                throw ServiceException.PARSE_ERROR("missing attribute [" + A_TYPE + "]", null);
            rightType = AdminRight.RightType.fromString(rt);
            
            right = AdminRight.newAdminSystemRight(name, rightType);
            if (targetTypeStr != null) {
                String taregtTypes[] = targetTypeStr.split(TARGET_TYPE_DELIMITER);
                for (String tt : taregtTypes) {
                    TargetType targetType = TargetType.fromCode(tt);
                    right.setTargetType(targetType);
                }
            }
        }

        for (Iterator elemIter = eRight.elementIterator(); elemIter.hasNext();) {
            Element elem = (Element)elemIter.next();
            if (elem.getName().equals(E_DESC))
                parseDesc(elem, right);
            else if (elem.getName().equals(E_DOC))
                parseDoc(elem, right);
            else if (elem.getName().equals(E_DEFAULT))
                parseDefault(elem, right);
            else if (elem.getName().equals(E_ATTRS))
                parseAttrs(elem, right);
            else if (elem.getName().equals(E_RIGHTS))
                parseRights(elem, right);
            else
                throw ServiceException.PARSE_ERROR("invalid element: " + elem.getName(), null);
        }
        
        // verify that all required fields are set and populate internal data
        right.completeRight();

        return right;
    }
    
    private static CheckRightFallback loadFallback(String clazz, Right right) {
        CheckRightFallback cb = null;
        if (clazz == null)
            return null;
        if (clazz.indexOf('.') == -1)
            clazz = "com.zimbra.cs.account.accesscontrol.fallback." + clazz;
        try {
            cb = (CheckRightFallback) Class.forName(clazz).newInstance();
            if (cb != null)
                cb.setRight(right);
        } catch (Exception e) {
            ZimbraLog.acl.warn("loadFallback " + clazz + " for right " + right.getName() +  " caught exception", e);
        }
        return cb;
    }
    
    private boolean loadSystemRights(File file, List<File> processedFiles) throws DocumentException, ServiceException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(file);
        Element root = doc.getRootElement();
        if (!root.getName().equals(E_RIGHTS))
            throw ServiceException.PARSE_ERROR("root tag is not " + E_RIGHTS, null);

        // preset rights can only be defined in our core right definition file
        boolean allowPresetRight = CoreRightDefFiles.isCoreRightFile(file);
        
        boolean seenRight = false;
        for (Iterator iter = root.elementIterator(); iter.hasNext();) {
            Element elem = (Element) iter.next();
            
            // see if all include files are processed already
            if (elem.getName().equals(E_INCLUDE)) {
                // all <include>'s have to appear <right>'s
                if (seenRight)
                    throw ServiceException.PARSE_ERROR(E_INCLUDE + " cannot appear after any right definition: " + elem.getName(), null);
                
                String includeFile = elem.attributeValue(A_FILE);
                boolean processed = false;
                for (File f : processedFiles) {
                    if (f.getName().equals(includeFile)) {
                        processed = true;
                        break;
                    }
                }
                if (!processed)
                    return false;
                else
                    continue;
            }
            
            Element eRight = elem;
            if (!eRight.getName().equals(E_RIGHT))
                throw ServiceException.PARSE_ERROR("unknown element: " + eRight.getName(), null);

            if (!seenRight) {
                seenRight = true;
                ZimbraLog.acl.debug("Loading " + file.getName());
            }
            
            String name = eRight.attributeValue(A_NAME);
            if (name == null)
                throw ServiceException.PARSE_ERROR("no name specified", null);
            
            if (sUserRights.get(name) != null || sAdminRights.get(name) != null) 
                throw ServiceException.PARSE_ERROR("right " + name + " is already defined", null);
            
            try {
                Right right = parseRight(eRight); 
                if (!allowPresetRight && RightType.preset == right.getRightType())
                    throw ServiceException.PARSE_ERROR(
                            "Encountered preset right " + name + " in " + file.getName() + 
                            ", preset right can only be defined in one of the core right definition files: " +
                            CoreRightDefFiles.listCoreDefFiles(), 
                            null);
                
                if (right instanceof UserRight)
                    sUserRights.put(name, (UserRight)right);
                else
                    sAdminRights.put(name, (AdminRight)right);
            } catch (ServiceException e) {
                throw ServiceException.PARSE_ERROR("unable to parse right: [" + name + "]", e);
            }
        }
        
        return true;
    }
    
    //
    // getters
    //
    
    public UserRight getUserRight(String right) throws ServiceException {
        UserRight r = sUserRights.get(right);
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        return r;
    }
    
    public AdminRight getAdminRight(String right) throws ServiceException {
        AdminRight r = sAdminRights.get(right);
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        return r;
    }
    
    public Right getRight(String right) throws ServiceException {
        if (InlineAttrRight.looksLikeOne(right))
            return InlineAttrRight.newInlineAttrRight(right);
        else
            return getRight(right, true);
    }
    
    private Right getRight(String right, boolean mustFind) throws ServiceException {
        Right r = sUserRights.get(right);
        if (r == null)
            r = sAdminRights.get(right);
        
        if (mustFind && r == null)
            throw AccountServiceException.NO_SUCH_RIGHT("invalid right " + right);
        
        return r;
    }
    
    public Map<String, UserRight> getAllUserRights() {
        return sUserRights;
    }
    
    public Map<String, AdminRight> getAllAdminRights() {
        return sAdminRights;
    }
    
    private String dump(StringBuilder sb) {
        if (sb == null)
            sb = new StringBuilder();
        
        sb.append("============\n");
        sb.append("user rights:\n");
        sb.append("============\n");
        for (Map.Entry<String, UserRight> ur : getAllUserRights().entrySet()) {
            sb.append("\n------------------------------\n");
            ur.getValue().dump(sb);
        }
        
        sb.append("\n");
        sb.append("\n");
        sb.append("=============\n");
        sb.append("admin rights:\n");
        sb.append("=============\n");
        for (Map.Entry<String, AdminRight> ar : getAllAdminRights().entrySet()) {
            sb.append("\n------------------------------\n");
            ar.getValue().dump(sb);
        }
        
        return sb.toString();
    }
    
    void genRightConst(Right r, StringBuilder sb) {
        sb.append("\n    /**\n");
        if (r.getDesc() != null) {
            sb.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(r.getDesc()), 70, "     * "));
            sb.append("\n");
        }
        sb.append("     */\n");
        sb.append("    public static final String RT_" + r.getName() + " = \"" + r.getName() + "\";" + "\n");
    }
    
    private String genRightConsts() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n\n");
        sb.append("    /*\n");
        sb.append("    ============\n");
        sb.append("    user rights:\n");
        sb.append("    ============\n");
        sb.append("    */\n\n");
        for (Map.Entry<String, UserRight> ur : getAllUserRights().entrySet()) {
            genRightConst(ur.getValue(), sb);
        }
        
        sb.append("\n\n");
        sb.append("    /*\n");
        sb.append("    =============\n");
        sb.append("    admin rights:\n");
        sb.append("    =============\n");
        sb.append("    */\n\n");
        for (Map.Entry<String, AdminRight> ar : getAllAdminRights().entrySet()) {
            genRightConst(ar.getValue(), sb);
        }
        
        return sb.toString();
    }
    
    private String genAdminRights() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n\n");
        for (AdminRight r : getAllAdminRights().values()) {
            sb.append("    public static AdminRight R_" + r.getName() + ";" + "\n");
        }
        
        sb.append("\n\n");
        sb.append("    public static void init(RightManager rm) throws ServiceException {\n");
        for (AdminRight r : getAllAdminRights().values()) {
            String s = String.format("        R_%-36s = rm.getAdminRight(Right.RT_%s);\n", r.getName(), r.getName());
            sb.append(s);
        }
        sb.append("    }\n");
        return sb.toString();
    }
    
    private String genUserRights() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n\n");
        for (UserRight r : getAllUserRights().values()) {
            sb.append("    public static UserRight R_" + r.getName() + ";" + "\n");
        }
        
        sb.append("\n\n");
        sb.append("    public static void init(RightManager rm) throws ServiceException {\n");
        for (UserRight r : getAllUserRights().values()) {
            String s = String.format("        R_%-36s = rm.getUserRight(Right.RT_%s);\n", r.getName(), r.getName());
            sb.append(s);
        }
        sb.append("    }\n");
        return sb.toString();
    }
    
    private static class CL {
        private static Options sOptions = new Options();
        
        static {
            sOptions.addOption("h", "help", false, "display this  usage info");
            sOptions.addOption("a", "action", true, "action, one of genRightConsts, genAdminRights, genUserRights");
            sOptions.addOption("i", "input", true,"rights definition xml input directory");
            sOptions.addOption("r", "regenerateFile", true, "file file to regenerate");
            sOptions.addOption("t", "templateFile", true, "template file");
        }
        
        private static void check() throws ServiceException  {
            ZimbraLog.toolSetupLog4j("DEBUG", "/Users/pshao/sandbox/conf/log4j.properties.phoebe");
            
            RightManager rm = new RightManager("/Users/pshao/p4/main/ZimbraServer/conf/rights");
            System.out.println(rm.dump(null));
        }
        
        private static void genDomainAdminSetAttrsRights(String outFile, String templateFile) throws Exception {
            Set<String> acctAttrs = getDomainAdminModifiableAttrs(AttributeClass.account);
            Set<String> crAttrs = getDomainAdminModifiableAttrs(AttributeClass.calendarResource);
            Set<String> dlAttrs = getDomainAdminModifiableAttrs(AttributeClass.distributionList);
            Set<String> domainAttrs = getDomainAdminModifiableAttrs(AttributeClass.domain);
            
            Set<String> acctAndCrAttrs = SetUtil.intersect(acctAttrs, crAttrs);
            Set<String> acctOnlyAttrs = SetUtil.subtract(acctAttrs, crAttrs);
            Set<String> crOnlyAttrs = SetUtil.subtract(crAttrs, acctAttrs);
            
            // sanity check, since we are not generating it, make sure it is indeed empty
            if (acctOnlyAttrs.size() != 0)
                throw ServiceException.FAILURE("account only attrs is not empty???", null);
            
            String acctAndCrAttrsFiller = genAttrs(acctAndCrAttrs);
            String crOnlyAttrsFiller = genAttrs(crOnlyAttrs);
            String dlAttrsFiller = genAttrs(dlAttrs);
            String domainAttrsFiller = genAttrs(domainAttrs);
            
            Map<String,String> templateFillers = new HashMap<String,String>();
            templateFillers.put("ACCOUNT_AND_CALENDAR_RESOURCE_ATTRS", acctAndCrAttrsFiller);
            templateFillers.put("CALENDAR_RESOURCE_ATTRS", crOnlyAttrsFiller);
            templateFillers.put("DISTRIBUTION_LIST_ATTRS", dlAttrsFiller);
            templateFillers.put("DOMAIN_ATTRS", domainAttrsFiller);
            
            genFile(outFile, templateFile, templateFillers);
        }
        
        private static void genFile(String outFile, String templateFile, Map<String,String> templateFillers) 
            throws Exception {
            // OutputStream os = new FileOutputStream(outFile);
            // PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "utf8")));
            
            byte[] templateBytes = ByteUtil.getContent(new File(templateFile));
            String templateString = new String(templateBytes, "utf-8");
            
            String content = StringUtil.fillTemplate(templateString, templateFillers);
            // pw.print(content);
            
            File oldFile = new File(outFile);
            if (!oldFile.canWrite()) {
                System.err.println("============================================");
                System.err.println("Unable to write to: "+outFile);
                System.err.println("============================================");
                System.exit(1);
            }
            
            BufferedWriter out = null;
            File newFile = new File(outFile+"-autogen");

            try {
                out = new BufferedWriter(new FileWriter(newFile));
                out.write(content);

                out.close();
                out = null;

                if (!newFile.renameTo(oldFile)) {
                    System.err.println("============================================");
                    System.err.format("Unable to rename(%s) to (%s)%n", newFile.getName(), oldFile);
                    System.err.println("============================================");
                    System.exit(1);
                }

                System.out.println("======================================");
                System.out.println("generated: "+outFile);
                System.out.println("======================================");

            } finally {
                if (out != null) out.close();
            }
        }
        
        private static Set<String> getDomainAdminModifiableAttrs(AttributeClass klass) throws ServiceException {
            AttributeManager am = AttributeManager.getInstance();
            Set<String> allAttrs = am.getAllAttrsInClass(klass);
            
            Set<String> domainAdminModifiableAttrs = new HashSet<String>();
            for (String attr : allAttrs) {
                if (am.isDomainAdminModifiable(attr, klass))
                    domainAdminModifiableAttrs.add(attr);
            }
            return domainAdminModifiableAttrs;
        }
            
        private static String genAttrs(Set<String> attrs) {
            // sort it
            Set<String> sortedAttrs = new TreeSet<String>(attrs);
            
            StringBuilder sb = new StringBuilder();
            for (String attr : sortedAttrs) {
                sb.append("    <a n=\"" + attr + "\"/>\n");
            }
            return sb.toString();
        }

        private static void usage(String errmsg) {
            if (errmsg != null) {
                System.out.println(errmsg);
            }
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("AttributeManager [options] where [options] are one of:", sOptions);
            System.exit((errmsg == null) ? 0 : 1);
        }
        
        private static CommandLine parseArgs(String args[]) {
            StringBuffer gotCL = new StringBuffer("cmdline: ");
            for (int i = 0; i < args.length; i++) {
                gotCL.append("'").append(args[i]).append("' ");
            }
            System.out.println(gotCL);
                
            CommandLineParser parser = new GnuParser();
            CommandLine cl = null;
            try {
                cl = parser.parse(sOptions, args);
            } catch (ParseException pe) {
                usage(pe.getMessage());
            }
            if (cl.hasOption('h')) {
                usage(null);
            }
            return cl;
        }
        
        private static void main(String[] args) throws Exception {
            CliUtil.toolSetup();
            CommandLine cl = parseArgs(args);
        
            if (!cl.hasOption('a')) usage("no action specified");
            String action = cl.getOptionValue('a');
            
            if (!"validate".equals(action)) {
                if (!cl.hasOption('r')) usage("no regenerate file specified");
            }
            
            String regenFile = cl.getOptionValue('r');
            
            String inputDir = null;
            RightManager rm = null;
            if (!"genDomainAdminSetAttrsRights".equals(action)) {
                if (!cl.hasOption('i')) usage("no input dir specified");
                inputDir = cl.getOptionValue('i');
                rm = new RightManager(inputDir);
            }
             
            if ("genRightConsts".equals(action))
                FileGenUtil.replaceJavaFile(regenFile, rm.genRightConsts());
            else if ("genAdminRights".equals(action))
                FileGenUtil.replaceJavaFile(regenFile, rm.genAdminRights());
            else if ("genUserRights".equals(action))
                FileGenUtil.replaceJavaFile(regenFile, rm.genUserRights());
            else if ("genDomainAdminSetAttrsRights".equals(action)) {
                String templateFile = cl.getOptionValue('t');
                genDomainAdminSetAttrsRights(regenFile, templateFile);
            } else if ("validate".equals(action)) {
                // do nothing, all we need is that new RightManager(inputDir) works,
                // which is done above.
            } else
                usage("invalid action");
        }
    }
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CL.main(args);
        
        // CL.check();
    }
    
}
