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
package com.zimbra.common.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MimeMultipart extends MimePart implements Iterable<MimePart> {
    static final String UNSET_BOUNDARY = "";

    private String mBoundary;
    private MimeBodyPart mPreamble, mEpilogue;
    private List<MimePart> mChildren = new ArrayList<MimePart>(3);

    public MimeMultipart(String subtype) {
        super(new ContentType("multipart/" + (subtype == null || subtype.trim().equals("") ? "mixed" : subtype)).setParameter("boundary", generateBoundary()));
    }

    MimeMultipart(ContentType ctype, MimePart parent, long start, long body, MimeHeaderBlock headers) {
        super(ctype, parent, start, body, headers);
        setEffectiveBoundary(ctype.getParameter("boundary"));
    }

    MimeMultipart(MimeMultipart mmp) {
        super(mmp);
        mBoundary = mmp.mBoundary;
        mPreamble = mmp.mPreamble == null ? null : mmp.mPreamble.clone();
        mEpilogue = mmp.mEpilogue == null ? null : mmp.mEpilogue.clone();
        for (MimePart child : mmp.mChildren) {
            mChildren.add(child.clone().setParent(this));
        }
    }


    @Override protected MimeMultipart clone() {
        return new MimeMultipart(this);
    }

    /** Returns the number of child parts of this multipart. */
    public int getCount() {
        return mChildren.size();
    }

    public MimeBodyPart getPreamble() {
        return mPreamble;
    }

    public MimeMultipart setPreamble(MimeBodyPart preamble) {
        mPreamble = preamble;
        return this;
    }

    public MimeBodyPart getEpilogue() {
        return mEpilogue;
    }

    public MimeMultipart setEpilogue(MimeBodyPart epilogue) {
        mEpilogue = epilogue;
        return this;
    }

    /** Returns the (0-based) {@code index}th child of this multipart. */
    public MimePart getSubpart(int index) {
        return (index < 0 || index >= mChildren.size() ? null : mChildren.get(index));
    }

    @Override public MimePart getSubpart(String part) {
        if (part == null || part.equals("")) {
            return this;
        }

        int dot = part.indexOf('.');
        if (dot == 0 || dot == part.length() - 1) {
            return null;
        }

        MimePart subpart = null;
        try {
            subpart = getSubpart(Integer.valueOf(dot == -1 ? part : part.substring(0, dot)) - 1);
        } catch (NumberFormatException nfe) { }

        if (dot == -1 || subpart == null) {
            return subpart;
        } else {
            return subpart.getSubpart(part.substring(dot + 1));
        }
    }

    @Override Map<String, MimePart> listMimeParts(Map<String, MimePart> parts, String parentName) {
        String prefix = parentName.isEmpty() ? "" : parentName + ".";
        for (int i = 0; i < mChildren.size(); i++) {
            MimePart child = mChildren.get(i);
            String childName = prefix + (i + 1);

            parts.put(childName, child);
            child.listMimeParts(parts, childName);
        }
        return parts;
    }

    /** Returns an iterator over the child parts of this multipart.  Note that
     *  changes made via the iterator (e.g. {@link Iterator#remove()}) will not
     *  affect the contents of the multipart. */
    @Override public Iterator<MimePart> iterator() {
        return new ArrayList<MimePart>(mChildren).iterator();
    }

    public MimeMultipart addPart(MimePart mp) {
        return addPart(mp, mChildren.size());
    }

    public MimeMultipart addPart(MimePart mp, int index) {
        if (mp == null) {
            throw new NullPointerException();
        } else if (index < 0 || index > mChildren.size()) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }

        markDirty(Dirty.CONTENT);
        mp.setParent(this);
        mChildren.add(index, mp);
        return this;
    }

    public MimePart removePart(int index) {
        return mChildren.get(index).setParent(null);
    }

    public boolean removePart(MimePart mp) {
        boolean present = mChildren.contains(mp);
        if (present) {
            mp.setParent(null);
        }
        return present;
    }

    @Override void removeChild(MimePart mp) {
        if (mChildren.remove(mp)) {
            markDirty(Dirty.CONTENT);
        }
    }


    String getDefaultChildContentType() {
        return getContentType().getSubType().equals("digest") ? "message/rfc822" : "text/plain";
    }

    @Override ContentType updateContentType(ContentType ctypeParam) {
        if (ctypeParam != null && !ctypeParam.getPrimaryType().equals("multipart")) {
            throw new UnsupportedOperationException("cannot change a multipart to text: " + ctypeParam);
        }
        ContentType ctype = ctypeParam == null ? new ContentType("multipart/mixed") : ctypeParam;

        // changing the boundary forces a recalc of the content
        String newBoundary = normalizeBoundary(ctype.getParameter("boundary"));
        if (!newBoundary.equals(mBoundary)) {
            markDirty(Dirty.CONTENT);
            mBoundary = newBoundary;
        }

        // FIXME: if moving to/from multipart/digest, make sure to recalculate defaults on subparts
        return super.updateContentType(ctype);
    }

    String getBoundary() {
        return mBoundary;
    }

    String setEffectiveBoundary(String boundary) {
        // can't change a real boundary
        if (mBoundary == UNSET_BOUNDARY || mBoundary == null) {
            mBoundary = normalizeBoundary(boundary);
        }
        return mBoundary;
    }

    private static String normalizeBoundary(String bnd) {
        String boundary = bnd == null ? UNSET_BOUNDARY : bnd;
        // RFC 2046 5.1.1: "The only mandatory global parameter for the "multipart" media type is
        //                  the boundary parameter, which consists of 1 to 70 characters from a
        //                  set of characters known to be very robust through mail gateways, and
        //                  NOT ending with white space."
        while (boundary.length() > 0 && Character.isWhitespace(boundary.charAt(boundary.length() - 1))) {
            boundary = boundary.substring(0, boundary.length() - 1);
        }
        return boundary;
    }

    private static String generateBoundary() {
        // RFC 1521 5.1: "A good strategy is to choose a boundary that includes a character
        //                sequence such as "=_" which can never appear in a quoted-printable body."
        return "=_" + UUID.randomUUID().toString();
    }


    @Override public long getSize() throws IOException {
        long size = super.getSize();
        if (size == -1) {
            size = mPreamble == null ? 0 : mPreamble.getSize();
            int bndlen = mBoundary.getBytes().length, count = mPreamble == null ? 0 : 1;
            for (MimePart mp : mChildren) {
                size += count++ == 0 ? bndlen + 4 : bndlen + 6;
                size += mp.getMimeHeaderBlock().getLength();
                size += mp.getSize();
            }
            size += bndlen + 8;
            size += mEpilogue == null ? 0 : mEpilogue.getSize();
            size = recordSize(size);
        }
        return size;
    }

    @Override public InputStream getRawContentStream() throws IOException {
        if (!isDirty()) {
            return super.getRawContentStream();
        }

        byte[] startBoundary = ("\r\n--" + mBoundary + "\r\n").getBytes();

        List<Object> sources = new ArrayList<Object>(mChildren.size() * 2 + 3);
        if (mPreamble != null) {
            sources.add(mPreamble);
        }
        for (MimePart mp : mChildren) {
            sources.add(sources.isEmpty() ? ("--" + mBoundary + "\r\n").getBytes() : startBoundary);
            sources.add(mp);
        }
        sources.add(("\r\n--" + mBoundary + "--\r\n").getBytes());
        if (mEpilogue != null) {
            sources.add(mEpilogue);
        }
        return new VectorInputStream(sources);
    }
}
