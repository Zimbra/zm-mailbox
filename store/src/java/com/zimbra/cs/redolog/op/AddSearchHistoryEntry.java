package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class AddSearchHistoryEntry extends RedoableOp {

    private Integer newSearchid;
    private String searchString;
    private boolean prompted = false;

    protected AddSearchHistoryEntry() {
        super(MailboxOperation.AddSearchHistoryEntry);
    }

    public AddSearchHistoryEntry(int mailboxId, String searchString) {
        this();
        setMailboxId(mailboxId);
        this.searchString = searchString;
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.addToSearchHistory(getOperationContext(), searchString, getTimestamp());
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder();
        if (newSearchid != null) {
            sb.append("id=").append(newSearchid).append(", ");
        }
        sb.append("searchString=").append(searchString);
        if (prompted) {
            sb.append(", prompted=true");
        }
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(searchString);
        boolean hasId = newSearchid != null;
        out.writeBoolean(hasId);
        if (hasId) {
            out.writeInt(newSearchid);
        }
        out.writeBoolean(prompted);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        searchString = in.readUTF();
        if (in.readBoolean()) {
            newSearchid = in.readInt();
        }
        prompted = in.readBoolean();
    }

    public void setSearchId(int id) {
        newSearchid = id;
    }

    public int getSearchId() {
        return newSearchid;
    }

    public void setPrompted(boolean prompted) {
        this.prompted = prompted;
    }

    public boolean isPrompted() {
        return prompted;
    }
}
