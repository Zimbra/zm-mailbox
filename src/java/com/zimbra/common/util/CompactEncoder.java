/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008 Zimbra, Inc.
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
 * Created on May 1, 2008
 */
package com.zimbra.common.util;

import java.io.CharConversionException;
import static java.lang.Character.isDigit;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * @author tfr
 * compact, fast encoder supporting all basic Java Objects
 */
public class CompactEncoder {
    public static class CompactEncoderException extends Exception {
        CompactEncoderException(Exception e) { super(e); }
        CompactEncoderException(String s) { super(s); }

        static final long serialVersionUID = -4610917332015428041L;
    }

    public static Object decode(byte[] data) throws CompactEncoderException {
        ByteBuffer bb = ByteBuffer.wrap(data);
        Object obj;
        byte order;

        try {
            order = bb.get();
            if (order == '[')
                bb.order(ByteOrder.BIG_ENDIAN);
            else if (order == ']')
                bb.order(ByteOrder.LITTLE_ENDIAN);
            else if (order != '\\')
                throw new CompactEncoderException("invalid data format");
            obj = decode(bb);
        } catch (CompactEncoderException e) {
            throw e;
        } catch (Exception e) {
            throw new CompactEncoderException(e);
        }
        if (bb.hasRemaining())
            throw new CompactEncoderException("invalid trailing data");
        return obj;
    }

    public static byte[] encode(Object obj) {
        return encode(obj, ByteOrder.nativeOrder());
    }

    public static byte[] encode(Object obj, ByteOrder bo) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        byte [] ret;
        
        bb.order(bo);
        bb.put(bo == ByteOrder.BIG_ENDIAN ? (byte)'[' : (byte)']');
        bb = encode(bb, obj);
        ret = new byte[bb.position()];
        System.arraycopy(bb.array(), 0, ret, 0, bb.position());
        return ret;
    }

    public static byte[] encodeToString(Object obj) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        byte [] ret;

        bb.put((byte)'\\');
        bb = encodeToString(bb, obj);
        ret = new byte[bb.position()];
        System.arraycopy(bb.array(), 0, ret, 0, bb.position());
        return ret;
    }

    private static Object decode(ByteBuffer bb) throws Exception {
        byte type = bb.get();
        
        switch (type) {
        case  ' ': case  '!': case  '"': case  '#': case  '$': case  '%':
        case  '&': case  '\'': case  '(': case  ')': case  '*': case  '+':
        case  ',': case  '-': case  '.': case  '\\': case  '0': case  '1':
        case  '2': case  '3': case  '4': case  '5': case  '6': case  '7':
        case  '8': case  '9': case  ':': case  ';': case  '<': case  '=':
        case  '>': case  '?': case  '@': {
            byte[] str = new byte[type - ' '];
            
            bb.get(str);
            return new String(str, "UTF-8");
        } case 'A': {
            byte atype = bb.get();
            int len = (int)getStringLong(bb);

            switch (atype) {
            case 'B': {
                byte [] ret = new byte[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = (byte)getStringLong(bb);
                return ret;
            } case 'C': {
                char [] ret = new char[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = (char)getStringLong(bb);
                return ret;
            } case 'S': {
                short [] ret = new short[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = (short)getStringLong(bb);
                return ret;
            } case 'I': {
                int [] ret = new int[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = (int)getStringLong(bb);
                return ret;
            } case 'L': {
                long [] ret = new long[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = getStringLong(bb);
                return ret;
            } case 'F': {
                float [] ret = new float[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = Float.parseFloat(getString(bb));
                return ret;
            } case 'D': {
                double [] ret = new double[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = Double.parseDouble(getString(bb));
                return ret;
            } case 'O': {
                Object [] ret = new Object[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = decode(bb);
                return ret;
            } default:
                throw new CompactEncoderException("invalid data type");
            }
        } case 'a': {
            byte atype = bb.get();
            int len = bb.getInt();

            switch (atype) {
            case 'b': {
                byte [] ret = new byte[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.get();
                return ret;
            } case 'c': {
                char [] ret = new char[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.getChar();
                return ret;
            } case 's': {
                short [] ret = new short[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.getShort();
                return ret;
            } case 'i': {
                int [] ret = new int[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.getInt();
                return ret;
            } case 'l': {
                long [] ret = new long[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.getLong();
                return ret;
            } case 'f': {
                float [] ret = new float[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.getFloat();
                return ret;
            } case 'd': {
                double [] ret = new double[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = bb.getDouble();
                return ret;
            } case 'o': {
                Object [] ret = new Object[len];
                
                for (int i = 0; i < len; i++)
                    ret[i] = decode(bb);
                return ret;
            } default:
                throw new CompactEncoderException("invalid data type");
            }
        } case 'B': {
            return new Byte((byte)getStringLong(bb));
        } case 'b': {
            return new Byte(bb.get());
        } case 'C': {
            return new Character((char)getStringLong(bb));
        } case 'c': {
            return new Character(bb.getChar());
        } case 'S': {
            return new Short((short)getStringLong(bb));
        } case 's': {
            return new Short(bb.getShort());
        } case 'I': {
            return new Integer((int)getStringLong(bb));
        } case 'i': {
            return new Integer(bb.getInt());
        } case 'L': {
            return new Long(getStringLong(bb));
        } case 'l': {
            return new Long(bb.getLong());
        } case 'F': {
            return new Float(Float.parseFloat(getString(bb)));
        } case 'f': {
            return new Float(bb.getFloat());
        } case 'D': {
            return new Double(Double.parseDouble(getString(bb)));
        } case 'd': {
            return new Double(bb.getDouble());
        } case 'E': {
            return new Integer((int)getStringLong(bb));
        } case 'e': {
            return new Integer(bb.getShort());
        } case 'n': {
            return null;
        } case 'M': {
            int cnt = (int)getStringLong(bb);
            Map<Object, Object> map = new HashMap<Object,
                Object>((int)(cnt * 1.5));

            for (int i = 0; i < cnt; i++)
                map.put(decode(bb), decode(bb));
            return map;
        } case 'm': {
            int cnt = bb.getInt();
            Map<Object, Object> map = new HashMap<Object,
                Object>((int)(cnt * 1.5));

            for (int i = 0; i < cnt; i++)
                map.put(decode(bb), decode(bb));
            return map;
        } case 'p': {
            byte[] val = new byte[bb.getShort()];
            
            bb.get(val);
            return new String(val, "UTF-8");
        } case 'T': {
            byte[] str = new byte[(int)getStringLong(bb)];
            
            bb.get(str);
            return new String(str, "UTF-8");
        } case 't': {
            byte[] val = new byte[bb.getInt()];
            
            bb.get(val);
            return new String(val, "UTF-8");
        } case 'V': {
            int cnt = (int)getStringLong(bb);
            ArrayList<Object> array = new ArrayList<Object>(cnt);

            for (int i = 0; i < cnt; i++)
                array.add(i, decode(bb));
            return array;
        } case 'v': {
            int cnt = bb.getInt();
            ArrayList<Object> array = new ArrayList<Object>(cnt);

            for (int i = 0; i < cnt; i++)
                array.add(i, decode(bb));
            return array;
        } case '{': {
            return new Boolean(true);
        } case '}': {
            return new Boolean(false);
        } default:
            throw new CompactEncoderException("invalid data type");
        }
    }

    private static ByteBuffer encode(ByteBuffer bb, Object obj) {
        if (obj instanceof String) {
        } else if (obj instanceof Integer) {
            return putRaw(bb, 'i', Integer.SIZE).putInt((Integer)obj);
        } else if (obj instanceof Boolean) {
            return putRaw(bb, (Boolean)obj ? '{' : '}', 0);
        } else if (obj instanceof Byte) {
            return putRaw(bb, 'b', Byte.SIZE).put((Byte)obj);
        } else if (obj instanceof Character) {
            return putRaw(bb, 'c', Character.SIZE).putShort((short)((Character)obj).charValue());
        } else if (obj instanceof Short) {
            return putRaw(bb, 's', Short.SIZE).putShort((Short)obj);
        } else if (obj instanceof Long) {
            return putRaw(bb, 'l', Long.SIZE).putLong((Long)obj);
        } else if (obj instanceof Float) {
            return putRaw(bb, 'f', Float.SIZE).putFloat((Float)obj);
        } else if (obj instanceof Double) {
            return putRaw(bb, 'd', Double.SIZE).putDouble((Double)obj);
        } else if (obj instanceof Enum) {
            return putRaw(bb, 'e', Short.SIZE).putShort((short)((Enum<?>)obj).ordinal());
        } else if (obj == null) {
            return putRaw(bb, 'n', 0);
        } else if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);

            bb = putRaw(bb, 'a', 0);
            if (obj instanceof int[]) {
                bb = putRaw(bb, 'i', Integer.SIZE + len * Integer.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.putInt(Array.getInt(obj, i));
            } else if (obj instanceof byte[]) {
                bb = putRaw(bb, 'b', Integer.SIZE + len * Byte.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.put(Array.getByte(obj, i));
            } else if (obj instanceof char[]) {
                bb = putRaw(bb, 'c', Integer.SIZE + len * Character.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.putChar(Array.getChar(obj, i));
            } else if (obj instanceof short[]) {
                bb = putRaw(bb, 's', Integer.SIZE + len * Short.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.putShort(Array.getShort(obj, i));
            } else if (obj instanceof long[]) {
                bb = putRaw(bb, 'l', Integer.SIZE + len * Long.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.putLong(Array.getLong(obj, i));
            } else if (obj instanceof float[]) {
                bb = putRaw(bb, 'f', Integer.SIZE + len * Float.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.putFloat(Array.getFloat(obj, i));
            } else if (obj instanceof Double[]) {
                bb = putRaw(bb, 'd', Integer.SIZE + len * Double.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb.putDouble(Array.getDouble(obj, i));
            } else {
                bb = putRaw(bb, 'o', Integer.SIZE).putInt(len);
                for (int i = 0; i < len; i++)
                    bb = encode(bb, Array.get(obj, i));
            }
            return bb;
        } else if (obj instanceof List) {
            List<?> l = (List<?>)obj;
            
            bb = putRaw(bb, 'v', Integer.SIZE).putInt(l.size());
            for (Object val : l)
                bb = encode(bb, val);
            return bb;
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)obj;

            bb = putRaw(bb, 'm', Integer.SIZE).putInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                bb = encode(bb, entry.getKey());
                bb = encode(bb, entry.getValue());
            }
            return bb;
        }
        byte[] val = null;
        try {
            val = obj.toString().getBytes("UTF-8");
        } catch (Exception e) {
        }
        if (val.length < 'A' - ' ')
            return putRaw(bb, (char)(' ' + val.length), val.length).put(val);
        else if (val.length <= Short.MAX_VALUE)
            return putRaw(bb, 'p', Short.SIZE + val.length).
                putShort((short)val.length).put(val);
        return putRaw(bb, 't', Integer.SIZE + val.length).putInt(val.length).
            put(val);
    }

    private static ByteBuffer encodeToString(ByteBuffer bb, Object obj) {
        if (obj instanceof String) {
        } else if (obj instanceof Integer) {
            return putString(bb, 'I', obj);
        } else if (obj instanceof Boolean) {
            return putRaw(bb, (Boolean)obj ? '{' : '}', 0);
        } else if (obj instanceof Byte) {
            return putString(bb, 'B', obj);
        } else if (obj instanceof Character) {
            return putString(bb, 'C', obj);
        } else if (obj instanceof Short) {
            return putString(bb, 'S', obj);
        } else if (obj instanceof Long) {
            return putString(bb, 'L', obj);
        } else if (obj instanceof Float) {
            return putString(bb, 'F', obj);
        } else if (obj instanceof Double) {
            return putString(bb, 'D', obj);
        } else if (obj instanceof Enum) {
            return putString(bb, 'E', ((Enum<?>)obj).ordinal());
        } else if (obj == null) {
            return putRaw(bb, 'n', 0);
        } else if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);

            bb = putRaw(bb, 'A', 0);
            if (obj instanceof int[]) {
                bb = putString(bb, 'I', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, Array.getInt(obj, i));
            } else if (obj instanceof byte[]) {
                bb = putString(bb, 'B', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, Array.getByte(obj, i));
            } else if (obj instanceof char[]) {
                bb = putString(bb, 'C', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, (short)Array.getChar(obj, i));
            } else if (obj instanceof short[]) {
                bb = putString(bb, 'S', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, Array.getShort(obj, i));
            } else if (obj instanceof long[]) {
                bb = putString(bb, 'L', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, Array.getLong(obj, i));
            } else if (obj instanceof float[]) {
                bb = putString(bb, 'F', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, Array.getFloat(obj, i));
            } else if (obj instanceof Double[]) {
                bb = putString(bb, 'D', len);
                for (int i = 0; i < len; i++)
                    bb = putStringElem(bb, Array.getDouble(obj, i));
            } else {
                bb = putString(bb, 'O', len);
                for (int i = 0; i < len; i++)
                    bb = encodeToString(bb, Array.get(obj, i));
            }
            return bb;
        } else if (obj instanceof List) {
            List<?> l = (List<?>)obj;
            
            bb = putString(bb, 'V', l.size());
            for (Object val : l)
                bb = encodeToString(bb, val);
            return bb;
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)obj;

            bb = putString(bb, 'M', map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                bb = encodeToString(bb, entry.getKey());
                bb = encodeToString(bb, entry.getValue());
            }
            return bb;
        }
        byte[] val = null;
        try {
            val = obj.toString().getBytes("UTF-8");
        } catch (Exception e) {
        }
        if (val.length < 'A' - ' ')
            return putRaw(bb, (char)(' ' + val.length), val.length).put(val);
        bb = putString(bb, 'T', String.valueOf(val.length));
        return ensure(bb, val.length).put(val);
    }

    private static String getString(ByteBuffer bb) throws Exception {
        byte len = (byte)(bb.get() - (byte)' ');
        byte[] val = new byte[len];
        
        bb.get(val);
        return new String(val, "UTF-8");
    }
    
    private static long getStringLong(ByteBuffer bb) throws
        CharConversionException {
        long l = 0;
        byte len = (byte)(bb.get() - ' ');
        boolean neg = false;
        
        while (len-- > 0) {
            byte b = bb.get();
            
            if (b == '-' && l == 0)
                neg = true;
            else if (!isDigit(b))
                throw new CharConversionException();
            else
                l = 10 * l + b - '0';
        }
        return neg ? -1 * l : l;
    }
    
    private static ByteBuffer ensure(ByteBuffer bb, int cap) {
        if (bb.remaining() < cap)
            return ByteBuffer.allocate(bb.capacity() < cap ? bb.capacity() * 2 :
                bb.capacity() + cap + 512).put(bb.array());
        return bb;
    }
    
    private static ByteBuffer putRaw(ByteBuffer bb, char type, int cap) {
        return ensure(bb, cap + 1).put((byte)type);
    }
    
    private static ByteBuffer putString(ByteBuffer bb, char type, Object obj) {
        byte [] str;
        
        try {
            str = obj.toString().getBytes("UTF-8");
        } catch (Exception e) {
            str = obj.getClass().getName().getBytes();
        }
        return ensure(bb, str.length + 2).put((byte)type).put((byte)(' ' +
            str.length)).put(str);
    }
    
    private static ByteBuffer putStringElem(ByteBuffer bb, Object obj) {
        byte [] str;
        
        try {
            str = obj.toString().getBytes("UTF-8");
        } catch (Exception e) {
            str = obj.getClass().getName().getBytes();
        }
        return ensure(bb, str.length + 1).put((byte)(' ' + str.length)).put(str);
    }
    
    public static void main(String[] args) throws CompactEncoderException {
        char[] carray = new char[] { 'a', 'r', 'r', 'a', 'y'};
        ArrayList<?> dlist;
        Map<?, ?> dmap;
        ArrayList<Object> list = new ArrayList<Object>();
        Map<Object, Object> map = new HashMap<Object, Object>();
        Object[] darray, oarray = new Object[] {
            new String("arraystring"), new Integer(9)
        };
        byte [] print, raw;

        print = encodeToString(oarray);
        raw = encode(oarray);
        darray = (Object [])decode(print);
        darray = (Object [])decode(raw);
        list.add(carray);
        list.add(new Integer(8));
        list.add("string");
        list.add(true);
        print = encodeToString(list);
        raw = encode(list);
        dlist = (ArrayList<?>)decode(print);
        dlist = (ArrayList<?>)decode(raw);
        map.put("array", oarray);
        map.put("byteorder", ByteOrder.nativeOrder());
        map.put("emptystring", "");
        map.put("float", new Float(1.1));
        map.put("hashmap", new HashMap<Object, Object>());
        map.put("list", list);
        map.put("long", new Long(3));
        map.put("mapstring", "test");
        map.put("null", null);
        map.put("text", "1234567890123456789012345678901234567890");
        print = encodeToString(map);
        raw = encode(map);
        dmap = (HashMap<?, ?>)decode(print);
        dmap = (HashMap<?, ?>)decode(raw);
        System.out.println(new String(print));
        darray.toString();
        dlist.toString();
        dmap.toString();
    }
}
