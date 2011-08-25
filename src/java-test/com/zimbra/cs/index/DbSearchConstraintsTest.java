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
package com.zimbra.cs.index;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link DbSearchConstraints}.
 *
 * @author ysasaki
 */
public final class DbSearchConstraintsTest {

    @Test
    public void copy() {
        DbSearchConstraints.Leaf leaf = new DbSearchConstraints.Leaf();
        leaf.addDateRange(100, true, 200, false, true);
        DbSearchConstraints.Leaf clone = leaf.clone();
        clone.addSizeRange(300, true, 300, false, true);

        Assert.assertEquals(1, leaf.ranges.size());
        Assert.assertEquals(2, clone.ranges.size());
    }
}
