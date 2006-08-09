package com.zimbra.cs.operation;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

public class ContactActionOperation extends ItemActionOperation {

    public static ContactActionOperation UPDATE(ZimbraSoapContext zc, Session session, OperationContext oc,
                                                Mailbox mbox, Requester req, List<Integer> ids, ItemId iidFolder,
                                                String flags, String tags, byte color, Map<String, String> fields)
    throws ServiceException {
        ContactActionOperation ca = new ContactActionOperation(zc, session, oc, mbox, req, LOAD, ids, Op.UPDATE);
        ca.setIidFolder(iidFolder);
        ca.setFlags(flags);
        ca.setTags(tags);
        ca.setColor(color);
        ca.setFields(fields);
        ca.schedule();
        return ca;
    }

    // only when OP=UPDATE
    private Map<String, String> mFields;


    public void setFields(Map<String, String> fields) {                        
        assert(mOp == Op.UPDATE);
        mFields = (fields == null || fields.isEmpty() ? null : fields); 
    }

    ContactActionOperation(ZimbraSoapContext zc, Session session, OperationContext oc, Mailbox mbox,
            Requester req, int baseLoad, List<Integer> ids, Op op)
            throws ServiceException {
        super(zc, session, oc, mbox, req, baseLoad, ids, op, MailItem.TYPE_CONTACT, true, null);
    }

    protected void callback() throws ServiceException {
        // iterate over the local items and perform the requested operation
        switch (mOp) {
            case UPDATE:
                if (!mIidFolder.belongsTo(getMailbox()))
                    throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                
                if (mIidFolder.getId() > 0)
                    getMailbox().move(getOpCtxt(), mIds, mType, mIidFolder.getId(), mTcon);
                if (mTags != null || mFlags != null)
                    getMailbox().setTags(getOpCtxt(), mIds, mType, mFlags, mTags, mTcon);
                if (mColor >= 0)
                    getMailbox().setColor(getOpCtxt(), mIds, mType, mColor);
                if (mFields != null)
                    for (int id : mIds)
                        getMailbox().modifyContact(getOpCtxt(), id, mFields, true);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unknown operation: " + mOp, null);
        }

        StringBuilder successes = new StringBuilder();
        for (int id : mIds)
            successes.append(successes.length() > 0 ? "," : "").append(mZc.formatItemId(id));
        mResult = successes.toString();
    }

    public String toString() {
        StringBuffer toRet = new StringBuffer(super.toString());
        if (mOp == Op.UPDATE) {
            if (mFields != null)
                toRet.append(" Fields=").append(mFields);
        }
        return toRet.toString();
    }
}
