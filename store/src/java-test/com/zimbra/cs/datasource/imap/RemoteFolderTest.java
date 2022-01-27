/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
package com.zimbra.cs.datasource.imap;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.cs.mailclient.imap.ImapConnection;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ImapConnection.class })
public class RemoteFolderTest {

    @Before
    public void setUp() throws Exception {
        PowerMock.mockStatic(ImapConnection.class);
    }

    @Test
    public void testRenameTo() throws Exception {
        final String originalPath = "Test";
        final String newPath = "Test_1";

        ImapConnection mockConnection = EasyMock.createMock(ImapConnection.class);
        RemoteFolder f = new RemoteFolder(mockConnection, originalPath);

        mockConnection.rename(originalPath, newPath);
        PowerMock.expectLastCall();

        PowerMock.replay(mockConnection);

        f.renameTo(newPath);

        PowerMock.verify(mockConnection);
    }

    // Since the folder name's are same hence rename method will not be called
    @Test
    public void testRenameToGrandChildSameName() throws Exception {
        final String originalPath = "Test/zimbra/ab";
        final String newPath = "Test/Zimbra/ab";

        ImapConnection mockConnection = EasyMock.createMock(ImapConnection.class);
        RemoteFolder f = new RemoteFolder(mockConnection, originalPath);

        PowerMock.replay(mockConnection);

        f.renameTo(newPath);

        PowerMock.verify(mockConnection);
    }

    // If we update the folder from Test/zimbra/pq to Test/zimbra_1/pq then the
    // rename of middle layer folder happens only when loop iterates to Test/zimbra
    // Below scenario run's in second iteration
    @Test
    public void testRenameToDifferentChildFolderName() throws Exception {
        final String originalPath = "Test/zimbra";
        final String newPath = "Test/Zimbra_1";

        ImapConnection mockConnection = EasyMock.createMock(ImapConnection.class);
        RemoteFolder f = new RemoteFolder(mockConnection, originalPath);

        mockConnection.rename(originalPath, newPath);
        PowerMock.expectLastCall();

        PowerMock.replay(mockConnection);

        f.renameTo(newPath);

        PowerMock.verify(mockConnection);
    }

    // In the below test case since grand child name's are same hence rename action
    // won't be called
    @Test
    public void testRenameToDifferentMiddleFolderNameandSameGrandChildName() throws Exception {
        final String originalPath = "Test/zimbra/pq";
        final String newPath = "Test/Zimbra_1/pq";

        ImapConnection mockConnection = EasyMock.createMock(ImapConnection.class);
        RemoteFolder f = new RemoteFolder(mockConnection, originalPath);

        PowerMock.replay(mockConnection);

        f.renameTo(newPath);

        PowerMock.verify(mockConnection);
    }

    @Test
    public void testRenameToDifferentGrandChildFolderName() throws Exception {
        final String originalPath = "Test/zimbra/pq";
        final String newPath = "Test/zimbra/pq_1";

        ImapConnection mockConnection = EasyMock.createMock(ImapConnection.class);
        RemoteFolder f = new RemoteFolder(mockConnection, originalPath);

        mockConnection.rename(originalPath, newPath);
        PowerMock.expectLastCall();

        PowerMock.replay(mockConnection);

        f.renameTo(newPath);

        PowerMock.verify(mockConnection);
    }

    // In this case when middle layer and grand child folder name's are renamed at a
    // time in this case in
    // rename method newer path is modified in such a way that only "pq" is renamed
    // in this loop
    // an in next loop Test/zimbra get's updated to Test/zimbra_1     
    // ref:testRenameToDifferentChildFolderName()
    @Test
    public void testRenameToDifferentMiddleChildandGrandChildFolderName() throws Exception {
        final String originalPath = "Test/zimbra/pq";
        final String newPath = "Test/zimbra_1/pq_1";

        ImapConnection mockConnection = EasyMock.createMock(ImapConnection.class);
        RemoteFolder f = new RemoteFolder(mockConnection, originalPath);

        mockConnection.rename(originalPath, "Test/zimbra/pq_1");
        PowerMock.expectLastCall();

        PowerMock.replay(mockConnection);

        f.renameTo(newPath);

        PowerMock.verify(mockConnection);
    }

}
