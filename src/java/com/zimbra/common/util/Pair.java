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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

public class Pair<F,S> {
	private F mFirst;
	private S mSecond;

    public Pair(F first, S second) {
        assert(first != null && second != null);
        mFirst = first;
        mSecond = second;
    }

	public F getFirst() {
		return mFirst;
	}
	public S getSecond() {
		return mSecond;
	}
	public F car() {
		return getFirst();
	}
	public S cdr() {
		return getSecond();
	}

    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            Pair that = (Pair) obj;
            if (mFirst != that.mFirst && (mFirst == null || !mFirst.equals(that.mFirst)))
                return false;
            if (mSecond != that.mSecond && (mSecond == null || !mSecond.equals(that.mSecond)))
                return false;
            return true;
        } else {
            return super.equals(obj);
        }
    }

    public int hashCode() {
        int code1 = mFirst == null ? 0 : mFirst.hashCode();
        int code2 = mSecond == null ? 0 : mSecond.hashCode();
        return code1 ^ code2;
    }

    public static void main(String[] args) {
        System.out.println(new Pair<String,String>("foo", "bar").equals(new Pair<String,String>("foo", "bar")));
        System.out.println(new Pair<String,String>("foo", null).equals(new Pair<String,String>("fo" + 'o', null)));
        System.out.println(new Pair<String,String>(null, "bar").equals(new Pair<String,String>(null, "foo")));
        System.out.println(new Pair<String,String>("foo", "bar").equals(new Pair<String,Integer>("foo", 8)));
        System.out.println(new Pair<String,String>(null, "bar").equals(new Pair<Integer,String>(0, "bar")));
    }
}
