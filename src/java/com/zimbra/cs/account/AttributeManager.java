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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    
    private static AttributeManager mInstance;

    public enum AttributeCardinality {
    	single, multi;
    }
    
    public enum AttributeClass {
		mailrecipient, account, alias, dl, cos, config, domain, group, server, mimeentry, objectentry, timezone, zimletentry, calresource;
	}
    
    public enum AttributeFlag {
        accountInfo, inheritFromCOS, domainAdminModifiable, inheritFromDomain, inheritFromServer
    }
    
    private HashMap mAttrs = new HashMap();

    public static AttributeManager getInstance() {
        if (mInstance == null) synchronized(AttributeManager.class) {
            if (mInstance == null)
                mInstance = new AttributeManager();
                String file = ZIMBRA_ATTRS_RESOURCE;
                
                InputStream is = null;
                try {
                    is = mInstance.getClass().getResourceAsStream(file);
                    if (is == null) {
                        ZimbraLog.misc.warn("unable to find attr file resource: "+file);
                    } else {
                        mInstance.loadAttrs(is, file);
                    }
                } catch (DocumentException e) {
                    ZimbraLog.misc.warn("unable to parse attr file: "+file+" "+e.getMessage(), e);
                } catch (Exception e) {
                    // swallow all of them
                    ZimbraLog.misc.warn("unable to load attr file: "+file+" "+e.getMessage(), e);                    
                } finally {
                    if (is != null)
                        try { is.close();}  catch (IOException e) { }
                }
        }
        return mInstance;
    }

    private void loadAttrs(InputStream attrsFile, String file) throws DocumentException {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(attrsFile);
            Element root = doc.getRootElement();
            if (!root.getName().equals(E_ATTRS)) {
                throw new DocumentException("attr file " + file + " root tag is not "+E_ATTRS);
            }
            for (Iterator iter = root.elementIterator(); iter.hasNext();) {
                Element eattr = (Element) iter.next();
                if (!eattr.getName().equals(E_ATTR)) {
                    ZimbraLog.misc.warn("attrs file("+file+") unknown element: "+eattr.getName());
                    continue;
                }
                String name = null;
                AttributeCallback callback = null;
                int type = AttributeInfo.TYPE_UNKNOWN;
                String value = null;
                long min = Long.MIN_VALUE;
                long max = Long.MAX_VALUE;
                boolean immutable = false;
                boolean ignore = false;
                int id = -1;
                AttributeCardinality cardinality = null;
                List<AttributeClass> requiredIn = null;
                List<AttributeClass> optionalIn = null;
                List<AttributeFlag> flags = null;

                for (Iterator attrIter = eattr.attributeIterator(); attrIter.hasNext();) {
                    Attribute attr = (Attribute) attrIter.next();
                    String aname = attr.getName();
                    if (aname.equals(A_NAME)) {
                        name = attr.getValue().toLowerCase();
                    } else if (aname.equals(A_CALLBACK)) {
                        callback = loadCallback(attr.getValue());
                    } else if (aname.equals(A_IMMUTABLE)) {
                        immutable = "1".equals(attr.getValue());
                    } else if (aname.equals(A_MAX)) {
                        max = AttributeInfo.parseLong(attr.getValue(), Integer.MAX_VALUE);
                    } else if (aname.equals(A_MIN)) {
                        min = AttributeInfo.parseLong(attr.getValue(), Integer.MIN_VALUE);
                    } else if (aname.equals(A_TYPE)) {
                         type = AttributeInfo.getType(attr.getValue());
                         if (type == AttributeInfo.TYPE_UNKNOWN) {
                             ZimbraLog.misc.warn("attrs file("+file+") unknown <attr> type: "+attr.getValue());
                             ignore = true;
                         }
                    } else if (aname.equals(A_VALUE)) { 
                        value = attr.getValue();
                    } else if (aname.equals(A_ID)) {
                    	try {
                    		id = Integer.parseInt(attr.getValue());
                    	} catch (NumberFormatException nfe) {
                    		throw new DocumentException("attrs file("+file+") " + aname + " is not a number: " + attr.getValue());
                    	}
                    } else if (aname.equals(A_CARDINALITY)) {
                    	try {
                    		cardinality = AttributeCardinality.valueOf(attr.getValue());
                    	} catch (IllegalArgumentException iae) {
                    		throw new DocumentException("attrs file("+file+") " + aname + " is not valid: " + attr.getValue());
                     	}
                    } else if (aname.equals(A_REQUIRED_IN)) {
                    	 requiredIn = getAttributeClasses(file, aname, attr.getValue());
                    } else if (aname.equals(A_OPTIONAL_IN)) {
                        optionalIn = getAttributeClasses(file, aname, attr.getValue());
                    } else if (aname.equals(A_FLAGS)) {
                        flags = getAttributeFlags(file, aname, attr.getValue());
                    } else {
                        ZimbraLog.misc.warn("attrs file("+file+") unknown <attr> attr: "+aname);
                    }
                }

                if (!ignore) {
                    if (name == null) {
                        ZimbraLog.misc.warn("attrs file("+file+") no name specified for attr");
                        continue;
                    }
                    if (type == AttributeInfo.TYPE_UNKNOWN) {
                        ZimbraLog.misc.warn("attrs file("+file+") no type specified for attr: "+name);
                        continue;
                    }
                    AttributeInfo info = new AttributeInfo(name, id, callback, type, value, immutable, min, max);
                    mAttrs.put(name, info);
                }
            }
    }
    
    private static List<AttributeClass> getAttributeClasses(String file, String attr, String value) throws DocumentException {
        List<AttributeClass> result = new LinkedList<AttributeClass>();
        String[] cnames = value.split(",");
        for (String cname : cnames) {
            try {
                AttributeClass ac = AttributeClass.valueOf(cname);
                result.add(ac);
            } catch (IllegalArgumentException iae) {
                throw new DocumentException("attrs file("+file+") " + attr + " invalid class: " + value);
            }
        }
        return result;
    }

    private static List<AttributeFlag> getAttributeFlags(String file, String attr, String value) throws DocumentException {
        List<AttributeFlag> result = new LinkedList<AttributeFlag>();
        String[] flags = value.split(",");
        for (String flag : flags) {
            try {
                AttributeFlag ac = AttributeFlag.valueOf(flag);
                result.add(ac);
            } catch (IllegalArgumentException iae) {
                throw new DocumentException("attrs file("+file+") " + attr + " invalid flag: " + value);
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

    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup("INFO");
        AttributeManager mgr = AttributeManager.getInstance();
        HashMap attrs = new HashMap();
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        attrs.put(Provisioning.A_zimbraImapBindPort, "143");
        attrs.put("xxxzimbraImapBindPort", "143");
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, null);
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, "FALSE");
        Map context = new HashMap();
        mgr.preModify(attrs, null, context, false, true);
        // modify
        mgr.postModify(attrs, null, context, false);
    }
}
