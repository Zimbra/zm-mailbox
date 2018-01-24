/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016, 2017 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.NumberArgument;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractCommand;
import org.apache.jsieve.comparators.ComparatorNames;
import org.apache.jsieve.comparators.ComparatorUtils;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.LookupException;
import org.apache.jsieve.exception.OperationException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.tests.ComparatorTags;

import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraComparatorUtils;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter.PARSESTATUS;

public class EditHeaderExtension {
    // index & last
    private Integer index = null;
    private boolean last = false;
    // newname and newvalue
    private String newName = null;
    private String newValue = null;
    // comparator
    private String comparator = null;
    // match-type
    private boolean contains = false;
    private boolean is = false;
    private boolean matches = false;

    // Constructors
    public EditHeaderExtension() {
        super();
    }

    /**
     * @return the index
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * @return the last
     */
    public boolean isLast() {
        return last;
    }

    /**
     * @param last the last to set
     */
    public void setLast(boolean last) {
        this.last = last;
    }

    /**
     * @return the newName
     */
    public String getNewName() {
        return newName;
    }

    /**
     * @param newName the newName to set
     */
    public void setNewName(String newName) {
        this.newName = newName;
    }

    /**
     * @return the newValue
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * @param newValue the newValue to set
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    /**
     * @return the comparator
     */
    public String getComparator() {
        return comparator;
    }

    /**
     * @param comparator the comparator to set
     */
    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    /**
     * @return the contains
     */
    public boolean isContains() {
        return contains;
    }

    /**
     * @param contains the contains to set
     */
    public void setContains(boolean contains) {
        this.contains = contains;
    }

    /**
     * @return the is
     */
    public boolean isIs() {
        return is;
    }

    /**
     * @param is the is to set
     */
    public void setIs(boolean is) {
        this.is = is;
    }

    /**
     * @return the matches
     */
    public boolean isMatches() {
        return matches;
    }

    /**
     * @param matches the matches to set
     */
    public void setMatches(boolean matches) {
        this.matches = matches;
    }

    /**
     * @return the countTag
     */
    public boolean isCountTag() {
        return countTag;
    }

    /**
     * @param countTag the countTag to set
     */
    public void setCountTag(boolean countTag) {
        this.countTag = countTag;
    }

    /**
     * @return the valueTag
     */
    public boolean isValueTag() {
        return valueTag;
    }

    /**
     * @param valueTag the valueTag to set
     */
    public void setValueTag(boolean valueTag) {
        this.valueTag = valueTag;
    }

    /**
     * @return the relationalComparator
     */
    public String getRelationalComparator() {
        return relationalComparator;
    }

    /**
     * @param relationalComparator the relationalComparator to set
     */
    public void setRelationalComparator(String relationalComparator) {
        this.relationalComparator = relationalComparator;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the valueList
     */
    public List<String> getValueList() {
        return valueList;
    }

    /**
     * @param valueList the valueList to set
     */
    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    private boolean countTag = false;
    private boolean valueTag = false;
    private String relationalComparator = null;
    // key and valuelist
    private String key = null;
    private List<String> valueList = null;

    // Utility methods
    /**
     * This method sets values provided with replaceheader or deleteheader in <b>EditHeaderExtension</b> object.
     * @param arguments
     * @param ac
     * @throws SyntaxException
     * @throws OperationException
     */
    public void setupEditHeaderData(Arguments arguments, AbstractCommand ac) throws SyntaxException, OperationException {
        // set up class variables
        Iterator<Argument> itr = arguments.getArgumentList().iterator();
        while (itr.hasNext()) {
            Argument arg = itr.next();
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                if (tag.is(HeaderConstants.INDEX)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof NumberArgument) {
                            this.index = ((NumberArgument) arg).getInteger();
                        } else {
                            throw new SyntaxException("Invalid index provided with replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(HeaderConstants.LAST)) {
                    this.last = true;
                } else if (tag.is(HeaderConstants.NEW_NAME)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            String origNewName = sla.getList().get(0);
                            if (StringUtil.isNullOrEmpty(origNewName)) {
                                throw new SyntaxException("New name must be present with :newname in replaceheader : " + arg);
                            }
                            this.newName = origNewName;
                        } else {
                            throw new SyntaxException("New name not provided with :newname in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(HeaderConstants.NEW_VALUE)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            this.newValue = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("New value not provided with :newValue in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(HeaderConstants.COUNT)) {
                    if (this.valueTag) {
                        throw new SyntaxException(":count and :value both can not be used with replaceheader");
                    }
                    this.countTag =true;
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            this.relationalComparator = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("Relational comparator not provided with :count in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(HeaderConstants.VALUE)) {
                    if (this.countTag) {
                        throw new SyntaxException(":count and :value both can not be used with replaceheader");
                    }
                    this.valueTag = true;
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            this.relationalComparator = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("Relational comparator not provided with :value in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(ComparatorTags.COMPARATOR_TAG)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            StringListArgument sla = (StringListArgument) arg;
                            this.comparator = sla.getList().get(0);
                        } else {
                            throw new SyntaxException("Comparator not provided with :comparator in replaceheader : " + arg);
                        }
                    }
                } else if (tag.is(MatchTypeTags.CONTAINS_TAG)) {
                    this.contains = true;
                } else if (tag.is(MatchTypeTags.IS_TAG)) {
                    this.is = true;
                } else if (tag.is(MatchTypeTags.MATCHES_TAG)) {
                    this.matches = true;
                } else {
                    throw new SyntaxException("Invalid tag argument provided with replaceheader.");
                }
            } else if (arg instanceof StringListArgument) {
                if (ac instanceof ReplaceHeader) {
                    StringListArgument sla = (StringListArgument) arg;
                    this.key = sla.getList().get(0);
                    if (itr.hasNext()) {
                        arg = itr.next();
                        sla = (StringListArgument) arg;
                        this.valueList = sla.getList();
                    }
                } else if (ac instanceof DeleteHeader) {
                    StringListArgument sla = (StringListArgument) arg;
                    this.key = sla.getList().get(0);
                    if (itr.hasNext()) {
                        arg = itr.next();
                        sla = (StringListArgument) arg;
                        this.valueList = sla.getList();
                    } else {
                        ZimbraLog.filter.info("Value for " + this.key + " is not provided in deleteheader. So all headers with this key will be deleted.");
                    }
                } else {
                    throw new OperationException("Invalid instance of AbstractCommand is obtained.");
                }
            } else {
                ZimbraLog.filter.info("Unknown argument provided: " + arg.getValue());
            }
        }

        if (!(isIs() || isContains() || isMatches() || isCountTag() || isValueTag())) {
           this.is = true;
        }
    }

    /**
     * This method decides whether header needs to be replaced or not
     * @param mailAdapter 
     * @param header : current instance of header
     * @param headerCount : count of matching header
     * @param value : current value passed in valueList with replaceheader
     * @param context : sieve context object
     * @return true if header needs to be replaced
     * @throws SieveException 
     * @throws LookupException 
     * @throws MessagingException 
     */
    public boolean matchCondition(ZimbraMailAdapter mailAdapter, Header header, List<String> headerList, String value, SieveContext context) throws LookupException, SieveException, MessagingException {
        boolean matchFound = false;
        String unfoldedAndDecodedHeaderValue = MimeUtility.unfold(header.getValue());
        try {
            unfoldedAndDecodedHeaderValue =  MimeUtility.decodeText(unfoldedAndDecodedHeaderValue);
            ZimbraLog.filter.debug("Header value after unfolding and decoding: %s", unfoldedAndDecodedHeaderValue);
        } catch (UnsupportedEncodingException uee) {
         // value would contain any un-decodable value, fine
        }

        if (this.valueTag) {
            matchFound = ZimbraComparatorUtils.values(mailAdapter, comparator, relationalComparator, unfoldedAndDecodedHeaderValue, value, context);
        } else if (this.countTag) {
            matchFound = ZimbraComparatorUtils.counts(mailAdapter, comparator, relationalComparator, headerList, value, context);
        } else if (this.is && ComparatorUtils.is(comparator, unfoldedAndDecodedHeaderValue, value, context)) {
            matchFound = true;
        } else if (this.contains && ComparatorUtils.contains(comparator, unfoldedAndDecodedHeaderValue, value, context)) {
            matchFound = true;
        } else if (this.matches && ComparatorUtils.matches(comparator, unfoldedAndDecodedHeaderValue, value, context)) {
            matchFound = true;
        } else {
            ZimbraLog.filter.debug("Key: %s and Value: %s pair not matching requested criteria.", this.key, value);
        }
        List<String> keyList = new ArrayList<String>();
        keyList.add(this.key);
        HeaderTest.evaluateVarExp(mailAdapter, keyList, HeaderTest.SourceType.HEADER, this.valueList);
        return matchFound;
    }

    /**
     * This method sets effective index of the header
     * @param headerCount : <b>int</b>
     */
    public void setEffectiveIndex(int headerCount) {
        if (this.last && headerCount >= this.index) {
            if (this.index == 0) {
                this.index = headerCount - this.index;
            } else  {
                this.index = headerCount - this.index + 1;
            }
        }
    }

    /**
     * Replace sieve variables with their values in <b>valueList</b>
     * @param mailAdapter : Object of <b>ZimbraMailAdapter</b>
     * @throws SyntaxException 
     */
    public void replaceVariablesInValueList(ZimbraMailAdapter mailAdapter) throws SyntaxException {
        List<String> temp = new ArrayList<String>();
        if (this.valueList != null && !this.valueList.isEmpty()) {
            for (String value : this.valueList) {
                temp.add(FilterUtil.replaceVariables(mailAdapter, value));
            }
        }
        this.valueList = temp;
    }

    /**
     * Replace sieve variables with their value in <b>key</b>
     * @param mailAdapter : Object of <b>ZimbraMailAdapter</b>
     * @throws SyntaxException 
     */
    public void replaceVariablesInKey(ZimbraMailAdapter mailAdapter) throws SyntaxException {
        if (this.key != null) {
            this.key = FilterUtil.replaceVariables(mailAdapter, key);
        }
    }

    /**
     * Common validation for replaceheader and deleteheader
     * @throws SyntaxException
     */
    public void commonValidation(String operation) throws SyntaxException {
        if(StringUtil.isNullOrEmpty(operation)) {
            operation = "EditHeaderExtension";
        }

        if (!StringUtil.isNullOrEmpty(this.key)) {
            if (!CharsetUtil.US_ASCII.equals(CharsetUtil.checkCharset(this.key, CharsetUtil.US_ASCII))) {
                throw new SyntaxException(operation +":Header name must be printable ASCII only.");
            }
        } else {
            throw new SyntaxException(operation +":Header name must be present.");
        }
        // relation comparator must be valid
        if (this.relationalComparator != null) {
            if (!(this.relationalComparator.equals(HeaderConstants.GT_OP)
                    || this.relationalComparator.equals(HeaderConstants.GE_OP)
                    || this.relationalComparator.equals(HeaderConstants.LT_OP)
                    || this.relationalComparator.equals(HeaderConstants.LE_OP)
                    || this.relationalComparator.equals(HeaderConstants.EQ_OP)
                    || this.relationalComparator.equals(HeaderConstants.NE_OP))) {
                throw new SyntaxException(operation +":Invalid relational comparator provided.");
            }
        }
        // comparator must be valid and if not set, then set to default i.e. ComparatorNames.ASCII_CASEMAP_COMPARATOR
        if (this.comparator != null) {
            if (!(this.comparator.equals(HeaderConstants.I_ASCII_NUMERIC)
                    || this.comparator.equals(ComparatorNames.OCTET_COMPARATOR)
                    || this.comparator.equals(ComparatorNames.ASCII_CASEMAP_COMPARATOR)
                    )) {
                throw new SyntaxException(operation +":Invalid comparator type provided");
            }
        } else {
            this.comparator = ComparatorNames.ASCII_CASEMAP_COMPARATOR;
            ZimbraLog.filter.info("No comparator type provided, so setting to default %s", ComparatorNames.ASCII_CASEMAP_COMPARATOR);
        }
        // relational comparator must be available with numeric comparison
        if (this.comparator.equals(HeaderConstants.I_ASCII_NUMERIC) && !(this.countTag || this.valueTag || this.is)) {
            throw new SyntaxException(operation +":No valid comparator (:value, :count or :is) found for numeric operation.");
        }
        // throw exception if last is provided but index is missing(as per rfc)
        if (this.index == null && this.last) {
            throw new SyntaxException(":index <offset> must be provided with :last");
        }
    }

    /**
     * @param mm
     * @return
     * @throws OperationException 
     */
    public List<String> getMatchingHeaders(MimeMessage mm) throws OperationException {
        List<String> headerList = new ArrayList<String>();
        try {
            String[] headerValues = mm.getHeader(this.key);
            if (headerValues != null) {
                headerList = Arrays.asList(headerValues);
            }
        } catch (MessagingException e) {
            throw new OperationException("Error occured while fetching " + this.key + " headers from mime.", e);
        }
        return headerList;
    }

    /**
     * This method verifies if the key is set for immutable header or not.
     * @return <b>true</b> if immutable header found else <b>false</b>
     */
    static public boolean isImmutableHeaderKey(String key, ZimbraMailAdapter mailAdapter) {
        List<String> immutableHeaders = Arrays.asList(mailAdapter.getAccount().getSieveImmutableHeaders().split(","));
        return immutableHeaders.stream().map(String::trim).anyMatch(x -> x.equalsIgnoreCase(key));
    }

    static public boolean saveChanges(ZimbraMailAdapter zma, String actionName, MimeMessage mm) {
        if (zma.getEditHeaderParseStatus() == PARSESTATUS.UNKNOWN) {
            try {
                mm.saveChanges();
            } catch (ParseException pe) {
                ZimbraLog.filter.info(actionName + ": malformed original message: " + pe.getMessage());
            } catch (Exception e) {
                ZimbraLog.filter.info(actionName + ": parse error:" + e.getMessage());
                zma.setEditHeaderParseStatus(PARSESTATUS.MIMEMALFORMED);
                return false;
            }
            zma.setEditHeaderParseStatus(PARSESTATUS.TOLERABLE);
            return true;
        } else if (zma.getEditHeaderParseStatus() == PARSESTATUS.MIMEMALFORMED) {
            ZimbraLog.filter.debug(actionName + ": Triggering message is malformed MIME");
            return false;
        }
        return true;
    }
}
