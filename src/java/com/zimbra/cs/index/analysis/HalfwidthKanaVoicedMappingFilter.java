/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.MappingCharFilter;
import org.apache.lucene.analysis.NormalizeCharMap;
import org.apache.lucene.analysis.CharStream;

public class HalfwidthKanaVoicedMappingFilter extends MappingCharFilter {
	private static NormalizeCharMap normMap = new NormalizeCharMap();
	static {
		normMap.add("ｶﾞ","ガ");
		normMap.add("ｷﾞ","ギ");
		normMap.add("ｸﾞ","グ");
		normMap.add("ｹﾞ","ゲ");
		normMap.add("ｺﾞ","ゴ");
		normMap.add("ｻﾞ","ザ");
		normMap.add("ｼﾞ","ジ");
		normMap.add("ｽﾞ","ズ");
		normMap.add("ｾﾞ","ゼ");
		normMap.add("ｿﾞ","ゾ");
		normMap.add("ﾀﾞ","ダ");
		normMap.add("ﾁﾞ","ヂ");
		normMap.add("ﾂﾞ","ヅ");
		normMap.add("ﾃﾞ","デ");
		normMap.add("ﾄﾞ","ド");
		normMap.add("ﾊﾟ","パ");
		normMap.add("ﾋﾟ","ピ");
		normMap.add("ﾌﾟ","プ");
		normMap.add("ﾍﾟ","ペ");
		normMap.add("ﾎﾟ","ポ");
		normMap.add("ﾊﾞ","バ");
		normMap.add("ﾋﾞ","ビ");
		normMap.add("ﾌﾞ","ブ");
		normMap.add("ﾍﾞ","ベ");
		normMap.add("ﾎﾞ","ボ");
		normMap.add("ｳﾞ","ヴ");
		normMap.add("ﾜﾞ","ヷ");
		normMap.add("ｦﾞ","ヺ");
	}

	public HalfwidthKanaVoicedMappingFilter(Reader in) {
		super(normMap, in);
	}
	public HalfwidthKanaVoicedMappingFilter(CharStream in) {
		super(normMap, in);
	}
}
