package com.zimbra.cs.gal;

import java.util.Set;
import java.util.Stack;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.mailbox.Contact;

public class FilteredGalSearchResultCallback extends GalSearchResultCallback {

    Set<String> mAttrs;
    EntrySearchFilter mFilter;
    
    public FilteredGalSearchResultCallback(GalSearchParams params, EntrySearchFilter filter, Set<String> attrs) {
        super(params);
        mAttrs = attrs;
        mFilter = filter;
    }

    public Element handleContact(Contact contact) throws ServiceException {
        if (matched(contact))
            com.zimbra.cs.service.account.ToXML.encodeCalendarResource(getResponse(), 
                    mFormatter.formatItemId(contact), contact.get(ContactConstants.A_email), 
                    contact.getAllFields(), mAttrs, null);
        return null; // return null because we don't want the sort field (sf) attr added to each hit
    }
    
    public void handleContact(GalContact galContact) throws ServiceException {
        if (matched(galContact))
            com.zimbra.cs.service.account.ToXML.encodeCalendarResource(getResponse(), 
                    galContact.getId(), galContact.getSingleAttr("email"), 
                    galContact.getAttrs(), mAttrs, null);
    }
    
    private boolean matched(Contact c) {
        FilterVisitor visitor = new FilterVisitor(c);
        return evaluate(visitor);
    }
    
    private boolean matched(GalContact c) {
        FilterVisitor visitor = new FilterVisitor(c);
        return evaluate(visitor);
    }
    
    private boolean evaluate(FilterVisitor visitor) {
        mFilter.traverse(visitor);
        return visitor.getResult();
    }
    
    private static interface KeyValue {
        // returns a String or String[]
        public Object get(String key);
    }
    
    private static class ContactKV implements KeyValue {
        
        Contact mContact;
        
        private ContactKV(Contact contact) {
            mContact = contact;
        }
        
        public Object get(String key) {
            return mContact.get(key);
        }
    }
    
    private static class GalContactKV implements KeyValue {
        
        GalContact mGalContact;
        
        private GalContactKV(GalContact galContact) {
            mGalContact = galContact;
        }
        
        public Object get(String key) {
            return mGalContact.getAttrs().get(key);
        }
    }
    
    private static class FilterVisitor implements Visitor {

        private static class Result {
            Multi mTerm;
            Boolean mCurResult;
            
            private Result(Multi term) {
                mTerm = term;
            }

            private Result(boolean result) {
                setResult(result);
            }
            
            Multi getTerm() {
                return mTerm;
            }
            private Boolean getResult() {
                return mCurResult;
            }
            
            private void setResult(boolean result) {
                mCurResult = result;
            }
            
            private void negateResult() {
                if (mCurResult != null)
                    mCurResult = !mCurResult;
            }
            
        }
        
        KeyValue mContact;
        Stack<Result> mParentResult;
        
        private FilterVisitor(Contact contact) {
            mContact = new ContactKV(contact);
            mParentResult = new Stack<Result>();
        }
        
        private FilterVisitor(GalContact galContact) {
            mContact = new GalContactKV(galContact);
            mParentResult = new Stack<Result>();
        }
        
        boolean getResult() {
            // there should one and only one item in the stack
            return mParentResult.pop().getResult().booleanValue();
        }
        @Override
        public void enterMulti(Multi term) {
            mParentResult.push(new Result(term));
        }

        @Override
        public void leaveMulti(Multi term) {
            // we must have a result by now
            Result thisTerm = mParentResult.pop(); // this is us
            if (thisTerm.getTerm().isNegation()) {
                thisTerm.negateResult();
            }
                
            // propagate this Term's result to its parent if there is one
            if (!mParentResult.empty()) {
                // have a parent
                Result parent = mParentResult.peek();
                Boolean parentResult = parent.getResult();
                if (parentResult == null || // we are the first child
                    (parentResult == Boolean.TRUE  && parent.getTerm().isAnd()) ||
                    (parentResult == Boolean.FALSE && !parent.getTerm().isAnd())) {
                    parent.setResult(thisTerm.getResult());
                }
            } else {
                // we are the top, push it back on the stack
                mParentResult.push(thisTerm);
            }
        }

        @Override
        public void visitSingle(Single term) {
            if (!mParentResult.empty()) {
                // have a parent
                Result parent = mParentResult.peek();
                Boolean parentResult = parent.getResult();
                if (parentResult == null || // we are the first child
                    (parentResult == Boolean.TRUE  && parent.getTerm().isAnd()) ||
                    (parentResult == Boolean.FALSE && !parent.getTerm().isAnd())) {
                    parent.setResult(evaluate(term));
                }
                // short-circuit it, no need to evaluate this single term, 
                // since it cannot affect the final result if we are here
            } else {
                // no parent, we are the only Term, evaluate and 
                // remember the result (push to the stack)
                mParentResult.push(new Result(evaluate(term)));
            }
        }
        
        private boolean evaluate(Single term) {
            String opAttr = term.getLhs();
            
            Object value = mContact.get(opAttr);
            if (value instanceof String[]) {
                for (String v : (String[])value) {
                    if (shouldInclude(term, v))
                        return true;
                }
                return false;
            } else if (value != null) {
                return shouldInclude(term, value.toString());
            } else {
                return false;
            }
        }
        
        private boolean shouldInclude(Single term, String value) {
            Operator op = term.getOperator();
            String opVal = term.getRhs();
            boolean result = true;
            
            if (op.equals(Operator.has)) {
                result = (value == null) ? false : value.toLowerCase().contains(opVal.toLowerCase());
            } else if (op.equals(Operator.eq)) {
                result = (value == null) ? false : value.toLowerCase().equals(opVal.toLowerCase());
            } else if (op.equals(Operator.ge)) {
                // always use number comparison
                result = (value == null) ? false : Integer.valueOf(value) >= Integer.valueOf(opVal);
            } else if (op.equals(Operator.le)) {
                // always use number comparison
                result = (value == null) ? false : Integer.valueOf(value) <= Integer.valueOf(opVal);
            } else if (op.equals(Operator.startswith)) {
                result = (value == null) ? false : value.toLowerCase().startsWith(opVal.toLowerCase());
            } else if (op.equals(Operator.endswith)) {
                result = (value == null) ? false : value.toLowerCase().endsWith(opVal.toLowerCase());
            } else {
                // fallback to EQUALS
                result = (value == null) ? false : value.toLowerCase().equals(opVal.toLowerCase());
            }

            if (term.isNegation()) 
                return !result;
            else
                return result;
        }
    }
}
