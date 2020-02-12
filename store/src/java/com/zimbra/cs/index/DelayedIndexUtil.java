package com.zimbra.cs.index;

import java.io.IOException;
import java.util.EnumSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;

public class DelayedIndexUtil {

    public static void asyncDeleteIndex(Mailbox mbox, OperationContext octxt) {
        Thread deleteIndex = new Thread() {
            @Override
            public void run() {
                try {
                    mbox.index.deleteIndex();
                    if (mbox.getContactCount() > 0) {
                        // Rebuild contact index for sieve rules.
                        mbox.index.startReIndexByType(EnumSet.of(MailItem.Type.CONTACT), octxt, true);
                    }
                } catch (IOException | ServiceException e) {
                    ZimbraLog.index.error("error deleting index");
                }
            }

        };
        deleteIndex.start();
    }

    public static void asyncReIndex(Mailbox mbox, OperationContext octxt) {
        Thread reIndex = new Thread() {
            @Override
            public void run() {
                try {
                    mbox.index.startReIndex(octxt);
                } catch (ServiceException e) {
                    ZimbraLog.index.error("problem re-indexing mailbox %s", e);
                }
            }
        };
        reIndex.start();
    }
}
