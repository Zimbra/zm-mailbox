/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jul 8, 2005
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;

/**
 * @author dkarp
 */
public class MetadataList {
    int  mVersion;
    List mList;

    public MetadataList()           { mList = new ArrayList(); }
    public MetadataList(List list)  { this(list, Metadata.CURRENT_METADATA_VERSION); }
    MetadataList(List list, int version)  { mList = new ArrayList(list);  mVersion = version; }

    public MetadataList(String encoded) throws ServiceException {
        try {
            mList = (List) BEncoding.decode(encoded);
        } catch (BEncodingException e) {
            throw ServiceException.FAILURE("error decoding list metadata: " + encoded, e);
        }
    }

    public int size()         { return mList.size(); }
    public boolean isEmpty()  { return mList.isEmpty(); }
    public int getVersion()   { return mVersion; }

    public MetadataList copy(MetadataList source)  { mList.addAll(source.mList);  return this; }
    public List asList() {
        ArrayList list = new ArrayList();
        for (int i = 0; i < mList.size(); i++) {
            Object obj = mList.get(i);
            if (obj == null)               continue;
            else if (obj instanceof Map)   list.add(new Metadata((Map) obj, mVersion));
            else if (obj instanceof List)  list.add(new MetadataList((List) obj, mVersion));
            else                           list.add(obj);
        }
        return list;
    }

    public MetadataList add(Object value)       { if (value != null) mList.add(value);        return this; }
    public MetadataList add(long value)         { mList.add(new Long(value));                 return this; }
    public MetadataList add(double value)       { mList.add(new Double(value));               return this; }
    public MetadataList add(boolean value)      { mList.add(new Boolean(value));              return this; }
    public MetadataList add(Metadata value)     { if (value != null) mList.add(value.mMap);   return this; }
    public MetadataList add(MetadataList value) { if (value != null) mList.add(value.mList);  return this; }

    public void remove(int index)    { if (index < mList.size()) mList.remove(index); }
    public void remove(Object value) { mList.remove(value); }
    
    public String get(int index) throws ServiceException        { Object obj = mList.get(index);  return checkNull(index, obj).toString(); }
    public long getLong(int index) throws ServiceException      { return parseLong(index, get(index)); }
    public double getDouble(int index) throws ServiceException  { return parseDouble(index, get(index)); }
    public boolean getBool(int index) throws ServiceException   { return parseBool(index, get(index)); }

    public MetadataList getList(int index) throws ServiceException {
        Object value = mList.get(index);
        if (value instanceof List)  return new MetadataList((List) value, mVersion);
        throw ServiceException.INVALID_REQUEST("invalid/null value for index: " + index, null);
    }
    public Metadata getMap(int index) throws ServiceException {
        Object value = mList.get(index);
        if (value instanceof Map)  return new Metadata((Map) value, mVersion);
        throw ServiceException.INVALID_REQUEST("invalid/null value for attribute: " + index, null);
    }

    private static Object checkNull(int index, Object value) throws ServiceException {
        if (value == null)
            throw ServiceException.INVALID_REQUEST("null element in list: " + index, null);
        return value;
    }
    public static long parseLong(int index, String value) throws ServiceException {
        try { return Long.parseLong(value); }
        catch (NumberFormatException nfe) { throw ServiceException.INVALID_REQUEST("invalid value for index: " + index, nfe); }
    }
    public static double parseDouble(int index, String value) throws ServiceException {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException nfe) { throw ServiceException.INVALID_REQUEST("invalid value for index: " + index, nfe); }
    }
    public static boolean parseBool(int index, String value) throws ServiceException {
        if (value.equals("1") || value.equalsIgnoreCase("true"))        return true;
        else if (value.equals("0") || value.equalsIgnoreCase("false"))  return false;
        throw ServiceException.INVALID_REQUEST("invalid boolean value for index: " + index, null);
    }

    public String toString()  { return BEncoding.encode(mList); }
}
