package com.zimbra.qa.unittest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Set;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.cs.imap.ImapRemoteSession;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;
import org.junit.Ignore;

@Ignore("For Zimbra-X just test the configured IMAP via proxy")
public abstract class TestImapNotificationsViaMailbox extends SharedImapNotificationTests {

    @Override
    protected void runOp(MailboxOperation op, ZMailbox zmbox, ZFolder folder) throws Exception {
        ImapServerListener listener = ImapServerListenerPool.getInstance().get(zmbox);
        Set<ImapRemoteSession> sessions = listener.getListeners(zmbox.getAccountId(), Integer.valueOf(folder.getId()));
        assertFalse(String.format("Folder %s does not have any IMAP listeners", folder.getPath()), sessions.isEmpty());
        ImapRemoteSession session = sessions.iterator().next();
        ZMailbox imapzmbox = (ZMailbox) session.getMailbox();
        op.run(imapzmbox);
        String failure = op.checkResult();
        assertNull(failure, failure);
    }

}
