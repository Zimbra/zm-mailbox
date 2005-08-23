/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on 2004. 7. 13.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.test.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/**
 * @author jhahm
 *
 * A test program that shows Lucene index reader and writer are unaware of
 * in-memory changes of each other inside the same JavaVM.  Index changes
 * by the writer are made visible to the reader only via disk file.
 *
 * Try this sequence of commands:
 *
 * Ready> index hello world
 *        (indexes a document with content "hello world")
 * Ready> list
 *        (shows all documents in the index)
 *        (no hits returned)
 * Ready> search hello
 *        (searches for documents containing "hello")
 *        (no hits returned)
 *
 * Ready> reopen
 *        (reopens index reader/searcher)
 * Ready> list
 *        (no hits returned)
 * Ready> search hello
 *        (no hits returned)
 *
 * Ready> flush
 *        (flushes pending writer changes to disk)
 * Ready> list
 *        (no hits returned)
 * Ready> search hello
 *        (no hits returned)
 *
 * Ready> reopen
 * Ready> list
 *        (hits returned)
 * Ready> search hello
 *        (hits returned)
 *
 * The conclusion is that new documents added to the index become searchable
 * only after flushing the writer changes to disk and reopening the
 * reader/searcher to sync with the latest data on disk.
 */
public class LuceneReadWriteTest {

	private static final String EMPTYSTR = "";

	public static void main(String[] args) {
		try {
			LuceneReadWriteTest tester = new LuceneReadWriteTest();
			tester.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private IndexReaderWriter mIndexRW;
	private BufferedReader mIn;

	public LuceneReadWriteTest() throws IOException {
		mIndexRW = new IndexReaderWriter();
		mIn = new BufferedReader(new InputStreamReader(System.in));
	}

	public void run() throws IOException {
		while (true) {
			String[] cmdline = getCommand();
			String cmd = cmdline[0];
			String arg = cmdline[1];
			if (cmd.compareToIgnoreCase("exit") == 0 ||
				cmd.compareToIgnoreCase("quit") == 0)
				break;
			else
				runCommand(cmd, arg);
		}
		mIndexRW.cleanup();
	}

	private String[] getCommand() throws IOException {
		System.out.print("Ready> ");

		String[] ret = new String[2];
		ret[0] = ret[1] = EMPTYSTR;

		String line = mIn.readLine();
		int ind = line.indexOf(' ');
		if (ind == -1) {
			ret[0] = line;
		} else {
			ret[0] = line.substring(0, ind);
			if (line.length() > ind + 1)
				ret[1] = line.substring(ind + 1);
		}

		return ret;
	}

	private void runCommand(String command, String arg) throws IOException {
		if (command.compareToIgnoreCase("index") == 0) {
			if (arg.length() > 0) {
				mIndexRW.index(arg);
				mIndexRW.status();
			} else
				printHelp();
		} else if (command.compareToIgnoreCase("search") == 0) {
			if (arg.length() > 0) {
				mIndexRW.search(arg);
				mIndexRW.status();
			} else
				printHelp();
		} else if (command.compareToIgnoreCase("list") == 0) {
			mIndexRW.list();
			mIndexRW.status();
		} else if (command.compareToIgnoreCase("flush") == 0) {
			mIndexRW.flushWriter();
			mIndexRW.status();
		} else if (command.compareToIgnoreCase("reopen") == 0) {
			mIndexRW.reopenSearcher();
			mIndexRW.status();
		} else if (command.compareToIgnoreCase("status") == 0)
			mIndexRW.status();
		else
			printHelp();
	}

	private void printHelp() {
		System.out.println("index <text>: Index a line of text.");
		System.out.println("search <word>: Search for a word.");
		System.out.println("list: Show all documents in the index.");
		System.out.println("flush: Flush pending IndexWriter changes to disk.");
		System.out.println("reopen: Reopen Searcher to sync with index on disk.");
		System.out.println("status: Report number of pending changes and index versions.");
		System.out.println("exit/quit: Exit this program.");
	}
}

class IndexReaderWriter {

	private static final String INDEX_PATH = "/tmp/testindex";
	private static final String FIELD_NAME = "content";
	private static final String TAG_FIELD = "tag";
	private static final String TAG_VALUE = "all";
    private static final long UNKNOWN_VERSION = -1;

	private IndexWriter mWriter;
	private Searcher mSearcher;
	private long mVersion;
	private int mNumPendingChanges;

	public IndexReaderWriter() throws IOException {
		mVersion = UNKNOWN_VERSION;
		openWriter();
		openSearcher();
	}

	public void cleanup() throws IOException {
		mSearcher.close();
		mSearcher = null;
		mWriter.close();
		mWriter = null;
	}
	
	private void openWriter() throws IOException {
		try {
			mWriter = new IndexWriter(INDEX_PATH, new StandardAnalyzer(), false);
        } catch (IOException e1) {
        	System.out.println("Creating new index in " + INDEX_PATH);
        	createIndex();
			mWriter = new IndexWriter(INDEX_PATH, new StandardAnalyzer(), false);
        }

        //mWriter.mergeFactor = 10; // this really means "# of segments - 1"
        //mWriter.minMergeDocs = 100; // min # documents in memory before we write to disk

        mNumPendingChanges = 0;
	}

	private void openSearcher() throws IOException {
		mVersion = IndexReader.getCurrentVersion(INDEX_PATH);
		mSearcher = new IndexSearcher(INDEX_PATH);
	}

	private void createIndex() throws IOException {
		File indexPath = new File(INDEX_PATH);
		indexPath.mkdir();
		IndexWriter writer = new IndexWriter(indexPath, new StandardAnalyzer(), true);
		writer.close();
	}
	
	public void flushWriter() throws IOException {
		mWriter.close();
		openWriter();
		System.out.println(Integer.toString(mNumPendingChanges) +
						   " pending IndexWriter changes flushed to disk.");
		mNumPendingChanges = 0;
	}

	public void index(String text) throws IOException {
		Document doc = new Document();
		doc.add(Field.Text(FIELD_NAME, text));
		doc.add(Field.Keyword(TAG_FIELD, TAG_VALUE));
		mWriter.addDocument(doc);
		mNumPendingChanges++;
		System.out.println("Document indexed");
	}

	public void reopenSearcher() throws IOException {
		long oldVer = mVersion;
		long ver = IndexReader.getCurrentVersion(INDEX_PATH);
		mSearcher.close();
		mSearcher = new IndexSearcher(INDEX_PATH);
		mVersion = ver;
		System.out.println("Searcher reopened");
		System.out.println("Old index version was " + oldVer);
		System.out.println("Current index version is " + mVersion);
	}
	
	public void search(String word) throws IOException {
		searchBy(word, FIELD_NAME);
	}

	public void list() throws IOException {
		searchBy(TAG_VALUE, TAG_FIELD);
	}

	private void searchBy(String word, String fieldName) throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		Query query = null;
		try {
			query = QueryParser.parse(word, fieldName, analyzer);
		} catch (ParseException e) {
			System.err.println("Bad query!");
			e.printStackTrace();
		}
		Hits hits = mSearcher.search(query);
		int numHits = hits.length();

		if (numHits > 0) {
			for (int i = 0; i < numHits; i++) {
				Document doc = hits.doc(i);
				int id = hits.id(i);
				Field f = doc.getField(FIELD_NAME);
				if (f != null) {
					String text = f.stringValue();
					System.out.println("Doc id " + id + ": " + text);
				} else {
					System.out.println("Doc id " + id + ": (missing text)");
				}
			}
		} else
			System.out.println("No hits found");
	}

	public void status() throws IOException {
		long ver = IndexReader.getCurrentVersion(INDEX_PATH);
		System.out.print("[");
		System.out.print("Pending changes: " + mNumPendingChanges);
		System.out.print(", ");
		System.out.print("Searcher version: " + mVersion);
		System.out.print(", ");
		System.out.print("Disk version: " + ver);
		System.out.println("]");
	}
}
