package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.PipedOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

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
        mMsg = msg;
        mOut = out;
    }
    
    public void run() {
        try {
            mMsg.writeTo(mOut);
            mOut.close();
        } catch (IOException e) {
            ZimbraLog.misc.warn("Unable to write MimeMessage to output stream.", e);
        } catch (MessagingException e) {
            ZimbraLog.misc.warn("Unable to write MimeMessage to output stream.", e);
        }
    }

}
