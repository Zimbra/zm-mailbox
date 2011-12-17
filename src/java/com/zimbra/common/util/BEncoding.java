/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @since Jul 7, 2005
 * @author dkarp
 */
public final class BEncoding {

    public static final class BEncodingException extends Exception {
        BEncodingException(String msg)   { super(msg); }
        BEncodingException(Exception e)  { super(e); }
    }

    public static String encode(Map<?, ?> object) {
        return encode(new StringBuilder(), object).toString();
    }

    public static String encode(List<?> object) {
        return encode(new StringBuilder(), object).toString();
    }

    public static <T> T decode(String data) throws BEncodingException {
        if (data == null)
            return null;
        try {
            Offset offset = new Offset();
            Object result = decode(data.toCharArray(), offset);
            if (offset.offset != data.length()) {
                throw new BEncodingException("extra characters at end of encoded string");
            }
            @SuppressWarnings("unchecked")
            T cast = (T) result;
            return cast;
        } catch (BEncodingException e) {
            throw e;
        } catch (Exception e) {
            throw new BEncodingException(e);
        }
    }

    private static StringBuilder encode(StringBuilder sb, Object object) {
        if (object instanceof Map) {
            SortedMap<?, ?> tree = (object instanceof SortedMap ?
                    (SortedMap<?, ?>) object : new TreeMap<Object, Object>((Map<?, ?>) object));
            sb.append('d');
            if (!tree.isEmpty())
                for (Map.Entry<?, ?> entry : tree.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        encode(sb, entry.getKey().toString());
                        encode(sb, entry.getValue());
                    }
                }
            sb.append('e');
        } else if (object instanceof List) {
            sb.append('l');
            for (Object value : (List<?>) object) {
                if (value != null) {
                    encode(sb, value);
                }
            }
            sb.append('e');
        } else if (object instanceof Long || object instanceof Integer || object instanceof Short || object instanceof Byte) {
            sb.append('i').append(object).append('e');
        } else if (object != null) {
            String value = object.toString();
            sb.append(value.length()).append(':').append(value);
        }
        return sb;
    }

    private static final class Offset {
        int offset;
    }

    private static Object decode(char[] buffer, Offset offset) throws BEncodingException {
        Object key, value;
        char c = buffer[offset.offset++];
        switch (c) {
            case 'd':
                Map<String, Object> map = new HashMap<String, Object>();
                while ((key = decode(buffer, offset)) != null) {
                    if ((value = decode(buffer, offset)) == null) {
                        throw new BEncodingException("missing dictionary value for key " + key.toString());
                    }
                    map.put(key.toString(), value);
                }
                return map;

            case 'l':
                List<Object> list = new ArrayList<Object>();
                while ((key = decode(buffer, offset)) != null) {
                    list.add(key);
                }
                return list;

            case 'e':
                return null;

            case 'i':
                return new Long(readLong(buffer, offset, 'e'));

            default:
                offset.offset--;
                long length = readLong(buffer, offset, ':');
                int start = offset.offset;
                offset.offset += length;
                return new String(buffer, start, (int) length);
        }
    }

    private static long readLong(char[] buffer, Offset offset, char terminator) {
        int start = offset.offset;
        while (buffer[offset.offset++] != terminator) ;
        return Long.parseLong(new String(buffer, start, offset.offset - start - 1));
    }

}
