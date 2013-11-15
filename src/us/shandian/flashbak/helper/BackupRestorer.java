package us.shandian.flashbak.helper;

import android.content.pm.ApplicationInfo;
import android.os.*;
import android.util.Base64;
import android.database.Cursor;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.*;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;

import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import us.shandian.flashbak.util.CMDProcessor;
import us.shandian.flashbak.helper.contact.ContactInfo;

public class BackupRestorer implements Runnable
{
	private ArrayList<ApplicationInfo> mAppList;
	private String mBackupName;
	private Handler mHandler;
	private Context mContext;

	public static final int MSG_ERROR_SU = 1;
	public static final int MSG_ERROR_DIR = 2;
	public static final int MSG_ERROR_SHELL = 3;
	public static final int MSG_PROGRESS_CHANGE = 4;
	public static final int MSG_RESTORE_SUCCESS = 5;

    public BackupRestorer(ArrayList<ApplicationInfo> applist, String backupname, Handler handler, Context context) {
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
		if (!dir.exists()) {
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return;
		}
		ApplicationInfo info;
		for (int i = 0; i < mAppList.size(); i++) {
			info = mAppList.get(i);
			if (info.packageName.equals("Contacts")) {
				if (!restoreContacts(backupDir)) {
					return;
				}
				continue;
			}
			if (!cmd.su.runWaitFor("pm install -r " + backupDir + info.packageName + "/package.apk").success()) {
				mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
				return;
			}
			String appUid = "";
			String appLib = "";
			String[] ls = cmd.su.runWaitFor("busybox ls -l /data/data").stdout.split(" ");
			for (String str : ls) {
				if (str.startsWith("app_")) {
					appUid = str;
				} else if (str.startsWith(info.packageName.substring(0, info.packageName.length() - 1))) {
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
			
			// Check for odex file
			if (new File(backupDir + info.packageName + "/package.odex").exists()) {
				String name = "/data/app/" + info.packageName + "-";
				cmd.su.runWaitFor("busybox rm " + name + "1" + ".odex"); // Delete possibly existing odex
				cmd.su.runWaitFor("busybox rm " + name + "2" + ".odex");
				for (int j = 1; j <= 2; j++) {
					if (new File(name + j + ".apk").exists()) {
						name = name + j + ".odex";
						break;
					}
				}
				
				if (!cmd.su.runWaitFor("busybox cp " + backupDir + info.packageName + "/package.odex " + name).success()) {
					mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
					return;
				}
				if (!cmd.su.runWaitFor("busybox chown system:" + appUid + " " + name).success()) {
					mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
					return;
				}
				if (!cmd.su.runWaitFor("busybox chmod 0644 " + name).success()) {
					mHandler.sendEmptyMessage(MSG_ERROR_SHELL);
					return;
				}
			}
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS_CHANGE, i + 1));
		}
		mHandler.sendEmptyMessage(MSG_RESTORE_SUCCESS);
	}
	
	private boolean restoreContacts(String dir) {
		ArrayList<ContactInfo> info = loadContacts(dir);
		if (info == null) {
			return false;
		}
		ContentResolver c = mContext.getContentResolver();
		c.delete(RawContacts.CONTENT_URI.buildUpon()
				 .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
				 .build(), null, null);
		
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		for (ContactInfo contact : info) {
			operations.add(ContentProviderOperation
						   .newInsert(Uri.parse("content://com.android.contacts/raw_contacts"))
						   .withValue("_id", String.valueOf(contact.getRawId()))
						   .build());
		}
		
		try {
			c.applyBatch("com.android.contacts", operations);
		} catch (RemoteException e1) {
			return false;
		} catch (OperationApplicationException e2) {
			return false;
		}
		
		ArrayList<ContentValues> values = new ArrayList<ContentValues>();
		for (ContactInfo contact : info) {
			int id = contact.getRawId();
			
			String name = contact.getName();
			if (name != null && name != "null") {
				ContentValues v = new ContentValues();
				v.put(Data.RAW_CONTACT_ID, id);
				v.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
				v.put(StructuredName.GIVEN_NAME, name);
				values.add(v);
			}
			
			String number = contact.getNumber();
			if (number != null && number != "null") {
				for (String num : number.split(";")) {
					String[] nt = num.split(":");
					if (nt.length == 2) {
						ContentValues v = new ContentValues();
						v.put(Data.RAW_CONTACT_ID, id);
						v.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
						v.put(Phone.TYPE, nt[0]);
						v.put(Phone.NUMBER, nt[1]);
						values.add(v);
					}
				}
			}
			
			String email = contact.getEmail();
			if (email != null && email != "null") {
				for (String em : email.split(";")) {
					String[] et = em.split(":");
					if (et.length == 2) {
						ContentValues v = new ContentValues();
						v.put(Data.RAW_CONTACT_ID, id);
						v.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
						v.put(Email.TYPE, et[0]);
						v.put(Email.ADDRESS, et[1]);
						values.add(v);
					}
				}
			}
		}
		
		c.bulkInsert(ContactsContract.Data.CONTENT_URI, values.toArray(new ContentValues[]{}));
		return true;
	}
	
	private ArrayList<ContactInfo> loadContacts(String dir) {
		ArrayList<ContactInfo> info = new ArrayList<ContactInfo>();
		File contacts = new File(dir + "Contacts/");
		if (!contacts.exists()) {
			mHandler.sendEmptyMessage(MSG_ERROR_DIR);
			return null;
		}
		for (File f : contacts.listFiles()) {
			if (f.isDirectory()) {
				FileInputStream ipt;
				try {
					ipt = new FileInputStream(f.getPath() + "/data");
				} catch (FileNotFoundException e1) {
					mHandler.sendEmptyMessage(MSG_ERROR_DIR);
					return null;
				}
				
				byte[] buffer = new byte[2048];
				try {
					ipt.read(buffer);
					ipt.close();
				} catch (IOException e2) {
					mHandler.sendEmptyMessage(MSG_ERROR_DIR);
					return null;
				}
				
				String[] items = new String(buffer).split("\n");
				
				ContactInfo item = new ContactInfo();
				item.setName(items[0]);
				item.setRawId(Integer.parseInt(items[1]));
				item.setEmail(items[2]);
				item.setNumber(items[3]);
				
				info.add(item);
			}
		}
		return info;
	}
}
