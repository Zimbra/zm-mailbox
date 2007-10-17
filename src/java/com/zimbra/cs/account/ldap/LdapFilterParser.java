package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.EntrySearchFilter.AndOr;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Operator;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Term;

/*
 * parse string representation of LDAP query into EntrySearchFilter
 */
public class LdapFilterParser {
    
    private static enum FilterType {
        AND,                 // &
        OR,                  // |
        NOT,                 // !
        /*
        EQUALITY_MATCH,      // =
        SUBSTRINGS,          // = [initial] any [final]
        GREATER_OR_EQUAL,    // ~=
        LESS_OR_EQUAL,       // <=
        PRESENT,             // =*
        APPROX_MATCH,        // ~=
        EXTENSIBLE_MATCH     // :=
        */
    }
    
    public static Term parse(String filterStr) throws ServiceException {
        return parse(filterStr, 0, filterStr.length());
    }
   
    private static Term parse(String filterStr, int startPos, int endPos) throws ServiceException {
        String dbgInfo = "filter=" + filterStr + ", startPos=" + startPos + ", endPos=" + endPos;
        
        // sanity check
        if (endPos - startPos <= 0)
            throw ServiceException.PARSE_ERROR(dbgInfo, null);

        // parentheses must be balenced
        if (filterStr.charAt(startPos) == '(') {
            if (filterStr.charAt(endPos-1) == ')') {
                startPos++;
                endPos--;
            } else
                throw ServiceException.PARSE_ERROR("mising parentheses: " + dbgInfo, null);
        }

        // see if it is a compound filter
        char c = filterStr.charAt(startPos);
        if (c == '&')
            return parseCompound(FilterType.AND, filterStr, startPos+1, endPos);
        else if (c == '|')
            return parseCompound(FilterType.OR, filterStr, startPos+1, endPos);
        else if (c == '!')
            return parseCompound(FilterType.NOT, filterStr, startPos+1, endPos);
     
        // if we've gotten here, then it must be a simple filter
        return parseSimple(filterStr, startPos, endPos);
    }
    
    private static Multi parseCompound(FilterType filterType, String filterStr, int startPos, int endPos) throws ServiceException {
        String dbgInfo = "filter=" + filterStr + ", startPos=" + startPos + ", endPos=" + endPos;

        if (startPos == endPos)
            throw ServiceException.PARSE_ERROR(dbgInfo, null);

        // the first and last characters must be parentheses
        if ((filterStr.charAt(startPos) != '(') || (filterStr.charAt(endPos-1) != ')'))
            throw ServiceException.PARSE_ERROR("mising parentheses: " + dbgInfo, null);
        
        // create the Term
        AndOr andOr = (filterType==FilterType.OR)?AndOr.or:AndOr.and;
        boolean negation = (filterType==FilterType.NOT)?true:false;
        Multi multi = new Multi(negation, andOr);
        
        // iterate through the characters in the value.  Whenever an open
        // parenthesis is found, locate the corresponding close parenthesis by
        // counting the number of intermediate open/close parentheses.
        int pendingOpens = 0;
        int openPos = -1;
        for (int i=startPos; i < endPos; i++) {
            char c = filterStr.charAt(i);
            if (c == '(') {
                if (openPos < 0) {
                    openPos = i;
                }
        
                pendingOpens++;
            } else if (c == ')') {
                pendingOpens--;
                if (pendingOpens == 0) {
                    Term subTerm = parse(filterStr, openPos, i+1);
                    multi.add(subTerm);
                    openPos = -1;
                } else if (pendingOpens < 0) {
                    throw ServiceException.PARSE_ERROR("mising open parentheses: " + dbgInfo, null);
                }
            } else if (pendingOpens <= 0) {
                throw ServiceException.PARSE_ERROR("mising parentheses: " + dbgInfo, null);
            }
        }

        if (pendingOpens != 0)
            throw ServiceException.PARSE_ERROR("mising parentheses: " + dbgInfo, null);

        return multi;
    }
    
    private static Single parseSimple(String filterStr, int startPos, int endPos) throws ServiceException {
        
        String dbgInfo = "filter=" + filterStr + ", startPos=" + startPos + ", endPos=" + endPos;
        
        // it must have an equal sign somewhere
        int equalPos = -1;
        for (int i=startPos; i < endPos; i++) {
            if (filterStr.charAt(i) == '=') {
              equalPos = i;
              break;
            }
        }

        if (equalPos == -1 || equalPos == startPos)
            throw ServiceException.PARSE_ERROR(dbgInfo, null);
      
        // determine the filter type by looking at the character immediately before the equal sign
        int attrEndPos;
        Operator op;
        switch (filterStr.charAt(equalPos-1)) {
        case '>':
            op = Operator.ge;
            attrEndPos = equalPos-1;
            break;
        case '<':
            op = Operator.le;
            attrEndPos = equalPos-1;
            break;
        case '~':
            throw ServiceException.PARSE_ERROR("approx match not supported " + dbgInfo, null);
        case ':':
            throw ServiceException.PARSE_ERROR("extensible match not supported " + dbgInfo, null);
        default:
            op = Operator.eq;
            attrEndPos = equalPos;
            break;
        }
        
        // get the attribute type(name)
        String attrName = filterStr.substring(startPos, attrEndPos);
        if (attrName.length()==0)
            throw ServiceException.PARSE_ERROR("missing attr name"+dbgInfo, null);

        // get the attribute value.
        String attrValue = filterStr.substring(equalPos+1, endPos);
        if (attrValue.length()==0)
            throw ServiceException.PARSE_ERROR("missing attr value"+dbgInfo, null);
        
        if (op==Operator.eq) {
            if (attrValue.equals("*")) {
                // treat it as eq and pass thru, we currently don't have a present operator
            } else if (attrValue.startsWith("*") && attrValue.endsWith("*")) {
                if (attrValue.length()>2) {
                    op = Operator.has;
                    attrValue = attrValue.substring(1, attrValue.length()-1);
                } 
                // otherwise treat it as eq and pass thru
            } else if (attrValue.startsWith("*")) {
                op = Operator.endswith;
                attrValue = attrValue.substring(1, attrValue.length());
            } else if (attrValue.endsWith("*")) {
                op = Operator.startswith;
                attrValue = attrValue.substring(0, attrValue.length()-1);
            }
        }
        
        return new Single(false, attrName, op, attrValue);
      
    }
    
    public static String test(String inFilterStr) {
        String outFilterStr = LdapEntrySearchFilter.toLdapIDNFilter(inFilterStr);
               
        System.out.println("In: " + inFilterStr);
        System.out.println("Out: " + outFilterStr);
        System.out.println();
        return outFilterStr;
    }
    
    public static void main(String[] args) {
        
        test("!(zimbraDomainName=*\u4e2d\u6587*)");

        test("!(objectClass=*)");
        test("!(objectClass=**)");
        test("!(objectClass=*abc)");
        test("!(objectClass=abc*)");
        test("!(objectClass=*abc*)");
        
        test("(|(zimbraMailDeliveryAddress=*@test.\u4e2d\u6587.com)(zimbraMailAlias=*@test.\u4e2d\u6587.com))");
    }

}
