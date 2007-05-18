package com.zimbra.perf.chart;

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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */



import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import com.zimbra.common.util.CsvReader;
import com.zimbra.common.util.StringUtil;
import com.zimbra.perf.chart.ChartSettings.ImageType;

public class ChartFromCsv {

	private static final String OPT_TITLE = "t";

	private static final String OPT_OFFSET = "o";	

	private static final String RESULT_CSV = TestResultConstants.RESULT_CSV;

	private StatHandler handler = null;

	private List<JFreeChart> charts = new ArrayList<JFreeChart>();

	private List<Double> finalResults = new ArrayList<Double>();

	private Date startPoint = new Date();
	
	private Date endPoint = new Date();

	private HashMap<String, Double> stats = new HashMap<String, Double>();

	private Options mOptions;
	
	private String mTitle = "Test Results";

	private int startOffset = 15;
	
	private int endOffset = 10;

	private int clientThread = 0;

	private long gminDate = Long.MAX_VALUE;

	private long gmaxDate = Long.MIN_VALUE;

	private Date testStartingPoint = null;
	

	
	private HashMap<String, String> infileList = new HashMap<String, String>();

	// use infile-outfile as key and the plots setting as the value used for
	// TestResultAnalyzer.
	private HashMap<String, List<PlotSettings>> outfilePlotsMapping = new HashMap<String, List<PlotSettings>>();

	class CSVFilter implements FilenameFilter {
		private String baseName = "";

		public CSVFilter(String baseName) {
			this.baseName = baseName;
		}

		public boolean accept(File dir, String name) {
			// to concatename all the files such as zimbrastats.csv and
			// zimbrastats.csv.2007-02-08
			if (name.indexOf(baseName + ".20") != -1 || name.equals(baseName)) {
				return true;
			}
			return false;

		}

	}

	class FileModifiedSorter implements Comparator<File> {
		public int compare(File o1, File o2) {
			long t1 = o1.lastModified();
			long t2 = o2.lastModified();
			return (int) (t1 - t2);
		}

	}

	private static final SimpleDateFormat[] sDateParsers = {
			new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
			new SimpleDateFormat("MM/dd/yyyy HH:mm") };

	private ChartFromCsv() {
		mOptions = new Options();
		mOptions.addOption(OPT_TITLE, "title", true,
				"Index page title.  Use '_' for spaces.");		
		mOptions.addOption(OPT_OFFSET, "offset", true,
				"The test start point (after warm up finish) ");	
	}

	public static void main(String[] args) throws Exception {
		ChartFromCsv app = new ChartFromCsv();
		app.run(args);
		// app.processInfile("zimbrastats.csv");
	}

	private void writeTestReport() throws Exception {

		TestResultAnalyzer analyzer = new TestResultAnalyzer(stats,
				outfilePlotsMapping);
		analyzer.writeTestReport();
	}

	private void run(String[] args) throws Exception {

		String[] chartDefs = processArgs(args);

		List<ChartSettings> allSettings = getAllChartSettings(chartDefs);

		for (ChartSettings cs : allSettings) {
			generateStats4SingleChart(cs, startPoint,endPoint);
			charts.add(createSingleChart(cs));
		}
		lineUpAxes();

		writeStats(allSettings);
		writeCharts(allSettings);
		writeTestReport();
	}

	private String formatDouble(double d) {
		// after we got the final result, we discard the data we collected.
		DecimalFormat formatter = new DecimalFormat("0.00");
		String finalResult = formatter.format(d);
		return finalResult;

	}

	private Date getStartingPoint(long minDate, int offsetInMin) {
		return new Date(minDate + offsetInMin * 60 * 1000);
	}
	
	private Date getEndingPoint(long maxDate, int offsetInMin) {
		return new Date(maxDate - offsetInMin * 60 * 1000);
	}

	private List<ChartSettings> getAllChartSettings(String[] chartDefs)
			throws Exception {

		List<ChartSettings> allSettings = new ArrayList<ChartSettings>();
		for (String filename : chartDefs) {
			List<ChartSettings> settings = XMLChartConfig.load(filename);
			if (settings.size() == 0) {
				System.out.println("No chart settings found in " + filename);
				System.exit(1);
			}
			for (ChartSettings cs : settings) {
				getRawData4SingleChartSetting(cs);
			}
			allSettings.addAll(settings);

		}
		validateChartSettings(allSettings);
		// we collect the sample default offset mins after
		// the test starting from the end of the warmup
		if (testStartingPoint != null) {
			startPoint = getStartingPoint(testStartingPoint.getTime(),
					startOffset);
		} else {
			startPoint = getStartingPoint(gminDate, startOffset);
		}
		
		endPoint = this.getEndingPoint(gmaxDate, endOffset);

		return allSettings;

	}

	private String[] processArgs(String[] args) throws Exception {

		// verify arguments
		CommandLineParser clParser = new GnuParser();
		CommandLine cl = null;
		cl = clParser.parse(mOptions, args);
		if (cl.hasOption(OPT_TITLE)) {
			mTitle = cl.getOptionValue(OPT_TITLE);
			// Commons CLI can't deal with spaces in the title, so use
			// underscores
			mTitle = mTitle.replace('_', ' ');
		}

		if (cl.hasOption(OPT_OFFSET)) {
			String o = cl.getOptionValue(OPT_OFFSET);
			o = o.replace('_', ' ');
			for (int i = 0; i < sDateParsers.length
					&& testStartingPoint == null; i++) {
				try {
					testStartingPoint = sDateParsers[i].parse(o);
				} catch (ParseException e) {

				}
			}

		}
		
		String[] chartDefs = cl.getArgs();
		if (chartDefs.length == 0) {
			usage();
			System.exit(1);
		}
		// initial the stat handler here because we needs to tell the info such
		// as the max threads
		// we have to stathandler so it can do stats based on the specific info:
		handler = new StatHandler();
		return chartDefs;
	}

	private void generateStats4SingleChart(ChartSettings cs, Date startingPoint, Date endPoint) {

		double d = 0;

		int size = cs.getPlots().size();
		for (int i = 0; i < size; i++) {
			PlotSettings ps = cs.getPlots().get(i);
			String key = ps.getInfile() + ":" + ps.getDataColumn() + ":"
					+ ps.getAggregateFunction();
			d = handler.process(cs.getDataSeries().get(i), startingPoint, endPoint, cs
					.getPlots().get(i).getAggregateFunction(), key);
			finalResults.add(new Double(d));
		}

	}

	private void writeStats(List<ChartSettings> allSettings) throws Exception {
		FileWriter writer = new FileWriter(RESULT_CSV);
		int count = 0;
		String key = "";
		Double val = null;
		for (ChartSettings cs : allSettings) {
			for (PlotSettings ps : cs.getPlots()) {
				key = ps.getInfile() + ":" + ps.getDataColumn() + ":"
						+ ps.getAggregateFunction();
				val = finalResults.get(count);
				count++;
				stats.put(key, val);
			}
		}
		Iterator<String> keyset = stats.keySet().iterator();
		StringBuffer keys = new StringBuffer();
		StringBuffer vals = new StringBuffer();

		while (keyset.hasNext()) {
			key = keyset.next();
			keys.append(key).append(",");
			vals.append(formatDouble(stats.get(key).doubleValue())).append(",");
		}

		writer.write(keys.toString());
		writer.write("\n");
		writer.write(vals.toString());
		writer.write("\n");

		writer.close();
	}

	private void writeCharts(List<ChartSettings> allSettings) throws Exception {
		// write all charts
		for (int i = 0; i < charts.size(); i++) {
			writeChart(charts.get(i), allSettings.get(i));
		}

		// Generate HTML
		FileWriter writer = new FileWriter("index.html");
		writer.write("<html>\n<head>\n<title>" + StringUtil.escapeHtml(mTitle)
				+ "</title>\n</head>\n<body bgcolor=\"#eeeeee\">\n");
		writer.write("<h1>" + StringUtil.escapeHtml(mTitle) + "</h1>\n");
		
		writer.write("<h4>" + "Test Result is loacted at  "
				+ "<A href=\"" + TestResultConstants.RESULT_REPORT + "\">"
				+ TestResultConstants.RESULT_REPORT + "</A>" + "</h4>\n");

		/*writer.write("<h4>" + "Test/Env config dump is located at "
				+ "<A href=\"" + TestResultConstants.TEST_ENV_PROP_DUMP + "\">"
				+ TestResultConstants.TEST_ENV_PROP_DUMP + "</A>" + "</h4>\n");*/

		List<String> noData = new ArrayList<String>();

		int count = 0;
		String statString = "";

		for (int i = 0; i < charts.size(); i++) {
			JFreeChart chart = charts.get(i);
			ChartSettings cs = allSettings.get(i);

			int size = cs.getPlots().size();
			statString = "";
			for (int x = 0; x < size; x++) {
				PlotSettings ps = cs.getPlots().get(x);
				statString += ps.getDataColumn() + "-"
						+ ps.getAggregateFunction() + " ";
				statString += formatDouble(finalResults.get(count)
						.doubleValue())
						+ " ";
				count++;
			}

			if (hasData(chart)) {
				writer.write("<h3>" + cs.getTitle() + "</h3>\n");
				writer.write("<h5>" + statString + "</h5>\n");
				writer.write(String.format(
						"<img src=\"%s\" width=\"%d\" height=\"%d\">\n", cs
								.getOutfile(), cs.getWidth(), cs.getHeight()));
			} else {
				noData.add(cs.getTitle());
			}
		}

		if (noData.size() > 0) {
			writer
					.write("<h3>No data available for the following charts:</h3>\n");
			writer.write("<p>"
					+ StringUtil.escapeHtml(StringUtil.join(", ", noData))
					+ "<p>\n");
		}
		writer.write("</body>\n</html>\n");
		writer.close();

	}

	/*
	 * concatenate the csv file if the test run is over night. ie: concatenate
	 * zimbrastats.csv and zimbrastats.csv.2007-05-09
	 */

	private void processInfile(String infile) throws Exception {

		CSVFilter filter = new CSVFilter(infile);
		File in = new File(infile);
		infileList.put(infile, "");// if the file already got processed, we
										// will not
		// process it next time.

		File dir = new File(new File(in.getAbsolutePath()).getParent());
		File[] files = dir.listFiles(filter);
		if (files.length == 1)
			return;

		FileModifiedSorter sorter = new FileModifiedSorter();
		List<File> list = Arrays.asList(files);
		Collections.sort(list, sorter);

		StringBuffer content = new StringBuffer();
		for (File f : list) {
			String line = null;
			BufferedReader reader = new BufferedReader(new FileReader(f));
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			reader.close();
		}

		FileWriter writer = new FileWriter(infile);
		writer.write(content.toString());
		writer.close();

	}

	private void getRawData4SingleChartSetting(ChartSettings cs)
			throws Exception {
		Date minDate = null;
		Date maxDate = null;
		String outfile = cs.getOutfile();
		this.outfilePlotsMapping.put(outfile, cs.getPlots());

		for (PlotSettings ps : cs.getPlots()) {
			String legend = ps.getLegend();
			System.out.format("Reading data for %s (%s) from %s in %s...\n", cs
					.getOutfile(), legend, ps.getDataColumn(), ps.getInfile());
			DataSeries series = new DataSeries();

			// Read CSV and populate data
			int line = 2;

			FileReader reader = null;
			try {

				if (!infileList.containsKey(ps.getInfile())) {
					processInfile(ps.getInfile());
				}
				reader = new FileReader(ps.getInfile());
			} catch (FileNotFoundException e) {
				System.out.printf("CSV file %s not found; Skipping...\n", ps
						.getInfile());
				cs.addDataSeries(series);
				continue;
			}
			CsvReader csv = new CsvReader(reader);
			if (!csv.columnExists(ps.getDataColumn())) {
				System.out.format("Could not find column '%s' in %s\n", ps
						.getDataColumn(), ps.getInfile());
				csv.close();
				cs.addDataSeries(series);
				continue;
			}
			while (csv.hasNext()) {
				String context = ps.getInfile() + ", line " + line;

				// Find timestamp column
				String val = csv.getValue(ps.getTstampColumn());
				if (val == null) {
					System.out.println(context + ": no timestamp found.");
					line++;
					continue;
				}

				// Try all date parsers until one succeeds
				Date ts = null;
				ParseException lastException = null;
				for (int i = 0; i < sDateParsers.length && ts == null; i++) {
					try {
						ts = sDateParsers[i].parse(val);
					} catch (ParseException e) {
						lastException = e;
					}
				}
				if (ts == null) {
					System.out.println(context + ": " + lastException);
					line++;
					continue;
				}
				String tstampStr = val;

				// Set min/max date
				if (minDate == null) {
					minDate = ts;
					maxDate = ts;
				} else {
					if (ts.compareTo(minDate) < 0) {
						minDate = ts;
					}
					if (ts.compareTo(maxDate) > 0) {
						maxDate = ts;
					}
				}

				// Parse values
				val = csv.getValue(ps.getDataColumn());
				if (!StringUtil.isNullOrEmpty(val)) {
					try {
						double d = Double.parseDouble(val);

						try {
							series.AddEntry(ts, d);
						} catch (SeriesException e) {
							System.out
									.printf(
											"Can't add sample to series: tstamp=%s, value=%s\n",
											tstampStr, val);
							e.printStackTrace(System.out);
						}

					} catch (NumberFormatException e) {
						System.out.println(String.format(
								"%s: unable to parse value '%s' for %s: %s",
								context, val, ps.getDataColumn(), e));
					}
				}
				line++;
			}

			csv.close();

			System.out.format(
					"Adding %d %s points between %s\nand %s to %s.\n\n", series
							.size(), legend, minDate, maxDate, cs.getOutfile());
			resetMinMaxDate(minDate, maxDate);

			cs.addDataSeries(series);

		}
	}

	private void resetMinMaxDate(Date minDate, Date maxDate) {

		long chartMinDate = 0;
		if (minDate != null) {
			chartMinDate = minDate.getTime();
		}

		long chartMaxDate = 0;
		if (maxDate != null) {
			chartMaxDate = maxDate.getTime();
		}

		if (chartMinDate != 0 && chartMinDate < gminDate) {
			gminDate = chartMinDate;
		}
		if (chartMaxDate > gmaxDate) {
			gmaxDate = chartMaxDate;
		}
	}

	private static void usage() throws Exception {
		System.out.println(ChartFromCsv.class.getName()
				+ " <xmlFile> [xmlFile2] ...");
	}

	private void validateChartSettings(List<ChartSettings> allSettings)
			throws ParseException {
		// Make sure we're not writing the same chart twice
		Set<String> usedFilenames = new HashSet<String>();
		for (ChartSettings cs : allSettings) {
			String filename = cs.getOutfile();
			if (usedFilenames.contains(filename)) {
				throw new ParseException("Found two charts that write "
						+ filename, 0);
			}
			usedFilenames.add(filename);
		}
	}

	private JFreeChart createSingleChart(ChartSettings cs) {

		double minValue = Double.MAX_VALUE;
		double maxValue = Double.MIN_VALUE;
		double d = 0;
		double sum = 0;
		double count = 0;
		double total = 0;
		int size = cs.getPlots().size();
		TimeSeriesCollection data = new TimeSeriesCollection();

		for (int i = 0; i < size; i++) {
			PlotSettings ps = cs.getPlots().get(i);
			DataSeries ds = cs.getDataSeries().get(i);
			TimeSeries ts = new TimeSeries(ps.getLegend(),
					FixedMillisecond.class);
			int s = ds.size();
			for (int j = 0; j < s; j++) {
				d = ds.get(j).getVal();
				d *= ps.getMultiplier();
				d /= ps.getDivisor();
				if (d != 0 || cs.getPlotZero()) {
					if (ps.getShowSum()) {
						sum += d;
						d = sum;
					}
					if (d < minValue) {
						minValue = d;
					}
					if (d > maxValue) {
						maxValue = d;
					}
					count++;
					total += d;

					try {
						ts.addOrUpdate(new FixedMillisecond(ds.get(j)
								.getTimestamp()), d);

					} catch (SeriesException e) {
						e.printStackTrace(System.out);
					}

				}

			}
			System.out.format("Adding %d %s points to %s.\n\n", ds.size(), ps
					.getLegend(), cs.getOutfile());
			if (ps.getShowRaw()) {
				data.addSeries(ts);
			}
			if (ps.getShowMovingAvg()) {
				int numPoints = ps.getMovingAvgPoints();
				if (numPoints == PlotSettings.DEFAULT_PLOT_MOVING_AVG_POINTS) {
					// Display 200 points for moving average.
					// Divide the total number of points by 200 to
					// determine the number of samples to average
					// for each point.
					numPoints = ts.getItemCount() / 200;
				}
				if (numPoints >= 2) {
					TimeSeries ma = MovingAverage.createPointMovingAverage(ts,
							ps.getLegend() + " (moving avg)", numPoints);
					data.addSeries(ma);
				} else {
					System.out
							.println("Not enough data to display moving average for "
									+ ps.getLegend());
					data.addSeries(ts);

				}
			}

		}
		// Create chart
		boolean legend = (data.getSeriesCount() > 1);
		JFreeChart chart = ChartFactory.createTimeSeriesChart(null, cs
				.getXAxis(), cs.getYAxis(), data, legend, false, false);

		// Make Y-axis logarithmic if a spike was detected
		if (cs.getAllowLogScale() && (minValue > 0) && (maxValue > 0)
				&& (maxValue > 20 * (total / count))) {
			if (maxValue / minValue > 100) {
				XYPlot plot = (XYPlot) chart.getPlot();
				ValueAxis oldAxis = plot.getRangeAxis();
				LogarithmicAxis newAxis = new LogarithmicAxis(oldAxis
						.getLabel());
				plot.setRangeAxis(newAxis);
			}
		}

		return chart;

	}

	private boolean hasData(JFreeChart chart) {
		if (chart == null) {
			return false;
		}
		XYPlot plot = chart.getXYPlot();
		XYDataset data = plot.getDataset();

		int numPoints = 0;
		for (int i = 0; i < data.getSeriesCount(); i++) {
			numPoints += data.getItemCount(i);
		}
		if (numPoints == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Updates axes for all charts so that they display the same time interval.
	 */
	private void lineUpAxes() {

		for (JFreeChart chart : charts) {
			XYPlot plot = (XYPlot) chart.getPlot();
			DateAxis axis = (DateAxis) plot.getDomainAxis();
			Date chartMinDate = axis.getMinimumDate();
			Date chartMaxDate = axis.getMaximumDate();

			if (chartMinDate != null && gminDate < chartMinDate.getTime()) {
				axis.setMinimumDate(new Date(gminDate));
			}
			if (chartMaxDate != null && gmaxDate > chartMaxDate.getTime()) {
				axis.setMaximumDate(new Date(gmaxDate));
			}
		}

	}

	private void writeChart(JFreeChart chart, ChartSettings cs)
			throws IOException {
		File file = new File(cs.getOutfile());
		if (cs.getImageType() == ImageType.PNG) {
			ChartUtilities.saveChartAsPNG(file, chart, cs.getWidth(), cs
					.getHeight());
		} else {
			ChartUtilities.saveChartAsJPEG(file, 90, chart, cs.getWidth(), cs
					.getHeight());
		}
	}

	class Entry {
		Date timestamp;

		double value;

		public Entry(Date t, double v) {
			this.timestamp = t;
			this.value = v;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public double getVal() {
			return value;
		}

	}

	class DataSeries {

		String data;

		List<Entry> dataCollection = new ArrayList<Entry>();

		public void AddEntry(Date t, double d) {
			Entry entry = new Entry(t, d);
			dataCollection.add(entry);
		}

		public int size() {
			return dataCollection.size();
		}

		public Entry get(int index) {
			return dataCollection.get(index);
		}

	}

	class StatHandler {
		HashMap<String, Integer> set = new HashMap<String, Integer>();

		public StatHandler() {
		
		}

		private int getPosition(DataSeries series, Date startingPoint) {
			int i = 0;
			int size = series.size();
			Date tstamp = null;

			for (i = 0; i < size; i++) {
				tstamp = series.get(i).getTimestamp();
				if (tstamp.after(startingPoint))
					return i;
			}
			return i;
		}	
		

		private double getAvg(DataSeries series, Date startingPoint, Date endPoint) {
			double d = 0.0;

			int j = getPosition(series, startingPoint);//start point
			int e = getPosition(series, endPoint);//end point			

			for (int i = j; i < e; i++) {
				d += series.get(i).getVal();
			}
			if (j >= e )
				return -1;
			else
				return d / (e - j);
		}

		private double getMax(DataSeries series) {
			double d = 0.0;		

			int j = getPosition(series, startPoint);
			int s = series.size();
			if (s == 0 )
				return -1;

			double max = Double.MIN_VALUE;

			for (int i = j; i < s; i++) {
				d = series.get(i).getVal();
				System.out.println("d" + d);
				if (d > max) {
					max = d;
					System.out.println("max" + max);
				}
			}
			return max;
		}

		private double getMaxPercentage(DataSeries series, Date startingPoint,
				String key) {

			int max = set.get(key).intValue();
			int j = getPosition(series, startingPoint);
			int s = series.size();
			if (s == 0 || (j == s)) {
				return -1;
			}
			int count = 0;
			for (int i = j; i < s; i++) {
				int ceil = (int) (Math.ceil(series.get(i).getVal()));	
				System.out.println("ceil " + ceil);
				if (ceil >= max)
					count++;
			}
			System.out.println(" count " + count);
			System.out.println(" s - j " + (s - j));
			return count / (s - j);

		}

		private double getLast(DataSeries series, Date startingPoint) {
			if (series.size() > 0) {
				return series.get(series.size() - 1).getVal();
			} else {
				return -1;
			}
		}

		public double process(DataSeries series, Date startingPoint, Date endPoint,
				String aggreateFunction, String key) {
			if (aggreateFunction.equalsIgnoreCase(PlotSettings.FUNCTION_LAST))
				return getLast(series, startingPoint);
			else if (aggreateFunction
					.equalsIgnoreCase(PlotSettings.FUNCTION_MAX))
				return getMax(series);
			else if (aggreateFunction
					.equalsIgnoreCase(PlotSettings.FUNCTION_MAX_PERCENTAGE))
				return getMaxPercentage(series, startingPoint,key);
			else
				return getAvg(series, startingPoint,endPoint);

		}
	}
}

 