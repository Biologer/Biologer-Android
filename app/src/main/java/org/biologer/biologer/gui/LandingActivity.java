package org.biologer.biologer.gui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import com.google.android.material.navigation.NavigationView;
import com.opencsv.CSVWriter;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
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
import android.widget.Toast;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.network.FetchTaxa;
import org.biologer.biologer.network.GetTaxaGroups;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.UpdateLicenses;
import org.biologer.biologer.network.UploadRecords;
import org.biologer.biologer.User;
import org.biologer.biologer.adapters.CreateExternalFile;
import org.biologer.biologer.adapters.StageAndSexLocalization;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.JSON.RefreshTokenResponse;
import org.biologer.biologer.network.JSON.UserDataResponse;
import org.biologer.biologer.network.UpdateObservationTypes;
import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;
import org.biologer.biologer.network.JSON.TaxaResponse;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandingActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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

        String token = SettingsManager.getAccessToken();
        String database_name = SettingsManager.getDatabaseName();
        boolean MAIL_CONFIRMED = SettingsManager.isMailConfirmed();

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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LandingActivity.this);

        // Set the text for side panel
        tv_username.setText(getUserName());
        tv_email.setText(getUserEmail());

        // Ensure this is run only once in each instance or just after login screen
        boolean fromLoginScreen = false;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            fromLoginScreen = bundle.getBoolean("fromLoginScreen");
        }
        if (savedInstanceState == null || fromLoginScreen) {

            // If SQL is updated we will try to login in the user
            if (SettingsManager.isSqlUpdated()) {
                Log.i(TAG, "SQL database must be updated!");
                Toast.makeText(LandingActivity.this, getString(R.string.sql_updated_message), Toast.LENGTH_LONG).show();

                // First get the existing groups of taxa so we can fetch them again
                if (InternetConnection.isConnected(LandingActivity.this)) {
                    final Intent getTaxaGroups = new Intent(LandingActivity.this, GetTaxaGroups.class);
                    startService(getTaxaGroups);

                    Call<UserDataResponse> service = RetrofitClient.getService(database_name).getUserData();
                    service.enqueue(new Callback<UserDataResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<UserDataResponse> service, @NonNull Response<UserDataResponse> response) {
                            if (response.isSuccessful()) {
                                if (response.body() != null) {
                                    String email = response.body().getData().getEmail();
                                    String name = response.body().getData().getFullName();
                                    int data_license = response.body().getData().getSettings().getDataLicense();
                                    int image_license = response.body().getData().getSettings().getImageLicense();
                                    UserData user = new UserData(null, name, email, data_license, image_license);
                                    App.get().getDaoSession().getUserDataDao().insertOrReplace(user);
                                    SettingsManager.setSqlUpdated(false);
                                } else {
                                    alertWarnAndExit(getString(R.string.login_after_sql_update_fail));
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<UserDataResponse> service, @NonNull Throwable t) {
                            Log.e(TAG, "Cannot get response from the server (test taxa response)");
                            alertWarnAndExit(getString(R.string.login_after_sql_update_fail));
                        }
                    });
                }
            }
            else {
                Log.i(TAG, "SQL is up to date!");
            }

            if (token != null && MAIL_CONFIRMED) {

                if (Long.parseLong(SettingsManager.getTokenExpire()) >= System.currentTimeMillis() / 1000) {
                    Log.d(TAG, "Token is still OK, email is confirmed. Token will expire on " + SettingsManager.getTokenExpire());
                } else {
                    Log.d(TAG, "Token expired. Refreshing login token.");
                    if (InternetConnection.isConnected(LandingActivity.this)) {

                        String refreshToken = SettingsManager.getRefreshToken();
                        String rsKey = BuildConfig.BiologerRS_Key;
                        String hrKey = BuildConfig.BiologerHR_Key;
                        String baKey = BuildConfig.BiologerBA_Key;

                        if (database_name.equals("https://biologer.org")) {
                            Log.d(TAG, "Serbian database selected.");
                            Call<RefreshTokenResponse> refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", rsKey, refreshToken, "*");
                            RefreshToken(refresh);
                        }
                        if (database_name.equals("https://biologer.hr")) {
                            Log.d(TAG, "Croatian database selected.");
                            Call<RefreshTokenResponse> refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", hrKey, refreshToken, "*");
                            RefreshToken(refresh);
                        }
                        if (database_name.equals("https://biologer.ba")) {
                            Log.d(TAG, "Bosnian database selected.");
                            Call<RefreshTokenResponse> refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", baKey, refreshToken, "*");
                            RefreshToken(refresh);
                        }
                        if (database_name.equals("https://dev.biologer.org")) {
                            Log.d(TAG, "Developmental database selected.");
                            Call<RefreshTokenResponse> refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", rsKey, refreshToken,"*");
                            RefreshToken(refresh);
                        }
                        Log.d(TAG, "Logging into " + database_name + " using refresh token.");
                    }
                    else {
                        alertWarnAndExit(getString(R.string.refresh_token_no_internet));
                    }
                }
            }

            else {
                Log.d(TAG, "No token or email address not confirmed..");
                Log.d(TAG, "TOKEN: " + token);
                Log.d(TAG, "Is mail confirmed: " + MAIL_CONFIRMED);
                Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
                intent.putExtra("TOKEN_EXPIRED", false);
                startActivity(intent);
            }

            // Check if notifications are enabled, if not warn the user!
            areNotificationsEnabled();

            showLandingFragment();

            // Get the user settings from preferences
            how_to_use_network = preferences.getString("auto_download", "wifi");

            // Check if there is network available and run the commands...
            String network_type = InternetConnection.networkType(this);
            if (network_type != null) {
                updateLicenses();
                UpdateObservationTypes.updateObservationTypes();
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
                    updateEntryListView();

                    if (s.equals("no image")) {
                        alertOKButton(getString(R.string.no_image_on_storage));
                    }
                }
            }
        };
    }

    private void RefreshToken(Call<RefreshTokenResponse> refresh_call) {
        refresh_call.enqueue(new Callback<RefreshTokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<RefreshTokenResponse> call, @NonNull Response<RefreshTokenResponse> response) {
                if(response.isSuccessful()) {
                    if (response.body() != null) {
                        String token1 = response.body().getAccessToken();
                        String refresh_token = response.body().getRefreshToken();
                        Log.d(TAG, "New token value is: " + token1);
                        SettingsManager.setAccessToken(token1);
                        SettingsManager.setRefreshToken(refresh_token);
                        long expire = response.body().getExpiresIn();
                        long expire_date = (System.currentTimeMillis() / 1000) + expire;
                        SettingsManager.setTokenExpire(String.valueOf(expire_date));
                        Log.d(TAG, "Token will expire on timestamp: " + expire_date);
                    }
                } else {
                    alertWarnAndExit(getString(R.string.refresh_token_failed));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RefreshTokenResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Cannot get response from the server (refresh token)" + t);
                alertWarnAndExit(getString(R.string.refresh_token_failed));
            }
        });
    }

    private void updateEntryListView() {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (int i = 0; i < fragmentList.size(); i++) {
            if (fragmentList.get(i) instanceof LandingFragment) {
                Log.d(TAG, "Updating entries list after upload to server.");
                ArrayList<Entry> entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
                ((LandingFragment) fragmentList.get(i)).updateEntries(entries);
            }
        }
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
                SettingsManager.getDatabaseName()).getTaxa(1, 1, Integer.parseInt(updated_at), false, null, true);
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
                SettingsManager.getDatabaseName()).getTaxa(1, 1, Integer.parseInt(skip_this), true, null, true);
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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        if (id == R.id.nav_list) {
            fragment = new LandingFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.content_frame, fragment);
            fragmentTransaction.addToBackStack("Landing fragment");
            fragmentTransaction.commit();
            drawer.closeDrawer(GravityCompat.START);
            return true;
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
        if (item.getItemId() == R.id.export_csv) {
            Log.d(TAG, "CSV export button clicked.");
            // Disable the upload button to avoid double taps
            //item.setEnabled(false);
            exportCSV();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportCSV() {

        String filename = "Export_" + new SimpleDateFormat("yyMMddss", Locale.getDefault()).format(new Date());

        Uri uri = CreateExternalFile.newDocumentFile(this, filename, ".csv");
        OutputStream output = null;
        try {
            if (uri != null) {
                output = getContentResolver().openOutputStream(uri);
            }
            else {
                Log.d(TAG, "URI is null!");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        CSVWriter writer;
        writer = new CSVWriter(
                new OutputStreamWriter(output),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
        );

        String[] title = {
                getString(R.string.taxon),
                getString(R.string.year),
                getString(R.string.month),
                getString(R.string.day),
                getString(R.string.latitude),
                getString(R.string.longitude),
                getString(R.string.elevation),
                getString(R.string.csv_accuracy),
                getString(R.string.location),
                getString(R.string.time),
                getString(R.string.csv_note),
                getString(R.string.found_dead),
                getString(R.string.note_on_dead),
                getString(R.string.observer),
                getString(R.string.identifier),
                getString(R.string.sex),
                getString(R.string.number),
                getString(R.string.project),
                getString(R.string.habitat),
                getString(R.string.found_on),
                getString(R.string.stage),
                getString(R.string.original_observation),
                getString(R.string.dataset),
                getString(R.string.data_license),
                getString(R.string.image_license),
                getString(R.string.atlas_code)
        };
        writer.writeNext(title);

        ArrayList<Entry> entries = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
        String u = getUserName();

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            String[] row = {
                    entry.getTaxonSuggestion(),
                    entry.getYear(),
                    entry.getMonth(),
                    entry.getDay(),
                    String.format(Locale.ENGLISH, "%.6f", entry.getLattitude()),
                    String.format(Locale.ENGLISH, "%.6f", entry.getLongitude()),
                    String.format(Locale.ENGLISH, "%.0f", entry.getElevation()),
                    String.format(Locale.ENGLISH, "%.0f", entry.getAccuracy()),
                    entry.getLocation(),
                    entry.getTime(),
                    entry.getComment(),
                    translateFoundDead(entry.getDeadOrAlive()),
                    entry.getCauseOfDeath(),
                    u,
                    u,
                    StageAndSexLocalization.getSexLocale(this, entry.getSex()),
                    removeNullInteger(entry.getNoSpecimens()),
                    entry.getProjectId(),
                    entry.getHabitat(),
                    entry.getFoundOn(),
                    StageAndSexLocalization.getStageLocaleFromID(this, entry.getStage()),
                    entry.getTaxonSuggestion(),
                    getString(R.string.dataset),
                    translateLicence(entry.getData_licence()),
                    translateLicence(String.valueOf(entry.getImage_licence())),
                    removeNullLong(entry.getAtlas_code())
            };
            writer.writeNext(row);
        }

        try {
            writer.close();
            Toast.makeText(LandingActivity.this, getString(R.string.export_to_csv_success) + filename, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String removeNullInteger(Integer integer) {
        if (integer != null) {
            return String.valueOf(integer);
        } else {
            return "";
        }
    }

    private String removeNullLong(Long l) {
        if (l != null) {
            return String.valueOf(l);
        } else {
            return "";
        }
    }

    private String translateLicence(String data_license) {
        if (data_license.equals("10")) {
            return getString(R.string.export_licence_10);
        }
        if (data_license.equals("11")) {
            return getString(R.string.export_licence_11);
        }
        if (data_license.equals("20")) {
            return getString(R.string.export_licence_20);
        }
        if (data_license.equals("30")) {
            return getString(R.string.export_licence_30);
        }
        if (data_license.equals("40")) {
            return getString(R.string.export_licence_40);
        }
        else {
            return "";
        }
    }

    private String translateFoundDead(String alive) {
        if (alive.equals("true")) {
            return getString(R.string.no);
        } else {
            return getString(R.string.yes);
        }
    }

    private void uploadRecords() {
        if (App.get().getDaoSession().getEntryDao().count() != 0) {
            Log.d(TAG, "Uploading entries to the online database.");
            final Intent uploadRecords = new Intent(LandingActivity.this, UploadRecords.class);
            uploadRecords.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            uploadRecords.setAction(UploadRecords.ACTION_START);
            startService(uploadRecords);
        } else {
            Log.d(TAG, "No entries to upload to the online database.");
        }
    }

    // Run after filling the new entry from EntryActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "On activity result.");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            updateEntryListView();
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

    protected void alertWarnAndExit(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // intent used to start service for fetching taxa
        builder.setMessage(message)
                .setCancelable(false)
                .setNeutralButton(getString(R.string.OK), (dialog, id) -> {
                    dialog.cancel();
                    this.finishAffinity();
                });
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