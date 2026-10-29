package com.velqor.app.files;

import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectoryWatcher {

    public interface OnDirectoryChangeListener {
        void onDirectoryChanged(List<File> files);
    }

    public static class FileSnapshot {
        public final String path;
        public final long length;
        public final long lastModified;
        public final boolean isDir;

        public FileSnapshot(File f) {
            this.path = f.getAbsolutePath();
            this.length = f.length();
            this.lastModified = f.lastModified();
            this.isDir = f.isDirectory();
        }

        public boolean matches(File f) {
            return f.length() == length && f.lastModified() == lastModified && f.isDirectory() == isDir;
        }
    }

    private File mCurrentDir;
    private boolean mShowHidden;
    private int mSortField;
    private boolean mSortAsc;

    private FileObserver mFileObserver;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean mIsActive = new AtomicBoolean(false);
    private final AtomicBoolean mIsPaused = new AtomicBoolean(false);
    private final AtomicBoolean mIsScanning = new AtomicBoolean(false);

    private final Map<String, FileSnapshot> mLastSnapshot = new HashMap<>();
    private OnDirectoryChangeListener mListener;

    private final Runnable mPeriodicScanTask = new Runnable() {
        @Override
        public void run() {
            if (!mIsActive.get()) return;
            if (!mIsPaused.get()) {
                scanInBackground(false);
            }
            if (mIsActive.get()) {
                mMainHandler.postDelayed(this, 3000);
            }
        }
    };

    private final Runnable mDebouncedScanTask = () -> {
        if (mIsActive.get() && !mIsPaused.get()) {
            scanInBackground(false);
        }
    };

    public DirectoryWatcher(OnDirectoryChangeListener listener) {
        mListener = listener;
    }

    public void setListener(OnDirectoryChangeListener listener) {
        mListener = listener;
    }

    public void watchDirectory(File dir, boolean showHidden, int sortField, boolean sortAsc) {
        mCurrentDir = dir;
        mShowHidden = showHidden;
        mSortField = sortField;
        mSortAsc = sortAsc;

        synchronized (mLastSnapshot) {
            mLastSnapshot.clear();
        }

        mIsActive.set(true);
        startFileObserver(dir);

        mMainHandler.removeCallbacks(mPeriodicScanTask);
        mMainHandler.postDelayed(mPeriodicScanTask, 3000);

        scanInBackground(true);
    }

    public void updateFilterSort(boolean showHidden, int sortField, boolean sortAsc) {
        mShowHidden = showHidden;
        mSortField = sortField;
        mSortAsc = sortAsc;
        scanImmediate();
    }

    public void setPaused(boolean paused) {
        boolean wasPaused = mIsPaused.getAndSet(paused);
        if (wasPaused && !paused && mIsActive.get()) {
            scanImmediate();
        }
    }

    public boolean isPaused() {
        return mIsPaused.get();
    }

    public void scanImmediate() {
        mMainHandler.removeCallbacks(mDebouncedScanTask);
        scanInBackground(false);
    }

    public void stop() {
        mIsActive.set(false);
        mMainHandler.removeCallbacks(mPeriodicScanTask);
        mMainHandler.removeCallbacks(mDebouncedScanTask);
        stopFileObserver();
    }

    public void destroy() {
        stop();
        mExecutor.shutdown();
    }

    private void startFileObserver(File dir) {
        stopFileObserver();
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        int mask = FileObserver.CREATE | FileObserver.DELETE
                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO
                | FileObserver.CLOSE_WRITE;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mFileObserver = new FileObserver(dir, mask) {
                    @Override
                    public void onEvent(int event, String path) {
                        onFileEventReceived();
                    }
                };
            } else {
                mFileObserver = new FileObserver(dir.getAbsolutePath(), mask) {
                    @Override
                    public void onEvent(int event, String path) {
                        onFileEventReceived();
                    }
                };
            }
            mFileObserver.startWatching();
        } catch (Throwable ignored) {
        }
    }

    private void stopFileObserver() {
        if (mFileObserver != null) {
            try {
                mFileObserver.stopWatching();
            } catch (Throwable ignored) {
            }
            mFileObserver = null;
        }
    }

    private void onFileEventReceived() {
        if (!mIsActive.get() || mIsPaused.get()) return;
        mMainHandler.removeCallbacks(mDebouncedScanTask);
        mMainHandler.postDelayed(mDebouncedScanTask, 300);
    }

    private void scanInBackground(boolean forceCallback) {
        if (mCurrentDir == null || !mCurrentDir.exists() || !mCurrentDir.isDirectory()) return;
        if (!mIsScanning.compareAndSet(false, true)) return;

        mExecutor.execute(() -> {
            try {
                File dir = mCurrentDir;
                if (dir == null || !dir.exists()) return;

                List<File> currentFiles = FileRepository.listFiles(dir, mShowHidden, mSortField, mSortAsc);

                boolean changed = forceCallback;
                if (!changed) {
                    synchronized (mLastSnapshot) {
                        if (currentFiles.size() != mLastSnapshot.size()) {
                            changed = true;
                        } else {
                            for (File f : currentFiles) {
                                FileSnapshot snap = mLastSnapshot.get(f.getAbsolutePath());
                                if (snap == null || !snap.matches(f)) {
                                    changed = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (changed) {
                    Map<String, FileSnapshot> newSnapshot = new HashMap<>();
                    for (File f : currentFiles) {
                        newSnapshot.put(f.getAbsolutePath(), new FileSnapshot(f));
                    }
                    synchronized (mLastSnapshot) {
                        mLastSnapshot.clear();
                        mLastSnapshot.putAll(newSnapshot);
                    }
                    mMainHandler.post(() -> {
                        if (mListener != null && mIsActive.get() && !mIsPaused.get()) {
                            mListener.onDirectoryChanged(currentFiles);
                        }
                    });
                }
            } finally {
                mIsScanning.set(false);
            }
        });
    }

    public static boolean hasDirectoryChanged(List<File> files, Map<String, FileSnapshot> snapshot) {
        if (files == null || snapshot == null) return true;
        if (files.size() != snapshot.size()) return true;
        for (File f : files) {
            FileSnapshot snap = snapshot.get(f.getAbsolutePath());
            if (snap == null || !snap.matches(f)) {
                return true;
            }
        }
        return false;
    }
}
