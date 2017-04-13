/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class ContactActionHelper extends ItemActionHelper {

    public static ContactActionHelper UPDATE(ZimbraSoapContext zsc, OperationContext octxt,
            Mailbox mbox, List<Integer> ids, ItemId iidFolder,
            String flags, String[] tags, Color color, ParsedContact pc)
    throws ServiceException {
        ContactActionHelper ca = new ContactActionHelper(octxt, mbox, zsc.getResponseProtocol(), ids, Op.UPDATE);
        ca.setIidFolder(iidFolder);
        ca.setFlags(flags);
        ca.setTags(tags);
        ca.setColor(color);
        ca.setParsedContact(pc);
        ca.schedule();
        return ca;
    }

    // only when OP=UPDATE
    private ParsedContact mParsedContact;


    public void setParsedContact(ParsedContact pc) {
        assert(mOperation == Op.UPDATE);
        mParsedContact = pc;
    }

    ContactActionHelper(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto, List<Integer> ids, Op op) throws ServiceException {
        super(octxt, mbox, responseProto, ids, op, MailItem.Type.CONTACT, true, null);
    }

    @Override
    protected void schedule() throws ServiceException {
        // iterate over the local items and perform the requested operation
        switch (mOperation) {
        case UPDATE:
            if (!mIidFolder.belongsTo(getMailbox())) {
                throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
            }

            if (mIidFolder.getId() > 0) {
                getMailbox().move(getOpCtxt(), itemIds, type, mIidFolder.getId(), mTargetConstraint);
            }
            if (mTags != null || mFlags != null) {
                getMailbox().setTags(getOpCtxt(), itemIds, type, Flag.toBitmask(mFlags), mTags, mTargetConstraint);
            }
            if (mColor != null) {
                getMailbox().setColor(getOpCtxt(), itemIds, type, mColor);
            }
            if (mParsedContact != null) {
                for (int id : itemIds) {
                    getMailbox().modifyContact(getOpCtxt(), id, mParsedContact);
                }
            }
            break;
        default:
            throw ServiceException.INVALID_REQUEST("unknown operation: " + mOperation, null);
        }

        List<String> successes = new ArrayList<String>();
        for (int id : itemIds) {
            successes.add(mIdFormatter.formatItemId(id));
        }
        mResult = new ItemActionResult();
        mResult.appendSuccessIds(successes);
    }

    @Override
    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        if (mOperation == Op.UPDATE) {
            if (mParsedContact != null) {
                toRet.append(" Fields=").append(mParsedContact.getFields());
            }
        }
        return toRet.toString();
    }
}
