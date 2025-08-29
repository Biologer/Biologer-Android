package org.biologer.biologer.gui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.ObservationViewModel;
import org.biologer.biologer.adapters.TaxaListAdapter;
import org.biologer.biologer.services.ArrayHelper;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.services.FileManipulation;
import org.biologer.biologer.services.ObjectBoxHelper;
import org.biologer.biologer.services.PreparePhotos;
import org.biologer.biologer.services.StageAndSexLocalization;
import org.biologer.biologer.services.TaxonSearchHelper;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.ObservationTypesDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.TaxonDb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ActivityObservation extends AppCompatActivity implements View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "Biologer.Entry";
    private LocationManager locationManager;
    private LocationListenerCompat locationListener;
    private TextInputLayout textViewAtlasCodeLayout, textViewSpecimensNo1, textViewSpecimensNo2, textViewDeathComment, textInputStages;
    private TextView textViewGPSAccuracy, textViewStage, textViewLatitude, textViewLongitude, textViewAtlasCode, textViewMeters;
    private EditText editTextDeathComment, editTextComment, editTextSpecimensNo1, editTextSpecimensNo2, editTextHabitat,
            editTextFoundOn, dateSelect, timeSelect;
    private MaterialCheckBox checkBox_males, checkBox_females, checkBox_dead;
    AutoCompleteTextView autoCompleteTextView_speciesName;
    FrameLayout frameLayoutPicture1, frameLayoutPicture2, frameLayoutPicture3;
    ImageView imageViewPicture1, imageViewPicture1Del, imageViewPicture2, imageViewPicture2Del,
            imageViewPicture3, imageViewPicture3Del, imageViewMap, imageViewCamera, imageViewGallery;
    ChipGroup observation_types;
    LinearLayout layoutCoordinates, layoutUnknownCoordinates;
    private SwipeRefreshLayout swipeRefreshLayout;
    boolean locationFromTheMap = false;
    boolean callTagAutoChecked = false;
    Integer callTagIndexNumber = null;
    private TaxonSearchHelper taxonSearchHelper;
    BroadcastReceiver receiver;
    private ObservationViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation);

        setupToolbar();
        setupLocationListenerAndManager();
        setupView();

        if (isNewEntry()) {
            addNewViewModel();
            Log.i(TAG, "Starting new entry.");
            getLocation(100, 2);

            // Always add Observation Type for observed specimen
            Long id = ObjectBoxHelper.getIdForObservedTag();
            if (id != null) {
                int id_for_observed_tag = id.intValue();
                viewModel.addObservationType(id_for_observed_tag);
            }
        } else {
            Log.d(TAG, "Opening existing entry.");
            loadExistingViewModel();
            fillExistingEntry();
        }

        // Get the data from the ViewModel and update UI
        setViewModelObservers();

        dateSelect.setOnClickListener(v -> {
            Calendar calendar = viewModel.getCalendar().getValue();
            if (calendar != null) {
                DialogFragment dateFragment = new FragmentDatePicker();
                dateFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });

        timeSelect.setOnClickListener(v -> {
            Calendar calendar = viewModel.getCalendar().getValue();
            if (calendar != null) {
                DialogFragment timeFragment = new FragmentTimePicker();
                timeFragment.show(getSupportFragmentManager(), "timePicker");
            }
        });

        // Broadcaster used for receiving resized images
        registerBroadcastReceiver();

        fillObservationTypes();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "Back button is pressed!");
                backPressed();
            }
        });

    }

    private void setupView() {
        dateSelect = findViewById(R.id.date_input);
        timeSelect = findViewById(R.id.time_input);
        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(this);
        textViewLatitude = findViewById(R.id.tv_latitude);
        textViewLongitude = findViewById(R.id.tv_longitude);
        textViewGPSAccuracy = findViewById(R.id.textView_gps_accuracy);
        textViewMeters = findViewById(R.id.textView_meter);
        textViewStage = findViewById(R.id.text_view_stages);
        textViewStage.setOnClickListener(this);
        editTextDeathComment = findViewById(R.id.editText_death_comment);
        textViewDeathComment = findViewById(R.id.textView_death_comment);
        editTextComment = findViewById(R.id.editText_comment);
        editTextSpecimensNo1 = findViewById(R.id.editText_number_of_specimens_1);
        textViewSpecimensNo1 = findViewById(R.id.textView_specimens_no_1);
        editTextSpecimensNo2 = findViewById(R.id.editText_number_of_specimens_2);
        textViewSpecimensNo2 = findViewById(R.id.textView_specimens_no_2);
        editTextHabitat = findViewById(R.id.editText_habitat);
        editTextFoundOn = findViewById(R.id.editText_found_on);
        checkBox_males = findViewById(R.id.male);
        checkBox_males.setOnClickListener(this);
        checkBox_females = findViewById(R.id.female);
        checkBox_females.setOnClickListener(this);
        textViewAtlasCodeLayout = findViewById(R.id.text_view_atlas_code_layout);
        textViewAtlasCode = findViewById(R.id.text_view_atlas_code);
        textViewAtlasCode.setOnClickListener(this);
        checkBox_dead = findViewById(R.id.dead_specimen);
        checkBox_dead.setOnClickListener(this);
        // Buttons to add images
        frameLayoutPicture1 = findViewById(R.id.ib_pic1_frame);
        imageViewPicture1 = findViewById(R.id.ib_pic1);
        imageViewPicture1.setOnClickListener(this);
        imageViewPicture1Del = findViewById(R.id.ib_pic1_del);
        imageViewPicture1Del.setOnClickListener(this);
        frameLayoutPicture2 = findViewById(R.id.ib_pic2_frame);
        imageViewPicture2 = findViewById(R.id.ib_pic2);
        imageViewPicture2.setOnClickListener(this);
        imageViewPicture2Del = findViewById(R.id.ib_pic2_del);
        imageViewPicture2Del.setOnClickListener(this);
        frameLayoutPicture3 = findViewById(R.id.ib_pic3_frame);
        imageViewPicture3 = findViewById(R.id.ib_pic3);
        imageViewPicture3.setOnClickListener(this);
        imageViewPicture3Del = findViewById(R.id.ib_pic3_del);
        imageViewPicture3Del.setOnClickListener(this);
        imageViewCamera = findViewById(R.id.image_view_take_photo_camera);
        imageViewCamera.setOnClickListener(this);
        imageViewGallery = findViewById(R.id.image_view_take_photo_gallery);
        imageViewGallery.setOnClickListener(this);
        // Map icon
        imageViewMap = findViewById(R.id.iv_map);
        imageViewMap.setOnClickListener(this);
        // Tag cloud for observation types
        observation_types = findViewById(R.id.observation_types);
        // Show advanced options for data entry if selected in preferences
        layoutCoordinates = findViewById(R.id.layout_coordinates);
        layoutUnknownCoordinates = findViewById(R.id.layout_unknown_coordinates);
        // This linear layout holds the stages. We will hide it before the taxon is not selected.
        textInputStages = findViewById(R.id.text_input_stages);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        LinearLayout linearLayout = findViewById(R.id.data_and_time_entry);
        if (!preferences.getBoolean("advanced_interface", false)) {
            linearLayout.setVisibility(View.GONE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
        }
        // Fill in the drop down menu with list of taxa
        TaxaListAdapter adapter = new TaxaListAdapter(this, R.layout.taxa_dropdown_list, new ArrayList<>());
        autoCompleteTextView_speciesName = findViewById(R.id.textview_list_of_taxa);
        autoCompleteTextView_speciesName.setAdapter(adapter);
        autoCompleteTextView_speciesName.setThreshold(2);
        taxonSearchHelper = new TaxonSearchHelper(this);
        autoCompleteTextView_speciesName.setOnItemClickListener((parent, view, position, id) -> {
            TaxonDb taxonDb = (TaxonDb) parent.getItemAtPosition(position);
            autoCompleteTextView_speciesName.setText(taxonDb.getLatinName());
            viewModel.setTaxonId(taxonDb.getId());
            viewModel.setTaxonSuggestion(taxonDb.getLatinName());
            viewModel.setTaxonSelectedFromTheList(true);
            showStagesAndAtlasCode(preferences);
        });
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
                        if (!viewModel.isTaxonSelectedFromTheList()) {
                            hideStagesAndAtlasCode();
                            viewModel.setTaxonId(null);
                        }
                        viewModel.setTaxonSelectedFromTheList(false);

                        List<TaxonDb> allTaxaLists = taxonSearchHelper.searchTaxa(s.toString());

                        // Add the Query to the drop down list (adapter)
                        TaxaListAdapter adapter1 =
                                new TaxaListAdapter(ActivityObservation.this, R.layout.taxa_dropdown_list, allTaxaLists);
                        autoCompleteTextView_speciesName.setAdapter(adapter1);
                        adapter1.notifyDataSetChanged();

                        // Enable/disable Save button in the Toolbar
                        if (autoCompleteTextView_speciesName.getText().toString().length() > 1) {
                            viewModel.setSaveEnabled(true);
                            Log.d(TAG, "Taxon is set to: " + autoCompleteTextView_speciesName.getText());
                        } else {
                            viewModel.setSaveEnabled(false);
                            Log.d(TAG, "Taxon entry field is empty.");
                        }
                        invalidateOptionsMenu();
                    };
                    handler.postDelayed(runnable, 300);
                }
            }
        });

        // Activate the field for species name and show the keyboard.
        autoCompleteTextView_speciesName.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void setupLocationListenerAndManager() {
        // Define locationListener and locationManager in order to
        // to receive the Location.
        locationListener = new LocationListenerCompat() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                viewModel.setCoordinates(new LatLng(location.getLatitude(), location.getLongitude()));
                viewModel.setElevation(location.getAltitude());
                viewModel.setAccuracy((double) location.getAccuracy());
            }

            @Override
            public void onProviderEnabled(@NonNull String s) {
                Log.i(TAG, "Location provider is enabled.");
            }

            @Override
            public void onProviderDisabled(@NonNull String s) {
                Log.i(TAG, "Location provider is disabled.");
                if (!isFinishing()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityObservation.this);
                    builder.setMessage(getString(R.string.global_location_disabled))
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes, (dialog, id) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                            .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    private void setViewModelObservers() {
        viewModel.getImage1().observe(this, imageUri -> {
            Log.d(TAG, "Image 1 loaded: " + imageUri);
            loadImageThumbnail(imageUri, imageViewPicture1, frameLayoutPicture1);
        });
        viewModel.getImage2().observe(this, imageUri -> {
            Log.d(TAG, "Image 2 loaded: " + imageUri);
            loadImageThumbnail(imageUri, imageViewPicture2, frameLayoutPicture2);
        });
        viewModel.getImage3().observe(this, imageUri -> {
            Log.d(TAG, "Image 3 loaded: " + imageUri);
            loadImageThumbnail(imageUri, imageViewPicture3, frameLayoutPicture3);
        });

        viewModel.getCalendar().observe(this, calendar -> {
            Log.d(TAG, "Calendar changed.");
            dateSelect.setText(DateHelper.getLocalizedCalendarDate(calendar));
            timeSelect.setText(DateHelper.getLocalizedCalendarTime(calendar));
        });

        viewModel.getCoordinates().observe(this, coordinates -> {
            Log.d(TAG, "Coordinates changed.");
            textViewLatitude.setText(viewModel.getLocalizedLatitiudeString());
            textViewLongitude.setText(viewModel.getLocalizedLongitudeString());
            layoutUnknownCoordinates.setVisibility(View.GONE);
            layoutCoordinates.setVisibility(View.VISIBLE);
        });

        viewModel.getAccuracy().observe(this, accuracy -> {
            // Update the coordinate accuracy labels
            if (accuracy == null || accuracy == 0.0) {
                textViewGPSAccuracy.setText(R.string.unknown);
                textViewMeters.setVisibility(View.GONE);
            } else {
                textViewGPSAccuracy.setText(String.valueOf(viewModel.getRoundedAccuracy().intValue()));
                textViewMeters.setVisibility(View.VISIBLE);
                // Set the colors to red if the accuracy is low
                if (accuracy <= 25) {
                    textViewMeters.setTextColor(ContextCompat.getColor(this, R.color.checkBox_text));
                    textViewGPSAccuracy.setTextColor(ContextCompat.getColor(this, R.color.checkBox_text));
                } else {
                    textViewMeters.setTextColor(ContextCompat.getColor(this, R.color.warningRed));
                    textViewGPSAccuracy.setTextColor(ContextCompat.getColor(this, R.color.warningRed));
                }
            }
        });

        viewModel.getCalendar().observe(this, calendar -> {
            dateSelect.setText(DateHelper.getLocalizedCalendarDate(calendar));
            timeSelect.setText(DateHelper.getLocalizedCalendarTime(calendar));
        });

        viewModel.getNumberOfSpecimens().observe(this, specimens -> {
            editTextSpecimensNo1.setText(specimens != null ? String.valueOf(specimens) : "");
        });

        viewModel.getNumberOfSpecimens2().observe(this, specimens -> {
            editTextSpecimensNo2.setText(specimens != null ? String.valueOf(specimens) : "");
        });

        viewModel.getDead().observe(this, dead -> {
            Log.d(TAG, "Specimen is dead? " + dead);
            if (dead) {
                checkBox_dead.setChecked(true);
                textViewDeathComment.setVisibility(View.VISIBLE);
            } else {
                checkBox_dead.setChecked(false);
                textViewDeathComment.setVisibility(View.GONE);
                editTextDeathComment.setText("");
            }
        });
    }

    private void loadImageThumbnail(String image, ImageView imageView, FrameLayout frame) {
        if (image != null) {
            Glide.with(this)
                    .load(image)
                    .override(100, 100)
                    .into(imageView);
            frame.setVisibility(View.VISIBLE);
        }
    }

    private void loadExistingViewModel() {
        viewModel = new ViewModelProvider(this).get(ObservationViewModel.class);
        // Get observation entry ID from the Bundle
        EntryDb observation = ObjectBoxHelper.getObservationById(
                getIntent().getLongExtra("ENTRY_ID", 0));
        if (observation != null) {
            viewModel.getFromObjectBox(observation);
        }
    }

    private void addNewViewModel() {
        viewModel = new ViewModelProvider(this).get(ObservationViewModel.class);
        viewModel.setCoordinates(new LatLng(0, 0));
        Calendar calendar = Calendar.getInstance();
        viewModel.setCalendar(calendar);
        viewModel.setProject(PreferenceManager.getDefaultSharedPreferences(this)
                .getString("project_name", "0"));
        getPhotoTag();
        viewModel.setDataLicence(String.valueOf(ObjectBoxHelper.getDataLicense()));
        viewModel.setImageLicence(ObjectBoxHelper.getImageLicense());
    }

    private void setupToolbar() {
        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.entry_title);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }
    }

    private void showStagesAndAtlasCode(SharedPreferences preferences) {
        // Enable stage entry
        // Check if the taxon has stages. If not hide the stages dialog.
        if (viewModel.getTaxonId() != null) {
            TaxonDb taxon = ObjectBoxHelper.getTaxonById(viewModel.getTaxonId());
            String stages = taxon.getStages();
            Log.i(TAG, "This are the stages for " + taxon.getLatinName() + ": " + stages);
            if (stages != null) {
                if (!stages.isEmpty()) {
                    Log.d(TAG, "Enabling Stages for this taxon.");
                    textInputStages.setVisibility(View.VISIBLE);

                    String[] all_stages = stages.split(";");

                    // If the user changed the taxon in the mean time, we'll try to get the previous stage
                    if (viewModel.getStage().getValue() != null) {
                        Log.d(TAG, "There is a stage already selected. ID: " + viewModel.getStage().getValue() + "; ");
                        StageDb stage = ObjectBoxHelper.getStageById(viewModel.getStage().getValue());
                        if (stage != null) {
                            if (Arrays.asList(all_stages).contains(String.valueOf(viewModel.getStage().getValue()))) {
                                String stageName = StageAndSexLocalization
                                        .getStageLocaleFromID(this, viewModel.getStage().getValue());
                                textViewStage.setText(stageName);
                                textViewStage.setTag(viewModel.getStage().getValue());
                            }
                        }
                    }

                    // If user preferences are selected, the stage for taxa will be set to adult by default.
                    // Step 1: Get the preferences
                    if (preferences.getBoolean("adult_by_default", false)) {
                        Log.d(TAG, "Should set adult by default.");
                        // If stage is already selected ignore this...
                        if (textViewStage.getText().toString().isEmpty()) {
                            Log.d(TAG, "Should fill in the textViewStage.");
                            Long adultId = ObjectBoxHelper.getAdultStageIdForTaxon(taxon);
                            String stageName = StageAndSexLocalization.getStageLocaleFromID(this, adultId);
                            textViewStage.setTag(adultId);
                            textViewStage.setText(stageName);
                        }
                    }
                }
            }
            if (taxon.isUseAtlasCode()) {
                Log.d(TAG, "Enabling Atlas code for this taxon.");
                textViewAtlasCodeLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideStagesAndAtlasCode() {
        textInputStages.setVisibility(View.GONE);
        textViewStage.setTag(null);
        textViewStage.setText(null);
        textViewAtlasCodeLayout.setVisibility(View.GONE);
        textViewAtlasCode.setText("");
        Log.d(TAG, "Taxon is not selected from the list. Disabling Stages and Atlas Codes for this taxon.");
    }

    private void registerBroadcastReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(PreparePhotos.RESIZED);
                if (s != null) {
                    Log.d(TAG, "Resize Images returned code: " + s);

                    if (s.equals("error")) {
                        Toast.makeText(ActivityObservation.this, getString(R.string.image_resize_error), Toast.LENGTH_LONG).show();
                    } else {
                        if (viewModel.getImage1().getValue() == null) {
                            viewModel.setImage1(s);
                            viewModel.addItemToListNewImage(s);
                        } else if (viewModel.getImage2().getValue() == null) {
                            viewModel.setImage2(s);
                            viewModel.addItemToListNewImage(s);
                        } else if (viewModel.getImage3().getValue() == null) {
                            viewModel.setImage3(s);
                            viewModel.addItemToListNewImage(s);
                            // Form is full, disable adding more images
                            imageViewGallery.setEnabled(false);
                            imageViewGallery.setImageAlpha(20);
                            imageViewCamera.setEnabled(false);
                            imageViewCamera.setImageAlpha(20);
                        }
                    }
                }
            }
        };
    }

    private Boolean isNewEntry() {
        String is_new_entry = getIntent().getStringExtra("IS_NEW_ENTRY");
        assert is_new_entry != null;
        return is_new_entry.equals("YES");
    }

    private void fillExistingEntry() {
        // Get the name of the taxon if there is one (i.e. when opening existing entry)
        if (viewModel.getTaxonSuggestion() != null) {
            autoCompleteTextView_speciesName.setText(viewModel.getTaxonSuggestion());
            autoCompleteTextView_speciesName.dismissDropDown();
        }

        if (viewModel.getTaxonId() != null) {
            viewModel.setTaxonSelectedFromTheList(true);
            TaxonDb taxon = ObjectBoxHelper.getTaxonById(viewModel.getTaxonId());

            // Get the atlas code
            boolean use_atlas_code;
            if (taxon != null) {
                use_atlas_code = taxon.isUseAtlasCode();
                if (use_atlas_code) {
                    Log.d(TAG, "There is an atlas code ID: "
                            + viewModel.getAtlasCode().getValue());
                    textViewAtlasCodeLayout.setVisibility(View.VISIBLE);
                }
            }
        }

        // Get the name of the stage for the entry from the database
        if (viewModel.getStage().getValue() != null) {
            Log.d(TAG, "There is a stage already selected for this entry!");
            long stage_id = ObjectBoxHelper.getStageById(viewModel.getStage().getValue()).getId();
            String stageName = StageAndSexLocalization.getStageLocaleFromID(this, stage_id);
            textViewStage.setTag(stage_id);
            textViewStage.setText(stageName);
            textInputStages.setVisibility(View.VISIBLE);
        } else {
            if (isStageAvailable(viewModel.getTaxonId())) {
                textInputStages.setVisibility(View.VISIBLE);
            } else {
                textInputStages.setVisibility(View.GONE);
            }
        }

        // Get the selected sex. If not selected set spinner to default...
        Log.d(TAG, "Sex of individual from previous entry is " + viewModel.getSex().getValue());
        if (viewModel.getSex().getValue().equals("male")) {
            Log.d(TAG, "Setting spinner selected item to male.");
            checkBox_males.setChecked(true);
        }
        if (viewModel.getSex().getValue().equals("female")) {
            Log.d(TAG, "Setting spinner selected item to female.");
            checkBox_females.setChecked(true);
        }

        // Get the atlas code.
        if (viewModel.getAtlasCode().getValue() != null) {
            Log.d(TAG, "Setting the spinner to atlas code: " + viewModel.getAtlasCode().getValue());
            textViewAtlasCode.setText(setAtlasCode(viewModel.getAtlasCode().getValue().intValue()));
        }

        disablePhotoButtons(viewModel.getImage1().getValue() != null &&
                viewModel.getImage2().getValue() != null &&
                viewModel.getImage3().getValue() != null);
        if (viewModel.getHabitat() != null) {
            editTextHabitat.setText(viewModel.getHabitat());
        }
        if (viewModel.getFoundOn() != null) {
            editTextFoundOn.setText(viewModel.getFoundOn());
        }

        // Get other values
        if (!viewModel.getCauseOfDeath().isEmpty()) {
            editTextDeathComment.setText(viewModel.getCauseOfDeath());
        }
        if (!viewModel.getComment().isEmpty()) {
            editTextComment.setText(viewModel.getComment());
        }

        // Load observation types and delete tag for photographed.
        //TODO Removing image tag just in case images got deleted.");
        if (viewModel.getImage1().getValue() != null ||
                viewModel.getImage2().getValue() != null ||
                viewModel.getImage3().getValue() != null) {
            Long id_photo_tag = ObjectBoxHelper.getIdForPhotographedTag();
            if (id_photo_tag != null) {
                viewModel.removeObservationType((int) id_photo_tag.intValue());
            }
        }
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
        if (viewModel.isSaveEnabled()) {
            item.setEnabled(true);
            Objects.requireNonNull(item.getIcon()).setAlpha(255);
        } else {
            // disabled
            item.setEnabled(false);
            Objects.requireNonNull(item.getIcon()).setAlpha(30);
        }
        return true;
    }

    // Process running after clicking the toolbar buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            backPressed();
        }
        if (id == R.id.action_save_entry) {
            saveEntry1();
        }
        return true;
    }

    private void backPressed() {
        deleteUnsavedImages();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(PreparePhotos.RESIZED)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    // On click
    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.text_view_atlas_code:
                getAtlasCodeForList();
                break;
            case R.id.text_view_stages:
                getStageForTaxon();
                break;
            case R.id.male:
                Log.d(TAG, "Males checkbox selected!");
                setMalesFemalesChecked();
                break;
            case R.id.female:
                Log.d(TAG, "Females checkbox selected!");
                setMalesFemalesChecked();
                break;
            case R.id.ib_pic1_del:
                Log.i(TAG, "Deleting image 1.");
                frameLayoutPicture1.setVisibility(View.GONE);
                disablePhotoButtons(false);
                deleteImageFile(viewModel.getImage1().getValue());
                viewModel.setImage1(null);
                break;
            case R.id.ib_pic1:
                Log.i(TAG, "Image 1 clicked. URL: " + viewModel.getImage1().getValue());
                viewImage(viewModel.getImage1().getValue());
                break;
            case R.id.ib_pic2_del:
                frameLayoutPicture2.setVisibility(View.GONE);
                disablePhotoButtons(false);
                deleteImageFile(viewModel.getImage2().getValue());
                viewModel.setImage2(null);
                break;
            case R.id.ib_pic2:
                Log.i(TAG, "Image 2 clicked. URL: " + viewModel.getImage2().getValue());
                viewImage(viewModel.getImage2().getValue());
                break;
            case R.id.ib_pic3_del:
                frameLayoutPicture3.setVisibility(View.GONE);
                disablePhotoButtons(false);
                deleteImageFile(viewModel.getImage3().getValue());
                viewModel.setImage3(null);
                break;
            case R.id.ib_pic3:
                Log.i(TAG, "Image 3 clicked. URL: " + viewModel.getImage3().getValue());
                viewImage(viewModel.getImage3().getValue());
                break;
            case R.id.dead_specimen:
                viewModel.checkDead();
                break;
            case R.id.iv_map:
                showMap();
                break;
            case R.id.image_view_take_photo_camera:
                takePhoto();
                break;
            case R.id.image_view_take_photo_gallery:
                pickMultipleImages.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .setMaxItems(getEmptyImageSlots())
                                .build());
                break;
        }
    }

    private void setMalesFemalesChecked() {
        boolean males = checkBox_males.isChecked();
        boolean females = checkBox_females.isChecked();
        Log.d(TAG, "Males checkbox is " + males + ". Females checkbox is " + females);
        if (males && females) {
            textViewSpecimensNo1.setVisibility(View.VISIBLE);
            textViewSpecimensNo1.setHint(getString(R.string.number_of_males));
            textViewSpecimensNo2.setVisibility(View.VISIBLE);
            textViewSpecimensNo2.setHint(getString(R.string.number_of_females));
        }
        if (males && !females) {
            textViewSpecimensNo1.setVisibility(View.VISIBLE);
            textViewSpecimensNo1.setHint(getString(R.string.number_of_males));
            textViewSpecimensNo2.setVisibility(View.GONE);
            viewModel.setNumberOfSpecimens2(null);
        }
        if (!males && females) {
            textViewSpecimensNo1.setVisibility(View.GONE);
            viewModel.setNumberOfSpecimens(null);
            textViewSpecimensNo2.setVisibility(View.VISIBLE);
            textViewSpecimensNo2.setHint(getString(R.string.number_of_females));
        }
        if (!males && !females) {
            textViewSpecimensNo1.setVisibility(View.VISIBLE);
            textViewSpecimensNo1.setHint(getString(R.string.broj_jedinki));
            viewModel.setNumberOfSpecimens(null);
            textViewSpecimensNo2.setVisibility(View.GONE);
            viewModel.setNumberOfSpecimens2(null);
        }
    }

    private void viewImage(String image) {
        Intent intent = new Intent(this, ActivityViewImage.class);
        intent.putExtra("image", image);
        startActivity(intent);
    }

    private void deleteImageFile(String image) {
        if (image != null) {
            String filename = new File(image).getName();
            final File file = new File(getFilesDir(), filename);
            boolean b = file.delete();
            Log.d(TAG, "Deleting image " + filename + " returned: " + b);
        }
    }

    /*
    /  PART 1: Check if taxon exist in GreenDao database
    */
    private void saveEntry1() {
        // Ensure that the taxon is entered correctly
        if (viewModel.getTaxonId() == null) {
            Log.d(TAG, "The taxon does not exist in GreenDao database. Asking user to save it as is.");
            buildAlertMessageInvalidTaxon();
        } else {
            TaxonDb taxon = ObjectBoxHelper.getTaxonById(viewModel.getTaxonId());
            Log.d(TAG, "The taxon with ID " + viewModel.getTaxonId()
                    + " selected. Checking coordinates and saving the entry!");
            viewModel.setTaxonId(taxon.getId());
            saveEntry2();
        }
    }

    /*
    /  PART 2: Check if the coordinates are OK
    */
    private void saveEntry2() {
        // If the location is not loaded, warn the user and
        // don’t send crappy data into the online database!
        LatLng latLng = viewModel.getCoordinates().getValue();
        if (latLng != null) {
            if (latLng.latitude == 0) {
                buildAlertMessageNoCoordinates();
            } else {
                // If the location is not precise ask the user to
                // wait. But, not if the location is taken from the map
                // or if the user opened existing entry.
                if (viewModel.getAccuracy().getValue() == null ||
                        viewModel.getAccuracy().getValue() <= 25 ||
                        locationFromTheMap ||
                        !isNewEntry()) {
                    saveEntry3();
                } else {
                    buildAlertMessageImpreciseCoordinates();
                }
            }
        }
    }

    /*
    /  PART 3: Check if the location has been changed and ask the user to update location name
    */
    private void saveEntry3() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String location_name_from_preferences = sharedPreferences.getString("location_name", null);
        // If the location is not given or the user opened existing entry, just continue.
        if (location_name_from_preferences == null
                || location_name_from_preferences.isEmpty()
                || !isNewEntry()) {
            saveEntry4();
        } else {
            // If the user selected the location name we should check if he left the location.
            // or if he is still there
            viewModel.setLocation(location_name_from_preferences);
            Location old_location = new Location("");

            if (SettingsManager.getPreviousLocationLong() == null) {
                // For the first time, just write the coordinates in the SettingsManager.
                SettingsManager.setPreviousLocationLong(viewModel.getLongitudeString());
                SettingsManager.setPreviousLocationLat(viewModel.getLatitudeString());
                saveEntry4();
            } else {
                // It there are coordinates already saved, read them and compare to the current location.
                old_location.setLongitude(Double.parseDouble(SettingsManager.getPreviousLocationLong()));
                old_location.setLatitude(Double.parseDouble(SettingsManager.getPreviousLocationLat()));
                Location current_location = new Location("");
                current_location.setLongitude(Double.parseDouble(viewModel.getLongitudeString()));
                current_location.setLatitude(Double.parseDouble(viewModel.getLatitudeString()));
                double distance = old_location.distanceTo(current_location);

                if (distance > 3000) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getString(R.string.location_changed,
                                    viewModel.getLocation().getValue()))
                            .setCancelable(false)
                            .setTitle(R.string.location_changed_title)

                            // If a user choose to update location name
                            .setPositiveButton(getString(R.string.update_location_name), (dialog, id) -> {
                                SettingsManager.setPreviousLocationLong(viewModel.getLongitudeString());
                                SettingsManager.setPreviousLocationLat(viewModel.getLatitudeString());
                                dialog.dismiss();

                                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                                builder1.setTitle(R.string.location);

                                View view = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
                                TextInputEditText input = view.findViewById(R.id.edit_text_input);

                                input.setText(location_name_from_preferences);
                                input.setInputType(InputType.TYPE_CLASS_TEXT);

                                builder1.setView(view);
                                builder1.setCancelable(false);
                                builder1.setPositiveButton(R.string.ok, (dialog1, which) -> {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    if (input.getText() != null) {
                                        String new_location_name = input.getText().toString();
                                        if (new_location_name.isEmpty()) {
                                            SettingsManager.setPreviousLocationLong(null);
                                            SettingsManager.setPreviousLocationLat(null);
                                            viewModel.setLocation(new_location_name);
                                        }
                                        editor.putString("location_name", new_location_name);
                                    } else {
                                        SettingsManager.setPreviousLocationLong(null);
                                        SettingsManager.setPreviousLocationLat(null);
                                        editor.putString("location_name", "");
                                        viewModel.setLocation(null);
                                    }
                                    editor.apply();
                                    dialog1.dismiss();
                                    saveEntry4();
                                });
                                builder1.show();
                            })

                            // If a user chooses to keep the current location name
                            .setNeutralButton(getString(R.string.keep_location_name), (dialog, id) -> {
                                // Update location within the settings to the current one
                                SettingsManager.setPreviousLocationLong(viewModel.getLongitudeString());
                                SettingsManager.setPreviousLocationLat(viewModel.getLatitudeString());
                                dialog.dismiss();
                                saveEntry4();
                            })

                            // If the user chooses to remove location name
                            .setNegativeButton(getString(R.string.remove_location_name), (dialog, id) -> {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("location_name", null);
                                editor.apply();
                                SettingsManager.setPreviousLocationLong(null);
                                SettingsManager.setPreviousLocationLat(null);
                                viewModel.setLocation(null);
                                dialog.dismiss();
                                saveEntry4();
                            });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    saveEntry4();
                }
            }
        }
    }

    /*
    /  PART 4: Prepare entry and check if both male and female entries should be created
    */
    private void saveEntry4() {
        String specimens_1 = editTextSpecimensNo1.getText().toString();
        Integer specimensNumber1 = (!specimens_1.isEmpty()) ? Integer.valueOf(specimens_1) : null;
        Log.d(TAG, "Number of specimens 1: " + specimensNumber1);
        String specimens_2 = editTextSpecimensNo2.getText().toString();
        Integer specimensNumber2 = (!specimens_2.isEmpty()) ? Integer.valueOf(specimens_2) : null;
        Log.d(TAG, "Number of specimens 2: " + specimensNumber2);

        viewModel.setComment(editTextComment.getText().toString());
        viewModel.setStage((textViewStage.getTag() != null)
                ? Long.parseLong(textViewStage.getTag().toString()) : null);
        viewModel.setCauseOfDeath((editTextDeathComment.getText() != null)
                ? editTextDeathComment.getText().toString() : "");
        viewModel.setHabitat(editTextHabitat.getText() != null
                ? editTextHabitat.getText().toString() : "");
        viewModel.setFoundOn(editTextFoundOn.getText() != null
                ? editTextFoundOn.getText().toString() : "");
        viewModel.setNumberOfSpecimens(specimensNumber1);
        if (viewModel.getTaxonSuggestion() == null) {
            viewModel.setTaxonSuggestion(autoCompleteTextView_speciesName.getText().toString());
        }

        Long second_entry_id = null;
        if (checkBox_males.isChecked() && !checkBox_females.isChecked()) {
            Log.d(TAG, "Only male individuals selected.");
            viewModel.setSex("male");
            long entry_id = saveEntryToObjectBox();
            entrySaver(entry_id, second_entry_id);
        }
        if (!checkBox_males.isChecked() && checkBox_females.isChecked()) {
            Log.d(TAG, "Only female individuals selected.");
            viewModel.setNumberOfSpecimens(specimensNumber2);
            long entry_id = saveEntryToObjectBox();
            entrySaver(entry_id, second_entry_id);
        }
        if (!checkBox_males.isChecked() && !checkBox_females.isChecked()) {
            Log.d(TAG, "No sex of individuals selected.");
            viewModel.setSex("");
            long entry_id = saveEntryToObjectBox();
            entrySaver(entry_id, second_entry_id);
        }
        if (checkBox_males.isChecked() && checkBox_females.isChecked()) {
            Log.d(TAG, "Both male and female individuals selected.");
            viewModel.setSex("male");
            second_entry_id = saveEntryToObjectBox();
            viewModel.setSex("female");
            viewModel.setNumberOfSpecimens(specimensNumber2);
            long entry_id = saveEntryToObjectBox();
            entrySaver(entry_id, second_entry_id);
        }
    }

    private long saveEntryToObjectBox() {
        // Write observation into the ObjectBox database
        EntryDb entry = viewModel.getObservation();
        long entryId = ObjectBoxHelper.setObservation(entry);
        Log.d(TAG, "Entry will be saved under ID " + entryId);
        return entryId;
    }

    //  Gather all the data into the Entry and wright it into the GreenDao database.
    private void entrySaver(long entry_id, Long second_entry_id) {
        // Finish the Activity
        Intent intent = new Intent();
        intent.putExtra("IS_NEW_ENTRY", isNewEntry());
        intent.putExtra("ENTRY_LIST_ID", entry_id);
        intent.putExtra("SECOND_ENTRY_ID", second_entry_id);
        setResult(RESULT_OK, intent);
        finish();
        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
    }

    private void getPhotoTag() {
        if (viewModel.getImage1().getValue() != null ||
                viewModel.getImage2().getValue() != null ||
                viewModel.getImage3().getValue() != null) {
            Long photo_tag_id = ObjectBoxHelper.getIdForPhotographedTag();
            if (photo_tag_id != null) {
                viewModel.addObservationType(photo_tag_id.intValue());
            }
        }
    }

    private Long getAtlasCode() {
        String[] atlas_codes = {getString(R.string.atlas_code_0), getString(R.string.atlas_code_1), getString(R.string.atlas_code_2),
                getString(R.string.atlas_code_3), getString(R.string.atlas_code_4), getString(R.string.atlas_code_5),
                getString(R.string.atlas_code_6), getString(R.string.atlas_code_7), getString(R.string.atlas_code_8),
                getString(R.string.atlas_code_9), getString(R.string.atlas_code_10), getString(R.string.atlas_code_11),
                getString(R.string.atlas_code_12), getString(R.string.atlas_code_13), getString(R.string.atlas_code_14),
                getString(R.string.atlas_code_15), getString(R.string.atlas_code_16)};
        String atlas_code = textViewAtlasCode.getText().toString();
        int atlas_code_id = Arrays.asList(atlas_codes).indexOf(atlas_code);
        if (atlas_code_id == -1) {
            Log.i(TAG, "Atlas code is not selected.");
            return null;
        } else {
            Log.i(TAG, "Setting the atlas code to: " + atlas_code_id);
            return (long) atlas_code_id;
        }
    }

    private Boolean isStageAvailable(Long taxonID) {
        // When the taxon is not selected from the list its value is null and there is no stage.
        if (taxonID == null) {
            return false;
        }
        // When the taxon is selected from the list we should query SQL to see if there is a stage.
        else {
            int size = ObjectBoxHelper.getStagesForTaxonIdCount(taxonID);
            return size != 0;
        }
    }

    private void getStageForTaxon() {
        if (viewModel.getTaxonId() != null) {
            TaxonDb taxon = ObjectBoxHelper.getTaxonById(viewModel.getTaxonId());
            String stages = taxon.getStages();
            if (stages == null || stages.trim().isEmpty()) {
                textViewStage.setEnabled(false);
                Log.d(TAG, "Stage list from selectedTaxon is null or empty for taxon " + getLatinName() + ".");
            } else {
                String[] all_stages_ids = stages.split(";");
                if (all_stages_ids.length != 0) {
                    final String[] all_stages_names = new String[all_stages_ids.length + 1];
                    all_stages_names[0] = getString(R.string.not_selected);
                    for (int i = 0; i < all_stages_ids.length; i++) {
                        StageDb stage = ObjectBoxHelper.getStageById(Long.parseLong(all_stages_ids[i]));
                        if (stage != null) {
                            all_stages_names[i + 1] = StageAndSexLocalization.getStageLocale(this, stage.getName());
                        }
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setItems(all_stages_names, (DialogInterface dialogInterface, int i) -> {
                        textViewStage.setText(all_stages_names[i]);
                        if (i == 0) {
                            textViewStage.setTag(null); // If no stage selected
                            viewModel.setStage(null);
                        } else {
                            textViewStage.setTag(all_stages_ids[i - 1]);
                            viewModel.setStage(Long.parseLong(all_stages_ids[i - 1]));
                            Log.d(TAG, "Setting stage to: " + all_stages_ids[i - 1]);
                        }
                    });
                    builder.show();
                    Log.d(TAG, "Available stages for " + getLatinName() + " include: " + Arrays.toString(all_stages_names));

                } else {
                    textViewStage.setEnabled(false);
                    Log.d(TAG, "Stage list from GreenDao is empty for taxon " + getLatinName() + ".");
                }
            }
        }
    }

    private void getAtlasCodeForList() {
        final String[] atlas_codes = {getString(R.string.not_selected),
                getString(R.string.atlas_code_0), getString(R.string.atlas_code_1),
                getString(R.string.atlas_code_2), getString(R.string.atlas_code_3),
                getString(R.string.atlas_code_4), getString(R.string.atlas_code_5),
                getString(R.string.atlas_code_6), getString(R.string.atlas_code_7),
                getString(R.string.atlas_code_8), getString(R.string.atlas_code_9),
                getString(R.string.atlas_code_10), getString(R.string.atlas_code_11),
                getString(R.string.atlas_code_12), getString(R.string.atlas_code_13),
                getString(R.string.atlas_code_14), getString(R.string.atlas_code_15),
                getString(R.string.atlas_code_16)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(atlas_codes, (dialogInterface, i)
                -> textViewAtlasCode.setText(atlas_codes[i]));
        builder.show();

        // If user select atlas code 2, the "call" tag should also be checked.
        textViewAtlasCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Long atlas_code_id = getAtlasCode();
                viewModel.setAtlasCode(atlas_code_id);
                if (callTagIndexNumber != null) {
                    Chip chip = (Chip) observation_types.getChildAt(callTagIndexNumber);
                    if (atlas_code_id != null && atlas_code_id == 2) {
                        Log.d(TAG, "This atlas code assume that the bird was calling...");
                        chip.setChecked(true);
                        callTagAutoChecked = true;
                    } else {
                        if (callTagAutoChecked) {
                            chip.setChecked(false);
                            callTagAutoChecked = false;
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    private String setAtlasCode(int index) {
        final String[] atlas_codes = {getString(R.string.atlas_code_0), getString(R.string.atlas_code_1), getString(R.string.atlas_code_2),
                getString(R.string.atlas_code_3), getString(R.string.atlas_code_4), getString(R.string.atlas_code_5),
                getString(R.string.atlas_code_6), getString(R.string.atlas_code_7), getString(R.string.atlas_code_8),
                getString(R.string.atlas_code_9), getString(R.string.atlas_code_10), getString(R.string.atlas_code_11),
                getString(R.string.atlas_code_12), getString(R.string.atlas_code_13), getString(R.string.atlas_code_14),
                getString(R.string.atlas_code_15), getString(R.string.atlas_code_16)};
        return atlas_codes[index];
    }

    private void showMap() {
        Intent intent = new Intent(this, ActivityMap.class);
        // If location is not loaded put some location on the map at the Balkan
        Bundle bundle = new Bundle();
        if (viewModel.getLatitude() == null || viewModel.getLatitude() == 0) {
            String database = SettingsManager.getDatabaseName();
            if (database.equals("https://biologer.org")) {
                viewModel.setCoordinates(new LatLng(44.0, 20.8));
            }
            if (database.equals("https://dev.biologer.org")) {
                viewModel.setCoordinates(new LatLng(44.0, 20.8));
            }
            if (database.equals("https://biologer.hr")) {
                viewModel.setCoordinates(new LatLng(45.5, 16.3));
            }
            if (database.equals("https://biologer.ba")) {
                viewModel.setCoordinates(new LatLng(44.3, 17.9));
            }
        }
        bundle.putDouble("LATITUDE", viewModel.getLatitude());
        bundle.putDouble("LONGITUDE", viewModel.getLongitude());
        bundle.putDouble("ACCURACY", viewModel.getRoundedAccuracy());
        bundle.putDouble("ELEVATION", viewModel.getRoundedElevation());
        bundle.putString("LOCATION", viewModel.getLocation().getValue());
        intent.putExtras(bundle);
        openMap.launch(intent);
    }

    private void disablePhotoButtons(Boolean value) {
        if (value) {
            imageViewGallery.setEnabled(false);
            imageViewGallery.setImageAlpha(20);
            imageViewCamera.setEnabled(false);
            imageViewCamera.setImageAlpha(20);
        } else {
            imageViewGallery.setEnabled(true);
            imageViewGallery.setImageAlpha(255);
            imageViewCamera.setEnabled(true);
            imageViewCamera.setImageAlpha(255);
        }
    }

    private void takePhoto() {
        // For Android <= P we need permission to write external storage
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // If permission already granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                takePhotoFromCamera();
            }

            // If permission was denied before
            else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.request_file_camera_permissions)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                            String[] perm = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
                            requestCameraAndWriteExternalStoragePermissions.launch(perm);
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss());
                AlertDialog alert = builder.create();
                alert.show();
            }

            // If permission is asked for the first time
            else {
                String[] perm = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
                requestCameraAndWriteExternalStoragePermissions.launch(perm);
            }
        }

        // For recent android we don’t need permit for external storage
        else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePhotoFromCamera();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.request_camera_permissions)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> requestCameraPermission.launch(Manifest.permission.CAMERA))
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.dismiss());
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
            }
        }
    }

    // Taking pictures from camera
    private final ActivityResultLauncher<Uri> takePictureFromCamera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        Log.i(TAG, "Camera returned picture.");
                        resizeAndDisplayImage(viewModel.getCurrentImage());
                    }
                }
            }
    );

    private void takePhotoFromCamera() {
        Uri current_image = FileManipulation.newExternalDocumentFile(this, null, ".jpg");
        viewModel.setCurrentImage(current_image);
        takePictureFromCamera.launch(current_image);
    }

    ActivityResultLauncher<PickVisualMediaRequest> pickMultipleImages = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(), photoPicker -> {
                if (!photoPicker.isEmpty()) {
                    int allowedImages = getEmptyImageSlots();
                    if (photoPicker.size() > allowedImages) {
                        Toast.makeText(this,
                                getString(R.string.limit_photo1) + " " + photoPicker.size() + " " +
                                        getString(R.string.limit_photo2) + " " + allowedImages + " " +
                                        getString(R.string.limit_photo3), Toast.LENGTH_LONG).show();
                    }

                    for (Uri uri : photoPicker) {
                        resizeAndDisplayImage(uri);

                        // TODO get location from image Exif data of the image, issue #27
                        ExifInterface exifInterface;
                        try {
                            exifInterface = getExifData(uri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        Log.i(TAG, "Exif data: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) + " (date); " +
                                exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP) + " (time); " +
                                exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE) + " (lat); " +
                                exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) + " (lat_ref); " +
                                exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) + " (long); " +
                                exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) + " (long_ref); " +
                                exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) + " (processing).");
                    }
                } else {
                    Toast.makeText(this, getString(R.string.no_photo_selected), Toast.LENGTH_LONG).show();
                }
            });

    private ExifInterface getExifData(Uri imageUri) throws IOException {
        ExifInterface exifData = null;
        InputStream imageStream = getContentResolver().openInputStream(imageUri);
        if (imageStream != null) {
            exifData = new ExifInterface(imageStream);
            imageStream.close();
        }
        return exifData;
    }

    private final ActivityResultLauncher<Intent> openMap = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    locationManager.removeUpdates(locationListener);
                    if (result.getData() != null) {
                        viewModel.setCoordinates(getLatLongFromMap(result));
                        viewModel.setAccuracy(getAccuracyFromMap(result));
                        viewModel.setElevation(getElevationFromMap(result));
                        viewModel.setLocation(getLocationNameFromMap(result));
                        Log.d(TAG, "Map returned this result: " + getLatLongFromMap(result));
                        locationFromTheMap = true;
                    }
                }
            });

    private int getEmptyImageSlots() {
        int emptySlots = 0;
        if (viewModel.getImage1().getValue() == null) {
            emptySlots = emptySlots + 1;
        } if (viewModel.getImage2().getValue() == null) {
            emptySlots = emptySlots + 1;
        } if (viewModel.getImage3().getValue() == null) {
            emptySlots = emptySlots + 1;
        }
        return emptySlots;
    }

    // Function used to retrieve the location
    private void getLocation(int time, int distance) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, locationListener);
        }

        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            Log.d(TAG, "User already selected permissions for GPS location.");
            if (preferences.getBoolean("ask_location", true)) {
                Log.d(TAG, "Asking user weather to use location or not.");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.request_location_permissions)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                            String[] perm = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                            requestLocationPermissions.launch(perm);
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, id) -> {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("ask_location", false);
                            editor.apply();
                            dialog.dismiss();
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }

        else {
            String[] perm = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
            requestLocationPermissions.launch(perm);
        }
    }

    // Show the message if the taxon is not chosen from the taxonomic list
    protected void buildAlertMessageInvalidTaxon() {
        final AlertDialog.Builder builder_taxon = new AlertDialog.Builder(ActivityObservation.this);
        builder_taxon.setMessage(getString(R.string.invalid_taxon_name))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.save_anyway), (dialog, id) -> {
                    // Save custom taxon with no ID.
                    // Just send null ID and do the rest in entryChecker.
                    saveEntry2();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.dismiss());
        final AlertDialog alert = builder_taxon.create();
        alert.show();
    }

    // Show the message if the location is not loaded
    protected void buildAlertMessageNoCoordinates() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.location_is_zero))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.wait), (dialog, id) -> {
                    getLocation(0, 0);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> {
                    finish();
                    deleteUnsavedImages();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    protected void buildAlertMessageImpreciseCoordinates() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.unprecise_coordinates))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.wait), (dialog, id) -> {
                    getLocation(0, 0);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.save_anyway), (dialog, id) -> {
                    // Save the taxon
                    saveEntry3();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Get Location if user refresh the view
    @Override
    public void onRefresh() {
        if (isNewEntry()) {
            swipeRefreshLayout.setRefreshing(true);
            getLocation(0, 0);
            swipeRefreshLayout.setRefreshing(false);
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.gps_update))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), (dialog, id) -> {
                        swipeRefreshLayout.setRefreshing(true);
                        getLocation(0, 0);
                        swipeRefreshLayout.setRefreshing(false);
                    })
                    .setNegativeButton(getString(R.string.no), (dialog, id) -> {
                        dialog.cancel();
                        swipeRefreshLayout.setRefreshing(false);
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void deleteUnsavedImages() {
        for (String image: viewModel.getListNewImages()) {
            if (image != null) {
                String filename = new File(image).getName();
                final File file = new File(getFilesDir(), filename);
                boolean b = file.delete();
                Log.d(TAG, "Deleting image " + image + " returned: " + b);
            }
        }
    }

    private TaxonDb getSelectedTaxon(Long taxonID) {
        if (taxonID == null) {
            return null;
        } else {
            TaxonDb taxon = ObjectBoxHelper.getTaxonById(taxonID);
            Log.d(TAG, "Selected taxon latin name is: " + taxon.getLatinName() + ". Taxon ID: " + taxon.getId());
            return taxon;
        }
    }

    private String getLatinName() {
        String entered_taxon_name = autoCompleteTextView_speciesName.getText().toString();
        return entered_taxon_name.split(" \\(")[0];
    }

    // When screen is rotated activity is destroyed, thus images should be saved and opened again!
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        Log.d(TAG, "Activity will be recreated. Saving the state!");
        super.onSaveInstanceState(outState);
    }

    private void fillObservationTypes() {
        List<ObservationTypesDb> list = ObjectBoxHelper.getObservationTypes();
        long number_of_observation_types = list.size();

        Log.d(TAG, "Filling types of observations with " + Localisation.getLocaleScript() + " script names. Total of " + number_of_observation_types + " entries.");

        for (int i = 0; i < number_of_observation_types; i++) {
            ObservationTypesDb observation_type =  list.get(i);
            String slug = observation_type.getSlug();
            int observation_id = (int)observation_type.getObservationId();

            if (slug.equals("observed") || slug.equals("photographed")) {
                Log.d(TAG, "Ignoring Chip for " + slug + " with ID " + observation_type.getObservationId());
            } else {
                Log.d(TAG, "Adding Chip for " + slug + " with ID " + observation_type.getObservationId());
                Chip chip = new Chip(this);
                // This selects the type of Chip to add. We are adding Filter Chips that are checkable.
                ChipDrawable chipDrawable = ChipDrawable.createFromAttributes(this, null, 0, R.style.Widget_MaterialComponents_Chip_Filter);
                chip.setChipDrawable(chipDrawable);
                chip.setId(observation_id);
                chip.setTag(slug);
                chip.setText(observation_type.getName());
                int[] ids = viewModel.getObservationTypes();
                chip.setChecked(ArrayHelper.arrayContainsNumber(ids, observation_id));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    String text = (String) compoundButton.getTag();
                    int id = compoundButton.getId();
                    if (compoundButton.isChecked()) {
                        Log.d(TAG, "Chip button \"" + text + "\" selected, ID: " + id);
                        viewModel.addObservationType(id);
                    } else {
                        Log.d(TAG, "Chip button \"" + text + "\" deselected, ID: " + id);
                        viewModel.removeObservationType(id);
                    }
                });
                observation_types.addView(chip);
                if (slug.equals("call")) {
                    callTagIndexNumber = observation_types.getChildCount() - 1;
                    Log.d(TAG, "The index number of the call chip is " + callTagIndexNumber);
                }
            }

        }
    }

    private void resizeAndDisplayImage(Uri uri) {
        // Start another Activity to resize captured image.
        Intent resizeImage = new Intent(this, PreparePhotos.class);
        resizeImage.putExtra("image_uri", String.valueOf(uri));
        startService(resizeImage);
    }

    private final ActivityResultLauncher<String[]> requestLocationPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> isGranted) -> {
                        boolean flag = isGranted.containsValue(false);
                        if (!flag) {
                            getLocation(100, 2);
                        } else {
                            Toast.makeText(this, "No permission to take picture from camera, field observations will have no images.", Toast.LENGTH_LONG).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            takePhotoFromCamera();
                        } else {
                            Toast.makeText(this, "No permission to take picture from camera, field observations will have no images.", Toast.LENGTH_LONG).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String[]> requestCameraAndWriteExternalStoragePermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> isGranted) -> {
                        boolean flag = isGranted.containsValue(false);
                        if (!flag) {
                            takePhotoFromCamera();
                        } else {
                            Toast.makeText(this, getString(R.string.no_permission_camera), Toast.LENGTH_LONG).show();
                        }
                    }
            );

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public LatLng getLatLongFromMap(ActivityResult result) {
        if (result.getData() != null) {
            if (result.getData().getExtras() != null) {
                String latitude = result.getData().getExtras().getString("google_map_lat");
                String longitude = result.getData().getExtras().getString("google_map_long");
                if (latitude != null && longitude != null) {
                    Log.i(TAG, "LonLat: " + longitude + "; " + latitude + ".");
                    return new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }
            }
        }
        return null;
    }

    public Double getAccuracyFromMap(ActivityResult result) {
        if (result.getData() != null) {
            if (result.getData().getExtras() != null) {
                return Double.valueOf(Objects.requireNonNull(result.getData().getExtras().getString("google_map_accuracy"), "Map accuracy must not be null!"));
            }
        }
        return null;
    }

    public Double getElevationFromMap(ActivityResult result) {
        if (result.getData() != null) {
            if (result.getData().getExtras() != null) {
                return Double.valueOf(Objects.requireNonNull(result.getData().getExtras().getString("google_map_elevation"), "Map elevation must not be null!"));
            }
        }
        return null;
    }

    public String getLocationNameFromMap(ActivityResult result) {
        if (result.getData() != null) {
            if (result.getData().getExtras() != null) {
                return result.getData().getExtras().getString("google_map_location_name");
            }
        }
        return null;
    }
}