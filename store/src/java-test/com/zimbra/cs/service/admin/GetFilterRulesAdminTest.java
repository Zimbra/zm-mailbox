package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.RuleManager.AdminFilterType;
import com.zimbra.cs.filter.RuleManager.FilterType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.mail.type.EditheaderTest;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;

import junit.framework.Assert;

public class GetFilterRulesAdminTest {
    private static final String ACCOUNTNAME = "test1_zcs273_"+System.currentTimeMillis()+"@zimbra.com";
    private static Provisioning prov = null;
    private Account account = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount(ACCOUNTNAME, "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = prov.getAccountByName(ACCOUNTNAME);
    }

    /**************addheader***************/
    @Test
    public void testSieveToSoapAddheaderActionWithoutLast() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "# rule1\n"
                + "require [\"editheader\"];\n"
                + "addheader \"X-My-Header\" \"Test Value\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule rule = filterRules.get(0);
        Assert.assertTrue(rule.isActive());
        Assert.assertEquals(rule.getName(), "rule1");
        Assert.assertEquals(rule.getActionCount(), 1);
        FilterAction filterAction = rule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.AddheaderAction);
        FilterAction.AddheaderAction action = (FilterAction.AddheaderAction) filterAction;
        Assert.assertEquals(action.getHeaderName(), "X-My-Header");
        Assert.assertEquals(action.getHeaderValue(), "Test Value");
        Assert.assertNull(action.getLast());
    }

    @Test
    public void testSieveToSoapAddheaderActionWithLast() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "# rule2\n"
                + "require [\"editheader\"];\n"
                + "addheader :last \"X-My-Header\" \"Test Value\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule rule = filterRules.get(0);
        Assert.assertTrue(rule.isActive());
        Assert.assertEquals(rule.getName(), "rule2");
        Assert.assertEquals(rule.getActionCount(), 1);
        FilterAction filterAction = rule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.AddheaderAction);
        FilterAction.AddheaderAction action = (FilterAction.AddheaderAction) filterAction;
        Assert.assertEquals(action.getHeaderName(), "X-My-Header");
        Assert.assertEquals(action.getHeaderValue(), "Test Value");
        Assert.assertTrue(action.getLast());
    }

    // worng tag instead of last
    @Test
    public void negativeTestSieveToSoapAddheaderAction1() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "addheader :abcd \"X-My-Header\" \"Test Value\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "Invalid argument :abcd received with addheader");
        }
    }

    // empty headerName
    @Test
    public void negativeTestSieveToSoapAddheaderAction2() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "addheader \"\" \"Test Value\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "parse error: Invalid addheader action: Missing headerName or headerValue");
        }
    }

    // empty headerValue
    @Test
    public void negativeTestSieveToSoapAddheaderAction3() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "addheader \"X-My-Header\" \"\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "parse error: Invalid addheader action: Missing headerName or headerValue");
        }
    }

    /**************deleteheader***************/
    @Test
    public void testSieveToSoapDeleteheaderActionBasic() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader \"X-My-Header\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        EditheaderTest test = action.getTest();
        Assert.assertNull(test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        Assert.assertNull(test.getHeaderValue());
    }

    @Test
    public void testSieveToSoapDeleteheaderActionBasicWithValue() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader \"X-My-Header\" \"Test Value\";";// matchType and comparator should be added by default
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-casemap", test.getComparator());
        Assert.assertEquals("is", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Test Value", values.get(0));
    }

    @Test
    public void testSieveToSoapDeleteheaderActionWithIndex() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :index 3 \"X-My-Header\" \"Test Value\";";// matchType and comparator should be added by default
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertEquals(3, action.getOffset().intValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-casemap", test.getComparator());
        Assert.assertEquals("is", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Test Value", values.get(0));
    }

    @Test
    public void testSieveToSoapDeleteheaderActionWithIndexAndLast() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 \"X-My-Header\" \"Test Value\";";// matchType and comparator should be added by default
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertTrue(action.getLast());
        Assert.assertEquals(3, action.getOffset().intValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-casemap", test.getComparator());
        Assert.assertEquals("is", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("Test Value", values.get(0));
    }

    @Test
    public void testSieveToSoapDeleteheaderActionWithIndexAndLastWithoutValue() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 \"X-My-Header\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertTrue(action.getLast());
        Assert.assertEquals(3, action.getOffset().intValue());
        EditheaderTest test = action.getTest();
        Assert.assertNull(test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        Assert.assertNull(test.getHeaderValue());
    }

    @Test
    public void testSieveToSoapDeleteheaderActionWithMultiValue() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 :comparator \"i;octet\" :contains \"X-My-Header\" [\"Value1\", \"Value2\"];";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertTrue(action.getLast());
        Assert.assertEquals(3, action.getOffset().intValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;octet", test.getComparator());
        Assert.assertEquals("contains", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("Value1", values.get(0));
        Assert.assertEquals("Value2", values.get(1));
    }

    @Test
    public void testSieveToSoapDeleteheaderActionWithValueAndRelationalComparator() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 :value \"ge\" :comparator \"i;ascii-numeric\" \"X-My-Header\" \"2\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertTrue(action.getLast());
        Assert.assertEquals(3, action.getOffset().intValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-numeric", test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertEquals("ge", test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertTrue(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("2", values.get(0));
    }

    @Test
    public void testSieveToSoapDeleteheaderActionWithCountAndRelationalComparator() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 :count \"ge\" :comparator \"i;ascii-numeric\" \"X-My-Header\" \"2\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.DeleteheaderAction);
        FilterAction.DeleteheaderAction action = (FilterAction.DeleteheaderAction) filterAction;
        Assert.assertTrue(action.getLast());
        Assert.assertEquals(3, action.getOffset().intValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-numeric", test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertEquals("ge", test.getRelationalComparator());
        Assert.assertTrue(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("2", values.get(0));
    }

    // invalid tag
    @Test
    public void negativeTestSieveToSoapDeleteheaderAction1() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 :comparator \"i;octet\" :asdf \"X-My-Header\" [\"Value1\", \"Value2\"];";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "parse error: Invalid tag \":asdf\" received with deleteheader");
        }
    }

    // missing index number
    @Test
    public void negativeTestSieveToSoapDeleteheaderAction2() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index :comparator \"i;octet\" \"X-My-Header\" [\"Value1\", \"Value2\"];";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "parse error: Invalid value \":comparator\" received with \":index\"");
        }
    }

    // missing index tag
    @Test
    public void negativeTestSieveToSoapDeleteheaderAction3() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader;";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "parse error: EditheaderTest : Missing headerName");
        }
    }

    // relational comparator and matchType together
    @Test
    public void negativeTestSieveToSoapDeleteheaderAction4() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\"];\n"
                + "# rule1\n"
                + "deleteheader :last :index 3 :count \"ge\" :comparator \"i;ascii-numeric\" :is \"X-My-Header\" \"2\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals(se.getMessage(), "parse error: EditheaderTest : :count or :value can not be used with matchType");
        }
    }

    /**************replaceheader***************/
    // TODO: start writing unit tests
    @Test
    public void testSieveToSoapReplaceheaderActionBasic() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :newvalue \"[new] ${1}\" \"X-My-Header\" \"xyz\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        Assert.assertNull(action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-casemap", test.getComparator());
        Assert.assertEquals("is", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("xyz", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithNewnameAndNewvaue() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" \"X-My-Header\" \"xyz\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-casemap", test.getComparator());
        Assert.assertEquals("is", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("xyz", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithComparator() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;octet\" \"X-My-Header\" \"xyz\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;octet", test.getComparator());
        Assert.assertEquals("is", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("xyz", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithComparatorAndMatchType() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;octet\" :contains \"X-My-Header\" \"xyz\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;octet", test.getComparator());
        Assert.assertEquals("contains", test.getMatchType());
        Assert.assertNull(test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("xyz", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithRelationalComparatorAndValue() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;ascii-numeric\" :value \"ge\" \"X-My-Header\" \"2\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-numeric", test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertEquals("ge", test.getRelationalComparator());
        Assert.assertNull(test.getCount());
        Assert.assertTrue(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("2", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithRelationalComparatorAndCount() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;ascii-numeric\" :count \"eq\" \"X-My-Header\" \"3\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertNull(action.getOffset());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-numeric", test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertEquals("eq", test.getRelationalComparator());
        Assert.assertTrue(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("3", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithIndex() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :index 4 :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;ascii-numeric\" :count \"eq\" \"X-My-Header\" \"3\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertNull(action.getLast());
        Assert.assertEquals(4, action.getOffset().intValue());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-numeric", test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertEquals("eq", test.getRelationalComparator());
        Assert.assertTrue(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("3", values.get(0));
    }

    @Test
    public void testSieveToSoapReplaceheaderActionBasicWithLastAndIndex() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :last :index 2 :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;ascii-numeric\" :count \"eq\" \"X-My-Header\" \"3\";";
        account.setAdminSieveScriptBefore(script);

        List<FilterRule> filterRules = RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        Assert.assertEquals(filterRules.size(), 1);
        FilterRule filterRule = filterRules.get(0);
        Assert.assertEquals("rule1", filterRule.getName());
        Assert.assertTrue(filterRule.isActive());
        Assert.assertEquals(1, filterRule.getFilterActions().size());
        FilterAction filterAction = filterRule.getFilterActions().get(0);
        Assert.assertTrue(filterAction instanceof FilterAction.ReplaceheaderAction);
        FilterAction.ReplaceheaderAction action = (FilterAction.ReplaceheaderAction) filterAction;
        Assert.assertTrue(action.getLast());
        Assert.assertEquals(2, action.getOffset().intValue());
        Assert.assertEquals("X-My-Header2", action.getNewName());
        Assert.assertEquals("[new] ${1}", action.getNewValue());
        EditheaderTest test = action.getTest();
        Assert.assertEquals("i;ascii-numeric", test.getComparator());
        Assert.assertNull(test.getMatchType());
        Assert.assertEquals("eq", test.getRelationalComparator());
        Assert.assertTrue(test.getCount());
        Assert.assertNull(test.getValue());
        Assert.assertEquals("X-My-Header", test.getHeaderName());
        List<String> values = test.getHeaderValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("3", values.get(0));
    }

    // with last without index
    @Test
    public void negativeTestSieveToSoapReplaceheaderAction() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :last :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;ascii-numeric\" :count \"eq\" \"X-My-Header\" \"3\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals("parse error: :index <offset> must be provided with :last", se.getMessage());
        }
    }

    // with last, index and without number
    @Test
    public void negativeTestSieveToSoapReplaceheaderAction2() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :last :index :newname \"X-My-Header2\" :newvalue \"[new] ${1}\" :comparator \"i;ascii-numeric\" :count \"eq\" \"X-My-Header\" \"3\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals("parse error: Invalid value \":newname\" received with \":index\"", se.getMessage());
        }
    }

    // missing :newname and :newvalue
    @Test
    public void negativeTestSieveToSoapReplaceheaderAction3() throws ServiceException {
        RuleManager.clearCachedRules(account);
        String script = "require [\"editheader\", \"variables\"];\n"
                + "# rule1\n"
                + "replaceheader :last :index 2 :comparator \"i;ascii-numeric\" :count \"eq\" \"X-My-Header\" \"3\";";
        account.setAdminSieveScriptBefore(script);

        try {
            RuleManager.getAdminRulesAsXML(account, FilterType.INCOMING, AdminFilterType.BEFORE);
        } catch (ServiceException se) {
            Assert.assertEquals("parse error: :newname or :newvalue must be provided with replaceHeader", se.getMessage());
        }
    }
}