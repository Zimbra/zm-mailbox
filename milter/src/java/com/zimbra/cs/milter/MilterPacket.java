/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.milter;

class MilterPacket {
    private final int len;
    private final byte cmd;
    private final byte[] data;

    MilterPacket(int len, byte cmd, byte[] data) {
        this.len = len;
        this.cmd = cmd;
        this.data = data;
    }

    MilterPacket(byte cmd) {
        this.len = 1;
        this.cmd = cmd;
        this.data = null;
    }

    int getLength() {
        return len;
    }

    byte getCommand() {
        return cmd;
    }

    byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(len);
        sb.append(':');
        sb.append((char)cmd);
        sb.append(':');
        if (data != null) {
            for (byte b : data) {
                if (b > 32 &&  b < 127) {
                    sb.append((char)b);
                } else {
                    sb.append("\\");
                    sb.append(b & 0xFF); // make unsigned
                }
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
