/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar.cache;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.Color;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;

import junit.framework.Assert;

public class InstanceDataTest {
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

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInstanceWithoutColor() throws Exception {
        Color color = null;
        InstanceData inst = createInstanceData(color);
        Assert.assertNull(inst.getRgbColor());
        Metadata metadata = inst.encodeMetadata();
        inst = new InstanceData(metadata);
        Assert.assertEquals(new Color(MailItem.DEFAULT_COLOR), inst.getRgbColor());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInstanceWithMappedColor() throws Exception {
        Color color = new Color((byte)3); // #6acb9e
        InstanceData inst = createInstanceData(color);
        Assert.assertEquals(color, inst.getRgbColor());
        Metadata metadata = inst.encodeMetadata();
        inst = new InstanceData(metadata);
        Assert.assertEquals(color, inst.getRgbColor());
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateInstanceWithCustomColor() throws Exception {
        Color color = new Color("#2e8af3");
        InstanceData inst = createInstanceData(color);
        Assert.assertEquals(color, inst.getRgbColor());
        Metadata metadata = inst.encodeMetadata();
        inst = new InstanceData(metadata);
        Assert.assertEquals(color, inst.getRgbColor());
    }

    private InstanceData createInstanceData(Color clr) {
        String recurIdZ = null;
        long dtStart = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        Long duration = new Long(10000);
        Long alarmAt = null;
        Long tzOffset = null;
        String partStat = null;
        String freeBusyActual = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
        String percentComplete = null;
        Color color = clr;
        InstanceData inst = new InstanceData(recurIdZ, dtStart, duration, alarmAt, tzOffset,
            partStat, freeBusyActual, percentComplete, color);
        return inst;
    }
}
