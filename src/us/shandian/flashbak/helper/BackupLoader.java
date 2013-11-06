package us.shandian.flashbak.helper;

import android.util.Base64;
import android.os.*;
import android.content.*;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;

import us.shandian.flashbak.helper.BackupItemComparator;
import android.content.pm.*;

public class BackupLoader
{
    private List<Map<String, Object>> mBackupList = new ArrayList<Map<String, Object>>();
	private Context mContext;

    public BackupLoader(Context context) {
		mContext = context;
	}
	
	public void loadBackups() {
		mBackupList.clear();
		File parentFolder = new File(Environment.getExternalStorageDirectory() + "/FlashBak/");
		if (!parentFolder.exists()) {
			parentFolder.mkdir();
			return;
		}
		File[] files = parentFolder.listFiles();
		for (File file : files) {
			Map<String, Object> map = new HashMap<String, Object>();
			if (file.isDirectory()) {
				map.put("name", new String(Base64.decode(file.getName(), Base64.NO_WRAP)));
				map.put("timestamp", file.lastModified());
				map.put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date(file.lastModified())));
				map.put("num", file.listFiles().length);
				mBackupList.add(map);
			}
		}
		Collections.sort(mBackupList, new BackupItemComparator());
	}
	
	public int size() {
		return mBackupList.size();
	}
	
	public List<Map<String, Object>> getAll() {
		return mBackupList;
	}
	
	public Map<String, Object> get(String name) {
		for (int i = 0; i < mBackupList.size(); i++) {
			if (mBackupList.get(i).get("name").equals(name)) {
				return mBackupList.get(i);
			}
		}
		return null;
	}
	
	public ArrayList<ApplicationInfo> getInfo(String name) {
		ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
		String backupDir = Environment.getExternalStorageDirectory() + "/FlashBak/" + Base64.encodeToString(get(name).get("name").toString().getBytes(), Base64.NO_WRAP) + "/";
		File dir = new File(backupDir);
		if (!dir.exists()) {
			return applist;
		}
		File[] sub = dir.listFiles();
		for (File f : sub) {
			if (f.isDirectory()) {
				String apkPath = f.getPath() + "/package.apk";
				if (new File(apkPath).exists()) {
					PackageManager pm = mContext.getPackageManager();
					ApplicationInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES).applicationInfo;
					
					// BEGIN Fix icon error
					info.sourceDir = apkPath;
					info.publicSourceDir = apkPath;
					// END Fix icon error
					
					applist.add(info);
				}
			}
		}
		return applist;
	}
}
