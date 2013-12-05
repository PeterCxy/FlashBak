package us.shandian.flashbak.helper;

import android.util.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileNotFoundException;

import us.shandian.flashbak.util.CMDProcessor;
import us.shandian.flashbak.helper.BackupLoader;
import static us.shandian.flashbak.BuildConfig.DEBUG;

public class BackupMerger implements Runnable
{
	
	private String TAG = "BackupMerger";
	
	private BackupLoader mBackups;
	private String[] mBackupNames;
	private String mBackupName;
	private ArrayList<ApplicationInfo> mAppsToMerge;
	private ArrayList<ApplicationInfo> mCheckedAppsToMerge;
	private Handler mHandler;
	private Context mContext;
	
	public static final int MSG_ERROR_SU = 1;
	public static final int MSG_ERROR_DIR = 2;
	public static final int MSG_ERROR_SHELL = 3;
	public static final int MSG_PROGRESS_CHANGE = 4;
	public static final int MSG_MERGE_SUCCESS = 5;
	
	public BackupMerger(Context context, Handler handler, BackupLoader backups, final String[] backupNames) {
		mContext = context;
		mHandler = handler;
		mBackups = backups;
		mBackupNames = backupNames;
	}
	
	public void load() {
		mAppsToMerge = new ArrayList<ApplicationInfo>();
		for (String name : mBackupNames) {
			if (name.equals("")) {
				continue;
			}
			ArrayList<ApplicationInfo> info = mBackups.getInfo(name, mContext);
			for (ApplicationInfo app : info) {
				boolean skip = false;
				for (ApplicationInfo existingApp : mAppsToMerge) {
					if (existingApp.packageName.equals(app.packageName)) {
						skip = true;
						break;
					}
				}
				
				if (skip) {
					continue;
				} else {
					mAppsToMerge.add(app);
				}
			}
		}
	}
	
	public ArrayList<ApplicationInfo> getAll() {
		return mAppsToMerge;
	}
	
	public void setCheckedAppsToMerge(ArrayList<ApplicationInfo> apps) {
		mCheckedAppsToMerge = apps;
	}
	
	public void setBackupName(String name) {
		mBackupName = name;
	}
	
	@Override
	public void run() {
		CMDProcessor cmd = new CMDProcessor();
		if (!cmd.canSU()) {
			mHandler.sendEmptyMessage(MSG_ERROR_SU);
			return;
		}
		String backupRoot = Environment.getExternalStorageDirectory() + "/FlashBak/";
		String backupDir = backupRoot + Base64.encodeToString(mBackupName.getBytes(), Base64.NO_WRAP) + "/";
		File dir = new File(backupDir);
		if (dir.exists()) {
			if (DEBUG) {
				Log.d(TAG, "Directory " + backupDir + " already exists");
			}
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		}
		dir.mkdir();
		ApplicationInfo info;
		Map<String, ArrayList<ApplicationInfo>> mapOfAllApps = new HashMap<String, ArrayList<ApplicationInfo>>();
		for (String backup : mBackupNames) {
			mapOfAllApps.put(backup, mBackups.getInfo(backup, mContext));
		}
		for (int i = 0; i < mCheckedAppsToMerge.size(); i++) {
			info = mCheckedAppsToMerge.get(i);
			for (String backup : mBackupNames) {
				String thisDir = backupRoot + Base64.encodeToString(backup.getBytes(), Base64.NO_WRAP) + "/";
				ArrayList<ApplicationInfo> apps = mapOfAllApps.get(backup);
				for (ApplicationInfo app : apps) {
					if (app.packageName.equals(info.packageName)) {
						if (!cmd.su.runWaitFor("busybox cp -R " + thisDir + app.packageName + " " + backupDir + app.packageName).success()) {
							if (DEBUG) {
								Log.d(TAG, "Command line " + "busybox cp -R " + thisDir + app.packageName + " " + backupDir + app.packageName + " exited with failure");
							}
							mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
							return;
						} else {
							// Here we only try to keep the first backup
							break;
						}
					}
				}
			}
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_CHANGE, i + 1));
		}
		mHandler.sendEmptyMessage(MSG_MERGE_SUCCESS);
	}
}
