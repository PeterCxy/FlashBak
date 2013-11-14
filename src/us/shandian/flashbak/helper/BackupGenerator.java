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

public class BackupGenerator implements Runnable
{

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
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		}
		dir.mkdir();
		ApplicationInfo info;
		for (int i = 0; i < mAppList.size(); i++) {
			info = mAppList.get(i);
			if (info.packageName.equals("Contacts")) {
				backupContacts(backupDir);
				continue;
			}
			new File(backupDir + info.packageName + "/").mkdir();
			if (!cmd.su.runWaitFor("busybox cp " + info.sourceDir + " " + backupDir + info.packageName + "/package.apk").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			// Don't need to check for success because not all apps have odex file
			cmd.su.runWaitFor("busybox cp " + info.sourceDir.substring(0, info.sourceDir.length() - 3) + "odex" + " " + backupDir + info.packageName + "/package.odex");
			if (!cmd.su.runWaitFor("busybox tar czvf " + backupDir + info.packageName + "/data.tar.gz /data/data/" + info.packageName + " --exclude lib --exclude cache").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_CHANGE, i + 1));
		}
		mHandler.sendEmptyMessage(MSG_GENERATE_SUCCESS);
	}
	
	private void backupContacts(String dir) {
		// Credit: ShenduOS Team
		Cursor c = mContext.getContentResolver().query(Data.CONTENT_URI, null, null, null, "raw_contact_id");
		ArrayList<ContactInfo> contacts = new ArrayList<ContactInfo>();
		String name = "";
		String number = "";
		String email = "";
		int oldId = -1;
		while (c.moveToNext()) {
			int newId = c.getInt(c.getColumnIndex(Data.RAW_CONTACT_ID));
			if (newId != oldId) {
				// Raw id changed: this is a new contact
				if (oldId != -1) {
					ContactInfo info = new ContactInfo();
					info.setRawId(oldId);
					info.setName(name);
					info.setNumber(number);
					info.setEmail(email);
					contacts.add(info);
				}
				oldId = newId;
			}
			
			String mime = c.getString(c.getColumnIndex(Data.MIMETYPE));
			if (Phone.CONTENT_ITEM_TYPE.equals(mime)) {
				String phoneType = c.getString(c.getColumnIndex(Phone.TYPE));
				String phoneNumber = c.getString(c.getColumnIndex(Phone.NUMBER));
				number = number + phoneType + ":" + phoneNumber + ";";
			} else if (StructuredName.CONTENT_ITEM_TYPE.equals(mime)) {
				name = c.getString(c.getColumnIndex(StructuredName.DISPLAY_NAME));
			} else if (Email.CONTENT_ITEM_TYPE.equals(mime)) {
				String emailType = c.getString(c.getColumnIndex(Email.TYPE));
				String emailAddress = c.getString(c.getColumnIndex(Email.ADDRESS));
				email = email + emailType + ":" + emailAddress + ";";
			}
		}
		
		writeContacts(dir, contacts);
	}
	
	private void writeContacts(String dir, ArrayList<ContactInfo> list) {
		File f = new File(dir + "Contacts/");
		if (f.exists()) {
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		} else {
			f.mkdir();
		}
		
		for (int i = 0; i < list.size(); i++) {
			ContactInfo info = list.get(i);
			String subPath = dir + "Contacts/" + i + "/";
			File sub = new File(subPath);
			if (sub.exists()) {
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return;
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
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return;
			} else {
				try {
					data.createNewFile();
				} catch (IOException e1) {
					mHandler.sendEmptyMessage(MSG_ERROR_DIR);
					return;
				}
			}
			FileOutputStream opt;
			try {
				opt = new FileOutputStream(data);
			} catch (FileNotFoundException e2) {
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return;
			}
			
			try {
				opt.write(str.toString().getBytes());
				opt.close();
			} catch (IOException e3) {
				mHandler.sendEmptyMessage(MSG_ERROR_DIR);
				return;
			}
		}
	}
}
