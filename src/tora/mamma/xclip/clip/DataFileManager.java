package tora.mamma.xclip.clip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;

public class DataFileManager {

	private String mFileName;
	private Context mCon;
	private final static String DATA_PATH = "/Android/data/tora.mamma.xclip";

	public DataFileManager(Context con, String paramString) {

		this.mFileName = paramString;
		this.mCon = con;
	}

	public List<XClipData> getClipList() {

		List<XClipData> result = new ArrayList<XClipData>();
		FileInputStream fileInputStream = null;
		BufferedReader reader = null;
		try {

			StringBuilder pathSd = new StringBuilder();
			pathSd.append(Environment.getExternalStorageDirectory().getPath());
			pathSd.append(DATA_PATH);
			File filePathToSaved = new File(pathSd.toString());
			if (filePathToSaved.exists()) {
				migrationData();
			}

			String lineBuffer;
			fileInputStream = mCon.openFileInput(mFileName);
			reader = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
			while ((lineBuffer = reader.readLine()) != null) {
				XClipData clip = new XClipData();
				clip.setEscapedText(lineBuffer);
				result.add(clip);
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {}
			}
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {}
			}
		}
		return result;
	}

	public void setClipList(List<XClipData> paramList) {

		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = mCon.openFileOutput(mFileName, Context.MODE_PRIVATE);
			StringBuilder writeString = new StringBuilder();
			for (XClipData clip : paramList) {
				writeString.append(clip.getEscapedText() + "\r\n");
			}
			fileOutputStream.write(writeString.toString().getBytes());
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {}
			}
		}
	}

	public void migrationData() {

		StringBuilder pathSd = new StringBuilder();
		pathSd.append(Environment.getExternalStorageDirectory().getPath());
		pathSd.append(DATA_PATH);

		File oldDir = new File(pathSd.toString());
		pathSd.append("/");
		pathSd.append(mFileName);
		File oldDat = new File(pathSd.toString());

		if (oldDat.exists()) {
			FileInputStream fileInputStream = null;
			FileOutputStream fileOutputStream = null;
			BufferedReader reader = null;
			try {
				String lineBuffer;
				fileInputStream = new FileInputStream(oldDat);
				reader = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));

				fileOutputStream = mCon.openFileOutput(mFileName, Context.MODE_PRIVATE);

				while ((lineBuffer = reader.readLine()) != null) {
					fileOutputStream.write(lineBuffer.getBytes());
					fileOutputStream.write("\r\n".getBytes());
				}
			} catch (Throwable th) {
				th.printStackTrace();
			} finally {
				if (fileOutputStream != null) {
					try {
						fileOutputStream.close();
					} catch (IOException e) {}
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {}
				}
				if (fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException e) {}
				}
			}
			try {
				delete(oldDir);
			} catch (Throwable th) {}
		}
	}

	static private void delete(File f) {

		if (f.exists() == false) {
			return;
		}

		if (f.isFile()) {
			f.delete();
		}

		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				delete(files[i]);
			}
			f.delete();
		}
	}
}