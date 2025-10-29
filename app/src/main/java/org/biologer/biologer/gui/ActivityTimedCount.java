package org.biologer.biologer.gui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.R;
import org.biologer.biologer.adapters.SpeciesCountItems;
import org.biologer.biologer.adapters.TaxaListAdapter;
import org.biologer.biologer.adapters.TimedCountAdapter;
import org.biologer.biologer.viewmodels.TimedCountViewModel;
import org.biologer.biologer.databinding.ActivityTimedCountBinding;
import org.biologer.biologer.network.RetrofitWeatherClient;
import org.biologer.biologer.network.json.WeatherResponse;
import org.biologer.biologer.helpers.DateHelper;
import org.biologer.biologer.services.LocationResultCallback;
import org.biologer.biologer.services.LocationTrackingService;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.helpers.TaxonSearchHelper;
import org.biologer.biologer.helpers.WeatherUtils;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsDb_;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.sql.UserDb;

import java.util.ArrayList;
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
    private TaxonSearchHelper taxonSearchHelper;
    TaxonDb selectedTaxon = null;
    boolean taxonSelectedFromTheList = false;
    private FusedLocationProviderClient fusedLocationClient;
    private TimedCountAdapter timedCountAdapter;
    private final ArrayList<SpeciesCountItems> speciesCountItems = new ArrayList<>();
    boolean save_enabled = false;
    private boolean is_fragment_visible = false;
    private TimedCountViewModel viewModel;
    private ActivityTimedCountBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTimedCountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.linearLayoutAdditionalData.setOnClickListener(v -> displayAdditionalDetailsFragment());

        addToolbar();
        setupBackPressedHandler();
        setupRecyclerView();
        if (isNewEntry()) {
            addNewViewModel();
            checkLocationPermission(); // Also starts setupNewTimedCount()
        } else {
            loadExistingViewModel();
            loadSpeciesData();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupSpeciesAutocompleteEntry();
    }

    private void loadSpeciesData() {
        Integer id = getTimedCountIdFromBundle();
        if (id != null) {
            ArrayList<EntryDb> timedCounts = ObjectBoxHelper.getTimedCountObservations(id);
            for (EntryDb entry : timedCounts) {
                if (timedCountAdapter.hasSpeciesWithID(entry.getTaxonId())) {
                    timedCountAdapter.addToSpeciesCount(entry.getTaxonId());
                } else {
                    SpeciesCountItems new_species = new SpeciesCountItems(entry.getTaxonSuggestion(),
                            entry.getTaxonId(), 1);
                    speciesCountItems.add(new_species);
                    timedCountAdapter.notifyItemInserted(speciesCountItems.size() - 1);
                }
            }
        }
    }

    private void setupSpeciesAutocompleteEntry() {
        // Fill in the drop down menu with list of taxa
        TaxaListAdapter adapter = new TaxaListAdapter(this,
                R.layout.taxa_dropdown_list,
                new ArrayList<>());
        binding.autoCompleteTextViewSpecies.setAdapter(adapter);
        binding.autoCompleteTextViewSpecies.setThreshold(2);
        taxonSearchHelper = new TaxonSearchHelper(this);

        binding.autoCompleteTextViewSpecies.setOnItemClickListener((parent,
                                                                 view,
                                                                 position,
                                                                 id) -> {
            TaxonDb taxonDb = (TaxonDb) parent.getItemAtPosition(position);
            binding.autoCompleteTextViewSpecies.setText(taxonDb.getLatinName());
            selectedTaxon = taxonDb;
            taxonSelectedFromTheList = true;
            if (timedCountAdapter.hasSpeciesWithID(selectedTaxon.getId())) {
                timedCountAdapter.addToSpeciesCount(selectedTaxon.getId());
            } else {
                SpeciesCountItems new_species = new SpeciesCountItems(selectedTaxon.getLatinName(),
                        selectedTaxon.getId(), 1);
                speciesCountItems.add(new_species);
                timedCountAdapter.notifyItemInserted(speciesCountItems.size() - 1);
            }
            binding.autoCompleteTextViewSpecies.setText("");

            addNewObjectBoxEntry(selectedTaxon);

        });

        // When user type taxon name...
        binding.autoCompleteTextViewSpecies.addTextChangedListener(new TextWatcher() {
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
                        if (viewModel.getTaxonGroupId() == 0) {
                            allTaxaLists = taxonSearchHelper.searchTaxa(s.toString(), 0);
                        } else {
                            allTaxaLists = taxonSearchHelper.searchTaxa(s.toString(), viewModel.getTaxonGroupId());
                        }

                        // Add the Query to the drop down list (adapter)
                        TaxaListAdapter adapter1 =
                                new TaxaListAdapter(ActivityTimedCount.this,
                                        R.layout.taxa_dropdown_list,
                                        allTaxaLists);
                        binding.autoCompleteTextViewSpecies.setAdapter(adapter1);
                        adapter1.notifyDataSetChanged();
                    };
                    handler.postDelayed(runnable, 300);
                }
            }
        });

        // Activate the field for species name and show the keyboard.
        binding.autoCompleteTextViewSpecies.requestFocus();
    }

    private void setupRecyclerView() {
        binding.recyclerViewTimedCounts.setLayoutManager(new LinearLayoutManager(this));
        timedCountAdapter = new TimedCountAdapter(speciesCountItems);
        timedCountAdapter.setOnItemClickListener(new TimedCountAdapter.OnItemClickListener() {
            @Override
            public void onPlusClick(SpeciesCountItems species) {
                TaxonDb taxon;
                if (species.getSpeciesID() != null) {
                    taxon = ObjectBoxHelper.getTaxonById(species.getSpeciesID());
                    if (taxon == null) {
                        Log.e(TAG, "There is no taxon for species ID " + species.getSpeciesID());
                        return;
                    }
                    taxon.setLatinName(TaxonSearchHelper.getLocalisedLatinName(taxon));
                } else {
                    taxon = new TaxonDb(0, 0, species.getSpeciesName(),
                            "", 0, "", false, false, "", "", "");
                }
                addNewObjectBoxEntry(taxon);

            }

            @Override
            public void onSpeciesClick(SpeciesCountItems species) {
                Long species_id = species.getSpeciesID();
                if (species_id != null) {
                    Log.d(TAG, "Species with ID " + species_id + " is clicked.");
                    viewModel.setTaxonId(species_id);
                    displayCountEntriesFragment();
                } else {
                    Log.d(TAG, "Species without ID is clicked!");
                    Toast.makeText(ActivityTimedCount.this,
                            R.string.can_not_edit_species_with_custom_names,
                            Toast.LENGTH_LONG).show();
                }

            }
        });

        binding.recyclerViewTimedCounts.setAdapter(timedCountAdapter);
    }

    private void addNewViewModel() {
        viewModel = new ViewModelProvider(this).get(TimedCountViewModel.class);
        viewModel.setTimedCountId(ObjectBoxHelper.getUniqueTimedCountID());
        viewModel.setNewEntry(true);
        addWeatherObserverToViewModel();

        viewModel.getElapsedTime().observe(this, elapsed -> {
            binding.textViewElapsedTime.setText(formatTime(elapsed));

            if (viewModel.getCountDuration() > 0 && elapsed >= viewModel.getCountDuration() * 60_000L) {
                viewModel.pauseTimer();
                Log.d(TAG, "Countdown finished!");

                Intent stopIntent = new Intent(ActivityTimedCount.this, LocationTrackingService.class);
                stopIntent.setAction(LocationTrackingService.ACTION_STOP);
                startService(stopIntent);

                viewModel.setEndTimeString(DateHelper.getCurrentTime());

                binding.linearLayoutTimer.setEnabled(false);
                binding.linearLayoutTimer.setVisibility(View.GONE);
                binding.textViewOnComplete.setVisibility(View.VISIBLE);

                save_enabled = true;
                invalidateOptionsMenu();
            }
        });
    }

    private void loadExistingViewModel() {
        viewModel = new ViewModelProvider(this).get(TimedCountViewModel.class);
        viewModel.setNewEntry(false);
        Integer id = getTimedCountIdFromBundle();
        if (id != null) {
            viewModel.setTimedCountId(id);
            TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(id);
            if (timedCount == null) {
                Log.e(TAG, "There is no timed count with ID: " + id);
                return;
            }

            // Load existing data into the View Model
            viewModel.getFromObjectBox(timedCount);

            addWeatherObserverToViewModel();

            binding.linearLayoutTimer.setEnabled(false);
            binding.linearLayoutTimer.setVisibility(View.GONE);
            binding.linearLayoutAdditionalData.setPadding(16, 24, 16, 8);
            save_enabled = true;
            invalidateOptionsMenu();
        }
    }

    private void addWeatherObserverToViewModel() {
        viewModel.getTemperatureData().observe(this, newTemperature -> {
            Log.d(TAG, "Temperature updated: " + newTemperature);
            binding.textViewTemperature.setText(String.valueOf(newTemperature));
        });
        viewModel.getCloudinessData().observe(this, newCloudiness -> {
            Log.d(TAG, "Cloudiness updated: " + newCloudiness);
            binding.textViewCloudiness.setText(String.valueOf(newCloudiness));
        });
    }

    private void setupBackPressedHandler() {
        // onBackPressed we should warn user not to quit the count
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int fragments_no = getSupportFragmentManager().getBackStackEntryCount();
                Log.d(TAG, "There are " + fragments_no + " active Fragments.");
                if (fragments_no == 0) {
                    Log.d(TAG, "new entry: " + viewModel.isNewEntry() + "; modified; " + viewModel.isModified());
                    if (viewModel.isNewEntry()) {
                        showExitConfirmationDialog();
                    } else {
                        if (viewModel.isModified()) {
                            showSaveOnExitDialog();
                        } else {
                            finish();
                        }
                    }
                } else if (fragments_no == 1) {
                    // Fragment is visible. Hide it and show the main layout.
                    getSupportFragmentManager().popBackStack();
                    binding.fragmentContainerViewTimedCount.setVisibility(View.GONE);
                    binding.linearLayoutMain.setVisibility(View.VISIBLE);
                    // Enable save button
                    is_fragment_visible = false;
                    invalidateOptionsMenu();
                } else {
                    getSupportFragmentManager().popBackStack();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // Add a toolbar to the Activity
    private void addToolbar() {
        setSupportActionBar(binding.toolbar.toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.timed_count_title);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }
    }

    private void addNewObjectBoxEntry(TaxonDb taxon) {

        // Case 1: NEW ENTRY - Timed Count is currently running. Use live data.
        if (viewModel.isNewEntry()) {
            fetchLatestLocation(new LocationResultCallback() {
                @Override
                public void onLocationSuccess(Location location) {
                    createAndSaveNewEntry(taxon, location,
                            DateHelper.getCurrentYear(),
                            DateHelper.getCurrentMonth(),
                            DateHelper.getCurrentDay(),
                            DateHelper.getCurrentTime());
                }

                @Override
                public void onLocationFailure(String errorMessage) {
                    Toast.makeText(ActivityTimedCount.this, "Location not available. " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });

        // Case 2: EXISTING ENTRY - Timed Count is reopened. Use stored data.
        } else {
            Integer timedCountId = viewModel.getTimedCountId();
            if (timedCountId == null) {
                Log.e(TAG, "Cannot add entry, Timed Count ID is missing for existing entry.");
                Toast.makeText(this, R.string.error_loading_timed_count_id, Toast.LENGTH_LONG).show();
                return;
            }

            TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(timedCountId);
            if (timedCount == null) {
                Toast.makeText(this, R.string.error_original_count_data_missing, Toast.LENGTH_LONG).show();
                return;
            }

            // Get the average location (centroid) of all observations in this count
            Location centralLocation = ObjectBoxHelper.calculateCentroidLocation(timedCountId);
            if (centralLocation == null) {
                Toast.makeText(this, R.string.error_no_location_data, Toast.LENGTH_LONG).show();
                return;
            }

            // Get average time
            String averageTime = DateHelper.getAverageTime(
                    timedCount.getStartTime(),
                    timedCount.getEndTime()
            );

            // Derive Accuracy as half of the walked distance
            double accuracy = (double) viewModel.getDistance() / 2.0;
            centralLocation.setAccuracy((float) accuracy);
            createAndSaveNewEntry(taxon,
                    centralLocation,
                    timedCount.getYear(),
                    timedCount.getMonth(),
                    timedCount.getDay(),
                    averageTime);
        }
    }

    /**
     * Creates and saves a new EntryDb record using the provided data, instead of
     * relying on current location/time.
     */
    private void createAndSaveNewEntry(TaxonDb taxon, Location location,
                                       String year, String month, String day, String time) {
        Log.d(TAG, "Saving observation data with location: Lat = " + location.getLatitude() + ", Lng = " + location.getLongitude() + ", Acc = " + location.getAccuracy());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ActivityTimedCount.this);

        // Always add Observation Type for observed specimen
        String observed_id = "[" + ObjectBoxHelper.getIdForObservedTag() + "]";
        // Always set the stage to adult
        Long stageId = ObjectBoxHelper.getAdultStageIdForTaxon(taxon);

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
                viewModel.getTimedCountId(),
                taxon.getLatinName(),
                year,
                month,
                day,
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
                time,
                "",
                observed_id);
        long newEntryId = ObjectBoxHelper.setObservation(entryDb);
        viewModel.addNewEntryId(newEntryId);
    }

    private void displayAdditionalDetailsFragment() {
        Log.d(TAG, "Showing additionalData fragment");
        // Hide the save button when the fragment is visible
        is_fragment_visible = true;
        invalidateOptionsMenu();

        binding.linearLayoutMain.setVisibility(View.GONE);
        binding.fragmentContainerViewTimedCount.setVisibility(View.VISIBLE);

        FragmentTimedCountAdditionalData additionalDataFragment = new FragmentTimedCountAdditionalData();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("TIMED_COUNT_ADDITIONAL_DATA");
        fragmentTransaction.replace(R.id.fragmentContainerViewTimedCount, additionalDataFragment);
        fragmentTransaction.commit();
    }

    private void displayCountEntriesFragment() {
        Log.d(TAG, "Showing entries fragment for the species");
        // Hide the save button when the fragment is visible
        is_fragment_visible = true;
        invalidateOptionsMenu();

        binding.linearLayoutMain.setVisibility(View.GONE);
        binding.fragmentContainerViewTimedCount.setVisibility(View.VISIBLE);

        FragmentTimedCountEntries timedCountEntries = new FragmentTimedCountEntries();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("TIMED_COUNT_ENTRIES");
        fragmentTransaction.replace(R.id.fragmentContainerViewTimedCount, timedCountEntries);
        fragmentTransaction.commit();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(ActivityTimedCount.this)
                .setTitle(R.string.exit_time_count)
                .setMessage(R.string.confirmation_exit_time_count)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // Get the list of new observations and delete them
                    removeNewEntriesFromObjectBox();
                    // Finish the activity
                    Intent serviceIntent = new Intent(ActivityTimedCount.this, LocationTrackingService.class);
                    stopService(serviceIntent);
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showSaveOnExitDialog() {
        new AlertDialog.Builder(ActivityTimedCount.this)
                .setTitle(R.string.exit_time_count)
                .setMessage(R.string.save_on_exit_time_count)
                .setPositiveButton(R.string.save, (dialog, which) -> saveTimedCount())
                .setNegativeButton(R.string.ignore, (dialog, which) -> {
                    // Get the list of new observations and delete them
                    removeNewEntriesFromObjectBox();
                    // Finish the activity
                    Intent serviceIntent = new Intent(ActivityTimedCount.this, LocationTrackingService.class);
                    stopService(serviceIntent);
                    finish();
                })
                .show();
    }

    private void removeNewEntriesFromObjectBox() {
        List<Long> new_entries = viewModel.getNewEntryIds();
        for (Long entryId : new_entries) {
            ObjectBoxHelper.removeObservationById(entryId);
        }
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
        TimedCountDb timedCountDb = new TimedCountDb(getObjectBoxID(),
                viewModel.getTimedCountId(),
                viewModel.getStartTimeString(),
                viewModel.getEndTimeString(),
                viewModel.getCountDuration(),
                viewModel.getArea(),
                viewModel.getDistance(),
                viewModel.getCloudinessData().getValue(),
                viewModel.getPressureData(),
                viewModel.getHumidityData(),
                viewModel.getTemperatureData().getValue(),
                viewModel.getWindDirectionData(),
                viewModel.getWindSpeedData(),
                viewModel.getHabitatData(),
                viewModel.getCommentData(),
                String.valueOf(viewModel.getTaxonGroupId()),
                DateHelper.getCurrentDay(),
                DateHelper.getCurrentMonth(),
                DateHelper.getCurrentYear());

        Box<TimedCountDb> timedCountDbBox = App.get().getBoxStore().boxFor(TimedCountDb.class);
        timedCountDbBox.put(timedCountDb);

        Intent intent = new Intent();
        intent.putExtra("IS_NEW_ENTRY", isNewEntry());
        intent.putExtra("TIMED_COUNT_ID", viewModel.getTimedCountId());
        intent.putExtra("TIMED_COUNT_START_TIME", viewModel.getStartTimeString());
        intent.putExtra("TIMED_COUNT_DAY", DateHelper.getCurrentDay());
        intent.putExtra("TIMED_COUNT_MONTH", DateHelper.getCurrentMonth());
        intent.putExtra("TIMED_COUNT_YEAR", DateHelper.getCurrentYear());

        setResult(Activity.RESULT_OK, intent);

        finish();
    }

    private long getObjectBoxID() {
        if (isNewEntry()) {
            return 0;
        } else {
            Integer id = viewModel.getTimedCountId();
            if (id != null) {
                return ObjectBoxHelper.getIdFromTimeCountId(id);
            } else {
                return 0;
            }
        }
    }

    // Create AlertDialog to setup before starting the timed count.
    private void setupNewTimedCount() {
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

        Spinner spinnerTaxaGroup;
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
                viewModel.setCountDuration(Integer.parseInt(minutesString));
            }

            // Get the taxa group ID for selected taxa
            String selectedTaxa = (String) spinnerTaxaGroup.getSelectedItem();
            Log.d(TAG, "Minutes: " + viewModel.getCountDuration() + ", Selected Option: " + selectedTaxa);
            Box<TaxonGroupsDb> taxonGroupsDataBox = App.get().getBoxStore().boxFor(TaxonGroupsDb.class);
            if (selectedTaxa.equals(getString(R.string.butterflies))) {
                Query<TaxonGroupsDb> query = taxonGroupsDataBox
                        .query(TaxonGroupsDb_.name.contains("butterfly", QueryBuilder.StringOrder.CASE_INSENSITIVE)
                                .or(TaxonGroupsDb_.name.contains("butterflies", QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                        .build();
                List<TaxonGroupsDb> listParents = query.find();
                query.close();
                if (!listParents.isEmpty()) {
                    viewModel.setTaxonGroupId(listParents.get(0).getId());
                }
            }
            if (selectedTaxa.equals(getString(R.string.birds))) {
                Query<TaxonGroupsDb> query = taxonGroupsDataBox
                        .query(TaxonGroupsDb_.name.contains("bird", QueryBuilder.StringOrder.CASE_INSENSITIVE))
                        .build();
                List<TaxonGroupsDb> listParents = query.find();
                query.close();
                if (!listParents.isEmpty()) {
                    viewModel.setTaxonGroupId(listParents.get(0).getId());
                }
            }
            Log.d(TAG, "Selected taxa (" + selectedTaxa + ") has ID: " + viewModel.getTaxonGroupId());

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
                                viewModel.setTemperatureData(response.body().getMain().getTemp());
                                viewModel.setCloudinessData(response.body().getClouds().getCloudiness());
                                viewModel.setPressureData(response.body().getMain().getPressure());
                                viewModel.setHumidityData(response.body().getMain().getHumidity());
                                viewModel.setWindSpeedData(
                                        WeatherUtils.getBeaufortScale(response.body().getWind().getSpeed()));
                                viewModel.setWindDirectionData(
                                        WeatherUtils.getWindDirection(response.body().getWind().getDeg()));

                                Log.d(TAG,"Temperature: " + viewModel.getTemperatureData().getValue() +
                                        ", Clouds: " + viewModel.getCloudinessData().getValue() +
                                        ", Pressure: " + viewModel.getPressureData() +
                                        ", Humidity: " + viewModel.getHumidityData() +
                                        ", Wind speed: " + viewModel.getWindSpeedData() +
                                        ", Wind direction: " + viewModel.getWindDirectionData());
                            } else {
                                Log.d(TAG, "Weather response not successful: "
                                        + response.code()  + "; Message: " + response.message());
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
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Start the timer in ViewModel
        viewModel.resetTimer();
        viewModel.startTimer();

        binding.linearLayoutTimer.setOnClickListener(view -> {
            if (viewModel.isRunning()) {
                viewModel.pauseTimer();
                binding.imageViewPauseTimer.setImageResource(R.drawable.ic_play);
                binding.textViewElapsedTime.setText(R.string.paused);

                Intent pauseIntent = new Intent(this, LocationTrackingService.class);
                pauseIntent.setAction(LocationTrackingService.ACTION_PAUSE);
                startService(pauseIntent);
            } else {
                viewModel.startTimer(); // resumes from pausedTime
                binding.imageViewPauseTimer.setImageResource(R.drawable.ic_pause);

                Intent resumeIntent = new Intent(this, LocationTrackingService.class);
                resumeIntent.setAction(LocationTrackingService.ACTION_RESUME);
                startService(resumeIntent);
            }
        });

        viewModel.setStartTimeString(DateHelper.getCurrentTime());
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

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            setupNewTimedCount();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupNewTimedCount();
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
                    Location location;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                         location = intent.getParcelableExtra(LocationTrackingService.CURRENT_LOCATION, Location.class);
                    } else {
                        location = intent.getParcelableExtra(LocationTrackingService.CURRENT_LOCATION);
                    }
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float accuracy = location.getAccuracy();
                        Log.d(TAG, "Received location: Lat=" + latitude + ", Lng=" + longitude + ", Accuracy=" + accuracy);
                    }
                } else if (intent.getAction().equals(LocationTrackingService.ACTION_ROUTE_RESULT)) {
                    double totalArea = intent.getDoubleExtra(LocationTrackingService.WALKED_AREA, 0.0);
                    Log.d(TAG, "Received total area: " + totalArea);
                    double totalDistance = intent.getDoubleExtra(LocationTrackingService.WALKED_DISTANCE, 0.0);
                    Log.d(TAG, "Received total area: " + totalDistance);
                    viewModel.setArea( (int) totalArea );
                    viewModel.setDistance( (int) totalDistance);
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
                SpeciesCountItems new_species;
                if (latin_name != null) {
                    new_species = new SpeciesCountItems(latin_name,
                            new_id, 1);
                } else {
                    new_species = new SpeciesCountItems(taxonNameSuggestion,
                            null, 1);
                }
                speciesCountItems.add(new_species);
                timedCountAdapter.notifyItemInserted(speciesCountItems.size() - 1);
            }
        }
    }

    private Boolean isNewEntry() {
        return getIntent().getBooleanExtra("IS_NEW_ENTRY", true);
    }

    private Integer getTimedCountIdFromBundle() {
        long id = getIntent().getLongExtra("TIMED_COUNT_ID", 0);
        if (id != 0) {
            return (Integer) (int) id;
        } else {
            Log.e(TAG, "Timed cound ID from Bundle is null!");
            return null;
        }
    }
}
