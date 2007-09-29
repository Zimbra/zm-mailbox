/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

//import org.apache.lucene.index.Term;
//import org.apache.lucene.search.BooleanClause;
//import org.apache.lucene.search.BooleanQuery;
//import org.apache.lucene.search.TermQuery;


/************************************************************************
 * 
 * AllQueryOperation
 * 
 ***********************************************************************/

//class AllQueryOperation extends LuceneQueryOperation 
//{
//    
//    public AllQueryOperation()
//    {
//        mQuery = new BooleanQuery();
//        mQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE)), true, false));
//    }
//}

class AllQueryOperation extends DBQueryOperation
{
    protected AllQueryOperation() {}
}