package us.shandian.flashbak.ui.widget;

import android.widget.RelativeLayout;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Rect;

public class FixedRelativeLayout extends RelativeLayout
{
	
	public FixedRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public FixedRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public void invalidate(Rect r) {
		super.invalidate();
	}
	
	@Override
	public void invalidate(int p1, int p2, int p3, int p4) {
		super.invalidate();
	}
}
