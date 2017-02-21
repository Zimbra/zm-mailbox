/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.util;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Multimap;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.util.PagedDelete;
import com.zimbra.cs.mailbox.util.TypedIdList;


/**
 * @author zimbra
 *
 */
public class PagedDeleteTest {

    @Test
    public void testTrimDeletesTillPageLimit() {
        TypedIdList tombstone = new TypedIdList();
        tombstone.add(Type.MESSAGE, 3, "", 100);
        tombstone.add(Type.MESSAGE, 4, "", 101);
        tombstone.add(Type.MESSAGE, 1, "", 101);
        tombstone.add(Type.MESSAGE, 5, "", 100);
        tombstone.add(Type.MESSAGE, 9, "", 103);
        PagedDelete pgDelete = new PagedDelete(tombstone, false);
        pgDelete.trimDeletesTillPageLimit(3);
        Collection<Integer> ids = pgDelete.getAllIds();
        Assert.assertEquals(3, ids.size());
        Assert.assertTrue(ids.contains(3));
        Assert.assertTrue(ids.contains(5));
        Assert.assertTrue(ids.contains(1));
        Assert.assertTrue(pgDelete.isDeleteOverFlow());
        Assert.assertTrue(pgDelete.getCutOffModsequnce() == 101);
        Assert.assertTrue(pgDelete.getLastItemId() == 4);
    }

    @Test
    public void testTypedDeletesTillPageLimit() {
        TypedIdList tombstone = new TypedIdList();
        tombstone.add(Type.MESSAGE, 3, "", 100);
        tombstone.add(Type.MESSAGE, 4, "", 101);
        tombstone.add(Type.APPOINTMENT, 1, "", 101);
        tombstone.add(Type.APPOINTMENT, 5, "", 100);
        tombstone.add(Type.MESSAGE, 9, "", 103);
        PagedDelete pgDelete = new PagedDelete(tombstone, true);
        pgDelete.trimDeletesTillPageLimit(3);
        Collection<Integer> ids = pgDelete.getAllIds();
        Assert.assertEquals(3, ids.size());
        Assert.assertTrue(ids.contains(3));
        Assert.assertTrue(ids.contains(5));
        Assert.assertTrue(ids.contains(1));
        Assert.assertTrue(pgDelete.isDeleteOverFlow());
        Assert.assertTrue(pgDelete.getCutOffModsequnce() == 101);
        Assert.assertTrue(pgDelete.getLastItemId() == 4);

        Multimap<Type,Integer> ids2Type = pgDelete.getTypedItemIds();
        Assert.assertEquals(3, ids2Type.size());
        Assert.assertTrue(ids2Type.containsEntry(Type.MESSAGE, 3));
        Assert.assertTrue(ids2Type.containsEntry(Type.APPOINTMENT, 5));
        Assert.assertTrue(ids2Type.containsEntry(Type.APPOINTMENT, 1));
    }

    @Test
    public void testRemoveBeforeCutoff() {
        TypedIdList tombstone = new TypedIdList();
        tombstone.add(Type.MESSAGE, 3, "", 100);
        tombstone.add(Type.MESSAGE, 4, "", 101);
        tombstone.add(Type.APPOINTMENT, 1, "", 101);
        tombstone.add(Type.APPOINTMENT, 5, "", 100);
        tombstone.add(Type.APPOINTMENT, 9, "", 103);
        tombstone.add(Type.MESSAGE, 2, "", 99);
        tombstone.add(Type.MESSAGE, 22, "", 105);
        tombstone.add(Type.MESSAGE, 24, "", 103);
        PagedDelete pgDelete = new PagedDelete(tombstone, true);
        pgDelete.removeBeforeCutoff(4, 101);
        Collection<Integer> ids = pgDelete.getAllIds();
        Assert.assertEquals(4, ids.size());
        pgDelete.trimDeletesTillPageLimit(3);
        ids = pgDelete.getAllIds();
        Assert.assertEquals(3, ids.size());
        Assert.assertTrue(ids.contains(4));
        Assert.assertTrue(ids.contains(9));
        Assert.assertTrue(ids.contains(24));
        Assert.assertTrue(pgDelete.isDeleteOverFlow());
        Assert.assertEquals(pgDelete.getCutOffModsequnce(),105);
        Assert.assertEquals(pgDelete.getLastItemId(), 22);
        Multimap<Type, Integer> ids2Type = pgDelete.getTypedItemIds();
        Assert.assertEquals(3, ids2Type.size());
        Assert.assertTrue(ids2Type.containsEntry(Type.MESSAGE, 4));
        Assert.assertTrue(ids2Type.containsEntry(Type.APPOINTMENT, 9));
        Assert.assertTrue(ids2Type.containsEntry(Type.MESSAGE, 24));
    }

    @Test
    public void testRemoveBeforeAfterCutoff() {
        TypedIdList tombstone = new TypedIdList();
        tombstone.add(Type.MESSAGE, 3, "", 100);
        tombstone.add(Type.MESSAGE, 4, "", 101);
        tombstone.add(Type.APPOINTMENT, 1, "", 101);
        tombstone.add(Type.APPOINTMENT, 5, "", 100);
        tombstone.add(Type.APPOINTMENT, 9, "", 103);
        tombstone.add(Type.MESSAGE, 2, "", 99);
        tombstone.add(Type.MESSAGE, 22, "", 105);
        tombstone.add(Type.MESSAGE, 24, "", 103);
        tombstone.add(Type.MESSAGE, 28, "", 106);
        PagedDelete pgDelete = new PagedDelete(tombstone, true);
        pgDelete.removeBeforeCutoff(4, 101);
        Collection<Integer> ids = pgDelete.getAllIds();
        Assert.assertEquals(5, ids.size());
        pgDelete.trimDeletesTillPageLimit(5);
        ids = pgDelete.getAllIds();
        Assert.assertEquals(5, ids.size());
        pgDelete.removeAfterCutoff(105);
        ids = pgDelete.getAllIds();
        Assert.assertEquals(4, ids.size());
        Assert.assertTrue(ids.contains(4));
        Assert.assertTrue(ids.contains(9));
        Assert.assertTrue(ids.contains(24));
        Assert.assertTrue(ids.contains(22));
        Assert.assertTrue(pgDelete.isDeleteOverFlow());
        Assert.assertEquals(pgDelete.getCutOffModsequnce(),106);
        Assert.assertEquals(pgDelete.getLastItemId(), 28);
        Multimap<Type, Integer> ids2Type = pgDelete.getTypedItemIds();
        Assert.assertEquals(4, ids2Type.size());
        Assert.assertTrue(ids2Type.containsEntry(Type.MESSAGE, 4));
        Assert.assertTrue(ids2Type.containsEntry(Type.APPOINTMENT, 9));
        Assert.assertTrue(ids2Type.containsEntry(Type.MESSAGE, 24));
        Assert.assertTrue(ids2Type.containsEntry(Type.MESSAGE, 22));
    }

}
