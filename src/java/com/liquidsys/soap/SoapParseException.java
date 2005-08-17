package com.liquidsys.soap;

/**
 * An IOException that SoapTransport.invoke throws if it is unable to
 * parse a soap message.
 */

public class SoapParseException extends java.io.IOException {

    private String mSoapMessage;
    
    public SoapParseException(String message, String soapMessage)
    {
        super(message + ": " + soapMessage);
        mSoapMessage = soapMessage;
    }

    public String getSoapMessage()
    {
        return mSoapMessage;
    }
}




