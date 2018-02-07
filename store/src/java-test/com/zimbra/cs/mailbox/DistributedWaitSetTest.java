package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.soap.base.WaitSetResp;
import com.zimbra.soap.mail.message.WaitSetResponse;
import com.zimbra.soap.mail.type.CreateItemNotification;
import com.zimbra.soap.mail.type.DeleteItemNotification;
import com.zimbra.soap.mail.type.ImapMessageInfo;
import com.zimbra.soap.mail.type.ModifyNotification;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.IdAndType;

public class DistributedWaitSetTest {

	@Test
	public void publishTest() throws Exception {
		final DistributedWaitSet dws = DistributedWaitSet.getInstance();
		MessageListener<WaitSetResp> msgListener = new MessageListener<WaitSetResp>() {
			@Override
			public void onMessage(String arg0, WaitSetResp arg1) {
				System.out.println("channel:"+arg0 +" message:"+arg1);
			}
		};
		dws.subscribe(MockProvisioning.DEFAULT_ACCOUNT_ID, msgListener);
		long delivered = dws.publish(MockProvisioning.DEFAULT_ACCOUNT_ID, getWaitSetResp());
		System.out.println("delivered:"+delivered );
	}
	
	private WaitSetResp getWaitSetResp() {
		Collection<PendingFolderModifications> mods = new ArrayList<>();
		PendingFolderModifications pendingFolderModifications = new PendingFolderModifications(10);
		pendingFolderModifications
				.addCreatedItem(new CreateItemNotification(new ImapMessageInfo(1, 2, "type", 1, "tags")));
		pendingFolderModifications.addDeletedItem(new DeleteItemNotification(1, " Atype"));
		pendingFolderModifications.addModifiedMsg(new ModifyNotification.ModifyItemNotification(
				new ImapMessageInfo(11, 22, "typeItem", 11, "tagsItem"), 103));
		pendingFolderModifications.addModifiedTag(new ModifyNotification.ModifyTagNotification(1, "aNameTag", 102));
		pendingFolderModifications.addRenamedFolder(new ModifyNotification.RenameFolderNotification(12, "path", 104));
		mods.add(pendingFolderModifications);
		AccountWithModifications accountWithModifications = new AccountWithModifications(1001, mods, 5);
		WaitSetResp waitSetResponse = new WaitSetResponse("1234");
		waitSetResponse.setCanceled(false);
		waitSetResponse.setSeqNo("56789");
		waitSetResponse.addSignalledAccount(accountWithModifications);
		waitSetResponse.addError(new IdAndType("1", " Atype"));
		return waitSetResponse;
	}
}
