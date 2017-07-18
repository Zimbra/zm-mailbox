package com.zimbra.soap.mail.type;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.jsieve.comparators.ComparatorNames;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.StringUtil;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "headerName", "headerValue" })
@JsonPropertyOrder({ "matchType", "countComparator", "valueComparator", "relationalComparator", "comparator", "headerName", "headerValue" })
public class EditheaderTest {
    /**
     * @zm-api-field-tag matchType
     * @zm-api-field-description matchType - <b>is|contains|matches|count|value</b>
     */
    @XmlAttribute(name=AdminConstants.A_MATCHTYPE /* matchType */, required=false)
    private String matchType;
    /**
     * @zm-api-field-tag count
     * @zm-api-field-description if true count comparison will be done
     */
    @XmlAttribute(name=AdminConstants.A_COUNT_COMPARATOR /* countComparator */, required=false)
    private Boolean count;
    /**
     * @zm-api-field-tag count
     * @zm-api-field-description if true count comparison will be done
     */
    @XmlAttribute(name=AdminConstants.A_VALUE_COMPARATOR /* valueComparator */, required=false)
    private Boolean value;
    /**
     * @zm-api-field-tag relationalComparator
     * @zm-api-field-description relational comparator - <b>gt|ge|lt|le|eq|ne</b>
     */
    @XmlAttribute(name=AdminConstants.A_RELATIONAL_COMPARATOR /* relationalComparator */, required=false)
    private String relationalComparator;
    /**
     * @zm-api-field-tag comparator
     * @zm-api-field-description comparator - <b>i;ascii-casemap|i;ascii-numeric|i;octet</b>
     */
    @XmlAttribute(name=AdminConstants.A_COMPARATOR /* comparator */, required=false)
    private String comparator;
    /**
     * @zm-api-field-tag headerName
     * @zm-api-field-description name of the header to be compared
     */
    @XmlElement(name=AdminConstants.E_HEADERNAME /* headerName */, required=true)
    private String headerName;
    /**
     * @zm-api-field-tag headerValue
     * @zm-api-field-description value of the header to be compared
     */
    @XmlElement(name=AdminConstants.E_HEADERVALUE /* headerValue */, required=true)
    private List<String> headerValue;

    @SuppressWarnings("unused")
    private EditheaderTest() {
        // private constructor so that no one can call it.
    }

    /**
     * @param matchType
     * @param count
     * @param value
     * @param relationalComparator
     * @param comparator
     * @param headerName
     * @param headerValue
     */
    public EditheaderTest(String matchType, Boolean count, Boolean value, String relationalComparator,
            String comparator, String headerName, List<String> headerValue) {
        this.matchType = matchType;
        this.count = count;
        this.value = value;
        this.relationalComparator = relationalComparator;
        this.comparator = comparator;
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    /**
     * @return the matchType
     */
    public String getMatchType() {
        return matchType;
    }

    /**
     * @param matchType the matchType to set
     */
    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    /**
     * @return the count
     */
    public Boolean getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(Boolean count) {
        this.count = count;
    }

    /**
     * @return the value
     */
    public Boolean getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Boolean value) {
        this.value = value;
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
     * @return the headerName
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * @param headerName the headerName to set
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /**
     * @return the headerValue
     */
    public List<String> getHeaderValue() {
        return headerValue;
    }

    /**
     * @param headerValue the headerValue to set
     */
    public void setHeaderValue(List<String> headerValue) {
        this.headerValue = headerValue;
    }

    /**
     * @param headerValue the headerValue to add
     */
    public void addHeaderValue(String headerValue) {
        if (this.headerValue == null) {
            this.headerValue = new ArrayList<String>();
        }
        this.headerValue.add(headerValue);
    }

    /**
     * @param headerValue the headerValues to add
     */
    public void addAllHeaderValues(List<String> headerValue) {
        if (this.headerValue == null) {
            this.headerValue = new ArrayList<String>();
        }
        this.headerValue.addAll(headerValue);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("matchType", matchType)
            .add("count", count)
            .add("value", value)
            .add("relationalComparator", relationalComparator)
            .add("comparator", comparator)
            .add("headerName", headerName)
            .add("headerValue", headerValue)
            .toString();
    }

    public void validateEditheaderTest() throws ServiceException {
        if (StringUtil.isNullOrEmpty(headerName)) {
            throw ServiceException.PARSE_ERROR("Invalid EditheaderTest: Missing headerName", null);
        }
        if (comparator == null && headerValue != null && !headerValue.isEmpty()) {
            comparator = ComparatorNames.ASCII_CASEMAP_COMPARATOR;
        }
        if (count != null && !count) {
            count = null;
        }
        if (value != null && !value) {
            value = null;
        }
        if (count != null && count && value != null && value) {
            throw ServiceException.PARSE_ERROR(":count and :value, both can not be received with EditheaderTest", null);
        }
        if (((count != null && count) || (value != null && value)) && relationalComparator == null) {
            throw ServiceException.PARSE_ERROR("relational comparator not received with EditheaderTest", null);
        }
        if (matchType == null && count == null && value == null && headerValue != null && !headerValue.isEmpty()) {
            matchType = MatchTypeTags.IS_TAG.substring(1); // remove preceding ":"
        }
        if (((count != null && count) || (value != null && value)) && matchType != null) {
            throw ServiceException.PARSE_ERROR(":count or :value can not be used with matchType in EditheaderTest", null);
        }
        if ((count != null && count) && !comparator.equals(HeaderConstants.I_ASCII_NUMERIC)) {
            throw ServiceException.PARSE_ERROR(":count can be used only with \"" + HeaderConstants.I_ASCII_NUMERIC +"\" in EditheaderTest", null);
        }
    }
}
