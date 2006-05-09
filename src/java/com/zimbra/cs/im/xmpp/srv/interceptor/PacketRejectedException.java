/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.interceptor;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown by a PacketInterceptor when a packet is prevented from being processed. If the packet was
 * received then it will not be processed and a not_allowed error will be sent back to the sender
 * of the packet. If the packet was going to be sent then the sending will be aborted.
 *
 * @see PacketInterceptor
 * @author Gaston Dombiak
 */
public class PacketRejectedException extends Exception {
    private static final long serialVersionUID = 1L;

    private Throwable nestedThrowable = null;

    /**
     * Text to include in a message that will be sent to the sender of the packet that got
     * rejected. If no text is specified then no message will be sent to the user.
     */
    private String rejectionMessage;

    public PacketRejectedException() {
        super();
    }

    public PacketRejectedException(String msg) {
        super(msg);
    }

    public PacketRejectedException(Throwable nestedThrowable) {
        this.nestedThrowable = nestedThrowable;
    }

    public PacketRejectedException(String msg, Throwable nestedThrowable) {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    public void printStackTrace() {
        super.printStackTrace();
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace();
        }
    }

    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(ps);
        }
    }

    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(pw);
        }
    }

    /**
     * Retuns the text to include in a message that will be sent to the sender of the packet
     * that got rejected or <tt>null</tt> if none was defined. If no text was specified then
     * no message will be sent to the sender of the rejected packet.
     *
     * @return the text to include in a message that will be sent to the sender of the packet
     *         that got rejected or <tt>null</tt> if none was defined.
     */
    public String getRejectionMessage() {
        return rejectionMessage;
    }

    /**
     * Sets the text to include in a message that will be sent to the sender of the packet
     * that got rejected or <tt>null</tt> if no message will be sent to the sender of the
     * rejected packet. Bt default, no message will be sent.
     *
     * @param rejectionMessage the text to include in the notification message for the rejection.
     */
    public void setRejectionMessage(String rejectionMessage) {
        this.rejectionMessage = rejectionMessage;
    }
}
