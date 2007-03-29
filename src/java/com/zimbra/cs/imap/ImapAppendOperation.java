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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.operation.Requester;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;

public class ImapAppendOperation extends Operation  {
    private static int LOAD = 10;
        static {
            Operation.Config c = loadConfig(ImapAppendOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    private ImapHandler mHandler;   
    private ImapPath mTargetPath;
    private List<String> mFlagNames;
    private Date mDate;
    private byte[] mContent;
    private List<Tag> mNewTags;
    private StringBuilder mAppendHint;

    public ImapAppendOperation(Session session, OperationContext octxt, Mailbox mbox,
            				   ImapHandler handler, ImapPath path, List<String> flagNames,
            				   Date date, byte[] content, List<Tag> newTags, StringBuilder appendHint) {
        super(session, octxt, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
        mHandler = handler;
        mTargetPath = path;
        mFlagNames = flagNames;
        mDate = date;
        mContent = content;
        mNewTags = newTags;
        mAppendHint = appendHint;
    }

    protected void callback() throws ServiceException {
        synchronized (mMailbox) {
            Folder folder = mMailbox.getFolderByPath(this.getOpCtxt(), mTargetPath.asZimbraPath());
            if (!mTargetPath.isVisible())
                throw ImapServiceException.FOLDER_NOT_VISIBLE(mTargetPath.asImapPath());
            else if (!mTargetPath.isWritable(ACL.RIGHT_INSERT))
                throw ImapServiceException.FOLDER_NOT_WRITABLE(mTargetPath.asImapPath());

            byte sflags = 0;
            int flagMask = Flag.BITMASK_UNREAD;
            StringBuffer tagStr = new StringBuffer();
            if (mFlagNames != null) {
                List<ImapFlag> i4flags;
                if (mTargetPath.belongsTo(mSession.getMailbox()))
                    i4flags = mHandler.findOrCreateTags(mMailbox, mFlagNames, mNewTags);
                else
                    i4flags = mHandler.getSystemFlags(mFlagNames);

                for (ImapFlag i4flag : i4flags) {
                    if (!i4flag.mPermanent)
                        sflags |= i4flag.mBitmask;
                    else if (Tag.validateId(i4flag.mId))
                        tagStr.append(tagStr.length() == 0 ? "" : ",").append(i4flag.mId);
                    else if (i4flag.mPositive)
                        flagMask |= i4flag.mBitmask;
                    else
                        flagMask &= ~i4flag.mBitmask;
                }
            }

            try {
                boolean idxAttach = mMailbox.attachmentsIndexingEnabled();
                ParsedMessage pm = mDate != null ? new ParsedMessage(mContent, mDate.getTime(), idxAttach) :
                                                   new ParsedMessage(mContent, idxAttach);
                try {
                    if (!pm.getSender().equals("")) {
                        InternetAddress ia = new InternetAddress(pm.getSender());
                        if (AccountUtil.addressMatchesAccount(mMailbox.getAccount(), ia.getAddress()))
                            flagMask |= Flag.BITMASK_FROM_ME;
                    }
                } catch (Exception e) { }

                Message msg = mMailbox.addMessage(getOpCtxt(), pm, folder.getId(), true, flagMask, tagStr.toString());
                if (msg != null) {
                    if (mAppendHint != null) {
                        mAppendHint.append("[APPENDUID ").append(ImapFolder.getUIDValidity(folder))
                                   .append(' ').append(msg.getImapUid()).append("] ");
                    }
                    if (sflags != 0 && mHandler.getState() == ImapHandler.State.SELECTED) {
                        ImapMessage i4msg = mHandler.getSelectedFolder().getById(msg.getId());
                        if (i4msg != null)
                            i4msg.setSessionFlags(sflags, mHandler.getSelectedFolder());
                    }
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE(e.toString(), e);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE(e.toString(), e);
            }
        }
    }
}
