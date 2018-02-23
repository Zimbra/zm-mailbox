package com.zimbra.soap.type;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.zimbra.soap.mail.type.CreateItemNotification;
import com.zimbra.soap.mail.type.DeleteItemNotification;
import com.zimbra.soap.mail.type.ImapMessageInfo;
import com.zimbra.soap.mail.type.ModifyNotification;
import com.zimbra.soap.mail.type.PendingFolderModifications;

import junit.framework.Assert;

public class AccountWithModificationsTest {

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
		String json = mapper.writeValueAsString(accountWithModifications);
		System.out.println(json);
		String expected = "{\"id\":\"1001\",\"lastChangeId\":5,\"pendingFolderModifications\":[{\"folderId\":10,\"created\":[{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}],\"deleted\":[{\"id\":1,\"type\":\" Atype\"}],\"modifiedMsgs\":[{\"changeBitmask\":103,\"messageInfo\":{\"id\":11,\"imapUid\":22,\"type\":\"typeItem\",\"flags\":11,\"tags\":\"tagsItem\"}}],\"modifiedTags\":[{\"changeBitmask\":102,\"id\":1,\"name\":\"aNameTag\"}],\"renamedFolders\":[{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}]}]}";
		Assert.assertEquals(expected, json);
	}
	
	@Test
	public void deserializesFromJsonTest() throws Exception {
		AccountWithModifications accountWithModifications = null;
		String expected = "{\"id\":\"1001\",\"lastChangeId\":5,\"pendingFolderModifications\":[{\"folderId\":10,\"created\":[{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}],\"deleted\":[{\"id\":1,\"type\":\" Atype\"}],\"modifiedMsgs\":[{\"changeBitmask\":103,\"messageInfo\":{\"id\":11,\"imapUid\":22,\"type\":\"typeItem\",\"flags\":11,\"tags\":\"tagsItem\"}}],\"modifiedTags\":[{\"changeBitmask\":102,\"id\":1,\"name\":\"aNameTag\"}],\"renamedFolders\":[{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}]}]}";
		accountWithModifications = mapper.readValue(expected, AccountWithModifications.class);
		System.out.println(accountWithModifications);
		Assert.assertNotNull(accountWithModifications);
	}
}
