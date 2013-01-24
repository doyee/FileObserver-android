package com.arcsoft.provider.facerecognition;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
/**
 * 
 * @author ddai
 * FileWatcher support subdirectory(recursively)
 */
public class FileWatcher extends FileObserver {
	/** Only modification events */
	public static int CHANGES_ONLY = CREATE | DELETE | 
			CLOSE_WRITE | MOVED_FROM | MOVED_TO;

	ArrayList<FileObserver> mObservers;
	String mPath;
	int mMask;
	
	String mThreadName = FileWatcher.class.getSimpleName();
	HandlerThread mThread;
	Handler mThreadHandler;

	public FileWatcher(String path) {
		this(path, ALL_EVENTS);
	}

	public FileWatcher(String path, int mask) {
		super(path, mask);
		mPath = path;
		mMask = mask;
	}

	@Override
	public void startWatching() {
		mThreadName = FileWatcher.class.getSimpleName();
		if (mThread == null || !mThread.isAlive()) {

			FileLogger.printInfo("startFileWather new HandlerThread...");
			mThread = new HandlerThread(mThreadName,Process.THREAD_PRIORITY_BACKGROUND);
			mThread.start();

			mThreadHandler = new Handler(mThread.getLooper());
			mThreadHandler.post(new startRunnable());
		}
	}
	
	@Override
	public void stopWatching() {
		if(null != mThreadHandler && null != mThread && mThread.isAlive()){
			mThreadHandler.post(new stopRunnable());
		}
		mThreadHandler = null;
		mThread.quit();
		mThread = null;
	}

	@Override
	public void onEvent(int event, String path) {
		switch (event) {
		case FileObserver.ACCESS:
			Log.i("FileWatcher", "ACCESS: " + path);
			break;
		case FileObserver.ATTRIB:
			Log.i("FileWatcher", "ATTRIB: " + path);
			break;
		case FileObserver.CLOSE_NOWRITE:
			Log.i("FileWatcher", "CLOSE_NOWRITE: " + path);
			break;
		case FileObserver.CLOSE_WRITE:
			Log.i("FileWatcher", "CLOSE_WRITE: " + path);
			break;
		case FileObserver.CREATE:
			Log.i("FileWatcher", "CREATE: " + path);
			break;
		case FileObserver.DELETE:
			Log.i("FileWatcher", "DELETE: " + path);
			break;
		case FileObserver.DELETE_SELF:
			Log.i("FileWatcher", "DELETE_SELF: " + path);
			break;
		case FileObserver.MODIFY:
			Log.i("FileWatcher", "MODIFY: " + path);
			break;
		case FileObserver.MOVE_SELF:
			Log.i("FileWatcher", "MOVE_SELF: " + path);
			break;
		case FileObserver.MOVED_FROM:
			Log.i("FileWatcher", "MOVED_FROM: " + path);
			break;
		case FileObserver.MOVED_TO:
			Log.i("FileWatcher", "MOVED_TO: " + path);
			break;
		case FileObserver.OPEN:
			Log.i("FileWatcher", "OPEN: " + path);
			break;
		default:
			Log.i("FileWatcher", "DEFAULT(" + event + ";) : " + path);
			break;
		}
	}

	/**
	 * Monitor single directory and dispatch all events to its parent, with full
	 * path.
	 */
	class SingleFileObserver extends FileObserver {
		String mPath;

		public SingleFileObserver(String path) {
			this(path, ALL_EVENTS);
			mPath = path;
		}

		public SingleFileObserver(String path, int mask) {
			super(path, mask);
			mPath = path;
		}

		@Override
		public void onEvent(int event, String path) {
			String newPath = mPath + "/" + path;
			FileWatcher.this.onEvent(event, newPath);
		}
	}
	
	class startRunnable implements Runnable {
		@Override
		public void run() {
			synchronized (FileWatcher.this) {
				if (mObservers != null)
					return;

				mObservers = new ArrayList<FileObserver>();
				Stack<String> stack = new Stack<String>();
				stack.push(mPath);

				while (!stack.isEmpty()) {
					String parent = String.valueOf(stack.pop());
					mObservers.add(new SingleFileObserver(parent, mMask));
					File path = new File(parent);
					File[] files = path.listFiles();
					if (null == files)
						continue;
					for (File f : files) {
						if (f.isDirectory() && !f.getName().equals(".")
								&& !f.getName().equals("..")) {
							stack.push(f.getPath());
						}
					}
				}

				Iterator<FileObserver> it = mObservers.iterator();
				while (it.hasNext()) {
					FileObserver sfo = it.next();
					sfo.startWatching();
				}
			}
		}
	}

	class stopRunnable implements Runnable {
		@Override
		public void run() {
			synchronized (FileWatcher.this) {
				if (mObservers == null)
					return;

				Iterator<FileObserver> it = mObservers.iterator();
				while (it.hasNext()) {
					FileObserver sfo = it.next();
					sfo.stopWatching();
				}
				mObservers.clear();
				mObservers = null;
			}
		}
	}
}
