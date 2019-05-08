package tora.mamma.xclip.clip;

import java.util.List;

import tora.mamma.xclip.bind.CDMServiceIF;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class CDMService extends Service {

	@Override
	public IBinder onBind(Intent intent) {

		if (CDMServiceIF.class.getName().equals(intent.getAction())) {
			return sIf;
		}
		return null;
	}

	private CDMServiceIF.Stub sIf = new CDMServiceIF.Stub() {

		@Override
		public List<XClipData> getClipList() throws RemoteException {

			XClipDataManager cdm = new XClipDataManager(CDMService.this, XClipConstants.CLIPHISTORY_TXT,XClipDataManager.DATA_TYPE_FILE);
			List<XClipData> clipList = cdm.getClipList();

			return clipList;
		}

		@Override
		public boolean registerClip(String str) throws RemoteException {

			XClipData mcd = new XClipData();
			mcd.setOriginalText(str);

			XClipDataManager cdm = new XClipDataManager(CDMService.this, XClipConstants.CLIPHISTORY_TXT,XClipDataManager.DATA_TYPE_FILE);
			return cdm.registerClip(mcd);
		}

		@Override
		public void removeClip(int pos) throws RemoteException {

			XClipDataManager cdm = new XClipDataManager(CDMService.this, XClipConstants.CLIPHISTORY_TXT,XClipDataManager.DATA_TYPE_FILE);
			cdm.removeClip(pos);
		}
	};
}