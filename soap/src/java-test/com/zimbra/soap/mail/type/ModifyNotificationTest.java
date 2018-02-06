package com.zimbra.soap.mail.type;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;

public class ModifyNotificationTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		ModifyNotification modifyNotification = new ModifyNotification(101);
		String json = mapper.writeValueAsString(modifyNotification);
		System.out.println(json);
		String expected = "{\"changeBitmask\":101}";
		Assert.assertEquals(expected, json);

		ModifyNotification.ModifyTagNotification modifyTagNotification = new ModifyNotification.ModifyTagNotification(1,
				"aName", 102);
		String jsonTag = mapper.writeValueAsString(modifyTagNotification);
		System.out.println(jsonTag);
		String expectedTag = "{\"changeBitmask\":102,\"id\":1,\"name\":\"aName\"}";
		Assert.assertEquals(expectedTag, jsonTag);

		ImapMessageInfo imapMessageInfo = new ImapMessageInfo(1, 2, "type", 1, "tags");
		ModifyNotification.ModifyItemNotification modifyItemNotification = new ModifyNotification.ModifyItemNotification(
				imapMessageInfo, 103);
		String jsonItem = mapper.writeValueAsString(modifyItemNotification);
		System.out.println(jsonItem);
		String expectedItem = "{\"changeBitmask\":103,\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}";
		Assert.assertEquals(expectedItem, jsonItem);

		ModifyNotification.RenameFolderNotification renameFolderNotification = new ModifyNotification.RenameFolderNotification(
				12, "path", 104);
		String jsonRename = mapper.writeValueAsString(renameFolderNotification);
		System.out.println(jsonRename);
		String expectedRename = "{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}";
		Assert.assertEquals(expectedRename, jsonRename);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		ModifyNotification modifyNotification = null;
		String expected = "{\"changeBitmask\":101}";
		modifyNotification = mapper.readValue(expected, ModifyNotification.class);
		System.out.println(modifyNotification);
		Assert.assertNotNull(modifyNotification);

		ModifyNotification.ModifyTagNotification modifyTagNotification = null;
		String expectedTag = "{\"changeBitmask\":102,\"id\":1,\"name\":\"aName\"}";
		modifyTagNotification = mapper.readValue(expectedTag, ModifyNotification.ModifyTagNotification.class);
		System.out.println(modifyTagNotification);
		Assert.assertNotNull(modifyTagNotification);

		ModifyNotification.ModifyItemNotification modifyItemNotification = null;
		String expectedItem = "{\"changeBitmask\":103,\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}";
		modifyItemNotification = mapper.readValue(expectedItem, ModifyNotification.ModifyItemNotification.class);
		System.out.println(modifyItemNotification);
		Assert.assertNotNull(modifyItemNotification);

		ModifyNotification.RenameFolderNotification renameFolderNotification = null;
		String expectedRename = "{\"changeBitmask\":104,\"folderId\":12,\"path\":\"path\"}";
		renameFolderNotification = mapper.readValue(expectedRename, ModifyNotification.RenameFolderNotification.class);
		System.out.println(renameFolderNotification);
		Assert.assertNotNull(renameFolderNotification);
	}

}
