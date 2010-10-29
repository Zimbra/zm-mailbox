/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.ChecksumIndexOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;

/**
 * Try to fix a corrupted Lucene index data.
 * <p>
 * Once corruption is detected, re-indexing is the only way to recover
 * correctness. But, since re-indexing is very expensive, we do our best to
 * repair the index data with compromising some level of correctness.
 * <p>
 * Make sure there is absolutely no IndexReader or IndexWriter opening the index
 * data during the repair.
 *
 * @author ysasaki
 */
final class LuceneIndexRepair {
    // need to sync with SegmentInfos#CURRENT_FORMAT
    private static final int FORMAT = SegmentInfos.FORMAT_DIAGNOSTICS;
    // need to sync with IndexFileNames#SEGMENTS_GEN
    private static final String SEGMENTS_GEN = "segments.gen";

    private final Directory directory;
    private boolean clearDeleteCount = false;

    /**
     * Constructs a new {@link LuceneIndexRepair}.
     *
     * @param dir index data to repair
     * @param ex exception thrown by Lucene
     * @throws Throwable the same exception if it's not repairable
     */
    LuceneIndexRepair(Directory dir, Throwable ex) throws Throwable {
        directory = dir;
        if (ex instanceof AssertionError &&
                ex.getMessage().startsWith("delete count mismatch:")) {
            // https://issues.apache.org/jira/browse/LUCENE-1474
            clearDeleteCount = true;
        } else { // unable to fix
            throw ex;
        }
    }

    void repair() throws IOException {
        SegmentInfos seg = new SegmentInfos();
        seg.read(directory);

        ChecksumIndexInput input = new ChecksumIndexInput(
                directory.openInput(seg.getCurrentSegmentFileName()));
        String nextSegFilename = seg.getNextSegmentFileName();
        try {
            ChecksumIndexOutput output = new ChecksumIndexOutput(
                    directory.createOutput(nextSegFilename));
            try {
                convert(input, output);
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }

        directory.sync(nextSegFilename);

        // verify
        SegmentInfos nextSeg = new SegmentInfos();
        nextSeg.read(directory, nextSegFilename);
        if (seg.size() != nextSeg.size()) {
            throw new CorruptIndexException("Failed to verify");
        }

        try {
            commit(SegmentInfos.generationFromSegmentsFileName(nextSegFilename));
        } catch (IOException e) {
            directory.deleteFile(nextSegFilename);
            throw e;
        }
    }

    private void commit(long gen) throws IOException {
        IndexOutput output = directory.createOutput(SEGMENTS_GEN);
        try {
            output.writeInt(SegmentInfos.FORMAT_LOCKLESS);
            output.writeLong(gen);
            output.writeLong(gen);
        } finally {
            output.close();
        }
        directory.sync(SEGMENTS_GEN);
    }

    private void convert(ChecksumIndexInput input, ChecksumIndexOutput output)
        throws IOException {

        int format = input.readInt();
        if (format < 0) {
            if (format < FORMAT) {
                throw new CorruptIndexException("Unknown format version: " + format);
            } else {
                output.writeInt(format);
                long version = input.readLong();
                version++; // increment
                output.writeLong(version);
                output.writeInt(input.readInt()); // counter
            }
        } else { // file is in old format without explicit format info
            output.writeInt(format);
        }

        int num = input.readInt();
        output.writeInt(num);
        for (int i = 0; i < num; i++) {
            output.writeString(input.readString()); // name
            output.writeInt(input.readInt()); // count
            if (format <= SegmentInfos.FORMAT_LOCKLESS) {
                output.writeLong(input.readLong()); // generation
                if (format <= SegmentInfos.FORMAT_SHARED_DOC_STORE) {
                    int docStoreOffset = input.readInt();
                    output.writeInt(docStoreOffset);
                    if (docStoreOffset != -1) {
                        output.writeString(input.readString()); // docStoreSegment
                        output.writeByte(input.readByte()); // docStoreIsCompoundFile
                    }
                }
            }
            if (format <= SegmentInfos.FORMAT_SINGLE_NORM_FILE) {
                 output.writeByte(input.readByte()); // hasSingleNormFile
            }
            int numNormGen = input.readInt();
            output.writeInt(numNormGen);
            if (numNormGen > 0) {
                for (int j = 0; j < numNormGen; j++) {
                    output.writeLong(input.readLong()); // normGen
                }
            }
            output.writeByte(input.readByte()); // isCompoundFile
            if (format <= SegmentInfos.FORMAT_DEL_COUNT) {
                int delCount = input.readInt();
                output.writeInt(clearDeleteCount ? -1 : delCount);
            }
            if (format <= SegmentInfos.FORMAT_HAS_PROX) {
                output.writeByte(input.readByte()); // hasProx
            }
            if (format <= SegmentInfos.FORMAT_DIAGNOSTICS) {
                output.writeStringStringMap(input.readStringStringMap()); // diagnostics
            }
        }

        if (format >= 0){ // in old format the version number may be at the end of the file
            if (input.getFilePointer() < input.length()) {
                output.writeLong(input.readLong()); // version
            }
        }

        if (format <= SegmentInfos.FORMAT_USER_DATA) {
            if (format <= SegmentInfos.FORMAT_DIAGNOSTICS) {
                output.writeStringStringMap(input.readStringStringMap()); // user data
            } else {
                output.writeByte(input.readByte());
                output.writeString(input.readString()); // user data
            }
        }

        if (format <= SegmentInfos.FORMAT_CHECKSUM) {
            final long checksumNow = input.getChecksum();
            final long checksumThen = input.readLong();
            if (checksumNow != checksumThen) {
                throw new CorruptIndexException("checksum mismatch in segments file");
            }
        }

        output.finishCommit();
    }

}
