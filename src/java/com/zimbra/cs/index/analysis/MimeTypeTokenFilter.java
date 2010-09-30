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
package com.zimbra.cs.index.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * {@code image/jpeg} becomes {@code image/jpeg} and {@code image}
 *
 * @author tim
 * @author ysasaki
 */
public final class MimeTypeTokenFilter extends TokenFilter {

    private Token mCurToken = new Token();
    private int mNextSplitPos;
    private int mNumSplits;

    private TermAttribute termAttr = addAttribute(TermAttribute.class);
    private OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

    public MimeTypeTokenFilter(TokenFilter in) {
        super(in);
    }

    public MimeTypeTokenFilter(TokenStream in) {
        super(in);
    }

    /**
     * Returns the next split point.
     *
     * @param s string
     * @return next split offset
     */
    private int getNextSplit(String s) {
        return s.indexOf("/");
    }

    /**
     * At this point, a token has been extracted from input, and the full
     * token has been returned to the stream. Now we want to return all the
     * "split" forms of the token.
     *
     * On the first call to this API for this token, mNextSplitPos is set to
     * the value of getNextSplit(full_token_text)..., then this API is
     * called repeatedly until mCurToken is cleared.
     */
    private void nextSplit() {
        if (mNextSplitPos > 0 && mNumSplits < 1) {
            // split another piece, save our state, and return...
            mNumSplits++;
            String term = mCurToken.term();
            setAttrs(term.substring(0, mNextSplitPos),
                    mCurToken.startOffset(),
                    mCurToken.startOffset() + mNextSplitPos,
                    mCurToken.type());
            mNextSplitPos++;
            String secondPart = term.substring(mNextSplitPos);
            if (mNumSplits < 1) {
                mNextSplitPos = getNextSplit(secondPart);
            }
            mCurToken.clear();
            return;
        }

        // if we get here, then we've either split as many times as we're
        // allowed, OR we've run out of places to split..
        // no more splitting, just return what's left...
        setAttrs(mCurToken.term(), mCurToken.startOffset(),
                mCurToken.endOffset(), mCurToken.type());
        mCurToken.clear();
    }

    @Override
    public boolean incrementToken() throws IOException {
        while (true) {
            if (mCurToken.termLength() == 0) {
                // Get a new token, and insert the full token (unsplit) into
                // the index.
                if (!input.incrementToken()) {
                    return false;
                }

                // Does it have any sub-parts that need to be added separately?
                // If so, then save them as internal state: we'll add them in a bit.
                String term = termAttr.term();
                if (term.length() <= 1) { // ignore short term text
                    continue;
                }
                mNextSplitPos = getNextSplit(term);
                if (mNextSplitPos <= 0) {
                    // no sub-tokens
                    return true;
                }

                // Now, Insert the full string as a token...we might continue down below
                // (other parts) if there is more to add...
                mNumSplits = 0;
                mCurToken.reinit(term, offsetAttr.startOffset(),
                        offsetAttr.endOffset(), typeAttr.type());

                return true;
            } else {
                // once we get here, we know that the full text has been inserted
                // once as a single token, now we need to insert all the split tokens
                nextSplit();
                return true;
            }
        }
    }

    private void setAttrs(String term, int start, int end, String type) {
        termAttr.setTermBuffer(term);
        offsetAttr.setOffset(start, end);
        typeAttr.setType(type);
    }

}
