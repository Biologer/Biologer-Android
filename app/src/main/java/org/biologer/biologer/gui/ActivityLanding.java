package org.biologer.biologer.gui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Supplier;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.ActivityLandingBinding;
import org.biologer.biologer.services.AuthHelper;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.UpdateAnnouncements;
import org.biologer.biologer.network.UpdateLicenses;
import org.biologer.biologer.network.UpdateObservationTypes;
import org.biologer.biologer.network.UpdateTaxa;
import org.biologer.biologer.network.UploadRecords;
import org.biologer.biologer.network.json.TaxaResponse;
import org.biologer.biologer.network.json.UserDataResponse;
import org.biologer.biologer.network.NotificationSyncWorker;
import org.biologer.biologer.services.CsvExporter;
import org.biologer.biologer.services.CsvTaxaLoader;
import org.biologer.biologer.services.ObjectBoxHelper;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.UserDb;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityLanding extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Biologer.Landing";
    private ActivityLandingBinding binding;
    static String how_to_use_network;

    // Define upload menu so that we can hide it if required
    static Menu uploadMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLandingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String database_url = SettingsManager.getDatabaseName();

        setSupportActionBar(binding.toolbar.toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar.toolbar,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        updateNavHeader();

        showFragment("LANDING_FRAGMENT", FragmentLanding::new);

        handleIncomingIntent(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "Back button is pressed!");
                backPressed();
            }
        });

        // If there is no token, falling back to the login screen
        if (SettingsManager.getAccessToken() == null) {
            Log.d(TAG, getString(R.string.no_login_token_falling_back_to_login_screen));
            Toast.makeText(this, getString(R.string.no_login_token_falling_back_to_login_screen), Toast.LENGTH_LONG).show();
            showUserLoginScreen(false);
            return;
        }

        // If Biologer is updated to 5.1, the user should get FCM here (no need to logout)
        if (SettingsManager.getLastFcmToken() == null) {
            ActivityLogin.getFirebaseMessagingToken();
        }

        // On the first run (after the user came from the login screen) show some help
        if (SettingsManager.isFirstRun()) {
            Log.d(TAG, "This is first run of the program.");
            SettingsManager.setFirstRun(false);
            Intent intent = new Intent(ActivityLanding.this, ActivityIntro.class);
            intent.putExtra("firstRun", true);
            startActivity(intent);
        } else {
            // If Landing activity is started for the first time in the session run all the online services
            if (savedInstanceState == null) {
                Log.d(TAG, "LandingActivity started for the first time (savedInstanceState is null).");
                if (database_url != null) {
                    runServices(database_url);
                } else {
                    Toast.makeText(this, R.string.database_url_empty, Toast.LENGTH_LONG).show();
                    fallbackToLoginScreen();
                }

                // If the user did not start the EntryActivity show a short help
                if (!SettingsManager.getEntryOpen()) {
                    binding.listEntriesInfoText.setText(R.string.entry_info_first_run);
                    binding.listEntriesInfoText.setVisibility(View.VISIBLE);
                }

            } else {
                // If the LandingActivity is restarted, just update GUI
                Log.d(TAG, "LandingActivity is already running (savedInstanceState is not null)");
                if (!SettingsManager.isMailConfirmed()) {
                    database_url = SettingsManager.getDatabaseName();
                    if (database_url != null) {
                        checkMailConfirmed(database_url);
                    } else {
                        Toast.makeText(this, R.string.database_url_empty, Toast.LENGTH_LONG).show();
                        fallbackToLoginScreen();
                    }
                }
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        // If the email is not confirmed we would like to check this every time the
        // user restarts the LandingActivity
        if (!SettingsManager.isMailConfirmed()) {
            String database = SettingsManager.getDatabaseName();
            if (database != null) {
                // Check mail should not be called if the user is not logged in.
                // The user is not logged in if the SQL UserData is empty.
                List<UserDb> userData = App.get().getBoxStore().boxFor(UserDb.class).getAll();
                if (!userData.isEmpty()) {
                    checkMailConfirmed(database);
                }
            }
            // It seems that the user is logged out, thus we need to go to the login screen
            else {
                Toast.makeText(this, getString(R.string.there_is_no_user_data_in_sql_database), Toast.LENGTH_LONG).show();
                showUserLoginScreen(false);
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && intent.hasExtra(ActivityAnnouncement.EXTRA_DISPLAY_FRAGMENT)) {
            String fragmentTag = intent.getStringExtra(ActivityAnnouncement.EXTRA_DISPLAY_FRAGMENT);

            if (ActivityAnnouncement.FRAGMENT_ANNOUNCEMENTS_TAG.equals(fragmentTag)) {

                showFragment(ActivityAnnouncement.FRAGMENT_ANNOUNCEMENTS_TAG, FragmentAnnouncements::new);
            }
            intent.removeExtra(ActivityAnnouncement.EXTRA_DISPLAY_FRAGMENT);
        }
    }

    private void backPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        int stackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (stackCount <= 1) {
            finishAffinity();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onResume() {
        binding.navView.setNavigationItemSelectedListener(this);

        View header = binding.navView.getHeaderView(0);
        TextView tv_username = header.findViewById(R.id.tv_username);
        TextView tv_email = header.findViewById(R.id.tv_email);
        tv_username.setText(ObjectBoxHelper.getUserName());
        tv_email.setText(ObjectBoxHelper.getUserEmail());

        super.onResume();
    }

    // Right menu for uploading entries
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload_menu, menu);
        uploadMenu = menu;
        updateMenuIconVisibility();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_upload) {
            Log.d(TAG, "Upload records button clicked.");
            // Disable the upload button to avoid double taps
            item.setEnabled(false);
            Objects.requireNonNull(item.getIcon()).setAlpha(100);
            // Upload data to the server
            uploadRecords();
            return true;
        }
        if (item.getItemId() == R.id.export_csv) {
            CsvExporter.exportEntriesToCsv(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fallbackToLoginScreen() {
        Log.e(TAG, "Something is wrong, the settings are lost...");
        ObjectBoxHelper.removeAllData(ActivityLanding.this);
        SettingsManager.deleteSettings();
        Toast.makeText(this, getString(R.string.something_is_wrong_falling_back_to_login_screen), Toast.LENGTH_LONG).show();
        showUserLoginScreen(false);
    }

    private void runServices(String database_url) {
        Log.d(TAG, "Running online services");

        updateTaxaDatabase(database_url);
        updateToken(database_url);
        startNetworkServices(database_url);
    }

    private void startNetworkServices(String databaseUrl) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        how_to_use_network = prefs.getString("auto_download", "wifi");

        String networkType = InternetConnection.networkType(this);
        if (networkType == null) {
            Log.d(TAG, "No network available. Skipping network operations.");
            return;
        }

        updateLicenses();
        UpdateObservationTypes.updateObservationTypes(databaseUrl);

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            checkNotificationPermission();
            return;
        }

        Log.d(TAG, "Notifications are enabled.");
        if (!shouldDownload(this)) return;

        uploadRecords();
        updateAnnouncements();
        updateNotifications();
    }

    private void updateNotifications() {
        long timestamp = Long.parseLong(SettingsManager.getNotificationsUpdatedAt());
        NotificationSyncWorker.enqueueNow(getApplicationContext(), timestamp);
    }

    private void updateAnnouncements() {
        long now = System.currentTimeMillis() / 1000;
        long lastCheck = Long.parseLong(SettingsManager.getLastInternetCheckout());
        if (lastCheck == 0 || now > lastCheck + 36000) { // 10 hours
            Log.d(TAG, "10 hours elapsed; updating announcements.");
            Intent intent = new Intent(this, UpdateAnnouncements.class);
            intent.putExtra("show_notification", true);
            startService(intent);
            SettingsManager.setLastInternetCheckout(String.valueOf(now));
        }
    }

    private void updateToken(String databaseUrl) {
        if (!SettingsManager.isMailConfirmed()) {
            Log.d(TAG, "Email not confirmed, verifying.");
            checkMailConfirmed(databaseUrl);
            return;
        }

        String expireValue = SettingsManager.getTokenExpire();
        if (expireValue == null || expireValue.isEmpty()) {
            Log.w(TAG, "Token expiration value missing, forcing log in.");
            showUserLoginScreen(true);
            return;
        }
        long expireIn = Long.parseLong(expireValue);
        long now = System.currentTimeMillis() / 1000;
        long refreshThreshold = 15778800; // ~6 months

        if (expireIn < now) {
            Log.d(TAG, "Token expired. Refreshing login token.");
            if (InternetConnection.isConnected(this)) {
                refreshToken(databaseUrl, true);
            } else {
                alertWarnAndExit(getString(R.string.refresh_token_no_internet));
            }
        } else if (expireIn - now <= refreshThreshold) {
            Log.d(TAG, "Token will expire soon. Refreshing in advance.");
            if (InternetConnection.isConnected(this)) {
                refreshToken(databaseUrl, false);
            }
        } else {
            Log.d(TAG, "Token still valid until " + expireIn);
        }
    }


    private void updateTaxaDatabase(String databaseUrl) {
        // Load database from local assets folder if newer
        int updatedAt = Integer.parseInt(SettingsManager.getTaxaUpdatedAt());
        String assetsTimestamp = "1752595284";
        if (updatedAt < Integer.parseInt(assetsTimestamp)) {
            Log.i(TAG, "Loading taxa database from Android assets (local newer).");
            new CsvTaxaLoader(this).loadInternalTaxaDataset(databaseUrl, assetsTimestamp);
        } else {
            Log.i(TAG, "Taxa DB is up to date, checking online.");
            updateTaxa();
        }
    }

    private void checkMailConfirmed(String database_url) {

        Call<UserDataResponse> userData = RetrofitClient.getService(database_url).getUserData();
        userData.enqueue(new Callback<>() {

            @Override
            public void onResponse(@NonNull Call<UserDataResponse> call, @NonNull Response<UserDataResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        if (response.body().getData().isEmailVerified()) {
                            updateGuiOnMailConfirmed(true);
                            SettingsManager.setMailConfirmed(true);
                            binding.listEntriesEmailNotConfirmed.setVisibility(View.GONE);
                        } else {
                            updateGuiOnMailConfirmed(false);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDataResponse> call, @NonNull Throwable t) {
                Toast.makeText(ActivityLanding.this, getString(R.string.cannot_connect_server), Toast.LENGTH_LONG).show();
                updateGuiOnMailConfirmed(false);
            }
        });
    }

    private void updateGuiOnMailConfirmed(boolean mail_confirmed) {
        FloatingActionButton floatingActionButton = findViewById(R.id.float_button_new_entry);
        floatingActionButton.setEnabled(mail_confirmed);
        if (mail_confirmed) {
            binding.listEntriesEmailNotConfirmed.setVisibility(View.GONE);
            floatingActionButton.setAlpha(1f);
        } else {
            binding.listEntriesEmailNotConfirmed.setVisibility(View.VISIBLE);
            floatingActionButton.setAlpha(0.25f);
        }
    }

    // Setting tokenExpired to true will send user to login screen, but without option
    // to choose database. This is used only to refresh expired token.
    private void showUserLoginScreen(boolean tokenExpired) {
        Intent intent = new Intent(ActivityLanding.this, ActivityLogin.class);
        if (tokenExpired) {
            SettingsManager.deleteAccessToken();
            SettingsManager.deleteFcmToken();
            intent.putExtra("refreshToken", "yes");
        }
        startActivity(intent);
    }

    private void updateNavHeader() {
        View header = binding.navView.getHeaderView(0);
        ((TextView) header.findViewById(R.id.tv_username)).setText(ObjectBoxHelper.getUserName());
        ((TextView) header.findViewById(R.id.tv_email)).setText(ObjectBoxHelper.getUserEmail());
    }

    private void refreshToken(String databaseUrl, boolean warnUser) {
        AuthHelper auth = new AuthHelper(this);
        auth.refreshToken(databaseUrl, warnUser, new AuthHelper.RefreshCallbacks() {
            @Override public void onSuccess() {
                Log.d(TAG, "Token refreshed successfully.");
            }

            @Override public void onExpired() {
                showUserLoginScreen(true);
            }

            @Override public void onError() {
                alertTokenExpired();
            }
        });
    }

    private void checkNotificationPermission() {
        // For Android API 33+ we need permissions for Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    Log.d(TAG, "Notifications are not enabled, requesting permissions.");
                    // Ask permission for notification if not definitely declined by user
                    requestNotificationPermissions.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    // Explain user why we need notifications
                    Log.d(TAG, "Notifications permit declined temporary.");
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.notificationPermit)
                            .setCancelable(false)
                            .setPositiveButton(R.string.enable, (dialog, id) -> requestNotificationPermissions.launch(Manifest.permission.POST_NOTIFICATIONS))
                            .setNegativeButton(R.string.ignore, (dialog, id) -> dialog.cancel());
                    final AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        }
        // For Android API 26-32 we just need to notify the user
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Notifications are not enabled, notifying user.");
                alertOKButton(getString(R.string.notifications_disabled));
            }
        }
    }

    private final ActivityResultLauncher<String> requestNotificationPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            Log.d(TAG, "Notification are just enabled... :)");
                            updateTaxa();
                        } else {
                            Toast.makeText(this, getString(R.string.notifications_are_disabled), Toast.LENGTH_LONG).show();
                        }
                    }
            );

    public static boolean shouldDownload(Context context) {
        String network_type = InternetConnection.networkType(context);

        if (network_type == null || how_to_use_network == null) {
            return true;
        }

        if ("all".equals(how_to_use_network) || ("wifi".equals(how_to_use_network) && "wifi".equals(network_type))) {
            return true;
        } else {
            Log.d(TAG, "Should ask user whether to download new taxonomic database (if there is one).");
            return false;
        }
    }

    // Send a short request to the server that will return if the taxonomic tree is up to date.
    private void updateTaxa() {
        Log.d(TAG, "Current timestamp: " + System.currentTimeMillis() / 1000);
        if (UpdateTaxa.isInstanceCreated()) {
            Log.d(TAG, "UpdateTaxa is already running. Skipping update for now!");
        } else {
            String updated_at = SettingsManager.getTaxaUpdatedAt();
            String skip_this = SettingsManager.getSkipTaxaDatabaseUpdate();
            String timestamp = updated_at;
            if (Long.parseLong(skip_this) > Long.parseLong(updated_at)) {
                timestamp = skip_this;
            }

            String database = SettingsManager.getDatabaseName();
            int finalTimestamp = Integer.parseInt(timestamp);
            Log.d(TAG, "Checking if there is new taxa for " + timestamp + " at " + database);
            Call<TaxaResponse> call = RetrofitClient.getService(
                    database).getTaxa(1, 1,
                    Integer.parseInt(timestamp), false,
                    null, false);
            call.enqueue(new Callback<>() {

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
                                updateTaxa2(finalTimestamp);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TaxaResponse> call, @NonNull Throwable t) {
                    // Inform the user on failure and write log message
                    Toast.makeText(ActivityLanding.this, getString(R.string.database_connect_error), Toast.LENGTH_LONG).show();
                    // Log.e(TAG, "Application could not get taxa database version data from a server (test request)!" + t.getMessage());
                }
            });
        }
    }

    private void updateTaxa2 (int timestamp) {
        // If user choose to update data on any network, just do it!
        if (shouldDownload(this)) {
            Log.d(TAG, "The user chooses to update taxa without asking. Fetching automatically.");
            startFetchingTaxa();
        } else {
            Log.d(TAG, "There is NO WiFi network, we should ask user for large download on mobile network.");
            if (timestamp == 0) {
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
        if (ObjectBoxHelper.getUser() == null) {
            Toast.makeText(ActivityLanding.this, getString(R.string.missing_user_data), Toast.LENGTH_LONG).show();
            fallbackToLoginScreen();
        } else {
            // Check if the licence has changed on the server and update if needed
            final Intent update_licenses = new Intent(this, UpdateLicenses.class);
            update_licenses.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(update_licenses);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Hide the info text if user moves away from landing fragment
        binding.listEntriesInfoText.setVisibility(View.GONE);
        binding.listEntriesEmailNotConfirmed.setVisibility(View.GONE);

        if (id == R.id.nav_list) {
            // Show the info text once back to the list
            if (!SettingsManager.getEntryOpen()) {
                binding.listEntriesInfoText.setVisibility(View.VISIBLE);
            }
            if (!SettingsManager.isMailConfirmed()) {
                binding.listEntriesEmailNotConfirmed.setVisibility(View.VISIBLE);
            }
            showFragment("LANDING_FRAGMENT", FragmentLanding::new);
        }
        if (id == R.id.nav_help) {
            startActivity(new Intent(ActivityLanding.this, ActivityIntro.class));
        }
        if (id == R.id.nav_setup) {
            showFragment("PREFERENCES_FRAGMENT", FragmentPreferences::new);
        }
        if (id == R.id.nav_logout) {
            showFragment("LOGOUT_FRAGMENT", FragmentLogout::new);
        }
        if (id == R.id.nav_about) {
            showFragment("ABOUT_FRAGMENT", FragmentAbout::new);
        }
        if (id == R.id.nav_notifications) {
            startActivity(new Intent(ActivityLanding.this, ActivityNotifications.class));
        }
        if (id == R.id.nav_announcements) {
            showFragment("ANNOUNCEMENTS_FRAGMENT", FragmentAnnouncements::new);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showFragment(String tag, Supplier<Fragment> fragmentSupplier) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);
        if (existing != null) tx.replace(R.id.content_frame, existing, tag);
        else tx.add(R.id.content_frame, fragmentSupplier.get(), tag);
        tx.addToBackStack(tag + "_stack");
        tx.commit();
    }

    private void uploadRecords() {
        if (!App.get().getBoxStore().boxFor(EntryDb.class).getAll().isEmpty()) {
            Log.d(TAG, "Uploading entries to the online database.");
            final Intent uploadRecords = new Intent(ActivityLanding.this, UploadRecords.class);
            uploadRecords.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            uploadRecords.setAction(UploadRecords.ACTION_START);
            startService(uploadRecords);
        } else {
            Log.d(TAG, "No entries to upload to the online database.");
        }
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
        if (UpdateTaxa.isInstanceCreated()) {
            Log.d(TAG, "UpdateTaxa is already running, skipping for now…");
        } else {
            final Intent updateTaxa = new Intent(ActivityLanding.this, UpdateTaxa.class);
            updateTaxa.setAction(UpdateTaxa.ACTION_DOWNLOAD);
            startService(updateTaxa);
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

    protected void alertTokenExpired() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityLanding.this);
        builder.setMessage(getString(R.string.refresh_token_failed))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ignore), (dialog, id) -> {
                    MenuItem item = findViewById(R.id.action_upload);
                    item.setEnabled(false);
                    Objects.requireNonNull(item.getIcon(), "The icon must not be null!").setAlpha(100);
                })
                .setNegativeButton(getString(R.string.exit), (dialog, id) -> {
                    dialog.cancel();
                    ActivityLanding.this.finishAffinity();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private static Menu getMenu() {
        return uploadMenu;
    }

    public void updateMenuIconVisibility() {
        long numberOfItems = App.get().getBoxStore().boxFor(EntryDb.class).count();
        Log.d(TAG, "Should disable buttons? There are " + numberOfItems + " items in the list.");

        // Disable the upload button
        // Enable the upload button
        setMenuIconVisibility(numberOfItems != 0);
    }

    public void setMenuIconVisibility (boolean visible) {
        if (getMenu() != null) {
            uploadMenu.getItem(0).setEnabled(visible);
            if (visible) {
                Objects.requireNonNull(uploadMenu.getItem(0).getIcon()).setAlpha(255);
            } else {
                Objects.requireNonNull(uploadMenu.getItem(0).getIcon()).setAlpha(100);
            }
            uploadMenu.getItem(1).setVisible(visible);
        }
    }

}
