/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.callback.IDNCallback;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.util.BuildInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
    private static final String E_OBJECTCLASSES = "objectclasses";
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
    private static final String A_SINCE = "since";
    private static final String A_REQUIRES_RESTART = "requiresRestart";
    
    private static final String E_OBJECTCLASS = "objectclass";
    private static final String E_SUP = "sup";
    private static final String E_COMMENT = "comment";

    private static final String E_DESCRIPTION = "desc";
    private static final String E_DEPRECATE_DESC = "deprecateDesc";
    private static final String E_GLOBAL_CONFIG_VALUE = "globalConfigValue";
    private static final String E_GLOBAL_CONFIG_VALUE_UPGRADE = "globalConfigValueUpgrade";
    private static final String E_DEFAULT_COS_VALUE = "defaultCOSValue";
    private static final String E_DEFAULT_COS_VALUE_UPGRADE = "defaultCOSValueUpgrade";
    
    // multi-line continuation prefix chars
    private static final String ML_CONT_PREFIX = "  ";
    
    private static AttributeManager mInstance;

    // contains attrs defined in one of the zimbra .xml files (currently zimbra attrs and some of the amavis attrs) 
    // these attrs have AttributeInfo
    // 
    // Note: does *not* contains attrs defiend in the extensions(attrs in OCs specified in global config ***ExtraObjectClass)
    //
    // Extension attr names are in the class -> attrs maps:
    //     mClassToAttrsMap, mClassToLowerCaseAttrsMap, mClassToAllAttrsMap maps.
    //
    private Map<String, AttributeInfo> mAttrs = new HashMap<String, AttributeInfo>();
    
    private Map<String, ObjectClassInfo> mOCs = new HashMap<String, ObjectClassInfo>();
    
    // only direct attrs
    private Map<AttributeClass, Set<String>> mClassToAttrsMap = new HashMap<AttributeClass, Set<String>>();
    private Map<AttributeClass, Set<String>> mClassToLowerCaseAttrsMap = new HashMap<AttributeClass, Set<String>>();
    
    // direct attrs and attrs from included objectClass's
    private Map<AttributeClass, Set<String>> mClassToAllAttrsMap = new HashMap<AttributeClass, Set<String>>();
    
    private boolean mLdapSchemaExtensionInited = false;
    
    private AttributeCallback mIDNCallback = new IDNCallback();

    private static Map<Integer,String> mGroupMap = new HashMap<Integer,String>();

    private static Map<Integer,String> mOCGroupMap = new HashMap<Integer,String>();
    
    // do not keep comments and descriptions when running in a server
    private static boolean mMinimize = false;
    
    public static AttributeManager getInstance() throws ServiceException {
        synchronized(AttributeManager.class) {
            if (mInstance != null) {
            	return mInstance;
            }
            String dir = LC.zimbra_attrs_directory.value();
            String className = LC.zimbra_class_attrmanager.value();
            if (className != null && !className.equals("")) {
                try {
                    try {
                        mInstance = (AttributeManager) Class.forName(className).getDeclaredConstructor(String.class).newInstance(dir);
                    } catch (ClassNotFoundException cnfe) {
                        // ignore and look in extensions
                        mInstance = (AttributeManager) ExtensionUtil.findClass(className).getDeclaredConstructor(String.class).newInstance(dir);
                    }
                } catch (Exception e) {
                    ZimbraLog.account.debug("could not instantiate AttributeManager interface of class '" + className + "'; defaulting to AttributeManager");
                }
            }
            if (mInstance == null) {
                mInstance = new AttributeManager(dir);
            }
            if (mInstance.hasErrors()) {
            	throw ServiceException.FAILURE(mInstance.getErrors(), null);
            }
            
            mInstance.computeClassToAllAttrsMap();
            
            return mInstance;
        }
    }

    public AttributeManager(String dir) throws ServiceException {
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
    		    SAXReader reader = new SAXReader();
    		    Document doc = reader.read(file);
    		    Element root = doc.getRootElement();
    		    if (root.getName().equals(E_ATTRS)) 
    		        loadAttrs(file);
    		    else if (root.getName().equals(E_OBJECTCLASSES)) 
    		        loadObjectClasses(file);
    		    else
    		        ZimbraLog.misc.warn("while loading attrs, ignored unknown file: " + file);
    			
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
            
            List<AttributeServerType> requiresRestart = null;
            BuildInfo.Version deprecatedSinceVer = null;
            BuildInfo.Version sinceVer = null;

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
                } else if (aname.equals(A_REQUIRES_RESTART)) {  
                    requiresRestart = parseRequiresRestart(name, file, attr.getValue());
                    
                } else if (aname.equals(A_DEPRECATED_SINCE)) {  
                    String depreSince = attr.getValue();
                    if (depreSince != null) {
                        try {
                            deprecatedSinceVer = new BuildInfo.Version(depreSince);
                        } catch (ServiceException e) {
                            error(name, file, aname + " is not valid: " + attr.getValue() + " (" + e.getMessage() + ")");
                        }
                    }
                    
                } else if (aname.equals(A_SINCE)) {  
                    String since = attr.getValue();
                    if (since != null) {
                        try {
                            sinceVer = new BuildInfo.Version(since);
                        } catch (ServiceException e) {
                            error(name, file, aname + " is not valid: " + attr.getValue() + " (" + e.getMessage() + ")");
                        }
                    }
                        
                } else {
                    error(name, file, "unknown <attr> attr: " + aname);
                }
            }

            List<String> globalConfigValues = new LinkedList<String>();
            List<String> globalConfigValuesUpgrade = null; // note: init to null instead of empty List
            List<String> defaultCOSValues = new LinkedList<String>();
            List<String> defaultCOSValuesUpgrade = null;   // note: init to null instead of empty List
            String description = null;
            String deprecateDesc = null;
            
            for (Iterator elemIter = eattr.elementIterator(); elemIter.hasNext();) {
                Element elem = (Element)elemIter.next();
                if (elem.getName().equals(E_GLOBAL_CONFIG_VALUE)) {
                    globalConfigValues.add(elem.getText());
                } else if (elem.getName().equals(E_GLOBAL_CONFIG_VALUE_UPGRADE)) {
                    if (globalConfigValuesUpgrade == null)
                        globalConfigValuesUpgrade = new LinkedList<String>();
                    globalConfigValuesUpgrade.add(elem.getText());
                } else if (elem.getName().equals(E_DEFAULT_COS_VALUE)) {
                    defaultCOSValues.add(elem.getText());
                } else if (elem.getName().equals(E_DEFAULT_COS_VALUE_UPGRADE)) {
                    if (defaultCOSValuesUpgrade == null)
                        defaultCOSValuesUpgrade = new LinkedList<String>();
                    defaultCOSValuesUpgrade.add(elem.getText());
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
            
            if (deprecatedSinceVer != null && deprecateDesc == null)
                error(name, file, "missing attr " + A_DEPRECATED_SINCE);
            else if (deprecatedSinceVer == null && deprecateDesc != null)
                error(name, file, "missing element " + E_DEPRECATE_DESC);
            
            if (deprecatedSinceVer != null) {
                String deprecateInfo = "Deprecated since: " + deprecatedSinceVer.toString() + ".  " + deprecateDesc; 
                if (description == null)
                    description = deprecateInfo;
                else
                    description = deprecateInfo + ".  Orig desc: " + description;
            }
            
            // since is required after(inclusive) oid 525 - first attribute in 5.0
            if (sinceVer == null && id >= 525) {
                error(name, file, "missing since (required after(inclusive) oid 710)");
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

            // Check that if it is COS inheritable it is in account and COS classes
            checkFlag(name, file, flags, AttributeFlag.accountInherited, AttributeClass.account, AttributeClass.cos, null, requiredIn, optionalIn);

            // Check that if it is COS-domain inheritable it is in account and COS and domain classes
            checkFlag(name, file, flags, AttributeFlag.accountCosDomainInherited, AttributeClass.account, AttributeClass.cos, AttributeClass.domain, requiredIn, optionalIn);
            
            // Check that if it is domain inheritable it is in domain and global config
            checkFlag(name, file, flags, AttributeFlag.domainInherited, AttributeClass.domain, AttributeClass.globalConfig, null, requiredIn, optionalIn);

            // Check that if it is server inheritable it is in server and global config
            checkFlag(name, file, flags, AttributeFlag.serverInherited, AttributeClass.server, AttributeClass.globalConfig, null, requiredIn, optionalIn);

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

            AttributeInfo info = createAttributeInfo(
                    name, id, parentOid, groupId, callback, type, order, value, immutable, min, max,
                    cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues,
                    globalConfigValuesUpgrade, defaultCOSValuesUpgrade,
                    mMinimize ? null : description, requiresRestart, sinceVer, deprecatedSinceVer);
            
            if (mAttrs.get(canonicalName) != null) {
                error(name, file, "duplicate definiton");
            }
            mAttrs.put(canonicalName, info);

            if (flags != null) {
            	for (AttributeFlag flag : flags) {
            		mFlagToAttrsMap.get(flag).add(name);
            		if (flag == AttributeFlag.accountCosDomainInherited)
            		    mFlagToAttrsMap.get(AttributeFlag.accountInherited).add(name);
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

    protected AttributeInfo createAttributeInfo(String name, int id, String parentOid, int groupId,
                                                AttributeCallback callback, AttributeType type, AttributeOrder order,
                                                String value, boolean immutable, String min, String max,
                                                AttributeCardinality cardinality, Set<AttributeClass> requiredIn,
                                                Set<AttributeClass> optionalIn, Set<AttributeFlag> flags,
                                                List<String> globalConfigValues, List<String> defaultCOSValues,
                                                List<String> globalConfigValuesUpgrade, List<String> defaultCOSValuesUpgrade,
                                                String description, List<AttributeServerType> requiresRestart,
                                                BuildInfo.Version sinceVer, BuildInfo.Version deprecatedSinceVer) {
        return new AttributeInfo(
                name, id, parentOid, groupId, callback, type, order, value, immutable, min, max,
                cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues,
                globalConfigValuesUpgrade, defaultCOSValuesUpgrade,
                description, requiresRestart, sinceVer, deprecatedSinceVer);
    }

    private enum ObjectClassType {
        ABSTRACT,
        AUXILIARY,
        STRUCTURAL;
    }

    private class ObjectClassInfo {
        private AttributeClass mAttributeClass;
        private String mName;
        private int mId;
        private int mGroupId;
        private ObjectClassType mType;
        private List<String> mSuperOCs;
        private String mDescription;
        private List<String> mComment;

        // there must be a one-to-one mapping between enums in AttributeClass and ocs defined in the xml


        ObjectClassInfo(AttributeClass attrClass, String ocName, int id, int groupId, ObjectClassType type,
                        List<String> superOCs, String description, List<String> comment) {
            mAttributeClass = attrClass;
            mName = ocName;
            mId = id;
            mGroupId = groupId;
            mType = type;
            mSuperOCs = superOCs;
            mDescription = description;
            mComment = comment;
        }

        AttributeClass getAttributeClass() {
            return mAttributeClass;
        }

        String getName() {
            return mName;
        }

        int getId() {
            return mId;
        }

        int getGroupId() {
            return mGroupId;
        }

        ObjectClassType getType() {
            return mType;
        }

        List<String> getSuperOCs() {
            return mSuperOCs;
        }

        String getDescription() {
            return mDescription;
        }

        List<String> getComment() {
            return mComment;
        }

    }

    private void loadObjectClasses(File file) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(file);
        Element root = doc.getRootElement();

        if (!root.getName().equals(E_OBJECTCLASSES)) {
            error(null, file, "root tag is not " + E_OBJECTCLASSES);
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
        if (groupId == 1) {
            error(null, file, A_GROUP_ID + " is not valid (used by ZimbraAttrType)");
        } else if (groupId > 0) {
            if (mOCGroupMap.containsKey(groupId)) {
                error(null, file, "duplicate group id: " + groupId);
            } else if (mOCGroupMap.containsValue(group)) {
                error(null, file, "duplicate group: " + group);
            } else {
                mOCGroupMap.put(groupId, group);
            }
        }

        for (Iterator iter = root.elementIterator(); iter.hasNext();) {
            Element eattr = (Element) iter.next();
            if (!eattr.getName().equals(E_OBJECTCLASS)) {
                error(null, file, "unknown element: " + eattr.getName());
                continue;
            }

            int id = -1;
            ObjectClassType type = null;
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
                } else if (aname.equals(A_TYPE)) {
                    type = ObjectClassType.valueOf(attr.getValue());
                } else if (aname.equals(A_ID)) {
                    try {
                        id = Integer.parseInt(attr.getValue());
                        if (id < 0)  {
                            error(name, file, "invalid id " + id + ": must be positive");
                        }
                    } catch (NumberFormatException nfe) {
                        error(name, file, aname + " is not a number: " + attr.getValue());
                    }
                } else {
                    error(name, file, "unknown <attr> attr: " + aname);
                }
            }

            List<String> superOCs = new LinkedList<String>();
            String description = null;
            List<String> comment = null;
            for (Iterator elemIter = eattr.elementIterator(); elemIter.hasNext();) {
                Element elem = (Element)elemIter.next();
                if (elem.getName().equals(E_SUP)) {
                    superOCs.add(elem.getText());
                } else if (elem.getName().equals(E_DESCRIPTION)) {
                    if (description != null) {
                        error(name, file, "more than one " + E_DESCRIPTION);
                    }
                    description = elem.getText();
                } else if (elem.getName().equals(E_COMMENT)) {
                    if (comment != null) {
                        error(name, file, "more than one " + E_COMMENT);
                    }
                    comment = new ArrayList<String>();
                    String[] lines = elem.getText().trim().split("\\n");
                    for (String line : lines)
                        comment.add(line.trim());
                } else {
                    error(name, file, "unknown element: " + elem.getName());
                }
            }

            // Check that if all bits are specified
            if (id <= 0) {
                error(name, file, "id not specified");
            }

            if (type == null) {
                error(name, file, "type not specified");
            }

            if (description == null) {
                error(name, file, "desc not specified");
            }

            if (superOCs.isEmpty()) {
                error(name, file, "sup not specified");
            }

            // there must be a one-to-one mapping between enums in AttributeClass and ocs defined in the xml
            AttributeClass attrClass = AttributeClass.getAttributeClass(name);
            if (attrClass == null) {
                error(name, file, "unknown class in AttributeClass: " + name);
            }

            ObjectClassInfo info = new ObjectClassInfo(attrClass, name, id, groupId, type, superOCs,
                mMinimize ? null : description, mMinimize ? null : comment);
            if (mOCs.get(canonicalName) != null) {
                error(name, file, "duplicate objectclass definiton");
            }
            mOCs.put(canonicalName, info);

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

    private void checkFlag(String attrName, File file, Set<AttributeFlag> flags, AttributeFlag flag,
                           AttributeClass c1, AttributeClass c2, AttributeClass c3,
                           Set<AttributeClass> required, Set<AttributeClass> optional) {

        if (flags != null && flags.contains(flag)) {
            boolean inC1 = (optional != null && optional.contains(c1)) || (required != null && required.contains(c1));
            boolean inC2 = (optional != null && optional.contains(c2)) || (required != null && required.contains(c2));
            boolean inC3 = (c3==null)? true : (optional != null && optional.contains(c3)) || (required != null && required.contains(c3));
            if (!(inC1 && inC2 && inC3)) {
                String classes = c1 + " and " + c2 + (c3==null?"":" and " + c3);
                error(attrName, file, "flag " + flag + " requires that attr be in all these classes: " + classes);
            }
        }
    }
    
    private List<AttributeServerType> parseRequiresRestart(String attrName, File file, String value) {
        List<AttributeServerType> result = new ArrayList<AttributeServerType>();
        String[] serverTypes = value.split(",");
        for (String server : serverTypes) {
            try {
                AttributeServerType ast = AttributeServerType.valueOf(server);
                if (result.contains(ast)) {
                    error(attrName, file, "duplicate server type: " + server);
                }
                result.add(ast);
            } catch (IllegalArgumentException iae) {
                error(attrName, file, "invalid server type: " + server);
            }
        }
        return result;
    }

    /*
     * Support for lookup by class
     */
    
    private void initClassToAttrsMap() {
        for (AttributeClass klass : AttributeClass.values()) {
            mClassToAttrsMap.put(klass, new HashSet<String>());
            mClassToLowerCaseAttrsMap.put(klass, new HashSet<String>());
        }
    }
    
    private void computeClassToAllAttrsMap() {
        
        Set<String> attrs;
        
        for (AttributeClass klass : mClassToAttrsMap.keySet()) {
            
            switch (klass) {
            case account:
                attrs = SetUtil.union(new HashSet<String>(),
                                      mClassToAttrsMap.get(AttributeClass.mailRecipient), 
                                      mClassToAttrsMap.get(AttributeClass.account));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case calendarResource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.mailRecipient), 
                        mClassToAttrsMap.get(AttributeClass.account));
                attrs = SetUtil.union(attrs,
                                      mClassToAttrsMap.get(AttributeClass.calendarResource));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case distributionList:
                attrs = SetUtil.union(new HashSet<String>(),
                                      mClassToAttrsMap.get(AttributeClass.mailRecipient), 
                                      mClassToAttrsMap.get(AttributeClass.distributionList));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case imapDataSource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.dataSource), 
                        mClassToAttrsMap.get(AttributeClass.imapDataSource));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case pop3DataSource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.dataSource), 
                        mClassToAttrsMap.get(AttributeClass.pop3DataSource));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case rssDataSource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.dataSource), 
                        mClassToAttrsMap.get(AttributeClass.rssDataSource));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case liveDataSource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.dataSource), 
                        mClassToAttrsMap.get(AttributeClass.liveDataSource));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case galDataSource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.dataSource), 
                        mClassToAttrsMap.get(AttributeClass.galDataSource));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            case domain:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.mailRecipient), 
                        mClassToAttrsMap.get(AttributeClass.domain));
                mClassToAllAttrsMap.put(klass, attrs);
                break;
            default:
                mClassToAllAttrsMap.put(klass, mClassToAttrsMap.get(klass));
            }
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

    public boolean isAccountCosDomainInherited(String attr) {
        return mFlagToAttrsMap.get(AttributeFlag.accountCosDomainInherited).contains(attr);
     }

    public boolean isDomainInherited(String attr) {
 	   return mFlagToAttrsMap.get(AttributeFlag.domainInherited).contains(attr);
    }

    public boolean isServerInherited(String attr) {
 	   return mFlagToAttrsMap.get(AttributeFlag.serverInherited).contains(attr);
    }

    public boolean isDomainAdminModifiable(String attr, AttributeClass klass) throws ServiceException {
        // bug 32507
        if (!mClassToAllAttrsMap.get(klass).contains(attr))    
            throw AccountServiceException.INVALID_ATTR_NAME("unknown attribute on " + klass.name() + ": " + attr, null);

        return mFlagToAttrsMap.get(AttributeFlag.domainAdminModifiable).contains(attr);
    }

    public void makeDomainAdminModifiable(String attr) {
    	mFlagToAttrsMap.get(AttributeFlag.domainAdminModifiable).add(attr);
    }

    public static enum IDNType {
        email,     // attr type is email
        emailp,    // attr type is emailp
        cs_emailp, // attr type is cs_emailp
        idn,       // attr has idn flag
        none;      // attr is not of type smail, emailp, cs_emailp, nor does it has idn flag
        
        public boolean isEmailOrIDN() {
            return this != none;
        }
    }
    
    public static IDNType idnType(AttributeManager am, String attr) {
        if (am == null)
            return IDNType.none;
        else
            return am.idnType(attr);
    }
    
    private IDNType idnType(String attr) {
        AttributeInfo ai = mAttrs.get(attr.toLowerCase());
        if (ai != null) {
            AttributeType at = ai.getType();
            if (at == AttributeType.TYPE_EMAIL)
                return IDNType.email;
            else if (at == AttributeType.TYPE_EMAILP)
                return IDNType.emailp;
            else if (at == AttributeType.TYPE_CS_EMAILP)
                return IDNType.cs_emailp;
            else if (mFlagToAttrsMap.get(AttributeFlag.idn).contains(attr))
                return IDNType.idn;
        }

        return IDNType.none;
    }

    public boolean inVersion(String attr, String version) throws ServiceException {
    	AttributeInfo ai = mAttrs.get(attr.toLowerCase());
    	if (ai != null) {
    	    BuildInfo.Version since = ai.getSince();
    	    if (since == null)
    	        return true;
    	    else
    		    return since.compare(version) <= 0;
    	} else
    	    throw AccountServiceException.INVALID_ATTR_NAME("unknown attribute: " + attr, null);
    }
    
    public AttributeType getAttributeType(String attr) throws ServiceException {
        AttributeInfo ai = mAttrs.get(attr.toLowerCase());
        if (ai != null)
            return ai.getType();
        else
            throw AccountServiceException.INVALID_ATTR_NAME("unknown attribute: " + attr, null);
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
    
    public Set<String> getAllAttrsInClass(AttributeClass klass) {
        return mClassToAllAttrsMap.get(klass);
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

    public static void setMinimize(boolean minimize) { mMinimize = minimize; }
    
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
            if (info != null) {
                // IDN unicode to ACE conversion needs to happen before checkValue or else
                // regex attrs will be rejected by checkValue
                if (idnType(name).isEmailOrIDN()) {
                    mIDNCallback.preModify(context, name, value, attrs, entry, isCreate);
                    value = attrs.get(name);
                }
                info.checkValue(value, checkImmutable, attrs);
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
        mOptions.addOption("a", "action", true, "[generateLdapSchema | generateGlobalConfigLdif | generateDefaultCOSLdif | generateSchemaLdif]");
        mOptions.addOption("t", "template", true, "template for LDAP schema");
        mOptions.addOption("r", "regenerateFile", true, "Java file to regenerate");


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
                          generateSchemaLdif,
                          generateGlobalConfigLdif,
                          generateDefaultCOSLdif,
                          dump,
                          generateProvisioning,
                          generateGetters,
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
        case generateSchemaLdif:
            am.generateSchemaLdif(pw);
            break;
        case dump:
            dumpSchema(pw);
            break;
        case listAttrs:
            listAttrs(pw, cl.getOptionValues('c'), cl.getOptionValues('n'), cl.getOptionValues('f'));
            break;
        case generateGetters:
            am.generateGetters(cl.getOptionValue('c'), cl.getOptionValue('r'));
            break;
        case generateProvisioning:
            am.generateProvisioningConstants(cl.getOptionValue('r'));
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

    private void buildSchemaBanner(StringBuilder BANNER) {

        BANNER.append("#\n");
        BANNER.append("# Zimbra LDAP Schema\n");
        BANNER.append("#\n");
        BANNER.append("# DO NOT MODIFY - generated by AttributeManager.\n");
        BANNER.append("#\n");
        BANNER.append("# " + CLOptions.buildVersion() + "\n");
        BANNER.append("#\n");
        BANNER.append("# our root OID (http://www.iana.org/assignments/enterprise-numbers)\n");
        BANNER.append("#\n");
        BANNER.append("#  1.3.6.1.4.1.19348\n");
        BANNER.append("#  1.3.6.1.4.1.19348.2      LDAP elements\n");
        BANNER.append("#  1.3.6.1.4.1.19348.2.1    Attribute Types\n");
        BANNER.append("#  1.3.6.1.4.1.19348.2.2    Object Classes\n");
        BANNER.append("#");
    }

    private void buildAttrDef(StringBuilder ATTRIBUTE_DEFINITIONS, AttributeInfo ai) {
        String lengthSuffix;

        ATTRIBUTE_DEFINITIONS.append("( " + ai.getName() + "\n");
        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "NAME ( '" + ai.getName() + "' )\n");
        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "DESC '" + ai.getDescription() + "'\n");
        String syntax = null, substr = null, equality = null, ordering = null;
        switch (ai.getType()) {
        case TYPE_BOOLEAN:
            syntax = "1.3.6.1.4.1.1466.115.121.1.7";
            equality = "booleanMatch";
            break;
        case TYPE_EMAIL:
        case TYPE_EMAILP:
        case TYPE_CS_EMAILP:
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

        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SYNTAX " + syntax +  "\n");
        ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "EQUALITY " + equality);
        if (substr != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "SUBSTR " + substr);
        }

        if (ordering != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "ORDERING " + ordering);
        } else if (ai.getOrder() != null) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "ORDERING " + ai.getOrder());
        }

        if (ai.getCardinality() == AttributeCardinality.single) {
            ATTRIBUTE_DEFINITIONS.append("\n" + ML_CONT_PREFIX + "SINGLE-VALUE");
        }

        ATTRIBUTE_DEFINITIONS.append(")");
    }

    private void buildZimbraRootOIDs(StringBuilder ZIMBRA_ROOT_OIDS, String prefix) {
        ZIMBRA_ROOT_OIDS.append(prefix + "ZimbraRoot 1.3.6.1.4.1.19348\n");
        ZIMBRA_ROOT_OIDS.append(prefix + "ZimbraLDAP ZimbraRoot:2\n");
    }

    private void buildObjectClassOIDs(StringBuilder OC_GROUP_OIDS, StringBuilder OC_OIDS, String prefix) {
        for (Iterator<Integer> iter = mOCGroupMap.keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            // OC_GROUP_OIDS
            OC_GROUP_OIDS.append(prefix + mOCGroupMap.get(i) + " ZimbraLDAP:" + i + "\n");

            // List all ocs which we define and which belong in this group
            List<ObjectClassInfo> list = getOCList(i);

            // OC_OIDS - sorted by OID
            sortOCsByOID(list);

            for (ObjectClassInfo oci : list) {
                OC_OIDS.append(prefix + oci.getName() + " " + mOCGroupMap.get(i) + ':' + oci.getId() + "\n");
            }
        }
    }

    /**
     *
     * @param OC_DEFINITIONS
     * @param prefix
     * @param blankLineSeperator whether to seperate each OC with a blank line
     */
    private void buildObjectClassDefs(StringBuilder OC_DEFINITIONS, String prefix, boolean blankLineSeperator) {
        for (AttributeClass cls : AttributeClass.values()) {

            String ocName = cls.getOCName();
            String ocCanonicalName = ocName.toLowerCase();
            ObjectClassInfo oci = mOCs.get(ocCanonicalName);
            if (oci == null)
                continue;  // oc not defined in xml, skip

            // OC_DEFINITIONS:
            List<String> comment = oci.getComment();
            OC_DEFINITIONS.append("#\n");
            for (String line : comment) {
                if (line.length() > 0)
                    OC_DEFINITIONS.append("# " + line + "\n");
                else
                    OC_DEFINITIONS.append("#\n");
            }
            OC_DEFINITIONS.append("#\n");

            OC_DEFINITIONS.append(prefix + "( " + oci.getName() + "\n");
            OC_DEFINITIONS.append(ML_CONT_PREFIX + "NAME '" + oci.getName() + "'\n");
            OC_DEFINITIONS.append(ML_CONT_PREFIX + "DESC '" + oci.getDescription() + "'\n");
            OC_DEFINITIONS.append(ML_CONT_PREFIX + "SUP ");
            for (String sup : oci.getSuperOCs())
                OC_DEFINITIONS.append(sup);
            OC_DEFINITIONS.append(" " + oci.getType() + "\n");

            StringBuilder value = new StringBuilder();
            buildObjectClassAttrs(cls, value);

            OC_DEFINITIONS.append(value);
            OC_DEFINITIONS.append(")\n");

            if (blankLineSeperator)
                OC_DEFINITIONS.append("\n");

        }
    }

    private void buildObjectClassAttrs(AttributeClass cls, StringBuilder value) {
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

        if (!must.isEmpty()) {
            value.append(ML_CONT_PREFIX + "MUST (\n");
            Iterator<String> mustIter = must.iterator();
            while (true) {
                value.append(ML_CONT_PREFIX + "  ").append(mustIter.next());
                if (!mustIter.hasNext()) {
                    break;
                }
                value.append(" $\n");
            }
            value.append("\n" + ML_CONT_PREFIX + ")\n");
        }
        if (!may.isEmpty()) {
            value.append(ML_CONT_PREFIX + "MAY (\n");
            Iterator<String> mayIter = may.iterator();
            while (true) {
                value.append(ML_CONT_PREFIX + "  ").append(mayIter.next());
                if (!mayIter.hasNext()) {
                    break;
                }
                value.append(" $\n");
            }
            value.append("\n" + ML_CONT_PREFIX + ")\n");
        }
        value.append(ML_CONT_PREFIX);
    }

    private List<AttributeInfo> getAttrList(int groupId) {
        List<AttributeInfo> list = new ArrayList<AttributeInfo>(mAttrs.size());
        for (AttributeInfo ai : mAttrs.values()) {
            if (ai.getId() > -1 && ai.getGroupId() == groupId) {
                list.add(ai);
            }
        }
        return list;
    }

    private void sortAttrsByOID(List<AttributeInfo> list) {
        Collections.sort(list, new Comparator<AttributeInfo>() {
            public int compare(AttributeInfo a1, AttributeInfo b1) {
                return a1.getId() - b1.getId();
            }
        });
    }

    private void sortAttrsByName(List<AttributeInfo> list) {
        Collections.sort(list, new Comparator<AttributeInfo>() {
            public int compare(AttributeInfo a1, AttributeInfo b1) {
                return a1.getName().compareTo(b1.getName());
            }
        });
    }

    private List<ObjectClassInfo> getOCList(int groupId) {
        List<ObjectClassInfo> list = new ArrayList<ObjectClassInfo>(mOCs.size());
        for (ObjectClassInfo oci : mOCs.values()) {
            if (oci.getId() > -1 && oci.getGroupId() == groupId) {
                list.add(oci);
            }
        }
        return list;
    }

    private void sortOCsByOID(List<ObjectClassInfo> list) {
        Collections.sort(list, new Comparator<ObjectClassInfo>() {
            public int compare(ObjectClassInfo oc1, ObjectClassInfo oc2) {
                return oc1.getId() - oc2.getId();
            }
        });
    }

    private void sortOCsByName(List<ObjectClassInfo> list) {
        Collections.sort(list, new Comparator<ObjectClassInfo>() {
            public int compare(ObjectClassInfo oc1, ObjectClassInfo oc2) {
                return oc1.getName().compareTo(oc2.getName());
            }
        });
    }

    /**
     * using the old schema template file (version 9), delete this methods after things are stablized.
     *
     * @param pw
     * @param schemaTemplateFile
     * @throws IOException
     */
    private void generateLdapSchema_old(PrintWriter pw, String schemaTemplateFile) throws IOException {
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
            List<AttributeInfo> list = getAttrList(i);

            // ATTRIBUTE_OIDS - sorted by OID
            sortAttrsByOID(list);

            for (AttributeInfo ai : list) {
                String parentOid = ai.getParentOid();
                if (parentOid == null)
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + mGroupMap.get(i) + ':' + ai.getId() + "\n");
                else
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + parentOid + "." + ai.getId() + "\n");
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            sortAttrsByName(list);

            for (AttributeInfo ai : list) {
                ATTRIBUTE_DEFINITIONS.append("attributetype ");
                buildAttrDef(ATTRIBUTE_DEFINITIONS, ai);
                ATTRIBUTE_DEFINITIONS.append("\n\n");
            }
        }

        Map<String,String> templateFillers = new HashMap<String,String>();
        templateFillers.put("SCHEMA_VERSION_INFO", CLOptions.buildVersion());
        templateFillers.put("GROUP_OIDS", GROUP_OIDS.toString());
        templateFillers.put("ATTRIBUTE_OIDS", ATTRIBUTE_OIDS.toString());
        templateFillers.put("ATTRIBUTE_DEFINITIONS", ATTRIBUTE_DEFINITIONS.toString());

        for (AttributeClass cls : AttributeClass.values()) {
            String key = "CLASS_MEMBERS_" + cls.toString().toUpperCase();
            StringBuilder value = new StringBuilder();
            buildObjectClassAttrs(cls, value);
            templateFillers.put(key, value.toString());
        }

        pw.print(StringUtil.fillTemplate(templateString, templateFillers));
    }

    /**
     * This methods uses xml for generating objectclass OIDs and definitions
     */
    private void generateLdapSchema(PrintWriter pw, String schemaTemplateFile) throws IOException {
        byte[] templateBytes = ByteUtil.getContent(new File(schemaTemplateFile));
        String templateString = new String(templateBytes, "utf-8");

        StringBuilder BANNER = new StringBuilder();
        StringBuilder ZIMBRA_ROOT_OIDS = new StringBuilder();
        StringBuilder GROUP_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_DEFINITIONS = new StringBuilder();
        StringBuilder OC_GROUP_OIDS = new StringBuilder();
        StringBuilder OC_OIDS = new StringBuilder();
        StringBuilder OC_DEFINITIONS = new StringBuilder();

        buildSchemaBanner(BANNER);
        buildZimbraRootOIDs(ZIMBRA_ROOT_OIDS, "objectIdentifier ");

        for (Iterator<Integer> iter = mGroupMap.keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            //GROUP_OIDS
            GROUP_OIDS.append("objectIdentifier " + mGroupMap.get(i) + " ZimbraLDAP:" + i + "\n");

            // List all attrs which we define and which belong in this group
            List<AttributeInfo> list = getAttrList(i);

            // ATTRIBUTE_OIDS - sorted by OID
            sortAttrsByOID(list);

            for (AttributeInfo ai : list) {
                String parentOid = ai.getParentOid();
                if (parentOid == null)
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + mGroupMap.get(i) + ':' + ai.getId() + "\n");
                else
                    ATTRIBUTE_OIDS.append("objectIdentifier " + ai.getName() + " " + parentOid + "." + ai.getId() + "\n");
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            sortAttrsByName(list);

            for (AttributeInfo ai : list) {
                ATTRIBUTE_DEFINITIONS.append("attributetype ");
                buildAttrDef(ATTRIBUTE_DEFINITIONS, ai);
                ATTRIBUTE_DEFINITIONS.append("\n\n");
            }
        }

        // object class OIDs
        buildObjectClassOIDs(OC_GROUP_OIDS, OC_OIDS, "objectIdentifier ");

        // object class definitions
        buildObjectClassDefs(OC_DEFINITIONS, "objectclass ", true);

        Map<String,String> templateFillers = new HashMap<String,String>();
        templateFillers.put("BANNER", BANNER.toString());
        templateFillers.put("ZIMBRA_ROOT_OIDS", ZIMBRA_ROOT_OIDS.toString());
        templateFillers.put("GROUP_OIDS", GROUP_OIDS.toString());
        templateFillers.put("ATTRIBUTE_OIDS", ATTRIBUTE_OIDS.toString());
        templateFillers.put("OC_GROUP_OIDS", OC_GROUP_OIDS.toString());
        templateFillers.put("OC_OIDS", OC_OIDS.toString());
        templateFillers.put("ATTRIBUTE_DEFINITIONS", ATTRIBUTE_DEFINITIONS.toString());
        templateFillers.put("OC_DEFINITIONS", OC_DEFINITIONS.toString());

        pw.print(StringUtil.fillTemplate(templateString, templateFillers));
    }

    private void generateSchemaLdif(PrintWriter pw) {

        StringBuilder BANNER = new StringBuilder();
        StringBuilder ZIMBRA_ROOT_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_GROUP_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_OIDS = new StringBuilder();
        StringBuilder ATTRIBUTE_DEFINITIONS = new StringBuilder();
        StringBuilder OC_GROUP_OIDS = new StringBuilder();
        StringBuilder OC_OIDS = new StringBuilder();
        StringBuilder OC_DEFINITIONS = new StringBuilder();

        buildSchemaBanner(BANNER);
        buildZimbraRootOIDs(ZIMBRA_ROOT_OIDS, "olcObjectIdentifier: ");

        for (Iterator<Integer> iter = mGroupMap.keySet().iterator(); iter.hasNext();) {
            int i = iter.next();

            //GROUP_OIDS
            ATTRIBUTE_GROUP_OIDS.append("olcObjectIdentifier: " + mGroupMap.get(i) + " ZimbraLDAP:" + i + "\n");

            // List all attrs which we define and which belong in this group
            List<AttributeInfo> list = getAttrList(i);

            // ATTRIBUTE_OIDS - sorted by OID
            sortAttrsByOID(list);

            for (AttributeInfo ai : list) {
                String parentOid = ai.getParentOid();
                if (parentOid == null)
                    ATTRIBUTE_OIDS.append("olcObjectIdentifier: " + ai.getName() + " " + mGroupMap.get(i) + ':' + ai.getId() + "\n");
                else
                    ATTRIBUTE_OIDS.append("olcObjectIdentifier: " + ai.getName() + " " + parentOid + "." + ai.getId() + "\n");
            }

            // ATTRIBUTE_DEFINITIONS: DESC EQUALITY NAME ORDERING SINGLE-VALUE SUBSTR SYNTAX
            // - sorted by name
            sortAttrsByName(list);

            /* Hack to add the company attribute from Microsoft schema
             * For generateLdapSchema, it is specified in the zimbra.schema-template file
             * We don't use a template file for generateSchemaLdif thus hardcode here.
             * Move to template file if really necessary.
             *
            #### From Microsoft Schema
            olcAttributeTypes ( 1.2.840.113556.1.2.146
                    NAME ( 'company' )
                    SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{512}
                    EQUALITY caseIgnoreMatch
                    SUBSTR caseIgnoreSubstringsMatch
                    SINGLE-VALUE )
            */

            ATTRIBUTE_DEFINITIONS.append("olcAttributeTypes: ( 1.2.840.113556.1.2.146\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "NAME ( 'company' )\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{512}\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "EQUALITY caseIgnoreMatch\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SUBSTR caseIgnoreSubstringsMatch\n");
            ATTRIBUTE_DEFINITIONS.append(ML_CONT_PREFIX + "SINGLE-VALUE )\n");

            for (AttributeInfo ai : list) {
                ATTRIBUTE_DEFINITIONS.append("olcAttributeTypes: ");
                buildAttrDef(ATTRIBUTE_DEFINITIONS, ai);
                ATTRIBUTE_DEFINITIONS.append("\n");
            }
        }

        // objectclass OIDs
        buildObjectClassOIDs(OC_GROUP_OIDS, OC_OIDS, "olcObjectIdentifier: ");

        // objectclass definitions
        buildObjectClassDefs(OC_DEFINITIONS, "olcObjectClasses: ", false);

        pw.println(BANNER);
        pw.println("dn: cn=zimbra,cn=schema,cn=config");
        pw.println("objectClass: olcSchemaConfig");
        pw.println("cn: zimbra");
        pw.print(ZIMBRA_ROOT_OIDS);
        pw.print(ATTRIBUTE_GROUP_OIDS);
        pw.print(ATTRIBUTE_OIDS);
        pw.print(OC_GROUP_OIDS);
        pw.print(OC_OIDS);
        pw.print(ATTRIBUTE_DEFINITIONS);
        pw.print(OC_DEFINITIONS);
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
        ZimbraLdapContext zlc = null;
        try {
          zlc = new ZimbraLdapContext(true);
          DirContext schema = zlc.getSchema();

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

          dumpAttrs(pw, "GlobalConfig", zlc.getAttributes("cn=config,cn=zimbra"));
          dumpAttrs(pw, "DefaultCOS", zlc.getAttributes("cn=default,cn=cos,cn=zimbra"));
        } catch (NamingException ne) {
            ne.printStackTrace();
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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

    public AttributeInfo getAttributeInfo(String name) {
        if (name == null)
            return null;
        else
            return mAttrs.get(name.toLowerCase());
    }

    /**
     *
     * @param pw
     * @param optionValue
     * @throws ServiceException
     */
    private void generateProvisioningConstants(String javaFile) throws ServiceException, IOException {
        //AttributeManager am = AttributeManager.getInstance();

        List<String> list = new ArrayList<String>(mAttrs.keySet());
        Collections.sort(list);

        StringBuilder result = new StringBuilder();

        for (String a : list) {
            AttributeInfo ai = mAttrs.get(a.toLowerCase());
            if (ai == null || ai.getType() != AttributeType.TYPE_ENUM)
                continue;
            generateEnum(result, ai);
        }

        for (String a : list) {
            AttributeInfo ai = mAttrs.get(a.toLowerCase());
            if (ai == null)
                continue;

            result.append("\n    /**\n");
            if (ai.getDescription() != null) {
                result.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(ai.getDescription()), 70, "     * "));
                result.append("\n");
            }
            if (ai.getSince() != null) {
                result.append("     *\n");
                result.append(String.format("     * @since ZCS %s%n", ai.getSince().toString()));
            }
            result.append("     */\n");
            result.append(String.format("    @ZAttr(id=%d)%n", ai.getId()));

            result.append(String.format("    public static final String A_%s = \"%s\";%n", ai.getName(), ai.getName()));
        }

        FileGenUtil.replaceJavaFile(javaFile, result.toString());

    }

    private static String enumName(AttributeInfo ai) {
        String enumName = ai.getName();
        if (enumName.startsWith("zimbra")) enumName = enumName.substring(6);
        enumName = StringUtil.escapeJavaIdentifier(enumName.substring(0,1).toUpperCase() + enumName.substring(1));
        return enumName;
    }
    
    private static void generateEnum(StringBuilder result, AttributeInfo ai) throws ServiceException {

        Map<String,String> values = new HashMap<String, String>();
        for (String v : ai.getEnumSet()) {
            values.put(v, StringUtil.escapeJavaIdentifier(v));
        }

        String enumName = enumName(ai);

        result.append(String.format("%n"));
        result.append(String.format("    public static enum %s {%n", enumName));
        Set<Map.Entry<String,String>> set = values.entrySet();
        int i =1;
        for (Map.Entry<String,String> entry : set) {
            result.append(String.format("        %s(\"%s\")%s%n", entry.getValue(), entry.getKey(), i == set.size() ? ";" : ","));
            i++;
        }

        result.append(String.format("        private String mValue;%n"));
        result.append(String.format("        private %s(String value) { mValue = value; }%n", enumName));
        result.append(String.format("        public String toString() { return mValue; }%n"));
        result.append(String.format("        public static %s fromString(String s) throws ServiceException {%n", enumName));
        result.append(String.format("            for (%s value : values()) {%n", enumName));
        result.append(String.format("                if (value.mValue.equals(s)) return value;%n"));
        result.append(String.format("             }%n"));
        result.append(String.format("             throw ServiceException.INVALID_REQUEST(\"invalid value: \"+s+\", valid values: \"+ Arrays.asList(values()), null);%n"));
        result.append(String.format("        }%n"));
        for (Map.Entry<String,String> entry : set) {
            result.append(String.format("        public boolean is%s() { return this == %s;}%n", StringUtil.capitalize(entry.getValue()), entry.getValue()));
        }
        result.append(String.format("    }%n"));
    }

    /**
     *
     * @param pw
     * @param inClass
     * @param optionValue
     * @throws ServiceException
     */
    private void generateGetters(String inClass, String javaFile) throws ServiceException, IOException {
        if (inClass == null)
            usage("no class specified");


        AttributeClass ac = AttributeClass.valueOf(inClass);
        Set<String> attrsInClass = getAttrsInClass(ac);

        // add in mailRecipient if we need to
        if (ac == AttributeClass.account) {
            SetUtil.union(attrsInClass, getAttrsInClass(AttributeClass.mailRecipient));
        }

        List<String> list = new ArrayList<String>(attrsInClass);
        Collections.sort(list);

        StringBuilder result = new StringBuilder();

        for (String a : list) {
            AttributeInfo ai = mAttrs.get(a.toLowerCase());
            if (ai == null)
                continue;

            switch (ai.getType()) {
                case TYPE_DURATION:
                case TYPE_GENTIME:
                case TYPE_ENUM:
                case TYPE_PORT:
                    generateGetter(result, ai, false, ac);
                    generateGetter(result, ai, true, ac);
                    generateSetters(result, ai, false, SetterType.set);
                    if (ai.getType() == AttributeType.TYPE_GENTIME || 
                        ai.getType() == AttributeType.TYPE_ENUM || 
                        ai.getType() == AttributeType.TYPE_PORT) {
                        generateSetters(result, ai, true, SetterType.set);
                    }
                    generateSetters(result, ai, false, SetterType.unset);
                    break;
                default:
                    if (ai.getName().equalsIgnoreCase("zimbraLocale")) {
                        generateGetter(result, ai, true, ac);
                    } else {
                        generateGetter(result, ai, false, ac);
                    }
                    generateSetters(result, ai, false, SetterType.set);
                    if (ai.getCardinality() == AttributeCardinality.multi) {
                        generateSetters(result, ai, false, SetterType.add);
                        generateSetters(result, ai, false, SetterType.remove);
                    }
                    generateSetters(result, ai, false, SetterType.unset);
                    break;
            }
        }
        FileGenUtil.replaceJavaFile(javaFile, result.toString());
    }

    private static String defaultValue(AttributeInfo ai, AttributeClass ac) {
        List<String> values;
        switch (ac) {
            case account:
            case calendarResource:
            case cos:
                values = ai.getDefaultCosValues();
                break;
            case domain:
                if (ai.hasFlag(AttributeFlag.domainInherited))
                    values = ai.getGlobalConfigValues();
                else
                    return null;
                break;
            case server:
                if (ai.hasFlag(AttributeFlag.serverInherited))
                    values = ai.getGlobalConfigValues();
                else 
                    return null;
                break;
            case globalConfig:
                values = ai.getGlobalConfigValues();
                break;
            default:
                return null;
        }
        if (values == null || values.size() == 0)
            return null;

        if (ai.getCardinality() != AttributeCardinality.multi) {
            return values.get(0);
        } else {
            StringBuilder result = new StringBuilder();
            result.append("new String[] {");
            boolean first = true;
            for (String v : values) {
                if (!first) result.append(","); else first = false;
                result.append("\"");
                result.append(v.replace("\"","\\\""));
                result.append("\"");
            }
            result.append("}");
            return result.toString();
        }
    }

    private static void generateGetter(StringBuilder result, AttributeInfo ai, boolean asString, AttributeClass ac) throws ServiceException {
        String javaType;
        String javaBody;
        String javaDocReturns;
        String name = ai.getName();
        AttributeType type = asString ? AttributeType.TYPE_STRING : ai.getType();
        boolean asStringDoc = false;

        String methodName = ai.getName();
        if (methodName.startsWith("zimbra")) methodName = methodName.substring(6);
        methodName = (type == AttributeType.TYPE_BOOLEAN ? "is" : "get")+methodName.substring(0,1).toUpperCase() + methodName.substring(1);
        if (asString) methodName += "AsString";

        String defaultValue = defaultValue(ai, ac);

        switch (type) {
            case TYPE_BOOLEAN:
                defaultValue = "TRUE".equalsIgnoreCase(defaultValue) ? "true" : "false";
                javaType = "boolean";
                javaBody = String.format("return getBooleanAttr(Provisioning.A_%s, %s);", name, defaultValue);
                javaDocReturns = String.format(", or %s if unset", defaultValue);
                break;
            case TYPE_INTEGER:
                if (defaultValue == null) defaultValue = "-1";
                javaType = "int";
                javaBody = String.format("return getIntAttr(Provisioning.A_%s, %s);", name, defaultValue);
                javaDocReturns = String.format(", or %s if unset", defaultValue);
                break;
            case TYPE_PORT:
                if (defaultValue == null) defaultValue = "-1";
                javaType = "int";
                javaBody = String.format("return getIntAttr(Provisioning.A_%s, %s);", name, defaultValue);
                javaDocReturns = String.format(", or %s if unset", defaultValue);
                asStringDoc = true;
                break;
            case TYPE_ENUM:
                javaType = "ZAttrProvisioning." + enumName(ai);
                if (defaultValue != null) {
                    defaultValue = javaType + "." + StringUtil.escapeJavaIdentifier(defaultValue);
                } else {
                    defaultValue = "null";
                }
                javaBody = String.format("try { String v = getAttr(Provisioning.A_%s); return v == null ? %s : ZAttrProvisioning.%s.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return %s; }", name, defaultValue,enumName(ai), defaultValue);
                javaDocReturns = String.format(", or %s if unset and/or has invalid value", defaultValue);
                break;
            case TYPE_LONG:
                if (defaultValue == null) defaultValue = "-1";
                javaType = "long";
                javaBody = String.format("return getLongAttr(Provisioning.A_%s, %sL);", name, defaultValue);
                javaDocReturns = String.format(", or %s if unset", defaultValue);
                break;
            case TYPE_DURATION:
                String defaultDurationStrValue;
                if (defaultValue != null) {
                    defaultDurationStrValue = " ("+defaultValue+") ";
                    defaultValue = String.valueOf(DateUtil.getTimeInterval(defaultValue, -1));
                } else {
                    defaultValue = "-1";
                    defaultDurationStrValue = "";
                }
                javaBody = String.format("return getTimeInterval(Provisioning.A_%s, %sL);", name, defaultValue);
                javaDocReturns = String.format(" in millseconds, or %s%s if unset", defaultValue, defaultDurationStrValue);
                javaType = "long";
                asStringDoc = true;
                break;
            case TYPE_GENTIME:
                javaType = "Date";
                javaBody = String.format("return getGeneralizedTimeAttr(Provisioning.A_%s, null);", name);
                javaDocReturns = " as Date, null if unset or unable to parse";
                asStringDoc = true;
                break;
            default:
                if (ai.getCardinality() != AttributeCardinality.multi) {
                    if (defaultValue != null) {
                        defaultValue = "\"" + defaultValue.replace("\"", "\\\"") +"\"";
                    } else {
                        defaultValue = "null";
                    }
                    javaType = "String";
                    javaBody = String.format("return getAttr(Provisioning.A_%s, %s);", name, defaultValue);
                    javaDocReturns = String.format(", or %s if unset", defaultValue);
                } else {
                    javaType = "String[]";
                    if (defaultValue == null) {
                        javaBody = String.format("return getMultiAttr(Provisioning.A_%s);", name);
                    } else {
                        javaBody = String.format("String[] value = getMultiAttr(Provisioning.A_%s); return value.length > 0 ? value : %s;", name, defaultValue);    
                    }
                    javaDocReturns = ", or empty array if unset";
                }
                break;
        }

        result.append("\n    /**\n");
        if (ai.getDescription() != null) {
            result.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(ai.getDescription()), 70, "     * "));
            result.append("\n");
        }
        if (ai.getType() == AttributeType.TYPE_ENUM) {
            result.append("     *\n");
            result.append(String.format("     * <p>Valid values: %s%n", ai.getEnumSet().toString()));
        }
        if (asStringDoc) {
            result.append("     *\n");
            result.append(String.format("     * <p>Use %sAsString to access value as a string.%n", methodName));
            result.append("     *\n");
            result.append(String.format("     * @see #%sAsString()%n", methodName));
        }
        result.append("     *\n");
        result.append(String.format("     * @return %s%s%n", name, javaDocReturns));
        if (ai.getSince() != null) {
            result.append("     *\n");
            result.append(String.format("     * @since ZCS %s%n", ai.getSince().toString()));
        }
        result.append("     */\n");
        result.append(String.format("    @ZAttr(id=%d)%n", ai.getId()));
        result.append(String.format("    public %s %s() {%n        %s%n    }%n", javaType, methodName, javaBody));
    }

    private static enum SetterType { set, add, unset, remove }

    private static void generateSetters(StringBuilder result, AttributeInfo ai, boolean asString, SetterType setterType) throws ServiceException {
        generateSetter(result, ai, asString, setterType, true);
        generateSetter(result, ai, asString, setterType, false);
    }

    private static void generateSetter(StringBuilder result, AttributeInfo ai, boolean asString, SetterType setterType, boolean noMap) throws ServiceException {
        String javaType;
        String putParam;

        String name = ai.getName();

        AttributeType type = asString ? AttributeType.TYPE_STRING : ai.getType();

        String methodNamePrefix;
        String methodName = ai.getName();
        if (methodName.startsWith("zimbra")) methodName = methodName.substring(6);
        methodName = setterType.name()+methodName.substring(0,1).toUpperCase() + methodName.substring(1);
        if (asString) methodName += "AsString";

        switch (type) {
            case TYPE_BOOLEAN:
                javaType = "boolean";
                putParam = String.format("%s ? Provisioning.TRUE : Provisioning.FALSE", name);
                break;
            case TYPE_INTEGER:
            case TYPE_PORT:    
                javaType = "int";
                putParam = String.format("Integer.toString(%s)", name);
                break;
            case TYPE_LONG:
                javaType = "long";
                putParam = String.format("Long.toString(%s)", name);
                break;
            case TYPE_GENTIME:
                javaType = "Date";
                putParam = String.format("DateUtil.toGeneralizedTime(%s)", name);
                break;
            case TYPE_ENUM:
                javaType = "ZAttrProvisioning." + enumName(ai);
                putParam = String.format("%s.toString()", name);
                break;
            default:
                if (ai.getCardinality() != AttributeCardinality.multi) {
                    javaType = "String";
                    putParam = String.format("%s", name);
                } else {
                    if (setterType == SetterType.set) javaType = "String[]";
                    else javaType = "String";
                    putParam = String.format("%s", name);
                }
                break;
        }

        String  mapType= "Map<String,Object>";
        
        result.append("\n    /**\n");
        if (ai.getDescription() != null) {
            result.append(FileGenUtil.wrapComments(StringUtil.escapeHtml(ai.getDescription()), 70, "     * "));
            result.append("\n");
        }
        if (ai.getType() == AttributeType.TYPE_ENUM) {
            result.append("     *\n");
            result.append(String.format("     * <p>Valid values: %s%n", ai.getEnumSet().toString()));
        }
        result.append("     *\n");

        String paramDoc = "";
        String  body = "";

         switch(setterType) {
            case set:
                body = String.format("        attrs.put(Provisioning.A_%s, %s);%n", name, putParam);
                paramDoc = String.format("     * @param %s new value%n", name);
                break;
            case add:
                body = String.format("        StringUtil.addToMultiMap(attrs, \"+\" + Provisioning.A_%s, %s);%n",name, name);
                paramDoc = String.format("     * @param %s new to add to existing values%n", name);
                break;
            case remove:
                body = String.format("        StringUtil.addToMultiMap(attrs, \"-\" + Provisioning.A_%s, %s);%n",name, name);
                paramDoc = String.format("     * @param %s existing value to remove%n", name);
                break;
            case unset:
                body = String.format("        attrs.put(Provisioning.A_%s, \"\");%n", name);
                paramDoc = null;
                break;
        }

        if (paramDoc != null) result.append(paramDoc);
        if (!noMap) {
            result.append(String.format("     * @param attrs existing map to populate, or null to create a new map%n"));
            result.append("     * @return populated map to pass into Provisioning.modifyAttrs\n");
        } else {
            result.append("     * @throws com.zimbra.common.service.ServiceException if error during update\n");
        }
        if (ai.getSince() != null) {
            result.append("     *\n");
            result.append(String.format("     * @since ZCS %s%n", ai.getSince().toString()));
        }
        result.append("     */\n");
        result.append(String.format("    @ZAttr(id=%d)%n", ai.getId()));
        if (noMap) {
            if (setterType !=  SetterType.unset)
                result.append(String.format("    public void %s(%s %s) throws com.zimbra.common.service.ServiceException {%n", methodName, javaType, name));
            else
                result.append(String.format("    public void %s() throws com.zimbra.common.service.ServiceException {%n", methodName));
            result.append(String.format("        HashMap<String,Object> attrs = new HashMap<String,Object>();%n"));
            result.append(body);
            result.append(String.format("        getProvisioning().modifyAttrs(this, attrs);%n"));
        } else {
            if (setterType !=  SetterType.unset)
                result.append(String.format("    public %s %s(%s %s, %s attrs) {%n", mapType, methodName, javaType, name, mapType));
            else
                result.append(String.format("    public %s %s(%s attrs) {%n", mapType, methodName, mapType));
            result.append(String.format("        if (attrs == null) attrs = new HashMap<String,Object>();%n"));
            result.append(body);
            result.append(String.format("        return attrs;%n"));
        }

        result.append(String.format("    }%n"));
    }

    public static void loadLdapSchemaExtensionAttrs(LdapProvisioning prov) {
        synchronized(AttributeManager.class) {
            try {
                AttributeManager theInstance = AttributeManager.getInstance();
                theInstance.getLdapSchemaExtensionAttrs(prov);
                theInstance.computeClassToAllAttrsMap();  // recompute the ClassToAllAttrsMap 
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to load LDAP schema extensions", e);
            }
        }
    }
    
    private void getLdapSchemaExtensionAttrs(LdapProvisioning prov) throws ServiceException {
        if (mLdapSchemaExtensionInited)
            return;
        
        mLdapSchemaExtensionInited = true;
        
        getExtraObjectClassAttrs(prov, AttributeClass.account, Provisioning.A_zimbraAccountExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.calendarResource, Provisioning.A_zimbraCalendarResourceExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.cos, Provisioning.A_zimbraCosExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.domain, Provisioning.A_zimbraDomainExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.server, Provisioning.A_zimbraServerExtraObjectClass);
    }
    
    private void getExtraObjectClassAttrs(LdapProvisioning prov, AttributeClass ac, String extraObjectClassAttr) throws ServiceException {
        Config config = prov.getConfig();
        
        String[] extraObjectClasses = config.getMultiAttr(extraObjectClassAttr);

        if (extraObjectClasses.length > 0) {
            Set<String> attrsInOCs = mClassToAttrsMap.get(AttributeClass.account);
            getAttrsInOCs(extraObjectClasses, attrsInOCs);
        }
    }
    
    private void getAttrsInOCs(String[] ocs, Set<String> attrsInOCs) throws ServiceException {
        
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            DirContext schema = zlc.getSchema();
          
            Map<String, Object> attrs;
            for (String oc : ocs) {
                attrs = null;
                try {
                    DirContext ocSchema = (DirContext)schema.lookup("ClassDefinition/" + oc);
                    Attributes attributes = ocSchema.getAttributes("");
                    attrs = LdapUtil.getAttrs(attributes);
                } catch (NamingException e) {
                    ZimbraLog.account.debug("unable to load LDAP schema extension for objectclass: " + oc, e);
                }
                
                if (attrs == null)
                    continue;
                
                for (Map.Entry<String, Object> attr : attrs.entrySet()) {
                    String attrName = attr.getKey();
                    if ("MAY".compareToIgnoreCase(attrName) == 0 || "MUST".compareToIgnoreCase(attrName) == 0) {
                        Object value = attr.getValue();
                        if (value instanceof String)
                            attrsInOCs.add((String)value);
                        else if (value instanceof String[]) {
                            for (String v : (String[])value)
                                attrsInOCs.add(v);
                        }
                    }
                }
              
            }          

        } catch (NamingException e) {
            ZimbraLog.account.debug("unable to load LDAP schema extension", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
