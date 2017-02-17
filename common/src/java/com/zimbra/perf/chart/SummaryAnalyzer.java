/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.perf.chart;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.CsvReader;


public class SummaryAnalyzer {

    private Map<String, List<PlotSettings>> mOutfilePlotsMap = null;

    private List<ReportEntry> mEntryList = new ArrayList<ReportEntry>();

    public SummaryAnalyzer(List<ChartSettings> chartSettings) {
        mOutfilePlotsMap = new HashMap<String, List<PlotSettings>>(chartSettings.size());
        for (ChartSettings cs : chartSettings) {
            mOutfilePlotsMap.put(cs.getOutfile(), cs.getPlots());
        }
    }

    private static String buildKey(String infile, String data, String aggregateFun) {
        return infile + ":" + data + ":" + aggregateFun;
    }

    private static String getVolume(String dataColumn) {
        return dataColumn.split(":")[0];
    }
    
    private void buildSingleVolume(String volumeName, String name){
          String key;
          key = buildKey(SummaryConstants.IO_IN,volumeName + ":"
                  + SummaryConstants.IO_UTIL,
                    PlotSettings.AGG_FUNCTION_AVG);
          mEntryList.add(new ReportEntry( name + " Disk Util",key));
          key = buildKey(
                SummaryConstants.IO_IN, volumeName + ":"
                        + SummaryConstants.R_THROUGHPUT,
                PlotSettings.AGG_FUNCTION_AVG);
          mEntryList.add(new ReportEntry( name + " Read Throughput",key));
          
          key = buildKey(
                SummaryConstants.IO_IN, volumeName + ":"
                        + SummaryConstants.W_THROUGHPUT,
                PlotSettings.AGG_FUNCTION_AVG);
          mEntryList.add(new ReportEntry(name + " Write Throughput",key));
          key = buildKey(
                SummaryConstants.IO_IN, volumeName + ":"
                        + SummaryConstants.R_IOPS,
                PlotSettings.AGG_FUNCTION_AVG);
          mEntryList.add(new ReportEntry(name + " Read IOPS",key));
          key = buildKey(
                SummaryConstants.IO_IN, volumeName + ":"
                        + SummaryConstants.W_IOPS,
                PlotSettings.AGG_FUNCTION_AVG);
          mEntryList.add(new ReportEntry(name + " write IOPS",key));
    }

    private List<PlotSettings> getIOPlotSettings(){
        Iterator<String> it = mOutfilePlotsMap.keySet().iterator();
        List<PlotSettings> settings = null;
        String key;
        while (it.hasNext()) {
            key = (String) it.next();
            if (key.indexOf(SummaryConstants.KEY_TO_DISK_UTIL) != -1 ) {
                settings = mOutfilePlotsMap.get(key);
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
         key = buildKey(SummaryConstants.CPU_IN,
                  SummaryConstants.SYS,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mail Server CPU Sys ",key));
         
         key = buildKey(SummaryConstants.CPU_IN,
                  SummaryConstants.USER,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mail Server CPU User ",key));
         
         key = buildKey(SummaryConstants.CPU_IN,
                  SummaryConstants.IDLE,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mail Server CPU Idle ",key));
         
         key = buildKey(SummaryConstants.CPU_IN,
                  SummaryConstants.IOWAIT,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mail Server CPU Iowait ",key));

    }

    private void buildMemKeys() {
         String key;
         key = buildKey(SummaryConstants.GC_IN,
                  SummaryConstants.F_GC,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Full GC% ",key));
         
         key = buildKey(SummaryConstants.GC_IN,
                  SummaryConstants.Y_GC,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Young GC% ",key));
    }

    private void buildServerResponseKeys() {
         String key;
         key = buildKey(SummaryConstants.SERVER_IN,
                  SummaryConstants.ADD_COUNT,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mailbox add rate ",key));
         
         key = buildKey(SummaryConstants.SERVER_IN,
                  SummaryConstants.ADD_LATENCY,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mailbox add latency ",key)); 
         
         key = buildKey(SummaryConstants.SERVER_IN,
                  SummaryConstants.GET_LATENCY,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Mailbox get latency ",key));
         
         key = buildKey(SummaryConstants.SERVER_IN,
                  SummaryConstants.SOAP_RESPONSE,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Soap response time",key));
         
         key = buildKey(SummaryConstants.SERVER_IN,
                  SummaryConstants.POP_RESPONSE,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Pop response time",key));
         
         key = buildKey(SummaryConstants.SERVER_IN,
                  SummaryConstants.IMAP_RESPONSE,
                    PlotSettings.AGG_FUNCTION_AVG);
         mEntryList.add(new ReportEntry("Imap response time",key));
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

    public void writeReport(File summaryCsv) throws IOException {
        buildIOKeys();
        buildCPUKeys();
        buildMemKeys();
        buildServerResponseKeys();

        FileReader reader = null;
        CsvReader csv = null;
        FileWriter writer = null;
        try {
            reader = new FileReader(summaryCsv);
            csv = new CsvReader(reader);
            File summaryTxt = new File(summaryCsv.getParentFile(),
                    SummaryConstants.SUMMARY_TXT);
            writer = new FileWriter(summaryTxt);
            writer.write("===========================================\n");
            writer.write("#            Perf Stats                   #\n");
            writer.write("===========================================\n");

            if(csv.hasNext()){
                for(ReportEntry entry : mEntryList){
                  if(csv.columnExists(entry.entryKey)){
                      writer.write(entry.entryName + ":" + csv.getValue(entry.entryKey));
                  }else{
                      writer.write(entry.entryName + ":");
                  }
                  writer.write("\n");
                }
            }
        } finally {
            if (writer != null)
                writer.close();
            if (csv != null)
                csv.close();
            else if (reader != null)
                reader.close();
        }
    }
}
