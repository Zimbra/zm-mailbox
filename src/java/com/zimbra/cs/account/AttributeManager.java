/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.callback.IDNCallback;
import com.zimbra.cs.account.ldap.LdapUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.zimbra.common.util.*;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class AttributeManager {

    private static final String E_ATTRS = "attrs";
    private static final String A_GROUP = "group";
    private static final String A_GROUP_ID = "groupid";

    private static final String E_ATTR = "attr";
    private static final String A_NAME = "name";
    private static final String A_IMMUTABLE = "immutable";
    private static final String A_TYPE = "type";
    private static final String A_ORDER = "order";
    private static final String A_VALUE = "value";
    static final String A_MAX = "max";
    static final String A_MIN = "min";
    private static final String A_CALLBACK = "callback";
    private static final String A_ID = "id";
    private static final String A_PARENT_OID = "parentOid";
    private static final String A_CARDINALITY = "cardinality";
    private static final String A_REQUIRED_IN = "requiredIn";
    private static final String A_OPTIONAL_IN = "optionalIn";
    private static final String A_FLAGS = "flags";
    private static final String A_DEPRECATED_SINCE = "deprecatedSince";

    private static final String E_DESCRIPTION = "desc";
    private static final String E_DEPRECATE_DESC = "deprecateDesc";
    private static final String E_GLOBAL_CONFIG_VALUE = "globalConfigValue";
    private static final String E_DEFAULT_COS_VALUE = "defaultCOSValue";
    
    private static AttributeManager mInstance;

    private Map<String, AttributeInfo> mAttrs = new HashMap<String, AttributeInfo>();
    
    private AttributeCallback mIDNCallback = new IDNCallback();

    private static Map<Integer,String> mGroupMap = new HashMap<Integer,String>();

    public static AttributeManager getInstance() throws ServiceException {
        synchronized(AttributeManager.class) {
            if (mInstance != null) {
            	return mInstance;
            }
            String dir = LC.zimbra_attrs_directory.value();
            mInstance = new AttributeManager(dir);
            if (mInstance.hasErrors()) {
            	throw ServiceException.FAILURE(mInstance.getErrors(), null);
            }
            return mInstance;
        }
    }

    private AttributeManager(String dir) throws ServiceException {
    	initFlagsToAttrsMap();
        initClassToAttrsMap();        
    	File fdir = new File(dir);
    	if (!fdir.exists()) {
    		throw ServiceException.FAILURE("attrs directory does not exists: " + dir, null);
    	}
    	if (!fdir.isDirectory()) {
    		throw ServiceException.FAILURE("attrs directory is not a directory: " + dir, null);
    	}
    	
    	File[] files = fdir.listFiles();
    	for (File file : files) { 
    		if (!file.getPath().endsWith(".xml")) {
    			ZimbraLog.misc.warn("while loading attrs, ignoring not .xml file: " + file);
    			continue;
    		}
    		if (!file.isFile()) {
    			ZimbraLog.misc.warn("while loading attrs, ignored non-file: " + file);
    		}
    		try {
    			loadAttrs(file);
    		} catch (DocumentException de) {
    			throw ServiceException.FAILURE("error loading attrs file: " + file, de);
    		}
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
    
    private void error(String attrName, File file, String error) {
        if (attrName != null) {
            mErrors.add("attr " + attrName + " in file " + file + ": " + error);
        } else {
            mErrors.add("file " + file + ": " + error);
        }
    }
    
    private void loadAttrs(File file) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(file);
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
            AttributeOrder order = null;
            String value = null;
            String min = null;
            String max = null;
            boolean immutable = false;
//            boolean ignore = false;
            int id = -1;
            String parentOid = null;
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

            String deprecatedSince = null;
            
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
                    max = attr.getValue();
                } else if (aname.equals(A_MIN)) {
                    min = attr.getValue();
                } else if (aname.equals(A_TYPE)) {
                    type = AttributeType.getType(attr.getValue());
                    if (type == null) {
                        error(name, file, "unknown <attr> type: " + attr.getValue());
                        continue NEXT_ATTR;
                    }
                } else if (aname.equals(A_VALUE)) { 
                    value = attr.getValue();
                } else if (aname.equals(A_PARENT_OID)) {
                    parentOid = attr.getValue();
                    if (!parentOid.matches("^\\d+(\\.\\d+)+")) 
                        error(name, file, "invalid parent OID " + parentOid + ": must be an OID");
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
                } else if (aname.equals(A_ORDER)) {
                	try {
                		order = AttributeOrder.valueOf(attr.getValue());
                	} catch (IllegalArgumentException iae) {
                        error(name, file, aname + " is not valid: " + attr.getValue());
                	}
                } else if (aname.equals(A_DEPRECATED_SINCE)) {  
                    deprecatedSince = attr.getValue();
                } else {
                    error(name, file, "unknown <attr> attr: " + aname);
                }
            }

            List<String> globalConfigValues = new LinkedList<String>();
            List<String> defaultCOSValues = new LinkedList<String>();
            String description = null;
            String deprecateDesc = null;
            for (Iterator elemIter = eattr.elementIterator(); elemIter.hasNext();) {
                Element elem = (Element)elemIter.next();
                if (elem.getName().equals(E_GLOBAL_CONFIG_VALUE)) {
                    globalConfigValues.add(elem.getText());
                } else if (elem.getName().equals(E_DEFAULT_COS_VALUE)) {
                    defaultCOSValues.add(elem.getText());
                } else if (elem.getName().equals(E_DESCRIPTION)) {
                    if (description != null) {
                        error(name, file, "more than one " + E_DESCRIPTION);
                    }
                    description = elem.getText();
                } else if (elem.getName().equals(E_DEPRECATE_DESC)) {
                    if (deprecateDesc != null) {
                        error(name, file, "more than one " + E_DEPRECATE_DESC);
                    }
                    deprecateDesc = elem.getText();
                } else {
                    error(name, file, "unknown element: " + elem.getName());
                }
            }
            
            if (deprecatedSince != null && deprecateDesc == null)
                error(name, file, "missing attr " + A_DEPRECATED_SINCE);
            else if (deprecatedSince == null && deprecateDesc != null)
                error(name, file, "missing element " + E_DEPRECATE_DESC);
            
            if (deprecatedSince != null) {
                String deprecateInfo = "Deprecated since: " + deprecatedSince + ".  " + deprecateDesc; 
                if (description == null)
                    description = deprecateInfo;
                else
                    description = deprecateInfo + ".  Orig desc: " + description;
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
            checkFlag(name, file, flags, AttributeFlag.accountInherited, AttributeClass.account, AttributeClass.cos, requiredIn, optionalIn);

            // Check that if it is domain inheritable it is in domain and
            // account
            checkFlag(name, file, flags, AttributeFlag.domainInherited, AttributeClass.account, AttributeClass.domain, requiredIn, optionalIn);

            // Check that if it is domain inheritable it is in domain and
            // account
            checkFlag(name, file, flags, AttributeFlag.serverInherited, AttributeClass.server, AttributeClass.globalConfig, requiredIn, optionalIn);

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

            AttributeInfo info = new AttributeInfo(name, id, parentOid, groupId, callback, type, order, value, immutable, min, max, 
                                                   cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues, 
                                                   description);
            if (mAttrs.get(canonicalName) != null) {
                error(name, file, "duplicate definiton");
            }
            mAttrs.put(canonicalName, info);

            if (flags != null) {
            	for (AttributeFlag flag : flags) {
            		mFlagToAttrsMap.get(flag).add(name);
            	}
            }
            
            if (requiredIn != null || optionalIn != null) {
                if (requiredIn != null) {
                    for (AttributeClass klass : requiredIn) {
                        mClassToAttrsMap.get(klass).add(name);
                        mClassToLowerCaseAttrsMap.get(klass).add(name.toLowerCase());                        
                    }
                }
                if (optionalIn != null) {
                    for (AttributeClass klass : optionalIn) {
                        mClassToAttrsMap.get(klass).add(name);
                        mClassToLowerCaseAttrsMap.get(klass).add(name.toLowerCase());                        
                    }
                }
            }
        }
    }

    private Set<AttributeClass> parseClasses(String attrName, File file, String value) {
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

    private Set<AttributeFlag> parseFlags(String attrName, File file, String value) {
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

    private void checkFlag(String attrName, File file, Set<AttributeFlag> flags, AttributeFlag flag, AttributeClass c1, AttributeClass c2, Set<AttributeClass> required, Set<AttributeClass> optional) {
        if (flags != null && flags.contains(flag)) {
            boolean inC1 = (optional != null && optional.contains(c1)) || (required != null && required.contains(c1));
            boolean inC2 = (optional != null && optional.contains(c2)) || (required != null && required.contains(c2));
            if (!inC1 && !inC2) {
                error(attrName, file, "flag " + flag + " requires that attr be in both these classes " + c1 + " and " + c2);
            }
        }
    }

    /*
     * Support for lookup by class
     */
    
    private Map<AttributeClass, Set<String>> mClassToAttrsMap = new HashMap<AttributeClass, Set<String>>();
    private Map<AttributeClass, Set<String>> mClassToLowerCaseAttrsMap = new HashMap<AttributeClass, Set<String>>();

    private void initClassToAttrsMap() {
        for (AttributeClass klass : AttributeClass.values()) {
            mClassToAttrsMap.put(klass, new HashSet<String>());
            mClassToLowerCaseAttrsMap.put(klass, new HashSet<String>());
        }
    }

    /*
     * Support for lookup by flag
     */
    private Map<AttributeFlag, Set<String>> mFlagToAttrsMap = new HashMap<AttributeFlag, Set<String>>();

    private void initFlagsToAttrsMap() {
        for (AttributeFlag flag : AttributeFlag.values()) {
            mFlagToAttrsMap.put(flag, new HashSet<String>());
        }
    }

    public boolean isAccountInherited(String attr) {
 	   return mFlagToAttrsMap.get(AttributeFlag.accountInherited).contains(attr);
    }

    public boolean isDomainInherited(String attr) {
 	   return mFlagToAttrsMap.get(AttributeFlag.domainInherited).contains(attr);
    }

    public boolean isServerInherited(String attr) {
 	   return mFlagToAttrsMap.get(AttributeFlag.serverInherited).contains(attr);
    }

    public boolean isDomainAdminModifiable(String attr) {
 	   return mFlagToAttrsMap.get(AttributeFlag.domainAdminModifiable).contains(attr);
    }
    
    public void makeDomainAdminModifiable (String attr) {
    	mFlagToAttrsMap.get(AttributeFlag.domainAdminModifiable).add(attr);
    }
    
    public boolean isEmailOrIDN(String attr) {
        AttributeInfo ai = mAttrs.get(attr.toLowerCase());
        if (ai != null) {
            AttributeType at = ai.getType();
            return (at == AttributeType.TYPE_EMAIL || at == AttributeType.TYPE_EMAILP ||
                    mFlagToAttrsMap.get(AttributeFlag.idn).contains(attr));
        } else
            return false;
    }
    
    private boolean hasFlag(AttributeFlag flag, String attr) {
        return mFlagToAttrsMap.get(flag).contains(attr);
    }
    
    public Set<String> getAttrsWithFlag(AttributeFlag flag) {
 	   return mFlagToAttrsMap.get(flag);
    }
    
    public Set<String> getAttrsInClass(AttributeClass klass) {
        return mClassToAttrsMap.get(klass);
    }
    
    public Set<String> getLowerCaseAttrsInClass(AttributeClass klass) {
        return mClassToLowerCaseAttrsMap.get(klass);
    }

    public Set<String> getImmutableAttrs() {
        Set<String> immutable = new HashSet<String>();
        for (AttributeInfo info : mAttrs.values()) {
            if (info != null && info.isImmutable())
                immutable.add(info.getName());
        }
        return immutable;
    }

    public Set<String> getImmutableAttrsInClass(AttributeClass klass) {
        Set<String> immutable = new HashSet<String>();
        for (String attr : mClassToAttrsMap.get(klass)) {
            AttributeInfo info = mAttrs.get(attr.toLowerCase());
            if (info != null) {
                if (info.isImmutable())
                    immutable.add(attr);
            } else {
                ZimbraLog.misc.warn("getImmutableAttrsInClass: no attribute info for: " + attr);
            }
        }
        return immutable;
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

    public void preModify(Map<String, ? extends Object> attrs,
                          Entry entry,
                          Map context,
                          boolean isCreate,
                          boolean checkImmutable)    
    throws ServiceException {
        preModify(attrs, entry, context, isCreate, checkImmutable, true);
    }

    public void preModify(Map<String, ? extends Object> attrs,
                          Entry entry,
                          Map context,
                          boolean isCreate,
                          boolean checkImmutable,
                          boolean allowCallback)    
    throws ServiceException {
    	String[] keys = attrs.keySet().toArray(new String[0]);
		for (int i = 0; i < keys.length; i++) {
		    String name = keys[i];
		    if (name.length() == 0) {
		    	throw AccountServiceException.INVALID_ATTR_NAME("empty attr name found", null);
		    }
            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = mAttrs.get(name.toLowerCase());
            if (info != null && value != null) {
                // IDN unicode to ACE conversion needs to happen before checkValue or else 
                // regex attrs will be rejected by checkValue
                if (isEmailOrIDN(name)) {
                    mIDNCallback.preModify(context, name, value, attrs, entry, isCreate);
                    value = attrs.get(name);
                }
                info.checkValue(value, checkImmutable);
                if (allowCallback && info.getCallback() != null)
                    info.getCallback().preModify(context, name, value, attrs, entry, isCreate);
            } else {
                ZimbraLog.misc.warn("checkValue: no attribute info for: "+name);
            }
		}
    }

    public void postModify(Map<String, ? extends Object> attrs,
            Entry entry,
            Map context,
            boolean isCreate) {
        postModify(attrs, entry, context, isCreate, true);
    }

    public void postModify(Map<String, ? extends Object> attrs,
                           Entry entry,
                           Map context,
                           boolean isCreate,
                           boolean allowCallback) {
    	String[] keys = attrs.keySet().toArray(new String[0]);
		for (int i = 0; i < keys.length; i++) {
			String name = keys[i];
//            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = mAttrs.get(name.toLowerCase());
            if (info != null) {
                if (allowCallback && info.getCallback() != null) {
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
        
        /*
         * options for the listAttrs action
         */
        Option copt = new Option("c", "inclass", true, "list attrs in class  (can repeat)");
        copt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(copt);
        
        Option nopt = new Option("n", "notinclass", true, "not list attrs in class  (can repeat)");
        nopt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(nopt);
        
        Option fopt = new Option("f", "flags", true, "flags to print  (can repeat)");
        fopt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(fopt);
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

    private enum Action { generateLdapSchema, 
                          generateGlobalConfigLdif, 
                          generateDefaultCOSLdif, 
                          dump,
                          listAttrs}
    
    public static void main(String[] args) throws IOException, ServiceException {
        CliUtil.toolSetup();
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
        if (action != Action.dump && action != Action.listAttrs) {
            if (!cl.hasOption('i')) usage("no input attribute xml files specified");
            am = new AttributeManager(cl.getOptionValue('i'));
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
        case listAttrs:
            listAttrs(pw, cl.getOptionValues('c'), cl.getOptionValues('n'), cl.getOptionValues('f'));
            break;
        }

        pw.close();
    }

    private void generateGlobalConfigLdif(PrintWriter pw) {
        pw.println("# DO NOT MODIFY - generated by AttributeManager.");
        pw.println("# LDAP entry that contains initial default Zimbra global config.");
        pw.println("# " + CLOptions.buildVersion()); 
        
        String baseDn = CLOptions.getBaseDn("config");
        pw.println("dn: cn=config," + baseDn);
        pw.println("objectclass: organizationalRole");
        pw.println("cn: config");
        pw.println("objectclass: zimbraGlobalConfig");
        
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
        pw.println("# " + CLOptions.buildVersion()); 

        String baseDn = CLOptions.getBaseDn("cos");
        String cosName = CLOptions.getEntryName("cos", "default");
        String cosId = CLOptions.getEntryId("cos", "e00428a1-0c00-11d9-836a-000d93afea2a");
        
        pw.println("dn: cn=" + cosName +",cn=cos," + baseDn);
        pw.println("cn: " + cosName);
        pw.println("objectclass: zimbraCOS");
        pw.println("zimbraId: " + cosId);
        pw.println("description: The " + cosName + " COS");

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
                String parentOid = ai.getParentOid();
                if (parentOid == null)
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + mGroupMap.get(i) + ':' + ai.getId() + "\n");
                else 
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + parentOid + "." + ai.getId() + "\n");                    
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            Collections.sort(list, new Comparator<AttributeInfo>() {
                public int compare(AttributeInfo a1, AttributeInfo b1) {
                    return a1.getName().compareTo(b1.getName());
                }
            });
            
            String lengthSuffix;
            
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
                case TYPE_EMAIL:
                case TYPE_EMAILP:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.26{256}";
                    equality = "caseIgnoreIA5Match";
                    substr = "caseIgnoreSubstringsMatch";
                    break;

                case TYPE_GENTIME:
                	syntax = "1.3.6.1.4.1.1466.115.121.1.24";
                    equality = "generalizedTimeMatch";
                    ordering = "generalizedTimeOrderingMatch ";
                    break;

                case TYPE_ID:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.15{256}";
                    equality = "caseIgnoreMatch";
                    substr = "caseIgnoreSubstringsMatch";
                    break;

                case TYPE_DURATION:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.26{32}";
                    equality = "caseIgnoreIA5Match";
                    break;

                case TYPE_ENUM:
                    int maxLen = Math.max(32, ai.getEnumValueMaxLength());
                    syntax = "1.3.6.1.4.1.1466.115.121.1.15{" + maxLen + "}";
                    equality = "caseIgnoreMatch";
                    substr = "caseIgnoreSubstringsMatch";
                    break;
                        
                case TYPE_INTEGER:
                case TYPE_PORT:
                case TYPE_LONG:
                    syntax = "1.3.6.1.4.1.1466.115.121.1.27";
                    equality = "integerMatch";
                    break;
                    
                case TYPE_STRING:
                case TYPE_REGEX:
                    lengthSuffix = "";
                    if (ai.getMax() != Long.MAX_VALUE) {
                        lengthSuffix = "{" + ai.getMax() + "}";
                    }
                    syntax = "1.3.6.1.4.1.1466.115.121.1.15" + lengthSuffix;
                    equality = "caseIgnoreMatch";
                    substr = "caseIgnoreSubstringsMatch";
                    break;
                    
                case TYPE_ASTRING:
                    lengthSuffix = "";
                	if (ai.getMax() != Long.MAX_VALUE) {
                		lengthSuffix = "{" + ai.getMax() + "}";
                	}
                	syntax = "1.3.6.1.4.1.1466.115.121.1.26" + lengthSuffix;
                	equality = "caseIgnoreIA5Match";
                	substr = "caseIgnoreSubstringsMatch";
                	break;

                case TYPE_OSTRING:
                    lengthSuffix = "";
                	if (ai.getMax() != Long.MAX_VALUE) {
                		lengthSuffix = "{" + ai.getMax() + "}";
                	}
                	syntax = "1.3.6.1.4.1.1466.115.121.1.40" + lengthSuffix;
                	equality = "octetStringMatch";
                	break;

                case TYPE_CSTRING:
                    lengthSuffix = "";
                	if (ai.getMax() != Long.MAX_VALUE) {
                		lengthSuffix = "{" + ai.getMax() + "}";
                	}
                	syntax = "1.3.6.1.4.1.1466.115.121.1.15" + lengthSuffix;
                	equality = "caseExactMatch";
                	substr = "caseExactSubstringsMatch";
                	break;
                	
                case TYPE_PHONE:
                    lengthSuffix = "";
                	if (ai.getMax() != Long.MAX_VALUE) {
                		lengthSuffix = "{" + ai.getMax() + "}";
                	}
                	syntax = "1.3.6.1.4.1.1466.115.121.1.50" + lengthSuffix;
                	equality = "telephoneNumberMatch";
                	substr = "telephoneNumberSubstringsMatch";
                	break;
                	
               	default:
               		throw new RuntimeException("unknown type encountered!");
                }

                ATTRIBUTE_DEFINITIONS.append("\tSYNTAX " + syntax +  "\n");
                ATTRIBUTE_DEFINITIONS.append("\tEQUALITY " + equality);
                if (substr != null) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tSUBSTR " + substr);
                }
                
                if (ordering != null) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tORDERING " + ordering);
                } else if (ai.getOrder() != null) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tORDERING " + ai.getOrder());
                }
                
                if (ai.getCardinality() == AttributeCardinality.single) {
                    ATTRIBUTE_DEFINITIONS.append("\n\tSINGLE-VALUE");
                }
                
                ATTRIBUTE_DEFINITIONS.append(")\n\n");
            }
        }
        
        Map<String,String> templateFillers = new HashMap<String,String>();
        templateFillers.put("SCHEMA_VERSION_INFO", CLOptions.buildVersion());
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
    
    private static void listAttrs(PrintWriter pw, String[] inClass, String[] notInClass, String[] printFlags) throws ServiceException {
        AttributeManager am = AttributeManager.getInstance();
        
        if (inClass == null)
            usage("no class specified");
        
        Set<String> attrsInClass = new HashSet<String>();
        for (String c : inClass) {
            AttributeClass ac = AttributeClass.valueOf(c);
            SetUtil.union(attrsInClass, am.getAttrsInClass(ac));
        }
        
        Set<String> attrsNotInClass = new HashSet<String>();
        if (notInClass != null) {
            for (String c : notInClass) {
                AttributeClass ac = AttributeClass.valueOf(c);
                SetUtil.union(attrsNotInClass, am.getAttrsInClass(ac));
            }
        }
        
        attrsInClass = SetUtil.subtract(attrsInClass, attrsNotInClass);
        
        List<String> list = new ArrayList<String>(attrsInClass);
        Collections.sort(list);
        
        for (String a : list) {
            StringBuffer flags = new StringBuffer();
            if (printFlags != null) {
                for (String f : printFlags) {
                    AttributeFlag af = AttributeFlag.valueOf(f);
                    if (am.hasFlag(af, a)) {
                        if (flags.length()>0)
                            flags.append(", ");
                        flags.append(af.name());
                    }
                }
                
                if (flags.length() > 0) {
                    flags.insert(0, "(").append(")");
                }
            }
            System.out.println(a + " " + flags);
        }
            
    }
    
    static class CLOptions { 
        
        private static String get(String key, String defaultValue) {
            String value = System.getProperty(key);
            if (value == null)
                return defaultValue;
            else
                return value;
        }
        
        public static String buildVersion() {
            return get("zimbra.version", "unknown");
        }
    
        public static String getBaseDn(String entry) {
            return get(entry + ".basedn", "cn=zimbra");
        }
    
        public static String getEntryName(String entry, String defaultValue) {
            return get(entry + ".name", defaultValue);
        }
        
        public static String getEntryId(String entry, String defaultValue) {
            return get(entry + ".id", defaultValue);
        }
    }
}
