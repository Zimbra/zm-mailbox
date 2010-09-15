/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.mime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.zimbra.common.util.ByteUtil;

public abstract class MimePart {
    /** The property specifying the default charset to use for both parsing
     *  and encoding 8-bit headers and message content. */
    public static final String PROP_CHARSET_DEFAULT = "charset.default";

    private MimePart mParent;
    private MimeHeaderBlock mMimeHeaders;
    private ContentType mContentType;
    private long mStartOffset = -1L, mBodyOffset = -1L, mEndOffset = -1L;
    private int mLineCount = -1;
    private PartSource mPartSource;
    private boolean mDirty;

    MimePart(ContentType ctype) {
        mContentType = new ContentType(ctype);
        checkContentType(mContentType);
        mMimeHeaders = new MimeHeaderBlock(this instanceof MimeMessage);
        setMimeHeader("Content-Type", new MimeHeader("Content-Type", mContentType));
        mDirty = true;
    }

    MimePart(ContentType ctype, MimePart parent, long start, long body, MimeHeaderBlock headers) {
        mParent = parent;
        mContentType = ctype;
        mMimeHeaders = headers;
        mStartOffset = start;
        mBodyOffset = body;
    }

    MimePart getParent() {
        return mParent;
    }

    void setParent(MimePart mp) {
        if (mParent != mp) {
            detach();
            mParent = mp;
        }
    }

    public MimePart detach() {
        mPartSource = getPartSource();
        if (mParent != null) {
            mParent.removeChild(this);
        }
        return this;
    }

    abstract void removeChild(MimePart mp);

    private PartSource getPartSource() {
        return mPartSource != null || mParent == null ? mPartSource : mParent.getPartSource();
    }

    /** Fetches a subpart of this part, specified via the IMAP part naming
     *  convention (see {@see http://tools.ietf.org/html/rfc3501#page-56}).
     *  If the part name is invalid or the requested part does not exist,
     *  returns <tt>null</tt>. */
    public MimePart getSubpart(String part) {
        return part == null || part.equals("") ? this : null;
    }

    Map<String, MimePart> listMimeParts(Map<String, MimePart> parts, String prefix) {
        return parts;
    }

    long getStartOffset() {
        return mStartOffset;
    }

    long getBodyOffset() {
        return mBodyOffset;
    }

    long getEndOffset() {
        return mEndOffset;
    }

    long getSize() {
        return mEndOffset - mBodyOffset;
    }

    int getLineCount() {
        return mLineCount;
    }

    Properties getProperties() {
        return mParent == null ? null : mParent.getProperties();
    }

    String getDefaultCharset() {
        Properties props = getProperties();
        return props == null ? null : props.getProperty(PROP_CHARSET_DEFAULT);
    }


    /** Returns the value of the first header matching the given
     *  <code>name</code>. */
    public String getMimeHeader(String name) {
        return mMimeHeaders == null ? null : mMimeHeaders.getHeader(name, getDefaultCharset());
    }

    /** Returns the raw (<code>byte[]</code>) value of the first header
     *  matching the given <code>name</code>. */
    public byte[] getRawMimeHeader(String name) {
        return mMimeHeaders == null ? null : mMimeHeaders.getRawHeader(name);
    }

    public MimePart setMimeHeader(String name, String value) {
        if (name.equalsIgnoreCase("Content-Type")) {
            setContentType(new ContentType(value));
        } else {
            setMimeHeader(name, value == null ? null : new MimeHeader(name, value));
        }
        return this;
    }

    public void setMimeHeader(String name, MimeHeader header) {
        getMimeHeaderBlock().setHeader(name, header);

        if (mParent != null) {
            mParent.markDirty(true);
        }
        mStartOffset = -1;
    }

    void addMimeHeader(String name, MimeHeader header) {
        getMimeHeaderBlock().addHeader(name, header);

        if (mParent != null) {
            mParent.markDirty(true);
        }
        mStartOffset = -1;
    }

    public MimeHeaderBlock getMimeHeaderBlock() {
        if (mMimeHeaders == null) {
            mMimeHeaders = new MimeHeaderBlock(false);
        }
        return mMimeHeaders;
    }

    /** Returns the effective Content-Type for this part.  Note that this should
     *  <u>never</u> be <tt>null</tt>; in the event that a <tt>Content-Type</tt>
     *  header was not specified for the part, a {@link ContentType} object will
     *  be returned representing the effective Content-Type for the part. */
    public ContentType getContentType() {
        return new ContentType(mContentType);
    }

    public void setContentType(ContentType ctype) {
        mContentType = new ContentType(ctype);
        setMimeHeader("Content-Type", ctype == null ? null : new MimeHeader("Content-Type", ctype));
    }

    abstract void checkContentType(ContentType ctype);

    public ContentDisposition getContentDisposition() {
        return new ContentDisposition(getMimeHeader("Content-Disposition"));
    }

    public String getFilename() {
        String filename = mContentType.getParameter("name");
        if (filename == null || filename.equals("")) {
            filename = getContentDisposition().getParameter("filename");
        }
        return filename;
    }


    /** Returns an <code>InputStream</code> whose content is the <u>entire</u>
     *  part, MIME headers and all.  If you only want the part body, try
     *  {@see #getContentStream()}. */
    public InputStream getInputStream() throws IOException {
        if (!isDirty() && mStartOffset >= 0) {
            return getRawContentStream(mStartOffset, mEndOffset);
        }

        List<Object> sources = new ArrayList<Object>(2);
        if (mStartOffset != -1 && getPartSource() != null) {
            sources.add(getRawContentStream(mStartOffset, mBodyOffset));
        } else if (mMimeHeaders != null) {
            sources.add(mMimeHeaders.toByteArray());
        }
        sources.add(getRawContentStream());
        return new VectorInputStream(sources);
    }

    /** Returns an <code>InputStream</code> whose content is the raw, undecoded
     *  body of the part.  If you want the body with the content transfer
     *  encoding removed, try {@see #getContentStream()}. */
    public InputStream getRawContentStream() throws IOException {
        return getRawContentStream(mBodyOffset, mEndOffset);
    }

    private InputStream getRawContentStream(long start, long end) throws IOException {
        PartSource source = getPartSource();
        return source == null ? null : source.getContentStream(start, end);
    }

    /** Returns a <code>byte[]</code> array whose content is the raw, undecoded
     *  body of the part.  If you want the body with the content transfer
     *  encoding removed, try {@see #getContent()}. */
    public byte[] getRawContent() throws IOException {
        if (!isDirty()) {
            return getPartSource().getContent(mBodyOffset, mEndOffset);
        }

        InputStream is = getRawContentStream();
        return is == null ? null : ByteUtil.getContent(is, (int) (mEndOffset - mBodyOffset));
    }

    /** Returns an <code>InputStream</code> whose content is the body of the
     *  part after any content transfer has been decoded.  If you want the raw
     *  part body with encoding intact, try {@see #getRawContentStream()()}. */
    public InputStream getContentStream() throws IOException {
        return getRawContentStream();
    }

    /** Returns a <code>byte[]</code> array whose content is the body of the
     *  part after any content transfer has been decoded.  If you want the raw
     *  part body with encoding intact, try {@see #getRawContent()}. */
    public byte[] getContent() throws IOException {
        return getRawContent();
    }

    public MimePart setContent(byte[] content) {
        return setContent(content, true);
    }

    MimePart setContent(byte[] content, boolean markDirty) {
        if (markDirty && mParent != null) {
            mParent.markDirty(true);
        }

        mPartSource  = new PartSource(content);
        mStartOffset = -1;
        mBodyOffset  = content == null ? -1 : 0;
        mEndOffset   = content == null ? -1 : content.length;
        return this;
    }

    public MimePart setContent(File file) {
        return setContent(file, true);
    }

    MimePart setContent(File file, boolean markDirty) {
        if (markDirty && mParent != null) {
            mParent.markDirty(true);
        }
        if (!file.exists()) {
            file = null;
        }

        mPartSource  = new PartSource(file);
        mStartOffset = -1;
        mBodyOffset  = file == null ? -1 : 0;
        mEndOffset   = file == null ? -1 : file.length();
        return this;
    }

    /** Marks the item as "dirty" so that we regenerate the part when
     *  serializing. */
    void markDirty(boolean dirtyBody) {
        if (!isDirty()) {
            mDirty |= dirtyBody;
            // changing anything in the part effectively changes the body of the parent
            if (mParent != null) {
                mParent.markDirty(true);
            }
        }
    }

    boolean isDirty() {
        return mDirty || getPartSource() == null;
    }

    protected void recordEndpoint(long position, int lineCount) {
        mEndOffset = position;
        mLineCount = lineCount;
    }


    private static class PartSource {
        private final byte[] mBodyContent;
        private final File mBodyFile;

        PartSource(byte[] content)  { mBodyContent = content;  mBodyFile = null; }
        PartSource(File file)       { mBodyContent = null;     mBodyFile = file; }

        InputStream getContentStream(long start, long end) throws IOException {
            if (mBodyContent != null) {
                start = Math.max(0, Math.min(start, mBodyContent.length));
                end = end < 0 ? mBodyContent.length : Math.max(start, Math.min(end, mBodyContent.length));
                return new ByteArrayInputStream(mBodyContent, (int) start, (int) (end - start));
            } else if (mBodyFile != null) {
                if (!mBodyFile.exists()) {
                    return null;
                }
                // FIXME: check for GZIPped content
                int fileLength = (int) mBodyFile.length();
                start = Math.max(0, Math.min(start, fileLength));
                end = end < 0 ? fileLength : Math.max(start, Math.min(end, fileLength));
                FileInputStream fis = new FileInputStream(mBodyFile);
                try {
                    return start == 0 && end == fileLength ? fis : ByteUtil.SegmentInputStream.create(fis, start, end);
                } catch (IOException ioe) {
                    ByteUtil.closeStream(fis);
                    throw ioe;
                }
            } else {
                return null;
            }
        }

        byte[] getContent(long start, long end) throws IOException {
            if (mBodyContent != null) {
                start = Math.max(0, Math.min(start, mBodyContent.length));
                end = end < 0 ? mBodyContent.length : Math.max(start, Math.min(end, mBodyContent.length));
                int size = (int) (end - start);
                if (size == mBodyContent.length) {
                    return mBodyContent;
                }
                byte[] content = new byte[size];
                System.arraycopy(mBodyContent, (int) start, content, 0, size);
                return content;
            } else if (mBodyFile != null) {
                RandomAccessFile raf;
                try {
                    raf = new RandomAccessFile(mBodyFile, "r");
                } catch (FileNotFoundException fnfe) {
                    return null;
                }
                // FIXME: check for GZIPped content
                int fileLength = (int) raf.length();
                start = Math.max(0, Math.min(start, fileLength));
                end = end < 0 ? fileLength : Math.max(start, Math.min(end, fileLength));
                raf.seek(start);
                byte[] content = new byte[(int) (end - start)];
                raf.readFully(content);
                return content;
            } else {
                return null;
            }
        }
    }


    static class VectorInputStream extends InputStream {
        private final List<Object> mItems;
        private int mNextIndex;
        private InputStream mCurrentStream;

        VectorInputStream(List<Object> items) throws IOException {
            mItems = new ArrayList<Object>(items);
            while (mItems.remove(null))
                ;
            getNextStream();
        }

        @Override public int read() throws IOException {
            int c = mCurrentStream == null ? -1 : mCurrentStream.read();
            while (c == -1 && mCurrentStream != null) {
                c = getNextStream() == null ? -1 : mCurrentStream.read();
            }
            return c;
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            int num = mCurrentStream == null ? -1 : mCurrentStream.read(b, off, len);
            while (num == -1 && mCurrentStream != null) {
                num = getNextStream() == null ? -1 : mCurrentStream.read(b, off, len);
            }
            return num;
        }

        @Override public long skip(long n) throws IOException {
            long remaining = n - (mCurrentStream == null ? 0 : mCurrentStream.skip(n));
            while (remaining > 0 && mCurrentStream != null) {
                remaining -= getNextStream() == null ? 0 : mCurrentStream.skip(remaining);
            }
            return n - remaining;
        }

        @Override public void close() {
            InputStream current = mCurrentStream;
            mCurrentStream = null;
            ByteUtil.closeStream(current);

            while (mNextIndex < mItems.size()) {
                Object next = mItems.get(mNextIndex++);
                if (next instanceof InputStream) {
                    ByteUtil.closeStream((InputStream) next);
                }
            }
        }

        private InputStream getNextStream() throws IOException {
            ByteUtil.closeStream(mCurrentStream);
            Object next = mNextIndex >= mItems.size() ? null : mItems.get(mNextIndex);
            if (next == null) {
                mCurrentStream = null;
            } else if (next instanceof byte[]) {
                mCurrentStream = new ByteArrayInputStream((byte[]) next);
            } else if (next instanceof InputStream) {
                mCurrentStream = (InputStream) next;
            } else if (next instanceof MimePart) {
                mCurrentStream = ((MimePart) next).getInputStream();
            } else {
                mCurrentStream = new ByteArrayInputStream(next.toString().getBytes());
            }
            mNextIndex++;
            return mCurrentStream;
        }
    }
}
