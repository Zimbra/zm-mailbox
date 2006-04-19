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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

public class AttributeManager {

    private static final String ZIMBRA_ATTRS_RESOURCE = "zimbraattrs.xml";

    private static final String E_ATTRS = "attrs";
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

    private Map<String,AttributeInfo> mAttrs = new HashMap<String,AttributeInfo>();

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
                    throw ServiceException.FAILURE("unable to parse attr file: "+file+" "+e.getMessage(), e);
                } catch (Exception e) {
                    // swallow all of them
                    throw ServiceException.FAILURE("unable to load attr file: "+file+" "+e.getMessage(), e);
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
    
    private void error(String file, String error) {
        mErrors.add(file + ": " + error);  
    }
    
    private void loadAttrs(InputStream attrsFile, String file) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(attrsFile);
        Element root = doc.getRootElement();
        if (!root.getName().equals(E_ATTRS)) {
            error(file, "root tag is not " + E_ATTRS);
            return;
        }
        
        NEXT_ATTR: for (Iterator iter = root.elementIterator(); iter.hasNext();) {
            Element eattr = (Element) iter.next();
            if (!eattr.getName().equals(E_ATTR)) {
                error(file, "unknown element: " + eattr.getName());
                continue;
            }

            String canonicalName = null;
            AttributeCallback callback = null;
            AttributeType type = null;
            String value = null;
            long min = Long.MIN_VALUE;
            long max = Long.MAX_VALUE;
            boolean immutable = false;
            boolean ignore = false;
            int id = -1;
            AttributeCardinality cardinality = null;
            Set<AttributeClass> requiredIn = null;
            Set<AttributeClass> optionalIn = null;
            Set<AttributeFlag> flags = null;
            String name = null;

            for (Iterator attrIter = eattr.attributeIterator(); attrIter.hasNext();) {
                Attribute attr = (Attribute) attrIter.next();
                String aname = attr.getName();
                if (aname.equals(A_NAME)) {
                    name = attr.getValue();
                    canonicalName = attr.getValue().toLowerCase();
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
                        error(file, "unknown <attr> type: "+attr.getValue());
                        continue NEXT_ATTR;
                    }
                } else if (aname.equals(A_VALUE)) { 
                    value = attr.getValue();
                } else if (aname.equals(A_ID)) {
                    try {
                        id = Integer.parseInt(attr.getValue());
                    } catch (NumberFormatException nfe) {
                        error(file, aname + " is not a number: " + attr.getValue());
                    }
                } else if (aname.equals(A_CARDINALITY)) {
                    try {
                        cardinality = AttributeCardinality.valueOf(attr.getValue());
                    } catch (IllegalArgumentException iae) {
                        error(file, aname + " is not valid: " + attr.getValue());
                    }
                } else if (aname.equals(A_REQUIRED_IN)) {
                    requiredIn = parseClasses(file, name, attr.getValue());
                } else if (aname.equals(A_OPTIONAL_IN)) {
                    optionalIn = parseClasses(file, name, attr.getValue());
                } else if (aname.equals(A_FLAGS)) {
                    flags = parseFlags(file, name, attr.getValue());
                } else {
                    error(file, "unknown <attr> attr: "+aname);
                }
            }

            if (name == null) {
                error(file, "no name specified for attr");
                continue;
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
                        error(file, "more than one description element for attr: " + name);
                    }
                    description = elem.getText();
                } else {
                    error(file, "unknown element: " + elem.getName() + " in attr: " + name);
                }
            }
                
            AttributeInfo info = new AttributeInfo(name, id, callback, type, value, immutable, min, max, cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues);
            if (mAttrs.get(canonicalName) != null) {
                error(file, "duplicate attr definiton: " + name);
            }
            mAttrs.put(canonicalName, info);
        }
    }
    
    private Set<AttributeClass> parseClasses(String file, String attr, String value) throws DocumentException {
        Set<AttributeClass> result = new HashSet<AttributeClass>();
        String[] cnames = value.split(",");
        for (String cname : cnames) {
            try {
                AttributeClass ac = AttributeClass.valueOf(cname);
                if (result.contains(ac)) {
                    error(file, attr + " duplicate class: " + cname);
                }
                result.add(ac);
            } catch (IllegalArgumentException iae) {
                error(file, attr + " invalid class: " + cname);
            }
        }
        return result;
    }

    private Set<AttributeFlag> parseFlags(String file, String attr, String value) throws DocumentException {
        Set<AttributeFlag> result = new HashSet<AttributeFlag>();
        String[] flags = value.split(",");
        for (String flag : flags) {
            try {
                AttributeFlag ac = AttributeFlag.valueOf(flag);
                if (result.contains(ac)) {
                    error(file, attr + " duplicate flag: " + flag);
                }
                result.add(ac);
            } catch (IllegalArgumentException iae) {
                error(file, attr + " invalid flag: " + flag);
            }
        }
        return result;
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

    public void preModify(Map attrs, Entry entry, Map context, boolean isCreate, boolean checkImmutable) throws ServiceException
    {
    	String[] keys = (String[]) attrs.keySet().toArray(new String[0]);
		for (int i = 0; i < keys.length; i++) {
		    String name = keys[i];
            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = (AttributeInfo) mAttrs.get(name.toLowerCase());
            if (info != null) {
                info.checkValue(value, checkImmutable);
                if (info.getCallback() != null)
                    info.getCallback().preModify(context, name, value, attrs, entry, isCreate);
            } else {
                ZimbraLog.misc.warn("checkValue: no attribute info for: "+name);
            }
		}
    }

    public void postModify(Map attrs, Entry entry, Map context, boolean isCreate)
    {
    	String[] keys = (String[]) attrs.keySet().toArray(new String[0]);
		for (int i = 0; i < keys.length; i++) {
			String name = keys[i];
            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = (AttributeInfo) mAttrs.get(name.toLowerCase());
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

    private enum Action { generateLdapSchema, generateGlobalConfigLdif, generateDefaultCOSLdif }
    
    public static void main(String[] args) throws IOException, DocumentException {
        Zimbra.toolSetup();
        CommandLine cl = parseArgs(args);

        if (!cl.hasOption('i')) usage("no input attribute xml files specified");
        if (!cl.hasOption('a')) usage("no action specified");
        
        AttributeManager am = new AttributeManager(cl.getOptionValues('i'));
        if (am.hasErrors()) {
            ZimbraLog.misc.warn(am.getErrors());
            System.exit(1);
        }
        
        OutputStream os = System.out;
        if (cl.hasOption('o')) {
            os = new FileOutputStream(cl.getOptionValue('o'));
        }
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "utf8")));

        String actionStr = cl.getOptionValue('a');
        Action action = null;
        try {
            action = Action.valueOf(actionStr);
        } catch (IllegalArgumentException iae) {
            usage("unknown action: " + actionStr);
        }

        switch (action) {
        case generateDefaultCOSLdif:
            am.generateDefaultCOSLdif(pw);
            break;
        case generateGlobalConfigLdif:
            am.generateGlobalConfigLdif(pw);
            break;
        case generateLdapSchema:
            am.generateLdapSchema(pw);
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
        String[] result = (String[])attrs.toArray(new String[0]);
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
        String[] outs = (String[])out.toArray(new String[0]);
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
        String[] outs = (String[])out.toArray(new String[0]);
        Arrays.sort(outs);
        for (String o : outs) {
            pw.println(o);
        }
    }

    private void generateLdapSchema(PrintWriter pw) {

    }
}
