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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

final class NioMilterDecoder extends CumulativeProtocolDecoder {

    @Override
    public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
        if (!in.prefixedDataAvailable(4)) {
            return false;
        }
        int len = in.getInt();
        byte cmd = in.get();
        byte[] data = null;
        if (len > 1) {
            data = new byte[len - 1];
            in.get(data);
        }
        MilterPacket packet = new MilterPacket(len, cmd, data);
        out.write(packet);
        return true;
    }
}
