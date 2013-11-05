package us.shandian.flashbak.helper;

import android.util.Base64;

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
import android.os.*;

public class BackupLoader
{
    private List<Map<String, Object>> mBackupList = new ArrayList<Map<String, Object>>();

    public void BackupLoader() {
		
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
}
