/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.index.ZimbraAnalyzer;

/**
 * Unit test for {@link SubjectQuery}.
 *
 * @author ysasaki
 */
public final class SubjectQueryTest {

    @Test
    public void emptySubject() throws Exception {
        Query query = SubjectQuery.create(ZimbraAnalyzer.getInstance(), "");
        Assert.assertEquals(TextQuery.class, query.getClass());
        Assert.assertEquals("Q(subject:)", query.toString());
    }

}
