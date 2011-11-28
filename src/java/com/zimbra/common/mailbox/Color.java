/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.common.mailbox;

public class Color {
    private static final int  ORANGE = 9;
    private static final long RGB_INDICATOR      = 0x01000000;
    private static final long RGB_MASK           = 0x00ffffff;
    private static final long[] COLORS = {
        // none,  blue,     cyan,     green,    purple
        0x000000, 0x5b9bf2, 0x43eded, 0x6acb9e, 0xba86e5,
        // red,   yellow,   pink,     gray      orange
        0xf66666, 0xf8fa33, 0xfe98d3, 0xbebebe, 0xfdbc55
    };

    public Color(long rgb) {
        setRgb(rgb);
    }
    public Color(byte c) {
        setColor(c);
    }
    public Color(String color) {
        // string representation of color.  e.g. #00008B, #F0F8FF, etc
        setColor(color);
    }
    // internal use only.  changing the visibility to public
    // in order to allow redo players to use it.
    public static Color fromMetadata(long value) {
        return (value & RGB_INDICATOR) != 0 ? new Color(value & RGB_MASK) : new Color((byte)value);
    }
    public static byte getMappedColor(String s) {
        if (s == null) return (byte)ORANGE;
        try {
            long value = s.startsWith("#") ? Long.parseLong(s.substring(1), 16) : Long.parseLong(s);
            for (int i = 0; i < COLORS.length; i++) {
                if (value == COLORS[i]) {
                    return (byte)i;
                }
            }
        }
        catch (NumberFormatException e) {
            // ignore
        }
        return (byte)ORANGE;
    }
    public long getValue() {
        return mValue;
    }
    public byte getRed() {
        return (byte)((getRgb() >> 16) & 0xff);
    }
    public byte getGreen() {
        return (byte)((getRgb() >> 8) & 0xff);
    }
    public byte getBlue() {
        return (byte)(getRgb() & 0xff);
    }
    public long getRgb() {
        return hasMapping() ? COLORS[(int)mValue] : mValue & RGB_MASK;
    }
    public byte getMappedColor() {
        return hasMapping() ? (byte)mValue : ORANGE;
    }
    public boolean hasMapping() {
        return (mValue & RGB_INDICATOR) == 0;
    }
    public void set(Color that) {
        mValue = that.mValue;
    }
    public void setRgb(long rgb) {
        mValue = rgb | RGB_INDICATOR;
    }
    public void setColor(byte color) {
        if (color > ORANGE || color < 0)
            color = ORANGE;
        mValue = color;
    }
    public void setColor(String color) {
        if (color.length() == 4 || color.length() == 7)
            color = color.substring(1);
        if (color.length() == 3) {
            String r = color.substring(0, 1);
            String g = color.substring(1, 2);
            String b = color.substring(2, 3);
            color = r+r+g+g+b+b;
        }
        if (color.length() == 6) {
            long rgb =
                Integer.parseInt(color.substring(0, 2), 16) << 16 |
                Integer.parseInt(color.substring(2, 4), 16) << 8 |
                Integer.parseInt(color.substring(4), 16);
            setRgb(rgb);
        }
    }
    public long toMetadata() {
        return mValue;
    }

    private long mValue;

    @Override public boolean equals(Object that) {
        if (that instanceof Color)
            return mValue == ((Color) that).mValue;
        return false;
    }
    @Override public String toString() {
        return String.format("#%06X", getRgb());
    }
}