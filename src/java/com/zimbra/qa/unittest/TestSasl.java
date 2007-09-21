/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import com.zimbra.cs.account.krb5.Krb5Login;
import com.zimbra.cs.mina.SaslInputBuffer;
import com.zimbra.cs.mina.SaslOutputBuffer;
import junit.framework.TestCase;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import javax.security.auth.login.LoginContext;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.File;
import java.nio.ByteBuffer;

public class TestSasl extends TestCase {
    private static final int SIZE = 23456;
    
    public void testSaslInputBuffer() throws SaslException {
        SaslInputBuffer buffer = new SaslInputBuffer();
        ByteBuffer data = fill(ByteBuffer.allocate(SIZE + 4).putInt(SIZE));
        for (int n = 2; data.hasRemaining(); n *= 2) {
            ByteBuffer bb = data.slice();
            if (bb.remaining() > n) bb.limit(n);
            buffer.read(bb);
            assertFalse(bb.hasRemaining());
            data.position(data.position() + bb.position());
            if (data.hasRemaining()) assertFalse(buffer.isComplete());
        }
        assertTrue(buffer.isComplete());
        byte[] unwrapped = buffer.unwrap(new TestSaslServer());
        checkData(unwrapped, SIZE);
        buffer.clear();
        assertFalse(buffer.isComplete());
    }

    public void testSaslOutputBuffer() throws SaslException {
        SaslOutputBuffer buffer = new SaslOutputBuffer(1, SIZE);
        ByteBuffer data = fill(ByteBuffer.allocate(SIZE));
        for (int n = 2; data.hasRemaining(); n *= 2) {
            ByteBuffer bb = data.slice();
            if (bb.remaining() > n) bb.limit(n);
            buffer.write(bb);
            assertFalse(bb.hasRemaining());
            data.position(data.position() + bb.position());
            if (data.hasRemaining()) assertFalse(buffer.isFull());
        }
        assertTrue(buffer.isFull());
        ByteBuffer bb = ByteBuffer.allocate(100);
        buffer.write(bb);
        assertEquals(100, bb.remaining());
        byte[] wrapped = buffer.wrap(new TestSaslServer());
        checkData(wrapped, SIZE);
        buffer.clear();
        assertFalse(buffer.isFull());
    }

    /*
    public void testGssCredentials() throws Exception {
        String host = "localhost";
        String principle = "imap@" + host;
//        LoginContext lc = Krb5Login.withKeyTab(principle, "/opt/zimbra/conf/krb5.keytab");
//        lc.login();
        GSSManager mgr = GSSManager.getInstance();
        Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");
	GSSName serviceName = mgr.createName("imap@nomad.local",
		GSSName.NT_HOSTBASED_SERVICE, krb5Oid);
        GSSCredential cred = mgr.createCredential(serviceName,
		GSSCredential.INDEFINITE_LIFETIME,
		krb5Oid, GSSCredential.ACCEPT_ONLY);
        System.out.println("Cred = " + cred);
    }
    */
    
    private static ByteBuffer fill(ByteBuffer bb) {
        for (int i = 0; bb.hasRemaining(); i++) {
            bb.put((byte) i);
        }
        bb.flip();
        return bb;
    }

    private static void checkData(byte[] b, int size) {
        assertNotNull(b);
        assertEquals(size, b.length);
        for (int i = 0; i < size; i++) {
            assertEquals((byte) i, b[i]);
        }
    }
    
    private static class TestSaslServer implements SaslServer {
        public String getMechanismName() { return "TEST"; }
        public byte[] evaluateResponse(byte[] b) { return b; }
        public boolean isComplete() { return true; }
        public String getAuthorizationID() { return "TEST"; }
        public byte[] wrap(byte[] b, int off, int len) {
            byte[] r = new byte[len];
            System.arraycopy(b, off, r, 0, len);
            return r;
        }
        public byte[] unwrap(byte[] b, int off, int len) {
            return wrap(b, off, len);
        }
        public Object getNegotiatedProperty(String name) { return null; }
        public void dispose() {}
    }
}
