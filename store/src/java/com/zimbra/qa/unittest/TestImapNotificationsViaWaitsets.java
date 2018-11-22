package com.zimbra.qa.unittest;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;
import com.zimbra.cs.session.SomeAccountsWaitSet;
import com.zimbra.cs.session.WaitSetMgr;
import org.junit.Ignore;

@Ignore("For Zimbra-X just test the configured IMAP via proxy")
public abstract class TestImapNotificationsViaWaitsets extends SharedImapNotificationTests {

    @Override
    protected void runOp(MailboxOperation op, ZMailbox zmbox, ZFolder folder) throws Exception {

        ImapServerListener listener = ImapServerListenerPool.getInstance().get(zmbox);
        String wsID = listener.getWSId();
        SomeAccountsWaitSet ws = (SomeAccountsWaitSet)(WaitSetMgr.lookup(wsID));
        long lastSequence = ws.getCurrentSeqNo();
        op.run(zmbox);
        boolean applied = false;
        int timeout = 6000;
        while(timeout > 0) {
            if(listener.getLastKnownSequenceNumber() > lastSequence) {
                applied = true;
                break;
            }
            timeout -= 500;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
        assertTrue("operation not applied within 6 seconds", applied);
        String failure = op.checkResult();
        assertNull(failure, failure);
    }

}
