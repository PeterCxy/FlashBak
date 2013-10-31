package us.shandian.flashbak.ui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.lang.reflect.Field;
import us.shandian.flashbak.R;

public class MainBackupListActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backuplist);
		forceShowOverflowMenu();
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
}
