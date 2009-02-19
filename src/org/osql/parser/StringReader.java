package org.osql.parser;

import java.io.CharArrayReader;
import java.io.IOException;



public class StringReader
extends CharArrayReader {


	public StringReader(String string) {
		super(string.toCharArray());
		// TODO Auto-generated constructor stub
	}


/*	public StringReader(String string, int arg1, int arg2) {
		super(string.toCharArray(), arg1, arg2);
		// TODO Auto-generated constructor stub
	}
*/
	
	public void reset(String string)
	throws IOException {
		buf = string.toCharArray();
		pos = 0;
		count = string.length();
		markedPos = 0;
		super.reset();
//		reset();
	}
	
	
}
