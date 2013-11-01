package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.Context;

import java.lang.reflect.Field;

import us.shandian.flashbak.helper.BackupLoader;
import us.shandian.flashbak.R;

public class MainBackupListActivity extends Activity
{

    private BackupLoader mBackups;

	private Context mContext;
	private MainUiHandler mHandler = new MainUiHandler();

	private ListView mBackupList;
	private ProgressBar mWait;
	private TextView mNoBackups;
	
	private final static int MSG_NO_BACKUPS = 0;
	private final static int MSG_SHOW_LIST = 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backuplist);
		mContext = (Context) this;
		forceShowOverflowMenu();
		
		mBackupList = (ListView) findViewById(R.id.backup_list);
		mWait = (ProgressBar) findViewById(R.id.wait_for_list_load);
		mNoBackups = (TextView) findViewById(R.id.no_backups);
		
		mNoBackups.setVisibility(View.GONE);
		mWait.setVisibility(View.VISIBLE);
		mBackupList.setVisibility(View.GONE);
		
		mHandler.postDelayed(new Runnable() {
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
		}, 1000);
    }

	/* Credit to: qii */
	private void forceShowOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ignored) {
			// Do something
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater(); 
		inflater.inflate(R.menu.backuplist, menu);
		return true;
	}
	
	private class MainUiHandler extends Handler {
		public void handleMessage(Message msg) {
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
					mBackupList.setAdapter(new SimpleAdapter(mContext, mBackups.getAll(), R.layout.item_listview_backup, 
						                   new String[] {"name", "date", "num"}, 
										   new int[] {R.id.backupitem_name, R.id.backupitem_date, R.id.backupitem_num}));
					break;
				}
			}
		}
	}
}
