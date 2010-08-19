/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for query operations that combine sets of sub-operations
 * (e.g. Intersections or Unions)
 */
abstract class CombiningQueryOperation extends QueryOperation {

    protected List<QueryOperation> mQueryOperations =
        new ArrayList<QueryOperation>();

    int getNumSubOps() {
        return mQueryOperations.size();
    }

}
