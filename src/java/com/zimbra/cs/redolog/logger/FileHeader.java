/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
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
 *   createTime     4 bytes; time this log file was created
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
    private long mCreateTime;           // create time of log file

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
        mCreateTime = 0;
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

    void setCreateTime(long t) {
        mCreateTime = t;
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

    public long getCreateTime() {
        return mCreateTime;
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
        RedoLogOutput out = new RedoLogOutput(baos);
        out.write(MAGIC);
        out.writeByte(mOpen);
        out.writeLong(mFileSize);
        out.writeLong(mSeq);

        // ServerId field:
        //
        //   length   (byte)   length of serverId in bytes
        //   serverId (byte[]) bytes in UTF-8;
        //                     up to SERVER_ID_FIELD_LEN bytes
        //   padding  (byte[]) optional; 0 bytes to make
        //                     length(serverId + padding) = SERVER_ID_FIELD_LEN
        byte[] serverIdBuf =
            getStringBytes(mServerId, "UTF-8", SERVER_ID_FIELD_LEN);
        out.writeByte((byte) serverIdBuf.length);
        out.write(serverIdBuf);
        if (serverIdBuf.length < SERVER_ID_FIELD_LEN) {
            byte[] padding = new byte[SERVER_ID_FIELD_LEN - serverIdBuf.length];
            Arrays.fill(padding, (byte) 0); // might not be necessary
            out.write(padding);
        }

        out.writeLong(mFirstOpTstamp);
        out.writeLong(mLastOpTstamp);
        mVersion.serialize(out);
        out.writeLong(mCreateTime);

        int currentLen = baos.size();
        if (currentLen < HEADER_LEN) {
            int paddingLen = HEADER_LEN - currentLen;
            byte[] b = new byte[paddingLen];
            Arrays.fill(b, (byte) 0);
            out.write(b);
        }

        byte[] headerBuf = baos.toByteArray();
        baos.close();
        if (headerBuf.length != HEADER_LEN)
            throw new IOException("Wrong redolog header length of " +
                                  headerBuf.length + "; should be " +
                                  HEADER_LEN);
        return headerBuf;
    }

    private void deserialize(byte[] headerBuf) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(headerBuf);
        RedoLogInput in = new RedoLogInput(bais);

        try {
            byte[] magic = new byte[MAGIC.length];
            in.readFully(magic, 0, MAGIC.length);
            if (!Arrays.equals(magic, MAGIC))
                throw new IOException("Missing magic bytes in redolog header");

            mOpen = in.readByte();
            mFileSize = in.readLong();
            mSeq = in.readLong();

            int serverIdLen = (int) in.readByte();
            if (serverIdLen > SERVER_ID_FIELD_LEN)
                throw new IOException("ServerId too long (" + serverIdLen +
                                      " bytes) in redolog header");
            byte[] serverIdBuf = new byte[SERVER_ID_FIELD_LEN];
            in.readFully(serverIdBuf, 0, SERVER_ID_FIELD_LEN);
            mServerId = new String(serverIdBuf, 0, serverIdLen, "UTF-8");

            mFirstOpTstamp = in.readLong();
            mLastOpTstamp = in.readLong();
            mVersion.deserialize(in);
            if (mVersion.tooHigh())
    			throw new IOException("Redo log version " + mVersion +
    								  " is higher than the highest known version " +
    								  Version.latest());
            // Versioning of file header was added late in the game.
            // Any redolog files created previously will have version 0.0.
            // Assume version 1.0 for those files.
            if (!mVersion.atLeast(1, 0))
            	mVersion = new Version(1, 0);

            mCreateTime = in.readLong();
        } finally {
            bais.close();
        }
    }

    private static String DATE_FORMAT = "EEE, yyyy/MM/dd HH:mm:ss.SSS z";

    public String toString() {
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
        StringBuilder sb = new StringBuilder(100);
        sb.append("sequence: ").append(mSeq).append("\n");
        sb.append("open:     ").append(mOpen).append("\n");
        sb.append("filesize: ").append(mFileSize).append("\n");
        sb.append("serverId: ").append(mServerId).append("\n");
        sb.append("created:  ");
        if (mCreateTime != 0)
            sb.append(fmt.format(new Date(mCreateTime))).append(" (").append(mCreateTime).append(")");
        sb.append("\n");
        sb.append("first op: ");
        if (mFirstOpTstamp != 0)
            sb.append(fmt.format(new Date(mFirstOpTstamp))).append(" (").append(mFirstOpTstamp).append(")");
        sb.append("\n");
        sb.append("last op:  ");
        if (mLastOpTstamp != 0) {
            sb.append(fmt.format(new Date(mLastOpTstamp))).append(" (").append(mLastOpTstamp).append(")");
            if (mOpen != 0)
                sb.append(" (not up to date)");
        }
        sb.append("\n");
        sb.append("version:  ").append(mVersion).append("\n");
    	return sb.toString();
    }
}
