package com.zimbra.cs.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.CopyOperation;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.Pair;

public class ImapCopyOperation extends Operation {

    private static int LOAD = 15;
        static {
            Operation.Config c = loadConfig(CopyOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    static final int SUGGESTED_BATCH_SIZE = 50;

    List<Integer> mItemIds;
    byte mType;
    int mFolderId;

    List<Pair<MailItem,MailItem>> mSources;


    public ImapCopyOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                List<Integer> itemIds, byte type, int folderId)
    {
        super(session, oc, mbox, req, LOAD * itemIds.size());
        mFolderId = folderId;
        mType = type;
        mItemIds = itemIds;
    }

    public ImapCopyOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                Collection<ImapMessage> i4set, int folderId)
    {
        super(session, oc, mbox, req, LOAD * i4set.size());

        mFolderId = folderId;
        mType = MailItem.TYPE_UNKNOWN;
        mItemIds = new ArrayList<Integer>();
        for (ImapMessage i4msg : i4set) {
            mItemIds.add(i4msg.msgId);
            if (mItemIds.size() == 1)
                mType = i4msg.getType();
            else if (i4msg.getType() != mType)
                mType = MailItem.TYPE_UNKNOWN;
        }
    }

    protected void callback() throws ServiceException {
        try {
            mSources = getMailbox().imapCopy(this.getOpCtxt(), mItemIds, mType, mFolderId);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException execiting " + this, e);
        }
    }

    public List<Pair<MailItem,MailItem>> getMessages() { return mSources; }

    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        toRet.append(" id=").append(mItemIds).append(" type=").append(mType).append(" target=").append(mFolderId);
        return toRet.toString();
    }
}
