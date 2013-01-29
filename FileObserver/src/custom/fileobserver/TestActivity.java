package custom.fileobserver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class TestActivity extends Activity {

	FileWatcher mWatcher;
	final String TAG = TestActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
		String dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
		//mWatcher = new FileWatcher(sdcard,true,FileWatcher.FILE_CHANGED);
		mWatcher = new FileWatcher(dcim,true,FileWatcher.FILE_CHANGED);
		mWatcher.setFileListener(mFileListener);
		mWatcher.startWatching();
	}

	FileListener mFileListener = new FileListener(){

		@Override
		public void onFileCreated(String name) {
			Log.i(TAG, "onFileCreated " + name);
		}

		@Override
		public void onFileDeleted(String name) {
			Log.i(TAG, "onFileDeleted " + name);
		}

		@Override
		public void onFileModified(String name) {
			Log.i(TAG, "onFileModified " + name);
		}

		@Override
		public void onFileRenamed(String oldName, String newName) {
			Log.i(TAG, "onFileRenamed from: " + oldName + " to: " + newName);
		}
		
	};
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
}
