package org.biologer.biologer.gui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.SpeciesCount;
import org.biologer.biologer.adapters.TaxaListAdapter;
import org.biologer.biologer.adapters.TimedCountAdapter;
import org.biologer.biologer.adapters.TimedCountViewModel;
import org.biologer.biologer.network.RetrofitWeatherClient;
import org.biologer.biologer.network.json.WeatherResponse;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.LocationResultCallback;
import org.biologer.biologer.services.LocationTrackingService;
import org.biologer.biologer.services.TaxonSearchHelper;
import org.biologer.biologer.services.WeatherUtils;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.ObservationTypesDb;
import org.biologer.biologer.sql.ObservationTypesDb_;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonDb_;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsDb_;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.sql.UserDb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityTimedCount extends AppCompatActivity implements FragmentTimedCountEntries.OnItemChangeListener {

    private static final String TAG = "Biologer.TimedCount";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    TextView elapsed_time;
    LinearLayout timerLayout, additionalDataLayout;
    private CountDownTimer countDownTimer;
    private long timeRemaining;
    private boolean isTimerRunning = false;
    ImageView pauseTimerImage;
    AutoCompleteTextView autoCompleteTextView_speciesName;
    private TaxonSearchHelper taxonSearchHelper;
    TaxonDb selectedTaxon = null;
    boolean taxonSelectedFromTheList = false;
    RecyclerView recyclerView;
    private FusedLocationProviderClient fusedLocationClient;
    private TimedCountAdapter timedCountAdapter;
    private final ArrayList<SpeciesCount> speciesCounts = new ArrayList<>();
    String start_time, end_time;
    int count_duration_minutes = 0;
    Spinner spinnerTaxaGroup;
    long selectedTaxaGroupID = 0;
    boolean save_enabled = false;
    private boolean is_fragment_visible = false;
    private TimedCountViewModel timedCountViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timed_count);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.timed_count_title);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        // onBackPressed we should warn user not to quit the count
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int no_fragments = getSupportFragmentManager().getBackStackEntryCount();
                Log.d(TAG, "There are " + no_fragments + " active Fragments.");
                if (no_fragments == 0) {
                    showExitConfirmationDialog();
                } else if (no_fragments == 1) {
                    // Fragment is visible. Hide it and show the main layout.
                    getSupportFragmentManager().popBackStack();
                    findViewById(R.id.timed_count_fragment).setVisibility(View.GONE);
                    findViewById(R.id.timed_count_main_layout).setVisibility(View.VISIBLE);
                    // Enable save button
                    is_fragment_visible = false;
                    invalidateOptionsMenu();
                } else {
                    getSupportFragmentManager().popBackStack();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        timedCountViewModel = new ViewModelProvider(this).get(TimedCountViewModel.class);
        timedCountViewModel.setTimedCountId(getUniqueTimedCountID());
        timedCountViewModel.getTemperatureData().observe(this, newTemperature -> {
            Log.d(TAG, "Temperature updated: " + newTemperature);
            TextView textViewTemperature = findViewById(R.id.timed_count_text_temperature);
            textViewTemperature.setText(String.valueOf(newTemperature));
        });
        timedCountViewModel.getCloudinessData().observe(this, newCloudiness -> {
            Log.d(TAG, "Cloudiness updated: " + newCloudiness);
            TextView textViewCloudiness = findViewById(R.id.timed_count_text_cloudiness);
            textViewCloudiness.setText(String.valueOf(newCloudiness));
        });

        elapsed_time = findViewById(R.id.time_elapsed);
        timerLayout = findViewById(R.id.timed_count_timer);
        additionalDataLayout = findViewById(R.id.timed_count_additional_data);
        additionalDataLayout.setOnClickListener(v -> displayAdditionalDetailsFragment());
        pauseTimerImage = findViewById(R.id.pause_timer_image);

        recyclerView = findViewById(R.id.recycled_view_timed_counts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        timedCountAdapter = new TimedCountAdapter(speciesCounts);
        timedCountAdapter.setOnItemClickListener(new TimedCountAdapter.OnItemClickListener() {
            @Override
            public void onPlusClick(SpeciesCount species) {
                if (species.getSpeciesID() != null) {
                    Box<TaxonDb> taxonDataBox = App.get().getBoxStore().boxFor(TaxonDb.class);
                    Query<TaxonDb> query = taxonDataBox
                            .query(TaxonDb_.id.equal(species.getSpeciesID()))
                            .build();
                    TaxonDb taxon = query.find(0, 1).get(0);
                    query.close();

                    taxon.setLatinName(TaxonSearchHelper.getLocalisedLatinName(taxon.getId()));
                    addNewObjectBoxEntry(taxon);
                } else {
                    TaxonDb taxon = new TaxonDb(0, 0, species.getSpeciesName(),
                            "", 0, "", false, false, "", "", "");
                    addNewObjectBoxEntry(taxon);
                }

            }

            @Override
            public void onSpeciesClick(SpeciesCount species) {
                Long species_id = species.getSpeciesID();
                if (species_id != null) {
                    Log.d(TAG, "Species with ID " + species_id + " is clicked.");
                    timedCountViewModel.setTaxonId(species_id);
                    displayCountEntriesFragment();
                } else {
                    Log.d(TAG, "Species without ID is clicked!");
                    Toast.makeText(ActivityTimedCount.this,
                            R.string.can_not_edit_species_with_custom_names,
                            Toast.LENGTH_LONG).show();
                }

            }
        });

        recyclerView.setAdapter(timedCountAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check the location permission and start all other stuff...
        checkLocationPermission();

        // Fill in the drop down menu with list of taxa
        TaxaListAdapter adapter = new TaxaListAdapter(this,
                R.layout.taxa_dropdown_list,
                new ArrayList<>());
        autoCompleteTextView_speciesName = findViewById(R.id.textview_list_of_taxa_time_count);
        autoCompleteTextView_speciesName.setAdapter(adapter);
        autoCompleteTextView_speciesName.setThreshold(2);
        taxonSearchHelper = new TaxonSearchHelper(this);

        autoCompleteTextView_speciesName.setOnItemClickListener((parent,
                                                                 view,
                                                                 position,
                                                                 id) -> {
            TaxonDb taxonDb = (TaxonDb) parent.getItemAtPosition(position);
            autoCompleteTextView_speciesName.setText(taxonDb.getLatinName());
            selectedTaxon = taxonDb;
            taxonSelectedFromTheList = true;
            if (timedCountAdapter.hasSpeciesWithID(selectedTaxon.getId())) {
                timedCountAdapter.addToSpeciesCount(selectedTaxon.getId());
            } else {
                SpeciesCount new_species = new SpeciesCount(selectedTaxon.getLatinName(),
                        selectedTaxon.getId(), 1);
                speciesCounts.add(new_species);
                timedCountAdapter.notifyItemInserted(speciesCounts.size() - 1);
            }
            autoCompleteTextView_speciesName.setText("");

            addNewObjectBoxEntry(selectedTaxon);

        });

        // When user type taxon name...
        autoCompleteTextView_speciesName.addTextChangedListener(new TextWatcher() {
            final Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable;
            String entered_name = null;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                entered_name = String.valueOf(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                final String changed_entered_name = String.valueOf(s);
                if (changed_entered_name.equals(entered_name)) {
                    Log.d(TAG, "TextChanged call ignored, previous text and entered text is the same.");
                } else {
                    // Try to get the latin name from ObjectBox database
                    handler.removeCallbacks(runnable);
                    runnable = () -> {

                        // Remove atlas code and stage for taxa not selected from the list
                        if (!taxonSelectedFromTheList) {
                            selectedTaxon = null;
                        }
                        taxonSelectedFromTheList = false;

                        List<TaxonDb> allTaxaLists;
                        if (selectedTaxaGroupID == 0) {
                            allTaxaLists = taxonSearchHelper.searchTaxa(s.toString());
                        } else {
                            allTaxaLists = taxonSearchHelper.searchTaxaByGroup(s.toString(), selectedTaxaGroupID);
                        }

                        // Add the Query to the drop down list (adapter)
                        TaxaListAdapter adapter1 =
                                new TaxaListAdapter(ActivityTimedCount.this,
                                        R.layout.taxa_dropdown_list,
                                        allTaxaLists);
                        autoCompleteTextView_speciesName.setAdapter(adapter1);
                        adapter1.notifyDataSetChanged();
                    };
                    handler.postDelayed(runnable, 300);
                }
            }
        });

        // Activate the field for species name and show the keyboard.
        autoCompleteTextView_speciesName.requestFocus();
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void addNewObjectBoxEntry(TaxonDb taxon) {
        fetchLatestLocation(new LocationResultCallback() {
            @Override
            public void onLocationSuccess(Location location) {
                Log.d(TAG, "Location for the entry data is: " + location.getLatitude() + ", " + location.getLongitude());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ActivityTimedCount.this);

                // Always add Observation Type for observed specimen
                Box<ObservationTypesDb> observationTypesDataBox = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
                Query<ObservationTypesDb> query = observationTypesDataBox
                        .query(ObservationTypesDb_.slug.equal("observed"))
                        .build();
                ObservationTypesDb observationTypesData = query.findFirst();
                query.close();

                String observed_id = null;
                if (observationTypesData != null) {
                    observed_id = "[" + observationTypesData.getObservationId() + "]";
                }

                String stages = taxon.getStages();
                Long stageId = null;
                if (stages != null) {
                    if (!stages.isEmpty()) {
                        Log.d(TAG, "Taxon contains stages.");
                        String[] all_stages = stages.split(";");

                        Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
                        Query<StageDb> queryStage = stageBox
                                .query(StageDb_.name.equal("adult"))
                                .build();
                        StageDb stage = queryStage.findFirst();
                        queryStage.close();

                        if (stage != null) {
                            String s = String.valueOf(stage.getId());
                            if (Arrays.asList(all_stages).contains(s)) {
                                Log.d(TAG, "Taxon contains adult stage.");
                                stageId = stage.getId();
                            }
                        }
                    }
                }

                List<UserDb> userData = App.get().getBoxStore().boxFor(UserDb.class).getAll();
                String data_license = "0";
                int image_license = 0;
                if (userData != null) {
                    if (!userData.isEmpty()) {
                        data_license = String.valueOf(userData.get(0).getDataLicense());
                        image_license = userData.get(0).getImageLicense();
                    }
                }

                EntryDb entryDb = new EntryDb(0,
                        taxon.getId(),
                        timedCountViewModel.getTimedCountId().getValue(),
                        taxon.getLatinName(),
                        DateHelper.getCurrentYear(),
                        DateHelper.getCurrentMonth(),
                        DateHelper.getCurrentDay(),
                        "",
                        1,
                        "",
                        stageId,
                        null,
                        "true",
                        "",
                        location.getLatitude(),
                        location.getLongitude(),
                        (double) location.getAccuracy(),
                        location.getAltitude(),
                        preferences.getString("location_name", ""),
                        null,
                        null,
                        null,
                        preferences.getString("project_name", ""),
                        "",
                        data_license,
                        image_license,
                        DateHelper.getCurrentTime(),
                        "",
                        observed_id);
                Box<EntryDb> entry = App.get().getBoxStore().boxFor(EntryDb.class);
                entry.put(entryDb);
            }

            @Override
            public void onLocationFailure(String errorMessage) {
                Toast.makeText(ActivityTimedCount.this, "Location not available. " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getUniqueTimedCountID() {
        Box<EntryDb> entries = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = entries
                .query(EntryDb_.timedCoundId.notNull())
                .build();
        List<EntryDb> entriesData = query.find();
        query.close();

        Integer maxTimedCountId = null;
        for (EntryDb entry : entriesData) {
            Integer currentId = entry.getTimedCoundId();
            if (currentId != null) {
                if (maxTimedCountId == null || currentId > maxTimedCountId) {
                    maxTimedCountId = currentId;
                }
            }
        }

        if (maxTimedCountId != null) {
            return ++maxTimedCountId;
        } else {
            return 0;
        }
    }

    private void displayAdditionalDetailsFragment() {
        Log.d(TAG, "Showing additionalData fragment");
        // Hide the save button when the fragment is visible
        is_fragment_visible = true;
        invalidateOptionsMenu();

        LinearLayout linearLayoutMain = findViewById(R.id.timed_count_main_layout);
        linearLayoutMain.setVisibility(View.GONE);
        FragmentContainerView fragmentContainerView = findViewById(R.id.timed_count_fragment);
        fragmentContainerView.setVisibility(View.VISIBLE);

        FragmentTimedCountAdditionalData additionalDataFragment = new FragmentTimedCountAdditionalData();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("TIMED_COUNT_ADDITIONAL_DATA");
        fragmentTransaction.replace(R.id.timed_count_fragment, additionalDataFragment);
        fragmentTransaction.commit();
    }

    private void displayCountEntriesFragment() {
        Log.d(TAG, "Showing entries fragment for the species");
        // Hide the save button when the fragment is visible
        is_fragment_visible = true;
        invalidateOptionsMenu();

        LinearLayout linearLayoutMain = findViewById(R.id.timed_count_main_layout);
        linearLayoutMain.setVisibility(View.GONE);
        FragmentContainerView fragmentContainerView = findViewById(R.id.timed_count_fragment);
        fragmentContainerView.setVisibility(View.VISIBLE);

        FragmentTimedCountEntries timedCountEntries = new FragmentTimedCountEntries();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("TIMED_COUNT_ENTRIES");
        fragmentTransaction.replace(R.id.timed_count_fragment, timedCountEntries);
        fragmentTransaction.commit();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(ActivityTimedCount.this)
                .setTitle(R.string.exit_time_count)
                .setMessage(R.string.confirmation_exit_time_count)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Intent serviceIntent = new Intent(ActivityTimedCount.this, LocationTrackingService.class);
                    stopService(serviceIntent);
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register the BroadcastReceiver when the activity starts
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationTrackingService.ACTION_LOCATION_UPDATE);
        filter.addAction(LocationTrackingService.ACTION_ROUTE_RESULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }

    // Add Save button in the right part of the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.entry_activity_menu, menu);
        return true;
    }

    // Customize Save item to enable if when needed
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_save_entry);

        if (is_fragment_visible) {
            item.setVisible(false);
        } else {
            item.setVisible(true);
            if (save_enabled) {
                item.setEnabled(true);
                Objects.requireNonNull(item.getIcon()).setAlpha(255);
            } else {
                item.setEnabled(false);
                Objects.requireNonNull(item.getIcon()).setAlpha(30);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (id == R.id.action_save_entry) {
            saveTimedCount();
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTimedCount() {
        TimedCountDb timedCountDb = new TimedCountDb(0,
                null,
                start_time,
                end_time,
                count_duration_minutes,
                timedCountViewModel.getCloudinessData().getValue(),
                timedCountViewModel.getPressureData().getValue(),
                timedCountViewModel.getHumidityData().getValue(),
                timedCountViewModel.getTemperatureData().getValue(),
                timedCountViewModel.getWindDirectionData().getValue(),
                timedCountViewModel.getWindSpeedData().getValue(),
                timedCountViewModel.getHabitatData().getValue(),
                timedCountViewModel.getCommentData().getValue(),
                String.valueOf(selectedTaxaGroupID));
    }

    // Create AlertDialog to setup before starting the timed count.
    private void setupCount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setup_your_timed_count);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        LinearLayout minutesLayout = new LinearLayout(this);
        minutesLayout.setPadding(0, 36, 0, 0);
        minutesLayout.setMinimumHeight(48);
        minutesLayout.setOrientation(LinearLayout.HORIZONTAL);

        final TextView textViewMinutes = new TextView(this);
        textViewMinutes.setText(R.string.minutes);
        textViewMinutes.setGravity(Gravity.CENTER);
        textViewMinutes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textViewMinutes.setPadding(24, 24, 24, 24);
        minutesLayout.addView(textViewMinutes);

        final ImageView minus = new ImageView(this);
        minus.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_minus));
        minus.setLayoutParams(params);
        minus.setPadding(16, 24, 16, 0);
        minutesLayout.addView(minus);

        final EditText editTextMinutes = new EditText(this);
        editTextMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextMinutes.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        editTextMinutes.setWidth(200);
        editTextMinutes.setText(String.valueOf(15));
        editTextMinutes.setHint(R.string.min);
        minutesLayout.addView(editTextMinutes);

        final ImageView plus = new ImageView(this);
        plus.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_plus));
        plus.setPadding(16, 24, 16, 0);
        minutesLayout.addView(plus);

        layout.addView(minutesLayout);

        final TextView textViewGroups = new TextView(this);
        textViewGroups.setText(R.string.select_group_for_timed_count);
        textViewGroups.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textViewGroups.setPadding(24, 24, 24, 24);
        layout.addView(textViewGroups);

        spinnerTaxaGroup = new Spinner(this);
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.butterflies));
        options.add(getString(R.string.birds));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaxaGroup.setAdapter(spinnerAdapter);
        layout.addView(spinnerTaxaGroup);

        builder.setView(layout);

        builder.setPositiveButton(R.string.start_counting, (dialog, which) -> {
            String minutesString = editTextMinutes.getText().toString();
            if (!minutesString.isEmpty()) {
                count_duration_minutes = Integer.parseInt(minutesString);
            }

            // Get the taxa group ID for selected taxa
            String selectedTaxa = (String) spinnerTaxaGroup.getSelectedItem();
            Log.d("Dialog", "Minutes: " + count_duration_minutes + ", Selected Option: " + selectedTaxa);
            Box<TaxonGroupsDb> taxonGroupsDataBox = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
            if (selectedTaxa.equals(getString(R.string.butterflies))) {
                Query<TaxonGroupsDb> query = taxonGroupsDataBox
                        .query(TaxonGroupsDb_.name.contains("butterfly", QueryBuilder.StringOrder.CASE_INSENSITIVE)
                                .or(TaxonGroupsDb_.name.contains("butterflies", QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                        .build();
                List<TaxonGroupsDb> listParents = query.find();
                query.close();
                if (!listParents.isEmpty()) {
                    selectedTaxaGroupID = listParents.get(0).getId();
                }
            }
            if (selectedTaxa.equals(getString(R.string.birds))) {
                Query<TaxonGroupsDb> query = taxonGroupsDataBox
                        .query(TaxonGroupsDb_.name.contains("bird", QueryBuilder.StringOrder.CASE_INSENSITIVE))
                        .build();
                List<TaxonGroupsDb> listParents = query.find();
                query.close();
                if (!listParents.isEmpty()) {
                    selectedTaxaGroupID = listParents.get(0).getId();
                }
            }
            Log.d(TAG, "Selected taxa (" + selectedTaxa + ") has ID: " + selectedTaxaGroupID);

            String api_key = BuildConfig.OpenWeather_KEY;
            fetchLatestLocation(new LocationResultCallback() {
                @Override
                public void onLocationSuccess(Location location) {
                    Log.d(TAG, "Location for the weather data is: " + location.getLatitude() + ", " +  location.getLongitude());
                    RetrofitWeatherClient.getClient().getCurrentWeather(
                            String.valueOf(location.getLatitude()),
                            String.valueOf(location.getLongitude()),
                            api_key,
                            "metric").enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "Weather response successful");
                                timedCountViewModel.setTemperatureData(response.body().getMain().getTemp());
                                timedCountViewModel.setCloudinessData(response.body().getClouds().getCloudiness());
                                timedCountViewModel.setPressureData(response.body().getMain().getPressure());
                                timedCountViewModel.setHumidityData(response.body().getMain().getHumidity());
                                timedCountViewModel.setWindSpeedData(
                                        WeatherUtils.getBeaufortScale(response.body().getWind().getSpeed()));
                                timedCountViewModel.setWindDirectionData(
                                        WeatherUtils.getWindDirection(response.body().getWind().getDeg()));

                                Log.d(TAG,"Temperature: " + timedCountViewModel.getTemperatureData().getValue() +
                                        ", Clouds: " + timedCountViewModel.getCloudinessData().getValue() +
                                        ", Pressure: " + timedCountViewModel.getPressureData().getValue() +
                                        ", Humidity: " + timedCountViewModel.getHumidityData().getValue() +
                                        ", Wind speed: " + timedCountViewModel.getWindSpeedData().getValue() +
                                        ", Wind direction: " + timedCountViewModel.getWindDirectionData().getValue());
                            } else {
                                Log.d(TAG, "Weather response not successful: " + response.code()  + "; Message: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                            Log.d(TAG, "Weather response network error: " + t.getMessage());
                        }
                    });
                }

                @Override
                public void onLocationFailure(String errorMessage) {
                    // Handle the location error, e.g., show a Toast
                    Toast.makeText(ActivityTimedCount.this, "Weather data not available. " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });

            startCount();

        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            Intent serviceIntent = new Intent(ActivityTimedCount.this, LocationTrackingService.class);
            stopService(serviceIntent);
            dialog.dismiss();
            finish();
        });

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            int initialValue = Integer.parseInt(editTextMinutes.getText().toString());
            positiveButton.setEnabled(initialValue > 1);

            minus.setOnClickListener(view -> {
                int min_orig = Integer.parseInt(editTextMinutes.getText().toString());
                int min_new = min_orig - 1;
                editTextMinutes.setText(String.valueOf(min_new));
            });

            plus.setOnClickListener(view -> {
                if (TextUtils.isEmpty(editTextMinutes.getText())) {
                    editTextMinutes.setText(String.valueOf(1));
                } else {
                    int min_orig = Integer.parseInt(editTextMinutes.getText().toString());
                    int min_new = min_orig + 1;
                    editTextMinutes.setText(String.valueOf(min_new));
                }
            });

            editTextMinutes.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    String value = editable.toString();
                    if (!TextUtils.isEmpty(value)) {
                        try {
                            int min = Integer.parseInt(value);
                            if (min <= 1) {
                                minus.setEnabled(false);
                                minus.setImageAlpha(100);
                                editTextMinutes.setError(null);
                                positiveButton.setEnabled(true);
                                if (min < 1) {
                                    editTextMinutes.setError("Must be greater than 0.");
                                    positiveButton.setEnabled(false);
                                }
                            } else {
                                minus.setEnabled(true);
                                minus.setImageAlpha(255);
                                editTextMinutes.setError(null);
                                positiveButton.setEnabled(true);
                            }
                        } catch (NumberFormatException e) {
                            positiveButton.setEnabled(false);
                            editTextMinutes.setError("Number format is incorrect.");
                        }
                    } else {
                        minus.setEnabled(false);
                        minus.setImageAlpha(100);
                        editTextMinutes.setError(null);
                        positiveButton.setEnabled(false);
                    }
                }
            });
        });

        dialog.show();

    }

    private void startCount() {
        Log.d(TAG, "Starting the timed count and location service.");
        // Record location
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Setup timer
        long millisInFuture = count_duration_minutes * 60 * 1000L; // minutes to milliseconds
        long countDownInterval = 1000L; // 1 second
        timeRemaining = millisInFuture;
        startOrResumeTimer(timeRemaining, countDownInterval);

        timerLayout.setOnClickListener(view -> {
            if (isTimerRunning) {
                pauseTimer();
                pauseTimerImage.setImageResource(R.drawable.ic_play);
                elapsed_time.setText(R.string.paused);
                Intent pauseIntent = new Intent(this, LocationTrackingService.class);
                pauseIntent.setAction(LocationTrackingService.ACTION_PAUSE);
                startService(pauseIntent);
            } else {
                resumeTimer(countDownInterval);
                pauseTimerImage.setImageResource(R.drawable.ic_pause);
                Intent resumeIntent = new Intent(this, LocationTrackingService.class);
                resumeIntent.setAction(LocationTrackingService.ACTION_RESUME);
                startService(resumeIntent);
            }
        });

        start_time = DateHelper.getCurrentTime();

    }

    private void fetchLatestLocation(LocationResultCallback callback) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        // Create a location request to get a high-accuracy location
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000 // Set the interval in milliseconds
        )
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();

        // Create a LocationCallback to receive the update
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Get the last location from the result
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Success! Call the callback with the location.
                    callback.onLocationSuccess(location);
                } else {
                    callback.onLocationFailure("Location is null. Device location might be off.");
                }
                // Stop receiving location updates to save battery
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        // Request the location update
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void startOrResumeTimer(long millisInFuture, long interval) {
        countDownTimer = new CountDownTimer(millisInFuture, interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished; // Update remaining time

                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d",
                        (millisUntilFinished / 1000) / 60, (millisUntilFinished / 1000) % 60);

                elapsed_time.setText(timeFormatted);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                Log.d(TAG, "Countdown finished!");

                // Create an Intent to stop the service with the custom action
                Intent stopIntent = new Intent(ActivityTimedCount.this, LocationTrackingService.class);
                stopIntent.setAction(LocationTrackingService.ACTION_STOP);
                startService(stopIntent);

                end_time = DateHelper.getCurrentTime();

                // Update message
                timerLayout.setEnabled(false);
                timerLayout.setVisibility(View.GONE);
                TextView complete_message = findViewById(R.id.timed_count_on_complete_text);
                complete_message.setVisibility(View.VISIBLE);

                // Enable save button
                save_enabled = true;
                invalidateOptionsMenu();
            }
        }.start();

        isTimerRunning = true;
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning = false;
        }
    }

    private void resumeTimer(long interval) {
        if (!isTimerRunning && timeRemaining > 0) {
            startOrResumeTimer(timeRemaining, interval);
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            setupCount();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCount();
            } else {
                Toast.makeText(this, "Location permission is required to track your route.", Toast.LENGTH_LONG).show();
                Intent fallBack = new Intent(this, ActivityLanding.class);
                startActivity(fallBack);
                finish();
            }
        }
    }

    // BroadcastReceiver to get observation location on user request
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(LocationTrackingService.ACTION_LOCATION_UPDATE)) {
                    Location location = intent.getParcelableExtra(LocationTrackingService.CURRENT_LOCATION);
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float accuracy = location.getAccuracy();
                        Log.d(TAG, "Received location: Lat=" + latitude + ", Lng=" + longitude + ", Accuracy=" + accuracy);
                    }
                } else if (intent.getAction().equals(LocationTrackingService.ACTION_ROUTE_RESULT)) {
                    double totalArea = intent.getDoubleExtra(LocationTrackingService.WALKED_AREA, 0.0);
                    Log.d(TAG, "Received total area: " + totalArea);
                }
            }
        }
    };

    @Override
    public void onTaxonChanged(Long old_id, Long new_id, String taxonNameSuggestion) {
        Log.d(TAG, "Taxon old ID: " + old_id + ", new ID: " + new_id + ", name: " + taxonNameSuggestion);
        if (timedCountAdapter.hasSpeciesWithID(old_id)) {
            // Delete the record if the species name is changed
            timedCountAdapter.removeFromSpeciesCount(old_id);
            if (timedCountAdapter.hasSpeciesWithID(new_id)) {
                // If the species is in the list, just add one individual to the count
                timedCountAdapter.addToSpeciesCount(new_id);
            } else {
                // If the species in not on the list we need to add new entry
                String latin_name = TaxonSearchHelper.getLocalisedLatinName(new_id);
                SpeciesCount new_species;
                if (latin_name != null) {
                    new_species = new SpeciesCount(latin_name,
                            new_id, 1);
                } else {
                    new_species = new SpeciesCount(taxonNameSuggestion,
                            null, 1);
                }
                speciesCounts.add(new_species);
                timedCountAdapter.notifyItemInserted(speciesCounts.size() - 1);
            }
        }
    }
}
