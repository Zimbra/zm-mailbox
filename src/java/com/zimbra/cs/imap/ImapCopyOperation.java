package com.zimbra.cs.imap;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.CopyOperation;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class ImapCopyOperation extends Operation {

    private static int LOAD = 15;
        static {
            Operation.Config c = loadConfig(CopyOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    int mItemId;
    byte mType;
    int mTargetId;

    MailItem mSource;

    public ImapCopyOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                int itemId, byte type, int targetId)
    {
        super(session, oc, mbox, req, LOAD);
        
        mItemId = itemId;
        mType = type;
        mTargetId = targetId;
    }

    protected void callback() throws ServiceException {
        try {
            mSource = getMailbox().imapCopy(this.getOpCtxt(), mItemId, mType, mTargetId);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException execiting " + this, e);
        }
    }

    public MailItem getSource() { return mSource; }

    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        toRet.append(" id=").append(mItemId).append(" type=").append(mType).append(" target=").append(mTargetId);
        return toRet.toString();
    }
}
