/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.perf.chart;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.util.CsvReader;


public class TestResultAnalyzer {

	private FileWriter writer = null;

	private HashMap<String, Double> stats = null;

	private HashMap<String, List<PlotSettings>> outfilePlotsMapping = null;
	
	private List<ReportEntry> entryList = new ArrayList<ReportEntry>();	

	public TestResultAnalyzer(HashMap<String, Double> stats,
			HashMap<String, List<PlotSettings>> outfilePlotsMapping) {
		this.stats = stats;
		this.outfilePlotsMapping = outfilePlotsMapping;
		
	}

	private static String buildKey(String infile, String data, String aggregateFun) {
		return infile + ":" + data + ":" + aggregateFun;
	}

	private static String getVolume(String dataColumn) {
		return dataColumn.split(":")[0];
	}
	
	private void buildSingleVolume(String volumeName, String name){
		  String key;
		  key = buildKey(TestResultConstants.IO_IN,volumeName + ":"
				  + TestResultConstants.IO_UTIL,
					PlotSettings.FUNCTION_AVG);
		  entryList.add(new ReportEntry( name + " Disk Util",key));
		  key = buildKey(
				TestResultConstants.IO_IN, volumeName + ":"
						+ TestResultConstants.R_THROUGHPUT,
				PlotSettings.FUNCTION_AVG);
		  entryList.add(new ReportEntry( name + " Read Throughput",key));
		  
		  key = buildKey(
				TestResultConstants.IO_IN, volumeName + ":"
						+ TestResultConstants.W_THROUGHPUT,
				PlotSettings.FUNCTION_AVG);
		  entryList.add(new ReportEntry(name + " Write Throughput",key));
		  key = buildKey(
				TestResultConstants.IO_IN, volumeName + ":"
						+ TestResultConstants.R_IOPS,
				PlotSettings.FUNCTION_AVG);
		  entryList.add(new ReportEntry(name + " Read IOPS",key));
		  key = buildKey(
				TestResultConstants.IO_IN, volumeName + ":"
						+ TestResultConstants.W_IOPS,
				PlotSettings.FUNCTION_AVG);
		  entryList.add(new ReportEntry(name + " write IOPS",key));
	}
	
	private List<PlotSettings> getIOPlotSettings(){
		Iterator it = outfilePlotsMapping.keySet().iterator();
		List<PlotSettings> settings = null;
		String key;
		while(it.hasNext()){
			key = (String)it.next();
			if(key.indexOf(TestResultConstants.KEY_TO_DISK_UTIL) != -1 ){
				settings = outfilePlotsMapping.get(key);
				break;				
			}
		}		
		return settings;
		
	}

	private  void buildIOKeys() {
		List<PlotSettings> settings = getIOPlotSettings();	    
		for (PlotSettings ps : settings) {
			String legend = ps.getLegend();
			String volume = getVolume(ps.getDataColumn());	
			buildSingleVolume(volume, legend);
		}	
	}

	private void buildCPUKeys() {
		 String key;
		 key = buildKey(TestResultConstants.CPU_IN,
				  TestResultConstants.SYS,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mail Server CPU Sys ",key));
		 
		 key = buildKey(TestResultConstants.CPU_IN,
				  TestResultConstants.USER,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mail Server CPU User ",key));
		 
		 key = buildKey(TestResultConstants.CPU_IN,
				  TestResultConstants.IDLE,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mail Server CPU Idle ",key));
		 
		 key = buildKey(TestResultConstants.CPU_IN,
				  TestResultConstants.IOWAIT,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mail Server CPU Iowait ",key));

	}

	private void buildMemKeys() {
		 String key;
		 key = buildKey(TestResultConstants.GC_IN,
				  TestResultConstants.F_GC,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Full GC% ",key));
		 
		 key = buildKey(TestResultConstants.GC_IN,
				  TestResultConstants.Y_GC,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Young GC% ",key));
	}

	private void buildServerResponseKeys() {
		 String key;
		 key = buildKey(TestResultConstants.SERVER_IN,
				  TestResultConstants.ADD_COUNT,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mailbox add rate ",key));
		 
		 key = buildKey(TestResultConstants.SERVER_IN,
				  TestResultConstants.ADD_LATENCY,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mailbox add latency ",key)); 
		 
		 key = buildKey(TestResultConstants.SERVER_IN,
				  TestResultConstants.GET_LATENCY,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Mailbox get latency ",key));
		 
		 key = buildKey(TestResultConstants.SERVER_IN,
				  TestResultConstants.SOAP_RESPONSE,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Soap response time",key));
		 
		 key = buildKey(TestResultConstants.SERVER_IN,
				  TestResultConstants.POP_RESPONSE,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Pop response time",key));
		 
		 key = buildKey(TestResultConstants.SERVER_IN,
				  TestResultConstants.IMAP_RESPONSE,
					PlotSettings.FUNCTION_AVG);
		 entryList.add(new ReportEntry("Imap response time",key));
	}
	
	public class ReportEntry{
		String entryName;
		String entryKey;
		public ReportEntry(String keyName, String keyVal){
			this.entryName = keyName;
			this.entryKey = keyVal;
		}
		public String getEntryKey() {
			return entryKey;
		}
		public void setEntryKey(String entryKey) {
			this.entryKey = entryKey;
		}
		public String getEntryName() {
			return entryName;
		}
		public void setEntryName(String entryName) {
			this.entryName = entryName;
		}
	}
	
	private void writeTestDetails()throws Exception{
		
		buildIOKeys();
		buildCPUKeys();
		buildMemKeys();
		buildServerResponseKeys();
		
		
		FileReader reader = new FileReader(TestResultConstants.RESULT_CSV);
		CsvReader csv = new CsvReader(reader);
		writer.write("===========================================\n");
		writer.write("#            Perf Stats                   #\n");
		writer.write("===========================================\n");
		
		if(csv.hasNext()){
			
			for(ReportEntry entry : entryList){
			  if(csv.columnExists(entry.entryKey)){
				  writer.write(entry.entryName + ":" + csv.getValue(entry.entryKey));
			  }else{
				  writer.write(entry.entryName + ":");
			  }
			  writer.write("\n");
			}
		}
		reader.close();
	}

	public void writeTestReport() throws Exception {
		writer = new FileWriter(TestResultConstants.RESULT_REPORT);			
	    writeTestDetails();	
		writer.close();
	}

}
