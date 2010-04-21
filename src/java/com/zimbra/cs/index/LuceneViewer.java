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
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.CheckIndex.Status;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.zimbra.common.util.SetUtil;

public class LuceneViewer {
    
    String mIndexDir;
    String mOutputFile;
    TermFilters mTermFilters;
    Console mConsole;
    
    // if filters are used, intersection of doc nums that appear in all terms that matched any filter
    // // if filters are used, null.
    Set<Integer> mDocsIntersection;
    
    IndexReader mIndexReader;
    FileWriter mWriter;

    private static class TermFilters {
        
        private static class TermFilter {
            String mField;
            String mText;
            
            private TermFilter(String field, String text) {
                mField = field;
                mText = text;
            }
        }
        
        private List<TermFilter> mFilters = new ArrayList<TermFilter>();
        
        private void addFilter(String field, String text) {
            mFilters.add(new TermFilter(field, text));
        }
        
        private List<TermFilter> getFilters() {
            return mFilters;
        }
        
    }
    
    public LuceneViewer(String indexName, String outfileName, TermFilters termFilters, Console console) throws Exception {
        mIndexDir=indexName;
        mOutputFile=outfileName;
        mTermFilters = termFilters;
        mConsole = console;
        
        mIndexReader = IndexReader.open(mIndexDir);
        mWriter = new FileWriter(mOutputFile);
        
        if (hasFilters())
            mDocsIntersection = new HashSet<Integer>();
    }
    
    private List<TermFilters.TermFilter> getFilters() {
        List<TermFilters.TermFilter> filters = mTermFilters==null? null : mTermFilters.getFilters();
        return filters;
    }
    
    private boolean hasFilters() {
        List<TermFilters.TermFilter> filters = mTermFilters==null? null : mTermFilters.getFilters();
        if (filters == null || filters.isEmpty())
            return false;
        else
            return true;
    }
    
    private boolean wantThisTerm(String termField, String termText) {
        if (!hasFilters())
            return true;
        
        for (TermFilters.TermFilter termFilter : getFilters()) {
            String field = termFilter.mField;
            String text = termFilter.mText;
            boolean matched = ((field == null || field.equalsIgnoreCase(termField)) & 
                               (text == null || text.equalsIgnoreCase(termText)));
            if (matched)
                return true;
        }
        
        return false;
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
        
        outputLn("Index directory: "   + mIndexDir);
        outputLn("Output file:     "   + mOutputFile);
        dumpTermFilters();
        
        dumpFields();
        dumpDocuments();
        dumpTerms();
        
        dumpDocsIntersection();
        
        outputBanner("end");
        
        closeIndexReader();
        closeOutputWriter();
    }
    
    private void dumpTermFilters() throws IOException {
        if (!hasFilters())
            return;
        
        outputLn("Term filters:");
        for (TermFilters.TermFilter termFilter : getFilters()) {
            String field = termFilter.mField;
            String text = termFilter.mText;
            outputLn("   (field: " + (field==null?"": field) + ") (text: " + (text==null?"":text) + ")");
        }
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
        
        mConsole.debug("Total number of documents: " + totalDocs);
        for (int i = 0; i < totalDocs; i++) {
            Document doc = null;
            try {
                doc = mIndexReader.document(i, null);
            } catch (IllegalArgumentException e) {
                if ("attempt to access a deleted document".equals(e.getMessage()))
                    mConsole.warn("encountered exception while dumping document " + i + ": " + e.getMessage());
                else
                    throw e;
            }
            dumpDocument(i, doc);
            
            if ((i+1) % 100 == 0)
                mConsole.debug("Dumped " + (i+1) + " documents");
        }
    }
    
    private void dumpDocument(int docNum, Document doc) throws IOException {
        
        outputLn();
        outputLn("Document " + docNum);
        
        if (doc == null) {
            outputLn("    deleted");
            return;
        }
        
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
    
    // keep track of docs that appear in all terms that are filtered in.
    private void computeDocsIntersection(Set<Integer> docs) {
        // sanity check
        if (!hasFilters())
            return;
        
        if (mDocsIntersection.isEmpty())
            mDocsIntersection = docs;
        else
            mDocsIntersection = SetUtil.intersect(mDocsIntersection, docs);
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
            
            if (!wantThisTerm(field, text))
                continue;
            
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
             *      88, 2, <2, 4>
             *      where 
             *      88 is the document number 
             *      2  is the frequency this term appear in the document
             *      <2, 4> are the positions for each appearance in the document
             */
            // by TermPositions
            outputLn("    document, frequency, <position>*");
            
            // keep track of docs that appear in all terms that are filtered in.
            Set<Integer> docNums = null;
            if (hasFilters())
                docNums = new HashSet<Integer>();
                
            TermPositions termPos = mIndexReader.termPositions(term);
            while (termPos.next()) {
                int docNum = termPos.doc();
                int freq = termPos.freq();
                
                if (docNums != null)
                    docNums.add(docNum);
                
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
            
             if (docNums != null)
                 computeDocsIntersection(docNums);
                 
            outputLn();
            
            if (order % 1000 == 0)
                mConsole.debug("Dumped " + order + " terms");
        }
        
        terms.close();
    }
    
    private void dumpDocsIntersection() throws IOException {
        if (mDocsIntersection == null)
            return;
        
        outputBanner("Documents in which all (filtered in) terms appear");
        
        List<Integer> sorted = new ArrayList<Integer>(mDocsIntersection);
        Collections.sort(sorted);
        for (Integer docNum : sorted) {
            outputLn("    " + docNum);
        }
    }
    
    private static class CLI {
        
        public static final int NUM_TERM_FILTERS = 10;
        
        public static final String O_ACTION = "a";
        public static final String O_HELP = "h";
        public static final String O_INPUT = "i";
        public static final String O_OUTPUT = "o";
        public static final String O_VERBOSE = "v";
        
        private static final String O_TERM_FILTER_FIELD_PREFIX = "f";
        private static final String O_TERM_FILTER_TEXT_PREFIX = "t";
        
        
        private Options getAllOptions() {    
            return getOptions(true);
        }
       
        private Options getOptions(boolean includeHiddenOptions) {
            Options options = new Options();
            
            options.addOption(O_ACTION, "action", true, "action, values are dump|check");
            options.addOption(O_HELP, "help", false, "input directory");
            options.addOption(O_INPUT, "input", true, "input directory");
            options.addOption(O_OUTPUT, "output", true, "output file");
            options.addOption(O_VERBOSE, "verbose", false, "verbose mode");
            
            if (includeHiddenOptions) {
                for (Option option : (Collection<Option>)getTermFilterOptions().getOptions())
                    options.addOption(option);
            }
            
            return options;
        }
        
        static private String termFilterFieldOption(int i) {
            return O_TERM_FILTER_FIELD_PREFIX+i;
        }
        
        static private String termFilterTextOption(int i) {
            return O_TERM_FILTER_TEXT_PREFIX+i;
        }
        
        private Options getTermFilterOptions() {
            Options options = new Options();
            
            for (int i = 1; i <= NUM_TERM_FILTERS; i++) {
                options.addOption(termFilterFieldOption(i), "field"+i, true, "field name of term filter "+i);
                options.addOption(termFilterTextOption(i), "text"+i, true, "text of term filter "+i);
            }
            
            return options;
        }
        
        private boolean helpOptionSpecified(String[] args) {
            return
                args != null && args.length == 1 &&
                ("-h".equals(args[0]) || "--help".equals(args[0]));
        }
        
        private void usage(boolean exit) {
            usage(null, exit);
        }

        protected String getCommandUsage() {
            return("zmjava com.zimbra.cs.index.LuceneViewer <options> [term filter options]");
        }
        
        private void usage(ParseException e, boolean exit) {
            if (e != null) {
                // e.printStackTrace();
                System.err.println(e.getMessage());
                System.err.println();
                System.err.println();
            }
            
            PrintWriter pw = new PrintWriter(System.err, true);
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(80);
            formatter.printHelp(pw, formatter.getWidth(), getCommandUsage(), null, getOptions(false), 
                    formatter.getLeftPadding(), formatter.getDescPadding(), null);
            pw.flush();
            
            System.err.println();
            System.err.println("term filter: ");
            System.err.println("    - each f[n], t[n] pair represents a term filter, f[n], t[n] don't have to both exist");
            System.err.println("    - f[n] represents a term field name, t[n] represents a term text");
            System.err.println("    - the final filter is formed by ORing all term filters together");
            System.err.println("    - maximum of " + NUM_TERM_FILTERS + " term filters are allowed");
            
            System.err.println();
            System.err.println("    examples:");
            System.err.println("        -f1 l.content -t1 foo" + " (term l.content=foo)");
            System.err.println("        -f2 from" + "              (all terms with from as the field name)");
            System.err.println("        -t3 bar" + "               (all terms with bar as the text)");
            
            
            System.err.println();
            System.err.println();
            System.err.println("Sample command lines:");
            System.err.println("zmjava com.zimbra.cs.index.LuceneViewer -a dump -i /opt/zimbra/index/0/2/index/0 -o /tmp/user1-index-dump.txt");
            System.err.println("zmjava com.zimbra.cs.index.LuceneViewer -a dump -v -f1 l.content -t1 jay -f2 subject -t2 howdy -i /opt/zimbra/index/0/2/index/0 -o /tmp/user1-index-dump.txt");
            System.err.println("zmjava com.zimbra.cs.index.LuceneViewer -a dump -f1 from jay@test.com -i /opt/zimbra/index/0/2/index/0 -o /tmp/user1-index-dump.txt");
            
            if (exit)
                System.exit(1);
        }
        
        protected CommandLine getCommandLine(String[] args) throws ParseException {
            CommandLineParser clParser = new GnuParser();
            CommandLine cl = null;

            Options opts = getAllOptions();
            try {
                cl = clParser.parse(opts, args);
            } catch (ParseException e) {
                if (helpOptionSpecified(args))
                    usage(true);
                else
                    usage(e, true);
            }

            return cl;
        }
        
        static TermFilters getTermFilters(CommandLine cl) {
            TermFilters termFilters = new TermFilters();
            
            for (int i = 1; i <= NUM_TERM_FILTERS; i++) {
                String fieldOption = termFilterFieldOption(i);
                String textOption = termFilterTextOption(i);
                if (cl.hasOption(fieldOption) || cl.hasOption(textOption)) {
                    termFilters.addFilter(cl.hasOption(fieldOption) ? cl.getOptionValue(fieldOption) : null,
                                          cl.hasOption(textOption) ? cl.getOptionValue(textOption) : null);
                }
            }
            
            return termFilters;
        }
    }
    
    private static class Console {
        boolean mVerbose;
        
        Console(boolean verbose) {
            mVerbose = verbose;
        }
        
        private void debug(String text) {
            if (mVerbose)
                System.out.println(text);
        }
        
        private void info(String text) {
            System.out.println(text);
        }
        
        private void warn(String text) {
            System.out.println(text);
        }
    }
    
    
    private static void doCheck(CommandLine cl) throws Exception {
        Console console = new Console(cl.hasOption(CLI.O_VERBOSE));
        
        String indexDir = cl.getOptionValue(CLI.O_INPUT);
        console.info("Checking index " + indexDir);
        
        Directory dir = null;
        try {
            dir = FSDirectory.getDirectory(indexDir);
        } catch (Throwable t) {
            console.info("ERROR: could not open directory \"" + indexDir + "\"; exiting");
            t.printStackTrace(System.out);
            System.exit(1);
        }

        CheckIndex checker = new CheckIndex(dir);
        checker.setInfoStream(System.out);
        
        Status result = checker.checkIndex();
        console.info("Result:" + (result.clean ? "clean" : "not clean"));
    }
    
    private static void doDump(CommandLine cl) throws Exception {
        Console console = new Console(cl.hasOption(CLI.O_VERBOSE));
        
        String indexDir = cl.getOptionValue(CLI.O_INPUT);
        String outputFile = cl.getOptionValue(CLI.O_OUTPUT);
        
        TermFilters termFilters = CLI.getTermFilters(cl);
        
        console.info("Dumping index directory: " + indexDir);
        console.info("Output file: " + outputFile);
        
        LuceneViewer viewer = new LuceneViewer(indexDir, outputFile, termFilters, console);
        
        viewer.dump();
        
        console.info("all done");
    }
    
    public static void main(String args[]) throws Exception {
        
        CLI cli = new CLI();
        CommandLine cl = cli.getCommandLine(args);
        
        if (!cl.hasOption(CLI.O_ACTION))
            cli.usage(new ParseException("missing required option " + CLI.O_ACTION), true);
        
        String action = cl.getOptionValue(CLI.O_ACTION);
        if ("dump".equals(action))
            doDump(cl);
        else if ("check".equals(action)) {
            doCheck(cl);
        } else {
            cli.usage(new ParseException("invalid option " + action), true);
        }
        
    }
}
