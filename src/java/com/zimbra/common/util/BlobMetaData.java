/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 8, 2004
 *
 */
package com.zimbra.common.util;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is used to encode meta-data stored in a hash-table that is bound for
 * a blob_metadata.metadata column (text).
 * 
 * The map should consist of keys which are Strings (and don't contain the '=' character),
 * and values which are also Strings (and can contain any value).
 * 
 * @author schemers
 *
 */
public class BlobMetaData {

    public static void encodeMetaData(String name, String value, StringBuffer sb) {
        if (value == null)
            return;
        sb.append(name);
        sb.append('=');
        sb.append(value.length());
        sb.append(':');
        sb.append(value);
        sb.append(';');
    }

    public static void encodeMetaData(String name, long value, StringBuffer sb) {
        String str = Long.toString(value);
        sb.append(name);
        sb.append('=');
        sb.append(str.length());
        sb.append(':');
        sb.append(str);
        sb.append(';');
    }

    public static void encodeMetaData(String name, boolean value, StringBuffer sb) {
        sb.append(name);
        sb.append(value ? "=1:1;" : "=1:0;");
    }

    /**
     * Encode name/value pairs (String/String) from the specified map into a String
     * suitable for storing as metadata and properly formatted so it can be feed back
     * into decodeMetaData.
     * 
     * @param map
     * @return
     */
    public static String encode(Map map) {
        if (map == null)
            return null;
        StringBuffer sb = new StringBuffer();
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            encodeMetaData((String) entry.getKey(), (String) entry.getValue(), sb);
        }
        return sb.toString();
    }

    /**
     * Take meta-data encoded via encodeMetaData and decode it back into a Map.
     * 
     * @param metaData the encoded meta-data
     * @return a Map consisting of the name/value pairs.
     * @throws BlobMetaDataEncodingException if unable to decode the meta-data.
     */
    public static Map decode(String metaData) throws BlobMetaDataEncodingException {
        Map map = new HashMap();
        int offset = 0;
        int p = 0;
        int len = metaData.length();
        while (offset < len && (p = metaData.indexOf('=', offset)) != -1) {
            String name = metaData.substring(offset, p);

            p++;            
            int i = p;

            while (p < len && Character.isDigit(metaData.charAt(p)))
                p++;

            if (p > len || metaData.charAt(p) != ':')
                throw new BlobMetaDataEncodingException("error decoding value length");

            int value_len = Integer.parseInt(metaData.substring(i, p));

            p++;
            i = p;

            if (p + value_len > len)
                throw new BlobMetaDataEncodingException("invalid value length");

            String value = metaData.substring(i, p + value_len);

            p += value_len;

            // TODO: should throw an exception and remove the meta data from the DB
            if ((p > len) || (metaData.charAt(p) != ';'))
                throw new BlobMetaDataEncodingException("expecting ';' after value");
            
            p++;
            map.put(name, value);
            offset = p;
        }
        return map;
    }

    public static Map decodeRecursive(String metaData) throws BlobMetaDataEncodingException {
        Map map = decode(metaData);
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            try {
                String value = entry.getValue().toString();
                if (value.length() > 5 && value.indexOf('=') != -1)
                    entry.setValue(decodeRecursive(value));
            } catch (Exception e) { }
        }
        return map;
    }

    public static String getString(Map m, String name) {
        return (String)m.get(name);
    }
    public static String getString(Map m, String name, String defaultValue) {
        String toRet = (String)m.get(name);
        if (toRet == null) {
            return defaultValue;
        } else {
            return toRet;
        }
    }
    public static int getInt(Map m, String name) {
        String s = (String)m.get(name);
        return Integer.parseInt(s);
    }
    public static int getInt(Map m, String name, int defaultValue) {
        String s = (String)m.get(name);
        if (s == null) { 
            return defaultValue;
        } else {
            return Integer.parseInt(s);
        }
    }
    public static long getLong(Map m, String name) {
        String s = (String)m.get(name);
        return Long.parseLong(s);
    }
    public static long getLong(Map m, String name, long defaultValue) {
        String s = (String)m.get(name);
        if (s == null) { 
            return defaultValue;
        } else {
            return Long.parseLong(s);
        }
    }
    public static boolean getBoolean(Map m, String name) {
        String s = (String)m.get(name);
        if (s.equals("1")) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean getBoolean(Map m, String name, boolean defaultValue) {
        String s = (String)m.get(name);
        if (s == null) { 
            return defaultValue;
        } else {
            if (s.equals("1")) {
                return true;
            } else {
                return false;
            }
        }
    }    
    
    
    public static void main(String[] args) {
        String TEST_STR_NAME = "name1";
        String TEST_STR = "hello";
        
        String TEST_INT_NAME = "name2";
        int TEST_INT = 314159;
        
        String TEST_LONG_NAME = "name3";
        long TEST_LONG = 124912348;
        
        String TEST_TRUE_NAME = "name4";
        boolean TEST_TRUE = true;
        
        String TEST_FALSE_NAME = "name5";
        boolean TEST_FALSE = false;
        
        String TEST_NOTTHERE_NAME = "no";
        
        StringBuffer sb = new StringBuffer();
        {
            BlobMetaData.encodeMetaData(TEST_STR_NAME, TEST_STR, sb);
            BlobMetaData.encodeMetaData(TEST_INT_NAME, TEST_INT, sb);
            BlobMetaData.encodeMetaData(TEST_LONG_NAME, TEST_LONG, sb);
            BlobMetaData.encodeMetaData(TEST_TRUE_NAME, TEST_TRUE, sb);
            BlobMetaData.encodeMetaData(TEST_FALSE_NAME, TEST_FALSE, sb);
        }
        
        try {
            Map m = BlobMetaData.decode(sb.toString());
            
            {
                String test_str = BlobMetaData.getString(m, TEST_STR_NAME);
                assert(test_str.equals(TEST_STR));
                
                int test_int = BlobMetaData.getInt(m, TEST_INT_NAME);
                assert(test_int == TEST_INT);
                
                long test_long = BlobMetaData.getLong(m, TEST_LONG_NAME);
                assert(test_long == TEST_LONG);
                
                boolean test_true = BlobMetaData.getBoolean(m, TEST_TRUE_NAME);
                assert(test_true == TEST_TRUE);
                
                boolean test_false = BlobMetaData.getBoolean(m, TEST_FALSE_NAME);
                assert(test_false == TEST_FALSE);
            }
            
            // now, make sure none of these crash:

            {
                String test_str = BlobMetaData.getString(m, TEST_STR_NAME);
                if (!(test_str.equals(TEST_STR))) { throw new ParseException("ERROR!",1); }
                
                int test_int = BlobMetaData.getInt(m, TEST_INT_NAME);
                if (!(test_int == TEST_INT)) { throw new ParseException("ERROR!",2); }
                
                long test_long = BlobMetaData.getLong(m, TEST_LONG_NAME);
                if (!(test_long == TEST_LONG)) { throw new ParseException("ERROR!",3); }
                
                boolean test_true = BlobMetaData.getBoolean(m, TEST_TRUE_NAME);
                if (!(test_true == TEST_TRUE)) { throw new ParseException("ERROR!",4); }
                
                boolean test_false = BlobMetaData.getBoolean(m, TEST_FALSE_NAME);
                if (!(test_false == TEST_FALSE)) { throw new ParseException("ERROR!",5); }
            }
            
            // now, make sure none of these crash:
            {
                String test_str = BlobMetaData.getString(m, TEST_NOTTHERE_NAME, TEST_STR);
                if (!(test_str.equals(TEST_STR))) { throw new ParseException("ERROR!",6); }
                
                int test_int = BlobMetaData.getInt(m, TEST_NOTTHERE_NAME, TEST_INT);
                if (!(test_int == TEST_INT)) { throw new ParseException("ERROR!",7); }
                
                long test_long = BlobMetaData.getLong(m, TEST_NOTTHERE_NAME, TEST_LONG);
                if (!(test_long == TEST_LONG)) { throw new ParseException("ERROR!",8); }
                
                boolean test_true = BlobMetaData.getBoolean(m, TEST_NOTTHERE_NAME, TEST_TRUE);
                if (!(test_true == TEST_TRUE)) { throw new ParseException("ERROR!",9); }
                
                boolean test_false = BlobMetaData.getBoolean(m, TEST_NOTTHERE_NAME, TEST_FALSE);
                if (!(test_false == TEST_FALSE)) { throw new ParseException("ERROR!",10); }
            }        
            
            System.out.println("Test ran OK!");
            
        } catch (ParseException e) {
            System.out.println("ERROR - caught ParseException in test "+e.getErrorOffset());
            e.printStackTrace();
        } catch (BlobMetaDataEncodingException e) {
            System.out.println("ERROR - caught BlobMetaDataEncodingException "+e);
            e.printStackTrace();
        }
        
    }
}
