package com.zimbra.client;

import java.util.LinkedList;
import java.util.List;

import com.zimbra.soap.account.message.ImapMessageInfo;
import com.zimbra.soap.account.message.OpenImapFolderResponse;

public class ZImapFolderInfo {

    private boolean hasMore;
    private List<ImapMessageInfo> messageInfo = new LinkedList<ImapMessageInfo>();

    public ZImapFolderInfo(OpenImapFolderResponse resp) {
        hasMore = resp.getHasMore();
        messageInfo = resp.getImapMessageInfo();
    }

    public boolean hasMore() {
        return hasMore;
    }

    public List<ImapMessageInfo> getMessageInfo() {
        return messageInfo;
    }
}
