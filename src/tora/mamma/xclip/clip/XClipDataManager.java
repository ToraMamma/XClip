package tora.mamma.xclip.clip;

import java.util.Iterator;
import java.util.List;

import android.content.Context;

public class XClipDataManager {

	private DataFileManager mFileManager;
	private DataBaseManager mDBManager;
	private int dataFlg = -1;

	public static final int DATA_TYPE_FILE = 0;
	public static final int DATA_TYPE_DB = 1;

	public static final String HISTORY_NONE_MESSAGE = "History None...";

	public static final int clipsize = 20;

	public XClipDataManager(Context con, String paramString, int dataflg) {

		this.dataFlg = dataflg;

		if (isDataBase()) {
			this.mDBManager = new DataBaseManager(con);
		} else {
			this.mFileManager = new DataFileManager(con, paramString);
		}

	}

	private boolean isDataBase() {

		return dataFlg == DATA_TYPE_DB;
	}

	public List<XClipData> getClipList() {

		return (isDataBase()) ? this.mDBManager.getClipList() : this.mFileManager.getClipList();
	}

	private void setClipList(List<XClipData> list) {

		if (isDataBase()) {
			this.mDBManager.setClipList(list);
		} else {
			this.mFileManager.setClipList(list);
		}

	}

	public void fixLength(List<XClipData> paramList) {

		while (true) {
			if (paramList.size() <= clipsize) {
				return;
			}
			paramList.remove(paramList.size() - 1);
		}
	}

	public boolean registerClip(XClipData addClip) {

		boolean ret = false;
		if ((addClip != null) && (!"".equals(addClip.getEscapedText()))) {
			List<XClipData> history = getClipList();
			if (containsInList(history, addClip)) {
				if (!history.get(0).equals(addClip)) {
					history.remove(addClip);
					history.add(0, addClip);
					fixLength(history);
					setClipList(history);
					ret = true;
				}
			} else {
				history.add(0, addClip);
				fixLength(history);
				setClipList(history);
			}
		}
		return ret;
	}

	public XClipData removeClip(int removePos) {

		List<XClipData> history = getClipList();
		XClipData removedClip = history.remove(removePos);
		setClipList(history);
		return removedClip;
	}

	private static boolean containsInList(List<XClipData> paramList, XClipData paramClip) {

		Iterator<XClipData> localIterator = paramList.iterator();
		boolean i = false;
		while (localIterator.hasNext()) {
			if (!paramClip.equals((XClipData) localIterator.next())) {
				continue;
			}
			i = true;
			break;
		}
		return i;
	}
}
