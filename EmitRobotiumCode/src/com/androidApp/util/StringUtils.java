package com.androidApp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

public class StringUtils {
	
	
	public static boolean isEmpty(String s) {
		return (s == null) || s.equals("");
	}
	
	/**
	 * is s not blank? (i.e has something other than a whiteapce
	 * @param s
	 * @return true if s is not blank
	 */
	public static boolean isNotBlank(String s) {
		if (StringUtils.isEmpty(s)) {
			return false;
		}
		for (int ich = 0; ich < s.length(); ich++) {
			if (!Character.isWhitespace(s.charAt(ich))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * is s blank? empty or all whitespace?
	 * @param s
	 * @return
	 */
	public static boolean isBlank(String s) {
		if (StringUtils.isEmpty(s)) {
			return true;
		}
		for (int ich = 0; ich < s.length(); ich++) {
			if (!Character.isWhitespace(s.charAt(ich))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * make a string digstible for regexp.
	 * @param s
	 * @return string with all non alphanumeric characters replaced with .*
	 */
	 public static String regexDigest(String s) {
		 StringBuffer sb = new StringBuffer(s.length());
		 for (int ich = 0; ich < s.length(); ich++) {
			 char ch = s.charAt(ich);
			 if (Character.isLetterOrDigit(ch)) {
				 sb.append(ch);
			 } else {
				 sb.append(".*");
			 }
		 }
		 return sb.toString();
	 }
	 
	// escape a string with a prefix character "for example \ 'escape this'" -> \"for example \\ \'escape this\'\"
	public static String escapeString(String s, String escapeChars, char prefix) {
		StringBuffer sb = new StringBuffer(s.length());
		for (int ich = 0; ich < s.length(); ich++) {
			char ch = s.charAt(ich);
			if (escapeChars.indexOf(ch) != -1) {
				sb.append(prefix);
			}
			sb.append(ch);
		}
		return sb.toString();
	}
	
	// unescape a string,  Strip the prefixes.
	public static String unescapeString(String s, char prefix) {
		StringBuffer sb = new StringBuffer(s.length());
		for (int ich = 0; ich < s.length(); ich++) {
			char ch = s.charAt(ich);
			if (ch == prefix) {
				continue;
			} 
			sb.append(ch);
		}
		return sb.toString();
	}
	
	// strip the quotes surrounding the string
	public static String stripQuotes(String s) {
		if ((s.charAt(0) == '"') && (s.charAt(s.length() - 1) == '"')){
			return s.substring(1, s.length() - 1);
		} else {
			return s;
		}
	}

	/**
	 * is s a hexidecimal number
	 * @param s
	 * @return true if 0x<digits>
	 */
	public static boolean isHexNumber(String s) {
		if ((s.charAt(0) != '0') || (s.charAt(1) != 'x')) {
			return false;
		}
		for (int ich = 2; ich < s.length(); ich++) {
			char ch = s.charAt(ich);
			if (!Character.isDigit(ch) && ((Character.toLowerCase(ch) >= 'a') || (Character.toLowerCase(ch) <= 'f'))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * is s a number
	 * @param s string to test
	 * @return true if digits and length > 0, false otherwise.
	 */
	public static boolean isNumber(String s) {
		if (s.length() == 0) {
			return false;
		}
		for (int ich = 0; ich < s.length(); ich++) {
			char ch = s.charAt(ich);
			if (!Character.isDigit(ch)) {
				return false;
			}
		}
		return true;
	}	 

	
	/**
	 * is s a quoted string
	 * @param s
	 * @return true if the string is surrounded by quotes.
	 */
	public static boolean isQuotedString(String s) {
		return (s.charAt(0) == '\"') && (s.charAt(s.length() - 1) == '\"');
	}
	
	/**
	 * return foo from com.example.foo
	 * @param s com.example.foo
	 * @return foo
	 */
	public static String getNameFromClassPath(String s) {
		int ich = s.lastIndexOf('.');
		if (ich != -1) {
			return s.substring(ich + 1);
		} else {
			return s;
		}
	}
	
	/**
	 * return com.example from com.example.foo
	 * @param s com.example.foo
	 * @return com.example
	 */

	public static String getPackageFromClassPath(String className) {
		int ich = className.lastIndexOf('.');
		if (ich != -1) {
			return className.substring(9, ich);
		} else {
			return className;
		}
	}
	
	/**
	 * given a list of strings, return the concatenated string with delimiters.
	 * @param stringList list of strings to concatenate
	 * @param delimiter to stick in between them
	 */
	public static String concatStringList(List<String> stringList, String delimiter) {
		StringBuffer sb = new StringBuffer();
		for (Iterator<String> iter = stringList.iterator(); iter.hasNext(); ) {
			String s = iter.next();
			sb.append(s);
			if (iter.hasNext()) {
				sb.append(delimiter);
			}
		}
		return sb.toString();
	}
}