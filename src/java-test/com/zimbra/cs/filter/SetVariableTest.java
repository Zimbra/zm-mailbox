/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jsieve.exception.SyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.SetVariable;
import com.zimbra.cs.filter.jsieve.Variables;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

public class SetVariableTest {
	private String filterScript = "require [\"variables\"];\n" + "set \"var\" \"hello\" ;"
			+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";

	@BeforeClass
	public static void init() throws Exception {
		MailboxTestUtil.initServer();
		Provisioning prov = Provisioning.getInstance();
		prov.createAccount("test@in.telligent.com", "secret", new HashMap<String, Object>());
	}

	@Before
	public void setUp() throws Exception {
		MailboxTestUtil.clearData();
	}

	@Test
	public void testSetVar() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
					+ "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}

	@Test
	public void testSetVarAndUseInHeader() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			filterScript = "require [\"variables\"];\n" + "set \"var\" \"hello\" ;"
					+ "if header :contains \"Subject\" \"${var}\" { tag \"blue\"; }";
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n"
					+ "Subject: hello version 1.0 is out\n" + "\n" + "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("blue", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}

	@Test
	public void testSetVarAndUseInAction() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			filterScript = "require [\"variables\"];\n"
					+ "if header :matches \"Subject\" \"*\"{set \"var\" \"hello\" ; tag \"${var}\"; }";
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n"
					+ "Subject: hello version 1.0 is out\n" + "\n" + "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}

    @Test
    public void testReplacement() {
        Map<String, String> variables = new TreeMap<String, String>();
        Map<String, String> testCases = new TreeMap<String, String>();
        // RFC 5229 Section 3. Examples
        variables.put("company", "ACME");

        testCases.put("${full}", "");
        testCases.put("${company}", "ACME");
        testCases.put("${BAD${Company}", "${BADACME");
        testCases.put("${President, ${Company} Inc.}", "${President, ACME Inc.}");
        testCases.put("${company", "${company");
        testCases.put("${${company}}", "${ACME}");
        testCases.put("${${${company}}}", "${${ACME}}");
        testCases.put("&%${}!", "&%${}!");
        testCases.put("${doh!}", "${doh!}");
        
        // More examples from RFC 5229 Section 3. and RFC 5228 Section 8.1. 
        // variable-ref        =  "${" [namespace] variable-name "}"
        // namespace           =  identifier "." *sub-namespace
        // sub-namespace       =  variable-name "."
        // variable-name       =  num-variable / identifier
        // num-variable        =  1*DIGIT
        // identifier         = (ALPHA / "_") *(ALPHA / DIGIT / "_")
        variables.put("a.b", "おしらせ");
        variables.put("c_d", "C");
        variables.put("1", "One");
        variables.put("23", "twenty three");

        testCases.put("${a.b}", "おしらせ");
        testCases.put("${c_d}", "C");
        testCases.put("${1}", "One");
        testCases.put("${23}", "${23}");     // Not defined
        testCases.put("${123}", "${123}");   // Invalid variable name
        testCases.put("${a.b} ${COMpANY} ${c_d}hao!", "おしらせ ACME Chao!");

        for (Map.Entry<String, String> entry : testCases.entrySet()) {
            String result = Variables.leastGreedyReplace(variables, entry.getKey());
            Assert.assertEquals(entry.getValue(), result);
        }
    }
    
    
    @Test
	public void testSetVarWithModifiersValid() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			filterScript = "require [\"variables\"];\n"
					+ "set \"var\" \"hello\" ;"
					+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
					+ "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));
			
			RuleManager.clearCachedRules(account);
			filterScript = "require [\"variables\"];\n"
					+ "set :length \"var\" \"hello\" ;"
					+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";
			account.setMailSieveScript(filterScript);
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("5", ArrayUtil.getFirstElement(msg.getTags()));
			
			RuleManager.clearCachedRules(account);
			filterScript = "require [\"variables\"];\n"
					+ "set :lower \"var\" \"heLLo\" ;"
					+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";
			account.setMailSieveScript(filterScript);
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));
			
			RuleManager.clearCachedRules(account);
			filterScript = "require [\"variables\"];\n"
					+ "set :upper \"var\" \"test\" ;"
					+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";
			account.setMailSieveScript(filterScript);
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("TEST", ArrayUtil.getFirstElement(msg.getTags()));
			
			RuleManager.clearCachedRules(account);
			filterScript = "require [\"variables\"];\n"
					+ "set :lowerfirst \"var\" \"WORLD\" ;"
					+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";
			account.setMailSieveScript(filterScript);
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("wORLD", ArrayUtil.getFirstElement(msg.getTags()));
			
			RuleManager.clearCachedRules(account);
			filterScript = "require [\"variables\"];\n"
					+ "set :UPPERFIRST \"var\" \"example\" ;"
					+ "if header :matches \"Subject\" \"*\" { tag \"${var}\"; }";
			account.setMailSieveScript(filterScript);
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("Example", ArrayUtil.getFirstElement(msg.getTags()));
			
		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}
    
    
    @Test
   	public void testSetVarWithModifiersInValid() {
   		try {
   			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
   			RuleManager.clearCachedRules(account);
   			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
   			
   			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
   					+ "Hello World.";
   			try {
   				filterScript = "require [\"variables\"];\n"
   	   					+ "set  \"hello\" ;";
   	   			account.setMailSieveScript(filterScript);
   				RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
   					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
   					Mailbox.ID_FOLDER_INBOX, true);
   			} catch (Exception e) {
   				if (e instanceof SyntaxException) {
   					SyntaxException se = (SyntaxException) e;
   					
   					assertTrue(se.getMessage().indexOf("Atleast 2 argument are needed. Found Arguments: [[hello]]") > -1);
   				}
   			}
   			
   			try {
   				filterScript = "require [\"variables\"];\n"
   					+ "set :lownner \"var\" \"hello\" ;";
   	   			account.setMailSieveScript(filterScript);
   				RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
   					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
   					Mailbox.ID_FOLDER_INBOX, true);
   			} catch (Exception e) {
   				if (e instanceof SyntaxException) {
   					SyntaxException se = (SyntaxException) e;
   					assertTrue(se.getMessage().indexOf("Invalid variable modifier:") > -1);
   				}
   			}
   			
   			try {
   				filterScript = "require [\"variables\"];\n"
   					+ "set :lower \"var\"  ;";
   	   			account.setMailSieveScript(filterScript);
   				RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
   					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
   					Mailbox.ID_FOLDER_INBOX, true);
   			} catch (Exception e) {
   				if (e instanceof SyntaxException) {
   					SyntaxException se = (SyntaxException) e;
   					assertTrue(se.getMessage().indexOf("Invalid variable modifier:") > -1);
   				}
   			}

   		} catch (Exception e) {
   			e.printStackTrace();
   			fail("No exception should be thrown");
   		}

   	}
    
    @Test
    public void testApplyModifiers() {
    	String [] modifiers = new String [6];
    	modifiers[SetVariable.getIndex(SetVariable.ALL_LOWER_CASE)] =  SetVariable.ALL_LOWER_CASE;
    	modifiers[SetVariable.getIndex(SetVariable.UPPERCASE_FIRST)] = SetVariable.UPPERCASE_FIRST;
    	String value = SetVariable.applyModifiers("juMBlEd lETteRS", modifiers);
    	Assert.assertEquals("Jumbled letters", value);
    	
    	
    	modifiers = new String [6];
    	modifiers[SetVariable.getIndex(SetVariable.STRING_LENGTH)] =  SetVariable.STRING_LENGTH;
    	value = SetVariable.applyModifiers("juMBlEd lETteRS", modifiers);
    	Assert.assertEquals("15", value);
    	
    	modifiers = new String [6];
    	modifiers[SetVariable.getIndex(SetVariable.QUOTE_WILDCARD)] =  SetVariable.QUOTE_WILDCARD;
    	modifiers[SetVariable.getIndex(SetVariable.ALL_UPPER_CASE)] =  SetVariable.ALL_UPPER_CASE;
    	modifiers[SetVariable.getIndex(SetVariable.LOWERCASE_FIRST)] = SetVariable.LOWERCASE_FIRST;
    	value = SetVariable.applyModifiers("j?uMBlEd*lETte\\RS", modifiers);
    	System.out.println(value);
    	Assert.assertEquals("j\\?UMBLED\\*LETTE\\\\RS", value);
    }
    
	//    set "a" "juMBlEd lETteRS";             => "juMBlEd lETteRS"
	//    set :length "b" "${a}";                => "15"
	//    set :lower "b" "${a}";                 => "jumbled letters"
	//    set :upperfirst "b" "${a}";            => "JuMBlEd lETteRS"
	//    set :upperfirst :lower "b" "${a}"; 
    @Test
	public void testModifier() {
    	try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			filterScript = "require [\"variables\"];\n"
					+ "set \"a\" \"juMBlEd lETteRS\" ;\n"
					+ "set :length \"b\" \"${a}\";\n"
					+ "set :lower \"b\" \"${a}\";\n"
					+ "set :upperfirst \"c\" \"${b}\";"
					+ "set :upperfirst :lower \"d\" \"${c}\"; "
					+ "if header :matches \"Subject\" \"*\" { tag \"${d}\"; }";
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
					+ "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("Jumbled letters", ArrayUtil.getFirstElement(msg.getTags()));
		} catch (Exception e) {
			fail("No exception should be thrown");
		}
    }
    
    @Test
   	public void testVariablesCombo() {
       	try {
   			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
   			RuleManager.clearCachedRules(account);
   			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
   			// set "company" "ACME";
   			// set "a.b" "おしらせ"; (or any non-ascii characters)
   			// set "c_d" "C";
   			// set "1" "One"; ==> Should be ignored or error [Note 1]
   			// set "23" "twenty three"; ==> Should be ignored or error [Note 1]
   			// set "combination" "Hello ${company}!!";
   			filterScript = "require [\"variables\"];\n"
   					+ "set \"company\" \"おしらせ\" ;\n"
   					+ "set  \"a.b\" \"${a}\";\n"
   					+ "set  \"c_d\" \"C\";\n"
   					+ "set  \"1\" \"One\";"
   					+ "set  \"combination\" \"Hello ${company}!!\"; "
   					+ "if header :matches \"Subject\" \"*\" { tag \"${combination}\"; }";
   			account.setMailSieveScript(filterScript);
   			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
   					+ "Hello World.";
   			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
   					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
   					Mailbox.ID_FOLDER_INBOX, true);
   			Message msg = mbox.getMessageById(null, ids.get(0).getId());
   			Assert.assertEquals("Hello おしらせ!!", ArrayUtil.getFirstElement(msg.getTags()));
   		} catch (Exception e) {
   			fail("No exception should be thrown");
   		}
       }
    
    @Test
   	public void testStringInterpretation() {
       	try {
   			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
   			RuleManager.clearCachedRules(account);
   			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
   			filterScript = "require [\"variables\"];\n"
   					+ "set \"a\" \"juMBlEd lETteRS\" ;\n"
   					
   					+ "if header :matches \"Subject\" \"*\" { tag \"${d}\"; }";;
   			account.setMailSieveScript(filterScript);
   			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
   					+ "Hello World.";
   			try {
   			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
   					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
   					Mailbox.ID_FOLDER_INBOX, true);
   			
   				Message msg = mbox.getMessageById(null, ids.get(0).getId());
   			} catch (MailServiceException e) {
   				String t = e.getArgumentValue("name");
   				assertTrue(e.getCode().equals("mail.INVALID_NAME"));
   				assertEquals("", t);
   			}
   			
   			RuleManager.clearCachedRules(account);
   			filterScript = "require [\"variables\"];\n"
   					+ "set \"a\" \"juMBlEd lETteRS\" ;\n"
   					+ "if header :matches \"Subject\" \"*\" { tag \"${}\"; }";;
   			account.setMailSieveScript(filterScript);
			try {
				List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
						new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
						Mailbox.ID_FOLDER_INBOX, true);
			} catch (MailServiceException e) {
				String t = e.getArgumentValue("name");
				assertTrue(e.getCode().equals("mail.INVALID_NAME"));
				assertEquals("${}", t);
			}
   			RuleManager.clearCachedRules(account);
			filterScript = "require [\"variables\"];\n"
   					+ "set \"ave\" \"juMBlEd lETteRS\" ;\n"
   					+ "if header :matches \"Subject\" \"*\" { tag \"${ave!}\"; }";;
   			account.setMailSieveScript(filterScript);
			try {
				List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
						new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
						Mailbox.ID_FOLDER_INBOX, true);
			} catch (MailServiceException e) {
				String t = e.getArgumentValue("name");
				assertTrue(e.getCode().equals("mail.INVALID_NAME"));
				assertEquals("${ave!}", t);
			}
   		} catch (Exception e) {	
   			fail("No exception should be thrown");
   		}
       }
    
    @Test
   	public void testStringTest() {
       	try {
   			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);	
   			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
   			
   			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
   					+ "Hello World.";
   			RuleManager.clearCachedRules(account);
   			filterScript = "require [\"variables\"];\n"
   					+ "set :lower :upperfirst \"name\" \"Joe\";"
   					+  "if string :is :comparator \"i;ascii-numeric\" \"${name}\" [ \"Joe\", \"Hello\", \"User\" ]{ tag \"sales\"; }";
   			account.setMailSieveScript(filterScript);
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("sales", ArrayUtil.getFirstElement(msg.getTags()));
			
			RuleManager.clearCachedRules(account);
   			filterScript = "require [\"variables\"];\n"
   					+ "set :lower :upperfirst \"name\" \"Joe\";"
   					+  "if string :is  \"${name}\" [ \"Joe\", \"Hello\", \"User\" ]{ tag \"sales2\"; }";
   			account.setMailSieveScript(filterScript);
   			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("sales2", ArrayUtil.getFirstElement(msg.getTags()));
			
       	} catch (Exception e) {	
   			fail("No exception should be thrown");
   		}
    }
    
    @Test
	public void testSetMatchVarAndUseInHeader() {
		try {
			Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
			RuleManager.clearCachedRules(account);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

			filterScript = "require [\"variables\"];\n" 
					+ "if header :matches [\"To\", \"Cc\"] [\"coyote@**.com\",\"wile@**.com\"]{ tag \"${2}\"; }";
			System.out.println(filterScript);
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" 
					+ "To: coyote@ACME.Example.COM\n"
					+ "Subject: hello version 1.0 is out\n" + "\n" + "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			Assert.assertEquals(1, ids.size());
			Message msg = mbox.getMessageById(null, ids.get(0).getId());
			Assert.assertEquals("ACME.Example", ArrayUtil.getFirstElement(msg.getTags()));

		} catch (Exception e) {
			fail("No exception should be thrown");
		}

	}

}
