/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.BlobMetaDataEncodingException;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.common.soap.Element;

public final class Metadata {

    /** Current Metadata version number.  Note that all MailItems have NOT
     *  been updated to check the version number, so make sure to look at
     *  the code if you depend on this. */
    public static final int CURRENT_METADATA_VERSION = 10;

    // MetaData attributes used in toplevel metadata for MailItems.

    // ****PLEASE KEEP THESE IN SORTED ORDER TO MAKE IT EASIER TO AVOID DUPS****

    // FIXME: FRAGMENT and FIRST conflict.
    // FIXME: SENDER and SORT conflict
    // FIXME: RECIPIENTS and TYPES conflict
    static final String FN_ATTRS           = "a";
    static final String FN_RIGHTS          = "acl";
    static final String FN_ALARM_DATA      = "ad";
    static final String FN_ACCOUNT_ID      = "aid";
    static final String FN_CALITEM_IDS     = "ais";
    static final String FN_CALITEM_END     = "ape";
    static final String FN_CALITEM_START   = "aps";
    static final String FN_ATTACHMENTS     = "att";
    static final String FN_COLOR           = "c";
    static final String FN_CAL_INTENDED_FOR = "cif";
    static final String FN_COMPONENT       = "comp";
    static final String FN_CREATOR         = "cr";
    static final String FN_MIME_TYPE       = "ct";
    static final String FN_DRAFT           = "d";
    static final String FN_DESCRIPTION     = "de";
    static final String FN_DESC_ENABLED    = "dee";
    static final String FN_REPLY_ORIG      = "do";
    static final String FN_REPLY_TYPE      = "dt";
    static final String FN_AUTO_SEND_TIME  = "ast";
    static final String FN_ENTRIES         = "en";
    static final String FN_FRAGMENT        = "f";
    static final String FN_FIRST           = "f";
    static final String FN_FIELDS          = "fld";
//    static final String FN_IMAP_ID         = "i4";
    static final String FN_DELETED         = "i4d";
    static final String FN_DELETED_UNREAD  = "i4du";
    static final String FN_RECENT          = "i4l";
    static final String FN_RECENT_CUTOFF   = "i4r";
    static final String FN_REMOTE_ID       = "id";
    static final String FN_IDENTITY_ID     = "idnt";
    static final String FN_INV             = "inv";
    static final String FN_BOUNDS          = "l";
    static final String FN_LAST_DATE       = "ld";
    static final String FN_LOCK_OWNER      = "lo";
    static final String FN_LOCK_TIMESTAMP  = "lt";
    static final String FN_MODSEQ          = "mseq";
    static final String FN_NUM_COMPONENTS  = "nc";
    static final String FN_NODES           = "no";
    static final String FN_PREFIX          = "p";
    static final String FN_PARTICIPANTS    = "prt";
    static final String FN_QUERY           = "q";
    static final String FN_RAW_SUBJ        = "r";
    static final String FN_REV_DATE        = "rd";
    static final String FN_REVISIONS       = "rev";
    static final String FN_REV_ID          = "rid";
    static final String FN_REPLY_LIST      = "rl";
    static final String FN_REV_SIZE        = "rs";
    static final String FN_REPLY_TO        = "rt";
    static final String FN_SENDER          = "s";
    static final String FN_SORT            = "s";
    static final String FN_SYNC_DATE       = "sd";
    static final String FN_SYNC_GUID       = "sg";
//    static final String FN_SENDER_LIST     = "sl";
    static final String FN_TOTAL_SIZE      = "sz";
    static final String FN_RECIPIENTS      = "t";
    static final String FN_TYPES           = "t";
    static final String FN_TZMAP           = "tzm"; // calendaring: timezone map
    static final String FN_UID             = "u";
    static final String FN_USER_AGENT      = "ua";
    static final String FN_UIDNEXT         = "unxt";
    static final String FN_URL             = "url";
    static final String FN_MD_VERSION      = "v"; // metadata version
    static final String FN_VERSION         = "ver";
    static final String FN_VIEW            = "vt";
    static final String FN_WIKI_WORD       = "ww";
    static final String FN_ELIDED          = "X";
    static final String FN_EXTRA_DATA      = "xd";


    private int version = CURRENT_METADATA_VERSION;
    Map<Object, Object> map;

    public Metadata() {
        map = new TreeMap<Object, Object>();
    }

    public Metadata(Map<?, ?> map) {
        this(map, CURRENT_METADATA_VERSION);
    }

    Metadata(Map<?, ?> map, int version) {
        this.map = new TreeMap<Object, Object>(map);
        this.version = version;
    }

    public Metadata(String encoded) throws ServiceException {
        this(encoded, null);
    }

    public Metadata(String encoded, MailItem item) throws ServiceException  {
        if (Strings.isNullOrEmpty(encoded)) {
            map = new HashMap<Object, Object>();
            return;
        }
        try {
            map = BEncoding.decode(encoded);
            return;
        } catch (BEncodingException e) {
            try {
                map = BlobMetaData.decodeRecursive(encoded);
                return;
            } catch (BlobMetaDataEncodingException e1) {
            }
            throw ServiceException.FAILURE("error decoding " +
                    (item == null ? "" : item.getType() + " " + item.getId() + ' ') + "metadata: " + encoded, e);
        } finally {
            if (map != null && map.containsKey(FN_MD_VERSION)) {
                try {
                    version = (int) getLong(FN_MD_VERSION, CURRENT_METADATA_VERSION);
                    map.remove(FN_MD_VERSION);
                } catch (Exception e) {
                }
            } else {
                version = 1; // if no version is encoded, assume oldest version, or "1"
            }
        }
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int getVersion() {
        return version;
    }

    public Metadata copy(Metadata source) {
        if (source != null) {
            map.putAll(source.map);
        }
        return this;
    }

    public Map<String, ?> asMap()  {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                continue;
            } else if (value instanceof Map) {
                result.put(key.toString(), new Metadata((Map<?, ?>) value, version));
            } else if (value instanceof List) {
                result.put(key.toString(), new MetadataList((List<?>) value, version));
            } else {
                result.put(key.toString(), value);
            }
        }
        return result;
    }

    public Metadata remove(String key) {
        map.remove(key);
        return this;
    }

    public Metadata put(String key, Object value) {
        if (key != null && value != null) {
            map.put(key, value);
        }
        return this;
    }

    public Metadata put(String key, long value) {
        if (key != null) {
            map.put(key, new Long(value));
        }
        return this;
    }

    public Metadata put(String key, double value) {
        if (key != null) {
            map.put(key, new Double(value));
        }
        return this;
    }

    public Metadata put(String key, boolean value) {
        if (key != null) {
            map.put(key, new Boolean(value));
        }
        return this;
    }

    public Metadata put(String key, Metadata value) {
        if (key != null && value != null) {
            map.put(key, value.map);
        }
        return this;
    }

    public Metadata put(String key, MetadataList value) {
        if (key != null && value != null) {
            map.put(key, value.list);
        }
        return this;
    }

    public String get(String key) throws ServiceException {
        return Element.checkNull(key, get(key, null));
    }

    public long getLong(String key) throws ServiceException {
        return Element.parseLong(key, Element.checkNull(key, get(key, null)));
    }

    public double getDouble(String key) throws ServiceException {
        return Element.parseDouble(key, Element.checkNull(key, get(key, null)));
    }

    public boolean getBool(String key) throws ServiceException {
        return Element.parseBool(key, Element.checkNull(key, get(key, null)));
    }

    public String get(String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : value.toString();
    }

    public long getLong(String key, long defaultValue) throws ServiceException {
        String raw = get(key, null);
        return raw == null ? defaultValue : Element.parseLong(key, raw);
    }

    public int getInt(String key, int defaultValue) throws ServiceException {
        String raw = get(key, null);
        return raw == null ? defaultValue : Element.parseInt(key, raw);
    }

    public double getDouble(String key, double defaultValue) throws ServiceException {
        String raw = get(key, null);
        return raw == null ? defaultValue : Element.parseDouble(key, raw);
    }

    public boolean getBool(String key, boolean defaultValue) throws ServiceException {
        String raw = get(key, null);
        return raw == null ? defaultValue : Element.parseBool(key, raw);
    }

    public MetadataList getList(String key) throws ServiceException {
        return getList(key, false);
    }

    public MetadataList getList(String key, boolean nullOK) throws ServiceException {
        Object value = map.get(key);
        if (nullOK && value == null) {
            return null;
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> cast = (List<Object>) value;
            return new MetadataList(cast, version);
        }
        throw ServiceException.INVALID_REQUEST("invalid/missing value for attribute: " + key, null);
    }

    public Metadata getMap(String key) throws ServiceException {
        return getMap(key, false);
    }

    public Metadata getMap(String key, boolean nullable) throws ServiceException {
        Object value = map.get(key);
        if (nullable && value == null) {
            return null;
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) value;
            return new Metadata(cast, version);
        }
        throw ServiceException.INVALID_REQUEST("invalid/missing value for attribute: " + key, null);
    }

    @Override
    public String toString() {
        put(FN_MD_VERSION, version);
        String result = BEncoding.encode(map);
        map.remove(FN_MD_VERSION);
        return result;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("MetaData version = ").append(version).append("\n");
        prettyEncode(sb, map, 0);
        sb.setLength(sb.length() - 1);  // Remove the last newline.
        return sb.toString();
    }

    private static StringBuilder prettyEncode(StringBuilder sb, Object object, int indentLevel) {
        if (object instanceof Map) {
            SortedMap<?, ?> tree = object instanceof SortedMap ?
                    (SortedMap<?, ?>) object : new TreeMap<Object, Object>((Map<?, ?>) object);
            sb.append("{\n");
            for (Map.Entry<?, ?> entry : tree.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    appendIndent(sb, indentLevel + 1);
                    sb.append(entry.getKey()).append(" = ");
                    prettyEncode(sb, entry.getValue(), indentLevel + 1);
                }
            }
            appendIndent(sb, indentLevel);
            sb.append("}\n");
        } else if (object instanceof List) {
            sb.append("[\n");
            for (Object value : ((List<?>) object)) {
                if (value != null) {
                    appendIndent(sb, indentLevel + 1);
                    prettyEncode(sb, value, indentLevel + 1);
                }
            }
            appendIndent(sb, indentLevel);
            sb.append("]\n");
        } else if (object instanceof Long || object instanceof Integer || object instanceof Short || object instanceof Byte) {
            sb.append(object).append("\n");
        } else if (object != null) {
            sb.append(object.toString()).append("\n");
        }
        return sb;
    }

    private static void appendIndent(StringBuilder sb, int indentLevel) {
        int num = indentLevel * 2;
        for (int i = 0; i < num; i++) {
            sb.append(' ');
        }
    }
}
