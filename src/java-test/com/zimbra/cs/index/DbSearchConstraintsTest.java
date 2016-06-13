/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
