package com.zimbra.cs.ldap.unboundid;

import static com.unboundid.util.Debug.debugException;

import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1Exception;
import com.unboundid.asn1.ASN1Integer;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DecodeableControl;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;

/**
 * 
 * @author pshao
 *
 */
public final class NoopSearchControl extends Control implements DecodeableControl {

    /**
     * The OID (2.16.840.1.113730.3.4.4) for the OpenLDAP noop search request control.
     */
    public static final String NOOP_SEARCH_OID = "1.3.6.1.4.1.4203.666.5.18";

    private final long count;
    
    enum ControlMessages {
        ERR_NOOP_SEARCH_NO_VALUE("No control value was provided, so it could not be decoded as a noop search response control."),
        ERR_NOOP_SEARCH_NOT_SEQUENCE("Unable to decode the value of the provided control as an noop search control sequence:  %s"),
        ERR_NOOP_SEARCH_INVALID_ELEMENT_COUNT("Unexpected number of elements in the noop search value sequence (expected 3, got %d)."),
        ERR_NOOP_SEARCH_ELEMENT_NOT_INTEGER("Unable to decode the first element of the simple paged results value sequence as an integer: %s"),
        ERR_NOOP_SEARCH_UNEXPECTED_SEARCH_RESULT_CODE("Unexpected search result code: [%s].");
        
        // The default text for this message
        private final String defaultText;

        private ControlMessages(final String defaultText) {
          this.defaultText = defaultText;
        }
        
        private String get() {
            return defaultText;
        }
        
        public String get(final Object... args) {
            return String.format(get(), args);
        }

    }
    
    public NoopSearchControl() {
        super(NOOP_SEARCH_OID, false);
        count = 0;
    }

    public NoopSearchControl(final String oid, final boolean isCritical,
            final ASN1OctetString value)
    throws LDAPException {
        super(oid, isCritical, value);

        if (value == null) {
            throw new LDAPException(ResultCode.DECODING_ERROR,
                    ControlMessages.ERR_NOOP_SEARCH_NO_VALUE.get());
        }

        final ASN1Sequence valueSequence;
        try {
            final ASN1Element valueElement = ASN1Element.decode(value.getValue());
            valueSequence = ASN1Sequence.decodeAsSequence(valueElement);
        } catch (final ASN1Exception ae) {
            debugException(ae);
            throw new LDAPException(ResultCode.DECODING_ERROR,
                    ControlMessages.ERR_NOOP_SEARCH_NOT_SEQUENCE.get(ae), ae);
        }

        final ASN1Element[] valueElements = valueSequence.elements();
        if (valueElements.length != 3) {
            throw new LDAPException(ResultCode.DECODING_ERROR,
                    ControlMessages.ERR_NOOP_SEARCH_INVALID_ELEMENT_COUNT.get(
                            valueElements.length));
        }

        int searchResultCode;
        try {
            searchResultCode = ASN1Integer.decodeAsInteger(valueElements[0]).intValue();
            
        } catch (final ASN1Exception ae) {
            debugException(ae);
            throw new LDAPException(ResultCode.DECODING_ERROR,
                    ControlMessages.ERR_NOOP_SEARCH_ELEMENT_NOT_INTEGER.get(ae), ae);
        }

        if (searchResultCode != ResultCode.SUCCESS.intValue()) {
            ResultCode resultCode = ResultCode.valueOf(searchResultCode);
            throw new LDAPException(resultCode,
                    ControlMessages.ERR_NOOP_SEARCH_UNEXPECTED_SEARCH_RESULT_CODE.get(resultCode.toString()));
        }
        
        try {
            count = ASN1Integer.decodeAsInteger(valueElements[1]).intValue();
        } catch (final ASN1Exception ae) {
            debugException(ae);
            throw new LDAPException(ResultCode.DECODING_ERROR,
                    ControlMessages.ERR_NOOP_SEARCH_ELEMENT_NOT_INTEGER.get(ae), ae);
        }
        
        // not used for now
        /*
        int searchRefs;
        try {
            searchRefs = ASN1Integer.decodeAsInteger(valueElements[2]).intValue();
        } catch (final ASN1Exception ae) {
            debugException(ae);
            throw new LDAPException(ResultCode.DECODING_ERROR,
                    ControlMessages.ERR_NOOP_SEARCH_ELEMENT_NOT_INTEGER.get(ae), ae);
        }
        */
    }
    
    public static NoopSearchControl get(final SearchResult result)
    throws LDAPException {
        final Control c = result.getResponseControl(NOOP_SEARCH_OID);
        if (c == null) {
            return null;
        }
        
        if (c instanceof NoopSearchControl) {
            return (NoopSearchControl) c;
        } else {
            return new NoopSearchControl(c.getOID(), c.isCritical(), c.getValue());
        }
    }
    
    @Override
    public NoopSearchControl decodeControl(String oid, boolean isCritical,
            ASN1OctetString value) throws LDAPException {
        return new NoopSearchControl(oid, isCritical, value);
    }
    
    public long getCount() {
        return count;
    }

            
}
