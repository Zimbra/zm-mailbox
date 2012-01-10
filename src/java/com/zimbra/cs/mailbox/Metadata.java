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

    /** never change this - implement structural changes in new attrs instead */
    public static final int LEGACY_METADATA_VERSION = 10;

    // MetaData attributes used in toplevel metadata for MailItems.

    // ****PLEASE KEEP THESE IN SORTED ORDER TO MAKE IT EASIER TO AVOID DUPS****

    // FIXME: FRAGMENT and FIRST conflict.
    // FIXME: SENDER and SORT conflict
    // FIXME: RECIPIENTS and TYPES conflict
    public static final String FN_ATTRS            = "a";
    @Deprecated
    public static final String FN_RIGHTS           = "acl";
    public static final String FN_RIGHTS_MAP       = "aclm";
    public static final String FN_ALARM_DATA       = "ad";
    public static final String FN_ACCOUNT_ID       = "aid";
    public static final String FN_CALITEM_IDS      = "ais";
    public static final String FN_CALITEM_END      = "ape";
    public static final String FN_CALITEM_START    = "aps";
    public static final String FN_ATTACHMENTS      = "att";
    public static final String FN_COLOR            = "c";
    public static final String FN_CAL_INTENDED_FOR = "cif";
    public static final String FN_COMPONENT        = "comp";
    public static final String FN_CREATOR          = "cr";
    public static final String FN_MIME_TYPE        = "ct";
    public static final String FN_DRAFT            = "d";
    public static final String FN_DESCRIPTION      = "de";
    public static final String FN_DESC_ENABLED     = "dee";
    public static final String FN_REPLY_ORIG       = "do";
    public static final String FN_REPLY_TYPE       = "dt";
    public static final String FN_AUTO_SEND_TIME   = "ast";
    public static final String FN_ENTRIES          = "en";
    public static final String FN_FRAGMENT         = "f";
    public static final String FN_FIRST            = "f";
    public static final String FN_FIELDS           = "fld";
    public static final String FN_DELETED          = "i4d";
    public static final String FN_DELETED_UNREAD   = "i4du";
    public static final String FN_RECENT           = "i4l";
    public static final String FN_RECENT_CUTOFF    = "i4r";
    public static final String FN_REMOTE_ID        = "id";
    public static final String FN_IDENTITY_ID      = "idnt";
    public static final String FN_INV              = "inv";
    public static final String FN_BOUNDS           = "l";
    public static final String FN_LAST_DATE        = "ld";
    public static final String FN_LOCK_OWNER       = "lo";
    public static final String FN_LISTED           = "lst";
    public static final String FN_LOCK_TIMESTAMP   = "lt";
    public static final String FN_MODSEQ           = "mseq";
    public static final String FN_NUM_COMPONENTS   = "nc";
    public static final String FN_NODES            = "no";
    public static final String FN_PREFIX           = "p";
    public static final String FN_PARTICIPANTS     = "prt";
    public static final String FN_QUERY            = "q";
    public static final String FN_RAW_SUBJ         = "r";
    public static final String FN_REV_DATE         = "rd";
    public static final String FN_REVISIONS        = "rev";
    public static final String FN_REV_ID           = "rid";
    public static final String FN_REPLY_LIST       = "rl";
    public static final String FN_RETENTION_POLICY = "rp";
    public static final String FN_REV_SIZE         = "rs";
    public static final String FN_REPLY_TO         = "rt";
    public static final String FN_SENDER           = "s";
    public static final String FN_SORT             = "s";
    public static final String FN_SYNC_DATE        = "sd";
    public static final String FN_SYNC_GUID        = "sg";
    public static final String FN_REMINDER_ENABLED = "rem";
    public static final String FN_TOTAL_SIZE       = "sz";
    public static final String FN_RECIPIENTS       = "t";
    public static final String FN_TYPES            = "t";
    public static final String FN_TZMAP            = "tzm"; // calendaring: timezone map
    public static final String FN_UID              = "u";
    public static final String FN_USER_AGENT       = "ua";
    public static final String FN_UIDNEXT          = "unxt";
    public static final String FN_URL              = "url";
    @Deprecated
    public static final String FN_MD_VERSION       = "v"; // metadata version; frozen but needs to be encoded for legacy clients
    public static final String FN_VERSION          = "ver";
    public static final String FN_VIEW             = "vt";
    public static final String FN_WIKI_WORD        = "ww";
    public static final String FN_ELIDED           = "X";
    public static final String FN_EXTRA_DATA       = "xd";

    Map<Object, Object> map;

    public Metadata() {
        map = new TreeMap<Object, Object>();
    }

    public Metadata(Map<?, ?> map) {
        this.map = new TreeMap<Object, Object>(map);
    }

    public Metadata(String encoded) throws MailServiceException {
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
            throw MailServiceException.INVALID_METADATA(encoded, e);
        } finally {
            if (map != null && map.containsKey(FN_MD_VERSION)) {
                try {
                    map.remove(FN_MD_VERSION);
                } catch (Exception e) {
                }
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
        return LEGACY_METADATA_VERSION;
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
                result.put(key.toString(), new Metadata((Map<?, ?>) value));
            } else if (value instanceof List) {
                result.put(key.toString(), new MetadataList((List<?>) value));
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
            return new MetadataList(cast);
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
            return new Metadata(cast);
        }
        throw ServiceException.INVALID_REQUEST("invalid/missing value for attribute: " + key, null);
    }

    @Override
    public String toString() {
        put(FN_MD_VERSION, LEGACY_METADATA_VERSION);
        String result = BEncoding.encode(map);
        map.remove(FN_MD_VERSION);
        return result;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder(2048);
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
