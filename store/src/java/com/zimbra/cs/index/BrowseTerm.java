/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index;

public class BrowseTerm {
    private final String text;
    private final int freq;

    public BrowseTerm(String text, int freq) {
        this.text = text;
        this.freq = freq;
    }

    public String getText() {
        return text;
    }

    public int getFreq() {
        return freq;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof BrowseTerm) {
            BrowseTerm other = (BrowseTerm) obj;
            if (text != null) {
                return text.equals(other.text);
            } else { // both null
                return other.text == null;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return text;
    }

}
