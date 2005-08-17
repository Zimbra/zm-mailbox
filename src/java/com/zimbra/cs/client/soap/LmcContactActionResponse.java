package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.soap.LmcSoapResponse;

public class LmcContactActionResponse extends LmcSoapResponse {

	private String mIDList;
	private String mOp;
	
	public String getIDList() { return mIDList; }
	public String getOp() { return mOp; }
	
	public void setIDList(String idList) { mIDList = idList; }
	public void setOp(String op) { mOp = op; }
}
