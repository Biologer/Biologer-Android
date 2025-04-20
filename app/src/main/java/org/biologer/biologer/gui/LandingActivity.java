package org.biologer.biologer.gui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.FileManipulation;
import org.biologer.biologer.adapters.StageAndSexLocalization;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.UpdateAnnouncements;
import org.biologer.biologer.network.UpdateLicenses;
import org.biologer.biologer.network.UpdateObservationTypes;
import org.biologer.biologer.network.UpdateTaxa;
import org.biologer.biologer.network.UpdateUnreadNotifications;
import org.biologer.biologer.network.UploadRecords;
import org.biologer.biologer.network.json.RefreshTokenResponse;
import org.biologer.biologer.network.json.TaxaResponse;
import org.biologer.biologer.network.json.UserDataResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.SynonymsDb;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsTranslationDb;
import org.biologer.biologer.sql.UserDb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandingActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Biologer.Landing";

    private DrawerLayout drawer;
    static String how_to_use_network;

    // Define upload menu so that we can hide it if required
    static Menu uploadMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        String database_url = SettingsManager.getDatabaseName();

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

        showLandingFragment();

        // If there is no token, falling back to the login screen
        if (SettingsManager.getAccessToken() == null) {
            Log.d(TAG, getString(R.string.no_login_token_falling_back_to_login_screen));
            Toast.makeText(this, getString(R.string.no_login_token_falling_back_to_login_screen), Toast.LENGTH_LONG).show();
            showUserLoginScreen(false);
        } else {
            // On the first run (after the user came from the login screen) show some help
            if (SettingsManager.isFirstRun()) {
                Log.d(TAG, "This is first run of the program.");
                SettingsManager.setFirstRun(false);
                Intent intent = new Intent(LandingActivity.this, IntroActivity.class);
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
                        TextView textView = findViewById(R.id.list_entries_info_text);
                        textView.setText(R.string.entry_info_first_run);
                        textView.setVisibility(View.VISIBLE);
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "Back button is pressed!");
                backPressed();
            }
        });
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

    private void backPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            Log.d(TAG, "Back button pressed, while LandingFragment is active.");
            getSupportFragmentManager().popBackStack();
            finishAffinity();
        } else {
            Log.d(TAG, "Back button pressed, while there are many fragments opened.");
            getSupportFragmentManager().popBackStack();
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
            Log.d(TAG, "CSV export button clicked.");
            exportCSV();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fallbackToLoginScreen() {
        Log.e(TAG, "Something is wrong, the settings are lost...");
        App.get().deleteAllBoxes();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, getString(R.string.something_is_wrong_falling_back_to_login_screen), Toast.LENGTH_LONG).show();
        showUserLoginScreen(false);
    }

    private void runServices(String database_url) {
        Log.d(TAG, "Running online services");

        // Load database from local assets folder if newer
        int updatedAt = Integer.parseInt(SettingsManager.getTaxaUpdatedAt());
        String assets_timestamp = "1733425371";
        if (updatedAt < Integer.parseInt(assets_timestamp)) {
            Log.i(TAG, "Loading taxa database from Android assets folder. Version: " +
                    updatedAt + "; Available version: " + assets_timestamp);
            loadInternalTaxaDataset(database_url, assets_timestamp);
        } else {
            Log.i(TAG, "Loading taxa from online API. Version: " + updatedAt);
            updateTaxa();
        }

        // Check if token is still valid and refresh if needed
        if (SettingsManager.isMailConfirmed()) {
            Log.d(TAG, "Email is confirmed.");

            long expire_in = Long.parseLong(SettingsManager.getTokenExpire());

            // Refresh the token if it expired
            if (expire_in >= System.currentTimeMillis() / 1000) {
                Log.d(TAG, "Token is OK. It will expire on " + SettingsManager.getTokenExpire());
                // Refresh token 6 months before the expiration
                if (expire_in > ((System.currentTimeMillis() / 1000) + 15778800)) {
                    Log.d(TAG, "There is no need to refresh token now.");
                } else {
                    Log.d(TAG, "Trying to refresh login token.");
                    if (InternetConnection.isConnected(LandingActivity.this)) {
                        RefreshToken(database_url, false);
                    }
                }
            } else {
                Log.d(TAG, "Token expired. Refreshing login token.");
                if (InternetConnection.isConnected(LandingActivity.this)) {
                    RefreshToken(database_url, true);
                } else {
                    alertWarnAndExit(getString(R.string.refresh_token_no_internet));
                }
            }
        } else {
            Log.d(TAG, "Email is not confirmed.");
            if (database_url != null) {
                checkMailConfirmed(database_url);
            }
        }

        // Get the user settings from preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LandingActivity.this);
        how_to_use_network = preferences.getString("auto_download", "wifi");

        // Check if there is network available and run the commands...
        String network_type = InternetConnection.networkType(this);
        if (network_type != null) {
            updateLicenses();
            UpdateObservationTypes.updateObservationTypes(database_url);

            // Check if notifications are enabled and download/upload data
            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Log.d(TAG, "Notifications are enabled.");
                if (shouldDownload(this)) {
                    uploadRecords();

                    // Update announcements
                    long current_time = System.currentTimeMillis() / 1000; // in seconds
                    long last_check = Long.parseLong(SettingsManager.getLastInternetCheckout());
                    if (last_check == 0 || current_time > (last_check + 36000) ) { // don’t get data from veb in the next 10 hours
                        Log.d(TAG, "Announcements should be updated since 10 hours elapsed.");
                        final Intent getAnnouncements = new Intent(LandingActivity.this, UpdateAnnouncements.class);
                        getAnnouncements.putExtra("show_notification", true);
                        startService(getAnnouncements);
                        SettingsManager.setLastInternetCheckout(String.valueOf(current_time));
                    }

                    // Update notifications
                    final Intent update_notifications = new Intent(this, UpdateUnreadNotifications.class);
                    update_notifications.putExtra("download", true);
                    startService(update_notifications);
                }

            } else {
                checkNotificationPermission();
            }

        } else {
            Log.d(TAG, "There is no network available. Application will not be able to get new data from the server.");
        }
    }

    private void loadInternalTaxaDataset(String databaseUrl, String timestamp) {
        Log.i(TAG, "Updating ObjectBox taxa database using local copy from assets.");

        List<String[]> taxa_csv = null;
        List<String[]> taxa_groups_csv = null;
        List<String[]> stages_csv = null;

        if (Objects.equals(databaseUrl, "https://biologer.rs")) {
            Log.d(TAG, "Loading assets file for Serbian Biologer");
            taxa_csv = readCSV("taxa/rs_taxa.csv");
            taxa_groups_csv = readCSV("taxa/rs_groups.csv");
            stages_csv = readCSV("taxa/rs_stages.csv");
        } if (Objects.equals(databaseUrl, "https://biologer.hr")) {
            Log.d(TAG, "Loading assets file for Croatian Biologer");
            taxa_csv = readCSV("taxa/hr_taxa.csv");
            taxa_groups_csv = readCSV("taxa/hr_groups.csv");
            stages_csv = readCSV("taxa/hr_stages.csv");
        } if (Objects.equals(databaseUrl, "https://biologer.ba")) {
            Log.d(TAG, "Loading assets file for Bosnian Biologer");
            taxa_csv = readCSV("taxa/ba_taxa.csv");
            taxa_groups_csv = readCSV("taxa/ba_groups.csv");
            stages_csv = readCSV("taxa/ba_stages.csv");
        } if (Objects.equals(databaseUrl, "https://biologer.me")) {
            Log.d(TAG, "Loading assets file for Montenegrin Biologer");
            taxa_csv = readCSV("taxa/me_taxa.csv");
            taxa_groups_csv = readCSV("taxa/me_groups.csv");
            stages_csv = readCSV("taxa/me_stages.csv");
        } if (Objects.equals(databaseUrl, "https://dev.biologer.org")) {
            Log.d(TAG, "Loading assets file for developmental Biologer");
            taxa_csv = readCSV("taxa/dev_taxa.csv");
            taxa_groups_csv = readCSV("taxa/dev_groups.csv");
            stages_csv = readCSV("taxa/dev_stages.csv");
        }

        if (taxa_csv != null) {
            // Delete old database
            App.get().getBoxStore().boxFor(TaxonDb.class).removeAll();
            App.get().getBoxStore().boxFor(TaxaTranslationDb.class).removeAll();
            App.get().getBoxStore().boxFor(SynonymsDb.class).removeAll();
            App.get().getBoxStore().boxFor(TaxonGroupsDb.class).removeAll();
            App.get().getBoxStore().boxFor(StageDb.class).removeAll();

            TaxonDb[] final_taxa = new TaxonDb[taxa_csv.size() - 1];
            List<TaxaTranslationDb> taxa_translations = new ArrayList<>();
            List<SynonymsDb> taxa_synonyms = new ArrayList<>();

            // NOTE: Skip the first row containing column names (i = 0)!
            for (int i = 1; i < taxa_csv.size(); i++) {
                String[] taxon = taxa_csv.get(i);
                long id = Long.parseLong(taxon[0]);
                String rank = taxon[1];
                String name = taxon[2];
                String author = taxon[3];
                boolean uses_atlas_codes;
                uses_atlas_codes = Objects.equals(taxon[4], "1");
                String translations = taxon[5];
                String stages = taxon[6];
                String groups = taxon[7];
                String synonyms = taxon[8];

                final_taxa[i - 1] = new TaxonDb(id, 0, name, rank, 0, author,
                        false, uses_atlas_codes, null, groups, stages);
                //Log.d(TAG, "Taxon " + final_taxa[i - 1].getLatinName() + " with ID: " + final_taxa[i - 1].getId());

                if (!Objects.equals(translations, "")) {
                    //Log.d(TAG, "Taxon " + final_taxa[i - 1].getLatinName() + " has translations: " + translations);
                    String[] split_translations = translations.split(";");
                    for (int t = 0; t < split_translations.length; t++) {
                        String locale = "en";
                        if (t == 1) {
                            locale = "sr";
                        } else if (t == 2) {
                            locale = "sr-Latn";
                        } else if (t == 3) {
                            locale = "hr";
                        } else if (t == 4) {
                            locale = "ba";
                        } else if (t == 5 ) {
                            locale = "me";
                        }
                        if (!Objects.equals(split_translations[t], "")) {
                            TaxaTranslationDb translation = new TaxaTranslationDb(0, id, locale, split_translations[t], name, "");
                            taxa_translations.add(translation);
                        }
                    }
                }

                if (!Objects.equals(synonyms, "")) {
                    //Log.d(TAG, "Taxon " + final_taxa[i - 1].getLatinName() + " has synonyms: " + synonyms);
                    String[] split_synonyms = synonyms.split(";");
                    for (String splitSynonym : split_synonyms) {
                        SynonymsDb synonym = new SynonymsDb(0, id, splitSynonym);
                        taxa_synonyms.add(synonym);
                    }
                }
            }
            App.get().getBoxStore().boxFor(TaxonDb.class).put(final_taxa);
            TaxaTranslationDb[] final_taxa_translations = new TaxaTranslationDb[taxa_translations.size()];
            taxa_translations.toArray(final_taxa_translations);
            App.get().getBoxStore().boxFor(TaxaTranslationDb.class).put(final_taxa_translations);
            SynonymsDb[] final_taxa_synonyms = new SynonymsDb[taxa_synonyms.size()];
            taxa_synonyms.toArray(final_taxa_synonyms);
            App.get().getBoxStore().boxFor(SynonymsDb.class).put(final_taxa_synonyms);

            TaxonGroupsDb[] final_taxa_groups = new TaxonGroupsDb[taxa_groups_csv.size() - 1];
            List<TaxonGroupsTranslationDb> taxa_groups_translations = new ArrayList<>();
            for (int i = 1; i < taxa_groups_csv.size(); i++) {
                String[] group = taxa_groups_csv.get(i);
                long id = Long.parseLong(group[0]);
                long parentId = Long.parseLong(group[1]);
                String ba = group[2];
                String en = group[3];
                String hr = group[4];
                String me = group[5];
                String sr = group[6];
                String sr_latin = group[7];

                final_taxa_groups[i - 1] = new TaxonGroupsDb(id, parentId, en, "");

                taxa_groups_translations.add(new TaxonGroupsTranslationDb(0, id, "en", en, ""));
                taxa_groups_translations.add(new TaxonGroupsTranslationDb(0, id, "sr", sr, ""));
                taxa_groups_translations.add(new TaxonGroupsTranslationDb(0, id, "sr-Latn", sr_latin, ""));
                taxa_groups_translations.add(new TaxonGroupsTranslationDb(0, id, "hr", hr, ""));
                taxa_groups_translations.add(new TaxonGroupsTranslationDb(0, id, "ba", ba, ""));
                taxa_groups_translations.add(new TaxonGroupsTranslationDb(0, id, "me", me, ""));
            }
            App.get().getBoxStore().boxFor(TaxonGroupsDb.class).put(final_taxa_groups);
            TaxonGroupsTranslationDb[] final_taxa_groups_translations = new TaxonGroupsTranslationDb[taxa_groups_translations.size()];
            taxa_groups_translations.toArray(final_taxa_groups_translations);
            App.get().getBoxStore().boxFor(TaxonGroupsTranslationDb.class).put(final_taxa_groups_translations);

            StageDb[] final_stages = new StageDb[stages_csv.size() - 1];
            for (int i = 1; i < stages_csv.size(); i++) {
                String[] stage = stages_csv.get(i);
                long id = Long.parseLong(stage[0]);
                String name = stage[1];

                final_stages[i - 1] = new StageDb(id, name);
            }
            App.get().getBoxStore().boxFor(StageDb.class).put(final_stages);

            Log.d(TAG, "Database loaded from assets file. Checking if there is new version online.");
            SettingsManager.setTaxaUpdatedAt(timestamp);

            updateTaxa();
        }
    }

    private List<String[]> readCSV(String filename) {
        InputStream inputStream;
        try {
            inputStream = getAssets().open(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        List<String[]> csv;
        try {
            csv = new CSVReader(reader).readAll();
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
        return csv;
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
                            TextView textView_confirmEmail = findViewById(R.id.list_entries_email_not_confirmed);
                            textView_confirmEmail.setVisibility(View.GONE);
                        } else {
                            updateGuiOnMailConfirmed(false);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDataResponse> call, @NonNull Throwable t) {
                Toast.makeText(LandingActivity.this, getString(R.string.cannot_connect_server), Toast.LENGTH_LONG).show();
                updateGuiOnMailConfirmed(false);
            }
        });
    }

    private void updateGuiOnMailConfirmed(boolean mail_confirmed) {
        TextView textView_confirmEmail = findViewById(R.id.list_entries_email_not_confirmed);
        FloatingActionButton floatingActionButton = findViewById(R.id.float_button_new_entry);
        floatingActionButton.setEnabled(mail_confirmed);
        if (mail_confirmed) {
            textView_confirmEmail.setVisibility(View.GONE);
            floatingActionButton.setAlpha(1f);
        } else {
            textView_confirmEmail.setVisibility(View.VISIBLE);
            floatingActionButton.setAlpha(0.25f);
        }
    }

    // Setting tokenExpired to true will send user to login screen, but without option
    // to choose database. This is used only to refresh expired token.
    private void showUserLoginScreen(boolean tokenExpired) {
        Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
        if (tokenExpired) {
            SettingsManager.deleteAccessToken();
            intent.putExtra("refreshToken", "yes");
        }
        startActivity(intent);
    }

    private void RefreshToken(String database_name, boolean warn_user) {
        String refreshToken = SettingsManager.getRefreshToken();
        String rsKey = BuildConfig.BiologerRS_Key;
        String hrKey = BuildConfig.BiologerHR_Key;
        String baKey = BuildConfig.BiologerBA_Key;
        String meKey = BuildConfig.BiologerME_Key;
        String devKey = BuildConfig.BiologerDEV_Key;

        Call<RefreshTokenResponse> refresh_call = null;

        if (database_name.equals("https://biologer.rs")) {
            Log.d(TAG, "Serbian database selected.");
            refresh_call = RetrofitClient.getService(database_name).refresh("refresh_token", "2", rsKey, refreshToken, "*");
        }
        if (database_name.equals("https://biologer.hr")) {
            Log.d(TAG, "Croatian database selected.");
            refresh_call = RetrofitClient.getService(database_name).refresh("refresh_token", "2", hrKey, refreshToken, "*");
        }
        if (database_name.equals("https://biologer.ba")) {
            Log.d(TAG, "Bosnian database selected.");
            refresh_call = RetrofitClient.getService(database_name).refresh("refresh_token", "2", baKey, refreshToken, "*");
        }
        if (database_name.equals("https://biologer.me")) {
            Log.d(TAG, "Montenegrin database selected.");
            refresh_call = RetrofitClient.getService(database_name).refresh("refresh_token", "2", meKey, refreshToken, "*");
        }
        if (database_name.equals("https://dev.biologer.org")) {
            Log.d(TAG, "Developmental database selected.");
            refresh_call = RetrofitClient.getService(database_name).refresh("refresh_token", "6", devKey, refreshToken, "*");
        }

        Log.d(TAG, "Logging into " + database_name + " using refresh token.");

        if (refresh_call != null) {
            refresh_call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<RefreshTokenResponse> call, @NonNull Response<RefreshTokenResponse> response) {
                    if (response.code() == 401) {
                        Log.e(TAG, "Error 401: It looks like the refresh token has expired.");
                        Toast.makeText(LandingActivity.this, getString(R.string.both_login_and_refresh_tokens_expired), Toast.LENGTH_LONG).show();
                        showUserLoginScreen(true);
                    }
                    if (response.isSuccessful()) {
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
                        if (warn_user) {
                            alertTokenExpired();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<RefreshTokenResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Cannot get response from the server (refresh token)" + t);
                    if (warn_user) {
                        alertTokenExpired();
                    }
                }
            });
        }
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
                            .setNegativeButton(R.string.ignore, (DialogInterface dialog, int id) -> dialog.cancel());
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
            String updated_at = SettingsManager.getTaxaUpdatedAt();
            String skip_this = SettingsManager.getSkipTaxaDatabaseUpdate();
            String timestamp = updated_at;
            if (Long.parseLong(skip_this) > Long.parseLong(updated_at)) {
                timestamp = skip_this;
            }

            String database = SettingsManager.getDatabaseName();
            int finalTimestamp = Integer.parseInt(timestamp);
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
                    Toast.makeText(LandingActivity.this, getString(R.string.database_connect_error), Toast.LENGTH_LONG).show();
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
        if (getLoggedUser() == null) {
            Toast.makeText(LandingActivity.this, getString(R.string.missing_user_data), Toast.LENGTH_LONG).show();
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        // Hide the info text if user moves away from landing fragment
        TextView textView = findViewById(R.id.list_entries_info_text);
        TextView textView1 = findViewById(R.id.list_entries_email_not_confirmed);
        textView.setVisibility(View.GONE);
        textView1.setVisibility(View.GONE);

        if (id == R.id.nav_list) {
            // Show the info text once back to the list
            if (!SettingsManager.getEntryOpen()) {
                textView.setVisibility(View.VISIBLE);
            }
            if (!SettingsManager.isMailConfirmed()) {
                textView1.setVisibility(View.VISIBLE);
            }
            showLandingFragment();
        }
        if (id == R.id.nav_help) {
            startActivity(new Intent(LandingActivity.this, IntroActivity.class));
        }
        if (id == R.id.nav_setup) {
            showSetupFragment();
        }
        if (id == R.id.nav_logout) {
            showLogoutFragment();
        }
        if (id == R.id.nav_about) {
            showAboutFragment();
        }
        if (id == R.id.nav_notifications) {
            startActivity(new Intent(LandingActivity.this, NotificationsActivity.class));
        }
        if (id == R.id.nav_announcements) {
            showAnnouncementsFragment();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLandingFragment() {
        Log.d(TAG, "Showing LandingFragment");
        Fragment landingFragment;
        landingFragment = getSupportFragmentManager().findFragmentByTag("LANDING_FRAGMENT");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (landingFragment != null) {
            fragmentTransaction.replace(R.id.content_frame, landingFragment, "LANDING_FRAGMENT");
        } else {
            landingFragment = new LandingFragment();
            fragmentTransaction.add(R.id.content_frame, landingFragment, "LANDING_FRAGMENT");
        }
        fragmentTransaction.addToBackStack("Landing fragment");
        fragmentTransaction.commit();
    }

    private void showAboutFragment() {
        Log.d(TAG, "User clicked about icon.");
        Fragment aboutFragment;
        aboutFragment = getSupportFragmentManager().findFragmentByTag("ABOUT_FRAGMENT");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (aboutFragment != null) {
            fragmentTransaction.replace(R.id.content_frame, aboutFragment, "ABOUT_FRAGMENT");
        } else {
            aboutFragment = new AboutFragment();
            fragmentTransaction.add(R.id.content_frame, aboutFragment, "ABOUT_FRAGMENT");
        }
        fragmentTransaction.addToBackStack("About fragment");
        fragmentTransaction.commit();
    }

    private void showAnnouncementsFragment() {
        Log.d(TAG, "User clicked announcements icon.");
        Fragment announcementsFragment;
        announcementsFragment = getSupportFragmentManager().findFragmentByTag("ANNOUNCEMENTS_FRAGMENT");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (announcementsFragment != null) {
            fragmentTransaction.replace(R.id.content_frame, announcementsFragment, "ANNOUNCEMENTS_FRAGMENT");
        } else {
            announcementsFragment = new AnnouncementsFragment();
            fragmentTransaction.add(R.id.content_frame, announcementsFragment, "ANNOUNCEMENTS_FRAGMENT");
        }
        fragmentTransaction.addToBackStack("Announcements fragment");
        fragmentTransaction.commit();
    }

    private void showLogoutFragment() {
        Log.d(TAG, "User clicked logout icon.");
        Fragment logoutFragment;
        logoutFragment = getSupportFragmentManager().findFragmentByTag("LOGOUT_FRAGMENT");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (logoutFragment != null) {
            fragmentTransaction.replace(R.id.content_frame, logoutFragment, "LOGOUT_FRAGMENT");
        } else {
            logoutFragment = new LogoutFragment();
            fragmentTransaction.add(R.id.content_frame, logoutFragment, "LOGOUT_FRAGMENT");
        }
        fragmentTransaction.addToBackStack("Logout fragment");
        fragmentTransaction.commit();
    }

    private void showSetupFragment() {
        Log.d(TAG, "User clicked preferences icon.");
        Fragment preferencesFragment;
        preferencesFragment = getSupportFragmentManager().findFragmentByTag("PREFERENCES_FRAGMENT");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (preferencesFragment != null) {
            fragmentTransaction.replace(R.id.content_frame, preferencesFragment, "PREFERENCES_FRAGMENT");
        } else {
            preferencesFragment = new PreferencesFragment();
            fragmentTransaction.add(R.id.content_frame, preferencesFragment, "PREFERENCES_FRAGMENT");
        }
        fragmentTransaction.addToBackStack("Preferences fragment");
        fragmentTransaction.commit();
    }

    private void exportCSV() {

        String filename = "Export_" + new SimpleDateFormat("yyMMddss", Locale.getDefault()).format(new Date());

        Uri uri = FileManipulation.newExternalDocumentFile(this, filename, ".csv");
        OutputStream output = null;
        try {
            if (uri != null) {
                output = getContentResolver().openOutputStream(uri);
            }
            else {
                Log.d(TAG, "URI is null!");
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(LandingActivity.this, getString(R.string.file_not_found) + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "File not found: " + e);
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

        ArrayList<EntryDb> entries = (ArrayList<EntryDb>) App.get().getBoxStore().boxFor(EntryDb.class).getAll();
        String u = getUserName();

        for (int i = 0; i < entries.size(); i++) {
            EntryDb entryDb = entries.get(i);
            String[] row = {
                    entryDb.getTaxonSuggestion(),
                    entryDb.getYear(),
                    entryDb.getMonth(),
                    entryDb.getDay(),
                    String.format(Locale.ENGLISH, "%.6f", entryDb.getLattitude()),
                    String.format(Locale.ENGLISH, "%.6f", entryDb.getLongitude()),
                    String.format(Locale.ENGLISH, "%.0f", entryDb.getElevation()),
                    String.format(Locale.ENGLISH, "%.0f", entryDb.getAccuracy()),
                    entryDb.getLocation(),
                    entryDb.getTime(),
                    entryDb.getComment(),
                    translateFoundDead(entryDb.getDeadOrAlive()),
                    entryDb.getCauseOfDeath(),
                    u,
                    u,
                    StageAndSexLocalization.getSexLocale(this, entryDb.getSex()),
                    removeNullInteger(entryDb.getNoSpecimens()),
                    entryDb.getProjectId(),
                    entryDb.getHabitat(),
                    entryDb.getFoundOn(),
                    StageAndSexLocalization.getStageLocaleFromID(this, entryDb.getStage()),
                    entryDb.getTaxonSuggestion(),
                    getString(R.string.dataset),
                    translateLicence(entryDb.getDataLicence()),
                    translateLicence(String.valueOf(entryDb.getImageLicence())),
                    removeNullLong(entryDb.getAtlasCode())
            };
            writer.writeNext(row);
        }

        try {
            writer.close();
            Toast.makeText(LandingActivity.this, getString(R.string.export_to_csv_success) + filename, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(LandingActivity.this, getString(R.string.io_error) + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "IO Error: " + e);
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
        return switch (data_license) {
            case "10" -> getString(R.string.export_licence_10);
            case "11" -> getString(R.string.export_licence_11);
            case "20" -> getString(R.string.export_licence_20);
            case "30" -> getString(R.string.export_licence_30);
            case "40" -> getString(R.string.export_licence_40);
            default -> "";
        };
    }

    private String translateFoundDead(String alive) {
        if (alive.equals("true")) {
            return getString(R.string.no);
        } else {
            return getString(R.string.yes);
        }
    }

    private void uploadRecords() {
        if (!App.get().getBoxStore().boxFor(EntryDb.class).getAll().isEmpty()) {
            Log.d(TAG, "Uploading entries to the online database.");
            final Intent uploadRecords = new Intent(LandingActivity.this, UploadRecords.class);
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
            final Intent updateTaxa = new Intent(LandingActivity.this, UpdateTaxa.class);
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(LandingActivity.this);
        builder.setMessage(getString(R.string.refresh_token_failed))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ignore), (dialog, id) -> {
                    MenuItem item = findViewById(R.id.action_upload);
                    item.setEnabled(false);
                    Objects.requireNonNull(item.getIcon(), "The icon must not be null!").setAlpha(100);
                })
                .setNegativeButton(getString(R.string.exit), (dialog, id) -> {
                    dialog.cancel();
                    LandingActivity.this.finishAffinity();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Get the data from GreenDao database
    private UserDb getLoggedUser() {
        // Get the user data from a GreenDao database
        List<UserDb> userdata_list = App.get().getBoxStore().boxFor(UserDb.class).getAll();
        // If there is no user data we should logout the user
        if (userdata_list == null || userdata_list.isEmpty()) {
            return null;
        } else {
            return userdata_list.get(0);
        }
    }

    private String getUserName() {
        UserDb userdata = getLoggedUser();
        if (userdata != null) {
            return userdata.getUsername();
        } else {
            return getString(R.string.not_logged_in);
        }
    }

    private String getUserEmail() {
        UserDb userdata = getLoggedUser();
        if (userdata != null) {
            return userdata.getEmail();
        } else {
            return getString(R.string.not_logged_in);
        }
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
