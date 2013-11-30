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

import us.shandian.flashbak.R;

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
	private View mFlingingChild;
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
			if (mFlingingChild.getAnimation() != null) {
				// Refuse any event when animate
				return false;
			}
		}
		
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: {
				mFlingingPos = -1;
				mFlingingAllowed = false;
				mColorState = false;
				mStartTouchPositionX = x;
				mStartTouchPositionY = y;
				mFlingingPos = pointToPosition((int) x, (int) y);
				if (mFlingingLayout != null && mFlingingAllowed == true) {
					mFlingingChild.setTranslationX(0);
					mFlingingChild.setBackgroundColor(0);
					mFlingingLayout.setBackgroundColor(0);
					mFlingingChild = null;
					mFlingingLayout = null;
				}
				mFlingingAllowed = false;
				if (mFlingingPos != INVALID_POSITION) {
					mFlingingLayout = (RelativeLayout) super.getChildAt(mFlingingPos);
					mFlingingChild = mFlingingLayout.getChildAt(0);
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
					if (Math.abs(y - mStartTouchPositionY) < mFlingingHeight && mListener != null) {
						float movedX = x - mStartTouchPositionX;
						float p = Math.abs(movedX / mFlingingWidth);
						float translationX = movedX * 0.7f;
						if (p > 0.15f || mFlingingChild.getTranslationX() != 0) {
							if (!mFlingingAllowed) {
								mFlingingAllowed = mListener.onItemFling(mFlingingChild, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout);
							}
							if (mFlingingAllowed) {
								mFlingingChild.setTranslationX(translationX);
								mFlingingChild.setAlpha(1.0f - p);
								if (!mColorState) {
									mFlingingLayout.setBackgroundColor(mColorBackground);
									mFlingingChild.setBackgroundColor(mColorForeground);
									mColorState = true;
								}
							} else {
								mFlingingChild.setTranslationX(0);
								mFlingingChild.setAlpha(1.0f);
							}
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
					if (mFlingingLayout != null && mFlingingAllowed == true) {
						if (Math.abs(y - mStartTouchPositionY) < mFlingingHeight && mListener != null) {
							float movedX = x - mStartTouchPositionX;
							float p = movedX / mFlingingWidth;
							if (Math.abs(p) > 0.7f) {
								if (mListener.onItemFlingEnd(mFlingingChild, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout)) {
									float translationX = mFlingingChild.getTranslationX();
									TranslateAnimation anim1;
									if (translationX > 0) {
										anim1 = new TranslateAnimation(0, mFlingingWidth, 0, 0);
									} else {
										anim1 = new TranslateAnimation(0, -mFlingingWidth, 0, 0);
									}
									AlphaAnimation anim2 = new AlphaAnimation(mFlingingChild.getAlpha(), 0f);
									AnimationSet anim = new AnimationSet(true);
									anim.addAnimation(anim1);
									anim.addAnimation(anim2);
									anim.setDuration(500);
									mFlingingChild.clearAnimation();
									mFlingingChild.setAnimation(anim);
									mFlingingChild.postDelayed(new Runnable() {
										@Override
										public void run() {
											mFlingingChild.clearAnimation();
											mFlingingChild.setAlpha(1.0f);
											mFlingingChild.setTranslationX(0);
											mFlingingChild.setBackgroundColor(0);
											mFlingingLayout.setBackgroundColor(0);
											mListener.onItemFlingOut(mFlingingPos);
											mFlingingLayout = null;
											mFlingingChild = null;
										}
									}, 500);
									anim.startNow();
								} else {
									float translationX = mFlingingChild.getTranslationX();
									TranslateAnimation anim1 = new TranslateAnimation(0, -translationX, 0, 0);
									AlphaAnimation anim2 = new AlphaAnimation(mFlingingChild.getAlpha(), 1.0f);
									AnimationSet anim = new AnimationSet(true);
									anim.addAnimation(anim1);
									anim.addAnimation(anim2);
									anim.setDuration(500);
									mFlingingChild.clearAnimation();
									mFlingingChild.setAnimation(anim);
									mFlingingChild.postDelayed(new Runnable() {
											@Override
											public void run() {
												mFlingingChild.clearAnimation();
												mFlingingChild.setAlpha(1.0f);
												mFlingingChild.setTranslationX(0);
												mFlingingChild.setBackgroundColor(0);
												mFlingingLayout.setBackgroundColor(0);
												mFlingingLayout = null;
												mFlingingChild = null;
											}
										}, 500);
									anim.startNow();
								}
							} else if (Math.abs(p) > 0.15f) {
								mListener.onItemFlingCancel(mFlingingChild, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout);
								float translationX = mFlingingChild.getTranslationX();
								TranslateAnimation anim1 = new TranslateAnimation(0, -translationX, 0, 0);
								AlphaAnimation anim2 = new AlphaAnimation(mFlingingChild.getAlpha(), 1.0f);
								AnimationSet anim = new AnimationSet(true);
								anim.addAnimation(anim1);
								anim.addAnimation(anim2);
								anim.setDuration(500);
								mFlingingChild.clearAnimation();
								mFlingingChild.setAnimation(anim);
								mFlingingChild.postDelayed(new Runnable() {
										@Override
										public void run() {
											mFlingingChild.clearAnimation();
											mFlingingChild.setAlpha(1.0f);
											mFlingingChild.setTranslationX(0);
											mFlingingChild.setBackgroundColor(0);
											mFlingingLayout.setBackgroundColor(0);
											mFlingingLayout = null;
											mFlingingChild = null;
										}
									}, 500);
								anim.startNow();
							} else {
								mListener.onItemFlingCancel(mFlingingChild, mFlingingPos, mFlingingLayout.getId(), mFlingingLayout);
								mFlingingChild.setAlpha(1.0f);
								mFlingingChild.setTranslationX(0);
								mFlingingChild.setBackgroundColor(0);
								mFlingingLayout.setBackgroundColor(0);
								mFlingingLayout = null;
								mFlingingChild = null;
							}
						}
					}
				// clear
				mFlingingAllowed = false;
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
}
