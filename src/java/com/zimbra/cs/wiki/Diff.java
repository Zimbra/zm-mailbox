package com.zimbra.cs.wiki;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;

import com.zimbra.common.util.ByteUtil;

/*************************************************************************
 *
 * http://www.cs.princeton.edu/introcs/96optimization/
 * Copyright © 2007, Robert Sedgewick and Kevin Wayne. 
 *
 *    Longest common subsequence. Now we consider a more sophisticated 
 *    application of dynamic programming to a central problem arising in 
 *    computational biology and other domains. Given two strings s and t, 
 *    we wish to determine how similar they are. Some examples include: 
 *    comparing two DNA sequences for homology (similarity), two English 
 *    words for spelling, two Java files for repeated code. It also arises 
 *    in molecular biology, gas chromatography, and bird song analysis. One 
 *    simple strategy is to find the length of the longest common subsequence 
 *    (LCS). If we delete some characters from s and some characters from t, 
 *    and the resulting two strings are equal, we call the resulting string 
 *    a common subsequence. The LCS problem is to find a common subsequence 
 *    that is as long as possible. For example the LCS of 
 *    ggcaccacg and acggcggatacg is ggcaacg.
 *
 *        --ggc--a-ccacg
 *        acggcggat--acg
 *
 *    Now we describe a systematic method for computing the LCS of two 
 *    strings x and y using dynamic programming. Let M and N be the lengths 
 *    of x and y, respectively. We use the notation x[i..M] to denote the 
 *    suffix of x starting at position i, and y[j..N] to denote the suffix 
 *    of y starting at position j. If x and y begin with the same letter, 
 *    then we should include that first letter in the LCS. Now our problem 
 *    reduces to finding the LCS of the two remaining substrings x[1..M] and 
 *    y[1..N]. On the other hand, if the two strings start with different 
 *    letters, both characters cannot be part of a common subsequence, so 
 *    we must remove one or the other. In either case, the problem reduces 
 *    to finding the LCS of two strings, at least one of which is strictly 
 *    shorter. If we let opt[i][j] denote the length of the LCS of x[i..M] 
 *    and y[j..N], then the following recurrence expresses it in terms of 
 *    the length of LCSs of shorter suffixes.
 *
 *    formula for longest common substring
 *
 *        opt[i][j] = 0                              if i = M or j = N
 *                  = opt[i+1][j+1] + 1              if xi = yj
 *                  = max(opt[i][j+1], opt[i+1][j])  otherwise
 *
 *    Program LCS.java is a bottom-up translation of this recurrence. We 
 *    maintain a two dimensional array opt[i][j] that is the length of the 
 *    LCS for the two strings x[i..M] and y[j..N]. For the input strings 
 *    ggcaccacg and acggcggatacg, the program computes the following table 
 *    by filling in values from right-to-left (j = N-1 to 0) and bottom-to-top 
 *    (i = M-1 to 0).
 *
 *               0  1  2  3  4  5  6  7  8  9 10 11 12
 *        x\y    a  c  g  g  c  g  g  a  t  a  c  g  
 *        --------------------------------------------
 *        0 g    7  7  7  6  6  6  5  4  3  3  2  1  0  
 *        1 g    6  6  6  6  5  5  5  4  3  3  2  1  0
 *        2 c    6  5  5  5  5  4  4  4  3  3  2  1  0  
 *        3 a    6  5  4  4  4  4  4  4  3  3  2  1  0  
 *        4 c    5  5  4  4  4  3  3  3  3  3  2  1  0  
 *        5 c    4  4  4  4  4  3  3  3  3  3  2  1  0  
 *        6 a    3  3  3  3  3  3  3  3  3  3  2  1  0  
 *        7 c    2  2  2  2  2  2  2  2  2  2  2  1  0  
 *        8 g    1  1  1  1  1  1  1  1  1  1  1  1  0  
 *        9      0  0  0  0  0  0  0  0  0  0  0  0  0  
 *
 *    One final challenge is to recover the optimal solution itself, not 
 *    just its value. The key idea is to retrace the steps of the dynamic 
 *    programming algorithm backwards, re-discovering the path of choices 
 *    (highlighted in red in the table above) from opt[0][0] to opt[M][N]. 
 *    To determine the choice that led to opt[i][j], we consider the three 
 *    possibilities:
 *
 *        o x[i] matches y[j]. In this case, we must have opt[i][j] = 
 *          opt[i+1][j+1] + 1, and the next character in the LCS is x[i].
 *
 *        o The LCS does not contain x[i]. In this case, opt[i][j] = opt[i+1][j].
 *
 *        o The LCS does not contain y[j]. In this case, opt[i][j] = opt[i][j+1]. 
 *
 *    The algorithm takes time and space proportional to MN. 
 *
 *    Space usage. Usually with dynamic programming you run out of space 
 *    before time. However, sometimes it is possible to avoid using an 
 *    M-by-N array and getting by with just one or two arrays of length 
 *    M and N. For example, it is not hard to modify Binomial.java to do 
 *    exactly this. (See Exercise 1.) Similarly, for the longest common 
 *    subsequence problem, it is easy to avoid the 2D array if you only 
 *    need the length of LCS. Finding the alignment itself in linear space 
 *    is substantially more challenging (but possible using Hirschberg's 
 *    divide-and-conquer algorithm).
 *
 *    Dynamic programming history. Bellman. LCS by Robinson in 1938. 
 *    Cocke-Younger-Kasami (CYK) algorithm for parsing context free grammars, 
 *    Floyd-Warshall for all-pairs shortest path, Bellman-Ford for arbitrage 
 *    detection (negative cost cycles), longest common subsequence for diff, 
 *    edit distance for global sequence alignment, bitonic TSP. Knapsack 
 *    problem, subset sum, partitioning. Application = multiprocessor 
 *    scheduling, minimizing VLSI circuit size. 
 *
 *************************************************************************/

public class Diff {

	public static final long MAX_LENGTH = 1024 * 1024;  // 1MB
	public enum Type { common, first, second };
	
	public static class Chunk {
		public Type disposition;
		public ArrayList<String> content;
		public Chunk(String c, Type disp) {
			content = new ArrayList<String>();
			content.add(c); disposition = disp;
		}
	}

	private static void addString(ArrayList<Chunk> chunks, String str, Type disp) {
		if (chunks.size() == 0) {
			chunks.add(new Chunk(str, disp));
			return;
		}
		Chunk c = chunks.get(chunks.size()-1);
		if (c.disposition == disp)
			c.content.add(str);
		else
			chunks.add(new Chunk(str, disp));
	}
	
	private static String[] readInputStream(InputStream in) throws IOException {
		byte[] buf = ByteUtil.getContent(in, -1);
		return new String(buf, "UTF-8").split("\\n");
	}
	
    public static Collection<Chunk> getResult(InputStream i1, InputStream i2) throws IOException {
        String[] x = readInputStream(i1);
        String[] y = readInputStream(i2);

        ArrayList<Chunk> result = new ArrayList<Chunk>();
        
        // number of lines of each file
        int M = x.length;
        int N = y.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+1][N+1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
                if (x[i].equals(y[j]))
                    opt[i][j] = opt[i+1][j+1] + 1;
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        int i = 0, j = 0;
        while(i < M && j < N) {
            if (x[i].equals(y[j])) {
            	addString(result, x[i], Type.common);
                i++; j++;
            } else if (opt[i+1][j] >= opt[i][j+1]) {
            	addString(result, x[i++], Type.first);
            } else {
            	addString(result, y[j++], Type.second);
            }
        }

        // dump out one remainder of one string if the other is exhausted
        while(i < M || j < N) {
            if (i == M) {
            	addString(result, y[j++], Type.second);
            } else if (j == N) {
            	addString(result, x[i++], Type.first);
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) throws Exception {
    	java.io.FileInputStream f1 = new java.io.FileInputStream(args[0]);
    	java.io.FileInputStream f2 = new java.io.FileInputStream(args[1]);
    	Collection<Chunk> chunks = Diff.getResult(f1, f2);
    	for (Chunk c : chunks) {
    		switch (c.disposition) {
    		case common:
    			continue;
    		case first:
    			for (String str : c.content)
    				System.out.println("< "+str);
    			break;
    		case second:
    			for (String str : c.content)
    				System.out.println("> "+str);
    			break;
    		}
    	}
    }
}

