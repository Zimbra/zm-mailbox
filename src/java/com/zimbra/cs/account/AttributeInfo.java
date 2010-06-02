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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.BuildInfo;

public class AttributeInfo {
    
    //  8        4  4     4      12
    //8cf3db5d-cfd7-11d9-884f-e7b38f15492d
    private static Pattern ID_PATTERN = 
        Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
                
    //yyyyMMddHHmmssZ
    private static Pattern GENTIME_PATTERN = Pattern.compile("^\\d{14}[zZ]$");

    private static Pattern DURATION_PATTERN = Pattern.compile("^\\d+([hmsd]|ms)?$");
    
    /** attribute name */
    protected String mName;
    
    /** attribute type */
    protected AttributeType mType;
    
    /** sort order */
    private AttributeOrder mOrder;
    
    /** for enums */
    private HashSet<String> mEnumSet;
    
    /** for regex */
    private Pattern mRegex;

    /** for holding initial value string */
    private String mValue;

    /** attribute callback */
    private AttributeCallback mCallback;

    /** whether this attribute can be modified directly */
    private boolean mImmutable;
    
    private AttributeCardinality mCardinality;
    
    private Set<AttributeClass> mRequiredInClasses; 

    private Set<AttributeClass> mOptionalInClasses;
    
    private Set<AttributeFlag> mFlags;
    
    private List<String> mGlobalConfigValues;
    
    private List<String> mGlobalConfigValuesUpgrade;
    
    protected List<String> mDefaultCOSValues;
    
    private List<String> mDefaultCOSValuesUpgrade;
    
    private long mMin = Long.MIN_VALUE, mMax = Long.MAX_VALUE;
    
    private String mMinDuration = null, mMaxDuration = null; 

    private int mId;
    
    private String mParentOid;
    
    private int mGroupId;
    
    private String mDescription;
    
    private List<AttributeServerType> mRequiresRestart;
    
    private BuildInfo.Version mSince;
    
    private BuildInfo.Version mDeprecatedSince;
    
    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static Long parseLong(String attrName, String propName, String value, long defaultValue) {
        if (!StringUtil.isNullOrEmpty(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to %d.",
                        value, propName, attrName, defaultValue);
                return defaultValue;
            }
        } else
            return defaultValue; 
    }

    
    protected AttributeInfo (String attrName, int id, String parentId, int groupId, AttributeCallback callback, AttributeType type,
                   AttributeOrder order, String value, boolean immutable, String min, String max, 
                   AttributeCardinality cardinality, Set<AttributeClass> requiredIn, 
                   Set<AttributeClass> optionalIn, Set<AttributeFlag> flags,
                   List<String> globalConfigValues, List<String> defaultCOSValues, 
                   List<String> globalConfigValuesUpgrade, List<String> defaultCOSValuesUpgrade, 
                   String description, List<AttributeServerType> requiresRestart,
                   BuildInfo.Version since, BuildInfo.Version deprecatedSince)
    {
        mName = attrName;
        mImmutable = immutable;
        mCallback = callback;
        mType = type;
        mOrder = order;
        mValue = value;
        mId = id;
        mParentOid = parentId;
        mGroupId = groupId;
        mCardinality = cardinality;
        mRequiredInClasses = requiredIn;
        mOptionalInClasses = optionalIn;
        mFlags = flags;
        mGlobalConfigValues = globalConfigValues;
        mGlobalConfigValuesUpgrade = globalConfigValuesUpgrade;
        mDefaultCOSValues = defaultCOSValues;
        mDefaultCOSValuesUpgrade = defaultCOSValuesUpgrade;
        mDescription = description;
        mRequiresRestart = requiresRestart;
        mSince = since;
        mDeprecatedSince = deprecatedSince;
        
        mMin = parseLong(attrName, AttributeManager.A_MIN, min, Long.MIN_VALUE);
        mMax = parseLong(attrName, AttributeManager.A_MAX, max, Long.MAX_VALUE);
        
        switch (mType) {
        case TYPE_INTEGER:
            mMin = Integer.MIN_VALUE;
            mMax = Integer.MAX_VALUE;
            
            if (!StringUtil.isNullOrEmpty(min)) {
                Integer i = parseInt(min);
                if (i == null) {
                    ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to %d.",
                        min, AttributeManager.A_MIN, attrName, mMin);
                } else {
                    mMin = i;
                }
            }
            if (!StringUtil.isNullOrEmpty(max)) {
                Integer i = parseInt(max);
                if (i == null) {
                    ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to %d.",
                        max, AttributeManager.A_MAX, attrName, mMax);
                } else {
                    mMax = i;
                }
            }
            break;
        case TYPE_LONG:
            mMin = Long.MIN_VALUE;
            mMax = Long.MAX_VALUE;
            
            if (!StringUtil.isNullOrEmpty(min)) {
                Long l = parseLong(min);
                if (l == null) {
                    ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to %d.",
                        min, AttributeManager.A_MIN, attrName, mMin);
                } else {
                    mMin = l;
                }
            }
            if (!StringUtil.isNullOrEmpty(max)) {
                Long l = parseLong(max);
                if (l == null) {
                    ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to %d.",
                        max, AttributeManager.A_MAX, attrName, mMax);
                } else {
                    mMax = l;
                }
            }
            break;
        case TYPE_ENUM:
            String enums[] = value.split(",");
            mEnumSet = new HashSet<String>(enums.length);
            for (int i=0; i < enums.length; i++) {
                mEnumSet.add(enums[i]);
            }
            break;
        case TYPE_REGEX:
            mRegex = Pattern.compile(value);
            break;
        case TYPE_DURATION:
            mMin = 0;
            mMax = Long.MAX_VALUE;
            mMinDuration = "0";
            mMaxDuration = Long.toString(mMax);
            
            if (!StringUtil.isNullOrEmpty(min)) {
                mMin = DateUtil.getTimeInterval(min, -1);
                if (mMin < 0) {
                    mMin = 0;
                    ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to 0.",
                        min, AttributeManager.A_MIN, attrName);
                } else {
                    mMinDuration = min;
                }
            }
            if (!StringUtil.isNullOrEmpty(max)) {
                mMax = DateUtil.getTimeInterval(max, -1);
                if (mMax < 0) {
                    mMax = Long.MAX_VALUE;
                    ZimbraLog.misc.warn("Invalid value '%s' for property %s of attribute %s.  Defaulting to %d.",
                        max, AttributeManager.A_MAX, attrName, mMax);
                } else {
                    mMaxDuration = max;
                }
            }
            break;
        }
            
    }

    public int getEnumValueMaxLength() {
        assert(mType == AttributeType.TYPE_ENUM);
        int max = 0;
        for (String s : mEnumSet) {
            int l = s.length();
            if (l > max) {
                max = l;
            }
        }
        return max;
    }
    
    public void checkValue(Object value, boolean checkImmutable, Map attrsToModify) throws ServiceException {
        if ((value == null) || (value instanceof String)) {
            checkValue((String) value, checkImmutable, attrsToModify);
        } else if (value instanceof String[]) {
            String[] values = (String[]) value;
            for (int i=0; i < values.length; i++)
                checkValue(values[i], checkImmutable, attrsToModify);
        }
    }

    protected void checkValue(String value, boolean checkImmutable, Map attrsToModify) throws ServiceException {
        if (checkImmutable && mImmutable)
            throw ServiceException.INVALID_REQUEST(mName+" is immutable", null);
        checkValue(value, attrsToModify);
    }

    protected void checkValue(String value, Map attrsToModify) throws ServiceException {

        // means to delete/unset the attribute
        if (value == null || value.equals(""))
            return;

        switch (mType) {
        case TYPE_BOOLEAN:
            if ("TRUE".equals(value) || "FALSE".equals(value))
                return;
            else
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be TRUE or FALSE", null);
        case TYPE_DURATION:
            if (!DURATION_PATTERN.matcher(value).matches())
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid duration: nnnn[hsmd]", null);
            long l = DateUtil.getTimeInterval(value, 0);
            if (l < mMin)
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" is shorter than minimum allowed: "+mMinDuration, null);
            if (l > mMax)
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" is longer than max allowed: "+mMaxDuration, null);
            return;
        case TYPE_EMAIL:
            if (value.length() > mMax)
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value length("+value.length()+") larger than max allowed: "+mMax, null);              
            validEmailAddress(value, false);
            return;           
        case TYPE_EMAILP:
            if (value.length() > mMax)
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value length("+value.length()+") larger than max allowed: "+mMax, null);              
            validEmailAddress(value, true);
            return;
        case TYPE_CS_EMAILP:
            if (value.length() > mMax)
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value length("+value.length()+") larger than max allowed: "+mMax, null);              
            String[] emails = value.split(",");
            for (String email : emails)
                validEmailAddress(email, true);
            return;
        case TYPE_ENUM:
            if (mEnumSet.contains(value))
                return;
            else
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be one of: "+mValue, null);
        case TYPE_GENTIME:
            if (GENTIME_PATTERN.matcher(value).matches())
                return;
            else
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid generalized time: yyyyMMddHHmmssZ", null);
        case TYPE_ID:
            // For bug 21776 we check format for id only if the Provisioning class mandates
            // that all attributes of type id must be an UUID.
            //
            if (!Provisioning.getInstance().idIsUUID())
                return;
            
            if (ID_PATTERN.matcher(value).matches())
                return;
            else
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid id", null); 
        case TYPE_INTEGER:
            try {
                int v = Integer.parseInt(value);
                if (v < mMin)
                    throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") smaller than minimum allowed: "+mMin, null);
                if (v > mMax)
                    throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") larger than max allowed: "+mMax, null);
                return;
            } catch (NumberFormatException e) {
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid integer: "+value, e);
            }
        case TYPE_LONG:
            try {
                long v = Long.parseLong(value);
                if (v < mMin)
                    throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") smaller than minimum allowed: "+mMin, null);
                if (v > mMax)
                    throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") larger than max allowed: "+mMax, null);
                return;
            } catch (NumberFormatException e) {
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid long: "+value, e);
            }
        case TYPE_PORT:
            try {
                int v = Integer.parseInt(value);
                if (v >= 0 && v <= 65535)
                    return;
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid port: "+value, null);               
            } catch (NumberFormatException e) {
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid port: "+value, null);
            }
        case TYPE_STRING:
        case TYPE_ASTRING:
        case TYPE_OSTRING:
        case TYPE_CSTRING:
        case TYPE_PHONE:
            if (value.length() > mMax)
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value length("+value.length()+") larger then max allowed: "+mMax, null);   
            // TODO
            return;
        case TYPE_REGEX:
            if (mRegex.matcher(value).matches())
                return;
            else
                throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must match the regex: "+mValue, null);
        default:
            ZimbraLog.misc.warn("unknown type("+mType+") for attribute: "+value);
            return;
        }
    }
   
    public static void validEmailAddress(String addr, boolean personal) throws ServiceException {
        if (addr.indexOf('@') == -1)
            throw AccountServiceException.INVALID_ATTR_VALUE("must include domain", null);

        try {
            InternetAddress ia = new InternetAddress(addr, true);
            // is this even needed?
            ia.validate();
            if (!personal && ia.getPersonal() != null && !ia.getPersonal().equals(""))
                throw AccountServiceException.INVALID_ATTR_VALUE("invalid email address", null);
        } catch (AddressException e) {
            throw AccountServiceException.INVALID_ATTR_VALUE("invalid email address", e);
        }
    }

    AttributeCallback getCallback() {
        return mCallback;
    }
   
    String getName() {
        return mName;
    }
   
    boolean hasFlag(AttributeFlag flag) {
        if (mFlags == null) {
            return false;
        }
        boolean result = mFlags.contains(flag);
        return result;
    }
   
    int getId() {
        return mId;
    }

    Set<String> getEnumSet() {
        return mEnumSet;
    }
   
    String getParentOid() {
        return mParentOid;
    }
   
    int getGroupId() {
        return mGroupId;
    }
   
    AttributeType getType() {
        return mType;
    }

    AttributeOrder getOrder() {
 	   return mOrder;
    }
   
    String getDescription() {
        return mDescription;
    }

    long getMax() {
        return mMax;
    }
    
    long getMin() {
        return mMin;
    }
   
    boolean requiredInClass(AttributeClass cls) {
        return mRequiredInClasses != null && mRequiredInClasses.contains(cls);
    }

    boolean optionalInClass(AttributeClass cls) {
        return mOptionalInClasses != null && mOptionalInClasses.contains(cls);
    }
    
    Set<AttributeClass> getRequiredIn() {
        return mRequiredInClasses;
    }
    
    Set<AttributeClass> getOptionalIn() {
        return mOptionalInClasses;
    }

    public AttributeCardinality getCardinality() {
        return mCardinality;
    }
   
    public List<String> getGlobalConfigValues() {
        return mGlobalConfigValues;
    }
    
    public List<String> getGlobalConfigValuesUpgrade() {
        return mGlobalConfigValuesUpgrade;
    }
   
    public List<String> getDefaultCosValues() {
        return mDefaultCOSValues;
    }
    
    public List<String> getDefaultCosValuesUpgrade() {
        return mDefaultCOSValuesUpgrade;
    }

    boolean isImmutable() {
        return mImmutable;
    }
    
    String getValue() {
        return mValue;
    }
    
    public List<AttributeServerType> getRequiresRestart() {
        return mRequiresRestart;
    }
    
    public BuildInfo.Version getSince() {
        return mSince;
    }
    
    public BuildInfo.Version getDeprecatedSince() {
        return mDeprecatedSince;
    }
}
