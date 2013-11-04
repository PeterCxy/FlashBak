package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.content.Context;
import android.content.pm.*;

import java.io.File;
import java.util.*;

import us.shandian.flashbak.R;
import us.shandian.flashbak.adapter.ApplicationAdapter;

public class NewBackupActivity extends Activity
{

    private Context mContext;

    private EditText mBackupName;
	private ListView mAppList;
	private ProgressBar mWait;
	
	private ApplicationAdapter mAdapter;
	
	private NewBackupUiHandler mHandler = new NewBackupUiHandler();
	
	private boolean mAppLoaded = false;
	
	private ArrayList<ApplicationInfo> mAppArrayList;
	
	private static final int MSG_APP_LIST_OK = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_newbackup);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mContext = (Context) this;

		mBackupName = (EditText) findViewById(R.id.newbackup_name);
		mAppList = (ListView) findViewById(R.id.newbackup_list);
		mWait = (ProgressBar) findViewById(R.id.newbackup_wait);
		
		mAppList.setVisibility(View.GONE);
		mWait.setVisibility(View.VISIBLE);

		int i = 1;
		String defaultName = getResources().getString(R.string.default_backup_name);
		while (true) {
			File f = new File(Environment.getExternalStorageDirectory() + "/FlashBak/" + defaultName + i + "/");
			if (!f.exists()) {
				defaultName = defaultName + i;
				break;
			} else {
				i++;
			}
		}
		mBackupName.setText(defaultName);
		
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
		        mAppArrayList = checkForLaunchIntent (getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA));
				mHandler.sendEmptyMessage(MSG_APP_LIST_OK);
			}
		}, 1000);
		

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
		}
		
		return ret;
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
				}
			}
		}
	}
}
