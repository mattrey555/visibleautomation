package com.androidApp.Intercept;

import java.io.IOException;
import java.util.List;

import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.Test.ActivityInterceptor;
import com.androidApp.Test.R;
import com.androidApp.Utility.Constants;
import com.androidApp.Utility.FileUtils;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * interstitial_activity activity_name: operations between activity_forward and activity_back on this activity are written
 * into an InterstitialActivityHandler which issues the operations whenever the activity is entered
 * copy_text view_reference: copy text
 * toggle_on view_reference: set the toggle on (don't toggle on/off)
 * toggle_off view_refernce: set the toggle off (don't toggle on/off)
 * @author matt2
 *
 */
public class MagicOverlay extends View implements OnGestureListener {
	protected static final String					TAG = "MagicOverlay";
	protected static final float					FLING_THRESHOLD = 4000.0F;
	protected static final int						MIN_TEXT_OFFSET = 100;			// draw view class text above/below view
	public static final int							MAGIC_BUTTON_ID = 0xdeadbeef;	// so we defeat event intercept
	protected ImageView 							mButton;						// button to display magic overlays
	protected GestureDetector 						mGestureDetector;				// for swipe to hide overlays
	protected Paint									mBackgroundPaint;				// paint grey overlay
	protected Paint									mViewPaint;						// paint green border around current view.
	protected Paint									mTextPaint;						// solid color for label of view class
	protected EventRecorder							mRecorder;						// to record events
	protected ClickMode								mMode;							// top-level or view mode
	protected View									mContentView;					// content referenced by this overlay
	protected View									mCurrentView;					// currently selected view of ALL overlays
	protected ActivityInterceptor.ActivityState		mActivityState;					// contains list of active overlays
	protected boolean								mfEnabled;						// slid-in
	protected Rect									mCurrentViewRect;				// performance onDraw()
	protected Rect									mOverlayViewRect;				// perf onDraw() to detect if in our overlay.
	protected DirectiveDialogs						mDirectiveDialogs;				// context dialogs to issue directives.
	
	protected enum ClickMode {
		BASE,
		VIEW_SELECT
	}
	
	/** 
	 * create the magic overlay, which allows us to interrogate views from the UI
	 * @param magicFrame the frame that we insert in the view hierarchy
	 * @param recorder
	 * @throws IOException
	 */
	public static void addMagicOverlay(MagicFrame magicFrame, EventRecorder recorder, ActivityInterceptor.ActivityState activityState) throws IOException, ClassNotFoundException {
		View contentView = magicFrame.getChildAt(0);

		// don't display the button for action bars
		// FAIL FAIL FAIL FAIL
		try {
			Class<? extends View> actionBarContainerClass = (Class<? extends View>) Class.forName(Constants.Classes.ACTION_BAR_CONTAINER);
			boolean fDisplayButton = !actionBarContainerClass.isAssignableFrom(contentView.getClass());
			MagicOverlay createOverlay = new MagicOverlay(magicFrame, fDisplayButton, activityState, recorder, contentView);
			activityState.addMagicOverlay(createOverlay);
			
			// we sort of have to fix our layout, since we're offscreen or onscreen depending on mfEnabled. This means, of course.
			// that we need to be created before the content is laid out.
			FrameLayout.LayoutParams overlayLayoutParams = new FrameLayout.LayoutParams(contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
			overlayLayoutParams.setMargins(contentView.getMeasuredWidth(), 0, 0, 0);
			createOverlay.setLayoutParams(overlayLayoutParams);
			createOverlay.setBackgroundColor(0x0);
			createOverlay.setWillNotDraw(false);
			createOverlay.bringToFront();
			createOverlay.setFocusable(true);
			createOverlay.setActivated(true);
			createOverlay.setEnabled(true);
			magicFrame.addView(createOverlay);
			if (createOverlay.getButton() != null) {
				magicFrame.addView(createOverlay.getButton());
				createOverlay.getButton().bringToFront();
			}
			magicFrame.requestLayout();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public MagicOverlay(Context context) {
		super(context);
	}
	
	public MagicOverlay(MagicFrame 							magicFrame, 
					    boolean 							fButton, 
					    ActivityInterceptor.ActivityState 	activityState,
					    EventRecorder						eventRecorder,
					    View								contentView) throws IOException {
		super(magicFrame.getContext());
		mContentView = contentView;
		mRecorder = eventRecorder;
		mActivityState = activityState;
		Context context = magicFrame.getContext();
		if (fButton) {
			mButton = createButton(context, contentView);
			mButton.setOnClickListener(new SlideInClickListener());
		}
		mBackgroundPaint = MagicOverlay.createBackgroundPaint();
		mViewPaint = MagicOverlay.createViewPaint();
		mTextPaint = MagicOverlay.createTextPaint();
		mCurrentViewRect = new Rect();
		mOverlayViewRect = new Rect();
		mGestureDetector = new GestureDetector(context, this);
		mMode = ClickMode.BASE;
		mCurrentView = null;
		mfEnabled = false;
		mDirectiveDialogs = new DirectiveDialogs(this);
	}
	
	public static Paint createBackgroundPaint() {
		Paint backgroundPaint = new Paint();
		backgroundPaint.setColor(0x0);
		backgroundPaint.setAlpha(128);
		backgroundPaint.setStyle(Style.FILL);
		return backgroundPaint;	
	}
	
	public static Paint createViewPaint() {
		Paint viewPaint = new Paint();
		viewPaint.setColor(0xff00ff00);
		viewPaint.setStyle(Style.STROKE);
		viewPaint.setStrokeWidth(2.0F);
		viewPaint.setTextSize(32.0F);
		return viewPaint;
	}
	
	public static Paint createTextPaint() {
		Paint viewPaint = new Paint();
		viewPaint.setColor(0xff00ff00);
		viewPaint.setStyle(Style.FILL);
		viewPaint.setStrokeWidth(2.0F);
		viewPaint.setTextSize(32.0F);
		return viewPaint;
	}
	
	// accessors
	public EventRecorder getEventRecorder() {
		return mRecorder;
	}
	
	public View getCurrentView() {
		return mCurrentView;
	}
	
	public ActivityInterceptor.ActivityState getActivityState() {
		return mActivityState;
	}
	
	public ClickMode getClickMode() {
		return mMode;
	}
	
	public void setClickMode(ClickMode mode) {
		mMode = mode;
	}
	
	public void resetCurrentView() {
		mCurrentView = mContentView;
	}
	
	/**
	 * create the button that goes on the right hand side of the overlay
	 * @param context
	 * @param contentView
	 * @return
	 * @throws IOException
	 */
	public static ImageView createButton(Context context, View contentView) throws IOException {
		// scale the button to the screen size
		ImageView button = new ImageView(context);
		button.setId(MAGIC_BUTTON_ID);
		byte[] buttonData = FileUtils.readJarBinaryResource(MagicOverlay.class, "/raw/left_arrow_button.png");
		Bitmap buttonBmp = BitmapFactory.decodeByteArray(buttonData, 0, buttonData.length);
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int buttonSize = Math.min(size.x, size.y)/10;
		@SuppressWarnings("deprecation")
		BitmapDrawable drawable = new BitmapDrawable(buttonBmp);
		button.setImageDrawable(drawable);
		FrameLayout.LayoutParams buttonLayoutParams = new FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.CENTER_VERTICAL|Gravity.RIGHT);
		button.setLayoutParams(buttonLayoutParams);
		button.bringToFront();
		return button;
	}
	
	public ImageView getButton() {
		return mButton;
	}
	
	/**
	 * slide in the overlays when the user clicks the right-hand button
	 * @author matt2
	 *
	 */
	protected class SlideInClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			for (MagicOverlay overlay : mActivityState.getMagicOverlayList()) {
				FrameLayout.LayoutParams overlayLayoutParams = (FrameLayout.LayoutParams) MagicOverlay.this.getLayoutParams();
				overlayLayoutParams.setMargins(0, 0, 0, 0);
				MagicOverlay.this.setX(MagicOverlay.this.mContentView.getX());
/*
 * for some reason, the slide-in animaton doesn't work
				TranslateAnimation slideInAnimation = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 1.0F,
																			 TranslateAnimation.RELATIVE_TO_SELF, 0.0F,
																			 TranslateAnimation.RELATIVE_TO_SELF, 0.0F,
																			 TranslateAnimation.RELATIVE_TO_SELF, 0.0F);
				slideInAnimation.setDuration(500);
				overlay.startAnimation(slideInAnimation);
				new Handler().postDelayed(new SlideCompleteRunnable(slideInAnimation, overlay, true), slideInAnimation.getDuration());
*/
			}
		}	
	}
	/**
	 * slide out the overlays when the user flings the view to the right 
	 * TODO: only to the right, please
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (velocityX > FLING_THRESHOLD) {
			for (MagicOverlay overlay : mActivityState.getMagicOverlayList()) {
				TranslateAnimation slideOutAnimation = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0.0F,
																			  TranslateAnimation.RELATIVE_TO_SELF, 1.0F,
																			  TranslateAnimation.RELATIVE_TO_SELF, 0.0F,
																			  TranslateAnimation.RELATIVE_TO_SELF, 0.0F);
				slideOutAnimation.setDuration(500);
				overlay.startAnimation(slideOutAnimation);
				new Handler().postDelayed(new SlideCompleteRunnable(slideOutAnimation, overlay, false), slideOutAnimation.getDuration());
				mMode = ClickMode.BASE;
				mCurrentView = mContentView;
			}
			return true;
		} 
		return false;
	}

	/**
	 * when the slide is complete, reset the left coordinate of the specified overlay
	 * @author matt2
	 * TODO: unused: there's problems with the animation
	 */
	protected class SlideCompleteRunnable implements Runnable {
		protected MagicOverlay 	mOverlay;
		protected boolean		mfEnable;
		protected Animation		mAnimation;
		
		public SlideCompleteRunnable(Animation animation, MagicOverlay overlay, boolean fEnable) {
			mAnimation = animation;
			mOverlay = overlay;
			mfEnable = fEnable;
		}

		@Override
		public void run() {
			long maxWait = mAnimation.getDuration()*2;
			while (!mAnimation.hasEnded() && (maxWait > 0)) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException iex) {
				}
				maxWait -= 100;
			}
			FrameLayout.LayoutParams overlayLayoutParams = (FrameLayout.LayoutParams) MagicOverlay.this.getLayoutParams();
			if (mfEnable) {
				overlayLayoutParams.setMargins(0, 0, 0, 0);
				MagicOverlay.this.setX(MagicOverlay.this.mContentView.getX());
			} else {
				overlayLayoutParams.setMargins(MagicOverlay.this.mContentView.getMeasuredWidth(), 0, 0, 0);
				MagicOverlay.this.setX(MagicOverlay.this.mContentView.getX() + MagicOverlay.this.mContentView.getMeasuredWidth());
			}
			MagicOverlay.this.setLayoutParams(overlayLayoutParams);
			MagicOverlay.this.getParent().requestLayout();

		}
	}
			
	
	public boolean onTouchEvent(MotionEvent me) {
		if (!mGestureDetector.onTouchEvent(me)) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		mDirectiveDialogs.viewDialog(MagicOverlay.this.getContext(), e);
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,float distanceY) {
		return false;
	}
	
	@Override
	public void onShowPress(MotionEvent e) {		
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (mMode == ClickMode.BASE) {
			String[] baseItems = new String[] { "Interstitial Activity", "View Selection" };
			Dialog dialog = mDirectiveDialogs.createSelectionDialog(MagicOverlay.this.getContext(), baseItems, mDirectiveDialogs.new OnBaseDialogSelectionListener());
			dialog.show();
		} else if (mMode == ClickMode.VIEW_SELECT) {
			Rect currentViewRect = new Rect();
			int eventX = (int) e.getX();
			int eventY = (int) e.getY();
			Rect thisRect = new Rect();
			this.getGlobalVisibleRect(thisRect);
			int offsetX = -thisRect.left;
			int offsetY = -thisRect.top;
			
			// if the current view is a view group, and the event is inside one of its children
			// then set the current view to that child
			if (mCurrentView instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) mCurrentView;
				mCurrentView.getGlobalVisibleRect(currentViewRect);
				currentViewRect.offset(offsetX, offsetY);
				if (currentViewRect.contains(eventX, eventY)) {
					for (int iChild = 0; iChild < vg.getChildCount(); iChild++) {
						View vChild = vg.getChildAt(iChild);
						if ((vChild.getVisibility() == View.VISIBLE) && vChild.getGlobalVisibleRect(mCurrentViewRect)) {
							mCurrentViewRect.offset(offsetX, offsetY);
							if (mCurrentViewRect.contains(eventX, eventY)) {
								mCurrentView = vChild;
								invalidate();
								return true;						
							}
						}
					}
				}
			}
			
			// otherwise, keep skipping up views until we find one that contains the event, or until we
			// reach the content view.
			while (mCurrentView != mContentView) {
				mCurrentView = (View)  mCurrentView.getParent();
				mCurrentView.getGlobalVisibleRect(currentViewRect);
				currentViewRect.offset(offsetX, offsetY);
				if (currentViewRect.contains(eventX, eventY)) {
					break;
				}
			}
			invalidate();
		}
		return false;
	}
	
	/**
	 * draw a rectangle around the current view, with a label of the view class
	 */
	@Override
	public void onDraw(Canvas canvas) {
		Rect rt = new Rect(0, 0, getWidth(), getHeight());
		canvas.drawRect(rt,  mBackgroundPaint);
		
		// need to ensure that the current view is inside our view.
		if (mCurrentView != null) {
			mCurrentView.getGlobalVisibleRect(mCurrentViewRect);
			getGlobalVisibleRect(mOverlayViewRect);
			if (mOverlayViewRect.contains(mCurrentViewRect.left, mCurrentViewRect.top) ||
				mOverlayViewRect.contains(mCurrentViewRect.right, mCurrentViewRect.bottom)) {
				mCurrentViewRect.offset(-mOverlayViewRect.left, -mOverlayViewRect.top);
				canvas.drawRect(mCurrentViewRect, mViewPaint);
				String text = mCurrentView.getClass().getSimpleName();

				if (mCurrentViewRect.top > MIN_TEXT_OFFSET) {
					// top above
					canvas.drawText(mCurrentView.getClass().getSimpleName(), mCurrentViewRect.left, mCurrentViewRect.top, mTextPaint);					
				} else if (mOverlayViewRect.bottom - mCurrentViewRect.bottom > MIN_TEXT_OFFSET) {
					// below above.
					canvas.drawText(mCurrentView.getClass().getSimpleName(), mCurrentViewRect.left, mCurrentViewRect.bottom + mViewPaint.getTextSize(), mTextPaint);					
				} else {
					// top below
					canvas.drawText(mCurrentView.getClass().getSimpleName(), mCurrentViewRect.left, mCurrentViewRect.top + mViewPaint.getTextSize(), mTextPaint);										
				}
			}				
		}
		super.onDraw(canvas);
	}
}
