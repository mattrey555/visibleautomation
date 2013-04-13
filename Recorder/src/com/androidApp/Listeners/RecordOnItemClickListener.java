package com.androidApp.Listeners;
import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.EventRecorder.ListenerIntercept;
import com.androidApp.EventRecorder.ViewReference;
import com.androidApp.Utility.Constants;

import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;

// record item clicks for listviews
public class RecordOnItemClickListener extends RecordListener implements AdapterView.OnItemClickListener, IOriginalListener  {
	protected AdapterView.OnItemClickListener	mOriginalItemClickListener;
	
	public RecordOnItemClickListener(EventRecorder eventRecorder, AdapterView<?> adapterView) {
		super(eventRecorder);
		mOriginalItemClickListener = adapterView.getOnItemClickListener();
		adapterView.setOnItemClickListener(this);
	}
	
	public RecordOnItemClickListener(EventRecorder eventRecorder, AdapterView.OnItemClickListener originalListener) {
		super(eventRecorder);
		mOriginalItemClickListener = originalListener;
	}
		
	public Object getOriginalListener() {
		return mOriginalItemClickListener;
	}

	/**
	 * record the the onItemClick event
	 * output:
	 * item_click:<time>,position,<reference>,<description>
	 *  @param parent parent adapter
	 *  @param view selected view
	 *  @param position index in adapter
	 *  @param id adapter item id
	 */

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		boolean fReentryBlock = getReentryBlock();
		if (!RecordListener.getEventBlock()) {
			setEventBlock(true);
			try {
				mEventRecorder.writeRecord(Constants.EventTags.ITEM_CLICK, position + "," + ViewReference.getClassIndexReference(parent) + "," + getDescription(view));
			} catch (Exception ex) {
				mEventRecorder.writeRecord(Constants.EventTags.EXCEPTION, view, "item click");
				ex.printStackTrace();
			}	
		}
		if (!fReentryBlock) {
			if (mOriginalItemClickListener != null) {
				mOriginalItemClickListener.onItemClick(parent, view, position, id);
			} 
		}
		setEventBlock(false);
	}
}