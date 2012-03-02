/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.imap.AbstractListCommand;
import com.zimbra.cs.imap.AppendCommand;
import com.zimbra.cs.imap.AppendMessage;
import com.zimbra.cs.imap.CopyCommand;
import com.zimbra.cs.imap.ExamineCommand;
import com.zimbra.cs.imap.FetchCommand;
import com.zimbra.cs.imap.ImapCommand;
import com.zimbra.cs.imap.ImapCommandThrottle;
import com.zimbra.cs.imap.ImapHandler;
import com.zimbra.cs.imap.ImapPartSpecifier;
import com.zimbra.cs.imap.ImapPath;
import com.zimbra.cs.imap.ListCommand;
import com.zimbra.cs.imap.Literal;
import com.zimbra.cs.imap.QResyncInfo;
import com.zimbra.cs.imap.SearchCommand;
import com.zimbra.cs.imap.SelectCommand;
import com.zimbra.cs.imap.AppendMessage.Part;
import com.zimbra.cs.imap.ImapHandler.StoreAction;
import com.zimbra.cs.imap.ImapSearch.SequenceSearch;
import com.zimbra.cs.imap.ImapSearch.FlagSearch;
import com.zimbra.cs.imap.ImapSearch.AndOperation;
import com.zimbra.cs.imap.ImapSearch.OrOperation;
import com.zimbra.cs.imap.ImapSearch.AllSearch;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class ImapCommandThrottleTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void repeatCommand() {
        int limit = 25;
        ImapCommandThrottle throttle = new ImapCommandThrottle(limit);

        for (int i = 0; i < limit; i++) {
            MockImapCommand command = new MockImapCommand("p1", "p3", 123);
            Assert.assertFalse(throttle.isCommandThrottled(command));
        }
        MockImapCommand command = new MockImapCommand("p1", "p3", 123);
        Assert.assertTrue(throttle.isCommandThrottled(command));
    }

    @Test
    public void repeatUnderLimit() {
        int limit = 55;
        ImapCommandThrottle throttle = new ImapCommandThrottle(limit);

        for (int i = 0; i < limit; i++) {
            MockImapCommand command = new MockImapCommand("p1", "p3", 123);
            Assert.assertFalse(throttle.isCommandThrottled(command));
        }
        MockImapCommand command = new MockImapCommand("p2", "p3", 1234);
        Assert.assertFalse(throttle.isCommandThrottled(command));
        command = new MockImapCommand("p1", "p3", 123);
        Assert.assertFalse(throttle.isCommandThrottled(command));
    }

    private QResyncInfo makeQri() {
        QResyncInfo qri = new QResyncInfo();
        qri.setKnownUIDs("knownUIDs");
        qri.setModseq(1);
        qri.setSeqMilestones("seqMilestones");
        qri.setUidMilestones("uidMilestones");
        qri.setUvv(123456);
        return qri;
    }

    @Test
    public void select() {
        String pathName = "testfolder";

        SelectCommand select = new SelectCommand(new ImapPath(pathName, null), (byte) 123, makeQri());

        Assert.assertTrue("same obj", select.isDuplicate(select));

        SelectCommand select2 = new SelectCommand(new ImapPath(pathName, null), (byte) 123, makeQri());
        Assert.assertTrue("diff obj same fields", select.isDuplicate(select2));

        SelectCommand select3 = new SelectCommand(new ImapPath(pathName + "foo", null), (byte) 123, makeQri());
        Assert.assertFalse("different path", select.isDuplicate(select3));

        SelectCommand select4 = new SelectCommand(new ImapPath(pathName, null), (byte) 101, makeQri());
        Assert.assertFalse("different params", select.isDuplicate(select4));

        QResyncInfo qri = makeQri();
        qri.setKnownUIDs("foo");
        SelectCommand select5 = new SelectCommand(new ImapPath(pathName, null), (byte) 123, qri);
        Assert.assertFalse("different qri", select.isDuplicate(select5));
    }

    @Test
    public void examine() {
        String pathName = "testfolder";

        ExamineCommand examine = new ExamineCommand(new ImapPath(pathName, null), (byte) 123, makeQri());

        Assert.assertTrue("same obj", examine.isDuplicate(examine));

        SelectCommand select = new SelectCommand(new ImapPath(pathName, null), (byte) 123, makeQri());
        Assert.assertFalse("select vs examine", examine.isDuplicate(select));

        ExamineCommand examine2 = new ExamineCommand(new ImapPath(pathName, null), (byte) 123, makeQri());
        Assert.assertTrue("diff obj same fields", examine.isDuplicate(examine2));

        ExamineCommand examine3 = new ExamineCommand(new ImapPath(pathName + "foo", null), (byte) 123, makeQri());
        Assert.assertFalse("different path", examine.isDuplicate(examine3));

        ExamineCommand examine4 = new ExamineCommand(new ImapPath(pathName, null), (byte) 101, makeQri());
        Assert.assertFalse("different params", examine.isDuplicate(examine4));

        QResyncInfo qri = makeQri();
        qri.setKnownUIDs("foo");
        ExamineCommand examine5 = new ExamineCommand(new ImapPath(pathName, null), (byte) 123, qri);
        Assert.assertFalse("different qri", examine.isDuplicate(examine5));
    }

    private List<ImapPartSpecifier> makeParts() {
        List<ImapPartSpecifier> parts = new ArrayList<ImapPartSpecifier>();
        parts.add(new ImapPartSpecifier("cmd1", "part1", "modifier1"));
        ImapPartSpecifier headerSpec = new ImapPartSpecifier("cmd2", null, null);
        List<String> headers = new ArrayList<String>();
        headers.add("h1");
        headers.add("h2");
        headerSpec.setHeaders(headers);
        parts.add(headerSpec);
        return parts;
    }

    @Test
    public void fetch() {
        String sequence = "1:*";
        int attributes = 123;
        List<ImapPartSpecifier> parts = makeParts();
        FetchCommand fetch = new FetchCommand(sequence, attributes, parts);

        Assert.assertTrue("same obj", fetch.isDuplicate(fetch));

        FetchCommand fetch2 = new FetchCommand(sequence, attributes, parts);
        Assert.assertTrue("same args, different obj", fetch.isDuplicate(fetch2));

        FetchCommand fetch3 = new FetchCommand(sequence + "foo", attributes, parts);
        Assert.assertFalse("different sequence", fetch.isDuplicate(fetch3));

        FetchCommand fetch4 = new FetchCommand(sequence, attributes + 1, parts);
        Assert.assertFalse("different attributes", fetch.isDuplicate(fetch4));

        FetchCommand fetch5 = new FetchCommand(sequence, attributes, null);
        Assert.assertFalse("null parts", fetch.isDuplicate(fetch5));

        List<ImapPartSpecifier> p2 = makeParts();
        p2.add(new ImapPartSpecifier("cmd3", "part1", "modifier1"));
        FetchCommand fetch6 = new FetchCommand(sequence, attributes, p2);
        Assert.assertFalse("different length parts", fetch.isDuplicate(fetch6));

        List<ImapPartSpecifier> p3 = makeParts();
        p3.add(p3.remove(0));
        FetchCommand fetch7 = new FetchCommand(sequence, attributes, p3);
        Assert.assertTrue("same parts; different order - should be a duplicate", fetch.isDuplicate(fetch7));

        List<ImapPartSpecifier> p4 = makeParts();
        ImapPartSpecifier headerPart = p4.get(1);
        List<String> headers = new ArrayList<String>();
        headers.add("h1");
        headers.add("h3");
        headerPart.setHeaders(headers);
        FetchCommand fetch8 = new FetchCommand(sequence, attributes, p4);
        Assert.assertFalse("same lengths, different headers", fetch.isDuplicate(fetch8));

        List<ImapPartSpecifier> p5 = makeParts();
        p5.remove(0);
        ImapPartSpecifier newPart = new ImapPartSpecifier("cmd2", "part1", "modifier1");
        p5.add(newPart);
        FetchCommand fetch9 = new FetchCommand(sequence, attributes, p5);
        Assert.assertFalse("different part.command, same length", fetch.isDuplicate(fetch9));

        List<ImapPartSpecifier> p6 = makeParts();
        p6.remove(0);
        newPart = new ImapPartSpecifier("cmd1", "part2", "modifier1");
        p6.add(newPart);
        FetchCommand fetch10 = new FetchCommand(sequence, attributes, p6);
        Assert.assertFalse("different part.part, same length", fetch.isDuplicate(fetch10));

        List<ImapPartSpecifier> p7 = makeParts();
        p7.remove(0);
        newPart = new ImapPartSpecifier("cmd1", "part1", "modifier2");
        p7.add(newPart);
        FetchCommand fetch11 = new FetchCommand(sequence, attributes, p7);
        Assert.assertFalse("different part.modifier, same length", fetch.isDuplicate(fetch11));
    }

    @Test
    public void fetchBug68556() {
        ImapPartSpecifier part = new ImapPartSpecifier("BODY", "", "HEADER.FIELDS");
        List<String> headers = new ArrayList<String>();
        headers.add("CONTENT-CLASS");
        part.setHeaders(headers);
        Assert.assertTrue("Exchange header detected", part.isIgnoredExchangeHeader());

        List<ImapPartSpecifier> parts = new ArrayList<ImapPartSpecifier>();
        parts.add(part);

        ImapCommand command = new FetchCommand("1:123", ImapHandler.FETCH_FROM_CACHE, parts);

        Assert.assertFalse("Fetch not throttled, just truncated parts", command.throttle(null));

        Assert.assertTrue("CONTENT-CLASS removed", parts.isEmpty());
    }

    @Test
    public void copy() {
        String destFolder = "destFolder";
        String sequenceSet = "10:20";

        CopyCommand copy = new CopyCommand(sequenceSet, new ImapPath(destFolder, null));

        Assert.assertTrue("same obj", copy.isDuplicate(copy));

        CopyCommand copy2 = new CopyCommand(sequenceSet, new ImapPath(destFolder, null));
        Assert.assertTrue("diff obj same fields", copy.isDuplicate(copy2));

        CopyCommand copy3 = new CopyCommand(sequenceSet, new ImapPath(destFolder + "foo", null));
        Assert.assertFalse("diff dest path", copy.isDuplicate(copy3));

        CopyCommand copy4 = new CopyCommand("20:30", new ImapPath(destFolder + "foo", null));
        Assert.assertFalse("diff dest path", copy.isDuplicate(copy4));
    }

    private Part makeAppendPart(AppendMessage append, int size, byte fillByte) throws IOException {
        Literal literal = Literal.newInstance(size);
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, fillByte);
        literal.put(bytes, 0, bytes.length);
        return append.new Part(literal);
    }

    private List<AppendMessage> makeAppends() throws IOException {
        List<AppendMessage> list = new ArrayList<AppendMessage>();
        List<String> flagNames = new ArrayList<String>();
        flagNames.add("F1");
        flagNames.add("F2");
        Date date = new Date(1234567890);
        List<Part> parts = new ArrayList<Part>();

        AppendMessage append = new AppendMessage(flagNames, date, parts);
        parts.add(makeAppendPart(append, 123, (byte) 99));

        List<String> flagNames2 = new ArrayList<String>();
        flagNames.add("F3");
        flagNames.add("F4");
        Date date2 = new Date(222222222);
        List<Part> parts2 = new ArrayList<Part>();
        AppendMessage append2 = new AppendMessage(flagNames2, date2, parts2);
        parts2.add(makeAppendPart(append2, 555, (byte) 55));
        parts2.add(makeAppendPart(append2, 444, (byte) 44));

        list.add(append);
        list.add(append2);
        return list;
    }

    @Test
    public void append() throws IOException {
        String path = "testPath";
        AppendCommand append = new AppendCommand(new ImapPath(path, null), makeAppends());

        Assert.assertTrue("same obj", append.isDuplicate(append));

        AppendCommand append2 = new AppendCommand(new ImapPath(path, null), makeAppends());
        Assert.assertTrue("diff obj same params", append.isDuplicate(append2));

        AppendCommand append3 = new AppendCommand(new ImapPath(path + "foo", null), makeAppends());
        Assert.assertFalse("different path", append.isDuplicate(append3));

        List<AppendMessage> appends = makeAppends();
        appends.remove(0);
        AppendCommand append4 = new AppendCommand(new ImapPath(path, null), appends);
        Assert.assertFalse("different length appends", append.isDuplicate(append4));

        appends = makeAppends();
        AppendMessage appendMsg = appends.remove(0);
        List<Part> parts = new ArrayList<Part>();
        AppendMessage appendMsg2 = new AppendMessage(appendMsg.getPersistentFlagNames(), appendMsg.getDate(), parts);
        parts.add(makeAppendPart(appendMsg2, 215, (byte) 215));

        appends.add(0, appendMsg2);
        AppendCommand append5 = new AppendCommand(new ImapPath(path, null), appends);
        Assert.assertFalse("different append parts", append.isDuplicate(append5));

        parts = new ArrayList<Part>();
        appendMsg2 = new AppendMessage(appendMsg.getPersistentFlagNames(), new Date(), parts);
        parts.add(makeAppendPart(appendMsg2, 123, (byte) 99));
        appends.remove(0);
        appends.add(0, appendMsg2);
        AppendCommand append6 = new AppendCommand(new ImapPath(path, null), appends);
        Assert.assertFalse("different date", append.isDuplicate(append6));

        parts = new ArrayList<Part>();
        List<String> flagNames = new ArrayList<String>();
        flagNames.add("F1");
        flagNames.add("F3");
        appendMsg2 = new AppendMessage(flagNames, appendMsg.getDate(), parts);
        parts.add(makeAppendPart(appendMsg2, 123, (byte) 99));
        appends.remove(0);
        appends.add(0, appendMsg2);
        AppendCommand append7 = new AppendCommand(new ImapPath(path, null), appends);
        Assert.assertFalse("different flag names", append.isDuplicate(append7));
    }

    @Test
    public void list() {
        String refName = "refName";
        Set<String> mailboxNames = new HashSet<String>(Arrays.asList(new String[] { "mbox1", "mbox2", "mbox3" }));
        byte selectOptions = (byte) 24;
        byte returnOptions = (byte) 38;
        byte status = (byte) 67;
        AbstractListCommand list = new ListCommand(refName, mailboxNames, selectOptions, returnOptions, status);

        Assert.assertTrue("same obj", list.isDuplicate(list));

        AbstractListCommand list2 = new ListCommand(refName, mailboxNames, selectOptions, returnOptions, status);
        Assert.assertTrue("same fields", list.isDuplicate(list2));

        list2 = new ListCommand(refName + "foo", mailboxNames, selectOptions, returnOptions, status);
        Assert.assertFalse("different ref name", list.isDuplicate(list2));

        list2 = new ListCommand(refName, new HashSet<String>(Arrays.asList(new String[] { "mbox1", "mbox2" })),
                selectOptions, returnOptions, status);
        Assert.assertFalse("different mailbox names", list.isDuplicate(list2));

        list2 = new ListCommand(refName, mailboxNames, (byte) 99, returnOptions, status);
        Assert.assertFalse("different selectOptions", list.isDuplicate(list2));

        list2 = new ListCommand(refName, mailboxNames, selectOptions, (byte) 99, status);
        Assert.assertFalse("different returnOptions", list.isDuplicate(list2));

        list2 = new ListCommand(refName, mailboxNames, selectOptions, returnOptions, (byte) 99);
        Assert.assertFalse("different status", list.isDuplicate(list2));
    }

    private ImapSearch makeSearch(String flagName) {
        ImapSearch sequenceSearch = new SequenceSearch("tag", "subseq", true);
        ImapSearch flagSearch = new FlagSearch(flagName);
        ImapSearch andSearch = new AndOperation(sequenceSearch, flagSearch);

        ImapSearch allSearch = new AllSearch();
        ImapSearch orSearch = new OrOperation(andSearch, allSearch);
        return orSearch;
    }

    @Test
    public void search() {
        String flagName = "flagName";

        SearchCommand search = new SearchCommand(makeSearch(flagName), 123);

        Assert.assertTrue("same obj", search.isDuplicate(search));

        SearchCommand search2 = new SearchCommand(makeSearch(flagName), 123);
        Assert.assertTrue("same fields", search.isDuplicate(search2));

        search2 = new SearchCommand(makeSearch(flagName), 456);
        Assert.assertFalse("different options", search.isDuplicate(search2));

        search2 = new SearchCommand(makeSearch(flagName + "foo"), 456);
        Assert.assertFalse("different search params", search.isDuplicate(search2));
    }

    @Test
    public void sort() {
        String flagName = "flagName";

        SortCommand sort = new SortCommand(makeSearch(flagName), 123);

        Assert.assertTrue("same obj", sort.isDuplicate(sort));

        SortCommand sort2 = new SortCommand(makeSearch(flagName), 123);
        Assert.assertTrue("same fields", sort.isDuplicate(sort2));

        sort2 = new SortCommand(makeSearch(flagName), 456);
        Assert.assertFalse("different options", sort.isDuplicate(sort2));

        sort2 = new SortCommand(makeSearch(flagName + "foo"), 456);
        Assert.assertFalse("different search params", sort.isDuplicate(sort2));

        SearchCommand search = new SearchCommand(makeSearch(flagName), 123);
        Assert.assertFalse("different class (search vs sort)", sort.isDuplicate(search));
    }

    @Test
    public void create() {
        String pathName = "folder123";
        CreateCommand create = new CreateCommand(new ImapPath(pathName, null));

        Assert.assertTrue("same obj", create.isDuplicate(create));

        CreateCommand create2 = new CreateCommand(new ImapPath(pathName, null));
        Assert.assertTrue("same fields", create.isDuplicate(create2));

        create2 = new CreateCommand(new ImapPath("foo", null));
        Assert.assertFalse("different path", create.isDuplicate(create2));

        for (int repeats = 0; repeats < LC.imap_throttle_command_limit.intValue(); repeats++) {
            create2 = new CreateCommand(new ImapPath("foo"+repeats, null));
            Assert.assertFalse(create2.throttle(create));
            create = create2;
        }
        Assert.assertTrue(create2.throttle(create));
    }

    private List<String> makeFlagNames() {
        List<String> list = new ArrayList<String>();
        list.add("F1");
        list.add("F2");
        list.add("F3");
        return list;
    }

    @Test
    public void store() {
        String seqSet = "1:200";
        StoreCommand store = new StoreCommand(seqSet, makeFlagNames(), StoreAction.ADD, 0);

        Assert.assertTrue("same obj", store.isDuplicate(store));

        StoreCommand store2 = new StoreCommand(seqSet, makeFlagNames(), StoreAction.ADD, 0);
        Assert.assertTrue("same fields", store.isDuplicate(store2));

        store2 = new StoreCommand("1:400", makeFlagNames(), StoreAction.ADD, 0);
        Assert.assertFalse("different sequence", store.isDuplicate(store2));

        List<String> flagNames = makeFlagNames();
        flagNames.remove(0);
        store2 = new StoreCommand(seqSet, flagNames, StoreAction.ADD, 0);
        Assert.assertFalse("different flag names", store.isDuplicate(store2));

        store2 = new StoreCommand(seqSet, makeFlagNames(), StoreAction.REMOVE, 0);
        Assert.assertFalse("different action", store.isDuplicate(store2));

        store2 = new StoreCommand(seqSet, makeFlagNames(), StoreAction.ADD, 999);
        Assert.assertFalse("different mod seq", store.isDuplicate(store2));
    }
}
