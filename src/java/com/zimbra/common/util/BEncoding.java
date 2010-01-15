/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

/*
 * Created on Jul 7, 2005
 */
package com.zimbra.common.util;

import java.util.*;

/**
 * @author dkarp
 */
public class BEncoding {

    public static final class BEncodingException extends Exception {
        BEncodingException(String msg)   { super(msg); }
        BEncodingException(Exception e)  { super(e); }
    }

    public static String encode(Map object) {
        return encode(new StringBuffer(), object).toString();
    }
    public static String encode(List object) {
        return encode(new StringBuffer(), object).toString();
    }

    public static Object decode(String data) throws BEncodingException {
        if (data == null)
            return null;
        try {
            Offset offset = new Offset();
            Object result = decode(data.toCharArray(), offset);
            if (offset.offset != data.length())
                throw new BEncodingException("extra characters at end of encoded string");
            return result;
        } catch (BEncodingException e) {
            throw e;
        } catch (Exception e) {
            throw new BEncodingException(e);
        }
    }


    private static StringBuffer encode(StringBuffer sb, Object object) {
        if (object instanceof Map) {
            SortedMap tree = (object instanceof SortedMap ? (SortedMap) object : new TreeMap((Map) object));
            sb.append('d');
            if (!tree.isEmpty())
                for (Iterator it = tree.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    if (entry.getKey() != null && entry.getValue() != null) {
                        encode(sb, entry.getKey().toString());
                        encode(sb, entry.getValue());
                    }
                }
            sb.append('e');
        } else if (object instanceof List) {
            Object value;
            sb.append('l');
            for (Iterator it = ((List) object).iterator(); it.hasNext(); )
                if ((value = it.next()) != null)
                    encode(sb, value);
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
                Map map = new HashMap();
                while ((key = decode(buffer, offset)) != null) {
                    if ((value = decode(buffer, offset)) == null)
                        throw new BEncodingException("missing dictionary value for key " + key.toString());
                    map.put(key.toString(), value);
                }
                return map;

            case 'l':
                List list = new ArrayList();
                while ((key = decode(buffer, offset)) != null)
                    list.add(key);
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

    public static void main(String[] args) throws BEncodingException {
        List list = new ArrayList();
        list.add(new Integer(654));
        list.add("hwhergk");
        list.add(new StringBuffer("74x"));
        Map map = new HashMap();
        map.put("testing", new Long(5));
        map.put("foo2", "bar");
        map.put("herp", list);
        map.put("Foo", new Float(6.7));
        map.put("yy", new TreeMap());
        String encoded = encode(map);
        System.out.println(encoded);
        System.out.println(decode(encoded));
    }
}
