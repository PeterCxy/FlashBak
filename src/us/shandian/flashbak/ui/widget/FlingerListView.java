package us.shandian.flashbak.ui.widget;

import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.os.*;
import android.util.*;
import android.content.*;
import android.content.res.Resources;
import android.database.*;
import android.graphics.*;

import java.util.HashMap;
import java.lang.Throwable;
import java.lang.StackTraceElement;

import us.shandian.flashbak.R;
import android.widget.*;
import android.renderscript.*;

public class FlingerListView extends ListView
{
	
	private class AdapterWrapper implements ListAdapter {
		
		private ListAdapter mAdapter;
		private Context mContext;
		private HashMap<View, View> mViews = new HashMap<View, View>();
		
		public AdapterWrapper(Context context, ListAdapter adapter) {
			mContext = context;
			mAdapter = adapter;
		}
		
		@Override
		public Object getItem(int position) {
			// Wrap
			return mAdapter.getItem(position);
		}
		
		@Override
		public long getItemId(int position) {
			// Wrap
			return mAdapter.getItemId(position);
		}
		
		@Override
		public int getCount() {
			// Wrap
			return mAdapter.getCount();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Wrap and surround
			
			View orig = mAdapter.getView(position, convertView, parent);
			if (orig == convertView) {
				return orig;
			}
			View nv = mViews.get(orig);
			if (nv == null) {
				nv = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.flingerlistview_wrapper, null);
				((ViewGroup) nv).addView(orig, 0);
				mViews.put(orig, nv);
			}
			return nv;
		}
		
		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			// Wrap
			mAdapter.unregisterDataSetObserver(observer);
		}

		@Override
		public int getItemViewType(int position) {
			// Wrap
			return mAdapter.getItemViewType(position);
		}
		
		@Override
		public boolean isEmpty() {
			// Wrap
			return mAdapter.isEmpty();
		}
		
		@Override
		public int getViewTypeCount() {
			// Wrap
			return mAdapter.getViewTypeCount();
		}
		
		@Override
		public boolean hasStableIds() {
			// Wrap
			return mAdapter.hasStableIds();
		}
		
		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			// Wrap
			mAdapter.registerDataSetObserver(observer);
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			// Wrap
			return mAdapter.areAllItemsEnabled();
		}
		
		@Override
		public boolean isEnabled(int position) {
			// Wrap
			return mAdapter.isEnabled(position);
		}
	}
	
	public interface OnItemFlingListener {
		
		boolean onItemFling(View view, int position, long id, ViewGroup parent);
		
		boolean onItemFlingEnd(View view, int position, long id, ViewGroup parent);
		
		void onItemFlingCancel(View view, int position, long id, ViewGroup parent);
		
		void onItemFlingOut(int position);
	}
	
	private Context mContext;
	
	private OnItemFlingListener mListener;
	
	private float mStartTouchPositionX;
	private float mStartTouchPositionY;
	private RelativeLayout mFlingingLayout;
	private int mFlingingPos;
	private int mFlingingWidth;
	private int mFlingingHeight;
	private int mColorBackground;
	private int mColorForeground;
	private boolean mColorState = false;
	private boolean mFlingingAllowed = false;
	
	public FlingerListView(Context context) {
		super(context);
		mContext = context;
		init();
	}
	
	public FlingerListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}
	
	public FlingerListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init();
	}
	
	private void init() {
		Resources r = mContext.getResources();
		mColorForeground = r.getColor(R.color.listitem_foreground);
		mColorBackground = r.getColor(R.color.listitem_background);
	}
	
	public void setOnItemFlingListener(OnItemFlingListener listener) {
		mListener = listener;
	}
	
	@Override
	public void setAdapter(ListAdapter adapter) {
		// Wrap
		super.setAdapter(new AdapterWrapper(mContext, adapter));
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		
		float x = ev.getX();
		float y = ev.getY();
		
		if (mFlingingLayout != null) {
			if (mFlingingLayout.getChildAt(0).getAnimation() != null) {
				// Refuse any event when animate
				return false;
			}
		}
		
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: {
				mFlingingPos = -1;
				mColorState = false;
				mStartTouchPositionX = x;
				mStartTouchPositionY = y;
				mFlingingPos = pointToPosition((int) x, (int) y);
				if (mFlingingLayout != null && mFlingingAllowed == true) {
					mFlingingLayout.getChildAt(0).setTranslationX(0);
					mFlingingLayout.getChildAt(0).setBackgroundColor(0);
					mFlingingLayout.setBackgroundColor(0);
					mFlingingLayout = null;
				}
				mFlingingAllowed = false;
				if (mFlingingPos != INVALID_POSITION) {
					mFlingingLayout = (RelativeLayout) super.getChildAt(mFlingingPos);
					Rect r = new Rect();
					mFlingingLayout.getDrawingRect(r);
					mFlingingWidth = r.width();
					mFlingingHeight = r.height();
				} else {
					mFlingingLayout = null;
				}
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				if (mFlingingLayout != null) {
					View child = mFlingingLayout.getChildAt(0);
					if (Math.abs(y - mStartTouchPositionY) < mFlingingHeight && mListener != null) {
						float movedX = x - mStartTouchPositionX;
						float p = movedX / mFlingingWidth;
						float translationX = movedX * (0.7f - Math.abs(p) / 20);
						if (Math.abs(p) > 0.15f || child.getTranslationX() != 0) {
							if (mListener.onItemFling(child, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout)) {
								mFlingingAllowed = true;
								child.setTranslationX(translationX);
								if (!mColorState) {
									mFlingingLayout.setBackgroundColor(mColorBackground);
									child.setBackgroundColor(mColorForeground);
									mColorState = true;
								}
							} else {
								mFlingingAllowed = false;
								child.setTranslationX(0);
							}
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
					if (mFlingingLayout != null && mFlingingAllowed == true) {
						View child = mFlingingLayout.getChildAt(0);
						if (Math.abs(y - mStartTouchPositionY) < mFlingingHeight && mListener != null) {
							float movedX = x - mStartTouchPositionX;
							float p = movedX / mFlingingWidth;
							if (Math.abs(p) > 0.7f) {
								if (mListener.onItemFlingEnd(child, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout)) {
									float translationX = child.getTranslationX();
									TranslateAnimation anim;
									if (translationX > 0) {
										anim = new TranslateAnimation(0, mFlingingWidth, 0, 0);
									} else {
										anim = new TranslateAnimation(0, -mFlingingWidth, 0, 0);
									}
									anim.setDuration(500);
									child.clearAnimation();
									child.setAnimation(anim);
									child.postDelayed(new Runnable() {
										@Override
										public void run() {
											mFlingingLayout.getChildAt(0).clearAnimation();
											mFlingingLayout.getChildAt(0).setTranslationX(0);
											mFlingingLayout.getChildAt(0).setBackgroundColor(0);
											mFlingingLayout.setBackgroundColor(0);
											mListener.onItemFlingOut(mFlingingPos);
											mFlingingLayout = null;
										}
									}, 500);
									anim.startNow();
								} else {
									float translationX = child.getTranslationX();
									TranslateAnimation anim = new TranslateAnimation(0, -translationX, 0, 0);
									anim.setDuration(500);
									child.clearAnimation();
									child.setAnimation(anim);
									child.postDelayed(new Runnable() {
											@Override
											public void run() {
												mFlingingLayout.getChildAt(0).clearAnimation();
												mFlingingLayout.getChildAt(0).setTranslationX(0);
												mFlingingLayout.getChildAt(0).setBackgroundColor(0);
												mFlingingLayout.setBackgroundColor(0);
												mFlingingLayout = null;
											}
										}, 500);
									anim.startNow();
								}
							} else if (Math.abs(p) > 0.15f) {
								mListener.onItemFlingCancel(child, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout);
								float translationX = child.getTranslationX();
								TranslateAnimation anim = new TranslateAnimation(0, -translationX, 0, 0);
								anim.setDuration(500);
								child.clearAnimation();
								child.setAnimation(anim);
								child.postDelayed(new Runnable() {
										@Override
										public void run() {
											mFlingingLayout.getChildAt(0).clearAnimation();
											mFlingingLayout.getChildAt(0).setTranslationX(0);
											mFlingingLayout.getChildAt(0).setBackgroundColor(0);
											mFlingingLayout.setBackgroundColor(0);
											mFlingingLayout = null;
										}
									}, 500);
								anim.startNow();
							} else {
								mListener.onItemFlingCancel(child, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout);
								child.setTranslationX(0);
							}
						}
					}
				// clear
				mStartTouchPositionX = 0;
				mStartTouchPositionY = 0;
				break;
			}
		}
		return super.onTouchEvent(ev);
	}
	
	public void runAfterAnimation(Runnable runnable) {
		if (mFlingingLayout != null) {
			mFlingingLayout.postDelayed(runnable, 500);
		}
	}
	
	private boolean isCallerAndroid() {
		String className = "";
		String myName = this.getClass().getName();
		StackTraceElement[] elements = new Throwable().getStackTrace();
		for (int i = 0; i < elements.length; i++) {
			if (myName.equals(elements[i].getClassName()) && !myName.equals(elements[i + 1].getClassName())) {
				className = elements[i + 1].getClassName();
				break;
			}
		}
		if (className.startsWith("android") || className.startsWith("com.android")) {
			// This call is from the Android framewrok
			return true;
		} else {
			// This call is not from the Android framework
			return false;
		}
	}
	
	@Override
	public View getChildAt(int index) {
		View orig = super.getChildAt(index);
		if (isCallerAndroid()) {
			// Return original layout to Android framework
			return orig;
		}
		
		View child = null;
		try {
			child = ((ViewGroup) orig).getChildAt(0);
		} catch (Exception e) {
			child = null;
		}
		
		if (child == null) {
			return orig;
		} else {
			return child;
		}
	}
		
}
