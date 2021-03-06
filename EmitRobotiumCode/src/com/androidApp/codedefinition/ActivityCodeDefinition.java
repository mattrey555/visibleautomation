package com.androidApp.codedefinition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import com.androidApp.emitter.EmitterException;
import com.androidApp.emitter.IEmitCode;
import com.androidApp.emitter.IEmitCode.LineAndTokens;
import com.androidApp.util.Constants;
import com.androidApp.util.SuperTokenizer;

/**
 * code can either be output in the the main thread (indicated with mActivityName = Constants.MAIN)
 * in an conditional activity case, or in an conditional dialog case.  activities are compared
 * by name, but dialogs are compared by the activity, dialog tag, scan type (content or title) and
 * tag type (id or text).  These are the tags for the output code hashtable, so we know where to 
 * write the output code (main, activity condition, dialog condition), and how to tag it (in
 * the dialog condition case)
 * views are tested in the normal way that views are retrieved, either by id, or class+index
 * Add: the previousLine is tested against for conditions to insert the actual code while we are parsing.
 * @author matt2
 *
 */

public class ActivityCodeDefinition extends CodeDefinition {
	
	public ActivityCodeDefinition() {
		mType = Type.ACTIVITY;
	}

	// code definition for main activity code
	public ActivityCodeDefinition(String activityName) {
		super(activityName);
		mType = Type.ACTIVITY;
	}
	
	// code definition for interstitial activity function definitions
	public ActivityCodeDefinition(String activityName, String codeType, String functionName, List<String> precedingTokens) {
		super(activityName, codeType, functionName, precedingTokens);
		mType = Type.ACTIVITY;
	}
	
	/**
	 * parse a code definition from a string.
	 * ACTIVITY: activityClass
	 * DIALOG: activityName, dialog_tag, scanType <title|content>, tagType <id|text>
	 * @param s
	 * @return
	 * @throws EmitterException
	 */
	public static ActivityCodeDefinition parse(String s) {
		SuperTokenizer st = new SuperTokenizer(s, "\"", ":,", '\\');
		List<String> tokens = st.toList();
		String tokenType = tokens.get(0);
		if (tokens.size() == 2) {
			return new ActivityCodeDefinition(tokens.get(1));
		} else {
			String activityName = tokens.get(1);
			String codeType = tokens.get(2);
			String functionName = tokens.get(3);
			return new ActivityCodeDefinition(activityName, codeType, functionName, null);
		}
	}
		
	/**
	 * is this code defintion the same as another code definition?
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ActivityCodeDefinition) {
			ActivityCodeDefinition b = (ActivityCodeDefinition) o;
			if (mType == Type.ACTIVITY) {
				if (mActivityName.equals(b.mActivityName) && mCodeType.equals(b.mCodeType)) {
					return mFunctionName.equals(b.mFunctionName);
				}
			} 
		}
		return false;
	}
	
	
	@Override
	public int hashCode() {
		return mActivityName.hashCode() + mFunctionName.hashCode() + mCodeType.hashCode();
	}
	public String getActivityName() {
		return mActivityName;
	}
	

	public String toString() {
		return mType + "," + mActivityName + "," + mCodeType + "," + mFunctionName;
	}
	
	// generate the code suitable for a handler java class
	public String toCode() {
		return "ActivityCodeDefinition(" + mActivityName + ".class"+ ")";
	}
	
	// generate the code suitable for a handler java class
	public String toBinaryCode() {
		return "ActivityCodeDefinition(" + mActivityName + ".class"+ ")";
	}
}
