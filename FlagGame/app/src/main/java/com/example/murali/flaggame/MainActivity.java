package com.example.murali.flaggame;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String CHOICES = "pre_numberOfChoices";
    public  static  final String REGIONS = "pref_regionsToInclude";
    public  static  final String example= "nextChange";
    private boolean phoneDevice = true;
    private boolean preferanceChanged =true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set default values for shared preference file
        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);
        //register listener for shared preference changes
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //determine the screen size
            int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;

        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize ==Configuration.SCREENLAYOUT_SIZE_XLARGE)
                phoneDevice=false;
        if(phoneDevice)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if(preferanceChanged) {
            MainActivityFragment quizFragment = (MainActivityFragment)  getFragmentManager().findFragmentById(R.id.quizFragment);

            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferanceChanged=false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.


        int orientation= getResources().getConfiguration().orientation;

        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        else

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent preferenceIntent = new Intent(this,SettingsActivity.class);
        startActivity(preferenceIntent);

        return super.onOptionsItemSelected(item);
    }

    //Listener for changes to the app's sharedpreferences

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener=new SharedPreferences.OnSharedPreferenceChangeListener() {

        //called when the user changes the app's preferences

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            preferanceChanged =true;// user changed app setting

            MainActivityFragment quizFragment = (MainActivityFragment) getFragmentManager().findFragmentById(R.id.quizFragment);

            if(key.equals(CHOICES)) {

                quizFragment.updateGuessRows(sharedPreferences);
                quizFragment.resetQuiz();
            }
            else if (key.equals(REGIONS)){
                Set<String> regions = sharedPreferences.getStringSet(REGIONS,null);
                 if(regions != null && regions.size()>0){
                     quizFragment.updateRegions(sharedPreferences);
                     quizFragment.resetQuiz();
                 }else {
                     SharedPreferences.Editor editor = sharedPreferences.edit();
                     regions.add(getString(R.string.default_region));
                     editor.putStringSet(REGIONS,regions);
                     editor.apply();

                     Toast.makeText(MainActivity.this,R.string.default_region_message,Toast.LENGTH_LONG).show();
                 }
            }
            Toast.makeText(MainActivity.this,R.string.restarting_quiz,Toast.LENGTH_SHORT).show();
        }
    };
}




