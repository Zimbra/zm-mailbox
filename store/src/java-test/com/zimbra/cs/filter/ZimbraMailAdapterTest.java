package com.zimbra.cs.filter;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IncomingMessageHandler.class, Mailbox.class})
public class ZimbraMailAdapterTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void testUpdateIncomingBlob() throws Exception{
        int mboxId = 10;

        Mailbox mbox = PowerMockito.mock(Mailbox.class);
        Mockito.when(mbox.getId()).thenReturn(mboxId);

        List<Integer> targetMailboxIds = new ArrayList<Integer>(1);
        targetMailboxIds.add(mboxId);
        DeliveryContext sharedDeliveryCtxt = new DeliveryContext(true, targetMailboxIds);

        String testStr = "test";
        ParsedMessage pm = new ParsedMessage(testStr.getBytes(), false);
        IncomingMessageHandler handler = PowerMockito.mock(IncomingMessageHandler.class);
        Mockito.when(handler.getDeliveryContext()).thenReturn(sharedDeliveryCtxt);
        Mockito.when(handler.getParsedMessage()).thenReturn(pm);
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mbox, handler);
        mailAdapter.updateIncomingBlob();

        Assert.assertNotNull(sharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId));
        Assert.assertNull(sharedDeliveryCtxt.getIncomingBlob());

        DeliveryContext nonSharedDeliveryCtxt = new DeliveryContext(false, targetMailboxIds);
        Mockito.when(handler.getDeliveryContext()).thenReturn(nonSharedDeliveryCtxt);
        mailAdapter.updateIncomingBlob();

        Assert.assertNull(nonSharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId));
        Assert.assertNotNull(nonSharedDeliveryCtxt.getIncomingBlob());

        Mockito.when(handler.getDeliveryContext()).thenReturn(sharedDeliveryCtxt);
        Blob blobFile = sharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId);
        mailAdapter.cloneParsedMessage();
        mailAdapter.updateIncomingBlob();
        Assert.assertNotNull(sharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId));
        Assert.assertNull(sharedDeliveryCtxt.getIncomingBlob());
        Assert.assertNotSame(blobFile, sharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId));

        Mockito.when(handler.getDeliveryContext()).thenReturn(nonSharedDeliveryCtxt);
        blobFile = nonSharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId);
        mailAdapter.cloneParsedMessage();
        mailAdapter.updateIncomingBlob();
        Assert.assertNull(nonSharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId));
        Assert.assertNotNull(nonSharedDeliveryCtxt.getIncomingBlob());
        Assert.assertEquals(blobFile, nonSharedDeliveryCtxt.getMailBoxSpecificBlob(mboxId));
    }
}
