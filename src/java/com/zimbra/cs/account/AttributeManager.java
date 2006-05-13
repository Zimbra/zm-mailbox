/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

public class AttributeManager {

    private static final String ZIMBRA_ATTRS_RESOURCE = "zimbraattrs.xml";

    private static final String E_ATTRS = "attrs";
    private static final String A_GROUP = "group";
    private static final String A_GROUP_ID = "groupid";

    private static final String E_ATTR = "attr";
    private static final String A_NAME = "name";
    private static final String A_IMMUTABLE = "immutable";
    private static final String A_TYPE = "type";
    private static final String A_VALUE = "value";
    private static final String A_MAX = "max";
    private static final String A_MIN = "min";
    private static final String A_CALLBACK = "callback";
    private static final String A_ID = "id";
    private static final String A_CARDINALITY = "cardinality";
    private static final String A_REQUIRED_IN = "requiredIn";
    private static final String A_OPTIONAL_IN = "optionalIn";
    private static final String A_FLAGS = "flags";

    private static final String E_DESCRIPTION = "desc";
    private static final String E_GLOBAL_CONFIG_VALUE = "globalConfigValue";
    private static final String E_DEFAULT_COS_VALUE = "defaultCOSValue";
    
    private static AttributeManager mInstance;

    private Map<String, AttributeInfo> mAttrs = new HashMap<String, AttributeInfo>();

    private static Map<Integer,String> mGroupMap = new HashMap<Integer,String>();

    public static AttributeManager getInstance() throws ServiceException {
        synchronized(AttributeManager.class) {
            if (mInstance == null) {
                String file = ZIMBRA_ATTRS_RESOURCE;
                InputStream is = null;
                try {
                    is = AttributeManager.class.getResourceAsStream(file);
                    if (is == null) {
                        ZimbraLog.misc.warn("unable to find attr file resource: "+file);
                    } else {
                        mInstance = new AttributeManager(is, file);
                        if (mInstance.hasErrors()) {
                            throw ServiceException.FAILURE(mInstance.getErrors(), null);
                        }
                    }
                } catch (DocumentException e) {
                    ZimbraLog.misc.error("unable to parse attr file: " + file, e);
                    throw ServiceException.FAILURE("unable to parse attr file: "+file+" "+e.getMessage(), e);
                } catch (Exception e) {
                    ZimbraLog.misc.error("unable to load attr file: " + file, e);
                    throw ServiceException.FAILURE("unable to load attr file: " + file + " " + e.getMessage(), e);
                } finally {
                    if (is != null)
                        try { is.close();}  catch (IOException e) { }
                }
            }
        }
        return mInstance;
    }

    private AttributeManager(InputStream attrsFile, String file) throws DocumentException {
        loadAttrs(attrsFile, file);
    }
    
    private AttributeManager(String[] attrFiles) throws IOException, DocumentException {
        for (String file : attrFiles) {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            loadAttrs(is, file);
            is.close();
        }
    }

    private List<String> mErrors = new LinkedList<String>();
    
    private boolean hasErrors() {
        return mErrors.size() > 0;
    }
    
    private String getErrors() {
        StringBuilder result = new StringBuilder();
        for (String error : mErrors) {
            result.append(error).append("\n");
        }
        return result.toString();
    }
    
    private void error(String attrName, String file, String error) {
        if (attrName != null) {
            mErrors.add("attr " + attrName + " in file " + file + ": " + error);
        } else {
            mErrors.add("file " + file + ": " + error);
        }
    }
    
    private void loadAttrs(InputStream attrsFile, String file) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(attrsFile);
        Element root = doc.getRootElement();
        if (!root.getName().equals(E_ATTRS)) {
            error(null, file, "root tag is not " + E_ATTRS);
            return;
        }

        String group = root.attributeValue(A_GROUP);
        String groupIdStr = root.attributeValue(A_GROUP_ID);
        if (group == null ^ groupIdStr == null) {
            error(null, file, A_GROUP + " and " + A_GROUP_ID + " both have to be both specified");
        }
        int groupId = -1;
        if (group != null) {
            try {
                groupId = Integer.valueOf(groupIdStr);
            } catch (NumberFormatException nfe) {
                error(null, file, A_GROUP_ID + " is not a number: " + groupIdStr);
            }
        }
        if (groupId == 2) {
            error(null, file, A_GROUP_ID + " is not valid (used by ZimbraObjectClass)");
        } else if (groupId > 0) {
            if (mGroupMap.containsKey(groupId)) {
                error(null, file, "duplicate group id: " + groupId);
            } else if (mGroupMap.containsValue(group)) {
                error(null, file, "duplicate group: " + group);
            } else {
                mGroupMap.put(groupId, group);
            }
        }
        
        NEXT_ATTR: for (Iterator iter = root.elementIterator(); iter.hasNext();) {
            Element eattr = (Element) iter.next();
            if (!eattr.getName().equals(E_ATTR)) {
                error(null, file, "unknown element: " + eattr.getName());
                continue;
            }

            AttributeCallback callback = null;
            AttributeType type = null;
            String value = null;
            long min = Long.MIN_VALUE;
            long max = Long.MAX_VALUE;
            boolean immutable = false;
//            boolean ignore = false;
            int id = -1;
            AttributeCardinality cardinality = null;
            Set<AttributeClass> requiredIn = null;
            Set<AttributeClass> optionalIn = null;
            Set<AttributeFlag> flags = null;

            String canonicalName = null;
            String name = eattr.attributeValue(A_NAME);
            if (name == null) {
                error(null, file, "no name specified");
                continue;
            }
            canonicalName = name.toLowerCase();

            for (Iterator attrIter = eattr.attributeIterator(); attrIter.hasNext();) {
                Attribute attr = (Attribute) attrIter.next();
                String aname = attr.getName();
                if (aname.equals(A_NAME)) {
                    // nothing to do - already processed
                } else if (aname.equals(A_CALLBACK)) {
                    callback = loadCallback(attr.getValue());
                } else if (aname.equals(A_IMMUTABLE)) {
                    immutable = "1".equals(attr.getValue());
                } else if (aname.equals(A_MAX)) {
                    max = AttributeInfo.parseLong(attr.getValue(), Integer.MAX_VALUE);
                } else if (aname.equals(A_MIN)) {
                    min = AttributeInfo.parseLong(attr.getValue(), Integer.MIN_VALUE);
                } else if (aname.equals(A_TYPE)) {
                    type = AttributeType.getType(attr.getValue());
                    if (type == null) {
                        error(name, file, "unknown <attr> type: " + attr.getValue());
                        continue NEXT_ATTR;
                    }
                } else if (aname.equals(A_VALUE)) { 
                    value = attr.getValue();
                } else if (aname.equals(A_ID)) {
                    try {
                        id = Integer.parseInt(attr.getValue());
                        if (id < 0)  {
                            error(name, file, "invalid id " + id + ": must be positive");
                        }
                    } catch (NumberFormatException nfe) {
                        error(name, file, aname + " is not a number: " + attr.getValue());
                    }
                } else if (aname.equals(A_CARDINALITY)) {
                    try {
                        cardinality = AttributeCardinality.valueOf(attr.getValue());
                    } catch (IllegalArgumentException iae) {
                        error(name, file, aname + " is not valid: " + attr.getValue());
                    }
                } else if (aname.equals(A_REQUIRED_IN)) {
                    requiredIn = parseClasses(name, file, attr.getValue());
                } else if (aname.equals(A_OPTIONAL_IN)) {
                    optionalIn = parseClasses(name, file, attr.getValue());
                } else if (aname.equals(A_FLAGS)) {
                    flags = parseFlags(name, file, attr.getValue());
                } else {
                    error(name, file, "unknown <attr> attr: " + aname);
                }
            }

            List<String> globalConfigValues = new LinkedList<String>();
            List<String> defaultCOSValues = new LinkedList<String>();
            String description = null;
            for (Iterator elemIter = eattr.elementIterator(); elemIter.hasNext();) {
                Element elem = (Element)elemIter.next();
                if (elem.getName().equals(E_GLOBAL_CONFIG_VALUE)) {
                    globalConfigValues.add(elem.getText());
                } else if (elem.getName().equals(E_DEFAULT_COS_VALUE)) {
                    defaultCOSValues.add(elem.getText());
                } else if (elem.getName().equals(E_DESCRIPTION)) {
                    if (description != null) {
                        error(name, file, "more than one description");
                    }
                    description = elem.getText();
                } else {
                    error(name, file, "unknown element: " + elem.getName());
                }
            }

            // Check that if id is specified, then cardinality is specified.
            if (id > 0  && cardinality == null) {
                error(name, file, "cardinality not specified");
            }
            
            // Check that if id is specified, then atleast one object class is
            // defined
            if (id > 0 && (optionalIn != null && optionalIn.isEmpty()) && (requiredIn != null && requiredIn.isEmpty())) {
                error(name, file, "atleast one of " + A_REQUIRED_IN + " or " + A_OPTIONAL_IN + " must be specified");
            }

            // Check that if it is COS inheritable it is in account and COS
            // classes
            checkFlag(name, file, flags, AttributeFlag.inheritFromCOS, AttributeClass.account, AttributeClass.cos, requiredIn, optionalIn);

            // Check that if it is domain inheritable it is in domain and
            // account
            checkFlag(name, file, flags, AttributeFlag.inheritFromDomain, AttributeClass.account, AttributeClass.domain, requiredIn, optionalIn);

            // Check that if it is domain inheritable it is in domain and
            // account
            checkFlag(name, file, flags, AttributeFlag.inheritFromGlobalConfig, AttributeClass.server, AttributeClass.globalConfig, requiredIn, optionalIn);

            // Check that is cardinality is single, then not more than one
            // default value is specified
            if (cardinality == AttributeCardinality.single) {
                if (globalConfigValues.size() > 1) {
                    error(name, file, "more than one global config value specified for cardinality " + AttributeCardinality.single);
                }
                if (defaultCOSValues.size() > 1) {
                    error(name, file, "more than one default COS value specified for cardinality " + AttributeCardinality.single);
                }
            }

            AttributeInfo info = new AttributeInfo(name, id, groupId, callback, type, value, immutable, min, max, cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues, description);
            if (mAttrs.get(canonicalName) != null) {
                error(name, file, "duplicate definiton");
            }
            mAttrs.put(canonicalName, info);
        }
    }
    
    private Set<AttributeClass> parseClasses(String attrName, String file, String value) {
        Set<AttributeClass> result = new HashSet<AttributeClass>();
        String[] cnames = value.split(",");
        for (String cname : cnames) {
            try {
                AttributeClass ac = AttributeClass.valueOf(cname);
                if (result.contains(ac)) {
                    error(attrName, file, "duplicate class: " + cname);
                }
                result.add(ac);
            } catch (IllegalArgumentException iae) {
                error(attrName, file, "invalid class: " + cname);
            }
        }
        return result;
    }

    private Set<AttributeFlag> parseFlags(String attrName, String file, String value) {
        Set<AttributeFlag> result = new HashSet<AttributeFlag>();
        String[] flags = value.split(",");
        for (String flag : flags) {
            try {
                AttributeFlag ac = AttributeFlag.valueOf(flag);
                if (result.contains(ac)) {
                    error(attrName, file, "duplicate flag: " + flag);
                }
                result.add(ac);
            } catch (IllegalArgumentException iae) {
                error(attrName, file, "invalid flag: " + flag);
            }
        }
        return result;
    }

    private void checkFlag(String attrName, String file, Set<AttributeFlag> flags, AttributeFlag flag, AttributeClass c1, AttributeClass c2, Set<AttributeClass> required, Set<AttributeClass> optional) {
        if (flags != null && flags.contains(flag)) {
            boolean inC1 = (optional != null && optional.contains(c1)) || (required != null && required.contains(c1));
            boolean inC2 = (optional != null && optional.contains(c2)) || (required != null && required.contains(c2));
            if (!inC1 && !inC2) {
                error(attrName, file, "flag " + flag + " requires that attr be in both these classes " + c1 + " and " + c2);
            }
        }
    }

    /**
     * @param type
     * @return
     */
    private static AttributeCallback loadCallback(String clazz) {
        AttributeCallback cb = null;
        if (clazz == null)
            return null;
        if (clazz.indexOf('.') == -1)
            clazz = "com.zimbra.cs.account.callback." + clazz;
        try {
            cb = (AttributeCallback) Class.forName(clazz).newInstance();
        } catch (Exception e) {
            ZimbraLog.misc.warn("loadCallback caught exception", e);
        }
        return cb;
    }

    public void preModify(Map<String, ? extends Object> attrs, Entry entry, Map context, boolean isCreate, boolean checkImmutable) throws ServiceException {
    	String[] keys = attrs.keySet().toArray(new String[0]);
		for (int i = 0; i < keys.length; i++) {
		    String name = keys[i];
            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = mAttrs.get(name.toLowerCase());
            if (info != null) {
                info.checkValue(value, checkImmutable);
                if (info.getCallback() != null)
                    info.getCallback().preModify(context, name, value, attrs, entry, isCreate);
            } else {
                ZimbraLog.misc.warn("checkValue: no attribute info for: "+name);
            }
		}
    }

    public void postModify(Map<String, ? extends Object> attrs, Entry entry, Map context, boolean isCreate) {
    	String[] keys = attrs.keySet().toArray(new String[0]);
		for (int i = 0; i < keys.length; i++) {
			String name = keys[i];
//            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = mAttrs.get(name.toLowerCase());
            if (info != null) {
                if (info.getCallback() != null) {
                    try {
                        info.getCallback().postModify(context, name, entry, isCreate);
                    } catch (Exception e) {
                        // need to swallow all exceptions as postModify shouldn't throw any...
                        ZimbraLog.account.warn("postModify caught exception: "+e.getMessage(), e);
                    }
                }
            }
       }
    }
    
    private static Log mLog = LogFactory.getLog(AttributeManager.class);

    private static Options mOptions = new Options();
    
    static {
        mOptions.addOption("h", "help", false, "display this  usage info");
        mOptions.addOption("o", "output", true, "output file (default it to generate output to stdout)");
        mOptions.addOption("a", "action", true, "[generateLdapSchema | generateGlobalConfigLdif | generateDefaultCOSLdif]");
        mOptions.addOption("t", "template", true, "template for LDAP schema");
        Option iopt = new Option("i", "input", true,"attrs definition xml input file (can repeat)");
        iopt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(iopt);
    }
    
    private static void usage(String errmsg) {
        if (errmsg != null) { 
            mLog.error(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("AttributeManager [options] where [options] are one of:", mOptions);
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        StringBuffer gotCL = new StringBuffer("cmdline: ");
        for (int i = 0; i < args.length; i++) {
            gotCL.append("'").append(args[i]).append("' ");
        }
        //mLog.info(gotCL);
        
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        if (cl.hasOption('h')) {
            usage(null);
        }
        return cl;
    }

    private enum Action { generateLdapSchema, generateGlobalConfigLdif, generateDefaultCOSLdif, dump }
    
    public static void main(String[] args) throws IOException, DocumentException, ServiceException {
        Zimbra.toolSetup();
        CommandLine cl = parseArgs(args);

        if (!cl.hasOption('a')) usage("no action specified");
        String actionStr = cl.getOptionValue('a');
        Action action = null;
        try {
            action = Action.valueOf(actionStr);
        } catch (IllegalArgumentException iae) {
            usage("unknown action: " + actionStr);
        }

        AttributeManager am = null;
        if (action != Action.dump) {
            if (!cl.hasOption('i')) usage("no input attribute xml files specified");
            am = new AttributeManager(cl.getOptionValues('i'));
            if (am.hasErrors()) {
                ZimbraLog.misc.warn(am.getErrors());
                System.exit(1);
            }
        }
        
        OutputStream os = System.out;
        if (cl.hasOption('o')) {
            os = new FileOutputStream(cl.getOptionValue('o'));
        }
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "utf8")));
        
        switch (action) {
        case generateDefaultCOSLdif:
            am.generateDefaultCOSLdif(pw);
            break;
        case generateGlobalConfigLdif:
            am.generateGlobalConfigLdif(pw);
            break;
        case generateLdapSchema:
            if (!cl.hasOption('t')) {
                usage("no schema template specified");
            }
            am.generateLdapSchema(pw, cl.getOptionValue('t'));
            break;
        case dump:
            dumpSchema(pw);
            break;
        }

        pw.close();
    }

    private String[] getAllAttributesWithFlag(AttributeFlag flag) {
        List<String> attrs = new LinkedList<String>();
        for (AttributeInfo info : mAttrs.values()) {
            if (info.hasFlag(flag)) {
                attrs.add(info.getName());
            }
        }
        String[] result = attrs.toArray(new String[0]);
        Arrays.sort(result);
        return result;
    }
    
    private void generateGlobalConfigLdif(PrintWriter pw) {
        pw.println("# DO NOT MODIFY - generated by AttributeManager.");
        pw.println("# LDAP entry that contains initial default Zimbra global config.");
        pw.println("dn: cn=config,cn=zimbra");
        pw.println("objectclass: organizationalRole");
        pw.println("cn: config");
        pw.println("objectclass: zimbraGlobalConfig");
        
        String[] attrs;
        
        attrs = getAllAttributesWithFlag(AttributeFlag.accountInfo);
        for (String attr : attrs) {
            pw.println(Provisioning.A_zimbraAccountClientAttr + ": " + attr);
        }

        attrs = getAllAttributesWithFlag(AttributeFlag.domainAdminModifiable);
        for (String attr : attrs) {
            pw.println(Provisioning.A_zimbraDomainAdminModifiableAttr + ": " + attr);
        }

        attrs = getAllAttributesWithFlag(AttributeFlag.inheritFromCOS);
        for (String attr : attrs) {
            pw.println(Provisioning.A_zimbraCOSInheritedAttr + ": " + attr);
        }

        attrs = getAllAttributesWithFlag(AttributeFlag.inheritFromDomain);
        for (String attr : attrs) {
            pw.println(Provisioning.A_zimbraDomainInheritedAttr + ": " + attr);
        }

        attrs = getAllAttributesWithFlag(AttributeFlag.inheritFromGlobalConfig);
        for (String attr : attrs) {
            pw.println(Provisioning.A_zimbraServerInheritedAttr + ": " + attr);
        }
        
        List<String> out = new LinkedList<String>();
        for (AttributeInfo attr : mAttrs.values()) {
           List<String> gcv = attr.getGlobalConfigValues();
           if (gcv != null) {
               for (String v : gcv) {
                   out.add(attr.getName() + ": " + v);
               }
           }
        }
        String[] outs = out.toArray(new String[0]);
        Arrays.sort(outs);
        for (String o : outs) {
            pw.println(o);
        }
    }

    private void generateDefaultCOSLdif(PrintWriter pw) {
        pw.println("# DO NOT MODIFY - generated by AttributeManager.");
        pw.println("# LDAP entry for the default Zimbra COS.");
        pw.println("dn: cn=default,cn=cos,cn=zimbra");
        pw.println("cn: default");
        pw.println("objectclass: zimbraCOS");
        pw.println("zimbraId: e00428a1-0c00-11d9-836a-000d93afea2a");
        pw.println("description: The default COS");

        List<String> out = new LinkedList<String>();
        for (AttributeInfo attr : mAttrs.values()) {
           List<String> gcv = attr.getDefaultCosValues();
           if (gcv != null) {
               for (String v : gcv) {
                   out.add(attr.getName() + ": " + v);
               }
           }
        }
        String[] outs = out.toArray(new String[0]);
        Arrays.sort(outs);
        for (String o : outs) {
            pw.println(o);
        }
    }

    private void generateLdapSchema(PrintWriter pw, String schemaTemplateFile) throws IOException {
        byte[] templateBytes = ByteUtil.getContent(new File(schemaTemplateFile));
        String templateString = new String(templateBytes, "utf-8");
        
        StringBuilder GROUP_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_DEFINITIONS = new StringBuilder();
        
        for (Iterator<Integer> iter = mGroupMap.keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            //GROUP_OIDS
            GROUP_OIDS.append("objectIdentifier " + mGroupMap.get(i) + " ZimbraLDAP:" + i + "\n");

            // List all attrs which we define and which belong in this group
            List<AttributeInfo> list = new ArrayList<AttributeInfo>(mAttrs.size());
            for (AttributeInfo ai : mAttrs.values()) {
                if (ai.getId() > -1 && ai.getGroupId() == i) {
                    list.add(ai);
                }
            }
            
            // ATTRIBUTE_OIDS - sorted by OID
            Collections.sort(list, new Comparator<AttributeInfo>() {
                public int compare(AttributeInfo a1, AttributeInfo b1) {
                    return a1.getId() - b1.getId();
                }
            });
            for (AttributeInfo ai : list) {
                ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + mGroupMap.get(i) + ':' + ai.getId() + "\n");
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            Collections.sort(list, new Comparator<AttributeInfo>() {
                public int compare(AttributeInfo a1, AttributeInfo b1) {
                    return a1.getName().compareTo(b1.getName());
                }
            });
            for (AttributeInfo ai : list) {
                ATTRIBUTE_DEFINITIONS.append("attributetype ( " + ai.getName() + "\n");
                ATTRIBUTE_DEFINITIONS.append("\tNAME ( '" + ai.getName() + "' )\n");
                ATTRIBUTE_DEFINITIONS.append("\tDESC '" + ai.getDescription() + "'\n");
                String syntax = null, substr = null, equality = null, ordering = null; 
                switch (ai.getType()) {
                case TYPE_BOOLEAN:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.7";
                    equality = "booleanMatch";
                    break;
                case TYPE_DURATION:
                case TYPE_GENTIME:
                case TYPE_EMAIL:
                case TYPE_EMAILP:
                case TYPE_ENUM:
                case TYPE_ID:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.26{256}";
                    equality = "caseIgnoreIA5Match";
                    substr = "caseIgnoreSubstringsMatch";
                    break;
                        
                case TYPE_INTEGER:
                case TYPE_PORT:
                case TYPE_LONG:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.27";
                    equality = "integerMatch";
                    ordering = "integerOrderingMatch";
                    break;
                    
                case TYPE_STRING:
                case TYPE_REGEX:
                    String length = "";
                    if (ai.getMax() != Long.MAX_VALUE) {
                        length = "{" + ai.getMax() + "}";
                    }
                    syntax = "1.3.6.1.4.1.1466.115.121.1.15" + length;
                    equality = "caseIgnoreMatch";
                    substr = "caseIgnoreSubstringsMatch";
                }

                ATTRIBUTE_DEFINITIONS.append("\tSYNTAX " + syntax +  "\n");
                ATTRIBUTE_DEFINITIONS.append("\tEQUALITY " + equality);
                if (substr != null) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tSUBSTR " + substr);
                }
                if (ordering != null) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tORDERING " + ordering);
                }
                
                if (ai.getCardinality() == AttributeCardinality.single) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tSINGLE-VALUE");
                }
                
                ATTRIBUTE_DEFINITIONS.append(")\n\n");
            }
        }
        
        Map<String,String> templateFillers = new HashMap<String,String>();
        templateFillers.put("GROUP_OIDS", GROUP_OIDS.toString());
        templateFillers.put("ATTRIBUTE_OIDS", ATTRIBUTE_OIDS.toString());
        templateFillers.put("ATTRIBUTE_DEFINITIONS", ATTRIBUTE_DEFINITIONS.toString());
        
        for (AttributeClass cls : AttributeClass.values()) {
            String key = "CLASS_MEMBERS_" + cls.toString().toUpperCase();
            
            List<String> must = new LinkedList<String>();
            List<String> may = new LinkedList<String>();
            for (AttributeInfo ai : mAttrs.values()) {
                if (ai.requiredInClass(cls)) {
                    must.add(ai.getName());
                }
                if (ai.optionalInClass(cls)) {
                    may.add(ai.getName());
                }
            }
            Collections.sort(must);
            Collections.sort(may);
            
            StringBuilder value = new StringBuilder();
            if (!must.isEmpty()) {
                value.append("\tMUST (\n");
                Iterator<String> mustIter = must.iterator();
                while (true) {
                    value.append("\t\t").append(mustIter.next());
                    if (!mustIter.hasNext()) {
                        break;
                    }
                    value.append(" $\n");
                }
                value.append("\n\t)\n");
            }
            if (!may.isEmpty()) {
                value.append("\tMAY (\n");
                Iterator<String> mayIter = may.iterator();
                while (true) {
                    value.append("\t\t").append(mayIter.next());
                    if (!mayIter.hasNext()) {
                        break;
                    }
                    value.append(" $\n");
                }
                value.append("\n\t)\n");
            }
            value.append('\t');
            templateFillers.put(key, value.toString());
        }

        pw.print(StringUtil.fillTemplate(templateString, templateFillers));
    }
    
    private static String sortCSL(String in) {
        String[] ss = in.split("\\s*,\\s*");
        Arrays.sort(ss);
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < ss.length; i++) {
            sb.append(ss[i]);
            if (i < (ss.length-1)) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private static void dumpAttrs(PrintWriter pw, String name, Attributes attrs) throws NamingException {
        NamingEnumeration<javax.naming.directory.Attribute> attrIter = (NamingEnumeration<javax.naming.directory.Attribute>) attrs.getAll();
        List<javax.naming.directory.Attribute> attrsList = new LinkedList<javax.naming.directory.Attribute>();
        while (attrIter.hasMore()) {
            attrsList.add(attrIter.next());
        }
        Collections.sort(attrsList, new Comparator<javax.naming.directory.Attribute>() {
            public int compare(javax.naming.directory.Attribute a1, javax.naming.directory.Attribute b1) {
                return a1.getID().compareTo(b1.getID());
            }
        });
        for (javax.naming.directory.Attribute attr : attrsList) {
//            String s = attr.toString();
            NamingEnumeration valIter = attr.getAll();
            List<String> values = new LinkedList<String>();
            while (valIter.hasMore()) {
                values.add((String)valIter.next());
            }
            Collections.sort(values);
            for (String val : values) {
                pw.println(name + ": " + attr.getID() + ": " + val);
            }
        }
    }
    
    private static void dumpSchema(PrintWriter pw) throws ServiceException {
        DirContext dc = null;
        try {
          dc = LdapUtil.getDirContext(true);
          DirContext schema = dc.getSchema("");

          // Enumerate over ClassDefinition, AttributeDefinition, MatchingRule, SyntaxDefinition
          NamingEnumeration<NameClassPair> schemaTypeIter = schema.list("");
          while (schemaTypeIter.hasMore()) {
              String schemaType = schemaTypeIter.next().getName();
              NamingEnumeration<NameClassPair> schemaEntryIter = schema.list(schemaType);
              List<String> schemaEntries = new LinkedList<String>();
              while (schemaEntryIter.hasMore()) {
                  schemaEntries.add(schemaEntryIter.next().getName());
              }
              Collections.sort(schemaEntries);

              for (String schemaEntry : schemaEntries) {
                  DirContext sdc = (DirContext) schema.lookup(schemaType + "/" + schemaEntry);  
                  dumpAttrs(pw, schemaType + ": " + schemaEntry, sdc.getAttributes(""));
              }
          }

          dumpAttrs(pw, "GlobalConfig", dc.getAttributes("cn=config,cn=zimbra"));
          dumpAttrs(pw, "DefaultCOS", dc.getAttributes("cn=default,cn=cos,cn=zimbra"));
        } catch (NamingException ne) {
            ne.printStackTrace();
        } finally {
            LdapUtil.closeContext(dc);
        }
    }
}
