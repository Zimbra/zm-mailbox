package com.zimbra.soap.mail.type;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import junit.framework.Assert;

public class ImapMessageInfoTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		ImapMessageInfo imapMessageInfo = new ImapMessageInfo(1, 2, "type", 1, "tags");
		String json = mapper.writeValueAsString(imapMessageInfo);
		System.out.println(json);
		String expected = "{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}";
		Assert.assertEquals(expected, json);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		ImapMessageInfo imapMessageInfo = null;
		String expected = "{\"id\":1,\"imapUid\":2,\"type\":\"type\",\"flags\":1,\"tags\":\"tags\"}";
		imapMessageInfo = mapper.readValue(expected, ImapMessageInfo.class);
		System.out.println(imapMessageInfo);
		Assert.assertNotNull(imapMessageInfo);
	}

}
