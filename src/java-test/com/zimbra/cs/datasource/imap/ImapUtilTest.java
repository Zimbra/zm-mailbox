package com.zimbra.cs.datasource.imap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.mailclient.imap.ListData;

public class ImapUtilTest {

    private List<ListData> folders = new ArrayList<ListData>();

    @Before
    public void preTest() {
        folders.clear();
        folders.add(new ListData("INBOX", '/'));
        folders.add(new ListData("Inbox", '/'));    //dup
        folders.add(new ListData("Inbox/sub", '/'));
        folders.add(new ListData("Personal", '/'));
        folders.add(new ListData("Receipts", '/'));
        folders.add(new ListData("Travel", '/'));
        folders.add(new ListData("Work", '/'));
        folders.add(new ListData("[Gmail]", '/'));
        folders.add(new ListData("[Gmail]/All Mail", '/'));
        folders.add(new ListData("[Gmail]/Drafts", '/'));
        folders.add(new ListData("[Gmail]/Sent Mail", '/'));
        folders.add(new ListData("[Gmail]/Spam", '/'));
        folders.add(new ListData("[Gmail]/Starred", '/'));
        folders.add(new ListData("[Gmail]/Trash", '/'));
        folders.add(new ListData("Inbox/a", '/'));
        folders.add(new ListData("Inbox/b", '/'));
        folders.add(new ListData("Inbox/c", '/'));
        folders.add(new ListData("travel", '/'));   //different from Travel
    }

    @Test
    public void testSortFolders() {
        List<ListData> result = ImapUtil.sortFolders(this.folders);
        Assert.assertEquals(result.size(), this.folders.size()-1); //dup of "INBOX"
        this.folders.add(new ListData("INbox/t/a", '/'));
        this.folders.add(new ListData("INBox/t/a", '/'));
        result = ImapUtil.sortFolders(this.folders);
        Assert.assertEquals(result.size(), this.folders.size()-1);

        folders.add(new ListData("INBox/T/a", '/'));
        folders.add(new ListData("INBox/T/A", '/'));
        result = ImapUtil.sortFolders(this.folders);
        Assert.assertEquals(result.size(), this.folders.size()-1);
    }

    @Test
    public void testComparator() {
        List<ListData> result = ImapUtil.sortFolders(this.folders);
        Assert.assertEquals("Inbox/sub", result.get(1).getMailbox());
        Assert.assertEquals("Inbox/a", result.get(4).getMailbox());
    }

    @Test
    public void testIsInboxInferior() throws Exception {
        ListData ld = new ListData("inbox/aa", '/');
        Method testMethod = ImapUtil.class.getDeclaredMethod("isInboxInferior", ListData.class);
        testMethod.setAccessible(true);
        Assert.assertEquals(true, testMethod.invoke(null, ld));
        ld = new ListData("inbox", '/');
        Assert.assertEquals(false, testMethod.invoke(null, ld));
        ld = new ListData("abc", '/');
        Assert.assertEquals(false, testMethod.invoke(null, ld));
    }
}
