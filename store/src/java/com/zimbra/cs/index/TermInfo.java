/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;

import com.zimbra.common.util.ZimbraLog;

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
        return MoreObjects.toStringHelper(this).add("pos", positions).toString();
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
    public static int updateMapWithDetailsForField(Analyzer analyzer, Field field,
            Map<String, TermInfo> term2info, int pos)
    throws IOException {
        FieldType fieldType = field.fieldType();
        if (fieldType.indexOptions() == null) {
            return pos;
        }
        Character prefix = LuceneFields.FIELD2PREFIX.get(field.name());
        if (prefix == null) {
            ZimbraLog.index.info("TermInfo.updateMapWithDetailsForField - skipping indexed field " + field.name() +
                    " isTokenized=" + fieldType.tokenized());
            return pos;
        }
        if (fieldType.tokenized()) {
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
