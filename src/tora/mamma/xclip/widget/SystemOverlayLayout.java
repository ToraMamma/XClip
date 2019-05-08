package tora.mamma.xclip.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class SystemOverlayLayout extends LinearLayout {

	private Context mContext;

	public SystemOverlayLayout(Context context) {

		super(context);
		mContext = context;
	}

	public SystemOverlayLayout(Context context, AttributeSet attrs) {

		super(context, attrs);
		mContext = context;
	}

	public void addWindow(LayoutParams wh, int gravity) {

		try {

			WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

			WindowManager.LayoutParams params = new WindowManager.LayoutParams();
			params.width = WindowManager.LayoutParams.WRAP_CONTENT;
			params.height = WindowManager.LayoutParams.WRAP_CONTENT;
			params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
			params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
			        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
			params.format = PixelFormat.TRANSLUCENT;
			params.gravity = gravity;

			setLayoutParams(wh);
			setBackgroundColor(Color.argb(0x00, 0x00, 0x00, 0x00));
			wm.addView(getRootView(), params);
			wm = null;

		} catch (Throwable th) {
			th.printStackTrace();
		}
	}

	public void removeWindow() {

		try {
			WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(getRootView());
			wm = null;

		} catch (Throwable th) {
			// ignore
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		return super.dispatchTouchEvent(ev);
	}
}