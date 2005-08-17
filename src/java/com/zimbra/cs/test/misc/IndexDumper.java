/*
 * Created on May 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.test.misc;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;

import com.zimbra.cs.index.LuceneFields;


/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class IndexDumper {

    public static void main(String[] args) throws IOException {
        IndexReader reader = IndexReader.open("/tmp/liquid/index");
        TermEnum terms = reader.terms();
        while (terms.next()) {
            if (terms.term().field().equals(LuceneFields.L_MAILBOX_BLOB_ID)) {
                System.out.println(terms.term());    
            }
            //String field = terms.term().field();
            
        }
    }
}
