/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package custom.fileobserver;
import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

import custom.fileobserver.TestActivity.FileWatcher;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

/**
 * Monitors files (using <a href="http://en.wikipedia.org/wiki/Inotify">inotify</a>)
 * to fire an event after files are accessed or changed by by any process on
 * the device (including this one).  FileObserver is an abstract class;
 * subclasses must implement the event handler {@link #onEvent(int, String)}.
 *
 * <p>Each FileObserver instance monitors a single file or directory.
 * If a directory is monitored, events will be triggered for all files and
 * subdirectories (recursively) inside the monitored directory.</p>
 *
 * <p>An event mask is used to specify which changes or actions to report.
 * Event type constants are used to describe the possible changes in the
 * event mask as well as what actually happened in event callbacks.</p>
 *
 * <p class="caution"><b>Warning</b>: If a FileObserver is garbage collected, it
 * will stop sending events.  To ensure you keep receiving events, you must
 * keep a reference to the FileObserver instance from some other live object.</p>
 */
/**
 * 
 * @author Dai Dongsheng
 * Email: doyee@163.com
 * 
 */
public abstract class FileObserver {
    /** Event type: Data was read from a file */
    public static final int ACCESS = 0x00000001;
    /** Event type: Data was written to a file */
    public static final int MODIFY = 0x00000002;
    /** Event type: Metadata (permissions, owner, timestamp) was changed explicitly */
    public static final int ATTRIB = 0x00000004;
    /** Event type: Someone had a file or directory open for writing, and closed it */
    public static final int CLOSE_WRITE = 0x00000008;
    /** Event type: Someone had a file or directory open read-only, and closed it */
    public static final int CLOSE_NOWRITE = 0x00000010;
    /** Event type: A file or directory was opened */
    public static final int OPEN = 0x00000020;
    /** Event type: A file or subdirectory was moved from the monitored directory */
    public static final int MOVED_FROM = 0x00000040;
    /** Event type: A file or subdirectory was moved to the monitored directory */
    public static final int MOVED_TO = 0x00000080;
    /** Event type: A new file or subdirectory was created under the monitored directory */
    public static final int CREATE = 0x00000100;
    /** Event type: A file was deleted from the monitored directory */
    public static final int DELETE = 0x00000200;
    /** Event type: The monitored file or directory was deleted; monitoring effectively stops */
    public static final int DELETE_SELF = 0x00000400;
    /** Event type: The monitored file or directory was moved; monitoring continues */
    public static final int MOVE_SELF = 0x00000800;
   
    public final static int IN_ISDIR = 0x40000000;
    /** Event type: a directory create under the mointored directory*/
    public final static int DIR_CREATE = IN_ISDIR | CREATE;

    /** Event mask: All valid event types, combined */
    public static final int ALL_EVENTS = ACCESS | MODIFY | ATTRIB | CLOSE_WRITE
            | CLOSE_NOWRITE | OPEN | MOVED_FROM | MOVED_TO | DELETE | CREATE
            | DELETE_SELF | MOVE_SELF;
    
	public static int ANY_CHANGED = CREATE | DELETE | 
			DELETE_SELF | MOVED_FROM | MOVED_TO | ATTRIB | MODIFY;

    private static final String LOG_TAG = "FileObserver";


    private static class FolderFilter implements FileFilter{
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }
    private static class ObserverThread extends Thread {
        private HashMap<Integer, WeakReference<Object>> mObservers = new HashMap<Integer, WeakReference<Object>>();
        private HashMap<Integer,String> mListPath = new HashMap<Integer,String>();
        private FolderFilter mFilter = new FolderFilter();
        private int m_fd;

        public ObserverThread() {
            super("FileObserver");
            m_fd = init();
        }

        public void run() {
            observe(m_fd);
        }

        public int startWatching(String observed, String path, int mask, FileObserver observer) {
            int wfd = startWatching(m_fd, path, mask);

            Integer i = new Integer(wfd);
            if (wfd >= 0) {
                synchronized (mObservers) {
                    mObservers.put(i, new WeakReference<Object>(observer));
                    mListPath.put(i, path.replaceFirst(observed, ""));

                    File rootFolder = new File(path);
                    File[] childFolders = rootFolder.listFiles(mFilter);
                    if((childFolders != null))
                    {
                        for(int index = 0; index < childFolders.length; index++)
                            startWatching(observed, childFolders[index].getPath(), mask, observer);
                    }

                }
            }

            return i;
        }

        public void stopWatching(int descriptor, FileObserver observer) {
            synchronized(mObservers)
            {
                stopWatching(m_fd, descriptor);
                mListPath.remove(descriptor);
                mObservers.remove(descriptor);

                Iterator <Integer> it = mListPath.keySet().iterator();
                while(it.hasNext())
                {
                    Integer fd = it.next();
                    if(mObservers.get(fd).get() == observer)
                    {
                        stopWatching(m_fd, fd);
                        it.remove();
                        mObservers.remove(fd);
                    }
                }
            }
        }

		public void onEvent(int wfd, int mask, int cookie, String path) {
			// look up our observer, fixing up the map if necessary...
			FileObserver observer = null;

			synchronized (mObservers) {
				WeakReference<Object> weak = mObservers.get(wfd);
				if (weak != null) { // can happen with lots of events from a
									// dead wfd
					observer = (FileObserver) weak.get();
					if (observer == null) {
						mObservers.remove(wfd);
						mListPath.remove(wfd);
					}
				}
			}

			// ...then call out to the observer without the sync lock held
			if (observer == null) {
				return;
			}

			try {
				String parentDir = observer.mPath;
				String p = mListPath.get(wfd);
				String observed = parentDir + p ;
				if (path != null) {
					if (p.length() > 0) {
						p += "/";
					}
					p += path;
				}
				
				String absPath = parentDir + p;
				if ((mask & CREATE) != 0 && (mask & IN_ISDIR) != 0) {
					//auto to watch the subdirectory
					startWatching(observed, absPath, observer.mMask, observer);
				}

				observer.onEvent(mask, absPath);
			} catch (Throwable throwable) {
				Log.wtf(LOG_TAG, "Unhandled exception in FileObserver "
						+ observer, throwable);
			}

		}

        private native int init();
        private native void observe(int fd);
        private native int startWatching(int fd, String path, int mask);
        private native void stopWatching(int fd, int wfd);
    }

   
    private static ObserverThread s_observerThread;

    static {
    	try{
    		System.loadLibrary("fileobserver_jni");
    	}catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
    	
    	
    	/*try {
			Thread.sleep(5000);
    	} catch (InterruptedException e) {
    	    e.printStackTrace();
    	}*/
    	
        s_observerThread = new ObserverThread();
        s_observerThread.start();
    }

    // instance
    private String mPath;
    private Integer mDescriptor;
    private int mMask;
    
	String mThreadName = FileObserver.class.getSimpleName();
	HandlerThread mThread;
	Handler mThreadHandler;

    /**
     * Equivalent to FileObserver(path, FileObserver.ALL_EVENTS).
     */
    public FileObserver(String path) {
        this(path, ALL_EVENTS);
    }

    /**
     * Create a new file observer for a certain file or directory.
     * Monitoring does not start on creation!  You must call
     * {@link #startWatching()} before you will receive events.
     *
     * @param path The file or directory to monitor
     * @param mask The event or events (added together) to watch for
     */
    public FileObserver(String path, int mask) {
        mPath = path;
        mMask = mask;
        mDescriptor = -1;
    }

    protected void finalize() {
        stopWatching();
    }

    /**
     * Start watching for events.  The monitored file or directory must exist at
     * this time, or else no events will be reported (even if it appears later).
     * If monitoring is already started, this call has no effect.
     */
    public void startWatching() {
    	mThreadName = FileWatcher.class.getSimpleName();
		if (mThread == null || !mThread.isAlive()) {

			mThread = new HandlerThread(mThreadName,Process.THREAD_PRIORITY_BACKGROUND);
			mThread.setDaemon(true);
			mThread.start();

			mThreadHandler = new Handler(mThread.getLooper());
			mThreadHandler.post(new Runnable() {
				@Override
				public void run() {

			        if (mDescriptor < 0) {
			            mDescriptor = s_observerThread.startWatching(mPath, mPath, mMask, FileObserver.this);
			        }
				}
			});
		}

    }

    /**
     * Stop watching for events.  Some events may be in process, so events
     * may continue to be reported even after this method completes.  If
     * monitoring is already stopped, this call has no effect.
     */
    public void stopWatching() {
		if(null != mThreadHandler && null != mThread && mThread.isAlive()){
			mThreadHandler.post(new Runnable() {
				@Override
				public void run() {

			        if (mDescriptor >= 0) {
			            s_observerThread.stopWatching(mDescriptor, FileObserver.this);
			            mDescriptor = -1;
			        }
				}
			});
		}
		mThreadHandler = null;
		mThread.quit();
		mThread = null;

    }

    /**
     * The event handler, which must be implemented by subclasses.
     *
     * <p class="note">This method is invoked on a special FileObserver thread.
     * It runs independently of any threads, so take care to use appropriate
     * synchronization!  Consider using {@link Handler#post(Runnable)} to shift
     * event handling work to the main thread to avoid concurrency problems.</p>
     *
     * <p>Event handlers must not throw exceptions.</p>
     *
     * @param event The type of event which happened
     * @param path The path, relative to the main monitored file or directory,
     *     of the file or directory which triggered the event
     */
    public abstract void onEvent(int event, String path);

}
