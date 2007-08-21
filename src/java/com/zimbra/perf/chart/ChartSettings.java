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

import java.util.ArrayList;
import java.util.List;

class ChartSettings {

	public static final String DEFAULT_CHART_XAXIS = "Time";

	public static final boolean DEFAULT_CHART_ALLOW_LOG_SCALE = true;

	public static final boolean DEFAULT_CHART_PLOT_ZERO = false;

	public static final int DEFAULT_CHART_WIDTH = 1200;

	public static final int DEFAULT_CHART_HEIGHT = 200;

	public static final int MINIMUM_CHART_HEIGHT = 200;

	public static enum ImageType {
		JPG, PNG
	};

	private String mTitle;

	private String mOutfile;

	private String mXAxis;

	private String mYAxis;

	private boolean mAllowLogScale;

	private int mWidth = 1200;

	private int mHeight = 200;

	private ImageType mImageType;

	private List<PlotSettings> mPlots;

	private List<ChartFromCsv.DataSeries> mDataSeries;

	/**
	 * <code>LogarithmicAxis</code> can't handle zero values. Setting this
	 * property to <code>false</code> tells <code>ChartFromCsv</code> to
	 * ignore zero values, which allows a logarithmic axis chart to be rendered.
	 */
	private boolean mPlotZero = false;

	public ChartSettings(String title, String outfile, String xAxis,
			String yAxis, boolean allowLogScale, boolean plotZero, int width,
			int height) {
		mTitle = title;
		mOutfile = outfile;
		String lower = mOutfile.toLowerCase();
		if (lower.endsWith(".png"))
			mImageType = ImageType.PNG;
		else if (lower.endsWith(".jpg"))
			mImageType = ImageType.JPG;
		else
			throw new IllegalArgumentException(String.format(
					"Unexpected file type '%s' for outfile in chart %s.  "
							+ "Only .jpg and .png are supported.", mOutfile,
					title));
		mXAxis = xAxis;
		mYAxis = yAxis;
		mAllowLogScale = allowLogScale;
		mPlotZero = plotZero;
		mWidth = width;
		mHeight = height;
		mPlots = new ArrayList<PlotSettings>();
		mDataSeries = new ArrayList<ChartFromCsv.DataSeries>();
	}

	public void addPlot(PlotSettings p) {
		mPlots.add(p);
	}

	public void addDataSeries(ChartFromCsv.DataSeries t) {
		mDataSeries.add(t);
	}

	public String getTitle() {
		return mTitle;
	}

	public String getOutfile() {
		return mOutfile;
	}

	public String getXAxis() {
		return mXAxis;
	}

	public String getYAxis() {
		return mYAxis;
	}

	public boolean getAllowLogScale() {
		return mAllowLogScale;
	}

	public boolean getPlotZero() {
		return mPlotZero;
	}

	public int getWidth() {
		return mWidth;
	}

	public ImageType getImageType() {
		return mImageType;
	}

	public List<PlotSettings> getPlots() {
		return mPlots;
	}

	public List<ChartFromCsv.DataSeries> getDataSeries() {
		return mDataSeries;
	}

	public int getHeight() {
		int h = Math.max(mHeight, MINIMUM_CHART_HEIGHT);
		if (mPlots.size() <= 1)
			h -= 40;
		return h;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<chart>\n");
		sb.append("  ").append("title=\"").append(mTitle).append("\"\n");
		sb.append("  ").append("outfile=\"").append(mOutfile).append("\"\n");
		sb.append("  ").append("xAxis=\"").append(mXAxis).append("\"\n");
		sb.append("  ").append("yAxis=\"").append(mYAxis).append("\"\n");
		sb.append("  ").append("allowLogScale=\"").append(mAllowLogScale)
				.append("\"\n");
		sb.append("  ").append("plotZero=\"").append(mPlotZero).append("\"\n");
		sb.append("  ").append("width=\"").append(mWidth).append("\"\n");
		sb.append("  ").append("height=\"").append(mHeight).append("\"\n");
		for (PlotSettings plot : mPlots) {
			sb.append(plot.toString());
		}
		sb.append("</chart>\n");
		return sb.toString();
	}
}

