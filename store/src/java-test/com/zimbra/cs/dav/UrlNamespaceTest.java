package com.zimbra.cs.dav;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.RemoteCalendarCollection;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Mountpoint;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlNamespace.class, DavContext.class, Mountpoint.class, RemoteCalendarCollection.class})
public class UrlNamespaceTest {
    private DavContext ctxt;
    private Mountpoint item;
    private RemoteCalendarCollection rcc;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        ctxt = PowerMockito.mock(DavContext.class);
        item = PowerMockito.mock(Mountpoint.class);
        rcc = PowerMockito.mock(RemoteCalendarCollection.class);
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testGetResourceFromMailItem() throws Exception {
        PowerMockito.when(ctxt.useIcalDelegation()).thenReturn(Boolean.FALSE);
        PowerMockito.when(item.getType()).thenReturn(MailItem.Type.MOUNTPOINT);
        PowerMockito.when(item.getDefaultView()).thenReturn(MailItem.Type.TASK);
        PowerMockito.whenNew(PowerMockito.constructor(RemoteCalendarCollection.class)).withArguments(ctxt, item).thenReturn(rcc);

        DavResource resource = UrlNamespace.getResourceFromMailItem(ctxt, item);

        Assert.assertTrue(resource instanceof RemoteCalendarCollection);
    }
}
