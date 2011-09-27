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
package com.zimbra.cs.index.global;

/**
 * Global search result.
 *
 * @author ysasaki
 */
public final class GlobalSearchHit {

    private final GlobalDocument document;
    private final int score;

    GlobalSearchHit(GlobalDocument doc, float score) {
        this.document = doc;
        this.score = (int) (score * 10000F); // want to avoid real numbers in SOAP
    }

    public GlobalDocument getDocument() {
        return document;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return document.getGID().toString() + '/' + score;
    }

}
