package com.zimbra.soap.mail.message;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zimbra.soap.mail.type.CreateItemNotification;
import com.zimbra.soap.mail.type.DeleteItemNotification;
import com.zimbra.soap.mail.type.ImapMessageInfo;
import com.zimbra.soap.mail.type.ModifyNotification;
import com.zimbra.soap.mail.type.PendingFolderModifications;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.IdAndType;

import junit.framework.Assert;

public class WaitSetResponseTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
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
		WaitSetResponse waitSetResponse = new WaitSetResponse("1234");
		waitSetResponse.setCanceled(false);
		waitSetResponse.setSeqNo("56789");
		waitSetResponse.addSignalledAccount(accountWithModifications);
		waitSetResponse.addError(new IdAndType("1", " Atype"));
		String json = mapper.writeValueAsString(waitSetResponse);
		System.out.println(json);
		String expected = "{\"waitSetId\":\"1234\",\"canceled\":false,\"seqNo\":\"56789\",\"signalledAccounts\":[{\"id\":\"1001\",\"lastChangeId\":5,\"pendingFolderModifications\":[{\"folderId\":10,\"created\":[{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}],\"deleted\":[{\"id\":1,\"type\":\" Atype\"}],\"modifiedMsgs\":[{\"changeBitmask\":103,\"messageInfo\":{\"id\":11,\"imapUid\":22,\"type\":\"typeItem\",\"flags\":11,\"tags\":\"tagsItem\"}}],\"modifiedTags\":[{\"changeBitmask\":102,\"id\":1,\"name\":\"aNameTag\"}],\"renamedFolders\":[{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}]}]}],\"errors\":[{\"id\":\"1\",\"type\":\" Atype\"}]}";
		Assert.assertEquals(expected, json);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		WaitSetResponse waitSetResponse = null;
		String expected = "{\"waitSetId\":\"1234\",\"canceled\":false,\"seqNo\":\"56789\",\"signalledAccounts\":[{\"id\":\"1001\",\"lastChangeId\":5,\"pendingFolderModifications\":[{\"folderId\":10,\"created\":[{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}],\"deleted\":[{\"id\":1,\"type\":\" Atype\"}],\"modifiedMsgs\":[{\"changeBitmask\":103,\"messageInfo\":{\"id\":11,\"imapUid\":22,\"type\":\"typeItem\",\"flags\":11,\"tags\":\"tagsItem\"}}],\"modifiedTags\":[{\"changeBitmask\":102,\"id\":1,\"name\":\"aNameTag\"}],\"renamedFolders\":[{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}]}]}],\"errors\":[{\"id\":\"1\",\"type\":\" Atype\"}]}";
		waitSetResponse = mapper.readValue(expected, WaitSetResponse.class);
		System.out.println(waitSetResponse);
		Assert.assertNotNull(waitSetResponse);
	}

}
