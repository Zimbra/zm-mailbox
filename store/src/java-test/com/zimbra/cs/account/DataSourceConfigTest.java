/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.cs.account.DataSourceConfig.Folder;
import com.zimbra.cs.account.DataSourceConfig.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DataSourceConfig} — parses real XML config files off disk through
 * {@code DataSourceConfig.read(File)}, then asserts the full parsed object graph. Exercises
 * happy paths and each "invalid configuration" failure branch.
 */
public class DataSourceConfigTest {

    private File writeTempConfig(String xml) throws IOException {
        File f = File.createTempFile("dsconfig", ".xml");
        f.deleteOnExit();
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(xml.getBytes(Charset.forName("UTF-8")));
        } finally {
            out.close();
        }
        return f;
    }

    @Test
    public void readFullConfigParsesServicesFoldersAndAttributes() throws Exception {
        String xml = "<datasource syncAllFolders='false'>"
                + "  <service name='gmail' saveToSent='false'"
                + "           calDavTargetUrl='https://cal' calDavPrincipalPath='/p'>"
                + "    <folder remotePath='Inbox' localPath='in' ignore='false' sync='true' folderName='\\Inbox'/>"
                + "  </service>"
                + "</datasource>";
        File f = writeTempConfig(xml);

        DataSourceConfig config = DataSourceConfig.read(f);

        assertFalse("syncAllFolders attr should be parsed as false", config.isSyncAllFolders());
        assertEquals(1, config.getServices().size());
        Service svc = config.getService("GMAIL");   // lookup is case-insensitive
        assertNotNull("service lookup should be case-insensitive", svc);
        assertEquals("gmail", svc.getName());
        assertFalse(svc.isSaveToSent());
        assertEquals("https://cal", svc.getCalDavTargetUrl());
        assertEquals("/p", svc.getCalDavPrincipalPath());
        assertEquals(1, svc.getFolders().size());
        Folder folder = svc.getFolderByRemotePath("inbox", null);   // case-insensitive
        assertNotNull(folder);
        assertEquals("Inbox", folder.getRemotePath());
        assertTrue(folder.isSync());
        assertFalse(folder.isIgnore());
    }

    @Test
    public void readDefaultsSyncAllFoldersTrueAndSaveToSentTrue() throws Exception {
        String xml = "<datasource>"
                + "  <service name='svc1'>"
                + "    <folder remotePath='Sent'/>"
                + "  </service>"
                + "</datasource>";
        File f = writeTempConfig(xml);

        DataSourceConfig config = DataSourceConfig.read(f);

        assertTrue("syncAllFolders defaults to true when absent", config.isSyncAllFolders());
        Service svc = config.getService("svc1");
        assertNotNull(svc);
        assertTrue("saveToSent defaults to true", svc.isSaveToSent());
    }

    @Test
    public void readFolderWithoutLocalPathDefaultsToSlashRemotePath() throws Exception {
        String xml = "<datasource>"
                + "  <service name='svc1'>"
                + "    <folder remotePath='Sent'/>"
                + "  </service>"
                + "</datasource>";
        File f = writeTempConfig(xml);

        DataSourceConfig config = DataSourceConfig.read(f);

        Folder folder = config.getService("svc1").getFolderByRemotePath("Sent", null);
        assertNotNull(folder);
        assertEquals("localPath defaults from remotePath with leading slash", "/Sent",
                folder.getLocalPath());
    }

    @Test
    public void getServiceUnknownNameReturnsNull() throws Exception {
        String xml = "<datasource><service name='svc1'><folder remotePath='x'/></service></datasource>";
        File f = writeTempConfig(xml);

        DataSourceConfig config = DataSourceConfig.read(f);

        assertNull("unknown service name should return null", config.getService("nope"));
    }

    @Test
    public void getFolderByLocalPathMatchReturnsFolderAndMissReturnsNull() throws Exception {
        String xml = "<datasource>"
                + "  <service name='svc1'>"
                + "    <folder remotePath='Sent' localPath='/sent'/>"
                + "  </service>"
                + "</datasource>";
        File f = writeTempConfig(xml);

        DataSourceConfig config = DataSourceConfig.read(f);
        Service svc = config.getService("svc1");

        assertNotNull("localPath lookup is case-insensitive", svc.getFolderByLocalPath("/SENT"));
        assertNull("unknown local path returns null", svc.getFolderByLocalPath("/nope"));
    }

    @Test
    public void readUnrecognizedRootAttributeThrowsInvalidConfig() throws Exception {
        String xml = "<datasource bogus='1'></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for unrecognized root attribute");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unrecognized attribute name"));
        }
    }

    @Test
    public void readUnrecognizedRootElementThrowsInvalidConfig() throws Exception {
        String xml = "<datasource><bogus/></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for unrecognized root element");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unrecognized element name"));
        }
    }

    @Test
    public void readServiceMissingNameThrowsInvalidConfig() throws Exception {
        String xml = "<datasource><service saveToSent='true'><folder remotePath='x'/></service></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for missing service name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Missing service name"));
        }
    }

    @Test
    public void readUnrecognizedServiceAttributeThrowsInvalidConfig() throws Exception {
        String xml = "<datasource><service name='s' bogus='1'><folder remotePath='x'/></service></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for unrecognized service attribute");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unrecognized service attribute name"));
        }
    }

    @Test
    public void readFolderMissingRemotePathThrowsInvalidConfig() throws Exception {
        String xml = "<datasource><service name='s'><folder localPath='/x'/></service></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for missing folder remotePath");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Missing folder remotePath"));
        }
    }

    @Test
    public void readUnrecognizedFolderAttributeThrowsInvalidConfig() throws Exception {
        String xml = "<datasource><service name='s'><folder remotePath='x' bogus='1'/></service></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for unrecognized folder attribute");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unrecognized folder attribute name"));
        }
    }

    @Test
    public void readFolderWithSubElementThrowsInvalidConfig() throws Exception {
        String xml = "<datasource><service name='s'><folder remotePath='x'><child/></folder></service></datasource>";
        File f = writeTempConfig(xml);

        try {
            DataSourceConfig.read(f);
            fail("expected IllegalArgumentException for folder with sub-elements");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Folder should not contain any sub-elements"));
        }
    }

    @Test
    public void readFolderNameFlagParsedAndMatchableByGetFolderByRemotePath() throws Exception {
        String xml = "<datasource>"
                + "  <service name='svc1'>"
                + "    <folder remotePath='Inbox' folderName='\\Inbox'/>"
                + "  </service>"
                + "</datasource>";
        File f = writeTempConfig(xml);

        DataSourceConfig config = DataSourceConfig.read(f);
        Folder folder = config.getService("svc1").getFolderByRemotePath("Inbox", null);

        assertNotNull("folderNameFlag should be parsed into an Atom", folder.getFolderNameFlag());
    }
}
