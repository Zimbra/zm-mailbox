/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;

public class LuceneViewer {
    String mIndexDir;
    String mOutputFile;
    IndexReader mIndexReader;
    FileWriter mWriter;

    public LuceneViewer(String indexName, String outfileName)throws Exception {
        mIndexDir=indexName;
        mOutputFile=outfileName;
        
        mIndexReader = IndexReader.open(mIndexDir);
        mWriter = new FileWriter(mOutputFile);
    }

    private void closeIndexReader() throws IOException {
        mIndexReader.close();
    }
    
    private void outputBanner(String bannerText) throws IOException{
        outputLn();
        outputLn("==============================");
        outputLn(bannerText);
        outputLn("==============================");
        outputLn();
    }
    
    private void outputLn() throws IOException {
        output("\n");
    }
    
    private void outputLn(String out) throws IOException {
        output(out + "\n");
    }
    
    private void output(String out) throws IOException {
        mWriter.write(out);
        mWriter.flush();
    }
    
    private void closeOutputWriter() throws IOException {
        mWriter.close();
    }

    private void dump() throws Exception {
        
        outputLn("Index directory: " + mIndexDir);
        outputLn("Output file:     " + mOutputFile);
        outputLn();
        
        dumpFields();
        dumpDocuments();
        dumpTerms();
        
        outputBanner("end");
        
        closeIndexReader();
        closeOutputWriter();
    }

    private void dumpFields() throws IOException {
        outputBanner("Fields");

        Collection fieldNames = mIndexReader.getFieldNames(IndexReader.FieldOption.ALL);
        for (Object fieldName : fieldNames) {
            outputLn("    " + fieldName.toString());
        }
    }
    
    private void dumpDocuments() throws IOException {
        outputBanner("Documents");
        
        int totalDocs = mIndexReader.numDocs();
        
        outputLn();
        outputLn("There are " + totalDocs + " documents in this index.");
        
        for (int i = 0; i < totalDocs; i++) {
            Document doc = mIndexReader.document(i, null);
            dumpDocument(i, doc);
        }
    }
    
    private void dumpDocument(int docNum, Document doc) throws IOException {
        
        outputLn();
        outputLn("Document " + docNum);
        
        for (Field field : (List<Field>)doc.getFields()) {
            String fieldName = field.name();
            // String value = field.stringValue();
            // outputLn("    Field " + name + ": " + value);
            
            boolean isDate = "l.date".equals(fieldName);
                
            outputLn("    Field [" + fieldName + "]: " + field.toString());
            String[] values = doc.getValues(fieldName);
            if (values != null) {
                for (String value : values) {
                    output("         " + value);
                    if (isDate) {
                        Date dt = DateField.stringToDate(value);
                        String d = dt.toString()+" ("+dt.getTime()+")";
                        output(" (" + d + ")");
                    }
                    outputLn();
                }
            }
        }
    }
    
    private void dumpTerms() throws IOException {
        outputBanner("Terms (in Term.compareTo() order)");
        
        TermEnum terms = mIndexReader.terms();
        int order = 0;
        
        while (terms.next()) {
            order++;
            Term term = terms.term();
            String field = term.field();
            String text = term.text();
            // outputLn(field + ": " + text + "(" + term.toString() + ")");
            outputLn(order + " " + field + ": " + text);
            
            /* 
             * for each term, print the 
             * <document, frequency, <position>* > tuples for a term. 
             * 
             * document:  document in which the Term appear
             * frequency: number of time the Term appear in the document
             * position:  position for each appearance in the document
             * 
             * e.g. doc.add(new Field("field", "one two three two four five", Field.Store.YES, Field.Index.ANALYZED));
             *      then the tuple for Term("field", "two") in this document would be like:
             *      88, 2, <2, 3>
             *      where 
             *      88 is the document number 
             *      2  is the frequency this term appear in the document
             *      <2, 4> are the positions for each appearance in the document
             */
            // by TermPositions
            outputLn("    document, frequency, <position>*");
            
            TermPositions termPos = mIndexReader.termPositions(term);
            while (termPos.next()) {
                int docNum = termPos.doc();
                int freq = termPos.freq();
                output("    " + docNum + ", " + freq + ", <");
                
                boolean first = true;
                for (int f = 0; f < freq; f++) {
                    int positionInDoc = termPos.nextPosition();
                    if (!first)
                        output(" ");
                    else
                        first = false;
                    output(positionInDoc + "");
                }
                outputLn(">");
            }
            termPos.close();
            
            outputLn();
        }
        
        terms.close();
    }

    private static void usage() {
        System.out.println("Usage: zmjava com.zimbra.cs.index.LuceneViewer <absolute index directory> <absolute output file path>");
        System.out.println();
        System.out.println("e.g. zmjava com.zimbra.cs.index.LuceneViewer /opt/zimbra/index/0/3/index/0 /tmp/user1-index-dump.txt");
        System.out.println();
    }
    
    public static void main(String args[]) throws Exception {
        
        if (args.length != 2) {
            usage();
            System.exit(1);
        }
        
        String indexDir = args[0];
        String outputFile = args[1];
        
        System.out.println("Dumping index directory: " + indexDir);

        LuceneViewer viewer = new LuceneViewer(indexDir, outputFile);
        viewer.dump();
        
        System.out.println("Index dump output written to file: " + outputFile);

    }
}
