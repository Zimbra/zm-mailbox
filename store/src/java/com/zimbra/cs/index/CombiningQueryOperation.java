/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for query operations that combine sets of sub-operations (e.g. Intersections or Unions).
 */
abstract class CombiningQueryOperation extends QueryOperation {

    protected List<QueryOperation> operations = new ArrayList<QueryOperation>();

    int getNumSubOps() {
        return operations.size();
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return operations.stream().allMatch(op -> op.isRelevanceSortSupported);
    }
}
