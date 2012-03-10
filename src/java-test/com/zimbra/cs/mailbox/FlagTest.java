/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link Flag}.
 *
 * @author ysasaki
 */
public final class FlagTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void id() {
        Assert.assertEquals(-1, Flag.ID_FROM_ME);
        Assert.assertEquals(-2, Flag.ID_ATTACHED);
        Assert.assertEquals(-3, Flag.ID_REPLIED);
        Assert.assertEquals(-4, Flag.ID_FORWARDED);
        Assert.assertEquals(-5, Flag.ID_COPIED);
        Assert.assertEquals(-6, Flag.ID_FLAGGED);
        Assert.assertEquals(-7, Flag.ID_DRAFT);
        Assert.assertEquals(-8, Flag.ID_DELETED);
        Assert.assertEquals(-9, Flag.ID_NOTIFIED);
        Assert.assertEquals(-10, Flag.ID_UNREAD);
        Assert.assertEquals(-11, Flag.ID_HIGH_PRIORITY);
        Assert.assertEquals(-12, Flag.ID_LOW_PRIORITY);
        Assert.assertEquals(-13, Flag.ID_VERSIONED);
        Assert.assertEquals(-14, Flag.ID_INDEXING_DEFERRED);
        Assert.assertEquals(-15, Flag.ID_POPPED);
        Assert.assertEquals(-16, Flag.ID_NOTE);
        Assert.assertEquals(-17, Flag.ID_PRIORITY);
        Assert.assertEquals(-18, Flag.ID_POST);
        Assert.assertEquals(-20, Flag.ID_SUBSCRIBED);
        Assert.assertEquals(-21, Flag.ID_EXCLUDE_FREEBUSY);
        Assert.assertEquals(-22, Flag.ID_CHECKED);
        Assert.assertEquals(-23, Flag.ID_NO_INHERIT);
        Assert.assertEquals(-24, Flag.ID_INVITE);
        Assert.assertEquals(-25, Flag.ID_SYNCFOLDER);
        Assert.assertEquals(-26, Flag.ID_SYNC);
        Assert.assertEquals(-27, Flag.ID_NO_INFERIORS);
        Assert.assertEquals(-28, Flag.ID_ARCHIVED);
        Assert.assertEquals(-29, Flag.ID_GLOBAL);
        Assert.assertEquals(-30, Flag.ID_IN_DUMPSTER);
        Assert.assertEquals(-31, Flag.ID_UNCACHED);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void bitmask() {
        Assert.assertEquals(1, Flag.BITMASK_FROM_ME);
        Assert.assertEquals(2, Flag.BITMASK_ATTACHED);
        Assert.assertEquals(4, Flag.BITMASK_REPLIED);
        Assert.assertEquals(8, Flag.BITMASK_FORWARDED);
        Assert.assertEquals(16, Flag.BITMASK_COPIED);
        Assert.assertEquals(32, Flag.BITMASK_FLAGGED);
        Assert.assertEquals(64, Flag.BITMASK_DRAFT);
        Assert.assertEquals(128, Flag.BITMASK_DELETED);
        Assert.assertEquals(256, Flag.BITMASK_NOTIFIED);
        Assert.assertEquals(512, Flag.BITMASK_UNREAD);
        Assert.assertEquals(1024, Flag.BITMASK_HIGH_PRIORITY);
        Assert.assertEquals(2048, Flag.BITMASK_LOW_PRIORITY);
        Assert.assertEquals(4096, Flag.BITMASK_VERSIONED);
        Assert.assertEquals(8192, Flag.BITMASK_INDEXING_DEFERRED);
        Assert.assertEquals(16384, Flag.BITMASK_POPPED);
        Assert.assertEquals(32768, Flag.BITMASK_NOTE);
        Assert.assertEquals(65536, Flag.BITMASK_PRIORITY);
        Assert.assertEquals(131072, Flag.BITMASK_POST);
        Assert.assertEquals(524288, Flag.BITMASK_SUBSCRIBED);
        Assert.assertEquals(1048576, Flag.BITMASK_EXCLUDE_FREEBUSY);
        Assert.assertEquals(2097152, Flag.BITMASK_CHECKED);
        Assert.assertEquals(4194304, Flag.BITMASK_NO_INHERIT);
        Assert.assertEquals(8388608, Flag.BITMASK_INVITE);
        Assert.assertEquals(16777216, Flag.BITMASK_SYNCFOLDER);
        Assert.assertEquals(33554432, Flag.BITMASK_SYNC);
        Assert.assertEquals(67108864, Flag.BITMASK_NO_INFERIORS);
        Assert.assertEquals(134217728, Flag.BITMASK_ARCHIVED);
        Assert.assertEquals(268435456, Flag.BITMASK_GLOBAL);
        Assert.assertEquals(536870912, Flag.BITMASK_IN_DUMPSTER);
        Assert.assertEquals(1073741824, Flag.BITMASK_UNCACHED);
    }

    @Test
    public void equals() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertEquals(Flag.FlagInfo.UNREAD.toFlag(mbox), Flag.FlagInfo.UNREAD.toFlag(mbox));
        Assert.assertEquals(Flag.FlagInfo.UNREAD.toFlag(mbox).hashCode(), Flag.FlagInfo.UNREAD.toFlag(mbox).hashCode());
    }

    @Test
    public void unique() throws Exception {
        Set<Character> seen = new HashSet<Character>();
        for (Flag.FlagInfo finfo : Flag.FlagInfo.values()) {
            if (!finfo.isHidden()) {
                Assert.assertFalse("have not yet seen " + finfo.ch, seen.contains(finfo.ch));
                seen.add(finfo.ch);
            }
        }
    }
}
