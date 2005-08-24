/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class AttributeInfo {
    
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_DURATION = 2;
    public static final int TYPE_EMAIL = 3;
    public static final int TYPE_ENUM = 4;
    public static final int TYPE_GENTIME = 5; // ldap generalized time
    public static final int TYPE_ID = 6;
    public static final int TYPE_INTEGER = 7;
    public static final int TYPE_PORT = 8;
    public static final int TYPE_STRING = 9;  
    public static final int TYPE_REGEX = 10;
    public static final int TYPE_EMAILP = 11;
    public static final int TYPE_LONG = 12;
    
    public static final String S_TYPE_BOOLEAN = "boolean";
    public static final String S_TYPE_DURATION = "duration";
    public static final String S_TYPE_GENTIME = "gentime";    
    public static final String S_TYPE_EMAIL = "email";
    public static final String S_TYPE_EMAILP = "emailp";    
    public static final String S_TYPE_ENUM = "enum";
    public static final String S_TYPE_ID = "id";
    public static final String S_TYPE_INTEGER = "integer";
    public static final String S_TYPE_PORT = "port";
    public static final String S_TYPE_STRING = "string";
    public static final String S_TYPE_REGEX = "regex";
    public static final String S_TYPE_LONG = "long";    

    public static int TYPE_UNKNOWN = -1;        
    
    //  8        4  4     4      12
    //8cf3db5d-cfd7-11d9-884f-e7b38f15492d
    private static Pattern ID_PATTERN = 
        Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
                
    //yyyyMMddHHmmssZ
    private static Pattern GENTIME_PATTERN = Pattern.compile("^\\d{14}[zZ]$");

    private static Pattern DURATION_PATTERN = Pattern.compile("^\\d+[hmsd]?$");
    
    private static HashMap mTypeMap = new HashMap();
    
    /** attribute name */
    private String mName;
    
    /** attribute type */
    private int mType;
    
    /** for enums */
    private HashSet mEnumSet;
    
    /** for regex */
    private Pattern mRegex;

    /** for holding initial value string */
    private String mValue;

    /** attribute callback */
    private AttributeCallback mCallback;

    private boolean mImmutable;

    private long mMin = Long.MIN_VALUE, mMax = Long.MAX_VALUE;

    static int parseLong(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public AttributeInfo (String attrName, AttributeCallback callback, int type, String value, boolean immutable, 
            long min, long max)
    {
        mName = attrName;
        mImmutable = immutable;
        mCallback = callback;
        mType = type;
        mValue = value;
        mMin = min;
        mMax = max;
        switch (mType) {
        case TYPE_INTEGER:
            if (mMin < Integer.MIN_VALUE) mMin = Integer.MIN_VALUE;
            if (mMax > Integer.MAX_VALUE) mMax = Integer.MAX_VALUE;            
            break;
        case TYPE_ENUM:
            String enums[] = value.split(",");
            mEnumSet = new HashSet(enums.length);
            for (int i=0; i < enums.length; i++) {
                mEnumSet.add(enums[i]);
            }
            break;
        case TYPE_REGEX:
            mRegex = Pattern.compile(value);
            break;
        }
    }

    public static int getType(String type) {
       Integer value = (Integer) mTypeMap.get(type);
       return value == null ? TYPE_UNKNOWN : value.intValue();
   }

   private static int initType(String name, int value) {
       mTypeMap.put(name, new Integer(value));
       return value;
   }

   static {
       initType(S_TYPE_BOOLEAN, TYPE_BOOLEAN);
       initType(S_TYPE_DURATION, TYPE_DURATION);
       initType(S_TYPE_EMAIL, TYPE_EMAIL);
       initType(S_TYPE_EMAILP, TYPE_EMAILP);       
       initType(S_TYPE_ENUM, TYPE_ENUM);
       initType(S_TYPE_GENTIME, TYPE_GENTIME);       
       initType(S_TYPE_ID, TYPE_ID);
       initType(S_TYPE_INTEGER, TYPE_INTEGER);
       initType(S_TYPE_PORT, TYPE_PORT);
       initType(S_TYPE_STRING, TYPE_STRING);
       initType(S_TYPE_REGEX, TYPE_REGEX);
   }

   public void checkValue(Object value, boolean checkImmutable) throws ServiceException
   {
       if ((value == null) || (value instanceof String)) {
           checkValue((String) value, checkImmutable);
       } else if (value instanceof String[]) {
           String[] values = (String[]) value;
           for (int i=0; i < values.length; i++)
               checkValue(values[i], checkImmutable);
       }
   }

   private void checkValue(String value, boolean checkImmutable) throws ServiceException
   {
       if (checkImmutable && mImmutable)
           throw ServiceException.INVALID_REQUEST(mName+" is immutable", null);

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
           if (DURATION_PATTERN.matcher(value).matches())
               return;
           else
               throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid duration: nnnn[hsmd]", null);
       case TYPE_EMAIL:
           if (value.length() > mMax)
               throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value length("+value.length()+") larger then max allowed: "+mMax, null);              
           validEmailAddress(value, false);
           return;           
       case TYPE_EMAILP:
           if (value.length() > mMax)
               throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value length("+value.length()+") larger then max allowed: "+mMax, null);              
           validEmailAddress(value, true);
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
           if (ID_PATTERN.matcher(value).matches())
               return;
           else
               throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid id", null);           
       case TYPE_INTEGER:
           try {
               int v = Integer.parseInt(value);
               if (v < mMin)
                   throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") smaller then minimum allowed: "+mMin, null);
               if (v > mMax)
                   throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") larger then max allowed: "+mMax, null);
               return;
           } catch (NumberFormatException e) {
               throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid integer: "+value, e);
           }
       case TYPE_LONG:
           try {
               long v = Long.parseLong(value);
               if (v < mMin)
                   throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") smaller then minimum allowed: "+mMin, null);
               if (v > mMax)
                   throw AccountServiceException.INVALID_ATTR_VALUE(mName+" value("+v+") larger then max allowed: "+mMax, null);
               return;
           } catch (NumberFormatException e) {
               throw AccountServiceException.INVALID_ATTR_VALUE(mName+" must be a valid integer: "+value, e);
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
   
   private static void validEmailAddress(String addr, boolean personal) throws ServiceException {
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

   public AttributeCallback getCallback() {
       return mCallback;
   }
   
   public String getName() {
       return mName;
   }
   
   public int getType() {
       return mType;
   }
}
