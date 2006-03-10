/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;


public class ArrayUtil {

    /**
     * Returns <code>true</code> if the given array is <code>null</code> or empty.
     */
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
    
}
