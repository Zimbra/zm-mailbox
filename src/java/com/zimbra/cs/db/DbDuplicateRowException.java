/*
 * Created on 2004. 6. 1.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.db;

import java.sql.SQLException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DbDuplicateRowException extends SQLException {

	/**
	 * @param msg
	 */
	public DbDuplicateRowException(SQLException cause) {
		super("Detected duplicate row being inserted");
		setNextException(cause);
	}
}
