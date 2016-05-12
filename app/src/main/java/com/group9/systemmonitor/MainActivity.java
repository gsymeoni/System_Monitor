package com.group9.systemmonitor;

import android.Manifest;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.KILL_BACKGROUND_PROCESSES;


public class MainActivity extends AppCompatActivity
{
    // Vars for listadapter
    ListView lvProcList;
    String[][] procListFull;
    Integer[] procID;

    // Vars for checking if it is app's first run ( used in isFirstRun() )
    private static final String PREF_IS_FIRST_RUN = "firstRun";
    private SharedPreferences prefs;

    // Vars for permissions
    private static final int RESULT_PERMS_INITIAL = 1339;

    // Provides fragments for each of the sections
    private SectionsPagerAdapter mSectionsPagerAdapter;

    //The {@link ViewPager} that will host the section contents.
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Uncomment here and at layout to enable toolbar
        ///Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ///setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        // Refresh process list on click
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Snackbar.make(view, "Refreshing process list...", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();



                // Populate Process list after refresh button is clicked TODO doesnt kill apps
                refreshProcList();

//                lvProcList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        //do stuff
//                        killProc(procListFull[position][0], procListFull[position][1],procID[position]); //refreshes list as well using refreshProcList();
//                    }
//                });
            }
        });



        // check for perms on first run
        if (isFirstRun() && useRuntimePermissions())
        {

            requestPermissions(PERMS_KILL_PROC, RESULT_PERMS_INITIAL);

        }



    }

    // Method for refreshing Process List
    void  refreshProcList ()
    {
        // For gettting the application label using the selected packages as shown below
        final PackageManager pm = getApplicationContext().getPackageManager();


        new AsyncTask<Void, Void, List<ProcessManager.Process>>() {

            long startTime;
            int howmanyprocs;

            @Override
            protected List<ProcessManager.Process> doInBackground(Void... params) {
                startTime = System.currentTimeMillis();
                return ProcessManager.getRunningApps();
            }

            @Override
            protected void onPostExecute(List<ProcessManager.Process> processes) {
                StringBuilder sb = new StringBuilder();
//                sb.append("Number of apps: ").append(processes.size()).append("\n");
                sb.append("Execution time: ").append(System.currentTimeMillis() - startTime).append("ms\n");
                sb.append("Root Enabled: ").append(RootUtil.isDeviceRooted()).append("\n");
//                sb.append("Running apps:\n");
//
//                for (ProcessManager.Process process : processes) {
//                    sb.append('\n').append(process.name);
//                }
                new AlertDialog.Builder(MainActivity.this).setMessage(sb.toString()).show();

                howmanyprocs = processes.size();

                String[] procList = new String[howmanyprocs];
                procID = new Integer[howmanyprocs];
                procListFull = new String[howmanyprocs][2];
                Drawable[] procIcons = new Drawable[howmanyprocs];

                TextView txtNumProc = (TextView) findViewById(R.id.no_of_procs_number);
                txtNumProc.setText(String.valueOf(howmanyprocs));
                for (int i = 0; i<howmanyprocs; i++){

                    procList[i]=processes.get(i).name;
                    procID[i]=processes.get(i).pid;

                    ApplicationInfo ai;
                    try {
                        ai = pm.getApplicationInfo( processes.get(i).getPackageName(), 0);
                    } catch (final PackageManager.NameNotFoundException e) {
                        ai = null;
                    }

                    final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                    procListFull[i][0]=applicationName;
                    procListFull[i][1]=processes.get(i).name;


                    //get app icons, if it cant then get default icon
                    try
                    {
                        procIcons[i]=getPackageManager().getApplicationIcon(processes.get(i).name); //getPackageManager().getApplicationIcon(procInfos.get(i).processName.toString());
                    }
                    catch (PackageManager.NameNotFoundException ne)
                    {
                        procIcons[i]= ContextCompat.getDrawable(MainActivity.this,R.mipmap.ic_process_default); //getResources().getDrawable(R.drawable.processIcon); //check
                    }
                }

                // Populate list view with all details (app name, package name, icons)
                lvProcList = (ListView) findViewById(R.id.lv_proc);
                ArrayAdapter procAdapter = new MyListAdapter(procListFull,procIcons);
                lvProcList.setAdapter(procAdapter);

            }



        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);



//        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
//        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
//        String[] procList = new String[procInfos.size()];
//        procID = new Integer[procInfos.size()];
//        procListFull = new String[procInfos.size()][2];
//        Drawable[] procIcons = new Drawable[procInfos.size()];
//        String processLabel;
//        TextView txtNumProc = (TextView) findViewById(R.id.no_of_procs_number);
//        txtNumProc.setText(""+procInfos.size()); //"" to make the whole thing string
//        //Converting to process list to string of arrays
//        for (int i = 0; i<procInfos.size(); i++){
//            procList[i]=procInfos.get(i).processName;
//            procID[i]=procInfos.get(i).pid;
//            //CharSequence c = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(procInfos.get(i).processName,PackageManager.GET_META_DATA));
//            try
//            {
//                CharSequence c = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(procInfos.get(i).processName, PackageManager.GET_META_DATA));
//                processLabel = c.toString();
//            }
//            catch (PackageManager.NameNotFoundException ne)
//            {
//                processLabel = procInfos.get(i).processName;
//            }
//            procListFull[i][0]=processLabel;
//            procListFull[i][1]=procInfos.get(i).processName;
//            //get app icons, if it cant then get default icon
//            try
//            {
//                procIcons[i]=getPackageManager().getApplicationIcon(procInfos.get(i).processName); //getPackageManager().getApplicationIcon(procInfos.get(i).processName.toString());
//            }
//            catch (PackageManager.NameNotFoundException ne)
//            {
//                procIcons[i]= ContextCompat.getDrawable(this,R.mipmap.ic_process_default); //getResources().getDrawable(R.drawable.processIcon); //check
//            }
//        }
//        lvProcList = (ListView) findViewById(R.id.lv_proc);
//        //ArrayAdapter<String> prcoAdapter = new ArrayAdapter<String>(this,android.R.layout .simple_expandable_list_item_1 ,procList);
//        //lvProcList.setAdapter(prcoAdapter);
//        ArrayAdapter procAdapter = new MyListAdapter(procListFull,procIcons);
//        lvProcList.setAdapter(procAdapter);
    }

    // Method for killing Processes
    void killProc(final String procLbl , final String pkgName, final Integer procID)
    {
        final ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        //Msgbox
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        if (RootUtil.isDeviceRooted()) {
                            try
                            {
                                Process suProcess = Runtime.getRuntime().exec("su");
                                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                                os.writeBytes("adb shell" + "\n");

                                os.flush();

                                os.writeBytes("am force-stop " + pkgName + "\n");

                                os.flush();
                                os.close();
                                suProcess.waitFor();
                                suProcess.destroy();
                                Toast.makeText(getBaseContext(), procLbl + " killed with root.", Toast.LENGTH_LONG).show();
                            }catch(IOException e){
                                Toast.makeText(getBaseContext(), "Failed with root:(IO). Trying normal procedure...", Toast.LENGTH_LONG).show();
                                activityManager.killBackgroundProcesses(pkgName);
                                android.os.Process.killProcess(procID);
                                Toast.makeText(getBaseContext(), procLbl + " killed.", Toast.LENGTH_LONG).show();
                            }catch(InterruptedException e){
                                Toast.makeText(getBaseContext(), "Failed with root:(su). Trying normal procedure...", Toast.LENGTH_LONG).show();
                                activityManager.killBackgroundProcesses(pkgName);
                                android.os.Process.killProcess(procID);
                                Toast.makeText(getBaseContext(), procLbl + " killed.", Toast.LENGTH_LONG).show();
                            }

                            //refreshProcList();
                        } else {
                            activityManager.killBackgroundProcesses(pkgName);
                            android.os.Process.killProcess(procID);
                            Toast.makeText(getBaseContext(), procLbl + " killed.", Toast.LENGTH_LONG).show();
                            //refreshProcList();
                        }
                        refreshProcList();
                        dialog.dismiss();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        //refreshProcList();
                        dialog.dismiss();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this); //maybe it doesnt refresh when u have it outside the kill func because of "this" ? returns to the wrong view?
        builder.setMessage("Are you sure you want to kill " + procLbl + "?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
        //end of messagebox
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //return TaskManagerFragment.newInstance(position + 1);



            switch (position)
            {
                case 0:
                    return PlaceholderFragment.newInstance(position + 1);

                case 1:
                    return TaskManagerFragment.newInstance(position + 2);


                case 2:

                    return PlaceholderFragment.newInstance(position + 3);

                default:
                    return null;
            }
        }

        @Override
        public int getCount()
        {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            switch (position)
            {
                case 0:
                    return "SYSTEM INFORMATION";
                case 1:
                    return "PROCESS MANAGER";
                case 2:
                    return "BATTERY MONITOR";
            }
            return null;
        }
    }


    // Method to check if app has run before
    private boolean isFirstRun()
    {
        boolean result = prefs.getBoolean(PREF_IS_FIRST_RUN, true);

        if (result)
        {
            prefs.edit().putBoolean(PREF_IS_FIRST_RUN, false).apply();
        }

        return (result);
    }

    // Permission Grouping
    private static final String[] PERMS_KILL_PROC =
            {
                    KILL_BACKGROUND_PROCESSES,
                    CAMERA
            };


    // Checks if build = 23 else returns false (checks if going to use runtime perms). Backwards compatibility measure.
    private boolean useRuntimePermissions()
    {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    //list adapter class
    private class MyListAdapter extends ArrayAdapter
    {
        String[][] items;
        Drawable[] itemIcon;
        public MyListAdapter(String[][] itemArray, Drawable[] icon)
        {
            super(MainActivity.this, R.layout.fragment_task_manager_process, itemArray);
            // ^custom list item layout
            this.items=itemArray;
            this.itemIcon=icon;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //position in list array in args

            //Make sure we have a view to work with
            View procItemView = convertView;
            if (procItemView == null)
            {
                procItemView=getLayoutInflater().inflate(R.layout.fragment_task_manager_process, parent , false);
            }
            ImageView procIcon = (ImageView) procItemView.findViewById(R.id.procIcon);
            procIcon.setImageDrawable(itemIcon[position]);

            TextView procClass = (TextView) procItemView.findViewById(R.id.txtLItem); //Main item txtbox
            procClass.setText(items[position][0]);
            TextView procPkg = (TextView) procItemView.findViewById(R.id.txtSubItem); //Main item txtbox
            procPkg.setText(items[position][1]);
            return procItemView;


        }
    }

}


