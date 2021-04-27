package org.biologer.biologer.gui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import org.biologer.biologer.App;
import org.biologer.biologer.FetchTaxa;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.UpdateLicenses;
import org.biologer.biologer.UploadRecords;
import org.biologer.biologer.User;
import org.biologer.biologer.network.JSON.ObservationTypes;
import org.biologer.biologer.sql.ObservationTypesData;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;
import org.biologer.biologer.network.JSON.ObservationTypesResponse;
import org.biologer.biologer.network.JSON.ObservationTypesTranslations;
import org.biologer.biologer.network.JSON.TaxaResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Biologer.Landing";

    private DrawerLayout drawer;
    BroadcastReceiver receiver;
    String how_to_use_network;
    String should_ask;

    Fragment fragment = null;

    // Define upload menu so that we can hide it if required
    Menu uploadMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        if (savedInstanceState == null) {
            // Check if notifications are enabled, if not warn the user!
            areNotificationsEnabled();

            showLandingFragment();

            // Get the user settings from preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LandingActivity.this);
            how_to_use_network = preferences.getString("auto_download", "wifi");

            // Check if there is network available and run the commands...
            String network_type = networkType();
            if (network_type.equals("connected") || network_type.equals("wifi")) {
                updateLicenses();
                updateObservationTypes();
                // AUTO upload/download if the right preferences are selected...
                if (how_to_use_network.equals("all") || (how_to_use_network.equals("wifi") && network_type.equals("wifi"))) {
                    uploadRecords();
                    should_ask = "download";
                } else {
                    Log.d(TAG, "Should ask user weather to download new taxonomic database (if there is one).");
                    should_ask = "ask";
                }
                updateTaxa();
            } else {
                Log.d(TAG, "There is no network available. Application will not be able to get new data from the server.");
            }
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

    private void areNotificationsEnabled() {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Log.d(TAG, "Global notifications are enabled. Good to know! :-)");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationmanager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                assert notificationmanager != null;
                if (notificationmanager.getNotificationChannel("biologer_taxa").getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    alertOKButton(getString(R.string.notifications_disabled1));
                }
                if (notificationmanager.getNotificationChannel("biologer_entries").getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    alertOKButton(getString(R.string.notifications_disabled2));
                }
            }
        } else {
            alertOKButton(getString(R.string.notifications_disabled));
        }
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
        String updated_at = SettingsManager.getTaxaUpdatedAt();

        Call<TaxaResponse> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).getTaxa(1, 1, Integer.parseInt(updated_at));
        call.enqueue(new Callback<TaxaResponse>() {

            @Override
            public void onResponse(@NonNull Call<TaxaResponse> call, @NonNull Response<TaxaResponse> response) {
                if (response.isSuccessful()) {
                    // Check if version of taxa from Server and Preferences match. If server version is newer ask for update
                    TaxaResponse taxaResponse = response.body();
                    if (taxaResponse != null) {
                        if (taxaResponse.getData().isEmpty()) {
                            Log.i(TAG, "It looks like this taxonomic database is already up to date. Nothing to do here!");
                        } else {
                            Log.i(TAG, "Taxa database on the server seems to be newer than your version timestamp: " + updated_at);
                            String skip_this = SettingsManager.getSkipTaxaDatabaseUpdate();

                            // Workaround to skip the update if user chooses so...
                            if (!skip_this.equals("0")) {
                                updateTaxaOnSkip(skip_this);
                            }
                            else {
                                updateTaxa2();
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<TaxaResponse> call, @NonNull Throwable t) {
                // Inform the user on failure and write log message
                //Toast.makeText(LandingActivity.this, getString(R.string.database_connect_error), Toast.LENGTH_LONG).show();
                Log.e("Taxa database: ", "Application could not get taxon version data from a server!");
            }
        });
    }

    private void updateTaxaOnSkip(String skip_this) {

        Log.i(TAG, "User chooses to skip updating taxonomic database on timestamp: " + skip_this);

        Call<TaxaResponse> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).getTaxa(1, 1, Integer.parseInt(skip_this));
        call.enqueue(new Callback<TaxaResponse>() {

            @Override
            public void onResponse(@NotNull Call<TaxaResponse> call, @NotNull Response<TaxaResponse> response) {
                if (response.isSuccessful()) {
                    TaxaResponse taxaResponse = response.body();
                    if (taxaResponse != null) {
                        if (taxaResponse.getData().isEmpty()) {
                            Log.i(TAG, "No new taxonomic database since the last time you skipped an update. Nothing to do here!");
                        } else {
                            updateTaxa2();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<TaxaResponse> call, @NotNull Throwable t) {
                Log.e("Taxa database: ", "Application could not get taxon version data from a server!");
            }
        });
    }

    private void updateTaxa2 () {
        int updated_at = Integer.parseInt(SettingsManager.getTaxaUpdatedAt());

        // If user choose to update data on any network, just do it!
        if (should_ask.equals("download")) {
            Log.d(TAG, "The user chooses to update taxa without asking. Fetching automatically.");
            startFetchingTaxa();
        } else {
            Log.d(TAG, "There is NO WiFi network, we should ask user for large download on mobile network.");
            if (updated_at == 0) {
                // If the online database is empty and user skips updating ask him on the next program startup
                buildAlertUpdateTaxa(getString(R.string.database_empty),
                        getString(R.string.contin),
                        getString(R.string.skip),
                        0);
            } else {
                // If the online database is more recent and user skips updating don’t ask him again for this version
                buildAlertUpdateTaxa(getString(R.string.new_database_available),
                        getString(R.string.yes),
                        getString(R.string.no),
                        1);
            }
        }
    }

    private void updateLicenses() {
        // Check if the licence has changed on the server and update if needed
        final Intent update_licenses = new Intent(this, UpdateLicenses.class);
        startService(update_licenses);
    }

    private void updateObservationTypes() {
        String system_time = String.valueOf(System.currentTimeMillis()/1000);
        String updated_at = SettingsManager.getObservationTypesUpdated();

        Call<ObservationTypesResponse> call = RetrofitClient.getService(
                SettingsManager.getDatabaseName()).getObservationTypes(Integer.parseInt(updated_at));
        call.enqueue(new Callback<ObservationTypesResponse>() {

            @Override
            public void onResponse(@NonNull Call<ObservationTypesResponse> call, @NonNull Response<ObservationTypesResponse> response) {
                ObservationTypesResponse observationsResponse = response.body();
                if (observationsResponse != null) {
                    if (observationsResponse.getData().length == 0) {
                        Log.d(TAG, "Recent observation types are already downloaded from server.");
                    } else {
                        ObservationTypes[] obs = observationsResponse.getData();
                        for (ObservationTypes ob : obs) {
                            Log.d(TAG, "Observation type ID: " + ob.getId() + "; Slug: " + ob.getSlug());

                            // Save translations in a separate table...
                            List<ObservationTypesTranslations> observation_translations = ob.getTranslations();
                            ObservationTypesData[] localizations = new ObservationTypesData[observation_translations.size()];
                            for (int j = 0; j < observation_translations.size(); j++) {
                                ObservationTypesData localization = new ObservationTypesData();
                                localization.setObservationId(ob.getId().longValue());
                                localization.setSlug(ob.getSlug());
                                localization.setLocaleId(observation_translations.get(j).getId());
                                localization.setLocale(observation_translations.get(j).getLocale());
                                localization.setName(observation_translations.get(j).getName());
                                localizations[j] = localization;
                            }
                            App.get().getDaoSession().getObservationTypesDataDao().insertOrReplaceInTx(localizations);

                        }
                        Log.d(TAG, "Observation types locales written to the database, there are " + App.get().getDaoSession().getObservationTypesDataDao().count() + " records");
                        SettingsManager.setObservationTypesUpdated(system_time);
                        Log.d(TAG, "Timestamp for observation time update is set to " + system_time);
                    }
                }
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

        if (id == R.id.nav_list) {
            fragment = new LandingFragment();
        }
        if (id == R.id.nav_setup) {
            fragment = new PreferencesFragment();
        }
        if (id == R.id.nav_logout) {
            fragment = new LogoutFragment();
        }
        if(id == R.id.nav_about) {
            Log.d(TAG, "User clicked about icon.");
            fragment = new AboutFragment();
        }
        if (id == R.id.nav_help) {
            startActivity(new Intent(LandingActivity.this, IntroActivity.class));
            drawer.closeDrawer(GravityCompat.START);
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.content_frame, fragment);
        fragmentTransaction.addToBackStack("new fragment");
        fragmentTransaction.commit();
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            Log.d(TAG, "Back button pressed, while LandingFragment is active.");
            getSupportFragmentManager().popBackStack();
            super.onBackPressed();
            return;
        }

        super.onBackPressed();
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
            // Disable the upload button to avoid double taps
            item.setEnabled(false);
            item.getIcon().setAlpha(100);
            // Upload data to the server
            uploadRecords();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void uploadRecords() {
        if (App.get().getDaoSession().getEntryDao().count() != 0) {
            Log.d(TAG, "Uploading entries to the online database.");
            final Intent uploadRecords = new Intent(LandingActivity.this, UploadRecords.class);
            uploadRecords.getFlags();
            uploadRecords.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            uploadRecords.setAction(UploadRecords.ACTION_START);
            startService(uploadRecords);
        } else {
            Log.d(TAG, "No entries to upload to the online database.");
        }
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

    protected void buildAlertUpdateTaxa(String message, String yes_string, String no_string, int should_skip_next) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(yes_string, (dialog, id) -> startFetchingTaxa())
                .setNegativeButton(no_string, (dialog, id) -> {
                    if (should_skip_next == 1) {
                        SettingsManager.setSkipTaxaDatabaseUpdate(String.valueOf(System.currentTimeMillis()/1000));
                    }
                    dialog.cancel();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void startFetchingTaxa() {
        if (!FetchTaxa.isInstanceCreated()) {
            final Intent fetchTaxa = new Intent(LandingActivity.this, FetchTaxa.class);
            fetchTaxa.setAction(FetchTaxa.ACTION_START);
            startService(fetchTaxa);
        }
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
            User.clearUserData(this);
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

    private String networkType() {
        ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivitymanager != null;
        NetworkInfo activeNetworkInfo = connectivitymanager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            NetworkInfo wifiNetworkInfo = connectivitymanager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected()) {
                Log.d(TAG, "You are connected to the WiFi network.");
                return "wifi";
            }
            Log.d(TAG, "You are connected to the mobile network.");
            return "connected";
        }
        Log.d(TAG, "You are not connected to the network.");
        return "not_connected";
    }

    private void setUploadIconVisibility() {
        long numberOfItems = App.get().getDaoSession().getEntryDao().count();
        Log.d(TAG, "There are " + numberOfItems + " items in the list.");
        if (numberOfItems == 0) {
            // Disable the upload button
            if (uploadMenu != null) {
                uploadMenu.getItem(0).setEnabled(false);
                uploadMenu.getItem(0).getIcon().setAlpha(100);
            }
        } else {
            // Disable the upload button
            if (uploadMenu != null) {
                uploadMenu.getItem(0).setEnabled(true);
                uploadMenu.getItem(0).getIcon().setAlpha(255);
            }
        }
    }

    private void userLogOut() {
        Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void showLandingFragment() {
        fragment = new LandingFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame, fragment);
        ft.addToBackStack("landing fragment");
        ft.commit();
    }

}