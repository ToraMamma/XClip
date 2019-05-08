package tora.mamma.xclip.clip;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ClipDBHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "xclipdb";
	private static final int DB_VERSION = 1;

	public static final String CREATE_TABLE = "create table cliphistory(_id,cliptext,package,created);";

	public ClipDBHelper(Context context) {

		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {

		sqLiteDatabase.execSQL(CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

	}
}