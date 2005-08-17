/*
 * Created on Dec 9, 2004
 */
package com.zimbra.cs.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.commons.fileupload.FileItem;

/**
 * @author dkarp
 */
public class FileItemDataSource implements DataSource {

    private FileItem mFileItem;

	/**
	 * 
	 */
	public FileItemDataSource(FileItem fi) {
		mFileItem = fi;
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getContentType()
	 */
	public String getContentType() {
        return mFileItem.getContentType();
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		return mFileItem.getInputStream(); 
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getName()
	 */
	public String getName() {
		return mFileItem.getName();
	}

	/* (non-Javadoc)
	 * @see javax.activation.DataSource#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException {
		return mFileItem.getOutputStream();
	}

}
