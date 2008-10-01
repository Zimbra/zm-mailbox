package com.zimbra.cs.filter;

import java.util.Date;
import java.util.List;

import org.apache.jsieve.parser.generated.Node;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element.ElementFactory;

/**
 * Converts a Sieve node tree to the SOAP representation of
 * filter rules.
 */
public class SieveToSoap extends SieveVisitor {

    private Element mRoot;
    private List<String> mRuleNames;
    private Element mCurrentRule;
    private int mCurrentRuleIndex = 0;
    
    public SieveToSoap(ElementFactory factory, List<String> ruleNames) {
        mRoot = factory.createElement(MailConstants.E_FILTER_RULES);
        mRuleNames = ruleNames;
    }
    
    public Element getRootElement() {
        return mRoot;
    }
    
    @Override
    protected void visitRule(Node ruleNode, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.end) {
            return;
        }
        
        // rule element
        mCurrentRule = mRoot.addElement(MailConstants.E_FILTER_RULE);
        String name = getCurrentRuleName();
        if (name != null) {
            mCurrentRule.addAttribute(MailConstants.A_NAME, name);
        }
        if (props.isEnabled) {
            mCurrentRule.addAttribute(MailConstants.A_ENABLED, "1");
        } else {
            mCurrentRule.addAttribute(MailConstants.A_ENABLED, "0");
        }

        // filterTests element
        Element filterTests = mCurrentRule.addElement(MailConstants.E_FILTER_TESTS);
        if (props.conditionType == ConditionType.allof) {
            filterTests.addAttribute(MailConstants.A_CONDITION, "allOf");
        } else if (props.conditionType == ConditionType.anyof) {
            filterTests.addAttribute(MailConstants.A_CONDITION, "anyOf");
        }
        
        // filterActions element
        mCurrentRule.addElement(MailConstants.E_FILTER_ACTIONS);

        mCurrentRuleIndex++;
    }
    
    private Element addTest(String elementName)
    throws ServiceException {
        return mCurrentRule.getElement(MailConstants.E_FILTER_TESTS).addElement(elementName);
    }
    
    private Element addAction(String elementName)
    throws ServiceException {
        return mCurrentRule.getElement(MailConstants.E_FILTER_ACTIONS).addElement(elementName);
    }
    
    @Override
    protected void visitAttachmentTest(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_ATTACHMENT_TEST);
        }
    }

    @Override
    protected void visitBodyTest(Node node, VisitPhase phase, RuleProperties props, String value) 
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addTest(MailConstants.E_BODY_TEST).addAttribute(MailConstants.A_VALUE, value);
        }
    }

    @Override
    protected void visitDateTest(Node node, VisitPhase phase, RuleProperties props,
                                 DateComparison comparison, Date date)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_DATE_TEST);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, comparison.toString());
            test.addAttribute(MailConstants.A_DATE, date.getTime() / 1000);
        }
    }

    @Override
    protected void visitHeaderExistsTest(Node node, VisitPhase phase, RuleProperties props,
                                         String header)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_HEADER_EXISTS_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
        }
    }

    @Override
    protected void visitHeaderTest(Node node, VisitPhase phase, RuleProperties props,
                                   String header, StringComparison comparison, String value)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_HEADER_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
            test.addAttribute(MailConstants.A_STRING_COMPARISON, comparison.toString());
            test.addAttribute(MailConstants.A_VALUE, value);
        }
    }

    @Override
    protected void visitSizeTest(Node node, VisitPhase phase, RuleProperties props,
                                 NumberComparison comparison, int size)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            Element test = addTest(MailConstants.E_SIZE_TEST);
            test.addAttribute(MailConstants.A_NUMBER_COMPARISON, comparison.toString());
            test.addAttribute(MailConstants.A_SIZE, size);
        }
    }

    private String getCurrentRuleName() {
        if (mRuleNames == null || mCurrentRuleIndex >= mRuleNames.size()) {
            return null;
        }
        return mRuleNames.get(mCurrentRuleIndex);
    }

    @Override
    protected void visitDiscardAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_DISCARD);
        }
    }

    @Override
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props,
                                       String folderPath) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_FILE_INTO).addAttribute(MailConstants.A_FOLDER_PATH, folderPath);
        }
    }

    @Override
    protected void visitFlagAction(Node node, VisitPhase phase, RuleProperties props, Flag flag)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_FILE_INTO).addAttribute(MailConstants.A_FLAG_NAME, flag.toString());
        }
    }

    @Override
    protected void visitKeepAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_KEEP);
        }
    }

    @Override
    protected void visitRedirectAction(Node node, VisitPhase phase, RuleProperties props,
                                       String address) throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_REDIRECT).addAttribute(MailConstants.A_ADDRESS, address);
        }
    }

    @Override
    protected void visitStopAction(Node node, VisitPhase phase, RuleProperties props)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_STOP);
        }
    }

    @Override
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tagName)
    throws ServiceException {
        if (phase == VisitPhase.begin) {
            addAction(MailConstants.E_ACTION_TAG).addAttribute(MailConstants.A_TAG_NAME, tagName);
        }
    }
    
    /*
    @Override
    protected void visitTest(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.END) {
            return;
        }
        
        SieveNode sieveNode = (SieveNode) node;
        Element test = null;
        
        if ("header".equals(sieveNode.getName())) {
            String comparison = stripLeadingColon(getValue(node, 0, 0));
            String header = stripQuotes(getValue(node, 0, 1, 0, 0));
            String value = stripQuotes(getValue(node, 0, 2, 0, 0));

            test = mFactory.createElement(MailConstants.E_HEADER_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
            test.addAttribute(MailConstants.A_STRING_COMPARISON, comparison);
            test.addAttribute(MailConstants.A_VALUE, value);
        } else if ("exists".equals(sieveNode.getName())) {
            String header = stripQuotes(getValue(node, 0, 0, 0, 0));
            
            test = mFactory.createElement(MailConstants.E_EXISTS_TEST);
            test.addAttribute(MailConstants.A_HEADER, header);
        } else if ("size".equals(sieveNode.getName())) {
            String comparison = stripLeadingColon(getValue(node, 0, 0));
            String size = getValue(node, 0, 1);
            
            test = mFactory.createElement(MailConstants.E_SIZE_TEST);
            test.addAttribute(MailConstants.A_NUMBER_COMPARISON, comparison);
            test.addAttribute(MailConstants.A_SIZE, size);
        } else if ("date".equals(sieveNode.getName())) {
            String comparison = stripLeadingColon(getValue(node, 0, 0));
            String date = stripQuotes(getValue(node, 0, 1, 0, 0));
            
            test = mFactory.createElement(MailConstants.E_DATE_TEST);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, comparison);
            test.addAttribute(MailConstants.A_DATE, date);
        } else if ("body".equals(sieveNode.getName())) {
            String value = stripQuotes(getValue(node, 0, 1, 0, 0));
            
            test = mFactory.createElement(MailConstants.E_BODY_TEST);
            test.addAttribute(MailConstants.A_VALUE, value);
        } else if ("attachment".equals(sieveNode.getName())) {
            test = mFactory.createElement(MailConstants.E_ATTACHMENT_TEST);
        } else {
            ZimbraLog.filter.info("Unrecognized test: %s.  Not converting to SOAP.", sieveNode.getName());
            return;
        }

        if (props.isNegativeTest) {
            test.addAttribute(MailConstants.A_NEGATIVE, true);
        }
        mCurrentTests.add(test);
    }
    */
    
}
