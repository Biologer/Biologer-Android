package org.biologer.biologer.gui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.biologer.biologer.R;
import org.biologer.biologer.adapters.SpeciesCount;
import org.biologer.biologer.adapters.TaxaListAdapter;
import org.biologer.biologer.adapters.TimedCountAdapter;
import org.biologer.biologer.services.LocationTrackingService;
import org.biologer.biologer.services.TaxonSearchHelper;
import org.biologer.biologer.sql.TaxonDb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimedCountActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.TimedCount";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    TextView elapsed_time;
    LinearLayout timerLayout;
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
    double latitude, longitude, accuracy;
    private TimedCountAdapter timedCountAdapter;
    private final ArrayList<SpeciesCount> speciesCounts = new ArrayList<>();

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

        elapsed_time = findViewById(R.id.time_elapsed);
        timerLayout = findViewById(R.id.timed_count_timer);
        pauseTimerImage = findViewById(R.id.pause_timer_image);

        recyclerView = findViewById(R.id.recycled_view_timed_counts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        timedCountAdapter = new TimedCountAdapter(speciesCounts);
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
            SpeciesCount new_species = new SpeciesCount(selectedTaxon.getLatinName(),
                    selectedTaxon.getId(), 1);
            speciesCounts.add(new_species);
            timedCountAdapter.notifyItemInserted(speciesCounts.size() - 1);
            autoCompleteTextView_speciesName.setText("");
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

                        List<TaxonDb> allTaxaLists = taxonSearchHelper.searchTaxa(s.toString());

                        // Add the Query to the drop down list (adapter)
                        TaxaListAdapter adapter1 =
                                new TaxaListAdapter(TimedCountActivity.this,
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);



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

        Spinner spinnerOptions = new Spinner(this);
        List<String> options = new ArrayList<>();
        options.add("Butterflies");
        options.add("Birds");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOptions.setAdapter(spinnerAdapter);
        layout.addView(spinnerOptions);

        builder.setView(layout);

        builder.setPositiveButton(R.string.start_counting, (dialog, which) -> {
            String minutesString = editTextMinutes.getText().toString();
            int minutes = 0;
            if (!minutesString.isEmpty()) {
                minutes = Integer.parseInt(minutesString);
            }
            String selectedOption = (String) spinnerOptions.getSelectedItem();
            Log.d("Dialog", "Minutes: " + minutes + ", Selected Option: " + selectedOption);

            startCount(minutes);

        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

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

    private void startCount(int minutes) {
        // Record location
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Setup timer
        long millisInFuture = minutes * 60 * 1000L; // minutes to milliseconds
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

    }

    private void getLatestLocationOnRequest() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, request them
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        accuracy = location.getAccuracy();
                        Log.d(TAG, "Observation location: " + latitude + "; " + longitude + " (" + accuracy + ")");
                    } else {
                        Log.d(TAG, "Directly requested location is null. Device location might be off or not recorded.");
                    }
                })
                .addOnFailureListener(this, e -> Log.e(TAG, "Error getting direct location: " + e.getMessage()));
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
                Intent stopIntent = new Intent(TimedCountActivity.this, LocationTrackingService.class);
                stopIntent.setAction(LocationTrackingService.ACTION_STOP);
                startService(stopIntent);

                elapsed_time.setText(R.string.finished);
                timerLayout.setEnabled(false);
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
                Intent fallBack = new Intent(this, LandingActivity.class);
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

}
