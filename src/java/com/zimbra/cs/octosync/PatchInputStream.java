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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.input.TeeInputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;


/**
 * Presents contents of the file resulting from Octopus patch application as
 * an input stream.
 *
 * The class effectively multiplexes data from existing files (blobs) on the server
 * as referenced by an Octopus patch and the data contained within the patch itself:
 *
 * 1) PatchReader provides access to the patch itself containing references to
 * exiting files and the embedded data
 *
 * 2) BlobAccess provides access to the existing blobs (files) on the server
 *
 * @author grzes
 */
public class PatchInputStream extends InputStream
{
    private static final Log log = LogFactory.getLog(PatchInputStream.class);

    /** Maximum size of referenced chunk */
    private static final int MAX_REF_LENGTH = 64*1024;

    private PatchReader patchReader;
    private BlobAccess blobAccess;
    private PatchManifest manifest;
    private InputStream currentIs;

    /**
     * Instantiates a new PatchInputStream.
     *
     * @param patchReader The patch reader, provides the patch contents.
     * @param blobAccess The blob index. Provides access to server blobs.
     * @param manifest The manifest to populate, optional, can be null
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException A service exception
     */
    public PatchInputStream(
            PatchReader patchReader,
            BlobAccess blobAccess,
            PatchManifest manifest) throws IOException, ServiceException
    {
        this.patchReader = patchReader;
        this.blobAccess = blobAccess;
        this.manifest = manifest;
        currentIs = nextInputStream();
    }

    /**
     * Factory method for convenient creation of PatchInputStream using
     * BinaryPatchReader and BlobAccessViaMailbox.
     *
     * @param is Input stream with the patch data
     * @param mbox Mailbox used to access blobs
     * @param opContext The operation context
     * @param defaultFileId The file id to be the default file id
     * @param defaultVersion The default version
     * @param patchOs Output stream for the patch. If provided a byte copy
     *      of the patch will be made and written to the output stream as the patch
     *      is being processed. Can be null
     *
     * @param manifest Patch manifest to populate with references being made by
     *      the patch. Optional, can be null
     *
     * @return New instance of PatchInputStream
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException the service exception
     */
    public static PatchInputStream create(InputStream is, Mailbox mbox, OperationContext opContext,
            int defaultFileId, int defaultVersion, OutputStream patchOs, PatchManifest manifest) throws IOException, ServiceException
    {
        InputStream input = null;

        if (patchOs != null) {
            input = new TeeInputStream(is, patchOs);
        } else {
            input = is;
        }

        return new PatchInputStream(
                new BinaryPatchReader(input),
                new BlobAccessViaMailbox(mbox, opContext, defaultFileId, defaultVersion),
                manifest);
    }

    private InputStream nextInputStream() throws IOException, ServiceException
    {
        if (!patchReader.hasMoreRecordInfos()) {
            return null;
        }

        PatchReader.RecordInfo ri = patchReader.getNextRecordInfo();

        InputStream nextStream = null;

        if (ri.type == PatchReader.RecordType.DATA) {

            log.debug("Patch data, length: " + ri.length);
            nextStream = patchReader.popData();

        } else if (ri.type == PatchReader.RecordType.REF) {

            PatchRef patchRef = patchReader.popRef();
            log.debug("Patch reference " + patchRef);

            if (patchRef.length > MAX_REF_LENGTH) {
                throw new InvalidPatchReferenceException("referenced data too large: " +
                        patchRef.length + " > " + MAX_REF_LENGTH);
            }

            if (manifest != null) {
                int[] actualRef = blobAccess.getActualReference(patchRef.fileId, patchRef.fileVersion);
                manifest.addReference(actualRef[0], actualRef[1], patchRef.length);
            }

            try {
                InputStream blobIs =
                    blobAccess.getBlobInputStream(patchRef.fileId, patchRef.fileVersion);

                blobIs.skip(patchRef.offset);

                byte[] chunkBuf = new byte[patchRef.length];

                DataInputStream dis = new DataInputStream(blobIs);
                dis.readFully(chunkBuf);
                dis.close();

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(chunkBuf);
                byte[] calcHash = md.digest();

                if (!Arrays.equals(patchRef.hashKey, calcHash)) {
                    throw new InvalidPatchReferenceException("refrenced data hash mismatch, actual hash: " +
                            new String(Hex.encodeHex(calcHash)) + "; " + patchRef);
                }

                nextStream = new ByteArrayInputStream(chunkBuf);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                assert false : "SHA-256 must be supported";
            }
        } else {
            assert false : "Invalid record type: " + ri.type;
        }

        assert nextStream != null : "Stream returned here must be non-null";
        return nextStream;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (currentIs == null) {
            return -1;
        }

        int result = currentIs.read(b, off, len);

        if (result == -1) {

            currentIs.close();

            // let's see we we can get another stream and read from it
            try {
                currentIs = nextInputStream();
            } catch (ServiceException e) {
                throw new PatchException(e);
            }
            if (currentIs != null) {
                result = currentIs.read(b, off, len);
            }
        }
        return result;
    }

    // InputStream API
    @Override
    public int read() throws IOException
    {
        // not the most efficient way to implemented read(), but we don't care
        // this one should not be really used anyway
        byte[] buf = new byte[1];
        int result = this.read(buf, 0, 1);
        if (result == 1) {
            return buf[0];
        } else {
            return result;
        }
    }


    // InputStream API
    @Override
    public int available() throws IOException
    {
        if (currentIs != null) {
            return currentIs.available();
        } else {
            return 0;
        }
    }

    public void close() throws IOException
    {
        // try here is paranoid, but we want to make sure we'll close PatchReader
        try {
            super.close();
        } finally {
            patchReader.close();
        }
    }
}

