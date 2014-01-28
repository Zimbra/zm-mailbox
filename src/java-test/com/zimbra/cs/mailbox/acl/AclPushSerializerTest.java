/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.acl;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * @author zimbra
 * 
 */
public class AclPushSerializerTest {

	private ShareInfoData sid;
	
	@Before
	public void setUp() throws Exception {
		try {
			sid = new ShareInfoData();
			sid.setGranteeId("7ad43260-e8c0-423a-a3e5-bdaa51ec11d5");
			sid.setGranteeName("user3@rdesai.local");
			sid.setGranteeType(ACL.stringToType("usr"));
			sid.setItemId(258);
			sid.setItemUuid("886d073c-00d5-429e-b8d3-4f7385d32109");
			sid.setPath("/Inbox/Test; Bed");
			sid.setFolderDefaultView(MailItem.Type.MESSAGE);
			sid.setRights(ACL.stringToRights("r"));
			sid.setType(MailItem.Type.FOLDER);
			} catch (Exception e) {
				fail("No exception should be raised.");
			}
	}
	
	
	/**
	 * Test method for
	 * {@link com.zimbra.cs.mailbox.acl.AclPushSerializer#serialize(com.zimbra.cs.account.ShareInfoData)}
	 * .
	 */
	@Test
	public void testSerializeShareInfoDataFolderWithSemiColon() {
		try {
		sid.setPath("/Inbox/Test; Bed");
		String serData = AclPushSerializer.serialize(sid);
		int index = serData.indexOf("*ASCII59*");
		Assert.assertTrue(index != -1);
		
		sid.setPath("/Inbox/Test; Bed; 123");
		serData = AclPushSerializer.serialize(sid);
		System.out.println(serData);
		index = serData.indexOf("*ASCII59*");
		Assert.assertTrue(index != -1);
		} catch (Exception e) {
			fail("No exception should be raised.");
		}
	}
	
	@Test
	public void testSerializeShareInfoDataFolderWithoutSemiColon() {
		try {
		sid.setPath("/Inbox/Test_Bed Hello");
		String serData = AclPushSerializer.serialize(sid);
		int index = serData.indexOf("*ASCII59*");
		Assert.assertTrue(index == -1);
		} catch (Exception e) {
			fail("No exception should be raised.");
		}
	}

	/**
	 * Test method for
	 * {@link com.zimbra.cs.mailbox.acl.AclPushSerializer#deserialize(java.lang.String)}
	 * .
	 */
	@Test
	public void testDeserialize() {
		String shareInfoData = "granteeId:7ad43260-e8c0-423a-a3e5-bdaa51ec11d5;granteeName:user3@rdesai.local;granteeType:usr;folderId:258;"
				+ "folderUuid:886d073c-00d5-429e-b8d3-4f7385d32109;folderPath:/Inbox/Test; Bed;folderDefaultView:message;rights:r;type:folder";
	
		try {
			
			ShareInfoData data = AclPushSerializer.deserialize(shareInfoData);
			String path = data.getPath();
			Assert.assertEquals("/Inbox/Test; Bed", path);
			
			shareInfoData = "granteeId:7ad43260-e8c0-423a-a3e5-bdaa51ec11d5;granteeName:user3@rdesai.local;granteeType:usr;folderId:258;"
					+ "folderUuid:886d073c-00d5-429e-b8d3-4f7385d32109;folderPath:/Inbox/Test*ASCII59* Bed;folderDefaultView:message;rights:r;type:folder";
			data = AclPushSerializer.deserialize(shareInfoData);
			path = data.getPath();
			Assert.assertEquals("/Inbox/Test; Bed", path);
			
			shareInfoData = "granteeId:7ad43260-e8c0-423a-a3e5-bdaa51ec11d5;granteeName:user3@rdesai.local;granteeType:usr;folderId:258;"
					+ "folderUuid:886d073c-00d5-429e-b8d3-4f7385d32109;folderPath:/Inbox/TestASCII59 Bed;folderDefaultView:message;rights:r;type:folder";
			data = AclPushSerializer.deserialize(shareInfoData);
			path = data.getPath();
			Assert.assertEquals("/Inbox/TestASCII59 Bed", path);
			
			shareInfoData = "granteeId:7ad43260-e8c0-423a-a3e5-bdaa51ec11d5;granteeName:user3@rdesai.local;granteeType:usr;" 
					+ "folderId:258;folderUuid:886d073c-00d5-429e-b8d3-4f7385d32109;folderPath:/Inbox/Test*ASCII59* Bed*ASCII59* "
					+ "123;folderDefaultView:message;rights:r;type:folder";
			data = AclPushSerializer.deserialize(shareInfoData);
			path = data.getPath();
			Assert.assertEquals("/Inbox/Test; Bed; 123", path);		
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have not thrown a exception.");
		}
	
	}
	
	/**
	 * Test method for
	 * {@link com.zimbra.cs.mailbox.acl.AclPushSerializer#serialize(com.zimbra.cs.account.ShareInfoData)}
	 * .
	 */
	@Test
	public void testSerializeShareInfoDataFolderWithNewSemiColonEscapeAsData() {
		try {
		sid.setPath("/Inbox/Test*ASCII59*Bed");
		String serData = AclPushSerializer.serialize(sid);
		int index = serData.indexOf("*ASCII59*");
		Assert.assertTrue(index != -1);
		} catch (Exception e) {
			fail("No exception should be raised.");
		}
	}

}
