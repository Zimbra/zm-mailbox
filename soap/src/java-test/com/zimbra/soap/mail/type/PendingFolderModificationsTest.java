package com.zimbra.soap.mail.type;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;

public class PendingFolderModificationsTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		PendingFolderModifications pendingFolderModifications = new PendingFolderModifications(10);
		pendingFolderModifications
				.addCreatedItem(new CreateItemNotification(new ImapMessageInfo(1, 2, "type", 1, "tags")));
		pendingFolderModifications.addDeletedItem(new DeleteItemNotification(1, " Atype"));
		pendingFolderModifications.addModifiedMsg(new ModifyNotification.ModifyItemNotification(
				new ImapMessageInfo(11, 22, "typeItem", 11, "tagsItem"), 103));
		pendingFolderModifications.addModifiedTag(new ModifyNotification.ModifyTagNotification(1, "aNameTag", 102));
		pendingFolderModifications.addRenamedFolder(new ModifyNotification.RenameFolderNotification(12, "path", 104));

		String json = mapper.writeValueAsString(pendingFolderModifications);
		System.out.println(json);
		String expected = "{\"folderId\":10,\"created\":[{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}],\"deleted\":[{\"id\":1,\"type\":\" Atype\"}],\"modifiedMsgs\":[{\"changeBitmask\":103,\"messageInfo\":{\"id\":11,\"imapUid\":22,\"type\":\"typeItem\",\"flags\":11,\"tags\":\"tagsItem\"}}],\"modifiedTags\":[{\"changeBitmask\":102,\"id\":1,\"name\":\"aNameTag\"}],\"renamedFolders\":[{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}]}";
		Assert.assertEquals(expected, json);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		PendingFolderModifications pendingFolderModifications = null;
		String expected = "{\"folderId\":10,\"created\":[{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}],\"deleted\":[{\"id\":1,\"type\":\" Atype\"}],\"modifiedMsgs\":[{\"changeBitmask\":103,\"messageInfo\":{\"id\":11,\"imapUid\":22,\"type\":\"typeItem\",\"flags\":11,\"tags\":\"tagsItem\"}}],\"modifiedTags\":[{\"changeBitmask\":102,\"id\":1,\"name\":\"aNameTag\"}],\"renamedFolders\":[{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}]}";
		pendingFolderModifications = mapper.readValue(expected, PendingFolderModifications.class);
		System.out.println(pendingFolderModifications);
		Assert.assertNotNull(pendingFolderModifications);
	}

}
