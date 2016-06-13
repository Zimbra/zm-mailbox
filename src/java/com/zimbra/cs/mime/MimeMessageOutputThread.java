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
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.PipedOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * Thread that writes the content of a <tt>MimeMessage</tt> to a
 * <tt>PipedOutputStream</tt>.  Required because the only access
 * that JavaMail provides to the RFC 822 version of a message is
 * via an <tt>OutputStream</tt>.
 */
public class MimeMessageOutputThread implements Runnable {
    
    private PipedOutputStream mOut;
    private MimeMessage mMsg;
    
    MimeMessageOutputThread(MimeMessage msg, PipedOutputStream out) {
        if (msg == null) {
            throw new NullPointerException("msg cannot be null");
        }
        if (out == null) {
            throw new NullPointerException("out cannot be null");
        }
        mMsg = msg;
        mOut = out;
    }
    
    public void run() {
        try {
            mMsg.writeTo(mOut);
        } catch (IOException e) {
            ZimbraLog.misc.warn("Unable to write MimeMessage to output stream.", e);
        } catch (MessagingException e) {
            ZimbraLog.misc.warn("Unable to write MimeMessage to output stream.", e);
        } catch (Throwable t) {
            ZimbraLog.misc.warn("Unable to write MimeMessage to output stream.", t);
        } finally {
            ByteUtil.closeStream(mOut);
        }
    }

}
