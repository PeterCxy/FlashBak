package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.content.Context;
import android.content.pm.*;
import android.util.Base64;

import java.io.File;
import java.util.*;

import us.shandian.flashbak.R;
import us.shandian.flashbak.adapter.ApplicationAdapter;
import us.shandian.flashbak.helper.BackupGenerator;

public class NewBackupActivity extends Activity
{

    protected Context mContext;

    protected EditText mBackupName;
	protected ListView mAppList;
	protected ProgressBar mWait;
	protected ProgressDialog mProgress;
	
	protected ApplicationAdapter mAdapter;
	
	protected NewBackupUiHandler mHandler = new NewBackupUiHandler();
	
	protected boolean mAppLoaded = false;
	
	protected ArrayList<ApplicationInfo> mAppArrayList;
	protected ArrayList<ApplicationInfo> mCheckedAppList;
	
	protected static final int MSG_APP_LIST_OK = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_newbackup);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mContext = (Context) this;

		mBackupName = (EditText) findViewById(R.id.newbackup_name);
		mAppList = (ListView) findViewById(R.id.newbackup_list);
		mWait = (ProgressBar) findViewById(R.id.newbackup_wait);
		
		initDisplay();

	}
	
	protected void initDisplay() {
		mAppList.setVisibility(View.GONE);
		mWait.setVisibility(View.VISIBLE);

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
					mAppArrayList = checkForLaunchIntent (getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA));
					mHandler.sendEmptyMessage(MSG_APP_LIST_OK);
				}
			}).start();
	}

	private ArrayList<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
		ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
		for (ApplicationInfo info : list) {
			try {
				if (null != getPackageManager().getLaunchIntentForPackage(info.packageName) 
					&& !info.packageName.equals(this.getApplicationInfo().packageName) 
					&& (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
					applist.add(info);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return applist;
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.newbackup, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;
		
		switch (item.getItemId()) {
			case android.R.id.home: {
				finish();
				ret = true;
				break;
			}
			case R.id.invert_select: {
				if (mAppLoaded) {
				    mAdapter.invertSeletion();
				}
				break;
			}
			case R.id.confirm_backup: {
				if (mAppLoaded) {
					mCheckedAppList = mAdapter.getCheckedItems();
					mProgress = new ProgressDialog(mContext);
					mProgress.setCancelable(false);
					mProgress.setTitle(R.string.progress_title);
					mProgress.setMessage(mContext.getResources().getString(R.string.progress_message));
					mProgress.show();
					startThread();
				}
				break;
			}
		}
		
		return ret;
	}
	
	protected void startThread() {
		new Thread(new BackupGenerator(mCheckedAppList, mBackupName.getText().toString(), mHandler)).start();
	}
	
	private class NewBackupUiHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_APP_LIST_OK: {
					mAppLoaded = true;
					mAdapter = new ApplicationAdapter(mContext, R.layout.item_listview_newbackup, mAppArrayList);
					mAppList.setAdapter(mAdapter);
					mAppList.setVisibility(View.VISIBLE);
					mWait.setVisibility(View.GONE);
					break;
				}
				case BackupGenerator.MSG_PROGRESS_CHANGE: {
					mProgress.setMessage(mContext.getResources().getString(R.string.progress_message) + "(" + msg.obj.toString() + "/" + mCheckedAppList.size() + ")");
					break;
				}
				case BackupGenerator.MSG_ERROR_SU: {
					mProgress.dismiss();
					AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
					dialog.setTitle(R.string.err_title);
					dialog.setMessage(R.string.err_su);
					dialog.create().show();
					break;
				}
				case BackupGenerator.MSG_ERROR_DIR: {
					mProgress.dismiss();
					AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
						dialog.setTitle(R.string.err_title);
					dialog.setMessage(R.string.err_dir);
					dialog.create().show();
					break;
				}
				case BackupGenerator.MSG_ERROR_SHELL: {
					mProgress.dismiss();
					AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
						dialog.setTitle(R.string.err_title);
					dialog.setMessage(R.string.err_shell);
					dialog.create().show();
					break;
				}
				case BackupGenerator.MSG_GENERATE_SUCCESS: {
					mProgress.dismiss();
					finish();
				}
			}
		}
	}
}
