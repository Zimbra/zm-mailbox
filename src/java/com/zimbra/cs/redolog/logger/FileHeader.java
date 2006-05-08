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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 4. 7.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.zimbra.cs.redolog.Version;

/**
 * @author jhahm
 *
 * Header for a redolog file.  Redolog header is exactly 512 bytes long.
 * The fields are:
 * 
 *   MAGIC          7 bytes containing "ZM_REDO"
 *   open           1 byte (1 or 0)
 *                  0 means file was closed normally
 *                  1 means either file is currently open, or process died
 *                  without closing file properly; there may be partially
 *                  written log entries at the end of file
 *   filesize       8 bytes; for self-integrity check
 *   sequence       8 bytes; log file sequence number; 0 to Long.MAX_VALUE
 *                  wraps around after Long.MAX_VALUE
 *   serverId       128 bytes; consists of the following subfields:
 *                    length  - 1 byte (0 to 127)
 *                    data    - up to 127 bytes of serverId in UTF-8
 *                    padding - 0-value bytes of length = 127 - length(data)
 *                  serverId is the zimbraId LDAP attribute of the server entry
 *   firstOpTstamp  4 bytes; time of first op in file
 *   lastOpTstamp   4 bytes; time of last op in file
 *   version        4 bytes; serialization version number
 *                  (2-byte major, 2-byte minor)
 *   padding        0-value bytes to bring total header size to 512
 */
public class FileHeader {

    public static final int HEADER_LEN = 512;
    private static final int SERVER_ID_FIELD_LEN = 127;
    private static final byte[] MAGIC = "ZM_REDO".getBytes();

    private byte mOpen;                 // logfile is open or closed
    private long mFileSize;             // filesize
    private long mSeq;                  // log file sequence number
    private String mServerId;           // host on which the file was created
                                        // zimbraId attribute from LDAP
    private long mFirstOpTstamp;        // time of first op in log file
    private long mLastOpTstamp;         // time of last op in log file

    private Version mVersion;			// redo log version

    FileHeader() {
    	this("unknown");
    }

    FileHeader(String serverId) {
        mOpen = 0;
        mFileSize = 0;
    	mSeq = 0;
        mServerId = serverId;
        mFirstOpTstamp = 0;
        mLastOpTstamp = 0;
        mVersion = Version.latest();
    }

    void write(RandomAccessFile raf) throws IOException {
    	// Update header redolog version to latest code version.
    	if (!mVersion.isLatest())
    		mVersion = Version.latest();
        byte[] buf = serialize();
        raf.seek(0);
        raf.write(buf);
        raf.getFD().sync();
    }

    void read(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        byte[] header = new byte[HEADER_LEN];
        int bytesRead = raf.read(header, 0, HEADER_LEN);
        if (bytesRead < HEADER_LEN)
            throw new IOException("Redolog is smaller than header length of " +
                                  HEADER_LEN + " bytes");
        deserialize(header);
    }

    void setOpen(boolean b) {
        if (b)
            mOpen = (byte) 1;
        else
            mOpen = (byte) 0;
    }

    void setFileSize(long s) {
    	mFileSize = s;
    }

    void setSequence(long seq) {
    	mSeq = seq;
    }

    void setFirstOpTstamp(long t) {
    	mFirstOpTstamp = t;
    }

    void setLastOpTstamp(long t) {
    	mLastOpTstamp = t;
    }

    public boolean getOpen() {
    	return mOpen != 0;
    }

    public long getFileSize() {
    	return mFileSize;
    }

    public long getSequence() {
        return mSeq;
    }

    public String getServerId() {
    	return mServerId;
    }

    public long getFirstOpTstamp() {
    	return mFirstOpTstamp;
    }

    public long getLastOpTstamp() {
    	return mLastOpTstamp;
    }

    /**
     * Get byte buffer of a String that fits within given maximum length.
     * String is trimmed at the end one character at a time until the
     * byte representation in given charset fits maxlen.
     * @param str
     * @param charset
     * @param maxlen
     * @return byte array of str in charset encoding;
     *              zero-length array if any trouble
     */
    private byte[] getStringBytes(String str, String charset, int maxlen) {
        String substr = str;
        int len = substr.length();
        while (len > 0) {
            byte[] buf = null;
            try {
                buf = substr.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                // Treat as if we had 0-length string.
                break;
            }
            if (buf.length <= maxlen)
                return buf;
            substr = substr.substring(0, --len);
        }
        byte[] buf = new byte[0];
        return buf;
    }


    private byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(HEADER_LEN);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write(MAGIC);
        dos.writeByte(mOpen);
        dos.writeLong(mFileSize);
        dos.writeLong(mSeq);

        // ServerId field:
        //
        //   length   (byte)   length of serverId in bytes
        //   serverId (byte[]) bytes in UTF-8;
        //                     up to SERVER_ID_FIELD_LEN bytes
        //   padding  (byte[]) optional; 0 bytes to make
        //                     length(serverId + padding) = SERVER_ID_FIELD_LEN
        //
        // Although unlikely, extremely long serverId might exceed
        // SERVER_ID_FIELD_LEN and only partial data may be written.  Don't
        // count on serverId being complete when reading the header.
        byte[] serverIdBuf =
            getStringBytes(mServerId, "UTF-8", SERVER_ID_FIELD_LEN);
        dos.writeByte((byte) serverIdBuf.length);
        dos.write(serverIdBuf);
        if (serverIdBuf.length < SERVER_ID_FIELD_LEN) {
            byte[] padding = new byte[SERVER_ID_FIELD_LEN - serverIdBuf.length];
            Arrays.fill(padding, (byte) 0); // might not be necessary
            dos.write(padding);
        }

        dos.writeLong(mFirstOpTstamp);
        dos.writeLong(mLastOpTstamp);
        mVersion.serialize(dos);

        int currentLen = baos.size();
        if (currentLen < HEADER_LEN) {
            int paddingLen = HEADER_LEN - currentLen;
            byte[] b = new byte[paddingLen];
            Arrays.fill(b, (byte) 0);
            dos.write(b);
        }

        byte[] headerBuf = baos.toByteArray();
        dos.close();
        if (headerBuf.length != HEADER_LEN)
            throw new IOException("Wrong redolog header length of " +
                                  headerBuf.length + "; should be " +
                                  HEADER_LEN);
        return headerBuf;
    }

    private void deserialize(byte[] headerBuf) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(headerBuf);
        DataInputStream dis = new DataInputStream(bais);

        try {
            byte[] magic = new byte[MAGIC.length];
            dis.read(magic, 0, MAGIC.length);
            if (!Arrays.equals(magic, MAGIC))
                throw new IOException("Missing magic bytes in redolog header");

            mOpen = dis.readByte();
            mFileSize = dis.readLong();
            mSeq = dis.readLong();

            int serverIdLen = (int) dis.readByte();
            if (serverIdLen > SERVER_ID_FIELD_LEN)
                throw new IOException("ServerId too long (" + serverIdLen +
                                      " bytes) in redolog header");
            byte[] serverIdBuf = new byte[SERVER_ID_FIELD_LEN];
            dis.read(serverIdBuf, 0, SERVER_ID_FIELD_LEN);
            mServerId = new String(serverIdBuf, 0, serverIdLen, "UTF-8");

            mFirstOpTstamp = dis.readLong();
            mLastOpTstamp = dis.readLong();
            mVersion.deserialize(dis);
            if (mVersion.tooHigh())
    			throw new IOException("Redo log version " + mVersion +
    								  " is higher than the highest known version " +
    								  Version.latest());
            // Versioning of file header was added late in the game.
            // Any redolog files created previously will have version 0.0.
            // Assume version 1.0 for those files.
            if (!mVersion.atLeast(1, 0))
            	mVersion = new Version(1, 0);
        } finally {
            dis.close();
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("sequence: ").append(mSeq).append("\n");
        sb.append("open: ").append(mOpen).append("\n");
        sb.append("filesize: ").append(mFileSize).append("\n");
        sb.append("serverId: ").append(mServerId).append("\n");
        sb.append("firstOpTstamp: ").append(mFirstOpTstamp).append("\n");
        sb.append("lastOpTstamp:  ").append(mLastOpTstamp).append("\n");
        sb.append("version: ").append(mVersion).append("\n");
    	return sb.toString();
    }
}
