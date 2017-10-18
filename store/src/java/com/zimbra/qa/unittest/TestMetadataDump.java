/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.util.MetadataDump;

public class TestMetadataDump {

    @Rule
    public TestName testInfo = new TestName();
    public final static String ZMMETADUMP ="/opt/zimbra/bin/zmmetadump";

    private String USER_NAME = null;
    private String prefix = null;

    @Before
    public void setUp() throws Exception {
        prefix = testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user1";
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    @Test
    public void zmmetadumpHelp() throws Exception
    {
        List<String> cmdArgs = Lists.newArrayList();
        cmdArgs.add(ZMMETADUMP);
        cmdArgs.add("-h");
        String cmd = Joiner.on(' ').join(cmdArgs);
        int exitValue;
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        Process process = pb.start();
        exitValue = process.waitFor();
        String error = new String(ByteUtil.getContent(process.getErrorStream(), -1));
        String lookFor = "Usage: zmmetadump -m";
        assertTrue(String.format("STDERR from '%s' should contain '%s' was:\n%s", cmd, lookFor, error),
                error.contains(lookFor));
        assertEquals(String.format("Exit code for '%s'", cmd), 0, exitValue);
    }

    /* ZCS-3227 zmmetadump non-functional due to NPE being thrown.
     * Not the first time commandline tools like this have been broken, so good to have a test
     * that will pick up on it.
     */
    @Test
    public void zmmetadumpItem() throws Exception
    {
        Account acct = TestUtil.createAccount(USER_NAME);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        String subject = prefix + "Test Message";
        Message msg = TestUtil.addMessage(mbox, subject);
        List<String> cmdArgs = Lists.newArrayList();
        cmdArgs.add(ZMMETADUMP);
        cmdArgs.add("-m");
        cmdArgs.add(acct.getName());
        cmdArgs.add("-i");
        cmdArgs.add(Integer.toString(msg.getId()));
        String cmd = Joiner.on(' ').join(cmdArgs);
        int exitValue;
        ProcessBuilder pb = new ProcessBuilder(cmdArgs);
        Process process = pb.start();
        exitValue = process.waitFor();
        String error = new String(ByteUtil.getContent(process.getErrorStream(), -1));
        String stdout = new String(ByteUtil.getContent(process.getInputStream(), -1));
        if ((exitValue != 0) && (!error.isEmpty())) {
            ZimbraLog.test.info("STDERR:%s", error);
        }
        String[] patts = { " subject: " + subject,
                MetadataDump.DB_COLS_HDR, MetadataDump.METADATA_HDR, MetadataDump.BLOBPATH_HDR };
        for (String patt : patts) {
            assertTrue(String.format("STDOUT from '%s' should contain '%s' was:\n%s", cmd, patt, stdout),
                    stdout.contains(patt));
        }
        assertEquals(String.format("Exit code for '%s'", cmd), 0, exitValue);
    }
}
