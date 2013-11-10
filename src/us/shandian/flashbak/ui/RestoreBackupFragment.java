package us.shandian.flashbak.ui;

import android.os.*;
import android.util.*;
import android.app.*;
import android.widget.*;
import android.content.*;
import android.view.*;

import java.io.File;

import us.shandian.flashbak.ui.NewBackupFragment;
import us.shandian.flashbak.helper.BackupLoader;
import us.shandian.flashbak.helper.BackupRestorer;
import us.shandian.flashbak.R;

public class RestoreBackupFragment extends NewBackupFragment
{

    private static final int MENU_DELETE_BACKUP = R.id.confirm_backup + 1;
	
	private BackupLoader mBackups;
	private String mName = "";
	
	@Override
	protected void initDisplay() {
		Bundle extras = getArguments();
		mBackups = extras.getParcelable("loader");
		mName = extras.getString("name");
		
		mBackupName.setText(mName);
		mBackupName.setEnabled(false);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				mAppArrayList = mBackups.getInfo(mName, mContext);
				mHandler.sendEmptyMessage(MSG_APP_LIST_OK);
			}
		}).start();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		mMenu.add(0, MENU_DELETE_BACKUP, 0, "").setIcon(R.drawable.ic_action_discard).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setVisible(false);
	}
	
	@Override
	public void pause() {
		super.pause();
		mMenu.findItem(MENU_DELETE_BACKUP).setVisible(false);
	}
	
	@Override
	public void resume() {
		super.resume();
		mMenu.findItem(MENU_DELETE_BACKUP).setVisible(true);
		((Activity) mContext).setTitle(R.string.title_restore_backup);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;
		
		switch (item.getItemId()) {
			case MENU_DELETE_BACKUP: {
				mBackups.delete(mName);
				finish();
				ret = true;
				break;
			}
			default: {
				ret = super.onOptionsItemSelected(item);
			}
		}
		
		return ret;
	}
	
	@Override
	protected void startThread() {
		new Thread(new BackupRestorer(mCheckedAppList, mBackupName.getText().toString(), mHandler)).start();
	}
	
	public static RestoreBackupFragment newInstance(Bundle bundle) {
		RestoreBackupFragment ret = new RestoreBackupFragment();
		ret.setArguments(bundle);
		return ret;
	}
	
	
}
