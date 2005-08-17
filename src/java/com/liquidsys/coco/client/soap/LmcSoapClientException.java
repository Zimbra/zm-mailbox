package com.liquidsys.coco.client.soap;

/**
 * An exception that can occur due to client-side errors.  
 */
public class LmcSoapClientException extends Exception {
    
    public LmcSoapClientException(String message) {
        super(message);
    }
}