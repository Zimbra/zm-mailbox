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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XMLChartConfig {

	// elements
	public static final String E_CHARTS = "charts";

	public static final String E_CHART = "chart";

	public static final String E_PLOT = "plot";

	// <chart> attributes
	public static final String A_CHART_TITLE = "title";

	public static final String A_CHART_OUTFILE = "outfile";

	public static final String A_CHART_XAXIS = "xAxis";

	public static final String A_CHART_YAXIS = "yAxis";

	public static final String A_CHART_ALLOW_LOG_SCALE = "allowLogScale";

	public static final String A_CHART_PLOT_ZERO = "plotZero";

	public static final String A_CHART_WIDTH = "width";

	public static final String A_CHART_HEIGHT = "height";

	// <plot> attributes
	public static final String A_PLOT_LEGEND = "legend";

	public static final String A_PLOT_INFILE = "infile";

	public static final String A_PLOT_TSTAMP_COLUMN = "tstamp";

	public static final String A_PLOT_DATA_COLUMN = "data";

	public static final String A_PLOT_SHOW_RAW = "showRaw";

	public static final String A_PLOT_SHOW_SUM = "showSum";

	public static final String A_PLOT_SHOW_MOVING_AVG = "showMovingAvg";

	public static final String A_PLOT_MOVING_AVG_POINTS = "movingAvgPoints";

	public static final String A_PLOT_MULTIPLIER = "multiplier";

	public static final String A_PLOT_DIVISOR = "divisor";

	public static final String A_PLOT_AGGREGATE_FUNCTION = "aggregateFunction";

	private static String getAttr(Element elem, String name)
			throws DocumentException {
		String val = elem.attributeValue(name, null);
		if (val != null)
			return val;
		else
			throw new DocumentException("Missing required attribute " + name
					+ " in element " + elem.getName());
	}

	private static String getAttr(Element elem, String name, String defaultValue) {
		String val = elem.attributeValue(name, null);
		if (val != null)
			return val;
		else
			return defaultValue;
	}

	private static String getInheritedAttr(Element elem, String name,
			String defaultValue) {
		String val = null;
		for (Element e = elem; val == null && e != null; e = e.getParent()) {
			val = e.attributeValue(name, null);
		}
		return val != null ? val : defaultValue;
	}

	private static Boolean getInheritedAttrBoolean(Element elem, String name,
			boolean defaultValue) {
		String val = getInheritedAttr(elem, name, null);
		if (val != null) {
			val = val.toLowerCase();
			return val.equalsIgnoreCase("true") || val.equals("1");
		} else
			return defaultValue;
	}

	private static int getInheritedAttrInt(Element elem, String name,
			int defaultValue) throws DocumentException {
		String val = getInheritedAttr(elem, name, null);
		if (val != null) {
			try {
				int i = Integer.parseInt(val);
				return i;
			} catch (NumberFormatException ex) {
				throw new DocumentException("Invalid integer value " + val
						+ " for attribute " + name + " in element "
						+ elem.getName());
			}
		} else
			return defaultValue;
	}

	private static double getInheritedAttrDouble(Element elem, String name,
			double defaultValue) throws DocumentException {
		String val = getInheritedAttr(elem, name, null);
		if (val != null) {
			try {
				double d = Double.parseDouble(val);
				return d;
			} catch (NumberFormatException ex) {
				throw new DocumentException("Invalid double value " + val
						+ " for attribute " + name + " in element "
						+ elem.getName());
			}
		} else
			return defaultValue;
	}

	/**
	 * Loads chart settings from the specified XML file.
	 * 
	 * @throws IOException
	 *             if there was an error reading the file
	 * @throws DocumentException
	 *             if there was an error parsing the file
	 * @throws IllegalArgumentException
	 *             if any attribute has invalid value
	 */
	public static List<ChartSettings> load(String xmlFilename)
			throws IOException, DocumentException {
		List<ChartSettings> charts = new ArrayList<ChartSettings>();
		SAXReader reader = new SAXReader();
		Document document = reader.read(xmlFilename);
		Element chartsElem = document.getRootElement();
		if (!chartsElem.getName().equals(E_CHARTS))
			throw new DocumentException("Missing <" + E_CHARTS
					+ "> root element");
		for (Iterator iter = chartsElem.elementIterator(E_CHART); iter
				.hasNext();) {
			Element chartElem = (Element) iter.next();
			String chartTitle = getAttr(chartElem, A_CHART_TITLE);
			String outfile = getAttr(chartElem, A_CHART_OUTFILE);

			// inheritable attributes
			String xAxis = getInheritedAttr(chartElem, A_CHART_XAXIS,
					ChartSettings.DEFAULT_CHART_XAXIS);
			String yAxis = getInheritedAttr(chartElem, A_CHART_YAXIS, "");
			boolean allowLogScale = getInheritedAttrBoolean(chartElem,
					A_CHART_ALLOW_LOG_SCALE,
					ChartSettings.DEFAULT_CHART_ALLOW_LOG_SCALE);
			boolean plotZero = getInheritedAttrBoolean(chartElem,
					A_CHART_PLOT_ZERO, ChartSettings.DEFAULT_CHART_PLOT_ZERO);
			if (!allowLogScale)
				plotZero = true;
			int width = getInheritedAttrInt(chartElem, A_CHART_WIDTH,
					ChartSettings.DEFAULT_CHART_WIDTH);
			int height = getInheritedAttrInt(chartElem, A_CHART_HEIGHT,
					ChartSettings.DEFAULT_CHART_HEIGHT);

			ChartSettings chart = new ChartSettings(chartTitle, outfile, xAxis,
					yAxis, allowLogScale, plotZero, width, height);

			for (Iterator plotIter = chartElem.elementIterator(E_PLOT); plotIter
					.hasNext();) {
				Element plotElem = (Element) plotIter.next();
				String dataCol = getAttr(plotElem, A_PLOT_DATA_COLUMN);

				String function = getAttr(plotElem, A_PLOT_AGGREGATE_FUNCTION,
						PlotSettings.DEFAULT_PLOT_AGGREGATE_FUNCTION);

				// inheritable attributes
				String legend = getInheritedAttr(plotElem, A_PLOT_LEGEND, null);
				String infile = getInheritedAttr(plotElem, A_PLOT_INFILE, null);
				String tstampCol = getInheritedAttr(plotElem,
						A_PLOT_TSTAMP_COLUMN,
						PlotSettings.DEFAULT_PLOT_TSTAMP_COLUMN);

				boolean showRaw = getInheritedAttrBoolean(plotElem,
						A_PLOT_SHOW_RAW, PlotSettings.DEFAULT_PLOT_SHOW_RAW);
				boolean showSum = getInheritedAttrBoolean(plotElem,
						A_PLOT_SHOW_SUM, PlotSettings.DEFAULT_PLOT_SHOW_SUM);
				boolean showMovingAvg = getInheritedAttrBoolean(plotElem,
						A_PLOT_SHOW_MOVING_AVG,
						PlotSettings.DEFAULT_PLOT_SHOW_MOVING_AVG);
				int movingAvgPoints = getInheritedAttrInt(plotElem,
						A_PLOT_MOVING_AVG_POINTS,
						PlotSettings.DEFAULT_PLOT_MOVING_AVG_POINTS);
				double multiplier = getInheritedAttrDouble(plotElem,
						A_PLOT_MULTIPLIER, PlotSettings.DEFAULT_PLOT_MULTIPLIER);
				double divisor = getInheritedAttrDouble(plotElem,
						A_PLOT_DIVISOR, PlotSettings.DEFAULT_PLOT_DIVISOR);

				PlotSettings plot = new PlotSettings(legend, infile, tstampCol,
						dataCol, showRaw, showSum, showMovingAvg,
						movingAvgPoints, multiplier, divisor, function);
				chart.addPlot(plot);
			}
			charts.add(chart);
		}
		return charts;
	}
}
