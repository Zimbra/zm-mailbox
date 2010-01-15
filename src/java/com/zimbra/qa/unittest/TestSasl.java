/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import com.zimbra.cs.security.sasl.SaslInputBuffer;
import com.zimbra.cs.security.sasl.SaslOutputBuffer;
import com.zimbra.cs.security.sasl.SaslSecurityLayer;
import junit.framework.TestCase;

import javax.security.sasl.SaslException;
import java.nio.ByteBuffer;

public class TestSasl extends TestCase {
    private static final int SIZE = 23456;
    
    public void testSaslInputBuffer() throws SaslException {
        SaslInputBuffer buffer = new SaslInputBuffer(SIZE);
        ByteBuffer data = fill(ByteBuffer.allocate(SIZE + 4).putInt(SIZE));
        for (int n = 2; data.hasRemaining(); n *= 2) {
            ByteBuffer bb = data.slice();
            if (bb.remaining() > n) bb.limit(n);
            buffer.put(bb);
            assertFalse(bb.hasRemaining());
            data.position(data.position() + bb.position());
            if (data.hasRemaining()) assertFalse(buffer.isComplete());
        }
        assertTrue(buffer.isComplete());
        byte[] unwrapped = buffer.unwrap(new SecurityLayer());
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
            buffer.put(bb);
            assertFalse(bb.hasRemaining());
            data.position(data.position() + bb.position());
            if (data.hasRemaining()) assertFalse(buffer.isFull());
        }
        assertTrue(buffer.isFull());
        ByteBuffer bb = ByteBuffer.allocate(100);
        buffer.put(bb);
        assertEquals(100, bb.remaining());
        byte[] wrapped = buffer.wrap(new SecurityLayer());
        checkData(wrapped, SIZE);
        buffer.clear();
        assertFalse(buffer.isFull());
    }

    public void testKrb5Auth() throws Exception {
        
    }
    /*
    public void testGssCredentials() throws Exception {
        String host = "localhost";
        String principle = "imap@" + host;
        LoginContext lc = Krb5Login.withKeyTab(
            "imap/localhost", "/opt/zimbra/conf/krb5.keytab");
        lc.login();
        GSSManager mgr = GSSManager.getInstance();
        Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");
	GSSName serviceName = mgr.createName(principle,
		GSSName.NT_HOSTBASED_SERVICE, krb5Oid);
        GSSCredential cred = mgr.createCredential(serviceName,
		GSSCredential.INDEFINITE_LIFETIME,
		krb5Oid, GSSCredential.ACCEPT_ONLY);
        System.out.println("Cred = " + cred);
    }

    public void testKeytab() throws Exception {
        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("sun.security.jgss.debug", "true");
        Krb5Keytab keytab = new Krb5Keytab("/opt/zimbra/conf/krb5.keytab");
        KerberosPrincipal kp = new KerberosPrincipal("imap/localhost");
        List<KerberosKey> keys = keytab.getKeys(kp);
        System.out.println("len = " + keys.size());
        Subject subject = new Subject();
        subject.getPrincipals().add(kp);
        subject.getPrivateCredentials().addAll(keys);
        subject.setReadOnly();
        System.out.println("subject = " + subject);
        final CallbackHandler cbh = new CallbackHandler() {
            public void handle(Callback[] cbs) {
                ((AuthorizeCallback) cbs[0]).setAuthorized(true);
            }
        };
        SaslServer server = (SaslServer) Subject.doAs(subject,
            new PrivilegedExceptionAction() {
                public Object run() throws SaslException {
                    return Sasl.createSaslServer("GSSAPI", "imap", "localhost", null, cbh);
                }
            });
        System.out.println("server = " + server);
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
    
    private static class SecurityLayer extends SaslSecurityLayer {
        public String getMechanismName() { return "TEST"; }
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
