/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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
