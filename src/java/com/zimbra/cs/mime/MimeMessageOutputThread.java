/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
