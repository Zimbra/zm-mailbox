package com.zimbra.soap.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import junit.framework.Assert;

public class ZmBooleanTest {

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void serializesToJsonTest() throws Exception {
		String json = mapper.writeValueAsString(ZmBoolean.ONE);
		System.out.println(json);
		Assert.assertEquals("true", json.toLowerCase());
	}

	@Test
	public void deserializesFromJsonTest() throws Exception {
		ZmBoolean zmBoolean = mapper.readValue(String.valueOf(true), ZmBoolean.class);
		System.out.println(zmBoolean);
		Assert.assertEquals(zmBoolean == ZmBoolean.ONE || zmBoolean == ZmBoolean.TRUE, true);
	}
}
