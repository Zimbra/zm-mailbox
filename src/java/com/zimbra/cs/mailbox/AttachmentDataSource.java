/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mime.Mime;

public class AttachmentDataSource implements DataSource {

    private Contact mContact;
    private String mPartName;

    public AttachmentDataSource(Contact contact, String partName) {
        if (contact == null) {
            throw new NullPointerException("contact cannot be null");
        }
        if (partName == null) {
            throw new NullPointerException("partName cannot be null");
        }
        mContact = contact;
        mPartName = partName;
    }
    
    public String getContentType() {
        String contentType = null;
        MimePart mp = null;
        try {
            mp = getMimePart();
            if (mp != null) {
                contentType = mp.getContentType();
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.error("Unable to determine content type for contact %d.", mContact.getId(), e);
        }
        return contentType;
    }
    
    private MimePart getMimePart()
    throws MessagingException, ServiceException {
        MimeMessage msg = mContact.getMimeMessage(false);
        MimePart mp = null;
        try {
            mp = Mime.getMimePart(msg, mPartName);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to look up part " + mPartName + " for contact " + mContact.getId(), null);
        }
        
        if (mp == null) {
            ZimbraLog.mailbox.warn("Unable to find part %s for contact %d.", mPartName, mContact.getId());
        }
        return mp;
    }

    public InputStream getInputStream() throws IOException {
        try {
            return getMimePart().getInputStream();
        } catch (Exception e) {
            ZimbraLog.mailbox.error("Unable to get stream to part %s for contact %d.", mPartName, mContact.getId());
            throw new IOException(e.toString());
        }
    }

    public String getName() {
        MimePart mp = null;
        String name = null;
        try {
            mp = getMimePart();
            if (mp != null) {
                name = mp.getFileName();
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.error("Unable to determine the filename for contact %d, part %s.", mContact.getId(), mPartName, e);
        }
        return name;
    }

    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }
}
