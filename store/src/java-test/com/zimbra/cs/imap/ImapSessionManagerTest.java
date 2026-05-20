/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.imap.ImapListener.ImapFolderData;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ImapSessionManager.class })
public class ImapSessionManagerTest {

    private ImapSessionManager spyManager;

    private ImapListener session;

    private ImapFolderData mFolder;

    private boolean origSkipLargeFolder;

    private int origMaxMessageCount;

    @BeforeClass
    public static void init() throws Exception {
        // matches the pattern used by other tests in this package
        LC.imap_use_ehcache.setDefault(false);
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        // remember LC values so we can restore them after each test
        origSkipLargeFolder = LC.imap_ehcache_skip_large_folder.booleanValue();
        origMaxMessageCount = LC.imap_ehcache_folder_max_message_count.intValue();

        // Spy the singleton so we can stub out cacheKey(). The real cacheKey()
        // walks the Mailbox/Folder graph which we don't want in a unit test.
        spyManager = PowerMockito.spy(ImapSessionManager.getInstance());
        PowerMockito.doReturn("stub-cache-key")
                .when(spyManager, "cacheKey", any(ImapListener.class), anyBoolean());

        session = Mockito.mock(ImapListener.class);
        mFolder = Mockito.mock(ImapFolderData.class);
        when(mFolder.getId()).thenReturn(42);

        // mFolder is a protected field on ImapListener; inject our mock via reflection
        Field f = ImapListener.class.getDeclaredField("mFolder");
        f.setAccessible(true);
        f.set(session, mFolder);

        // make the session a plain, non-interactive, non-virtual local listener
        when(session.isInteractive()).thenReturn(false);
        when(session.isVirtual()).thenReturn(false);
        // getMailbox() returning null short-circuits the post-serialize block in closeFolder
        when(session.getMailbox()).thenReturn(null);
    }

    @After
    public void tearDown() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(origSkipLargeFolder);
        LC.imap_ehcache_folder_max_message_count.setDefault(origMaxMessageCount);
    }

    /**
     * When the feature flag is ON and the folder's message count strictly
     * exceeds the configured threshold, the session must NOT be unloaded
     * (i.e., not pushed into Ehcache).
     */
    @Test
    public void testCloseFolder_SkipsUnloadWhenFolderExceedsThreshold() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(true);
        LC.imap_ehcache_folder_max_message_count.setDefault(100);
        when(mFolder.getSize()).thenReturn(101);

        spyManager.closeFolder(session, false);

        verify(session, never()).unload(anyBoolean());
    }

    /**
     * Verifies behavior at a realistic production-scale threshold.
     */
    @Test
    public void testCloseFolder_SkipsUnloadWhenFolderFarExceedsThreshold() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(true);
        LC.imap_ehcache_folder_max_message_count.setDefault(80000);
        when(mFolder.getSize()).thenReturn(200000);

        spyManager.closeFolder(session, false);

        verify(session, never()).unload(anyBoolean());
    }

    /**
     * When the feature flag is ON but the folder is small enough, the session
     * should still be unloaded (normal happy path).
     */
    @Test
    public void testCloseFolder_UnloadsWhenFolderBelowThreshold() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(true);
        LC.imap_ehcache_folder_max_message_count.setDefault(100);
        when(mFolder.getSize()).thenReturn(50);

        spyManager.closeFolder(session, false);

        verify(session, times(1)).unload(false);
    }

    /**
     * Boundary: the check is "actualMsgCount > threshold" (strictly greater),
     * so a folder exactly at the threshold must still be unloaded.
     */
    @Test
    public void testCloseFolder_UnloadsAtThresholdBoundary() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(true);
        LC.imap_ehcache_folder_max_message_count.setDefault(100);
        when(mFolder.getSize()).thenReturn(100);

        spyManager.closeFolder(session, false);

        verify(session, times(1)).unload(false);
    }

    /**
     * When the feature flag is OFF the size check must be ignored entirely and
     * the session must always be unloaded.
     */
    @Test
    public void testCloseFolder_UnloadsWhenSkipDisabledEvenForHugeFolder() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(false);
        LC.imap_ehcache_folder_max_message_count.setDefault(100);
        when(mFolder.getSize()).thenReturn(9_999_999);

        spyManager.closeFolder(session, false);

        verify(session, times(1)).unload(false);
    }

    /**
     * Virtual (search-folder backed) sessions never enter the serialize block,
     * so the new size check should never run and unload() must not be called.
     */
    @Test
    public void testCloseFolder_VirtualSessionIsDetachedAndNotUnloaded() throws Exception {
        when(session.isVirtual()).thenReturn(true);
        // make the skip branch attractive to prove we never reach it
        LC.imap_ehcache_skip_large_folder.setDefault(true);
        LC.imap_ehcache_folder_max_message_count.setDefault(0);

        spyManager.closeFolder(session, false);

        verify(session, times(1)).detach();
        verify(session, never()).unload(anyBoolean());
    }

    /**
     * Interactive sessions must be inactivated before any serialization
     * decision is made.
     */
    @Test
    public void testCloseFolder_InteractiveSessionIsInactivated() throws Exception {
        when(session.isInteractive()).thenReturn(true);
        LC.imap_ehcache_skip_large_folder.setDefault(false);
        when(mFolder.getSize()).thenReturn(1);

        spyManager.closeFolder(session, false);

        verify(session, times(1)).inactivate();
        // not virtual, skip disabled, small folder -> we should still unload
        verify(session, times(1)).unload(false);
    }

    /**
     * If serialization throws, closeFolder must swallow the exception and not
     * propagate it to the caller (it's a best-effort cache write).
     */
    @Test
    public void testCloseFolder_SwallowsUnloadException() throws Exception {
        LC.imap_ehcache_skip_large_folder.setDefault(false);
        when(mFolder.getSize()).thenReturn(1);
        Mockito.doThrow(new RuntimeException("boom")).when(session).unload(anyBoolean());

        // must not throw
        spyManager.closeFolder(session, false);

        verify(session, times(1)).unload(false);
    }
}

