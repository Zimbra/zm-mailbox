/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

public class ExpandMimeMessage {
    
    private static Log sLog = LogFactory.getLog(ExpandMimeMessage.class);
    
    private MimeMessage mMimeMessage;
    private MimeMessage mExpandedMessage;
    
    public ExpandMimeMessage(MimeMessage original) {
        mMimeMessage = original;
        mExpandedMessage = original;
    }

    /**
     * Copies the <tt>MimeMessage</tt> if a converter would want 
     * to make a change, but doesn't alter the original MimeMessage.
     */
    private class ForkMimeMessage implements MimeVisitor.ModificationCallback {
        private boolean mForked = false;

        public boolean onModification() {
            if (mForked)
                return false;

            try {
                mForked = true;
                mExpandedMessage = new Mime.FixedMimeMessage(mMimeMessage);
            } catch (Exception e) {
                sLog.warn("Unable to fork MimeMessage.", e);
            }
            return false;
        }
    }
    
    /**
     * Returns the expanded message, or the original message
     * if no expansion was done.
     */
    public MimeMessage getExpanded() {
        return mExpandedMessage;
    }
    
    public boolean wasExpanded() {
        return mExpandedMessage != mMimeMessage;
    }

    /** 
     * Applies all registered on-the-fly MIME converters to a the
     * encapsulated message.  The original message is not modified.
     *   
     * @return a new <tt>MimeMessage</tt> if converters were applied, or
     * <tt>null</tt> if not. 
     *         
     * @see MimeVisitor#registerConverter
     */
    public boolean expand()
    throws MessagingException {
        MimeVisitor.ModificationCallback forkCallback = new ForkMimeMessage();

        // first, find out if *any* of the converters would be triggered (but don't change the message)
        try {
            for (Class<? extends MimeVisitor> vclass : MimeVisitor.getConverters()) {
                if (mExpandedMessage == mMimeMessage)
                    vclass.newInstance().setCallback(forkCallback).accept(mMimeMessage);
                // if there are attachments to be expanded, expand them in the MimeMessage *copy*
                if (mExpandedMessage != mMimeMessage)
                    vclass.newInstance().accept(mExpandedMessage);
            }
        } catch (IllegalAccessException e) {
            mExpandedMessage = mMimeMessage;
            throw new MessagingException("Unable to instantiate MimeVisitor", e);
        } catch (InstantiationException e) {
            mExpandedMessage = mMimeMessage;
            throw new MessagingException("Unable to instantiate MimeVisitor", e);
        }
        
        return wasExpanded();
    }

}
