/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

public class Pair<F,S> {
    private F mFirst;
    private S mSecond;

    public Pair(F first, S second) {
        mFirst = first;
        mSecond = second;
    }

    public F car() {
        return getFirst();
    }
    public S cdr() {
        return getSecond();
    }
    public F getFirst() {
        return mFirst;
    }
    public S getSecond() {
        return mSecond;
    }

    public void setFirst(F first) {
        mFirst = first;
    }
    public void setSecond(S second) {
        mSecond = second;
    }

    @Override public boolean equals(Object obj) {
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

    @Override public int hashCode() {
        int code1 = mFirst == null ? 0 : mFirst.hashCode();
        int code2 = mSecond == null ? 0 : mSecond.hashCode();
        return code1 ^ code2;
    }

    @Override public String toString() {
        return "(" + mFirst + "," + mSecond + ")";
    }

    public static void main(String[] args) {
        System.out.println(new Pair<String,String>("foo", "bar").equals(new Pair<String,String>("foo", "bar")));
        System.out.println(new Pair<String,String>("foo", null).equals(new Pair<String,String>("fo" + 'o', null)));
        System.out.println(new Pair<String,String>(null, "bar").equals(new Pair<String,String>(null, "foo")));
        System.out.println(new Pair<String,String>("foo", "bar").equals(new Pair<String,Integer>("foo", 8)));
        System.out.println(new Pair<String,String>(null, "bar").equals(new Pair<Integer,String>(0, "bar")));
    }
}
