package usi.memotion;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.aware.Aware;
import com.aware.ESM;
import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import usi.memotion.Reminders.FinalScheduler;
import usi.memotion.UI.fragments.AboutApplicationFragment;
import usi.memotion.UI.fragments.AboutFragment;
import usi.memotion.UI.fragments.DailySurveysFragment;
import usi.memotion.UI.fragments.LectureSurveysFragment;
import usi.memotion.UI.fragments.HomeFragment;
import usi.memotion.UI.fragments.ProfileFragment;
import usi.memotion.UI.views.RegistrationView;
import usi.memotion.gathering.gatheringServices.ApplicationLogs.AppUsageStatisticsFragment;
import usi.memotion.local.database.controllers.LocalStorageController;
import usi.memotion.local.database.controllers.SQLiteController;
import usi.memotion.gathering.GatheringSystem;
import usi.memotion.gathering.SensorType;
import usi.memotion.remote.database.upload.DataUploadService;
import usi.memotion.remote.database.upload.UploadAlarmReceiver;

public class MainActivity extends AppCompatActivity implements  NavigationView.OnNavigationItemSelectedListener {
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ENABLED_USAGE_LISTENERS = "enabled_usage_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private AlertDialog enableNotificationListenerAlertDialog;
    private AlertDialog enableUsageServiceAlertDialog;


    private GatheringSystem gSys;
    private boolean viewIsAtHome;

    protected DrawerLayout drawerLayout;
    private Toolbar toolbar;
    protected NavigationView navigationView;

    Calendar calendar;
    String weekday;
    SimpleDateFormat dayFormat;
    int month;
    int dayOfMonth;
    FinalScheduler scheduler;


    ExpandableRelativeLayout expandableLayout0, expandableLayout1, expandableLayout2,
            expandableLayoutMood, expandableLayoutFatigue, expandableLayoutStress, expandableLayoutProductivity, expandableLayoutSleep;


    private final int PERMISSION_REQUEST_STATUS = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        displayView(R.id.nav_home);

        triggerReminders();

        if(!checkPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.PROCESS_OUTGOING_CALLS,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.INTERNET},
                    PERMISSION_REQUEST_STATUS);

            // If the user did not turn the notification listener service on we prompt him to do so
            if(!isNotificationServiceEnabled()){
                enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
                enableNotificationListenerAlertDialog.show();
            }

            if(!isUsageAccessServiceEnabled()){
                enableUsageServiceAlertDialog = buildUsageStatsManagerAlertDialog();
                enableUsageServiceAlertDialog.show();
            }
        } else {
            initServices(grantedPermissions());
        }

    }

    private void initServices(List<String> grantedPermissions) {
        gSys = new GatheringSystem(getApplicationContext());

        for(SensorType type: SensorType.values()) {
            if(permissionAreGranted(grantedPermissions, type.getPermissions())) {
                gSys.addSensor(type);
                Log.d("MainActivity", "All permissions granted for sensor " + type);
            } else {
                Log.d("MainActivity", "Not all permissions granted for sensor " + type);
            }
        }

        gSys.start();

        startService(new Intent(this, DataUploadService.class));

    }

    private boolean permissionAreGranted(List<String> grantedPermissions, String[] requiredPermissions) {
        for(String permission: requiredPermissions) {
            if(!grantedPermissions.contains(permission)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_REQUEST_STATUS) {
            initServices(convertPermissionResultsToList(permissions, grantResults));
        }
    }

    private List<String> convertPermissionResultsToList(String[] permissions, int[] grantResults) {
        List<String> grantedPermissions = new ArrayList<>();

        for(int i = 0; i < permissions.length; i++) {
            if(grantResults[i] >= 0) {
                grantedPermissions.add(permissions[i]);
            }
        }

        return grantedPermissions;
    }
    private List<String> grantedPermissions() {
        List<String> granted = new ArrayList<String>();
        try {
            PackageInfo pi = getPackageManager().getPackageInfo("usi.memotion", PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted.add(pi.requestedPermissions[i]);
                }
            }
        } catch (Exception e) {
        }
        return granted;
    }
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void expandableButton0(View view) {
        expandableLayout0 = (ExpandableRelativeLayout) findViewById(R.id.expandableLayout0);
        expandableLayout0.toggle(); // toggle expand and collapse
    }

    public void expandableButton1(View view) {
        expandableLayout1 = (ExpandableRelativeLayout) findViewById(R.id.expandableLayout1);
        expandableLayout1.toggle(); // toggle expand and collapse
    }

    public void expandableButton2(View view) {
        expandableLayout2 = (ExpandableRelativeLayout) findViewById(R.id.expandableLayout2);
        expandableLayout2.toggle(); // toggle expand and collapse
    }

    public void expandableButtonMood(View view) {
        expandableLayoutMood = (ExpandableRelativeLayout) findViewById(R.id.expandableLayoutMood);
        expandableLayoutMood.toggle(); // toggle expand and collapse
    }

    public void expandableButtonFatigue(View view) {
        expandableLayoutFatigue = (ExpandableRelativeLayout) findViewById(R.id.expandableLayoutFatigue);
        expandableLayoutFatigue.toggle(); // toggle expand and collapse
    }

    public void expandableButtonSleep(View view) {
        expandableLayoutSleep = (ExpandableRelativeLayout) findViewById(R.id.expandableLayoutSleep);
        expandableLayoutSleep.toggle(); // toggle expand and collapse
    }

    public void expandableButtonStress(View view) {
        expandableLayoutStress = (ExpandableRelativeLayout) findViewById(R.id.expandableLayoutStress);
        expandableLayoutStress.toggle(); // toggle expand and collapse
    }

    public void expandableButtonProductivity(View view) {
        expandableLayoutProductivity = (ExpandableRelativeLayout) findViewById(R.id.expandableLayoutProductivity);
        expandableLayoutProductivity.toggle(); // toggle expand and collapse
    }

    public void displayView(int viewId) {
        android.support.v4.app.Fragment fragment = null;
        String title = getString(R.string.app_name);

        switch (viewId) {
            case R.id.nav_home:
                fragment = new HomeFragment();
                title  = "Welcome";
                viewIsAtHome = true;
                break;
            case R.id.nav_lecture_surveys:
                fragment = new LectureSurveysFragment();
                title  = "Lecture Surveys";
                viewIsAtHome = false;
                break;
            case R.id.nav_general_surveys:
                fragment = new DailySurveysFragment();
                title = "General Surveys";
                viewIsAtHome = false;
                break;

            case R.id.nav_usage_statistics:
                fragment = new AppUsageStatisticsFragment();
                title = "Application Logs";
                viewIsAtHome = false;
                break;

            case R.id.nav_register:
                if(checkAndroidID()){
                    fragment = new ProfileFragment();
                    title = "Account Details";
                }else{
                    fragment = new RegistrationView();
                    title = "Create Account";
                }

                viewIsAtHome = false;
                break;

            case R.id.nav_about_study:
                fragment = new AboutFragment();
                title = "About Study";
                viewIsAtHome = false;
                break;

            case R.id.nav_about_app:
                fragment = new AboutApplicationFragment();
                title = "About Study";
                viewIsAtHome = false;
                break;
        }


        if(fragment != null){
            String menuFragment = getIntent().getStringExtra("fragmentChoice");
            String questionnaireSession = getIntent().getStringExtra("LectureSession");

            Log.v("displayView", "The received extras are: " + menuFragment);

            // If menuFragment is defined, then this activity was launched with a fragment selection
            if (menuFragment != null) {
                if (menuFragment.equals("lectureSurveys")) {
                    Bundle bundle = new Bundle();
                    bundle.putString("LectureSession", questionnaireSession);
                    fragment = new LectureSurveysFragment();
                    fragment.setArguments(bundle);
                }else if(menuFragment.equals("dailySurveys")){
                    fragment = new DailySurveysFragment();
                }
            }
            // Activity was not launched with a menuFragment selected -- continue as if this activity was opened from a launcher (for example)
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();
        }

        // set the toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

    }

    private boolean checkAndroidID() {
        String query = "SELECT * FROM usersTable";

        LocalStorageController localController = SQLiteController.getInstance(getApplicationContext());;

        Cursor records = localController.rawQuery(query, null);
        records.moveToFirst();

//        String query2 = "SELECT * FROM lectureSurvey";
//        Cursor records2 = localController.rawQuery(query2, null);
//        Log.v("MAIN ACTIVITY", "" + records.moveToFirst());


        if (records.getCount() > 0) {
            return true;
        }else{
            return false;
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        if (!viewIsAtHome) {
            displayView(R.id.nav_home);
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        displayView(item.getItemId());
        return true;

    }

    public void triggerReminders(){
        dayFormat = new SimpleDateFormat("EEEE", Locale.US);
        calendar = Calendar.getInstance();
        weekday = dayFormat.format(calendar.getTime());
        scheduler = new FinalScheduler();
        month = calendar.get(Calendar.MONTH);
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);


        //January - 0 - if(month == Calendar.SEPTEMBER && dayOfMonth >= 1 && dayOfMonth <= 30){
        if(month == Calendar.SEPTEMBER || month == Calendar.OCTOBER || month == Calendar.NOVEMBER || month == Calendar.DECEMBER){
            Log.v("Homeee", "Alarms Triggered");
            scheduler.createReminder(getApplicationContext());
            uploadDataEveryday();
        }
    }

    public void uploadDataEveryday(){

        // Retrieve a PendingIntent that will perform a broadcast
        Intent intent = new Intent(getApplicationContext(), UploadAlarmReceiver.class);
        AlarmManager am = (AlarmManager) getSystemService(getApplicationContext().ALARM_SERVICE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 17);
        cal.set(Calendar.MINUTE, 36);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTimeInMillis() > System.currentTimeMillis()) { //if it is more than 19:00 o'clock, trigger it tomorrow
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);

            String time = sdf.format(new Date());
            Log.v("HOMEEE", time + "Upload alarm triggered for today");
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_ONE_SHOT));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
            Log.v("HOMEEE", "Upload Alarm Triggered for next day");
            cal.add(Calendar.DAY_OF_MONTH, 1); //trigger alarm tomorrow
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_ONE_SHOT));
        }
    }


    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if eanbled, false otherwise.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if eanbled, false otherwise.
     */
    private boolean isUsageAccessServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_USAGE_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Build Notification Listener Alert Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the Notification Listener Service on yet.
     * @return An alert dialog which leads to the notification enabling screen
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Permission needed");
        alertDialogBuilder.setMessage("We need the permission to collect data about notifications.");
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

    /**
     * Build UsageStatsManager Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the UsageStatsManager on yet.
     * @return An alert dialog which leads to the notification enabling screen
     */
    private AlertDialog buildUsageStatsManagerAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Permission needed");
        alertDialogBuilder.setMessage("We need the permission to collect data about the phone usage statistics.");
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

}

