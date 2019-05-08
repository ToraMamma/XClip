package tora.mamma.xclip.clip;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class DataBaseManager {

	private Context mCon;

	public DataBaseManager(Context con) {

		this.mCon = con;

	}

	public List<XClipData> getClipList() {

		List<XClipData> result = new ArrayList<XClipData>();

		Cursor cursor = mCon.getContentResolver().query(ClipProvider.Contract.CLIPHISTORY.contentUri, null, null, null, null);
		if (cursor != null) {
			boolean isEof = cursor.moveToFirst();
			while (isEof) {
				XClipData data = new XClipData();
				data.setEscapedText(cursor.getString(cursor.getColumnIndex(ClipProvider.Contract.CLIPHISTORY.columns.get(1))));
				result.add(data);
				isEof = cursor.moveToNext();
			}
			cursor.close();
		}

		return result;
	}

	public void setClipList(List<XClipData> paramList) {

		mCon.getContentResolver().delete(ClipProvider.Contract.CLIPHISTORY.contentUri, null, null);

		for (XClipData param : paramList) {
			ContentValues cv = new ContentValues();
			cv.put(ClipProvider.Contract.CLIPHISTORY.columns.get(1), param.getEscapedText());
			cv.put(ClipProvider.Contract.CLIPHISTORY.columns.get(2), param.getmPkg());
			cv.put(ClipProvider.Contract.CLIPHISTORY.columns.get(3), param.getmTime());

			mCon.getContentResolver().insert(ClipProvider.Contract.CLIPHISTORY.contentUri, cv);
		}

	}
}
