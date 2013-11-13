package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.SlidingPaneLayout;

import java.lang.reflect.Field;
import java.util.Map;


import us.shandian.flashbak.helper.BackupLoader;
import us.shandian.flashbak.ui.NewBackupFragment;
import us.shandian.flashbak.ui.RestoreBackupFragment;
import us.shandian.flashbak.R;

public class MainBackupListActivity extends Activity
{

    private BackupLoader mBackups;

	private Context mContext;
	private FragmentManager mFragments;
	private MainUiHandler mHandler = new MainUiHandler();
	private Thread mThread = new Thread(new MainUiRunnable());
	
	private boolean mThreadRunning = false;

	private ListView mBackupList;
	private ProgressBar mWait;
	private TextView mNoBackups;
	private SlidingPaneLayout mPane;
	private LinearLayout mLayout;
	private Menu mMenu;
	
	private SimpleAdapter mAdapter;
	
	public String FlashBakTitle = "";
	
	private final static int MSG_NO_BACKUPS = 0;
	private final static int MSG_SHOW_LIST = 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backuplist);
		mContext = (Context) this;
		String version = "";
		try {
			version = getPackageManager().getPackageInfo(this.getApplicationInfo().packageName, 0).versionName;
		} catch (Exception e) {
			version = "0.00";
		}
		FlashBakTitle = getResources().getString(R.string.app_name) + " " + version;
		setTitle(FlashBakTitle);

		mFragments = getFragmentManager();
		mBackupList = (ListView) findViewById(R.id.backup_list);
		mWait = (ProgressBar) findViewById(R.id.wait_for_list_load);
		mNoBackups = (TextView) findViewById(R.id.no_backups);
		mLayout = (LinearLayout) findViewById(R.id.main_layout);
		mPane = (SlidingPaneLayout) findViewById(R.id.mainPane);
		
		mPane.setShadowResource(R.drawable.panel_shadow);
		mPane.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {
			@Override
			public void onPanelOpened(View view) {
				mLayout.setPadding(0, 0, 0, 0);
				NewBackupFragment frag = (NewBackupFragment) mFragments.findFragmentById(R.id.container);
				if (frag != null) {
					frag.pause();
				}
				mMenu.findItem(R.id.new_backup).setVisible(true);
			}
			
			@Override
			public void onPanelClosed(View view) {
				mLayout.setPadding(0, 0, 0, 0);
				NewBackupFragment frag = (NewBackupFragment) mFragments.findFragmentById(R.id.container);
				if (frag != null) {
					frag.resume();
				}
				mMenu.findItem(R.id.new_backup).setVisible(false);
			}
			
			@Override
			public void onPanelSlide(View v, float f) {
				mLayout.setPadding((int) -((1 - f) * 72), 0, (int) ((1 - f) * 72), 0);
			}
		});
		mPane.openPane();
		
		mNoBackups.setVisibility(View.GONE);
		mWait.setVisibility(View.VISIBLE);
		mBackupList.setVisibility(View.GONE);
		
		mFragments.beginTransaction().replace(R.id.container, new NewBackupFragment()).commit();
		
		mThreadRunning = true;
		mThread.start();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater(); 
		inflater.inflate(R.menu.backuplist, menu);
		mMenu = menu;
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;

		switch (item.getItemId()) {
			case R.id.new_backup: {
				mFragments.beginTransaction().replace(R.id.container, new NewBackupFragment()).commit();
				mPane.closePane();
				ret = true;
				break;
			}
			case android.R.id.home: {
				mPane.openPane();
			}
		}
		
		return ret;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mWait.setVisibility(View.VISIBLE);
		mNoBackups.setVisibility(View.GONE);
		mBackupList.setVisibility(View.GONE);
		if (!mThreadRunning) {
			mThreadRunning = true;
			mThread = new Thread(new MainUiRunnable());
			mThread.start();
		}
	}
	
	@Override
	public void onBackPressed() {
		if (mPane.isOpen()) {
			finish();
		} else {
			mPane.openPane();
		}
	}
	
	public void openPane() {
		mPane.openPane();
	}
	
	private class MainUiHandler extends Handler {
		public void handleMessage(Message msg) {
			mThreadRunning = false;
			switch (msg.what) {
				case MSG_NO_BACKUPS: {
					mNoBackups.setVisibility(View.VISIBLE);
					mWait.setVisibility(View.GONE);
					mBackupList.setVisibility(View.GONE);
					break;
				}
				case MSG_SHOW_LIST: {
					mNoBackups.setVisibility(View.GONE);
					mWait.setVisibility(View.GONE);
					mBackupList.setVisibility(View.VISIBLE);
					mAdapter = new SimpleAdapter(mContext, mBackups.getAll(), R.layout.item_listview_backup, 
						                   new String[] {"name", "date", "num"}, 
										   new int[] {R.id.backupitem_name, R.id.backupitem_date, R.id.backupitem_num});
					mBackupList.setAdapter(mAdapter);
					mBackupList.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							Map<String, Object> map = (Map<String, Object>) mAdapter.getItem(arg2);
							String name = (String) map.get("name");
							Bundle bundle = new Bundle();
							bundle.putString("name", name);
							bundle.putParcelable("loader", mBackups);
							mFragments.beginTransaction().replace(R.id.container, RestoreBackupFragment.newInstance(bundle)).commit();
							mPane.closePane();
						}
					});
					break;
				}
			}
		}
	}
	
	private class MainUiRunnable implements Runnable {
		@Override
		public void run() {
			mBackups = new BackupLoader();
			mBackups.loadBackups();
			if (mBackups.size() == 0) {
				mHandler.sendEmptyMessage(MSG_NO_BACKUPS);
			} else {
				mHandler.sendEmptyMessage(MSG_SHOW_LIST);
			}
		}
	}
}
