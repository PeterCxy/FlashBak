package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.AdapterView.*;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.SlidingPaneLayout;

import java.util.Map;
import java.util.ArrayList;

import us.shandian.flashbak.helper.BackupLoader;
import us.shandian.flashbak.ui.NewBackupFragment;
import us.shandian.flashbak.ui.RestoreBackupFragment;
import us.shandian.flashbak.ui.widget.FlingerListView;
import us.shandian.flashbak.ui.widget.FlingerListView.*;
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
	private ArrayList<View> mSelectedViews = new ArrayList<View>();
	
	private SimpleAdapter mAdapter;
	
	private ActionMode mActionMode;
	private ActionMode.Callback mCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
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
					if (mSelectedViews.size() > 0) {
						for (View v : mSelectedViews) {
							mBackups.delete(((TextView)v.findViewById(R.id.backupitem_name)).getText().toString());
							ScaleAnimation anim = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
							anim.setDuration(200);
							v.clearAnimation();
							v.setAnimation(anim);
							anim.startNow();
						}
					}
					mBackupList.postDelayed(new Runnable() {
						@Override
						public void run() {
							mAdapter.notifyDataSetChanged();
						}
					}, 150);
					break;
				}
			}
			mActionMode.finish();
			mActionMode = null;
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			for (View v : mSelectedViews) {
				v.findViewById(R.id.backupitem_num).getBackground().setAlpha(255);
			}
			mSelectedViews.clear();
		}
	};
	
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
		
		mBackupList.setOnItemFlingerListener(new OnItemFlingerListener() {
			@Override
			public boolean onItemFlingerStart(AdapterView<?> parent, View view, int position, long id) {
				if (mActionMode == null) return true;
				else return false;
			}
			
			@Override
			public void onItemFlingerEnd(OnItemFlingerResponder responder, AdapterView<?> parent, View view, int position, long id) {
				mBackups.deleteById(position);
				view.clearAnimation();
				ScaleAnimation anim = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				anim.setDuration(200);
				view.setAnimation(anim);
				anim.startNow();
				mBackupList.postDelayed(new Runnable() {
					@Override
					public void run() {
						mAdapter.notifyDataSetChanged();
						if (mBackups.getAll().size() == 0) {
							mNoBackups.setVisibility(View.VISIBLE);
							mWait.setVisibility(View.GONE);
							mBackupList.setVisibility(View.GONE);
						}
					}
				}, 200);
				responder.accept();
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
							if (mActionMode == null) {
								Map<String, Object> map = (Map<String, Object>) mAdapter.getItem(arg2);
								String name = (String) map.get("name");
								Bundle bundle = new Bundle();
								bundle.putString("name", name);
								bundle.putParcelable("loader", mBackups);
								mFragments.beginTransaction().replace(R.id.container, RestoreBackupFragment.newInstance(bundle)).commit();
								mPane.closePane();
							} else {
								for (View v : mSelectedViews) {
									if (v == arg1) {
										arg1.findViewById(R.id.backupitem_num).getBackground().setAlpha(255);
										mSelectedViews.remove(arg1);
										if (mSelectedViews.size() == 0) {
											mActionMode.finish();
											mActionMode = null;
										}
										return;
									}
								}
								arg1.findViewById(R.id.backupitem_num).getBackground().setAlpha(125);
								mSelectedViews.add(arg1);
							}
						}
					});
					mBackupList.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							if (mActionMode != null) {
								return false;
							}
							
							mActionMode = MainBackupListActivity.this.startActionMode(mCallback);
							arg1.setSelected(true);
							arg1.findViewById(R.id.backupitem_num).getBackground().setAlpha(125);
							mSelectedViews.add(arg1);
							return true;
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
