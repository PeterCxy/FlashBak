package us.shandian.flashbak.adapter;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import us.shandian.flashbak.R;
import android.widget.*;

public class ApplicationAdapter extends ArrayAdapter<ApplicationInfo>
{
	private List<ApplicationInfo> mAppsList = null;
	private Context mContext;
	private PackageManager mPackageManager;
	
	public ApplicationAdapter(Context context, int textViewResourceId,
	                          List<ApplicationInfo> appsList) {
		super(context, textViewResourceId, appsList);
		this.mContext = context;
		this.mAppsList = appsList;
		mPackageManager = mContext.getPackageManager();
	}
	
	@Override
	public int getCount() {
		return ((null != mAppsList) ? mAppsList.size() : 0);
	}
	
	@Override
	public ApplicationInfo getItem(int position) {
		return ((null != mAppsList) ? mAppsList.get(position) : null);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (null == view) {
			LayoutInflater layoutInflater = (LayoutInflater) mContext
				                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    view = layoutInflater.inflate(R.layout.item_listview_newbackup, null);
		}
		
		ApplicationInfo data = mAppsList.get(position);
		if (null != data) {
		    TextView appName = (TextView) view.findViewById(R.id.newbackup_app_name);
			ImageView iconview = (ImageView) view.findViewById(R.id.newbackup_app_icon);
			CheckBox checkBox = (CheckBox) view.findViewById(R.id.newbackup_checkbox);
			
		    checkBox.setChecked(true);
			appName.setText(data.loadLabel(mPackageManager));
			iconview.setImageDrawable(data.loadIcon(mPackageManager));
		}
		return view;
	}
}
