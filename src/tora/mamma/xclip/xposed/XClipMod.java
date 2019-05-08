package tora.mamma.xclip.xposed;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import tora.mamma.xclip.ClipBoardArrayAdapter;
import tora.mamma.xclip.bind.CDMServiceIF;
import tora.mamma.xclip.clip.XClipData;
import tora.mamma.xclip.clip.XClipDataManager;
import tora.mamma.xclip.widget.SystemOverlayLayout;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XClipMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {

	private XSharedPreferences pref;
	private Context xclictx;
	private static Context textctx;
	private TextView edittv;
	private static Dialog dialog;

	private static final String CLIP_LABEL = "xclip_text";
	private static final String IGNORE_LABEL = "ignore_xclip_text";
	private static long LASTEXEC = Integer.MIN_VALUE;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

		try {

			// showLog("initZygote:" + startupParam.modulePath);
			// showLog("OS version:" + Build.VERSION.SDK_INT);

			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				return;
			}

			pref = new XSharedPreferences("tora.mamma.xclip");

			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {

				initZygoteUtilLegacy();

			} else if (Build.VERSION.SDK_INT >= 21) {

				initZygoteUtil(startupParam);
			}

		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	private void initZygoteUtil(StartupParam startupParam) {

		// copy
		{
			XposedHelpers.findAndHookConstructor(android.content.ClipboardManager.class, Context.class, Handler.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {

					// showLog("Create ClipboardManager.");

					xclictx = (Context) param.args[0];
				}
			});
			findAndHookMethod(android.content.ClipboardManager.class, "setPrimaryClip", android.content.ClipData.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					// showLog("Call ClipboardManager#setPrimaryClip.");
					try {
						if (param.args != null && param.args.length == 1) {

							android.content.ClipData cd = (android.content.ClipData) param.args[0];
							android.content.ClipData.Item item = cd.getItemAt(0);
							if (item != null && item.getText() != null && !IGNORE_LABEL.equals(cd.getDescription().getLabel())) {
								String str = item.coerceToText(xclictx).toString();
								if (!str.equals("") && !str.equals(XClipDataManager.HISTORY_NONE_MESSAGE)) {
									// showLog("addDatabaseClipText()." + str);
									addDatabaseClipText(str);
								}
							}
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
			findAndHookMethod(android.content.ClipboardManager.class, "setText", CharSequence.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					// showLog("Call ClipboardManager#setText.");
					try {
						if (param.args != null && param.args.length == 1) {
							String str = ((CharSequence) param.args[0]).toString();
							if (!str.equals("") && !str.equals(XClipDataManager.HISTORY_NONE_MESSAGE)) {
								// showLog("addDatabaseClipText()." + str);
								addDatabaseClipText(str);
							}
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
		}// copy

	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		try {
			// showLog("handleLoadPackage:" + lpparam.packageName);

			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				return;
			}

			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {

				handleLoadPackageUtilLegacy(lpparam);

			} else if (Build.VERSION.SDK_INT >= 21) {

				handleLoadPackageUtil(lpparam);
			}
		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	private void handleLoadPackageUtil(LoadPackageParam lpparam) {

		final Class<?> textView = XposedHelpers.findClass("android.widget.TextView", lpparam.classLoader);
		final Class<?> popup = XposedHelpers.findClass("android.widget.Editor.ActionPopupWindow", lpparam.classLoader);

		XposedHelpers.findAndHookMethod(textView, "onFocusChanged", boolean.class, int.class, Rect.class, new XC_MethodHook() {

			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

				TextView tv = (TextView) param.thisObject;
				if (!(tv instanceof EditText)) {
					return;
				}
				if ((Boolean) param.args[0]) {
					edittv = tv;
					textctx = edittv.getContext();

					if (showHistory_isMenu()) {
						edittv.setCustomSelectionActionModeCallback(createActionmodeCallback(textctx, edittv));
					}
					if ("".equals(getPrimaryClip(textctx))) {
						setPrimaryClipFromHistory(textctx);
					}
				}
			}
		});

		// long press
		{
			findAndHookMethod(textView, "performLongClick", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					if (showHistory_isLongPress()) {
						try {
							TextView tv = (TextView) param.thisObject;
							if (!(tv instanceof EditText)) {
								return;
							}
							boolean canP = (Boolean) XposedHelpers.callMethod(edittv, "canPaste", new Object[] {});
							int selStart = edittv.getSelectionStart();
							int selEnd = edittv.getSelectionEnd();

							final int min = Math.max(0, Math.min(selStart, selEnd));
							final int max = Math.max(0, Math.max(selStart, selEnd));

							if (min == max && canP) {

								showHistoryDialog();
							}
						} catch (Throwable th) {
							printThrowable(th);
						}
					}
				}
			});
		}// long press

		// paste button
		{
			findAndHookMethod(popup, "onClick", View.class, new XC_MethodHook() {

				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

					if (showHistory_isButton()) {
						setPrimaryClip(textctx, "", false);
					}
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					if (showHistory_isButton()) {
						try {
							showHistoryDialog();
						} catch (Throwable th) {
							printThrowable(th);
						}
					}
				}
			});
		}// paste button

	}

	private boolean showHistory_isButton() {
		pref.reload();
		return pref.getBoolean("btnpst", false);
	}

	private boolean showHistory_isMenu() {
		pref.reload();
		return pref.getBoolean("menupst", false);
	}

	private boolean showHistory_isLongPress() {
		pref.reload();
		return pref.getBoolean("ext", false);
	}

	private boolean showHistory_isWebView() {
		pref.reload();
		return pref.getBoolean("webvpst", false);
	}

	private boolean isOverlayList() {
		pref.reload();
		return pref.getBoolean("overlay", false);
	}

	private String getPrimaryClip(Context con) {
		try {
			ClipboardManager cm = (ClipboardManager) con.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData cd = cm.getPrimaryClip();
			if (cd != null && cd.getItemAt(0) != null && cd.getItemAt(0).getText() != null) {
				return cd.getItemAt(0).getText().toString();
			}
		} catch (Throwable th) {
			printThrowable(th);
		}
		return "";

	}

	private void setPrimaryClip(Context con, String str, boolean regist) {

		try {
			String label = IGNORE_LABEL;
			if (regist) {
				label = CLIP_LABEL;
			}
			ClipboardManager cm = (ClipboardManager) con.getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setPrimaryClip(ClipData.newPlainText(label, str));
		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	// ------------------------------------------------

	private void addDatabaseClipText(String cliplistText) {

		XClipDataManager datamng = new XClipDataManager(xclictx, null, XClipDataManager.DATA_TYPE_DB);
		XClipData data = new XClipData();
		data.setOriginalText(cliplistText);
		datamng.registerClip(data);
	}

	private XClipData removeDatabaseClipText(int position) {

		XClipDataManager datamng = new XClipDataManager(xclictx, null, XClipDataManager.DATA_TYPE_DB);
		return datamng.removeClip(position);
	}

	private List<XClipData> getDatabaseClipList() {

		XClipDataManager datamng = new XClipDataManager(xclictx, null, XClipDataManager.DATA_TYPE_DB);
		return datamng.getClipList();
	}

	private void setPrimaryClipFromHistory(final Context con) {

		List<XClipData> list = getDatabaseClipList();
		if (list != null && list.size() >= 1) {
			String str = list.get(0).getOriginalText();
			setPrimaryClip(con, str, false);
		}
	}

	private ActionMode.Callback createActionmodeCallback(final Context con, final TextView tv) {

		return new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {

				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

				int id = item.getItemId();
				switch (id) {
					case android.R.id.paste:

						showHistoryDialog();
						return true;
				}
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {

			}
		};
	}

	private void showHistoryDialog() {

		try {
			if (System.currentTimeMillis() - LASTEXEC <= 100) {
				return;
			}
			LASTEXEC = System.currentTimeMillis();
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
				dialog = null;
			}

			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {

					return null;
				}

				@Override
				protected void onPostExecute(Void result) {

					try {
						List<XClipData> clipList = getDatabaseClipList();
						int clipsize = clipList.size();
						if (clipsize == 0) {
							XClipData data = new XClipData();
							data.setOriginalText(XClipDataManager.HISTORY_NONE_MESSAGE);
							clipList.add(data);
						}

						final int min;
						final int max;
						if (edittv == null) {
							min = 0;
							max = 0;

						} else {
							int selStart = edittv.getSelectionStart();
							int selEnd = edittv.getSelectionEnd();

							min = Math.max(0, Math.min(selStart, selEnd));
							max = Math.max(0, Math.max(selStart, selEnd));
						}

						Context wrappedContext = new ContextThemeWrapper(textctx, android.R.style.Theme_Material_Light);

						ListView lv = createListView(wrappedContext, edittv, clipList, min, max);
						dialog = new AlertDialog.Builder(wrappedContext).setView(lv).create();
						dialog.show();

						WindowManager wm = (WindowManager) textctx.getSystemService(Context.WINDOW_SERVICE);
						Display disp = wm.getDefaultDisplay();
						DisplayMetrics displayMetrics = new DisplayMetrics();
						disp.getMetrics(displayMetrics);
						int w = (int) (displayMetrics.widthPixels * 0.95f);
						int h = (int) (displayMetrics.heightPixels * 0.95f);

						WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
						lp.copyFrom(dialog.getWindow().getAttributes());
						lp.width = w;
						if (clipsize >= 12) {
							lp.height = h;
						} else {
							lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
						}
						dialog.getWindow().setAttributes(lp);

					} catch (Throwable th) {
						printThrowable(th);
					}
				};
			}.execute();

		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	private ListView createListView(final Context con, final TextView tv, final List<XClipData> clipList, final int min, final int max) {

		Context wrappedContext = new ContextThemeWrapper(con, android.R.style.Theme_Material_Light);
		ListView lv = new ListView(wrappedContext);
		lv.setDivider(new InsetDrawable(new ColorDrawable(Color.argb(0x66, 0x95, 0xa5, 0xa6)), 14, 0, 14, 0));
		lv.setDividerHeight(1);
		lv.setAdapter(new ClipBoardArrayAdapter(con, clipList));
		lv.setScrollingCacheEnabled(false);
		lv.setOnItemClickListener(createItemClickListner(con, clipList, tv, max, min));
		lv.setOnItemLongClickListener(createItemLongClickListner(con));
		return lv;
	}

	private OnItemClickListener createItemClickListner(final Context con, final List<XClipData> clipList, final TextView tv, final int max, final int min) {

		return new OnItemClickListener() {

			public void onItemClick(AdapterView<?> items, View view, int position, long id) {

				try {
					String pasteStr = clipList.get(position).getOriginalText();

					setPrimaryClip(con, pasteStr, true);

					if (tv != null) {
						if (max == min) {
							tv.getEditableText().insert(max, pasteStr);
						} else {
							tv.getEditableText().replace(min, max, pasteStr);
						}
					}
				} catch (Throwable th) {
					printThrowable(th);
				} finally {
					dialogClose();
				}
			}
		};
	}

	private OnItemLongClickListener createItemLongClickListner(final Context con) {

		return new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

				try {

					XClipData removedData = removeDatabaseClipText(position);
					showToast(con, removedData.getToastText() + " removed.", true);

				} catch (Throwable th) {
					printThrowable(th);
				} finally {
					dialogClose();
				}

				return false;
			}
		};
	}

	private void dialogClose() {

		try {
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}
		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	// ------- LEGACY SOURCE ---------------------------------------------------------------------------------------

	private static SystemOverlayLayout overlay = null;

	private CDMServiceIF clipDataManagerService;

	private static final Class<?>[] SERVICE_CLASSES = new Class<?>[] { CDMServiceIF.class, };
	private CountDownLatch bindCountDown = new CountDownLatch(SERVICE_CLASSES.length);

	private ServiceConnection sCon = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			clipDataManagerService = CDMServiceIF.Stub.asInterface(service);
			bindCountDown.countDown();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

			clipDataManagerService = null;
		}
	};

	private void bindClipboardService(Context con) {

		// showLog("bindClipboardService:" + bindCountDown.getCount());

		try {
			Intent intent = new Intent(CDMServiceIF.class.getName());
			con.bindService(intent, sCon, Service.BIND_AUTO_CREATE);
		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	private void unbindClipboardService(Context con) {

		bindCountDown = new CountDownLatch(SERVICE_CLASSES.length);
		// showLog("unbindClipboardService:" + bindCountDown.getCount());

		try {
			con.unbindService(sCon);
		} catch (Throwable th) {
			printThrowable(th);
		}
	}


	private void initZygoteUtilLegacy() {

		{
			findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						Activity activity = (Activity) param.thisObject;
						Context con = activity.getApplicationContext();

						bindClipboardService(con);

						if ("".equals(getPrimaryClip(con))) {
							setPrimaryClipFromHistoryLegacy(con);
						}
						if (overlay != null) {
							overlay.removeWindow();
							overlay = null;
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});

			findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						if (dialog != null && dialog.isShowing()) {
							dialog.dismiss();
						}
						if (overlay != null) {
							overlay.removeWindow();
							overlay = null;
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});

			findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						// Unregister the rotate observer
						Activity activity = (Activity) param.thisObject;
						Context con = activity.getApplicationContext();

						unbindClipboardService(con);

						if (overlay != null) {
							overlay.removeWindow();
							overlay = null;
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
		}

		// copy
		{
			findAndHookMethod(android.content.ClipboardManager.class, "setPrimaryClip", android.content.ClipData.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						final Context con = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

						if (param.args != null && param.args.length == 1) {

							android.content.ClipData cd = (android.content.ClipData) param.args[0];
							android.content.ClipData.Item item = cd.getItemAt(0);
							if (item != null && item.getText() != null) {
								addHistoryLegacy(con, item.getText().toString());
							}
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
			findAndHookMethod(android.content.ClipboardManager.class, "setText", CharSequence.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						final Context con = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

						if (param.args != null && param.args.length == 1) {
							addHistoryLegacy(con, ((CharSequence) param.args[0]).toString());
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
		}// copy
	}

	private void handleLoadPackageUtilLegacy(LoadPackageParam lpparam) {

		// paste button
		{

			final Class<?> popup = XposedHelpers.findClass("android.widget.Editor.ActionPopupWindow", null);

			hookAllConstructors(popup, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						if (showHistory_isButton()) {
							final Object outerObject = XposedHelpers.getSurroundingThis(param.thisObject);
							final TextView tv = (TextView) XposedHelpers.getObjectField(outerObject, "mTextView");

							final Context con = (Context) tv.getContext();

							XposedHelpers.setAdditionalInstanceField(param.thisObject, "mContext", con);
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});

			findAndHookMethod(popup, "onClick", View.class, new XC_MethodHook() {

				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

					try {
						if (showHistory_isButton()) {
							final Context con = (Context) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mContext");

							setPrimaryClip(con, "", false);
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						if (showHistory_isButton()) {
							final Object outerObject = XposedHelpers.getSurroundingThis(param.thisObject);
							final TextView tv = (TextView) XposedHelpers.getObjectField(outerObject, "mTextView");

							final Context con = (Context) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mContext");

							// Toast.makeText(mContext, "click", Toast.LENGTH_SHORT).show();

							showHistoryDialogLegacy(con, tv);
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
		}// paste button

		// long press & paste menu
		{
			final Class<?> textView = XposedHelpers.findClass("android.widget.TextView", null);

			hookAllConstructors(textView, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						final Context con = (Context) param.args[0];
						XposedHelpers.setAdditionalInstanceField(param.thisObject, "mContext", con);

						if (showHistory_isMenu()) {
							// menu update
							final TextView tv = ((TextView) (param.thisObject));
							tv.setCustomSelectionActionModeCallback(createACLegacy(con, tv));
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});

			findAndHookMethod(textView, "performLongClick", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					try {
						if (showHistory_isLongPress()) {
							final TextView tv = ((TextView) (param.thisObject));
							final Context con = (Context) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mContext");

							boolean canP = (Boolean) XposedHelpers.callMethod(tv, "canPaste", new Object[] {});
							int selStart = tv.getSelectionStart();
							int selEnd = tv.getSelectionEnd();

							final int min = Math.max(0, Math.min(selStart, selEnd));
							final int max = Math.max(0, Math.max(selStart, selEnd));

							if (min == max && canP) {

								showHistoryDialogLegacy(con, tv);
							}
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			});
		}// long press & paste menu

		// WebView
		{

			final Class<?> webView = XposedHelpers.findClass("android.webkit.WebView", null);

			hookAllConstructors(webView, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					if (showHistory_isWebView()) {

						final Context con = (Context) param.args[0];
						XposedHelpers.setAdditionalInstanceField(param.thisObject, "mContext", con);
					}
				}
			});

			findAndHookMethod(webView, "performLongClick", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					if (showHistory_isWebView()) {

						final Context con = (Context) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mContext");
						showToast(con, "performLongClick");

						final InputConnection in = (InputConnection) XposedHelpers.getAdditionalInstanceField(param.thisObject, "xinput");
						if (in != null) {
							showHistoryDialogWebView(con, in);
							XposedHelpers.setAdditionalInstanceField(param.thisObject, "xinput", null);
						}
					}
				}
			});

			findAndHookMethod(webView, "onCreateInputConnection", EditorInfo.class, new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {

					if (showHistory_isWebView()) {

						final Context con = (Context) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mContext");
						showToast(con, "onCreateInputConnection");

						InputConnection in = (InputConnection) param.getResult();
						XposedHelpers.setAdditionalInstanceField(param.thisObject, "xinput", in);
					}
				}
			});

		}// WebView
	}

	private void setPrimaryClipFromHistoryLegacy(final Context con) {

		// Toast.makeText(mContext, "showHistoryDialog", Toast.LENGTH_SHORT).show();

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				try {
					// showLog("setPrimaryClipFromHistory#doInBackground:" + bindCountDown.getCount());
					bindCountDown.await(5000, TimeUnit.MICROSECONDS);
				} catch (Throwable e) {}
				return null;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void onPostExecute(Void result) {

				try {
					// showLog("setPrimaryClipFromHistory#onPostExecute:" + bindCountDown.getCount());
					if (clipDataManagerService != null) {
						List<XClipData> clipList = new ArrayList<XClipData>();
						clipList = clipDataManagerService.getClipList();

						if (clipList != null && clipList.size() != 0) {
							XClipData clip = clipList.get(0);

							String str = clip.getOriginalText();
							if (str != null) {
								setPrimaryClip(con, str, false);
							}
						}
					}
				} catch (Throwable th) {
					printThrowable(th);
				}
			};
		}.execute();

	}

	private void addHistoryLegacy(final Context con, final String str) {

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				try {
					// showLog("addHistory#doInBackground:" + bindCountDown.getCount());
					bindCountDown.await(5000, TimeUnit.MICROSECONDS);
				} catch (Throwable e) {}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {

				try {
					// showLog("addHistory#onPostExecute:" + bindCountDown.getCount());
					if (clipDataManagerService != null) {
						clipDataManagerService.registerClip(str);
					}
				} catch (Throwable th) {
					printThrowable(th);
				}
			}
		}.execute();
	}

	private void removeClipLegacy(final Context con, final int pos) {

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				try {
					// showLog("addHistory#doInBackground:" + bindCountDown.getCount());
					bindCountDown.await(5000, TimeUnit.MICROSECONDS);
				} catch (Throwable e) {}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {

				try {
					// showLog("addHistory#onPostExecute:" + bindCountDown.getCount());
					if (clipDataManagerService != null) {
						clipDataManagerService.removeClip(pos);
					}
				} catch (Throwable th) {
					printThrowable(th);
				}
			}
		}.execute();
	}

	private void showHistoryDialogLegacy(final Context con, final TextView tv) {

		// Toast.makeText(mContext, "showHistoryDialog", Toast.LENGTH_SHORT).show();

		try {
			if (System.currentTimeMillis() - LASTEXEC <= 100) {
				return;
			}

			LASTEXEC = System.currentTimeMillis();
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
				dialog = null;
			}
			if (overlay != null) {
				overlay.removeWindow();
				overlay = null;
			}

			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {

					try {
						// showLog("showHistoryDialog#doInBackground:" + bindCountDown.getCount());
						bindCountDown.await(5000, TimeUnit.MICROSECONDS);
					} catch (Throwable e) {}
					return null;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void onPostExecute(Void result) {

					try {
						// showLog("showHistoryDialog#onPostExecute:" + bindCountDown.getCount());
						if (clipDataManagerService != null) {

							final List<XClipData> clipList = clipDataManagerService.getClipList();

							final int min;
							final int max;
							if (tv == null) {
								min = 0;
								max = 0;

							} else {
								int selStart = tv.getSelectionStart();
								int selEnd = tv.getSelectionEnd();

								min = Math.max(0, Math.min(selStart, selEnd));
								max = Math.max(0, Math.max(selStart, selEnd));
							}

							Context wrappedContext = new ContextThemeWrapper(con, android.R.style.Theme_Holo_Light);

							dialog = new AlertDialog.Builder(wrappedContext).setView(createListViewLegacy(wrappedContext, tv, clipList, min, max)).create();

							try {
								dialog.show();

							} catch (BadTokenException bex) {
								if (isOverlayList()) {
									WindowManager wm = (WindowManager) con.getSystemService(Context.WINDOW_SERVICE);
									Display disp = wm.getDefaultDisplay();
									DisplayMetrics displayMetrics = new DisplayMetrics();
									disp.getMetrics(displayMetrics);

									ListView lv = createListViewLegacy(wrappedContext, tv, clipList, min, max);
									lv.setLayoutParams(new ViewGroup.LayoutParams(displayMetrics.widthPixels / 10 * 9, displayMetrics.heightPixels / 10 * 9));

									LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

									overlay = new SystemOverlayLayout(con);
									overlay.addWindow(lp, Gravity.CENTER);
									overlay.addView(lv);
								}
							}
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				};
			}.execute();

		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	private ListView createListViewLegacy(final Context con, final TextView tv, final List<XClipData> clipList, final int min, final int max) {

		Context wrappedContext = new ContextThemeWrapper(con, android.R.style.Theme_Holo_Light);

		ListView lv = new ListView(wrappedContext);
		lv.setDivider(new ColorDrawable(Color.argb(0x66, 0x95, 0xa5, 0xa6)));
		lv.setDividerHeight(1);
		lv.setAdapter(new ClipBoardArrayAdapter(con, clipList));
		lv.setScrollingCacheEnabled(false);
		lv.setOnItemClickListener(createItemClickListnerLegacy(con, clipList, tv, max, min));
		lv.setOnItemLongClickListener(createItemLongClickListnerLegacy(con));
		return lv;
	}

	private ActionMode.Callback createACLegacy(final Context con, final TextView tv) {

		return new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {

				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

				try {
					int id = item.getItemId();
					switch (id) {
						case android.R.id.paste:

							showHistoryDialogLegacy(con, tv);
							return true;
					}
				} catch (Throwable th) {
					printThrowable(th);
				}
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {

			}
		};
	}

	private OnItemClickListener createItemClickListnerLegacy(final Context con, final List<XClipData> clipList, final TextView tv, final int max, final int min) {

		return new OnItemClickListener() {

			public void onItemClick(AdapterView<?> items, View view, int position, long id) {

				try {
					String pasteStr = clipList.get(position).getOriginalText();

					setPrimaryClip(con, pasteStr, true);

					if (tv != null) {
						if (max == min) {
							tv.getEditableText().insert(max, pasteStr);
						} else {
							tv.getEditableText().replace(min, max, pasteStr);
						}
					}
				} catch (Throwable th) {
					printThrowable(th);
				} finally {
					try {
						if (overlay != null) {
							overlay.removeWindow();
						}
						if (dialog != null && dialog.isShowing()) {
							dialog.dismiss();
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			}
		};
	}

	private OnItemLongClickListener createItemLongClickListnerLegacy(final Context con) {

		return new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

				try {

					removeClipLegacy(con, position);

				} catch (Throwable th) {
					printThrowable(th);
				} finally {
					try {
						if (overlay != null) {
							overlay.removeWindow();
						}
						if (dialog != null && dialog.isShowing()) {
							dialog.dismiss();
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}

				return false;
			}
		};
	}

	private void showHistoryDialogWebView(final Context con, final InputConnection in) {

		// Toast.makeText(mContext, "showHistoryDialog", Toast.LENGTH_SHORT).show();

		try {
			if (System.currentTimeMillis() - LASTEXEC <= 100) {
				return;
			}

			LASTEXEC = System.currentTimeMillis();
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
				dialog = null;
			}
			if (overlay != null) {
				overlay.removeWindow();
				overlay = null;
			}

			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {

					try {
						bindCountDown.await(5000, TimeUnit.MICROSECONDS);
					} catch (Throwable e) {}
					return null;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void onPostExecute(Void result) {

					try {
						if (clipDataManagerService != null) {

							final List<XClipData> clipList = clipDataManagerService.getClipList();

							final int min = 0;
							final int max = 0;

							Context wrappedContext = new ContextThemeWrapper(con, android.R.style.Theme_Holo_Light);

							dialog = new AlertDialog.Builder(wrappedContext).setView(createListViewWebView(wrappedContext, in, clipList, min, max)).create();

							try {
								dialog.show();

							} catch (BadTokenException bex) {
								if (isOverlayList()) {
									WindowManager wm = (WindowManager) con.getSystemService(Context.WINDOW_SERVICE);
									Display disp = wm.getDefaultDisplay();
									DisplayMetrics displayMetrics = new DisplayMetrics();
									disp.getMetrics(displayMetrics);

									ListView lv = createListViewWebView(wrappedContext, in, clipList, min, max);
									lv.setLayoutParams(new ViewGroup.LayoutParams(displayMetrics.widthPixels / 10 * 9, displayMetrics.heightPixels / 10 * 9));

									LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

									overlay = new SystemOverlayLayout(con);
									overlay.addWindow(lp, Gravity.CENTER);
									overlay.addView(lv);
								}
							}
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				};
			}.execute();

		} catch (Throwable th) {
			printThrowable(th);
		}
	}

	private ListView createListViewWebView(final Context con, final InputConnection in, final List<XClipData> clipList, final int min, final int max) {

		Context wrappedContext = new ContextThemeWrapper(con, android.R.style.Theme_Holo_Light);

		ListView lv = new ListView(wrappedContext);
		lv.setDivider(new ColorDrawable(Color.argb(0x66, 0x95, 0xa5, 0xa6)));
		lv.setDividerHeight(1);
		lv.setAdapter(new ClipBoardArrayAdapter(con, clipList));
		lv.setScrollingCacheEnabled(false);
		lv.setOnItemClickListener(createICLWebView(con, clipList, in, max, min));
		return lv;
	}

	private OnItemClickListener createICLWebView(final Context con, final List<XClipData> clipList, final InputConnection in, final int max, final int min) {

		return new OnItemClickListener() {

			public void onItemClick(AdapterView<?> items, View view, int position, long id) {

				try {
					String pasteStr = clipList.get(position).getOriginalText();

					setPrimaryClip(con, pasteStr, true);

					if (in != null) {
						in.commitText(pasteStr, 0);
					}
				} catch (Throwable th) {
					printThrowable(th);
				} finally {
					try {
						if (overlay != null) {
							overlay.removeWindow();
						}
						if (dialog != null && dialog.isShowing()) {
							dialog.dismiss();
						}
					} catch (Throwable th) {
						printThrowable(th);
					}
				}
			}
		};
	}

	// -----------------------------------------

	@SuppressWarnings("unused")
	private void showLog(String str) {

		showLog(str, false);
	}

	private void showLog(String str, boolean is) {

		if (is) {
			// adb logcat -v time | grep "XClip"
			XposedBridge.log(" ** XClip " + str);
		}
	}

	private void showToast(Context con, String str) {

		showToast(con, str, false);
	}

	private void showToast(Context con, String str, boolean is) {

		if (is) {
			Toast.makeText(con, str, Toast.LENGTH_SHORT).show();
		}
	}

	private void printThrowable(Throwable th) {

		try {
			StringBuilder sb = new StringBuilder(th.getClass().getName());
			sb.append(" ");
			for (StackTraceElement st : th.getStackTrace()) {
				if (st.getClassName().startsWith("tora.mamma.xclip") && st.getLineNumber() >= 10) {
					sb.append(st.getClassName());
					sb.append("#");
					sb.append(st.getMethodName());
					sb.append("(");
					sb.append(st.getLineNumber());
					sb.append(")");
					// sb.append("\r\n");
				}
			}
			showLog("Exception: " + sb.toString(), true);
		} catch (Throwable t) {
			showLog("Exception: " + th.getClass().getName(), true);
		}
	}

	// -----------------------------------------
}