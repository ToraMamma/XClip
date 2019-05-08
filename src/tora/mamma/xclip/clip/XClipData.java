package tora.mamma.xclip.clip;

import android.os.Parcel;
import android.os.Parcelable;

public class XClipData implements Parcelable {

	private String mId;
	private String mText;
	private String mPkg;
	private String mTime;

	public XClipData() {

	}

	public boolean equals(Object paramObject) {
		boolean bool;
		if (!(paramObject instanceof XClipData)) {
			bool = false;
		} else {
			bool = getEscapedText().equals(((XClipData) paramObject).getEscapedText());
		}
		return bool;
	}

	XClipData setEscapedText(String paramString) {
		this.mText = paramString;
		return this;
	}

	String getEscapedText() {
		return this.mText;
	}

	public String getToastText() {
		String txt = convertToDisp(this.mText);
		String ret=txt;
		if(txt!=null && txt.length()>=13){
			ret=txt.substring(0, 13);
			ret = ret+"...";
		}
		return ret;
	}

	public String getDispText() {
		return convertToDisp(this.mText);
	}

	public XClipData setOriginalText(String paramString) {
		this.mText = convertToClip(paramString);
		return this;
	}

	public String getOriginalText() {
		return convertToText(this.mText);
	}

	private String convertToClip(String paramString) {
		if (paramString == null) {
			return "";
		}
		return paramString.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "<r>").replace(
		        "\n", "<n>");
	}

	private String convertToText(String paramString) {
		if (paramString == null) {
			return "";
		}

		return paramString.replace("<n>", "\n").replace("<r>", "\r").replace("&gt;", ">").replace("&lt;", "<").replace(
		        "&amp;", "&");
	}

	private String convertToDisp(String paramString) {
		if (paramString == null) {
			return "";
		}
		return paramString.replace("<n>", "").replace("<r>", "").replace("&gt;", ">").replace("&lt;", "<").replace(
		        "&amp;", "&");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Creator<XClipData> CREATOR = new Parcelable.Creator() {

		@Override
		public XClipData createFromParcel(Parcel in) {
			return new XClipData(in);
		}

		@Override
		public XClipData[] newArray(int size) {
			return new XClipData[size];
		}
	};

	public XClipData(Parcel parcel) {
		mText = parcel.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(mText);
	}

    public String getmPkg() {
    	return mPkg;
    }

    public void setmPkg(String mPkg) {
    	this.mPkg = mPkg;
    }

    public String getmTime() {
    	return mTime;
    }

    public void setmTime(String mTime) {
    	this.mTime = mTime;
    }

    public String getmId() {
    	return mId;
    }

    public void setmId(String mId) {
    	this.mId = mId;
    }
}
