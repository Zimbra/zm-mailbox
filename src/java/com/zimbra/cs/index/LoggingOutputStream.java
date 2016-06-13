/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.io.OutputStream;
import com.zimbra.common.util.Log;

public class LoggingOutputStream extends OutputStream
{
    private static final byte CR = (byte)0x0d;
    private static final byte LF = (byte)0x0a;
    private static final int DEFAULT_BUFFER_LENGTH = 2048;
    private static final int MAX_BUFFER_LENGTH =  16 * 1024;
    
    protected boolean mClosed = false;
    private Log mLog = null;
    private Log.Level mLevel = null;
    protected int                 bytesInBuf;
    protected byte[]              buf;
    private int                   bufLength;
    

    LoggingOutputStream(Log log, Log.Level level)
    {
        bufLength = DEFAULT_BUFFER_LENGTH;
        buf = new byte[DEFAULT_BUFFER_LENGTH];
        bytesInBuf = 0;
        mLog = log;
        mLevel = level;
    }

    public void close()
    {
        flush();
        mClosed = true;
    }

    public void write(final int in) throws IOException
    {
        if (mClosed)
            throw new IOException("Stream closed.");

        // don't log nulls
        if (in != 0)
        {
            // would this be writing past the buffer?
            if (bytesInBuf == bufLength)
            {
                if (bufLength * 2 >= MAX_BUFFER_LENGTH) {
                    flush();
                } else {
                    // grow the buffer
                    final int newBufLength = 2*bufLength; 
                    final byte[] newBuf = new byte[newBufLength];
                    System.arraycopy(buf, 0, newBuf, 0, bufLength);
                    buf = newBuf;
                    bufLength = newBufLength;
                }
            }
            buf[bytesInBuf] = (byte)in;
            bytesInBuf++;
            
            // flush on newline
            if (in == 0x0a) 
                flush();
        }
    }

    public void flush()
    {
        if (bytesInBuf > 0) {
            
            // skip blank newlines
            if ((bytesInBuf == 1 && buf[0] == LF) ||
                        (bytesInBuf ==2 && buf[0] == CR && buf[1] == LF)) {
                bytesInBuf = 0;
            }
            
            byte[] toPrint = new byte[bytesInBuf];
            System.arraycopy(buf, 0, toPrint, 0, bytesInBuf);
            
            switch (mLevel) {
                case error:
                    mLog.error(toPrint);
                    break;
                case warn:
                    mLog.warn(toPrint);
                    break;
                case info:
                    mLog.info(toPrint);
                    break;
                case debug:
                default:
                    mLog.debug(toPrint);
                break;
            }
            bytesInBuf = 0;
        }
    }
}