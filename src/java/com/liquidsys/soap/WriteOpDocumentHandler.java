/*
 * Created on 2005. 1. 7.
 */
package com.liquidsys.soap;

/**
 * @author jhahm
 *
 * Abstract base class for document handlers for operations that
 * cause state change in backend data stores.
 */
public abstract class WriteOpDocumentHandler extends DocumentHandler {

    public boolean isReadOnly() {
        return false;
    }
}
