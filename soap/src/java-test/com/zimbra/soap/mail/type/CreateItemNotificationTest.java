package com.zimbra.soap.mail.type;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;

public class CreateItemNotificationTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		ImapMessageInfo imapMessageInfo = new ImapMessageInfo(1, 2, "type", 1, "tags");
		CreateItemNotification createItemNotification = new CreateItemNotification(imapMessageInfo);
		String json = mapper.writeValueAsString(createItemNotification);
		System.out.println(json);
		String expected = "{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}";
		Assert.assertEquals(expected, json);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		CreateItemNotification createItemNotification = null;
		String expected = "{\"messageInfo\":{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}}";
		createItemNotification = mapper.readValue(expected, CreateItemNotification.class);
		System.out.println(createItemNotification);
		Assert.assertNotNull(createItemNotification);
	}

}
