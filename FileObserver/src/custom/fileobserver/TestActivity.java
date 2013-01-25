package custom.fileobserver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class TestActivity extends Activity {

	FileWatcher mWatcher ;
	final String TAG = TestActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
		mWatcher = new FileWatcher(sdcard, FileWatcher.ANY_CHANGED);
		mWatcher.startWatching();
	}

	@Override
	protected void onResume() {

		super.onResume();
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onUserLeaveHint() {

		super.onUserLeaveHint();
	}

	@Override
	protected void onStop() {
		
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		mWatcher.stopWatching();
		super.onDestroy();
	}

	
	class FileWatcher extends FileObserver{
		public FileWatcher(String path, int mask) {
			super(path, mask);
		}

		@Override
		public void onEvent(int event, String path) {
			switch (event) {
			case FileObserver.ACCESS:
				Log.i(TAG, "ACCESS: " + path);
				break;
			case FileObserver.ATTRIB:
				Log.i(TAG, "ATTRIB: " + path);
				break;
			case FileObserver.CLOSE_NOWRITE:
				Log.i(TAG, "CLOSE_NOWRITE: " + path);
				break;
			case FileObserver.CLOSE_WRITE:
				Log.i(TAG, "CLOSE_WRITE: " + path);
				break;
			case FileObserver.CREATE:
				Log.i(TAG, "CREATE: " + path);
				break;
			case FileObserver.DELETE:
				Log.i(TAG, "DELETE: " + path);
				break;
			case FileObserver.DELETE_SELF:
				Log.i(TAG, "DELETE_SELF: " + path);
				break;
			case FileObserver.MODIFY:
				Log.i(TAG, "MODIFY: " + path);
				break;
			case FileObserver.MOVE_SELF:
				Log.i(TAG, "MOVE_SELF: " + path);
				break;
			case FileObserver.MOVED_FROM:
				Log.i(TAG, "MOVED_FROM: " + path);
				break;
			case FileObserver.MOVED_TO:
				Log.i(TAG, "MOVED_TO: " + path);
				break;
			case FileObserver.OPEN:
				Log.i(TAG, "OPEN: " + path);
				break;
			default:
				Log.i(TAG, "DEFAULT(" + event + ") : " + path);
				break;
			}
		}
		
	}
}
