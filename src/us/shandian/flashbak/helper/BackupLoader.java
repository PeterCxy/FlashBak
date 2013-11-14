package us.shandian.flashbak.helper;

import android.util.Base64;
import android.os.*;
import android.content.*;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.pm.*;

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

public class BackupLoader implements Parcelable
{
	
	private List<Map<String, Object>> mBackupList = new ArrayList<Map<String, Object>>();

    public BackupLoader() {
		
	}
	
	public BackupLoader(Parcel parcel) {
		parcel.readList(mBackupList, getClass().getClassLoader());
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
	
	public ArrayList<ApplicationInfo> getInfo(String name, Context context) {
		ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
		String backupDir = Environment.getExternalStorageDirectory() + "/FlashBak/" + Base64.encodeToString(name.getBytes(), Base64.NO_WRAP) + "/";
		File dir = new File(backupDir);
		if (!dir.exists()) {
			return applist;
		}
		File[] sub = dir.listFiles();
		for (File f : sub) {
			if (f.isDirectory()) {
				String apkPath = f.getPath() + "/package.apk";
				if (new File(apkPath).exists()) {
					PackageManager pm = context.getPackageManager();
					ApplicationInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES).applicationInfo;
					
					// BEGIN Fix icon error
					info.sourceDir = apkPath;
					info.publicSourceDir = apkPath;
					// END Fix icon error
					
					applist.add(info);
				} else {
					ApplicationInfo info = new ApplicationInfo();
					if (f.getName().equals("Contacts")) {
						info.packageName = "Contacts";
						applist.add(info);
					}
				}
			}
		}
		return applist;
	}
	
	public void delete(String name) {
		deleteDir(Environment.getExternalStorageDirectory() + "/FlashBak/" + Base64.encodeToString(name.getBytes(), Base64.NO_WRAP) + "/");
	}
	
	private void deleteDir(String dirName) {
		File dir = new File(dirName);
		File[] sub = dir.listFiles();
		for (File f : sub) {
			if (f.isDirectory()) {
				deleteDir(f.getPath());
			} else {
				f.delete();
			}
		}
		dir.delete();
	}
	
	
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeList(mBackupList);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	public static final Parcelable.Creator<BackupLoader> CREATOR = new Parcelable.Creator<BackupLoader>() {
		@Override
		public BackupLoader createFromParcel(Parcel parcel) {
			return new BackupLoader(parcel);
		}
		
		@Override
		public BackupLoader[] newArray(int size) {
			return new BackupLoader[size];
		}
	};
}
