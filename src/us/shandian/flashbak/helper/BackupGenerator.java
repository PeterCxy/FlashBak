package us.shandian.flashbak.helper;

import android.content.pm.ApplicationInfo;
import android.os.*;
import android.util.Log;
import android.util.Base64;
import android.database.Cursor;
import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.*;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import us.shandian.flashbak.util.CMDProcessor;
import us.shandian.flashbak.helper.contact.ContactInfo;
import static us.shandian.flashbak.BuildConfig.DEBUG;

public class BackupGenerator implements Runnable
{

	private static final String TAG = "BackupGeneratpr";
	
	private ArrayList<ApplicationInfo> mAppList;
	private String mBackupName;
	private Handler mHandler;
	private Context mContext;
	
	public static final int MSG_ERROR_SU = 1;
	public static final int MSG_ERROR_DIR = 2;
	public static final int MSG_ERROR_SHELL = 3;
	public static final int MSG_PROGRESS_CHANGE = 4;
	public static final int MSG_GENERATE_SUCCESS = 5;

    public BackupGenerator(ArrayList<ApplicationInfo> applist, String backupname, Handler handler, Context context) {
		mAppList = applist;
		mBackupName = backupname;
		mHandler = handler;
		mContext = context;
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
			if (DEBUG) {
				Log.d(TAG, "Directory " + backupDir + " already exists");
			}
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		}
		dir.mkdir();
		ApplicationInfo info;
		for (int i = 0; i < mAppList.size(); i++) {
			info = mAppList.get(i);
			if (info.packageName.equals("Contacts")) {
				if (!backupContacts(backupDir)) {
					return;
				}
				continue;
			}
			new File(backupDir + info.packageName + "/").mkdir();
			if (!cmd.su.runWaitFor("busybox cp " + info.sourceDir + " " + backupDir + info.packageName + "/package.apk").success()) {
				if (DEBUG) {
					Log.d(TAG, "Command line " + "busybox cp " + info.sourceDir + " " + backupDir + info.packageName + "/package.apk" + " exited with failure");
				}
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			// Don't need to check for success because not all apps have odex file
			cmd.su.runWaitFor("busybox cp " + info.sourceDir.substring(0, info.sourceDir.length() - 3) + "odex" + " " + backupDir + info.packageName + "/package.odex");
			if (!cmd.su.runWaitFor("busybox tar czvf " + backupDir + info.packageName + "/data.tar.gz /data/data/" + info.packageName + " --exclude lib --exclude cache").success()) {
				if (DEBUG) {
					Log.d(TAG, "Command line " + "busybox tar czvf " + backupDir + info.packageName + "/data.tar.gz /data/data/" + info.packageName + " --exclude lib --exclude cache" + " exited with failure");
				}
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_CHANGE, i + 1));
		}
		mHandler.sendEmptyMessage(MSG_GENERATE_SUCCESS);
	}
	
	private boolean backupContacts(String dir) {
		// Credit: ShenduOS Team
		Cursor c = mContext.getContentResolver().query(Data.CONTENT_URI, null, null, null, "raw_contact_id");
		ArrayList<ContactInfo> contacts = new ArrayList<ContactInfo>();
		String name = "";
		String number = "";
		String email = "";
		int oldId = -1;
		ContactInfo info = null;
		while (c.moveToNext()) {
			int newId = c.getInt(c.getColumnIndex(Data.RAW_CONTACT_ID));
			if (newId != oldId) {
				// Raw id changed: this is a new contact
				oldId = newId;
				info = new ContactInfo();
				info.setRawId(oldId);
				contacts.add(info);
				name = "";
				number = "";
				email = "";
			}
			
			String mime = c.getString(c.getColumnIndex(Data.MIMETYPE));
			if (Phone.CONTENT_ITEM_TYPE.equals(mime)) {
				String phoneType = c.getString(c.getColumnIndex(Phone.TYPE));
				String phoneNumber = c.getString(c.getColumnIndex(Phone.NUMBER));
				number = number + phoneType + ":" + phoneNumber + ";";
				info.setNumber(number);
			} else if (StructuredName.CONTENT_ITEM_TYPE.equals(mime)) {
				name = c.getString(c.getColumnIndex(StructuredName.DISPLAY_NAME));
				info.setName(name);
			} else if (Email.CONTENT_ITEM_TYPE.equals(mime)) {
				String emailType = c.getString(c.getColumnIndex(Email.TYPE));
				String emailAddress = c.getString(c.getColumnIndex(Email.ADDRESS));
				email = email + emailType + ":" + emailAddress + ";";
				info.setEmail(email);
			}
		}
		
		c.close();
		
		return writeContacts(dir, contacts);
	}
	
	private boolean writeContacts(String dir, ArrayList<ContactInfo> list) {
		File f = new File(dir + "Contacts/");
		if (f.exists()) {
			if (DEBUG) {
				Log.d(TAG, "Directory " + dir + "Contacts/" + " already exists");
			}
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return false;
		} else {
			f.mkdir();
		}
		
		for (ContactInfo info : list) {
			if (info == null || info.getName() == null) {
				continue;
			}
			String subPath = dir + "Contacts/" + Base64.encodeToString(info.getName().getBytes(), Base64.NO_WRAP) + "/";
			File sub = new File(subPath);
			if (sub.exists()) {
				if (DEBUG) {
					Log.d(TAG, "Directory " + subPath + " already exists");
				}
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return false;
			} else {
				sub.mkdir();
			}
			
			StringBuilder str = new StringBuilder();
			str.append(info.getName()).append("\n");
			str.append(info.getRawId()).append("\n");
			str.append(info.getEmail()).append("\n");
			str.append(info.getNumber());
			
			File data = new File(subPath + "data");
			if (data.exists()) {
				if (DEBUG) {
					Log.d(TAG, "File " + subPath + "data" + " already exists");
				}
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return false;
			} else {
				try {
					data.createNewFile();
				} catch (IOException e1) {
					if (DEBUG) {
						Log.d(TAG, "Create file " + subPath + "data" + " failed");
					}
					mHandler.sendEmptyMessage(MSG_ERROR_DIR);
					return false;
				}
			}
			FileOutputStream opt;
			try {
				opt = new FileOutputStream(data);
			} catch (FileNotFoundException e2) {
				if (DEBUG) {
					Log.d(TAG, "Open file " + subPath + "data" + " failed");
				}
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return false;
			}
			
			try {
				opt.write(str.toString().getBytes());
				opt.close();
			} catch (IOException e3) {
				if (DEBUG) {
					Log.d(TAG, "Write file " + subPath + "data" + " failed");
				}
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return false;
			}
		}
		return true;
	}
}
