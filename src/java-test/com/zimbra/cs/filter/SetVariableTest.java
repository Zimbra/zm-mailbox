package com.zimbra.cs.filter;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jsieve.exception.SyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
					+ "set \"var\" \"hello\" ;";
			account.setMailSieveScript(filterScript);
			String raw = "From: sender@in.telligent.com\n" + "To: test1@in.telligent.com\n" + "Subject: Test\n" + "\n"
					+ "Hello World.";
			List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			filterScript = "require [\"variables\"];\n"
					+ "set :length \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :lower \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :upper \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :lowerfirst \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :UPPERFIRST \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			//quotewildcard
			filterScript = "require [\"variables\"];\n"
					+ "set :quotewildcard \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :quotewildcard  :UPPERFIRST  \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :quotewildcard  :UPPERFIRST :length \"var\" \"hello\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);
			
			filterScript = "require [\"variables\"];\n"
					+ "set :quotewildcard  :UPPERFIRST :length \"var\" \"${1}\" ;";
			ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
					new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
					Mailbox.ID_FOLDER_INBOX, true);			

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
    	modifiers[SetVariable.getIndex(":" + SetVariable.ALL_LOWER_CASE)] =  SetVariable.ALL_LOWER_CASE;
    	modifiers[SetVariable.getIndex(":" + SetVariable.UPPERCASE_FIRST)] = SetVariable.UPPERCASE_FIRST;
    	String value = SetVariable.applyModifiers("juMBlEd lETteRS", modifiers);
    	Assert.assertEquals("Jumbled letters", value);
    	
    	
    	modifiers = new String [6];
    	modifiers[SetVariable.getIndex(":" + SetVariable.STRING_LENGTH)] =  SetVariable.STRING_LENGTH;
    	value = SetVariable.applyModifiers("juMBlEd lETteRS", modifiers);
    	Assert.assertEquals("15", value);
    	
    	modifiers = new String [6];
    	modifiers[SetVariable.getIndex(":" + SetVariable.QUOTE_WILDCARD)] =  SetVariable.QUOTE_WILDCARD;
    	modifiers[SetVariable.getIndex(":" + SetVariable.ALL_UPPER_CASE)] =  SetVariable.ALL_UPPER_CASE;
    	modifiers[SetVariable.getIndex(":" + SetVariable.LOWERCASE_FIRST)] = SetVariable.LOWERCASE_FIRST;
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
					+ "if header :matches \"Subject\" \"*\" { tag \"${d}\"; }";;
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

}
