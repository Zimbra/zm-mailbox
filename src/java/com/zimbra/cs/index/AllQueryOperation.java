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