package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetSavedSearchStatus extends RedoableOp {

    private String searchString;
    private SavedSearchStatus status;

    protected SetSavedSearchStatus() {
        super(MailboxOperation.SetSavedSearchStatus);
    }

    public SetSavedSearchStatus(int mboxId, String searchString, SavedSearchStatus status) {
        this();
        setMailboxId(mboxId);
        this.searchString = searchString;
        this.status = status;
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setSavedSearchPromptStatus(getOperationContext(), searchString, status);
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("searchString=").append(searchString)
                .append(", status=").append(status.toString());
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(searchString);
        out.writeShort(status.getId());
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        searchString = in.readUTF();
        try {
            status = SavedSearchStatus.of(in.readShort());
        } catch (ServiceException e) {
            ZimbraLog.redolog.error("cannot deserialize SetSavedSearchStatus; defaulting to NOT_PROMPTED", e);
            status = SavedSearchStatus.NOT_PROMPTED;
        }

    }

}
