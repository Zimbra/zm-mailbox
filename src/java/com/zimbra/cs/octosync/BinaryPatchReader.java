/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.octosync;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * Patch reader implementation for octopus patch format (version 0).
 *
 * Not thread safe.
 *
 * @author grzes
 */
public class BinaryPatchReader implements PatchReader
{
    private static final Log log = LogFactory.getLog(BinaryPatchReader.class);

    /** Header id */
    private static final String HEADER_ID = "OPATCH";

    /** Octopus patch version supported by the implementation */
    private static final short OPATCH_VERSION = 0;

    /** Data record id */
    private static final byte DATA_ID = 'D';

    /** Reference record id */
    private static final byte REF_ID = 'R';

    /** Number of bytes used for hash in patch reference records */
    private static final int HASH_SIZE = 32;

    /** Underlying input stream wrapped into DataInputStream for the luxury of readFully()
     */
    private DataInputStream dis;
    /** Final size of the file after the patch is applied */
    private long fileSize;

    /** Holds next record info to be returned by getNextRecordInfo() */
    private RecordInfo nextRecordInfo;

    /** No. of bytes left to read in  popData's InputStream */
    private long numBytesLeftInCurrentDataRecord;

    /** Position in the stream relative to the original position of the stream
     * as passed to the constructor. For detailed error reporting only.
     */
    private long pos;


    /**
     * For interim testing only. Validates patch format, checking the syntax,
     * but without verifying if e.g. patch references are valid.
     *
     * @param is
     *            Input stream with patch

     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws BadPatchFormatException
     */
    public static void validatePatchFormat(InputStream is) throws IOException, BadPatchFormatException
    {
        PatchReader patchReader = new BinaryPatchReader(is);

        while (patchReader.hasMoreRecordInfos()) {

            RecordInfo ri = patchReader.getNextRecordInfo();

            if (ri.type == RecordType.REF) {
                patchReader.popRef();
            } else if (ri.type == RecordType.DATA) {
                patchReader.popData().skip(ri.length);
            } else {
                assert false;
            }
        }
    }

    /**
     * Instantiates a new binary patch reader.
     *
     * @param is
     *            The input stream to read patch contents from. The stream is closed
     *            once fully read from or upon encountering patch format related error.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws BadPatchFormatException
     *             Signals malformed patch format
     */
    public BinaryPatchReader(InputStream is) throws IOException,
        BadPatchFormatException
    {
        dis = new DataInputStream(is);

        byte idBuf[] = new byte[HEADER_ID.length()];
        dis.readFully(idBuf);
        String id = new String(idBuf);

        if (!id.equals(HEADER_ID)) {
            throw new BadPatchFormatException("invalid patch header id: " + id);
        }

        pos += idBuf.length;

        short version = readShort();

        if (version != OPATCH_VERSION) {
            throw new BadPatchFormatException("unsupported version: " + version);
        }

        fileSize = readLong();
    }

    // PatchReader API
    @Override
    public long getFileSize()
    {
        return fileSize;
    }

    // PatchReader API
    @Override
    public boolean hasMoreRecordInfos() throws IOException, BadPatchFormatException
    {
        if (nextRecordInfo != null) {
            return true;
        }

        // can't go to the next record if current data record has not been read completely
        assert numBytesLeftInCurrentDataRecord == 0 : "Not all bytes read from the current data record at " + pos;

        int value = dis.read();

        log.debug("Next record id at pos " + pos + ": " + value + " (char: " + (char)value + ")");

        if (value == -1) {
            close();
            return false;
        }

        ++pos;

        if (value == DATA_ID) {
            long len = readLong();

            if (len == 0) {
                throw new BadPatchFormatException("zero length data record at position: " + (pos - 8));
            }

            nextRecordInfo = new RecordInfo();
            nextRecordInfo.type = PatchReader.RecordType.DATA;
            nextRecordInfo.length = len;
        } else if (value == REF_ID) {
            nextRecordInfo = new RecordInfo();
            nextRecordInfo.type = PatchReader.RecordType.REF;
        } else {
            throw new BadPatchFormatException("garbage in patch: " + value + " at position: " + pos);
        }

        return true;
    }

    // PatchReader API
    @Override
    public RecordInfo getNextRecordInfo() throws IOException, BadPatchFormatException
    {
        assert nextRecordInfo != null;
        return nextRecordInfo;
    }

    // PatchReader API
    @Override
    public InputStream popData() throws IOException
    {
        assert nextRecordInfo != null;
        assert nextRecordInfo.type == PatchReader.RecordType.DATA;

        numBytesLeftInCurrentDataRecord = nextRecordInfo.length;
        nextRecordInfo = null;

        // input stream over a subsequence of bytes for the current data record
        return new InputStream() {

            // InputStream API
            @Override
            public int read(byte[] b, int off, int len) throws IOException
            {
                if (numBytesLeftInCurrentDataRecord == 0) {
                    return -1;
                }

                int numRead = dis.read(b, off, (int)Math.min(len, numBytesLeftInCurrentDataRecord));

                // Note, if we got -1 (eof) that means that the underlying stream got closed prematurely
                // before we have exhausted current data record, just return -1 in this case
                if (numRead != -1) {
                    numBytesLeftInCurrentDataRecord -= numRead;
                    pos += numRead;
                }

                return numRead;
            }

            // InputStream API
            @Override
            public int read() throws IOException
            {
                if (numBytesLeftInCurrentDataRecord == 0) {
                    return -1;
                }

                int value = dis.read();

                if (value != -1) {
                    --numBytesLeftInCurrentDataRecord;
                    ++pos;
                }

                return value;
            }

            // InputStream API
            @Override
            public int available() throws IOException
            {
                return (int)Math.min(numBytesLeftInCurrentDataRecord, dis.available());
            }

        };
    }

    // PatchReader API
    @Override
    public PatchRef popRef()
        throws IOException, BadPatchFormatException, InvalidPatchReferenceException
    {
        log.debug("Reading REF at pos " + pos);

        assert nextRecordInfo.type == PatchReader.RecordType.REF;

        nextRecordInfo = null;

        PatchRef patchRef = new PatchRef();

        patchRef.fileId = readInt();
        patchRef.fileVersion = readInt();
        patchRef.offset = readLong();
        patchRef.length = readInt();

        if (patchRef.length == 0) {
            throw new InvalidPatchReferenceException("zero length referenced at position: " + (pos - 4));
        }

        patchRef.hashKey = new byte[HASH_SIZE];
        dis.readFully(patchRef.hashKey);
        pos += patchRef.hashKey.length;

        return patchRef;
    }

    public void close() throws IOException
    {
        dis.close();
    }

    private ByteBuffer getByteBuffer(byte[] array)
    {
        return ByteBuffer.wrap(array).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    private short readShort() throws IOException, BadPatchFormatException
    {
        byte[] buf = new byte[2];
        dis.readFully(buf);
        pos += buf.length;
        return getByteBuffer(buf).getShort();
    }

    private int readInt() throws IOException, BadPatchFormatException
    {
        byte[] buf = new byte[4];
        dis.readFully(buf);
        pos += buf.length;
        return getByteBuffer(buf).getInt();
    }

    private long readLong() throws IOException, BadPatchFormatException
    {
        byte[] buf = new byte[8];
        dis.readFully(buf);
        pos += buf.length;
        return getByteBuffer(buf).getLong();
    }

}
