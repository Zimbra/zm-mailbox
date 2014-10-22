/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.Objects;

public class Triple<F,S,T> {
    protected F mFirst;
    protected S mSecond;
    protected T mThird;

    public Triple(F first, S second, T third) {
        mFirst = first;
        mSecond = second;
        mThird = third;
    }

    public F getFirst() {
        return mFirst;
    }

    public S getSecond() {
        return mSecond;
    }

    public T getThird() {
        return mThird;
    }

    public void setFirst(F first) {
        mFirst = first;
    }

    public void setSecond(S second) {
        mSecond = second;
    }

    public void setThird(T third) {
        mThird = third;
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof Triple) {
            @SuppressWarnings("rawtypes")
            Triple that = (Triple) obj;
            if (!Objects.equals(mFirst, that.mFirst)) {
                return false;
            }
            if (!Objects.equals(mSecond, that.mSecond)) {
                return false;
            }
            if (!Objects.equals(mThird, that.mThird)) {
                return false;
            }
            return true;
        }
        return super.equals(obj);
    }

    @Override public int hashCode() {
        int code1 = mFirst == null ? 0 : mFirst.hashCode();
        int code2 = mSecond == null ? 0 : mSecond.hashCode();
        int code3 = mThird == null ? 0 : mThird.hashCode();
        return ("" + code1 + code2 + code3).hashCode();
    }

    @Override public String toString() {
        return "(" + mFirst + "," + mSecond + "," + mThird + ")";
    }
}
