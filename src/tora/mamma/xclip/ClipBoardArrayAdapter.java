package tora.mamma.xclip;

import java.util.List;

import tora.mamma.xclip.clip.XClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ClipBoardArrayAdapter extends ArrayAdapter<XClipData> {

	private List<XClipData> xcdl;
	private float textwide;

	private static final int background = Color.argb(0xFF, 0xFA, 0xFA, 0xFA);
	private static final int backgroundDark = Color.argb(0xFF, 0xDA, 0xDA, 0xDA);
	private static final int textColor = Color.argb(0xFF, 0x42, 0x42, 0x42);

	public ClipBoardArrayAdapter(Context context, List<XClipData> objects) {

		super(context, -1, objects);

		xcdl = objects;

		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		disp.getMetrics(displayMetrics);
		textwide = displayMetrics.widthPixels * 0.8f;

	}

	@Override
	public XClipData getItem(int position) {

		return xcdl.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if (null == convertView) {
			Context context = getContext();

			LinearLayout layout = new LinearLayout(context);
			layout.setPadding(2, 2, 2, 2);
			layout.setBackgroundColor(background);
			convertView = layout;

			TextView textview = new TextView(context);
			textview.setTag("text");
			textview.setTextColor(textColor);
			textview.setPadding(28, 28, 28, 28);
			textview.setMaxLines(1);
			textview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			if (Build.VERSION.SDK_INT >= 21) {
				if(parent!=null){
					textview.setLayoutParams(parent.getLayoutParams());
				}
				textview.setBackground(getPressedColorRippleDrawable(background, backgroundDark));
			}
			layout.addView(textview);
		}

		XClipData clipdata = xcdl.get(position);
		TextView textview = (TextView) convertView.findViewWithTag("text");
		CharSequence ellipsizedText = TextUtils.ellipsize(clipdata.getDispText(), textview.getPaint(), textwide, TruncateAt.END);
		textview.setText(ellipsizedText);

		return convertView;
	}

	public static RippleDrawable getPressedColorRippleDrawable(int normalColor, int pressedColor) {

		return new RippleDrawable(getPressedColorSelector(normalColor, pressedColor), getColorDrawableFromColor(normalColor), null);
	}

	public static ColorStateList getPressedColorSelector(int normalColor, int pressedColor) {

		return new ColorStateList(new int[][] {
		        new int[] { android.R.attr.state_pressed },
		        new int[] { android.R.attr.state_focused },
		        new int[] { android.R.attr.state_activated },
		        new int[] {} }, new int[] { pressedColor, pressedColor, pressedColor, normalColor });
	}

	public static ColorDrawable getColorDrawableFromColor(int color) {

		return new ColorDrawable(color);
	}
}
