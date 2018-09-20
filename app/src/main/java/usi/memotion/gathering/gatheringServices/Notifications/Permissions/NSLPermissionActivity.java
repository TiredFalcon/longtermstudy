package usi.memotion.gathering.gatheringServices.Notifications.Permissions;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import usi.memotion.MainActivity;
import usi.memotion.R;
import usi.memotion.gathering.gatheringServices.Notifications.Utils.Log;
import usi.memotion.gathering.gatheringServices.Notifications.Utils.Popup;


public class NSLPermissionActivity extends Activity {

	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Drawable background;

		if(Build.VERSION.SDK_INT >= 21)
			background = getResources().getDrawable(R.drawable.info, null);
		else
			background = getResources().getDrawable(R.drawable.info);


		ActionBar actionBar = getActionBar();
		actionBar.setBackgroundDrawable(background);
//		actionBar.setCustomView(R.layout.actionbar_layout);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(true);

//		TextView actionbar_title = (TextView) findViewById(R.id.tvActionBarTitle);
//		actionbar_title.setText(getResources().getString(R.string.title_activity_nsl_permission));

//		ShimmerFrameLayout container = (ShimmerFrameLayout) findViewById(R.id.shimmer_action_bar);
//		container.setBaseAlpha(0.8f);
//		container.setAutoStart(true);
		
//		setContentView(R.layout.activity_nsl_permission);
//
//		if( !(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler) )
//		{
//			Thread.setDefaultUncaughtExceptionHandler( new CustomExceptionHandler(getApplicationContext()) );
//		}
//
//		ShimmerFrameLayout container1 = (ShimmerFrameLayout) findViewById(R.id.shimmer_view_container);
//		container1.setBaseAlpha(0.8f);
//		container1.setAutoStart(true);
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(new Permission(getApplicationContext()).isNSLPermitted())
		{
			new Log().v("Permission granted");
			Intent i = new Intent(this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			finish();
		}
		else
		{
			new Popup().showPopup(NSLPermissionActivity.this, "Permission Required",
					"Provide the permission to keep the app running. "
					+ "\n\n"
					+ "If you have previously given this permission, please reset it by diabling and enabling it again.");
		}
	}

	public void openSettings(View v)
	{
		Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
		startActivityForResult(i, 0);
	}
}
