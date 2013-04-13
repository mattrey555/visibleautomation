package com.example.android.apis.test;

import java.io.IOException;

import com.androidApp.Test.RecordTest;
import com.example.android.apis.ApiDemos;


public class ApiDemosTest extends RecordTest<ApiDemos> {

	public ApiDemosTest() throws IOException {
		super(ApiDemos.class);
	}
	
	public ApiDemosTest(Class<ApiDemos> activityClass) throws IOException {
		super(activityClass);
	}
	
	@Override
	public void initializeResources() {
		addRdotID(new com.example.android.apis.R.id());
		addRdotString(new com.example.android.apis.R.string());	
	}
	
	public void setUp() throws Exception { 
		super.setUp();
	}
}