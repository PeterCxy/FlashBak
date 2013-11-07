package us.shandian.flashbak.helper;

import android.content.pm.ApplicationInfo;
import android.os.*;
import android.util.Log;
import android.util.Base64;

import java.util.ArrayList;
import java.io.File;

import us.shandian.flashbak.util.CMDProcessor;

public class BackupRestorer implements Runnable
{
	private ArrayList<ApplicationInfo> mAppList;
	private String mBackupName;
	private Handler mHandler;

	public static final int MSG_ERROR_SU = 1;
	public static final int MSG_ERROR_DIR = 2;
	public static final int MSG_ERROR_SHELL = 3;
	public static final int MSG_PROGRESS_CHANGE = 4;
	public static final int MSG_RESTORE_SUCCESS = 5;

    public BackupRestorer(ArrayList<ApplicationInfo> applist, String backupname, Handler handler) {
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
		if (!dir.exists()) {
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		}
		ApplicationInfo info;
		for (int i = 0; i < mAppList.size(); i++) {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_CHANGE, i + 1));
			info = mAppList.get(i);
			if (!cmd.su.runWaitFor("pm install -r " + backupDir + info.packageName + "/package.apk").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			String appUid = "";
			String appLib = "";
			String[] ls = cmd.su.runWaitFor("busybox ls -l /data/data").stdout.split(" ");
			Log.d("FlashBak", cmd.su.runWaitFor("busybox ls -l /data/data").stdout);
			for (String str : ls) {
				if (str.startsWith("app_")) {
					Log.d("FlashBak", str);
					appUid = str;
				} else if (str.startsWith(info.packageName.substring(0, info.packageName.length() - 1))) {
					Log.d("FlashBak", "found");
					break;
				}
			}
			ls = cmd.su.runWaitFor("busybox ls -l /data/data/" + info.packageName + "/lib").stdout.split(" ");
			for (String str : ls) {
				if (str.startsWith("/data/app-lib")) {
					appLib = str;
					break;
				}
			}
			if (!cmd.su.runWaitFor("busybox rm -rf /data/data/" + info.packageName).success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			if (!cmd.su.runWaitFor("busybox tar zxvf " + backupDir + info.packageName + "/data.tar.gz -C /").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			if (!cmd.su.runWaitFor("busybox chown -R " + appUid + ":" + appUid + " /data/data/" + info.packageName).success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			if (!cmd.su.runWaitFor("busybox ln -s " + appLib + " /data/data/" + info.packageName + "/lib").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
		}
		mHandler.sendEmptyMessage(MSG_RESTORE_SUCCESS);
	}
}
