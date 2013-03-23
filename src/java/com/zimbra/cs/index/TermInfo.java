/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 VMware, Inc.
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.zimbra.common.util.ZimbraLog;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Fieldable;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Record of information related to search terms
 */
public final class TermInfo {
    @JsonProperty("pos")
    private List<Integer> positions;

    void addPosition(int value) {
        if (positions == null) {
            positions = new ArrayList<Integer>();
        }
        positions.add(value);
    }

    List<Integer> getPositions() {
        return positions;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("pos", positions).toString();
    }

    /**
     * Update {@code term2info} with information from {@code field}
     *
     *  if the field from the Lucene document is indexed and tokenized, for each token:
     *      a)   construct a key based on the field name and info about the token
     *      b)   if {@code term2info} has an entry for that key, get it, otherwise create an entry
     *      c)   update the entry with position information for this token
     *
     * @param pos is the current position
     * @return new value for {@code pos}
     */
    public static int updateMapWithDetailsForField(Analyzer analyzer, Fieldable field,
            Map<String, TermInfo> term2info, int pos)
    throws IOException {
        if (!field.isIndexed()) {
            return pos;
        }
        Character prefix = LuceneFields.FIELD2PREFIX.get(field.name());
        if (prefix == null) {
            ZimbraLog.index.info("TermInfo.updateMapWithDetailsForField - skipping indexed field " + field.name() +
                    " isTokenized=" + field.isTokenized());
            return pos;
        }
        if (field.isTokenized()) {
            TokenStream stream = field.tokenStreamValue();
            if (stream == null) {
                stream = analyzer.tokenStream(field.name(), new StringReader(field.stringValue()));
            }
            CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posAttr = stream.addAttribute(PositionIncrementAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                if (termAttr.length() == 0) {
                    continue;
                }
                String term = prefix + termAttr.toString();
                TermInfo info = term2info.get(term);
                if (info == null) {
                    info = new TermInfo();
                    term2info.put(term, info);
                }
                pos += posAttr.getPositionIncrement();
                info.addPosition(pos);
            }
        } else {
            // whole field is the only "token".  Info potentially getting stored twice - here as well as where
            // the field is stored.
            String term = prefix + field.stringValue();
            TermInfo info = term2info.get(term);
            if (info == null) {
                info = new TermInfo();
                term2info.put(term, info);
            }
        }
        return pos;
    }

}
