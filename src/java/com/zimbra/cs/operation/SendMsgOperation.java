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
package com.zimbra.cs.operation;

import java.util.List;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.session.Session;

public class SendMsgOperation extends Operation {

    private static int LOAD = 3;
    static {
        Operation.Config c = loadConfig(SendMsgOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }

    private boolean mSaveToSent;
    private MimeMessage mMm;
    private List<InternetAddress> mNewContacts;
    private List<Upload> mUploads;
    private int mOrigMsgId;
    private String mReplyType;
    private boolean mIgnoreFailedAddresses;

    private int mMsgId;

    public SendMsgOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                boolean saveToSent, MimeMessage mm, List<InternetAddress> newContacts,
                List<Upload> uploads, int origMsgId, String replyType, boolean ignoreFailedAddresses) {
        super(session, oc, mbox, req, LOAD);

        mSaveToSent = saveToSent;
        mMm = mm;
        mNewContacts = newContacts;
        mUploads = uploads;
        mOrigMsgId = origMsgId;
        mReplyType = replyType;
        mIgnoreFailedAddresses = ignoreFailedAddresses;
    }

    protected void callback() throws ServiceException {
        mMsgId = MailSender.sendMimeMessage(getOpCtxt(), getMailbox(), mSaveToSent, mMm,
                    mNewContacts, mUploads,
                    mOrigMsgId, mReplyType, mIgnoreFailedAddresses);
    }

    public int getMsgId() { return mMsgId; }
}
