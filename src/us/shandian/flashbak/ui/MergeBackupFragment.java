package us.shandian.flashbak.ui;

import android.os.*;
import android.util.*;
import android.app.*;
import android.widget.*;
import android.content.*;
import android.view.*;

import java.util.ArrayList;
import java.io.File;

import us.shandian.flashbak.ui.NewBackupFragment;
import us.shandian.flashbak.helper.BackupLoader;
import us.shandian.flashbak.helper.BackupMerger;
import us.shandian.flashbak.R;

public class MergeBackupFragment extends NewBackupFragment
{
	
	private BackupLoader mBackups;
	private BackupMerger mMerger;
	private String[] mApps;
	
	@Override
	protected void initDisplay() {
		Bundle bundle = getArguments();
		mBackups = (BackupLoader) bundle.getParcelable("loader");
		mApps = bundle.getString("needs").split("\n");
		mMerger = new BackupMerger(mContext, mHandler, mBackups, mApps);
		
		int i = 1;
		String defaultName = getResources().getString(R.string.default_backup_name);
		while (true) {
			File f = new File(Environment.getExternalStorageDirectory() + "/FlashBak/" + Base64.encodeToString((defaultName + i).getBytes(), Base64.NO_WRAP) + "/");
			if (!f.exists()) {
				defaultName = defaultName + i;
				break;
			} else {
				i++;
			}
		}
		mBackupName.setText(defaultName);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				mMerger.load();
				mAppArrayList = mMerger.getAll();
				mHandler.sendEmptyMessage(MSG_APP_LIST_OK);
			}
		}).start();
	}
	
	@Override
	public void resume() {
		super.resume();
		((Activity) mContext).setTitle(R.string.title_merge_backups);
	}
	
	@Override
	protected void startThread() {
		mMerger.setCheckedAppsToMerge(mCheckedAppList);
		mMerger.setBackupName(mBackupName.getText().toString());
		new Thread(mMerger).start();
	}
	
	public static MergeBackupFragment newInstance(Bundle bundle) {
		MergeBackupFragment ret = new MergeBackupFragment();
		ret.setArguments(bundle);
		return ret;
	}
}
