/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.util.Collection;


public class ArrayUtil {

    /** Destructively reverses an array of objects and returns the altered passed-in array. */
    public static <A> A[] reverse(A[] array) {
        if (array != null && array.length > 1) {
            for (int i = 0, length = array.length; i < length / 2; i++) {
                A temp = array[i];
                array[i] = array[length - i - 1];
                array[length - i - 1] = temp;
            }
        }
        return array;
    }

    /** Destructively reverses an array of ints and returns the altered passed-in array. */
    public static int[] reverse(int[] array) {
        if (array != null && array.length > 1) {
            for (int i = 0, length = array.length; i < length / 2; i++) {
                int temp = array[i];
                array[i] = array[length - i - 1];
                array[length - i - 1] = temp;
            }
        }
        return array;
    }

    /** Destructively reverses an array of bytes and returns the altered passed-in array. */
    public static byte[] reverse(byte[] array) {
        if (array != null && array.length > 1) {
            for (int i = 0, length = array.length; i < length / 2; i++) {
                byte temp = array[i];
                array[i] = array[length - i - 1];
                array[length - i - 1] = temp;
            }
        }
        return array;
    }

    /** Destructively reverses an array of chars and returns the altered passed-in array. */
    public static char[] reverse(char[] array) {
        if (array != null && array.length > 1) {
            for (int i = 0, length = array.length; i < length / 2; i++) {
                char temp = array[i];
                array[i] = array[length - i - 1];
                array[length - i - 1] = temp;
            }
        }
        return array;
    }

    /** Converts a {@link Collection} of Bytes into a byte[] array. */
    public static byte[] toByteArray(Collection<Byte> c) {
        int pos = 0;
        byte byteArray[] = new byte[c.size()];
        for (Byte id : c)
            byteArray[pos++] = id;
        return byteArray;
    }

    /** Converts a {@link Collection} of Integers into an int[] array. */
    public static int[] toIntArray(Collection<Integer> c) {
        int intArray[] = new int[c.size()], pos = 0;
        for (Integer id : c)
            intArray[pos++] = id;
        return intArray;
    }

    /** Returns <code>true</code> if the given array is <tt>null</tt> or empty. */
    public static boolean isEmpty(Object[] array) {
        return (array == null || array.length == 0);
    }
    
    public boolean byteArrayContains(byte[] array, byte val) {
        for (int i = array.length-1; i >= 0; i--) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param a1
     * @param a2
     * @return the UNION of the two byte arrays
     */
    public byte[] combineByteArraysNoDups(byte[] a1, byte[] a2) {
        byte[] tmp = new byte[a2.length];
        int tmpOff = 0;
        
        for (int i = a2.length-1; i>=0; i--) {
            if (!byteArrayContains(a1, a2[i])) {
                tmp[tmpOff++] = a2[i];
            }
        }
        
        byte[] toRet = new byte[tmpOff + a1.length];
        
        System.arraycopy(tmp, 0, toRet, 0, tmpOff);
        System.arraycopy(a1, 0, toRet, tmpOff, a1.length);

        return toRet;
    }
    
    public byte[] addToByteArrayNoDup(byte[] array, byte val) {
        if (!byteArrayContains(array, val)) {
            byte[] toRet = new byte[array.length+1];
            System.arraycopy(array, 0, toRet, 0, array.length);
            toRet[toRet.length-1] = val;
            return toRet;
        }
        return array;
    }
    
    /**
     * @param a1
     * @param a2
     * @return the INTERSECTION of the two byte arrays
     */
    public byte[] intersectByteArrays(byte[] a1, byte[] a2) {
        byte[] tmp = new byte[a1.length + a2.length];
        int tmpOff = 0;
        
        for (int i = a1.length-1; i >=0; i--) {
            if (byteArrayContains(a2, a1[i])) {
                tmp[tmpOff++] = a1[i];
            }
        }

        byte[] toRet = new byte[tmpOff];
        for (int i = 0; i < tmpOff; i++) {
            toRet[i] = tmp[i];
        }
        
        // FIXME testing code only!
        for (int i = toRet.length-1; i>=0; i--) {
            assert(byteArrayContains(a1, toRet[i]) &&
                    byteArrayContains(a2, toRet[i]));
        }
        
        return toRet;
    }
 
    /**
     * Returns the first element, or <tt>null</tt> if the array
     * is <tt>null</tt> or empty.
     */
    public static <E> E getFirstElement(E[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[0];
    }
}
