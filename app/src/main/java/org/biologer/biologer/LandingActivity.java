package org.biologer.biologer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.biologer.biologer.model.RetrofitClient;
import org.biologer.biologer.model.UserData;
import org.biologer.biologer.model.network.ObservationTypes;
import org.biologer.biologer.model.network.ObservationTypesResponse;
import org.biologer.biologer.model.network.TaksoniResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Biologer.Landing";

    private DrawerLayout drawer;
    FrameLayout progressBar;
    private String last_updated_taxa;
    private String totalSpeciesOnline;
    private String totalSpeciesDao;
    BroadcastReceiver receiver;

    Fragment fragment = null;

    // Define upload menu so that we can hide it if required
    Menu uploadMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progress);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open_drawer, R.string.nav_close_drawer);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        TextView tv_username = header.findViewById(R.id.tv_username);
        TextView tv_email = header.findViewById(R.id.tv_email);

        // Set the text for side panel
        tv_username.setText(getUserName());
        tv_email.setText(getUserEmail());

        showLandingFragment();

        if (isNetworkAvailable()) {
            updateTaxa();
            updateLicenses();
            updateObservationTypes();
        } else {
            Log.d(TAG, "There is no network available. Application will not be able to get new data from the server.");
        }

        // Broadcast will watch if upload service is active
        // and run the command when the upload is complete
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(UploadRecords.TASK_COMPLETED);
                // This will be executed after upload is completed
                if (s != null) {
                    Log.d(TAG, "Uploading records returned the code: " + s);
                    setUploadIconVisibility();

                    if (s.equals("no image")) {
                        alertOKButton(getString(R.string.no_image_on_storage));
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(UploadRecords.TASK_COMPLETED)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    // Send a short request to the server that will return if the taxonomic tree is up to date.
    private void updateTaxa() {
        Call<TaksoniResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getTaxa(1, 1, 0);
        call.enqueue(new Callback<TaksoniResponse>() {
            @Override
            public void onResponse(@NonNull Call<TaksoniResponse> call, @NonNull Response<TaksoniResponse> response) {
                if (response.isSuccessful()) {
                    // Check if version of taxa from Server and Preferences match. If server version is newer ask for update
                    TaksoniResponse taksoniResponse = response.body();
                    if(taksoniResponse != null) {
                        last_updated_taxa = Long.toString(taksoniResponse.getMeta().getLastUpdatedAt());
                        totalSpeciesOnline = Long.toString(taksoniResponse.getMeta().getLastPage());
                        totalSpeciesDao = String.valueOf(App.get().getDaoSession().getTaxonDao().count());
                        if (last_updated_taxa.equals(SettingsManager.getTaxaDatabaseUpdated())) {
                            Log.i(TAG,"It looks like this taxonomic database is already up to date. Nothing to do here!");
                            Log.i(TAG, "There are total of " + totalSpeciesOnline + " taxa online and a total of " + totalSpeciesDao + " entries in the database.");
                        } else {
                            Log.i(TAG, "Taxa database on the server (version: " + last_updated_taxa + ") seems to be newer that your version (" + SettingsManager.getTaxaDatabaseUpdated() + ").");
                            Log.i(TAG, "There are total of " + totalSpeciesOnline + " taxa online and a total of " + totalSpeciesDao + " entries in the database.");
                            // If there is no database on the phone, ask user to update it!
                            if (SettingsManager.getTaxaDatabaseUpdated().equals("0")) {
                                // If the database was never updated...
                                buildAlertMessageEmptyTaxaDb();
                            } else {
                                // If the online database is more recent try to get only the new taxa.
                                buildAlertMessageNewerTaxaDb();
                            }
                        }
                    }
                }

            }
            @Override
            public void onFailure(@NonNull Call<TaksoniResponse> call, @NonNull Throwable t) {
                // Inform the user on failure and write log message
                //Toast.makeText(LandingActivity.this, getString(R.string.database_connect_error), Toast.LENGTH_LONG).show();
                Log.e("Taxa database: ", "Application could not get taxon version data from a server!");
            }
        });
    }

    private void updateLicenses() {
        // Check if the licence has changed on the server and update if needed
        final Intent update_licenses = new Intent(LandingActivity.this, UpdateLicenses.class);
        startService(update_licenses);
    }

    private void updateObservationTypes() {
        Call<ObservationTypesResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getObservationTypes();
        call.enqueue(new Callback<ObservationTypesResponse>() {
            @Override
            public void onResponse(@NonNull Call<ObservationTypesResponse> call, @NonNull Response<ObservationTypesResponse> response) {
                ObservationTypesResponse observations = response.body();
                assert observations != null;
                String slug = observations.getData().getSlug();
                Log.d(TAG, "Observation types " + slug);
            }

            @Override
            public void onFailure(@NonNull Call<ObservationTypesResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Observation types could not be retrieved from server: " + t.getLocalizedMessage());
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        switch (id) {
            case R.id.nav_about:
                fragment = new AboutFragment();
                break;
            case R.id.nav_logout:
                fragment = new LogoutFragment();
                break;
            case R.id.nav_setup:
                fragment = new PreferencesFragment();
                break;
            case R.id.nav_help:
                startActivity(new Intent(LandingActivity.this, IntroActivity.class));
                finish();
                drawer.closeDrawer(GravityCompat.START);
                return true;
                /*
                startActivity(new Intent(LandingActivity.this, SetupActivity.class));
                drawer.closeDrawer(GravityCompat.START);
                return true;
                */
            default:
                fragment = new LandingFragment();
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame, fragment);
        ft.addToBackStack("new fragment");
        ft.commit();

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.confirmExit))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                            //finish();
                            finishAffinity();
                            System.exit(0);
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel());
                final AlertDialog alert = builder.create();
                alert.show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onResume() {
        //navDrawerFill();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        TextView tv_username = header.findViewById(R.id.tv_username);
        TextView tv_email = header.findViewById(R.id.tv_email);
        tv_username.setText(getUserName());
        tv_email.setText(getUserEmail());
        super.onResume();
    }

    // Right menu for uploading entries
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload_menu, menu);
        uploadMenu = menu;
        setUploadIconVisibility();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.action_upload) {
                    Log.d(TAG, "Upload records button clicked.");
                    final Intent uploadRecords = new Intent(LandingActivity.this, UploadRecords.class);
                    uploadRecords.setAction(UploadRecords.ACTION_START);
                    startService(uploadRecords);
                    return true;
            }
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            List<Fragment> fl = getSupportFragmentManager().getFragments();
            for (int i = 0; i < fl.size(); i++) {
                if (fl.get(i) instanceof LandingFragment) {
                    ((LandingFragment) fl.get(i)).updateData();
                }
            }
        }
        // Change the visibility of the Upload Icon
        setUploadIconVisibility();
    }

    protected void buildAlertMessageNewerTaxaDb() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // intent used to start service for fetching taxa

        final Intent fetchTaxa = new Intent(LandingActivity.this, FetchTaxa.class);
        fetchTaxa.setAction(FetchTaxa.ACTION_START);

        builder.setMessage(getString(R.string.new_database_available))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                    int toUpdate = Integer.parseInt(totalSpeciesOnline) - Integer.parseInt(totalSpeciesDao);
                    Log.i(TAG, "There are " + toUpdate + " taxa to be updated.");
                        startService(fetchTaxa);
                })
                .setNegativeButton(getString(R.string.no), (dialog, id) -> {
                    // If user don’t update just ignore updates until next session
                    SettingsManager.setTaxaDatabaseUpdated(last_updated_taxa);
                    dialog.cancel();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    protected void buildAlertMessageEmptyTaxaDb() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // intent used to start service for fetching taxa
        final Intent fetchTaxa = new Intent(this, FetchTaxa.class);
        fetchTaxa.setAction(FetchTaxa.ACTION_START);
        builder.setMessage(getString(R.string.database_empty))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.contin), (dialog, id) -> startService(fetchTaxa))
                .setNegativeButton(getString(R.string.skip), (dialog, id) -> {
                    // If user don’t update just ignore updates until next session
                    dialog.cancel();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    protected void alertOKButton(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // intent used to start service for fetching taxa
        builder.setMessage(message)
                .setCancelable(false)
                .setNeutralButton(getString(R.string.OK), (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Get the data from GreenDao database
    private UserData getLoggedUser() {
        // Get the user data from a GreenDao database
        List<UserData> userdata_list = App.get().getDaoSession().getUserDataDao().loadAll();
        // If there is no user data we should logout the user
        if (userdata_list == null || userdata_list.isEmpty()) {
            // Delete user data
            clearUserData(this);
            // Go to login screen
            userLogOut();
            return null;
        } else {
            return userdata_list.get(0);
        }
    }

    private String getUserName() {
        UserData userdata = getLoggedUser();
        if (userdata != null) {
            return userdata.getUsername();
        } else {
            return "User is not logged in";
        }
    }

    private String getUserEmail() {
        UserData userdata = getLoggedUser();
        if (userdata != null) {
            return userdata.getEmail();
        } else {
            return "Could not get email address.";
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivitymanager != null;
        NetworkInfo activeNetworkInfo = connectivitymanager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setUploadIconVisibility() {
        long numberOfItems = App.get().getDaoSession().getEntryDao().count();
        if (numberOfItems == 0) {
            // Disable the upload button
            uploadMenu.getItem(0).setEnabled(false);
            uploadMenu.getItem(0).getIcon().setAlpha(100);
        } else {
            // Disable the upload button
            uploadMenu.getItem(0).setEnabled(true);
            uploadMenu.getItem(0).getIcon().setAlpha(255);
        }
        Log.d(TAG, "There are " + numberOfItems + " items in the list.");
    }

    private void userLogOut() {
        Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public static void clearUserData(Context context) {
        // Delete user token
        SettingsManager.deleteToken();
        // Set the default preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().apply();
        SettingsManager.setTaxaDatabaseUpdated("0");
        SettingsManager.setProjectName(null);
        SettingsManager.setTaxaLastPageFetched("1");
        // Maybe also to delete database...
        App.get().getDaoSession().getTaxonDao().deleteAll();
        App.get().getDaoSession().getStageDao().deleteAll();
        App.get().getDaoSession().getUserDataDao().deleteAll();
        App.get().getDaoSession().getTaxonLocalizationDao().deleteAll();
    }

    private void showLandingFragment() {
        fragment = new LandingFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame, fragment);
        ft.addToBackStack("landing fragment");
        ft.commit();
    }

}