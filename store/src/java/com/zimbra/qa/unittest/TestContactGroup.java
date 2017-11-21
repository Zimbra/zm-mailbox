/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapAccount;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.ContactGroup.ContactRefMember;
import com.zimbra.cs.mailbox.ContactGroup.GalRefMember;
import com.zimbra.cs.mailbox.ContactGroup.InlineMember;
import com.zimbra.cs.mailbox.ContactGroup.Member;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.client.ZMailbox;

public class TestContactGroup {
    private static final String CONTACT_REF_VALUE = "736ae588-ae90-4427-a3de-6451e47e0857:257";
    private static final String GAL_REF_VALUE = "uid=user1,ou=people,dc=zimbra,dc=com";
    private static final String INLINE_VALUE = "user@test.com";
    
    private static class MemberData {
        private Member.Type type;
        private String value;
        
        private MemberData(Member.Type type, String value) {
            this.type = type;
            this.value = value;
        }
    }
        
    private ContactGroup createContactGroup(MemberData[] members) throws Exception {
        ContactGroup contactGroup = ContactGroup.init();
        for (MemberData member : members) {
            contactGroup.addMember(member.type, member.value);
        }
        return reEncode(contactGroup);
    }
    
    // encode, then return decoded, so encode/decode get tested
    private ContactGroup reEncode(ContactGroup contactGroup) throws Exception {
        String encoded = contactGroup.encode();
        return ContactGroup.init(encoded, "ownerAcctId" );
    }
    
    @BeforeClass
    public static void init() throws Exception {
        Zimbra.startupCLI();
        CliUtil.toolSetup();
        ZimbraLog.contact.setLevel(Log.Level.debug);
        /*
        ZimbraLog.contact.setLevel(Log.Level.debug);
        ZimbraLog.gal.setLevel(Log.Level.debug);
        ZimbraLog.index.setLevel(Log.Level.debug);
        ZimbraLog.search.setLevel(Log.Level.debug);
        */
    }
        
    @Test
    public void deleteAllMembers() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        contactGroup.removeAllMembers();
        
        contactGroup = reEncode(contactGroup);
        
        List<Member> members = contactGroup.getMembers();
        assertEquals(0, members.size());
    }
    
    @Test
    public void getMembers() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        List<Member> members = contactGroup.getMembers();
        assertEquals(3, members.size());
        assertTrue(members.get(0) instanceof ContactRefMember);
        assertEquals(CONTACT_REF_VALUE, members.get(0).getValue());
        assertTrue(members.get(1) instanceof GalRefMember);
        assertEquals(GAL_REF_VALUE, members.get(1).getValue());
        assertTrue(members.get(2) instanceof InlineMember);
        assertEquals(INLINE_VALUE, members.get(2).getValue());
    }
    
    @Test
    public void addMember() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        MemberData memberToAdd = new MemberData(Member.Type.INLINE, "added");
        Member addedMember = contactGroup.addMember(memberToAdd.type, memberToAdd.value);
        
        contactGroup = reEncode(contactGroup);
        
        List<Member> members = contactGroup.getMembers();
        assertEquals(4, members.size());
        
        Member newMember = members.get(3);
        assertEquals(memberToAdd.type, newMember.getType());
        assertEquals(memberToAdd.value, newMember.getValue());
    }
    
    @Test
    public void addDupMember() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        MemberData memberToAdd = new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE);
        
        boolean caughtException = false;
        try {
            contactGroup.addMember(memberToAdd.type, memberToAdd.value);
        } catch (ServiceException e) {
            if (e.getMessage().startsWith("invalid request: member already exists:")) {
                caughtException = true;
            }
        }
        
        assertTrue(caughtException);
    }
        
    @Test
    public void removeMember() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        Member memberToRemove = contactGroup.getMembers().get(0);
        contactGroup.removeMember(memberToRemove.getType(), memberToRemove.getValue());
        
        contactGroup = reEncode(contactGroup);
        
        List<Member> members = contactGroup.getMembers();
        assertEquals(2, members.size());
    }
    
    @Test
    public void removeNonExistingMember() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        boolean caughtException = false;
        try {
            contactGroup.removeMember(Member.Type.INLINE, "not there");
        } catch (ServiceException e) {
            if (e.getMessage().startsWith("invalid request: no such member:")) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        // verify members are untouched
        contactGroup = reEncode(contactGroup);
        
        List<Member> members = contactGroup.getMembers();
        assertEquals(3, members.size());
    }
    
    
    @Test
    public void insertOrder() throws Exception {
        int NUM_MEMBERS = 5;
        
        ContactGroup contactGroup = createContactGroup(new MemberData[0]);
        
        for (int i=1; i<=NUM_MEMBERS; i++) {
            MemberData memberData = new MemberData(Member.Type.INLINE, "" + i);
            contactGroup.addMember(memberData.type, memberData.value);
        }
        
        contactGroup = reEncode(contactGroup);
        
        List<Member> members = contactGroup.getMembers();
        
        Member firstMember = members.get(0);
        
        // delete
        contactGroup.removeMember(firstMember.getType(), firstMember.getValue());
        
        // then re-add
        contactGroup.addMember(firstMember.getType(), firstMember.getValue());
        
        contactGroup = reEncode(contactGroup);
        members = contactGroup.getMembers();
        
        // verify the member is now at the end and other member shifted up
        for (int i=0; i<NUM_MEMBERS-1; i++) {
            Member member = members.get(i);
            assertEquals(Member.Type.INLINE, member.getType());
            assertEquals("" + (i+2), member.getValue());
        }
        
        Member lastMember = members.get(NUM_MEMBERS-1);
        assertEquals(firstMember.getType(), lastMember.getType());
        assertEquals(firstMember.getValue(), lastMember.getValue());
    }
    
    @Test
    public void unmodifiableList() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        boolean caughtException = false;
        try {
            List<Member> members = contactGroup.getMembers();
            for (Member member : members) {
                members.remove(member);  // not allowed
            }
        } catch (UnsupportedOperationException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
    }
    
    @Test
    public void membersIterator() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE),
                new MemberData(Member.Type.INLINE, INLINE_VALUE)});
        
        boolean caughtException = false;
        try {
            for (Iterator<Member> iter = contactGroup.getMembers().iterator(); iter.hasNext();) {
                Member member = iter.next();
                
                if (member.getType() == Member.Type.GAL_REF || member.getType() == Member.Type.INLINE) {
                    iter.remove();  // not allowed
                }
            }
        } catch (UnsupportedOperationException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
    }
    
    @Test
    public void derefContact() throws Exception {
        Account account = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        OperationContext octxt = null;  //TODO
        
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_fileAs, ContactConstants.FA_FIRST_LAST);
        fields.put(ContactConstants.A_firstName, "test");
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        fields.put(ContactConstants.A_workEmail1, "test2@zimbra.com");
        Contact contact = mbox.createContact(octxt, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, "" + contact.getId()),
                new MemberData(Member.Type.INLINE, "aaa@test.com"), 
                new MemberData(Member.Type.INLINE, "zzz@test.com")});
        
        contactGroup.derefAllMembers(mbox, octxt);
        
        boolean gotContactRefMember = false;
        String prevMemberKey = null;
        for (Member member : contactGroup.getDerefedMembers()) {
            String memberKey = member.getDerefedKey();
            if (prevMemberKey != null) {
                assertTrue(prevMemberKey.compareTo(memberKey) < 0);
            }
            prevMemberKey = memberKey;
            
            Member.Type type = member.getType();
            if (type == Member.Type.CONTACT_REF) {
                assertEquals("test", memberKey);
                gotContactRefMember = true;
            }
            // System.out.println(memberKey);
        }
        
        List<String> emailAddrs = contactGroup.getEmailAddresses(false, mbox, octxt, false);
        assertEquals(4, emailAddrs.size());
        assertTrue(emailAddrs.contains("test1@zimbra.com"));
        assertTrue(emailAddrs.contains("test2@zimbra.com"));
        assertTrue(emailAddrs.contains("aaa@test.com"));
        assertTrue(emailAddrs.contains("zzz@test.com"));
        
        assertTrue(gotContactRefMember);
    }

    @Test
    public void derefGal() throws Exception {
        Account account = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        OperationContext octxt = null;  //TODO
        
        Account galMember = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user2"));
        LdapAccount ldapAccount = (LdapAccount) galMember;
        String dn = ldapAccount.getDN();
        String email = galMember.getName();
        
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.GAL_REF, dn),
                new MemberData(Member.Type.INLINE, "aaa@test.com"), 
                new MemberData(Member.Type.INLINE, "zzz@test.com")});
        
        contactGroup.derefAllMembers(mbox, octxt);
        
        boolean gotGalRefMember = false;
        String prevMemberKey = null;
        for (Member member : contactGroup.getDerefedMembers()) {
            String memberKey = member.getDerefedKey();
            if (prevMemberKey != null) {
                assertTrue(prevMemberKey.compareTo(memberKey) < 0);
            }
            prevMemberKey = memberKey;
            
            Member.Type type = member.getType();
            if (type == Member.Type.GAL_REF) {
                assertEquals(email, memberKey);
                gotGalRefMember = true;
            }
            // System.out.println(memberKey);
        }
        
        List<String> emailAddrs = contactGroup.getEmailAddresses(false, mbox, octxt, false);
        assertEquals(3, emailAddrs.size());
        assertTrue(emailAddrs.contains(email));
        assertTrue(emailAddrs.contains("aaa@test.com"));
        assertTrue(emailAddrs.contains("zzz@test.com"));
        
        assertTrue(gotGalRefMember);
    }
    
    @Test
    public void replaceMembersWithDlist() throws Exception {
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, CONTACT_REF_VALUE),
                new MemberData(Member.Type.GAL_REF, GAL_REF_VALUE)});
        
        String dlist = "\"Ballard, Martha\" <martha34@aol.com>, \"Davidson, Ross\" <rossd@example.zimbra.com>, user1@test.com";
        contactGroup.migrateFromDlist(dlist);
        
        contactGroup = reEncode(contactGroup);
        
        List<Member> members = contactGroup.getMembers();
        assertEquals(3, members.size());
        
        Member member = members.get(0);
        assertEquals(Member.Type.INLINE, member.getType());
        assertEquals("\"Ballard, Martha\" <martha34@aol.com>", member.getValue());
        
        member = members.get(1);
        assertEquals(Member.Type.INLINE, member.getType());
        assertEquals("\"Davidson, Ross\" <rossd@example.zimbra.com>", member.getValue());
        
        member = members.get(2);
        assertEquals(Member.Type.INLINE, member.getType());
        assertEquals("user1@test.com", member.getValue());
    }
    
    @Test 
    public void returnMembersAsDlist() throws Exception {
        Account account = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        OperationContext octxt = null;  //TODO
        
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_fileAs, ContactConstants.FA_FIRST_LAST);
        fields.put(ContactConstants.A_firstName, "test");
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        fields.put(ContactConstants.A_workEmail1, "test2@zimbra.com");
        Contact contact = mbox.createContact(octxt, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        
        Account galMember = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user2"));
        LdapAccount ldapAccount = (LdapAccount) galMember;
        String dn = ldapAccount.getDN();
        String galEntryEmail = galMember.getName();
        
        ContactGroup contactGroup = createContactGroup(new MemberData[] {
                new MemberData(Member.Type.CONTACT_REF, "" + contact.getId()),
                new MemberData(Member.Type.GAL_REF, dn),
                new MemberData(Member.Type.INLINE, "aaa@test.com"), 
                new MemberData(Member.Type.INLINE, "zzz@test.com")});
        
        contactGroup.derefAllMembers(mbox, octxt);
        for (Member member : contactGroup.getDerefedMembers()) {
            String memberKey = member.getDerefedKey();
            System.out.println(memberKey);
        }
        
        String dlist = contactGroup.migrateToDlist(mbox, octxt);
        
        // should be in member order
        assertEquals("test1@zimbra.com, test2@zimbra.com, " + galEntryEmail + ", aaa@test.com, zzz@test.com", dlist);
    }
    
    @Test
    @Ignore
    public void zimbraDelimittedFields() throws Exception {
        Account acct = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
       
        String relativePath = "/Contacts?fmt=cf&t=2&all";
        
        ZMailbox mbox = TestUtil.getZMailbox(acct.getName());
        byte[] bytes = ByteUtil.getContent(mbox.getRESTResource(relativePath), 1024);
        String data = new String(bytes);
        // System.out.println(data);
        
        // delimeters defined in ContactFolderFormatter
        String[] contacts = data.split("\u001E");
        for (String contact : contacts) {
            // System.out.println(contact);
            String[] fields = contact.split("\u001D");
            for (int i = 0; i < fields.length-1; i+=2) {
                System.out.println(fields[i] + " = " + fields[i+1]);
            }
            System.out.println();
        }
    }
    
    @Test
    public void csvOldZCOClient() throws Exception {
        Account acct = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
        
        // String relativePath = "/?fmt=zip&meta=0&zlv=4&list=259";
        
        String relativePath = "/?fmt=zip&meta=1&zlv=4&list=257";
        
        final int BUFFER = 2048;
        
        ZMailbox mbox = TestUtil.getZMailbox(acct.getName());
        ZipInputStream zin = new ZipInputStream(mbox.getRESTResource(relativePath));
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            System.out.println("Unzipping " + ze.getName());
            int count;
            byte data[] = new byte[BUFFER];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            while ((count = zin.read(data, 0, BUFFER)) 
              != -1) {
               os.write(data, 0, count);
            }
            os.flush();
            
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            byte[] bytes = ByteUtil.getContent(is, 1024);
            String dd = new String(bytes, "UTF-8");
            System.out.println(dd);
            is.close();
            
            os.close();
        }
        zin.close();
    }
    
    /*
    public class UnZip {
        final int BUFFER = 2048;
        public static void main (String argv[]) {
           try {
              BufferedOutputStream dest = null;
              FileInputStream fis = new 
            FileInputStream(argv[0]);
              ZipInputStream zis = new 
            ZipInputStream(new BufferedInputStream(fis));
              ZipEntry entry;
              while((entry = zis.getNextEntry()) != null) {
                 System.out.println("Extracting: " +entry);
                 int count;
                 byte data[] = new byte[BUFFER];
                 // write the files to the disk
                 FileOutputStream fos = new 
               FileOutputStream(entry.getName());
                 dest = new 
                   BufferedOutputStream(fos, BUFFER);
                 while ((count = zis.read(data, 0, BUFFER)) 
                   != -1) {
                    dest.write(data, 0, count);
                 }
                 dest.flush();
                 dest.close();
              }
              zis.close();
           } catch(Exception e) {
              e.printStackTrace();
           }
        }
     }
    */
    
    /*
    private SoapHttpTransport auth(String acctName, String password) throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        
        Element request = Element.create(transport.getRequestProtocol(), AccountConstants.AUTH_REQUEST);
        request.addElement(AccountConstants.E_ACCOUNT).addAttribute(AccountConstants.A_BY, AccountBy.name.name()).setText(acctName);
        request.addElement(AccountConstants.E_PASSWORD).setText(password);
        
        Element response = transport.invoke(request);
        
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
        
        return transport;
    }
    
    @Test
    public void migrateFromDlist() throws Exception {
        String acctName = TestUtil.getAddress("user1");
        String password = "test123";
        
        SoapHttpTransport transport = auth(acctName, password);
        
        Element request = Element.create(transport.getRequestProtocol(), MailConstants.CREATE_CONTACT_REQUEST);
        Element eContact = request.addElement(MailConstants.E_CONTACT);
        eContact.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, "nickname").setText("group1");
        eContact.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, "fileAs").setText("8:group1");
        eContact.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, "type").setText("group");
        eContact.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, "dlist").
            setText("user1@test.com, user2@test.com, user3@test.com");

        Element response = transport.invoke(request);
    }
    */
}
