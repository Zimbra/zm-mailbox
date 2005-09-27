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

package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.BEncoding;
import com.zimbra.cs.util.BlobMetaData;
import com.zimbra.cs.util.BlobMetaDataEncodingException;
import com.zimbra.cs.util.BEncoding.BEncodingException;

public class Metadata {

    /** Current Metadata version number.  Note that all MailItems have NOT
     *  been updated to check the version number, so make sure to look at
     *  the code if you depend on this. */
    static final int CURRENT_METADATA_VERSION = 9;
    
    // MetaData attributes used in toplevel metadata for MailItems.
    
    // ****PLEASE KEEP THESE IN SORTED ORDER TO MAKE IT EASIER TO AVOID DUPS****

    // FIXME: FRAGMENT and FIRST conflict.
    // FIXME: LOCATION and BOUNDS conflict
    // FIXME: SENDER and SORT conflict
    // FIXME: RECIPIENTS and TYPES conflict
    static final String FN_ATTRS           = "a";
    static final String FN_RIGHTS          = "acl";
    static final String FN_APPT_FLAGS      = "af";
    static final String FN_ACCOUNT_ID      = "aid";
    static final String FN_APPT_IDS        = "ais";
    static final String FN_APPT_END        = "ape";
    static final String FN_APPT_START      = "aps";
    static final String FN_ATTENDEE        = "at";
    static final String FN_COLOR           = "c";
    static final String FN_COMPONENT       = "comp";
    static final String FN_MIME_TYPE       = "ct";
    static final String FN_DRAFT           = "d";
    static final String FN_REPLY_ORIG      = "do";
    static final String FN_REPLY_TYPE      = "dt";
    static final String FN_DTSTAMP         = "dts";
    static final String FN_DURATION        = "duration";
    static final String FN_ENTRIES         = "en";
    static final String FN_END             = "et";
    static final String FN_FRAGMENT        = "f";
    static final String FN_FIRST           = "f";
    static final String FN_APPT_FREEBUSY   = "fb";
    static final String FN_FIELDS          = "fld";
    static final String FN_IMAP_ID         = "i4";
    static final String FN_REMOTE_ID       = "id";
    static final String FN_INV             = "inv";
    static final String FN_LOCATION        = "l";
    static final String FN_BOUNDS          = "l";
    static final String FN_METHOD          = "mthd";
    static final String FN_NAME            = "n";
    static final String FN_NUM_COMPONENTS  = "nc";
    static final String FN_NODES           = "no";
    static final String FN_NUM_ATTENDEES   = "numAt";
    static final String FN_ORGANIZER       = "org";
    static final String FN_PREFIX          = "p";
    static final String FN_PARTSTAT        = "ptst";
    static final String FN_QUERY           = "q";
    static final String FN_RAW_SUBJ        = "r";
    static final String FN_RECUR_ID        = "rid";
    static final String FN_REPLY_TO        = "rt";
    static final String FN_SENDER          = "s";
    static final String FN_SORT            = "s";
    static final String FN_SEQ_NO          = "seq";
    static final String FN_SENDER_LIST     = "sl";
    static final String FN_START           = "st";
    static final String FN_STATUS          = "status";  // calendar: event/todo/journal status
    static final String FN_RECIPIENTS      = "t";
    static final String FN_TYPES           = "t";
    static final String FN_TRANSP          = "tr";
    static final String FN_TZMAP           = "tzm"; // calendaring: timezone map
    static final String FN_UID             = "u";
    static final String FN_MD_VERSION      = "v"; // metadata version
    static final String FN_VIEW            = "vt";
    static final String FN_ELIDED          = "X";


    int mVersion = CURRENT_METADATA_VERSION;
    Map mMap;

    public Metadata()         { mMap = new TreeMap(); }
    public Metadata(Map map)  { this(map, CURRENT_METADATA_VERSION); }
    Metadata(Map map, int version)  { mMap = new TreeMap(map);  mVersion = version; }

    public Metadata(String encoded) throws ServiceException {
        this(encoded, null);
    }
    public Metadata(String encoded, MailItem item) throws ServiceException  {
        if (encoded == null || encoded.equals("")) {
            mMap = new HashMap();
            return;
        }
        try {
            mMap = (Map) BEncoding.decode(encoded);
            return;
        } catch (BEncodingException e) {
            try {
                mMap = BlobMetaData.decodeRecursive(encoded);
                return;
            } catch (BlobMetaDataEncodingException e1) { }
            String message = "error decoding " + (item == null ? "" : MailItem.getNameForType(item) + ' ' + item.getId() + ' ') + "metadata: " + encoded;
            throw ServiceException.FAILURE(message, e);
        } finally {
            if (mMap != null && mMap.containsKey(FN_MD_VERSION)) {
                try {
                    mVersion = (int) getLong(FN_MD_VERSION, CURRENT_METADATA_VERSION);
                    mMap.remove(FN_MD_VERSION);
                } catch (Exception e) { }
            } else {
                mVersion = 1; // if no version is encoded, assume oldest version, or "1" 
            }
        }
    }
    
    public boolean containsKey(String key) {
        return mMap.containsKey(key);
    }

    public int size()         { return mMap.size(); }
    public boolean isEmpty()  { return mMap.isEmpty(); }
    public int getVersion()   { return mVersion; }

    public Metadata copy(Metadata source)  { if (source != null) mMap.putAll(source.mMap);  return this; }
    public Map asMap()  {
        HashMap map = new HashMap();
        for (Iterator it = mMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey(), value = entry.getValue();
            if (key == null || value == null)  continue;
            else if (value instanceof Map)     map.put(key.toString(), new Metadata((Map) value, mVersion));
            else if (value instanceof List)    map.put(key.toString(), new MetadataList((List) value, mVersion));
            else                               map.put(key.toString(), value);
        }
        return map;
    }

    public Metadata remove(String key)  { mMap.remove(key);  return this; }

    public Metadata put(String key, Object value)       { if (key != null && value != null) mMap.put(key, value);        return this; }
    public Metadata put(String key, long value)         { if (key != null) mMap.put(key, new Long(value));               return this; }
    public Metadata put(String key, double value)       { if (key != null) mMap.put(key, new Double(value));             return this; }
    public Metadata put(String key, boolean value)      { if (key != null) mMap.put(key, new Boolean(value));            return this; }
    public Metadata put(String key, Metadata value)     { if (key != null && value != null) mMap.put(key, value.mMap);   return this; }
    public Metadata put(String key, MetadataList value) { if (key != null && value != null) mMap.put(key, value.mList);  return this; }

    public String get(String key) throws ServiceException       { return Element.checkNull(key, get(key, null)); }
    public long getLong(String key) throws ServiceException     { return Element.parseLong(key, Element.checkNull(key, get(key, null))); }
    public double getDouble(String key) throws ServiceException { return Element.parseDouble(key, Element.checkNull(key, get(key, null))); }
    public boolean getBool(String key) throws ServiceException  { return Element.parseBool(key, Element.checkNull(key, get(key, null))); }

    public String get(String key, String defaultValue)  { Object value = mMap.get(key);  return (value == null ? defaultValue : value.toString()); }
    public long getLong(String key, long defaultValue) throws ServiceException        { String raw = get(key, null); return (raw == null ? defaultValue : Element.parseLong(key, raw)); }
    public double getDouble(String key, double defaultValue) throws ServiceException  { String raw = get(key, null); return (raw == null ? defaultValue : Element.parseDouble(key, raw)); }
    public boolean getBool(String key, boolean defaultValue) throws ServiceException  { String raw = get(key, null); return (raw == null ? defaultValue : Element.parseBool(key, raw)); }

    public MetadataList getList(String key) throws ServiceException { return getList(key, false); }
    public MetadataList getList(String key, boolean nullOK) throws ServiceException {
        Object value = mMap.get(key);
        if (nullOK && value == null)  return null;
        if (value instanceof List)    return new MetadataList((List) value, mVersion);
        throw ServiceException.INVALID_REQUEST("invalid/missing value for attribute: " + key, null);
    }
    public Metadata getMap(String key) throws ServiceException { return getMap(key, false); }
    public Metadata getMap(String key, boolean nullOK) throws ServiceException {
        Object value = mMap.get(key);
        if (nullOK && value == null)  return null;
        if (value instanceof Map)     return new Metadata((Map) value, mVersion);
        throw ServiceException.INVALID_REQUEST("invalid/missing value for attribute: " + key, null);
    }

    public String toString() {
        put(FN_MD_VERSION, mVersion);  String result = BEncoding.encode(mMap);
        mMap.remove(FN_MD_VERSION);    return result;
    }
}
