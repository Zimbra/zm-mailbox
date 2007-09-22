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

package com.zimbra.cs.security.kerberos;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to parse Kerberos 5 keytab file format.
 */
public class Krb5Keytab {
    private final File file;
    private final Map<KerberosPrincipal, List<KerberosKey>> keyMap;
    private long lastModified;
    private int version;

    private static final int VERSION_1 = 0x0501;    // DCE compatible
    private static final int VERSION_2 = 0x0502;    // Standard

    public Krb5Keytab(String name) throws IOException {
        file = new File(name);
        keyMap = new HashMap<KerberosPrincipal, List<KerberosKey>>();
        loadKeytab();
    }

    public List<KerberosKey> getKeys(KerberosPrincipal kp) throws IOException {
        checkReload();
        return Collections.unmodifiableList(keyMap.get(kp));
    }

    public List<KerberosKey> getKey(String name) throws IOException {
        return getKeys(new KerberosPrincipal(name));
    }

    // Reload keytab file is last modified time changed
    private void checkReload() throws IOException {
        if (file.lastModified() != lastModified) loadKeytab();
    }
    
    private void loadKeytab() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        lastModified = file.lastModified();
        version = readVersion(raf);
        if (version != VERSION_1 && version != VERSION_2) {
            throw formatError(
                "Unsupported format version 0x" + Integer.toHexString(version));
        }
        keyMap.clear();
        FileChannel fc = raf.getChannel();
        while (fc.position() < fc.size()) readEntry(fc);
    }

    private static int readVersion(RandomAccessFile raf) throws IOException {
        int hi = raf.readUnsignedByte();
        int lo = raf.readUnsignedByte();
        return (hi << 8) | lo;
    }

    private void readEntry(FileChannel fc) throws IOException {
        int size = readInt(fc);
        if (size < 0) {
            // Skip deleted entry
            long newPos = fc.position() + -size;
            if (newPos >= fc.size()) {
                throw new EOFException();
            }
            fc.position(newPos);
            return;
        }
        ByteBuffer bb = readBytes(fc, size);
        try {
            KerberosPrincipal kp = getPrincipal(bb);
            KerberosKey key = getKey(bb, kp);
            addKey(kp, key);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw formatError("Invalid entry size " + size);
        }
    }

    private KerberosPrincipal getPrincipal(ByteBuffer bb) throws IOException {
        int count = bb.getShort() & 0xffff;
        if (version == VERSION_1) --count;
        if (count < 1) {
            throw formatError("Invalid component count (" + count + ")");
        }
        String realm = getString(bb);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count - 1; i++) {
            sb.append(getString(bb)).append('/');
        }
        sb.append(getString(bb)).append('@').append(realm);
        String name = sb.toString();
        int type = version == VERSION_1
            ? KerberosPrincipal.KRB_NT_PRINCIPAL : bb.getInt();
        bb.getInt(); // Skip timestamp
        return new KerberosPrincipal(name, type);
    }

    private KerberosKey getKey(ByteBuffer bb, KerberosPrincipal kp) {
        int vno = bb.get() & 0xff;
        int type = bb.getShort() & 0xffff;
        byte[] b = getBytes(bb);
        if (bb.remaining() >= 4) {
            vno = bb.getInt(); // 32-bit vno present only if >= 4 bytes left
        }
        return new KerberosKey(kp, b, type, vno);
    }

    private void addKey(KerberosPrincipal kp, KerberosKey key) {
        List<KerberosKey> keys = keyMap.get(kp);
        if (keys == null) {
            keys = new ArrayList<KerberosKey>();
            keyMap.put(kp, keys);
        }
        keys.add(key);
    }

    private String getString(ByteBuffer bb) {
        try {
            return new String(getBytes(bb), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("US-ASCII encoding not supported");
        }
    }

    private byte[] getBytes(ByteBuffer bb) {
        int len = bb.getShort() & 0xffff;
        byte[] b = new byte[len];
        bb.get(b);
        return b;
    }
    
    private int readInt(FileChannel fc) throws IOException {
        return readBytes(fc, 4).getInt();
    }

    private ByteBuffer readBytes(FileChannel fc, int size) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(size);
        while (bb.hasRemaining()) {
            if (fc.read(bb) == -1) {
                throw new EOFException();
            }
        }
        bb.flip();
        if (version == VERSION_1) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        return bb;
    }

    private IOException formatError(String s) {
        return new IOException("Invalid keytab file '" + file + "': " + s);
    }
    
    public void dump(PrintStream ps) {
        ps.printf("Keytab name: %s\n", file);
        ps.printf("Keytab version: 0x%x\n", version);
        ps.printf("KVNO Principal\n");
        ps.print("---- ");
        for (int i = 0; i < 75; i++) ps.print('-');
        ps.println();
        for (KerberosPrincipal kp : keyMap.keySet()) {
            for (KerberosKey key : keyMap.get(kp)) {
                ps.printf("%4d %s (%s) (0x%x)\n",
                    key.getVersionNumber(), kp.getName(),
                    getKeyTypeName(key.getKeyType()),
                    new BigInteger(1, key.getEncoded()));
            }
        }
    }

    private static String getKeyTypeName(int keyType) {
        switch (keyType) {
            case 0x00: return "No encryption";
            case 0x01: return "DES cbc mode with CRC-32";
            case 0x02: return "DES cbc mode with RSA-MD4";
            case 0x03: return "DES cbc mode with RSA-MD5";
            case 0x04: return "DES cbc mode raw";
            case 0x06: return "Triple DES cbc mode raw";
            case 0x08: return "DES with HMAC/sha1";
            case 0x10: return "Triple DES cbc mode with HMAC/sha1";
            case 0x11: return "AES-128 CTS mode with 96-bit SHA-1 HMAC";
            case 0x12: return "AES-256 CTS mode with 96-bit SHA-1 HMAC";
            case 0x17: return "ArcFour with HMAC/md5";
            case 0x18: return "Exportable ArcFour with HMAC/md5";
            default:   return "Unknown type 0x" + Integer.toHexString(keyType);
        }
    }
    
    public static void main(String... args) throws Exception {
        new Krb5Keytab(args[0]).dump(System.out);
    }
}
