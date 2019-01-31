/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.Version;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.callback.CallbackContext;
import com.zimbra.cs.account.callback.IDNCallback;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.extension.ExtensionUtil;

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
    private static final String E_DEFAULT_EXTERNAL_COS_VALUE = "defaultExternalCOSValue";
    private static final String E_DEFAULT_COS_VALUE_UPGRADE = "defaultCOSValueUpgrade";

    private static final Set<AttributeFlag> allowedEphemeralFlags = new HashSet<AttributeFlag>();
    static {
        allowedEphemeralFlags.add(AttributeFlag.ephemeral);
        allowedEphemeralFlags.add(AttributeFlag.dynamic);
        allowedEphemeralFlags.add(AttributeFlag.expirable);
    }

    private static AttributeManager mInstance;

    // contains attrs defined in one of the zimbra .xml files (currently zimbra attrs and some of the amavis attrs)
    // these attrs have AttributeInfo
    //
    // Note: does *not* contains attrs defined in the extensions(attrs in OCs specified in global config ***ExtraObjectClass)
    //
    // Extension attr names are in the class -> attrs maps:
    //     mClassToAttrsMap, mClassToLowerCaseAttrsMap, mClassToAllAttrsMap maps.
    //
    private final Map<String, AttributeInfo> mAttrs = new HashMap<String, AttributeInfo>();

    private final Map<String, ObjectClassInfo> mOCs = new HashMap<String, ObjectClassInfo>();

    // only direct attrs
    private final Map<AttributeClass, Set<String>> mClassToAttrsMap = new HashMap<AttributeClass, Set<String>>();
    private final Map<AttributeClass, Set<String>> mClassToLowerCaseAttrsMap = new HashMap<AttributeClass, Set<String>>();

    // direct attrs and attrs from included objectClass's
    private final Map<AttributeClass, Set<String>> mClassToAllAttrsMap = new HashMap<AttributeClass, Set<String>>();

    private boolean mLdapSchemaExtensionInited = false;

    private final AttributeCallback mIDNCallback = new IDNCallback();

    private static Map<Integer,String> mGroupMap = new HashMap<Integer,String>();

    private static Map<Integer,String> mOCGroupMap = new HashMap<Integer,String>();

    // attrs declared as type="binary" in zimbra-attrs.xml
    private static Set<String> mBinaryAttrs = new HashSet<String>();

    // attrs that require ";binary" appended explicitly when transferred.
    // The only such syntax we support for now is:
    // 1.3.6.1.4.1.1466.115.121.1.8 - Certificate syntax
    private static Set<String> mBinaryTransferAttrs = new HashSet<String>();

    private final Map<String, AttributeInfo> mEphemeralAttrs = new HashMap<String, AttributeInfo>(); //lowercased
    private final Set<String> mEphemeralAttrsSet = new HashSet<String>(); // not lowercased
    private final Map<EntryType, Map<String, AttributeInfo>> mNonDynamicEphemeralAttrs = new HashMap<EntryType, Map<String, AttributeInfo>>(); // ephemeral attributes that can be retrieved as part of Entry.getAttrs()

    /*
     * Notes on certificate attributes
     *
     * attribute                       origin               cardinality   has EQUALITY rule       SYNTAX                          require     require JNDI
     *                                                                    (i.e. can add/delete                                    ";binary"   "java.naming.ldap.attributes.binary"
     *                                                                     individual values)                                     transfer    environment property
     * ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     * userCertificate                 RFC2256              multi         yes                     1.3.6.1.4.1.1466.115.121.1.8    yes         no
           # contains DER format certificate
     *
     * userSMIMECertificate            RFC2798              multi         no                      1.3.6.1.4.1.1466.115.121.1.5    no          yes
           # A PKCS#7 [RFC2315] SignedData, where the content that is signed is
           # ignored by consumers of userSMIMECertificate values.  It is
           # recommended that values have a `contentType' of data with an absent
           # `content' field.  Values of this attribute contain a person's entire
           # certificate chain and an smimeCapabilities field [RFC2633] that at a
           # minimum describes their SMIME algorithm capabilities.  Values for
           # this attribute are to be stored and requested in binary form, as
           # 'userSMIMECertificate;binary'.  If available, this attribute is
           # preferred over the userCertificate attribute for S/MIME applications.
           ## OpenLDAP note: ";binary" transfer should NOT be used as syntax is binary
     *
     *
     * zimbraPrefMailSMIMECertificate  Zimbra(deprecated)   multi         yes                     1.3.6.1.4.1.1466.115.121.1.40   no          yes
     *
     */

    // do not keep comments and descriptions when running in a server
    private static boolean mMinimize = false;

    public static AttributeManager getInst() {
        try {
            return AttributeManager.getInstance();
        } catch (ServiceException e) {
            ZimbraLog.account.warn("could not get AttributeManager instance", e);
            return null;
        }
    }

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

    public boolean isMultiValued(String attrName) {
        AttributeInfo attributeInfo = getAttributeInfo(attrName);
        return attributeInfo != null && attributeInfo.getCardinality() == AttributeCardinality.multi;
    }

    public boolean isEphemeral(String attrName) {
        return mEphemeralAttrsSet.contains(attrName);
    }

    public boolean isDynamic(String attrName) {
        AttributeInfo ai = mEphemeralAttrs.get(attrName.toLowerCase());
        if (ai != null) {
            return ai.isDynamic();
        } else {
            return false;
        }
    }

    /**
     * Retrieves a map of ephemeral attributes that don't have dynamic components, for the given EntryType.
     * This is used to include ephemeral data in Entry.getAttrs() results. Dynamic ephemeral attributes are
     * excluded, as the API does not support fetching dynamic key components.
     * @param entryType
     */
    public Map<String, AttributeInfo> getNonDynamicEphemeralAttrs(EntryType entryType) {
        return mNonDynamicEphemeralAttrs.get(entryType);
    }

    private void addNonDynamicEphemeralAttr(AttributeInfo info) {
        Set<AttributeClass> attrClasses = new HashSet<AttributeClass>();
        if (info.getOptionalIn() != null) {
            attrClasses.addAll(info.getOptionalIn());
        }
        if (info.getRequiredIn() != null) {
            attrClasses.addAll(info.getRequiredIn());
        }
        for (AttributeClass attrClass: attrClasses) {
            EntryType entryType = attrClass.getEntryType();
            if (entryType == null) {
                continue;
            }
            Map<String, AttributeInfo> infoMap = mNonDynamicEphemeralAttrs.get(entryType);
            if (infoMap == null) {
                infoMap = new HashMap<String, AttributeInfo>();
                mNonDynamicEphemeralAttrs.put(entryType, infoMap);
            }
            infoMap.put(info.getName(), info);
        }
    }

    @VisibleForTesting
    public void addAttribute(AttributeInfo info) {
        mAttrs.put(info.mName.toLowerCase(), info);
        if (info.isEphemeral()) {
            mEphemeralAttrs.put(info.mName.toLowerCase(), info);
            mEphemeralAttrsSet.add(info.mName);
            if (!info.isDynamic()) {
                addNonDynamicEphemeralAttr(info);
            }
        }
    }

    @VisibleForTesting
    public AttributeManager() {
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
                ZimbraLog.misc.warn("while loading attrs, ignoring not .xml file: %s", file);
                continue;
            }
            if (!file.isFile()) {
                ZimbraLog.misc.warn("while loading attrs, ignored non-file: %s", file);
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                Document doc = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(fis);
                Element root = doc.getRootElement();
                if (root.getName().equals(E_ATTRS)) {
                    loadAttrs(file, doc);
                } else if (root.getName().equals(E_OBJECTCLASSES)) {
                    loadObjectClasses(file, doc);
                } else {
                    ZimbraLog.misc.warn("while loading attrs, ignored unknown file: %s", file);
                }

            } catch (IOException | XmlParseException ex) {
                throw ServiceException.FAILURE("error loading attrs file: " + file, ex);
            }
        }
    }

    private final List<String> mErrors = new LinkedList<String>();

    boolean hasErrors() {
        return mErrors.size() > 0;
    }

    String getErrors() {
        StringBuilder result = new StringBuilder();
        for (String error : mErrors) {
            result.append(error).append("\n");
        }
        return result.toString();
    }

    // called only from AttributeManagerUtil
    Map<String, AttributeInfo> getAttrs() {
        return mAttrs;
    }

    // called only from AttributeManagerUtil
    Map<String, ObjectClassInfo> getOCs() {
        return mOCs;
    }

    // called only from AttributeManagerUtil
    Map<Integer,String> getGroupMap() {
        return mGroupMap;
    }

    // called only from AttributeManagerUtil
    Map<Integer,String> getOCGroupMap() {
        return mOCGroupMap;
    }

    private void error(String attrName, File file, String error) {
        if (attrName != null) {
            mErrors.add("attr " + attrName + " in file " + file + ": " + error);
        } else {
            mErrors.add("file " + file + ": " + error);
        }
    }

    private void loadAttrs(File file, Document doc) {
        Element root = doc.getRootElement();

        if (!root.getName().equals(E_ATTRS)) {
            error(null, file, "root tag is not " + E_ATTRS);
            return;
        }

        Map<Integer, String> idsSeen = new HashMap<Integer, String>();

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
            Version deprecatedSinceVer = null;
            List<Version> sinceVer = null;
            Boolean ephemeral = false;

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
                            deprecatedSinceVer = new Version(depreSince);
                        } catch (ServiceException e) {
                            error(name, file, aname + " is not valid: " + attr.getValue() + " (" + e.getMessage() + ")");
                        }
                    }

                } else if (aname.equals(A_SINCE)) {
                    String since = attr.getValue();
                    if (since != null) {
                        try {
                            String[] versions = since.split(",");
                            sinceVer = new ArrayList<Version>();
                            for (String verStr : versions) {
                                sinceVer.add(new Version(verStr.trim()));
                            }
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
            List<String> defaultExternalCOSValues = new LinkedList<String>();
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
                } else if (elem.getName().equals(E_DEFAULT_EXTERNAL_COS_VALUE)) {
                    defaultExternalCOSValues.add(elem.getText());
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
                error(name, file, "missing element " + E_DEPRECATE_DESC);
            else if (deprecatedSinceVer == null && deprecateDesc != null)
                error(name, file, "missing attr " + A_DEPRECATED_SINCE);

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

            // Check that if id is specified, then at least one object class is defined
            if (id > 0 && (optionalIn != null && optionalIn.isEmpty()) && (requiredIn != null && requiredIn.isEmpty())) {
                error(name, file, "atleast one of " + A_REQUIRED_IN + " or " + A_OPTIONAL_IN + " must be specified");
            }

            // Check that if id is specified, it must be unique
            if (id > 0) {
                String idForAttr = idsSeen.get(Integer.valueOf(id));
                if (idForAttr != null) {
                    error(name, file, "duplicate id: " + id + " is already used for " + idForAttr);
                } else {
                    idsSeen.put(Integer.valueOf(id), name);
                }
            }

            // Check that if it is COS inheritable it is in account and COS classes
            checkFlag(name, file, flags, AttributeFlag.accountInherited, AttributeClass.account, AttributeClass.cos, null, requiredIn, optionalIn);

            // Check that if it is COS-domain inheritable it is in account and COS and domain classes
            checkFlag(name, file, flags, AttributeFlag.accountCosDomainInherited, AttributeClass.account, AttributeClass.cos, AttributeClass.domain, requiredIn, optionalIn);

            // Check that if it is domain inheritable it is in domain and global config
            checkFlag(name, file, flags, AttributeFlag.domainInherited, AttributeClass.domain, AttributeClass.globalConfig, null, requiredIn, optionalIn);

            // Check that if it is server inheritable it is in server and global config
            checkFlag(name, file, flags, AttributeFlag.serverInherited, AttributeClass.server, AttributeClass.globalConfig, null, requiredIn, optionalIn);

            // Check that if it is serverPreferAlwaysOn it is in server and alwaysOnCluster
            checkFlag(name, file, flags, AttributeFlag.serverPreferAlwaysOn, AttributeClass.server, AttributeClass.alwaysOnCluster, null, requiredIn, optionalIn);

            // Check that is cardinality is single, then not more than one
            // default value is specified
            if (cardinality == AttributeCardinality.single) {
                if (globalConfigValues.size() > 1) {
                    error(name, file, "more than one global config value specified for cardinality " + AttributeCardinality.single);
                }
                if (defaultCOSValues.size() > 1 || defaultExternalCOSValues.size() > 1) {
                    error(name, file, "more than one default COS value specified for cardinality " + AttributeCardinality.single);
                }
            }

            checkEphemeralFlags(name, file, flags, min, max, cardinality);

            AttributeInfo info = createAttributeInfo(
                    name, id, parentOid, groupId, callback, type, order, value, immutable, min, max,
                    cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues,
                    defaultExternalCOSValues, globalConfigValuesUpgrade, defaultCOSValuesUpgrade,
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

            if (isBinaryType(type)) {
                mBinaryAttrs.add(canonicalName);
            } else if (isBinaryTransferType(type)) {
                mBinaryTransferAttrs.add(canonicalName);
            }

            if (flags != null && flags.contains(AttributeFlag.ephemeral)) {
                mEphemeralAttrs.put(canonicalName, info);
                mEphemeralAttrsSet.add(name);
                if (!info.isDynamic()) {
                    addNonDynamicEphemeralAttr(info);
                }
            }
        }
   }

    private void checkEphemeralFlags(String attrName, File file,
            Set<AttributeFlag> flags, String min, String max, AttributeCardinality cardinality) {
        if (flags == null) { return; }
        boolean isEphemeral = flags.contains(AttributeFlag.ephemeral);
        if (flags.contains(AttributeFlag.dynamic) && !isEphemeral) {
            error(attrName, file, "'dynamic' flag can only be used with ephemeral attributes");
        } else if (flags.contains(AttributeFlag.expirable) && !isEphemeral) {
            error(attrName, file, "'expirable' flag can only be used with ephemeral attributes");
        }
        if (isEphemeral) {
            if (cardinality == AttributeCardinality.multi && !flags.contains(AttributeFlag.dynamic)) {
                error(attrName, file, "multi-valued ephemeral attributes must have the 'dynamic' flag set");
            }
            Sets.SetView<AttributeFlag> diff = Sets.difference(flags, allowedEphemeralFlags);
            if (!diff.isEmpty()) {
                error(attrName, file, String.format("flags %s cannot be used with ephemeral attributes", Joiner.on(",").join(diff)));
            }
            if (!Strings.isNullOrEmpty(min)) {
                error(attrName, file, "'min' constraint cannot be used with ephemeral attributes");
            } else if (!Strings.isNullOrEmpty(max)) {
                error(attrName, file, "'max' constraint cannot be used with ephemeral attributes");
            }
        }
    }

    protected AttributeInfo createAttributeInfo(
            String name, int id, String parentOid, int groupId,
            AttributeCallback callback, AttributeType type, AttributeOrder order,
            String value, boolean immutable, String min, String max,
            AttributeCardinality cardinality, Set<AttributeClass> requiredIn,
            Set<AttributeClass> optionalIn, Set<AttributeFlag> flags,
            List<String> globalConfigValues, List<String> defaultCOSValues,
            List<String> defaultExternalCOSValues, List<String> globalConfigValuesUpgrade,
            List<String> defaultCOSValuesUpgrade, String description, List<AttributeServerType> requiresRestart,
            List<Version> sinceVer, Version deprecatedSinceVer) {
        return new AttributeInfo(
                name, id, parentOid, groupId, callback, type, order, value, immutable, min, max,
                cardinality, requiredIn, optionalIn, flags, globalConfigValues, defaultCOSValues,
                defaultExternalCOSValues, globalConfigValuesUpgrade, defaultCOSValuesUpgrade,
                description, requiresRestart, sinceVer, deprecatedSinceVer);
    }

    private enum ObjectClassType {
        ABSTRACT,
        AUXILIARY,
        STRUCTURAL;
    }

    class ObjectClassInfo {
        private final AttributeClass mAttributeClass;
        private final String mName;
        private final int mId;
        private final int mGroupId;
        private final ObjectClassType mType;
        private final List<String> mSuperOCs;
        private final String mDescription;
        private final List<String> mComment;

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

    private void loadObjectClasses(File file, Document doc) {
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
            case oauth2DataSource:
                attrs = SetUtil.union(new HashSet<String>(),
                        mClassToAttrsMap.get(AttributeClass.dataSource),
                        mClassToAttrsMap.get(AttributeClass.oauth2DataSource));
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
    private final Map<AttributeFlag, Set<String>> mFlagToAttrsMap = new HashMap<AttributeFlag, Set<String>>();

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
        none;      // attr is not of type smail, emailp, cs_emailp, nor does it have idn flag

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

    /**
     * returns whether attr is in the specified version.
     *
     * An attr is considered in a version if it is introduced prior to version
     * or on the same version.
     *
     * e.g.
     *   - if attr is introduced on 7.1.0, it is in 7.1.1
     *   - if attr is introduced on 7.1.1, it is in 7.1.1
     *   - if attr is introduced on 7.1.2, it is not in 7.1.1
     *
     * @param attr
     * @param version
     * @return
     * @throws ServiceException
     */
    public boolean inVersion(String attr, String version) throws ServiceException {
        return versionCheck(attr, version, true, true);
    }

    /**
     * returns whether attr is introduced before the specified version.
     *
     * e.g.
     *   - if attr is introduced on 7.1.0, it is before 7.1.1
     *   - if attr is introduced on 7.1.1, it is *NOT* before 7.1.1
     *   - if attr is introduced on 7.1.2, it is not before 7.1.1
     *
     * @param attr
     * @param version
     * @return
     * @throws ServiceException
     */
    public boolean beforeVersion(String attr, String version) throws ServiceException {
        return versionCheck(attr, version, false, true);
    }

    private boolean versionCheck(String attr, String version, boolean in, boolean before) throws ServiceException {
        AttributeInfo ai = mAttrs.get(attr.toLowerCase());
        if (ai != null) {
            List<Version> since = ai.getSince();
            if (since == null) {
                return true;
            } else {
                Version current = new Version(version);
                boolean good = false;
                for (Version sinceVer : since) {
                    if (current.isSameMinorRelease(sinceVer)) {
                        //ok same release; just compare
                        return (before && sinceVer.compare(version) < 0) || (in && sinceVer.compare(version) == 0);
                    } else if (!current.isLaterMajorMinorRelease(sinceVer)) {
                        //current is lower series than one item in list
                        //if it was OK from earlier series then it's OK
                        return good;
                    } else {
                        //current is later major/minor, so check in/before and iterate further
                        good = (before && sinceVer.compare(version) < 0) || (in && sinceVer.compare(version) == 0);
                    }
                }
                return good;
            }
        } else {
            throw AccountServiceException.INVALID_ATTR_NAME("unknown attribute: " + attr, null);
        }
    }

    public boolean isFuture(String attr) {
        AttributeInfo ai = mAttrs.get(attr.toLowerCase());
        return (ai != null && ai.getSince() != null && ai.getSince().size() == 1 && ai.getSince().iterator().next().isFuture());
    }

    public boolean addedIn(String attr, String version) throws ServiceException {
        return versionCheck(attr, version, true, false);
    }



    public AttributeType getAttributeType(String attr) throws ServiceException {
        AttributeInfo ai = mAttrs.get(attr.toLowerCase());
        if (ai != null)
            return ai.getType();
        else
            throw AccountServiceException.INVALID_ATTR_NAME("unknown attribute: " + attr, null);
    }

    // types need to be set in JNDI "java.naming.ldap.attributes.binary" environment property
    // when making a connection
    public static boolean isBinaryType(AttributeType type) {
        return type == AttributeType.TYPE_BINARY;
    }

    // types need the ";binary" treatment to/from the LDAP server
    // for now the only supported binary transfer type is certificate
    public static boolean isBinaryTransferType(AttributeType type) {
        return type == AttributeType.TYPE_CERTIFICATE;
    }

    public boolean containsBinaryData(String attr) {
        return mBinaryAttrs.contains(attr.toLowerCase()) ||
               mBinaryTransferAttrs.contains(attr.toLowerCase());
    }

    public boolean isBinaryTransfer(String attr) {
        return mBinaryTransferAttrs.contains(attr.toLowerCase());
    }

    public Set<String> getBinaryAttrs() {
        return mBinaryAttrs;
    }

    public Set<String> getBinaryTransferAttrs() {
        return mBinaryTransferAttrs;
    }

    public Map<String, AttributeInfo> getEphemeralAttrs() {
        return mEphemeralAttrs;
    }

    public Set<String> getEphemeralAttributeNames() {
        return mEphemeralAttrsSet;
    }

    boolean hasFlag(AttributeFlag flag, String attr) {
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
            Entry entry, CallbackContext context, boolean checkImmutable)
    throws ServiceException {
        preModify(attrs, entry, context, checkImmutable, true);
    }

    public void preModify(Map<String, ? extends Object> attrs,
            Entry entry, CallbackContext context, boolean checkImmutable, boolean allowCallback)
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
                if (info.isDeprecated()) {
                    ZimbraLog.misc.warn("Attempt to modify a deprecated attribute: " + name);
                }

                // IDN unicode to ACE conversion needs to happen before checkValue or else
                // regex attrs will be rejected by checkValue
                if (idnType(name).isEmailOrIDN()) {
                    mIDNCallback.preModify(context, name, value, attrs, entry);
                    value = attrs.get(name);
                }
                info.checkValue(value, checkImmutable, attrs);
                if (allowCallback && info.getCallback() != null) {
                    info.getCallback().preModify(context, name, value, attrs, entry);
                }
            } else {
                ZimbraLog.misc.warn("checkValue: no attribute info for: "+name);
            }
        }
    }

    public void postModify(Map<String, ? extends Object> attrs,
            Entry entry, CallbackContext context) {
        postModify(attrs, entry, context, true);
    }

    public void postModify(Map<String, ? extends Object> attrs,
            Entry entry, CallbackContext context, boolean allowCallback) {
        String[] keys = attrs.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            String name = keys[i];
//            Object value = attrs.get(name);
            if (name.charAt(0) == '-' || name.charAt(0) == '+') name = name.substring(1);
            AttributeInfo info = mAttrs.get(name.toLowerCase());
            if (info != null) {
                if (allowCallback && info.getCallback() != null) {
                    try {
                        info.getCallback().postModify(context, name, entry);
                    } catch (Exception e) {
                        // need to swallow all exceptions as postModify shouldn't throw any...
                        ZimbraLog.account.warn("postModify caught exception: "+e.getMessage(), e);
                    }
                }
            }
       }
    }


    public AttributeInfo getAttributeInfo(String name) {
        if (name == null)
            return null;
        else
            return mAttrs.get(name.toLowerCase());
    }

    public static void loadLdapSchemaExtensionAttrs(LdapProv prov) {
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

    private void getLdapSchemaExtensionAttrs(LdapProv prov) throws ServiceException {
        if (mLdapSchemaExtensionInited)
            return;

        mLdapSchemaExtensionInited = true;

        getExtraObjectClassAttrs(prov, AttributeClass.account, Provisioning.A_zimbraAccountExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.calendarResource, Provisioning.A_zimbraCalendarResourceExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.cos, Provisioning.A_zimbraCosExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.domain, Provisioning.A_zimbraDomainExtraObjectClass);
        getExtraObjectClassAttrs(prov, AttributeClass.server, Provisioning.A_zimbraServerExtraObjectClass);
    }

    private void getExtraObjectClassAttrs(LdapProv prov, AttributeClass attrClass, String extraObjectClassAttr)
    throws ServiceException {
        Config config = prov.getConfig();

        String[] extraObjectClasses = config.getMultiAttr(extraObjectClassAttr);

        if (extraObjectClasses.length > 0) {
            Set<String> attrsInOCs = mClassToAttrsMap.get(AttributeClass.account);
            prov.getAttrsInOCs(extraObjectClasses, attrsInOCs);
        }
    }
}

