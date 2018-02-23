package com.zimbra.soap.mail.type;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import junit.framework.Assert;

public class DeleteItemNotificationTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		DeleteItemNotification deleteItemNotification = new DeleteItemNotification(1," Atype");
		String json = mapper.writeValueAsString(deleteItemNotification);
		System.out.println(json);
		String expected = "{\"id\":1,\"type\":\" Atype\"}";
		Assert.assertEquals(expected, json);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		DeleteItemNotification deleteItemNotification = null;
		String expected = "{\"id\":1,\"type\":\" Atype\"}";
		deleteItemNotification = mapper.readValue(expected, DeleteItemNotification.class);
		System.out.println(deleteItemNotification);
		Assert.assertNotNull(deleteItemNotification);
	}
	
}
