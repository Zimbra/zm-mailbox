package com.zimbra.cs.redolog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.CommitTxn;

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

public class CommitIdTest extends EasyMockSupport {
    private CommitId id;
    private CommitTxn commitTxn;

    @Before
    public void setUp() {
        commitTxn = createMock(CommitTxn.class);
        EasyMock.expect(commitTxn.getTimestamp()).andReturn((long)1);
        EasyMock.expect(commitTxn.getTransactionId())
            .andReturn(new TransactionId(2, 3));
    }

    @Test
    public void id() {
        replayAll();
        id = new CommitId(4, commitTxn);
        Assert.assertEquals("Sequence != 4", 4, id.getRedoSeq());
        Assert.assertEquals("encodeToString != 4-1-2-3",
                            "4-1-2-3", id.encodeToString());
    }

    @Test
    public void matches() {
        EasyMock.expect(commitTxn.getTimestamp()).andReturn((long)1);
        EasyMock.expect(commitTxn.getTransactionId())
            .andReturn(new TransactionId(2, 3));
        // Change timestamp for the next try.
        EasyMock.expect(commitTxn.getTimestamp()).andReturn((long)5);
        EasyMock.expect(commitTxn.getTransactionId())
            .andReturn(new TransactionId(2, 3));
        replayAll();
        id = new CommitId(4, commitTxn);
        Assert.assertTrue(id.matches(commitTxn));

        Assert.assertFalse(id.matches(commitTxn));
    }

    @Test
    public void encodeDecode() throws Exception {
        replayAll();
        id = new CommitId(4, commitTxn);
        Assert.assertEquals("id mismatch on decode",
                            id, CommitId.decodeFromString(id.encodeToString()));
    }

    @Test(expected = ServiceException.class)
    public void decodeJunk() throws Exception {
        CommitId.decodeFromString("not-a-Commit-Id");
    }
}
