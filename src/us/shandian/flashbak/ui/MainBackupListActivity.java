package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.AdapterView.*;
import android.content.Context;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.support.v4.widget.SlidingPaneLayout;

import java.util.Map;
import java.util.ArrayList;

import us.shandian.flashbak.helper.BackupLoader;
import us.shandian.flashbak.ui.NewBackupFragment;
import us.shandian.flashbak.ui.RestoreBackupFragment;
import us.shandian.flashbak.ui.MergeBackupFragment;
import us.shandian.flashbak.ui.widget.FlingerListView;
import us.shandian.flashbak.ui.widget.FlingerListView.OnItemFlingListener;
import us.shandian.flashbak.util.CMDProcessor;
import us.shandian.flashbak.R;

public class MainBackupListActivity extends Activity
{

    private BackupLoader mBackups;

	private Context mContext;
	private FragmentManager mFragments;
	private MainUiHandler mHandler = new MainUiHandler();
	private Thread mThread = new Thread(new MainUiRunnable());
	
	private boolean mThreadRunning = false;

	private FlingerListView mBackupList;
	private ProgressBar mWait;
	private TextView mNoBackups;
	private SlidingPaneLayout mPane;
	private LinearLayout mLayout;
	private Menu mMenu;
	
	private SimpleAdapter mAdapter;
	
	private ListView.MultiChoiceModeListener mCallback = new ListView.MultiChoiceModeListener() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mActionMode = mode;
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.context, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
				case R.id.context_delete: {
					SparseBooleanArray items = mBackupList.getCheckedItemPositions();
					for (int i = 0; i < items.size(); i++) {
						if (items.valueAt(i)) {
							// Checked
							mBackups.delete(((TextView) mBackupList.getChildAt(items.keyAt(i)).findViewById(R.id.backupitem_name)).getText().toString());
						}
					}
					mAdapter.notifyDataSetChanged();
					break;
				}
				case R.id.context_merge: {
					SparseBooleanArray items = mBackupList.getCheckedItemPositions();
					StringBuilder str = new StringBuilder();
					for (int i = 0; i < items.size(); i++) {
						if (items.valueAt(i)) {
							// Checked
							str.append(((TextView) ((ViewGroup) mBackupList.getChildAt(items.keyAt(i))).findViewById(R.id.backupitem_name)).getText().toString());
							str.append("\n");
						}
					}
					Bundle bundle = new Bundle();
					bundle.putString("needs", str.toString());
					bundle.putParcelable("loader", mBackups);
					mFragments.beginTransaction().replace(R.id.container, MergeBackupFragment.newInstance(bundle)).commit();
					mPane.closePane();
					break;
				}
			}
			mActionMode.finish();
			mActionMode = null;
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			SparseBooleanArray items = mBackupList.getCheckedItemPositions();
			for (int i = 0; i < items.size(); i++) {
				if (items.valueAt(i)) {
					View item = mBackupList.getChildAt(items.keyAt(i));
					View num = item.findViewById(R.id.backupitem_num);
					item.setBackgroundColor(0);
					num.getBackground().setAlpha(255);
					items.put(items.keyAt(i), false);
				}
			}
			mActionMode = null;
		}
		
		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
		{
			View item = mBackupList.getChildAt(position);
			View num = item.findViewById(R.id.backupitem_num);
			if (checked) {
				item.setBackgroundResource(android.R.color.holo_orange_light);
				num.getBackground().setAlpha(125);
			} else {
				item.setBackgroundColor(0);
				num.getBackground().setAlpha(255);
			}
		}
	};
	
	private ActionMode mActionMode;
	
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
		mBackupList = (FlingerListView) findViewById(R.id.backup_list);
		mWait = (ProgressBar) findViewById(R.id.wait_for_list_load);
		mNoBackups = (TextView) findViewById(R.id.no_backups);
		mLayout = (LinearLayout) findViewById(R.id.main_layout);
		mPane = (SlidingPaneLayout) findViewById(R.id.mainPane);
		
		mBackupList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mBackupList.setMultiChoiceModeListener(mCallback);
		mBackupList.setOnItemFlingListener(new OnItemFlingListener() {
			@Override
			public boolean onItemFling(View view, int position, long id, ViewGroup parent) {
				if (mActionMode == null) {
					mBackupList.setChoiceMode(ListView.CHOICE_MODE_NONE);
					return true;
				} else {
					return false;
				}
			}
			
			@Override
			public boolean onItemFlingEnd(View view, int position, long id, ViewGroup parent) {
				if (mActionMode == null) {
					return true;
				} else {
					return false;
				}
			}
			
			@Override
			public void onItemFlingCancel(View view, int position, long id, ViewGroup parent) {
				if (mActionMode == null) {
					mBackupList.runAfterAnimation(new Runnable() {
						@Override
						public void run() {
							mBackupList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
						}
					});
				}
			}
			
			@Override
			public void onItemFlingOut(int position) {
				if (mActionMode == null) {
					mBackups.deleteById(position);
					if (mBackups.size() == 0) {
						mNoBackups.setVisibility(View.VISIBLE);
						mWait.setVisibility(View.GONE);
						mBackupList.setVisibility(View.GONE);
					}
					mBackupList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		
		mPane.setShadowResource(R.drawable.panel_shadow);
		mPane.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {
			@Override
			public void onPanelOpened(View view) {
				NewBackupFragment frag = (NewBackupFragment) mFragments.findFragmentById(R.id.container);
				if (frag != null) {
					frag.pause();
				}
				mMenu.findItem(R.id.new_backup).setVisible(true);
			}
			
			@Override
			public void onPanelClosed(View view) {
				if (mActionMode != null) {
					mActionMode.finish();
					mActionMode = null;
				}
				NewBackupFragment frag = (NewBackupFragment) mFragments.findFragmentById(R.id.container);
				if (frag != null) {
					frag.resume();
				}
				mMenu.findItem(R.id.new_backup).setVisible(false);
			}
			
			@Override
			public void onPanelSlide(View v, float f) {
				mLayout.setTranslationX(- (1 - f) * 108);
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
							if (mActionMode == null && mBackupList.getChoiceMode() != ListView.CHOICE_MODE_NONE) {
								Map<String, Object> map = (Map<String, Object>) mAdapter.getItem(arg2);
								String name = (String) map.get("name");
								Bundle bundle = new Bundle();
								bundle.putString("name", name);
								bundle.putParcelable("loader", mBackups);
								mFragments.beginTransaction().replace(R.id.container, RestoreBackupFragment.newInstance(bundle)).commit();
								mPane.closePane();
							}
						}
					});
					/* mBackupList.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							if (mActionMode != null) {
								return false;
							}
							
							mActionMode = MainBackupListActivity.this.startActionMode(mCallback);
							mBackupList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
							 mBackupList.setMultiChoiceModeListener(mCallback);
							mBackupList.setItemChecked(arg2, true);
							arg1.findViewById(R.id.backupitem_num).getBackground().setAlpha(125);
							
							return true;
						}
					});*/
					break;
				}
			}
		}
	}
	
	private class MainUiRunnable implements Runnable {
		@Override
		public void run() {
			CMDProcessor.exportBusybox(mContext);
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
