package com.zimbra.cs.client.soap;

import java.util.List;

public class LmcSearchResponse extends LmcSoapResponse {
    
    private String mOffset;
    private String mMore;
    private List mResults;
    
    public void setOffset(String o) { mOffset = o; }
    public void setMore(String m) { mMore = m; }
    public void setResults(List l) { mResults = l; }
    
    public String getOffset() { return mOffset; }
    public String getMore() { return mMore; }
    public List getResults() { return mResults; }
}
