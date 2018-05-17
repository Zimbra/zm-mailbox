package com.zimbra.soap.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import com.zimbra.soap.mail.type.DeleteItemNotification;

import junit.framework.Assert;

public class IdAndTypeTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		IdAndType idAndType = new IdAndType("1"," Atype");
		String json = mapper.writeValueAsString(idAndType);
		System.out.println(json);
		String expected = "{\"id\":\"1\",\"type\":\" Atype\"}";
		Assert.assertEquals(expected, json);
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		IdAndType idAndType = null;
		String expected = "{\"id\":1,\"type\":\" Atype\"}";
		idAndType = mapper.readValue(expected, IdAndType.class);
		System.out.println(idAndType);
		Assert.assertNotNull(idAndType);
	}

}
