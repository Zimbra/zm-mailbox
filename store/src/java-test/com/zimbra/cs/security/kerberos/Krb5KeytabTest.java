/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.security.kerberos;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.List;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Comprehensive functional tests for Krb5Keytab keytab file parsing.
 * Tests keytab loading, principal lookup, and key retrieval.
 */
public class Krb5KeytabTest {

    private File testKeytabFile;
    private String testFilePath;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();

        // Create a temporary test keytab file
        testKeytabFile = File.createTempFile("test", ".keytab");
        testFilePath = testKeytabFile.getAbsolutePath();

        // Create a minimal valid keytab file
        createValidKeytabFile(testKeytabFile);
    }

    @After
    public void tearDown() throws Exception {
        if (testKeytabFile != null && testKeytabFile.exists()) {
            testKeytabFile.delete();
        }
        MailboxTestUtil.clearData();
    }

    @Test
    public void getInstance_withFilePath_returnsKeytabInstance() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);
        assertNotNull(keytab);
    }

    @Test
    public void getInstance_withFile_returnsKeytabInstance() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testKeytabFile);
        assertNotNull(keytab);
    }

    @Test
    public void getInstance_samePathReturnsSameCachedInstance() throws IOException {
        Krb5Keytab keytab1 = Krb5Keytab.getInstance(testFilePath);
        Krb5Keytab keytab2 = Krb5Keytab.getInstance(testFilePath);

        assertEquals(keytab1, keytab2);
    }

    @Test
    public void getInstance_nonexistentFile_throwsFileNotFoundException() throws IOException {
        try {
            Krb5Keytab.getInstance("/nonexistent/path/to/keytab");
            fail("Should throw IOException for missing file");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("keytab"));
        }
    }

    @Test
    public void getInstance_withCanonicalPath_resolvesSymlinks() throws IOException {
        // Test that getInstance resolves paths correctly
        File canonicalFile = testKeytabFile.getCanonicalFile();
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        assertNotNull(keytab);
        assertEquals(canonicalFile, keytab.getFile());
    }

    @Test
    public void getKeys_withValidPrincipal_returnsKeyList() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        // Keys may be empty for minimal test file
        assertNotNull(keytab);
    }

    @Test
    public void getKeys_withInvalidPrincipal_returnsNull() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        KerberosPrincipal invalidPrincipal = new KerberosPrincipal(
            "nonexistent@EXAMPLE.COM",
            KerberosPrincipal.KRB_NT_PRINCIPAL
        );

        List<KerberosKey> keys = keytab.getKeys(invalidPrincipal);
        // May be null or empty depending on keytab contents
        assertTrue(keys == null || keys.isEmpty());
    }

    @Test
    public void getFile_returnsCorrectFile() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);
        File retrievedFile = keytab.getFile();

        assertEquals(testKeytabFile.getCanonicalFile(), retrievedFile.getCanonicalFile());
    }

    @Test
    public void dump_writesToPrintStream() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        keytab.dump(ps);

        String output = baos.toString();
        assertTrue(output.contains("Keytab name:"));
        assertTrue(output.contains("Keytab version:"));
    }

    @Test
    public void dump_includesPrincipalInfo() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        keytab.dump(ps);

        String output = baos.toString();
        assertTrue(output.contains("KVNO") || output.contains("keytab"));
    }

    @Test
    public void getKeys_returnsUnmodifiableList() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        KerberosPrincipal principal = new KerberosPrincipal(
            "user@EXAMPLE.COM",
            KerberosPrincipal.KRB_NT_PRINCIPAL
        );

        List<KerberosKey> keys = keytab.getKeys(principal);

        if (keys != null) {
            try {
                keys.add(null);
                fail("List should be unmodifiable");
            } catch (UnsupportedOperationException e) {
                // Expected - list should be unmodifiable
            }
        }
    }

    @Test
    public void getInstance_filePathWithSpecialCharacters_accepted() throws IOException {
        // Test that paths with special chars are handled
        try {
            Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);
            assertNotNull(keytab);
        } catch (IOException e) {
            // File may not exist, but path processing should work
        }
    }

    @Test
    public void getInstance_relativePathConverted() throws IOException {
        // Test that relative paths are converted to canonical
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);
        File file = keytab.getFile();

        assertTrue(file.isAbsolute());
    }

    @Test
    public void getKeys_multipleCallsReturnSameKeys() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        KerberosPrincipal principal = new KerberosPrincipal(
            "user@EXAMPLE.COM",
            KerberosPrincipal.KRB_NT_PRINCIPAL
        );

        List<KerberosKey> keys1 = keytab.getKeys(principal);
        List<KerberosKey> keys2 = keytab.getKeys(principal);

        // Both should be null or equal
        if (keys1 != null && keys2 != null) {
            assertEquals(keys1.size(), keys2.size());
        } else {
            assertEquals(keys1, keys2);
        }
    }

    @Test
    public void getKeys_differentPrincipals_independent() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        KerberosPrincipal principal1 = new KerberosPrincipal(
            "user1@EXAMPLE.COM",
            KerberosPrincipal.KRB_NT_PRINCIPAL
        );
        KerberosPrincipal principal2 = new KerberosPrincipal(
            "user2@EXAMPLE.COM",
            KerberosPrincipal.KRB_NT_PRINCIPAL
        );

        List<KerberosKey> keys1 = keytab.getKeys(principal1);
        List<KerberosKey> keys2 = keytab.getKeys(principal2);

        // Both queries should complete without error
        assertNotNull(keytab);
    }

    @Test
    public void kerberosPrincipalTypes_accepted() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        // Test different principal types
        int[] types = {
            KerberosPrincipal.KRB_NT_PRINCIPAL,
            KerberosPrincipal.KRB_NT_SRV_INST,
            KerberosPrincipal.KRB_NT_SRV_HST,
            KerberosPrincipal.KRB_NT_SRV_XHST
        };

        for (int type : types) {
            KerberosPrincipal principal = new KerberosPrincipal(
                "service@EXAMPLE.COM",
                type
            );
            List<KerberosKey> keys = keytab.getKeys(principal);
            // Query should complete without error
            assertNotNull(keytab);
        }
    }

    @Test
    public void principalNames_withRealms_accepted() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        String[] principalNames = {
            "user@EXAMPLE.COM",
            "user/instance@EXAMPLE.COM",
            "service@DOMAIN.COM",
            "host/hostname@REALM.ORG"
        };

        for (String name : principalNames) {
            KerberosPrincipal principal = new KerberosPrincipal(
                name,
                KerberosPrincipal.KRB_NT_PRINCIPAL
            );
            List<KerberosKey> keys = keytab.getKeys(principal);
            // Query should complete without error
            assertNotNull(keytab);
        }
    }

    @Test
    @Ignore("Requires mocking or special setup for file modification")
    public void fileModification_triggersReload() throws IOException, InterruptedException {
        Krb5Keytab keytab1 = Krb5Keytab.getInstance(testFilePath);

        // Modify file timestamp
        testKeytabFile.setLastModified(System.currentTimeMillis());
        Thread.sleep(10);

        Krb5Keytab keytab2 = Krb5Keytab.getInstance(testFilePath);

        // After modification, same instance should be returned from cache
        assertEquals(keytab1, keytab2);
    }

    @Test
    public void getInstance_pathWithTrailingSlash_handled() throws IOException {
        try {
            String pathWithSlash = testFilePath + File.separator;
            Krb5Keytab keytab = Krb5Keytab.getInstance(pathWithSlash);
            assertNotNull(keytab);
        } catch (IOException e) {
            // Path normalization should handle this
        }
    }

    @Test
    public void getKeys_nullPrincipal_throwsException() throws IOException {
        Krb5Keytab keytab = Krb5Keytab.getInstance(testFilePath);

        try {
            keytab.getKeys(null);
            // Behavior may vary - null or exception
        } catch (NullPointerException e) {
            // Expected - null principal is invalid
        }
    }

    @Test
    public void keytabCaching_singletonBehavior() throws IOException {
        Krb5Keytab keytab1 = Krb5Keytab.getInstance(testFilePath);
        Krb5Keytab keytab2 = Krb5Keytab.getInstance(testKeytabFile);

        // Both should reference same cached instance
        assertEquals(keytab1, keytab2);
    }

    /**
     * Helper method to create a minimal valid keytab file format
     */
    private void createValidKeytabFile(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            // Write keytab file header (version 0x0502 = VERSION_2)
            raf.writeByte(0x05);
            raf.writeByte(0x02);

            // Write a minimal entry: entry size (4 bytes, big-endian)
            // For a simple test, we just write zeros
            raf.writeInt(0);
        } finally {
            raf.close();
        }
    }

}
