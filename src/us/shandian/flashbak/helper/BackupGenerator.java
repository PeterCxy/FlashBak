package us.shandian.flashbak.helper;

import android.content.pm.ApplicationInfo;
import android.os.*;
import android.util.Log;
import android.util.Base64;

import java.util.ArrayList;
import java.io.File;

import us.shandian.flashbak.util.CMDProcessor;

public class BackupGenerator implements Runnable
{

	private ArrayList<ApplicationInfo> mAppList;
	private String mBackupName;
	private Handler mHandler;
	
	public static final int MSG_ERROR_SU = 1;
	public static final int MSG_ERROR_DIR = 2;
	public static final int MSG_ERROR_SHELL = 3;
	public static final int MSG_PROGRESS_CHANGE = 4;
	public static final int MSG_GENERATE_SUCCESS = 5;

    public BackupGenerator(ArrayList<ApplicationInfo> applist, String backupname, Handler handler) {
		mAppList = applist;
		mBackupName = backupname;
		mHandler = handler;
	}
	
	@Override
	public void run() {
		CMDProcessor cmd = new CMDProcessor();
		if (!cmd.canSU()) {
			mHandler.sendEmptyMessage(MSG_ERROR_SU);
			return;
		}
		String backupDir = Environment.getExternalStorageDirectory() + "/FlashBak/" + Base64.encodeToString(mBackupName.getBytes(), Base64.NO_WRAP) + "/";
		File dir = new File(backupDir);
		if (dir.exists()) {
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		}
		dir.mkdir();
		ApplicationInfo info;
		for (int i = 0; i < mAppList.size(); i++) {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_CHANGE, i + 1));
			info = mAppList.get(i);
			new File(backupDir + info.packageName + "/").mkdir();
			if (!cmd.su.runWaitFor("busybox cp " + info.sourceDir + " " + backupDir + info.packageName + "/package.apk").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			if (!cmd.su.runWaitFor("busybox tar czvf " + backupDir + info.packageName + "/data.tar.gz /data/data/" + info.packageName + " --exclude lib --exclude cache").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
		}
		mHandler.sendEmptyMessage(MSG_GENERATE_SUCCESS);
	}
}
