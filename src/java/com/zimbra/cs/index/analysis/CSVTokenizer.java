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

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

/**
 * Comma-separated values, typically for content type list.
 *
 * @author tim
 * @author ysasaki
 */
public final class CSVTokenizer extends CharTokenizer {

    public CSVTokenizer(Reader in) {
        super(in);
    }

    @Override
    protected boolean isTokenChar(char c) {
        return c != ',';
    }

    @Override
    protected char normalize(char c) {
        return Character.toLowerCase(c);
    }

}
