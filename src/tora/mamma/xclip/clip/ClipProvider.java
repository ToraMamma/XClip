package tora.mamma.xclip.clip;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

@SuppressLint("DefaultLocale")
public class ClipProvider extends ContentProvider {

	// Authority
	public static final String AUTHORITY = "tora.mamma.xclip";

	public enum Contract {

		CLIPHISTORY(BaseColumns._ID, "cliptext", "package", "created"),

		TEST(BaseColumns._ID, "title2", "note2");

		Contract(final String... columns) {
			this.columns = Collections.unmodifiableList(Arrays.asList(columns));
		}
		private final String tableName = name().toLowerCase();
		private final int allCode = ordinal() * 10;
		private final int byIdCode = ordinal() * 10 + 1;
		public final Uri contentUri = Uri.parse("content://" + ClipProvider.AUTHORITY + "/" + tableName);
		public final String mimeTypeForOne = "vnd.android.cursor.item/vnd.xclip." + tableName;
		public final String mimeTypeForMany = "vnd.android.cursor.dir/vnd.xclip." + tableName;
		public final List<String> columns;
	}

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sUriMatcher.addURI(AUTHORITY, Contract.CLIPHISTORY.tableName, Contract.CLIPHISTORY.allCode);
		sUriMatcher.addURI(AUTHORITY, Contract.CLIPHISTORY.tableName + "/#", Contract.CLIPHISTORY.byIdCode);
		sUriMatcher.addURI(AUTHORITY, Contract.TEST.tableName, Contract.TEST.allCode);
		sUriMatcher.addURI(AUTHORITY, Contract.TEST.tableName + "/#", Contract.TEST.byIdCode);
	}

	private ClipDBHelper mDBHelper;

	@Override
	public boolean onCreate() {

		mDBHelper = new ClipDBHelper(getContext());
		return true;
	}

	private void checkUri(Uri uri) {

		final int code = sUriMatcher.match(uri);
		for (final Contract contract : Contract.values()) {
			if (code == contract.allCode) {
				return;
			} else if (code == contract.byIdCode) {
				return;
			}
		}
		throw new IllegalArgumentException("unknown uri : " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		checkUri(uri);

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(Contract.CLIPHISTORY.tableName);

		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		checkUri(uri);

		String insertTable = Contract.CLIPHISTORY.tableName;
		Uri contentUri = Contract.CLIPHISTORY.contentUri;

		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		long rowId = db.insert(insertTable, null, values);
		if (rowId > 0) {
			Uri returnUri = ContentUris.withAppendedId(contentUri, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;
		} else {
			throw new IllegalArgumentException("Failed to insert row into " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		checkUri(uri);

		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = db.update(Contract.CLIPHISTORY.tableName, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		checkUri(uri);

		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = db.delete(Contract.CLIPHISTORY.tableName, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {

		final int code = sUriMatcher.match(uri);
		for (final Contract contract : Contract.values()) {
			if (code == contract.allCode) {
				return contract.mimeTypeForMany;
			} else if (code == contract.byIdCode) {
				return contract.mimeTypeForOne;
			}
		}
		throw new IllegalArgumentException("unknown uri : " + uri);
	}
}
