package org.biologer.biologer.gui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.CameraActivity;
import org.biologer.biologer.adapters.PreparePhotos;
import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.sql.ObservationTypesData;
import org.biologer.biologer.sql.ObservationTypesDataDao;
import org.biologer.biologer.sql.Stage;
import org.biologer.biologer.sql.StageDao;
import org.biologer.biologer.sql.TaxaTranslationData;
import org.biologer.biologer.sql.TaxaTranslationDataDao;
import org.biologer.biologer.sql.TaxonData;
import org.biologer.biologer.sql.TaxonDataDao;
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsDataDao;
import org.biologer.biologer.sql.UserData;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EntryActivity extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "Biologer.Entry";

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 1005;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1006;
    private static final int REQUEST_LOCATION = 1;

    private LocationManager locationManager;
    private LocationListener locationListener;
    String latitude = "0", longitude = "0";
    private double elev = 0.0;
    private LatLng currentLocation = new LatLng(0.0, 0.0);
    private Double acc = 0.0;
    private final int CAMERA = 2;
    private final int MAP = 3;
    private TextInputLayout textViewAtlasCodeLayout, textViewSpecimensNo1, textViewSpecimensNo2, textViewDeathComment;
    private TextView textViewGPSAccuracy, textViewStage, textViewLatitude, textViewLongitude, textViewAtlasCode, textViewMeters;
    private EditText editTextDeathComment, editTextComment, editTextSpecimensNo1, editTextSpecimensNo2, editTextHabitat, editTextFoundOn;
    private MaterialCheckBox checkBox_males, checkBox_females, checkBox_dead;
    AutoCompleteTextView acTextView;
    FrameLayout frameLayoutPicture1, frameLayoutPicture2, frameLayoutPicture3;
    ImageView imageViewPicture1, imageViewPicture1Del, imageViewPicture2, imageViewPicture2Del,
            imageViewPicture3, imageViewPicture3Del, imageViewMap, imageViewCamera, imageViewGallery;
    ChipGroup observation_types;
    LinearLayout detailedEntry, layoutCoordinates, layoutUnknownCoordinates;
    private boolean save_enabled = false;
    private String image1, image2, image3;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Entry currentItem;
    private String locale_script = "en";
    Calendar calendar;
    SimpleDateFormat simpleDateFormat;
    // Get the data from the GreenDao database
    List<UserData> userDataList = App.get().getDaoSession().getUserDataDao().loadAll();
    List<Stage> stageList = App.get().getDaoSession().getStageDao().loadAll();
    String observation_type_ids_string;
    int[] observation_type_ids = null;
    ArrayList<String> list_new_images = new ArrayList<>();
    ArrayList<Integer> selectedTaxaGroups = new ArrayList<>();

    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(R.string.entry_title);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            checkWriteStoragePermission();
        }
        // Get the system locale to translate names of the taxa
        locale_script = Localisation.getLocaleScript();

        /*
         * Get the view...
         */
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
        detailedEntry = findViewById(R.id.detailed_entry);
        layoutCoordinates = findViewById(R.id.layout_coordinates);
        layoutUnknownCoordinates = findViewById(R.id.layout_unknown_coordinates);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("advanced_interface", false)) {
            detailedEntry.setVisibility(View.VISIBLE);
        }

        // Query to get taxa groups that should be used in a query
        QueryBuilder<TaxonGroupsData> query = App.get().getDaoSession().getTaxonGroupsDataDao().queryBuilder();
        query.where(TaxonGroupsDataDao.Properties.Id.isNotNull());
        List<TaxonGroupsData> allTaxaGroups = query.list();
        for (int i = 0; i < allTaxaGroups.size(); i++) {
            int id = allTaxaGroups.get(i).getId().intValue();
            boolean checked = preferences.getBoolean(allTaxaGroups.get(i).getId().toString(), true);
            if (checked)  {
                Log.d(TAG, "Checkbox for taxa group ID " + id + " is checked.");
                selectedTaxaGroups.add(allTaxaGroups.get(i).getId().intValue());
            } else {
                Log.d(TAG, "Checkbox for taxa group ID " + id + " is not checked.");
            }
        }

        // Restore images on screen rotation...
        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring saved state of captured images in the EntryActivity.");
            image1 = savedInstanceState.getString("image1");
            image2 = savedInstanceState.getString("image2");
            image3 = savedInstanceState.getString("image3");
            if (image1 != null) {
                Glide.with(this)
                        .load(image1)
                        .override(100, 100)
                        .into(imageViewPicture1);
                frameLayoutPicture1.setVisibility(View.VISIBLE);
            }
            if (image2 != null) {
                Glide.with(this)
                        .load(image2)
                        .override(100, 100)
                        .into(imageViewPicture2);
                frameLayoutPicture2.setVisibility(View.VISIBLE);
            }
            if (image3 != null) {
                Glide.with(this)
                        .load(image3)
                        .override(100, 100)
                        .into(imageViewPicture3);
                frameLayoutPicture3.setVisibility(View.VISIBLE);
            }
            list_new_images = savedInstanceState.getStringArrayList("list_new_images");
        }

        // Fill in the drop down menu with list of taxa
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[1]);
        acTextView = findViewById(R.id.textview_list_of_taxa);
        acTextView.setAdapter(adapter);
        acTextView.setThreshold(2);
        // This linear layout holds the stages. We will hide it before the taxon is not selected.
        final TextInputLayout stages = findViewById(R.id.text_input_stages);
        // When user type taxon name...
        acTextView.addTextChangedListener(new TextWatcher() {
            final Handler handler = new Handler();
            Runnable runnable;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(runnable);

                final String typed_name = String.valueOf(s);

                runnable = () -> {
                    /*
                    Get the list of taxa from the GreenDao database
                     */

                    // Query latin names
                    QueryBuilder<TaxonData> latinQuery = App.get().getDaoSession().getTaxonDataDao().queryBuilder();
                    latinQuery.where(TaxonDataDao.Properties.LatinName.like("%" + typed_name + "%"));
                    latinQuery.limit(10);
                    List<TaxonData> latinNames = latinQuery.list();

                    List<String> taxaNames = new ArrayList<>();

                    for (int i = 0; i < latinNames.size(); i++) {
                        Long id = latinNames.get(i).getId();
                        QueryBuilder<TaxaTranslationData> translationQuery = App.get().getDaoSession().getTaxaTranslationDataDao().queryBuilder();
                        translationQuery.where(
                                translationQuery.and(TaxaTranslationDataDao.Properties.TaxonId.eq(id),
                                        TaxaTranslationDataDao.Properties.Locale.eq(locale_script)));
                        List<TaxaTranslationData> translation = translationQuery.list();
                        if (translation.size() >= 1) {
                            String name = translation.get(0).getNativeName();
                            if (name != null) {
                                taxaNames.add(latinNames.get(i).getLatinName() + " (" + translation.get(0).getNativeName() + ")");
                            }
                        } else {
                            taxaNames.add(latinNames.get(i).getLatinName());
                        }
                    }

                    // Query native names
                    QueryBuilder<TaxaTranslationData> nativeQuery = App.get().getDaoSession().getTaxaTranslationDataDao().queryBuilder();

                    // For Serbian language we should also search for latin ans cyrilic names
                    if (locale_script.equals("sr")) {
                        if (preferences.getBoolean("english_names", false)) {
                            nativeQuery.where(
                                    nativeQuery.or(
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq("en"),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%")),
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq("sr"),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%")),
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq("sr-Latn"),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%"))));
                        } else {
                            nativeQuery.where(
                                    nativeQuery.or(
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq("sr"),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%")),
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq("sr-Latn"),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%"))));
                        }
                    }

                    // Fot other languages it is more simple...
                    else {
                        if (preferences.getBoolean("english_names", false)) {
                            nativeQuery.where(
                                    nativeQuery.or(
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq("en"),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%")),
                                            nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq(locale_script),
                                                    TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%"))));
                        } else {
                            nativeQuery.where(
                                    nativeQuery.and(TaxaTranslationDataDao.Properties.Locale.eq(locale_script),
                                            TaxaTranslationDataDao.Properties.NativeName.like("%" + typed_name + "%")));
                        }
                    }

                    nativeQuery.limit(10);
                    List<TaxaTranslationData> nativeNames = nativeQuery.list();
                    for (int i = 0; i < nativeNames.size(); i++) {
                        Long id = nativeNames.get(i).getTaxonId();
                        QueryBuilder<TaxonData> translationQuery = App.get().getDaoSession().getTaxonDataDao().queryBuilder();
                        translationQuery.where(TaxonDataDao.Properties.Id.eq(id));
                        List<TaxonData> translation = translationQuery.list();
                        if (translation.size() >= 1) {
                            String name = translation.get(0).getLatinName();
                            if (name != null) {
                                taxaNames.add(name + " (" + nativeNames.get(i).getNativeName() + ")");
                            }
                        } else {
                            // Should not be called at all, but I will live it just in case...
                            taxaNames.add(nativeNames.get(i).getNativeName());
                        }
                    }

                    // Add the Query to the drop down list
                    ArrayAdapter<String> adapter1 = new ArrayAdapter<>(EntryActivity.this, android.R.layout.simple_dropdown_item_1line, taxaNames);
                    acTextView.setAdapter(adapter1);
                    adapter1.notifyDataSetChanged();

                    /*
                    Update the UI elements
                     */
                    // Enable stage entry
                    TaxonData taxonData = getSelectedTaxon();
                    if (taxonData != null) {
                        // Check if the taxon has stages. If not hide the stages dialog.
                        if (isStageAvailable(taxonData)) {
                            stages.setVisibility(View.VISIBLE);

                            // If user preferences are selected, the stage for taxa will be set to adult by default.
                            // Step 1: Get the preferences
                            if (preferences.getBoolean("adult_by_default", false)) {
                                // If stage is already selected ignore this...
                                if (textViewStage.getText().toString().equals("")) {
                                    stageList = App.get().getDaoSession().getStageDao().queryBuilder()
                                            .where(StageDao.Properties.TaxonId.eq(taxonData.getId()))
                                            .where(StageDao.Properties.Name.eq("adult"))
                                            .list();
                                    if (stageList.get(0) != null) {
                                        Stage stage = new Stage(null, "adult", stageList.get(0).getStageId(), taxonData.getId());
                                        textViewStage.setText(getString(R.string.stage_adult));
                                        textViewStage.setTag(stage);
                                    }
                                }

                            }
                        }
                        Log.d(TAG, "Taxon is selected from the list. Enabling Stages for this taxon.");
                        if (taxonData.isUseAtlasCode()) {
                            textViewAtlasCodeLayout.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Taxon uses the atlas code. Enabling Atlas code for this taxon.");
                        }
                    } else {
                        stages.setVisibility(View.GONE);
                        textViewStage.setTag(null);
                        textViewStage.setText(null);
                        textViewAtlasCodeLayout.setVisibility(View.GONE);
                        textViewAtlasCode.setText("");
                        Log.d(TAG, "Taxon is not selected from the list. Disabling Stages and Atlas Codes for this taxon.");
                    }

                    // Enable/disable Save button in the Toolbar
                    if (acTextView.getText().toString().length() > 1) {
                        save_enabled = true;
                        Log.d(TAG, "Taxon is set to: " + acTextView.getText());
                    } else {
                        save_enabled = false;
                        Log.d(TAG, "Taxon entry field is empty.");
                    }
                    invalidateOptionsMenu();
                };
                handler.postDelayed(runnable, 400);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Define locationListener and locationManager in order to
        // to receive the Location.
        // Call the function updateLocation() to do all the magic...
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                setLocationValues(location.getLatitude(), location.getLongitude());
                elev = location.getAltitude();
                acc = (double) location.getAccuracy();
                textViewGPSAccuracy.setText(String.format(Locale.ENGLISH, "%.0f", acc));
                textViewMeters.setVisibility(View.VISIBLE);
                setAccuracyColor();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(@NonNull String s) {
            }

            @Override
            public void onProviderDisabled(@NonNull String s) {
                //buildAlertMessageNoGps();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Broadcaster used for receiving resized images
        registerBroadcastReceiver();

        // Finally to start the gathering of data...
        startEntryActivity();
        fillObservationTypes();
    }

    private void setAccuracyColor() {
        if (acc != null) {
            if (acc <= 25) {
                textViewMeters.setTextColor(getResources().getColor(R.color.checkBox_text));
                textViewGPSAccuracy.setTextColor(getResources().getColor(R.color.checkBox_text));
            } else {
                textViewMeters.setTextColor(getResources().getColor(R.color.warningRed));
                textViewGPSAccuracy.setTextColor(getResources().getColor(R.color.warningRed));
            }
        }
    }

    private void registerBroadcastReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(PreparePhotos.RESIZED);
                if (s != null) {
                    Log.d(TAG, "Resize Images returned code: " + s);

                    if (s.equals("error")) {
                        Log.d(TAG, "Unknown error. Can not get resized image!");
                    } else {
                        if (image1 == null) {
                            image1 = s;
                            Glide.with(EntryActivity.this)
                                    .load(image1)
                                    .override(100, 100)
                                    .into(imageViewPicture1);
                            frameLayoutPicture1.setVisibility(View.VISIBLE);
                            list_new_images.add(image1);
                        } else if (image2 == null) {
                            image2 = s;
                            Glide.with(EntryActivity.this)
                                    .load(image2)
                                    .override(100, 100)
                                    .into(imageViewPicture2);
                            frameLayoutPicture2.setVisibility(View.VISIBLE);
                            list_new_images.add(image2);
                        } else if (image3 == null) {
                            image3 = s;
                            Glide.with(EntryActivity.this)
                                    .load(image3)
                                    .override(100, 100)
                                    .into(imageViewPicture3);
                            frameLayoutPicture3.setVisibility(View.VISIBLE);
                            list_new_images.add(image3);
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

    /*
    /  If new entry just get the coordinates.
    /  If existing entry get the known values from the entry.
    */
    private void startEntryActivity() {
        if (isNewEntry()) {

            Log.i(TAG, "Starting new entry.");
            getLocation(100, 2);

            // Always add Observation Type for observed specimen
            int id_for_observed_tag = App.get().getDaoSession().getObservationTypesDataDao().queryBuilder()
                    .where(ObservationTypesDataDao.Properties.Slug.eq("observed"))
                    .list().get(0).getObservationId().intValue();
            Log.d(TAG, "Observed tag has ID: " + id_for_observed_tag);
            observation_type_ids = insertIntoArray(observation_type_ids, id_for_observed_tag);

        } else {

            Log.d(TAG, "Opening existing entry.");
            fillExistingEntry();

        }
    }

    private Boolean isNewEntry() {
        String is_new_entry = getIntent().getStringExtra("IS_NEW_ENTRY");
        assert is_new_entry != null;
        return is_new_entry.equals("YES");
    }

    private void fillExistingEntry() {

        Long existing_entry_id = getIntent().getLongExtra("ENTRY_ID", 0);
        currentItem = App.get().getDaoSession().getEntryDao().load(existing_entry_id);
        Log.i(TAG, "Opening existing entry with ID: " + existing_entry_id + ".");

        // Get the latitude, longitude, coordinate precision and elevation...
        currentLocation = new LatLng(currentItem.getLattitude(), currentItem.getLongitude());
        elev = currentItem.getElevation();
        acc = currentItem.getAccuracy();
        textViewLatitude.setText(String.format(Locale.ENGLISH, "%.1f", currentItem.getLattitude()));
        textViewLongitude.setText(String.format(Locale.ENGLISH, "%.1f", currentItem.getLongitude()));
        layoutUnknownCoordinates.setVisibility(View.GONE);
        layoutCoordinates.setVisibility(View.VISIBLE);
        textViewGPSAccuracy.setText(String.format(Locale.ENGLISH, "%.0f", currentItem.getAccuracy()));
        textViewMeters.setVisibility(View.VISIBLE);
        setAccuracyColor();

        // Get the name of the taxon for this entry
        acTextView.setText(currentItem.getTaxonSuggestion());
        acTextView.dismissDropDown();

        // Get the name of the stage for the entry from the database
        if (currentItem.getStage() != null) {
            String stageName = (App.get().getDaoSession().getStageDao().queryBuilder()
                    .where(StageDao.Properties.StageId.eq(currentItem.getStage()))
                    .list().get(1).getName());
            long stage_id = (App.get().getDaoSession().getStageDao().queryBuilder()
                    .where(StageDao.Properties.StageId.eq(currentItem.getStage()))
                    .list().get(1).getStageId());
            Stage stage = new Stage(null, stageName, stage_id, currentItem.getTaxonId());
            textViewStage.setTag(stage);
            textViewStage.setText(stageName);
        }

        // Get the selected sex. If not selected set spinner to default...
        Log.d(TAG, "Sex of individual from previous entry is " + currentItem.getSex());
        if (currentItem.getSex().equals("male")) {
            Log.d(TAG, "Setting spinner selected item to male.");
            checkBox_males.setChecked(true);
        }
        if (currentItem.getSex().equals("female")) {
            Log.d(TAG, "Setting spinner selected item to female.");
            checkBox_females.setChecked(true);
        }

        // Get the atlas code.
        if (currentItem.getAtlas_code() != null) {
            Long code = currentItem.getAtlas_code();
            Log.d(TAG, "Setting the spinner to atlas code: " + code);
            textViewAtlasCode.setText(setAtlasCode(code.intValue()));

        }

        if (currentItem.getDeadOrAlive().equals("true")) {
            // Specimen is a live
            checkBox_dead.setChecked(false);
        } else {
            // Specimen is dead, Checkbox should be activated and Dead Comment shown
            checkBox_dead.setChecked(true);
            showDeadComment();
        }

        // Get the images
        image1 = currentItem.getSlika1();
        if (image1 != null) {
            Glide.with(this)
                    .load(image1)
                    .override(100, 100)
                    .into(imageViewPicture1);
            frameLayoutPicture1.setVisibility(View.VISIBLE);
        }
        image2 = currentItem.getSlika2();
        if (image2 != null) {
            Glide.with(this)
                    .load(image2)
                    .override(100, 100)
                    .into(imageViewPicture2);
            frameLayoutPicture2.setVisibility(View.VISIBLE);
        }
        image3 = currentItem.getSlika3();
        if (image3 != null) {
            Glide.with(this)
                    .load(image3)
                    .override(100, 100)
                    .into(imageViewPicture3);
            frameLayoutPicture3.setVisibility(View.VISIBLE);
        }

        disablePhotoButtons(image1 != null && image2 != null && image3 != null);
        if (currentItem.getHabitat() != null) {
            editTextHabitat.setText(currentItem.getHabitat());
        }
        if (currentItem.getFoundOn() != null) {
            editTextFoundOn.setText(currentItem.getFoundOn());
        }

        // Get other values
        if (currentItem.getCauseOfDeath().length() != 0) {
            editTextDeathComment.setText(currentItem.getCauseOfDeath());
        }
        if (currentItem.getComment().length() != 0) {
            editTextComment.setText(currentItem.getComment());
        }
        if (currentItem.getNoSpecimens() != null) {
            editTextSpecimensNo1.setText(String.valueOf(currentItem.getNoSpecimens()));
        }

        // Load observation types and delete tag for photographed.
        observation_type_ids_string = currentItem.getObservation_type_ids();
        Log.d(TAG, "Loading observation types with IDs " + observation_type_ids_string);
        observation_type_ids = getArrayFromText(observation_type_ids_string);
        if (image1 != null || image2 != null || image3 != null) {
            Log.d(TAG, "Removing image tag just in case images got deleted.");
            int id_photo_tag = App.get().getDaoSession().getObservationTypesDataDao().queryBuilder()
                    .where(ObservationTypesDataDao.Properties.Slug.eq("photographed"))
                    .list().get(0).getObservationId().intValue();
            observation_type_ids = removeFromArray(observation_type_ids, id_photo_tag);
        }
    }

    // Add Save button in the right part of the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        return true;
    }

    // Customize Save item to enable if when needed
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_save);
        if (save_enabled) {
            item.setEnabled(true);
            item.getIcon().setAlpha(255);
        } else {
            // disabled
            item.setEnabled(false);
            item.getIcon().setAlpha(30);
        }
        return true;
    }

    // Process running after clicking the toolbar buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            //this.finish();
            //return true;
        }
        if (id == R.id.action_save) {
            saveEntry1();
        }
        return true;
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

    @Override
    public void onResume() {
        super.onResume();
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
                deleteImageFile(image1);
                image1 = null;
                break;
            case R.id.ib_pic1:
                Log.i(TAG, "Image 1 clicked. URL: " + image1);
                viewImage(image1);
                break;
            case R.id.ib_pic2_del:
                frameLayoutPicture2.setVisibility(View.GONE);
                disablePhotoButtons(false);
                deleteImageFile(image2);
                image2 = null;
                break;
            case R.id.ib_pic2:
                Log.i(TAG, "Image 2 clicked. URL: " + image2);
                viewImage(image2);
                break;
            case R.id.ib_pic3_del:
                frameLayoutPicture3.setVisibility(View.GONE);
                disablePhotoButtons(false);
                deleteImageFile(image3);
                image3 = null;
                break;
            case R.id.ib_pic3:
                Log.i(TAG, "Image 3 clicked. URL: " + image3);
                viewImage(image3);
                break;
            case R.id.dead_specimen:
                showDeadComment();
                break;
            case R.id.iv_map:
                showMap();
                break;
            case R.id.image_view_take_photo_camera:
                takePhoto();
                break;
            case R.id.image_view_take_photo_gallery:
                takePhotoFromGallery();
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
        } if (males && !females) {
            textViewSpecimensNo1.setVisibility(View.VISIBLE);
            textViewSpecimensNo1.setHint(getString(R.string.number_of_males));
            textViewSpecimensNo2.setVisibility(View.GONE);
            editTextSpecimensNo2.setText("");
        } if (!males && females) {
            textViewSpecimensNo1.setVisibility(View.GONE);
            editTextSpecimensNo1.setText("");
            textViewSpecimensNo2.setVisibility(View.VISIBLE);
            textViewSpecimensNo2.setHint(getString(R.string.number_of_females));
        } if (!males && !females) {
            textViewSpecimensNo1.setVisibility(View.VISIBLE);
            textViewSpecimensNo1.setHint(getString(R.string.broj_jedinki));
            editTextSpecimensNo1.setText("");
            textViewSpecimensNo2.setVisibility(View.GONE);
            editTextSpecimensNo2.setText("");
        }
    }

    private void viewImage(String image) {
        Intent intent = new Intent(this, ViewImage.class);
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
        // Insure that the taxon is entered correctly
        TaxonData taxon = getSelectedTaxon();
        if (taxon == null) {
            Log.d(TAG, "The taxon does not exist in GreenDao database. Asking user to save it as is.");
            buildAlertMessageInvalidTaxon();
        } else {
            Log.d(TAG, "The taxon with ID " + taxon.getId() + " selected. Checking coordinates and saving the entry!");
            saveEntry2(taxon);
        }
    }

    /*
    /  PART 2: Check if the coordinates are OK
    */
    private void saveEntry2(TaxonData taxon) {
        // If the location is not loaded, warn the user and
        // don’t send crappy data into the online database!
        if (currentLocation.latitude == 0) {
            buildAlertMessageNoCoordinates();
        } else {
            // If the location is not precise ask the user to
            // wait for a precise location, or to go for it anyhow...
            if (currentLocation.latitude > 0 && acc >= 25) {
                buildAlertMessageImpreciseCoordinates();
            }
        }

        if (currentLocation.latitude > 0 && (acc <= 25)) {
            if (taxon == null) {
                Log.d(TAG, "Saving taxon with unknown ID as simple text.");
                TaxonData taxon_noId = new TaxonData(null, null, acTextView.getText().toString(),
                        null, null, null, false, false, null);
                saveEntry3(taxon_noId);
            } else {
                Log.d(TAG, "Saving taxon with known ID: " + taxon.getId());
                saveEntry3(taxon);
            }
        }
    }

    /*
    /  PART 3: Check if both male and female entries should be created
    */
    private void saveEntry3(TaxonData taxon) {
        String specimens_number_1 = editTextSpecimensNo1.getText().toString();
        String specimens_number_2 = editTextSpecimensNo2.getText().toString();
        if (checkBox_males.isChecked() && !checkBox_females.isChecked()) {
            Log.d(TAG, "Only male individuals selected.");
            entrySaver(taxon, specimens_number_1, "male", 1);
        }
        if (!checkBox_males.isChecked() && checkBox_females.isChecked()) {
            Log.d(TAG, "Only female individuals selected.");
            entrySaver(taxon, specimens_number_2, "female", 1);
        }
        if (!checkBox_males.isChecked() && !checkBox_females.isChecked()) {
            Log.d(TAG, "No sex of individuals selected.");
            entrySaver(taxon, specimens_number_1, "", 1);
        }
        if (checkBox_males.isChecked() && checkBox_females.isChecked()) {
            Log.d(TAG, "Both male and female individuals selected.");
                entrySaver(taxon, specimens_number_1, "male", 1);
                entrySaver(taxon, specimens_number_2, "female", 2);
        }
    }

    //  Gather all the data into the Entry and wright it into the GreenDao database.
    private void entrySaver(TaxonData taxon, String specimens, String sex, int entry_id) {
        Stage stage = (textViewStage.getTag() != null) ? (Stage) textViewStage.getTag() : null;
        String comment = editTextComment.getText().toString();
        Integer numberOfSpecimens = (!specimens.equals("")) ? Integer.valueOf(specimens) : null;
        Long selectedStage = (stage != null) ? stage.getStageId() : null;
        String deathComment = (editTextDeathComment.getText() != null) ? editTextDeathComment.getText().toString() : "";
        String habitat = editTextHabitat.getText() != null ? editTextHabitat.getText().toString() : "";
        String foundOn = editTextFoundOn.getText() != null ? editTextFoundOn.getText().toString() : "";

        if (entry_id != 1 || isNewEntry()) {
            calendar = Calendar.getInstance();
            simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
            String fullDate = simpleDateFormat.format(calendar.getTime());
            String day = fullDate.substring(0, 2);
            String month = fullDate.substring(3, 5);
            String year = fullDate.substring(6, 10);
            String time = fullDate.substring(11, 16);
            Long taxon_id = taxon.getId();
            String taxon_name = taxon.getLatinName();
            String project_name = PreferenceManager.getDefaultSharedPreferences(this).getString("project_name", "0");
            getPhotoTag();
            observation_type_ids_string = Arrays.toString(observation_type_ids);
            Log.d(TAG, "Converting array of observation type IDs into string: " + observation_type_ids_string);

            // Get the data structure and save it into a database Entry
            Entry entry1 = new Entry(null, taxon_id, taxon_name, year, month, day, comment, numberOfSpecimens, sex, selectedStage, getAtlasCode(),
                    String.valueOf(!checkBox_dead.isChecked()), deathComment, currentLocation.latitude, currentLocation.longitude, acc,
                    elev, "", image1, image2, image3, project_name, foundOn, String.valueOf(getGreenDaoDataLicense()),
                    getGreenDaoImageLicense(), time, habitat, observation_type_ids_string);
            App.get().getDaoSession().getEntryDao().insertOrReplace(entry1);
        } else { // if the entry exist already
            currentItem.setTaxonId(taxon.getId());
            currentItem.setTaxonSuggestion(taxon.getLatinName());
            currentItem.setComment(comment);
            currentItem.setNoSpecimens(numberOfSpecimens);
            currentItem.setSex(sex);
            currentItem.setStage(selectedStage);
            currentItem.setAtlasCode(getAtlasCode());
            currentItem.setDeadOrAlive(String.valueOf(!checkBox_dead.isChecked()));
            currentItem.setCauseOfDeath(deathComment);
            currentItem.setLattitude(currentLocation.latitude);
            currentItem.setLongitude(currentLocation.longitude);
            currentItem.setElevation(elev);
            currentItem.setAccuracy(acc);
            currentItem.setSlika1(image1);
            currentItem.setSlika2(image2);
            currentItem.setSlika3(image3);
            currentItem.setHabitat(habitat);
            getPhotoTag();
            currentItem.setObservation_type_ids(Arrays.toString(observation_type_ids));

            // Now just update the database with new data...
            App.get().getDaoSession().getEntryDao().updateInTx(currentItem);

        }
        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void getPhotoTag() {
        if (image1 != null || image2 != null || image3 != null) {
            int id_photo_tag = App.get().getDaoSession().getObservationTypesDataDao().queryBuilder()
                    .where(ObservationTypesDataDao.Properties.Slug.eq("photographed"))
                    .list().get(0).getObservationId().intValue();
            Log.d(TAG, "Photographed tag has ID: " + id_photo_tag);
            observation_type_ids = insertIntoArray(observation_type_ids, id_photo_tag);
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

    private Boolean isStageAvailable(TaxonData taxonData) {
        stageList = App.get().getDaoSession().getStageDao().queryBuilder()
                .where(StageDao.Properties.TaxonId.eq(taxonData.getId()))
                .list();
        return stageList.size() != 0;
    }

    private void getStageForTaxon() {
        TaxonData taxon = App.get().getDaoSession().getTaxonDataDao().queryBuilder()
                .where(TaxonDataDao.Properties.LatinName.eq(getLatinName()))
                .limit(1)
                .unique();
        stageList = App.get().getDaoSession().getStageDao().queryBuilder()
                .where(StageDao.Properties.TaxonId.eq(taxon.getId()))
                .list();
        if (stageList != null) {
            final String[] taxon_stages = new String[stageList.size()];
            for (int i = 0; i < stageList.size(); i++) {
                taxon_stages[i] = stageList.get(i).getName();
                // Translate this to interface...
                if (taxon_stages[i].equals("egg")) {
                    taxon_stages[i] = getString(R.string.stage_egg);
                }
                if (taxon_stages[i].equals("larva")) {
                    taxon_stages[i] = getString(R.string.stage_larva);
                }
                if (taxon_stages[i].equals("pupa")) {
                    taxon_stages[i] = getString(R.string.stage_pupa);
                }
                if (taxon_stages[i].equals("adult")) {
                    taxon_stages[i] = getString(R.string.stage_adult);
                }
                if (taxon_stages[i].equals("juvenile")) {
                    taxon_stages[i] = getString(R.string.stage_juvenile);
                }
            }
            if (taxon_stages.length == 0) {
                Log.d(TAG, "No stages are available for " + getLatinName() + ".");
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(taxon_stages, (dialogInterface, i) -> {
                    textViewStage.setText(taxon_stages[i]);
                    textViewStage.setTag(stageList.get(i));
                });
                builder.show();
                Log.d(TAG, "Available stages for " + getLatinName() + " include: " + Arrays.toString(taxon_stages));
            }
        } else {
            textViewStage.setEnabled(false);
            Log.d(TAG, "Stage list from GreenDao is empty for taxon " + getLatinName() + ".");
        }
    }

    private void getAtlasCodeForList() {
        final String[] atlas_codes = {getString(R.string.not_selected),
                getString(R.string.atlas_code_0), getString(R.string.atlas_code_1), getString(R.string.atlas_code_2),
                getString(R.string.atlas_code_3), getString(R.string.atlas_code_4), getString(R.string.atlas_code_5),
                getString(R.string.atlas_code_6), getString(R.string.atlas_code_7), getString(R.string.atlas_code_8),
                getString(R.string.atlas_code_9), getString(R.string.atlas_code_10), getString(R.string.atlas_code_11),
                getString(R.string.atlas_code_12), getString(R.string.atlas_code_13), getString(R.string.atlas_code_14),
                getString(R.string.atlas_code_15), getString(R.string.atlas_code_16)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(atlas_codes, (dialogInterface, i) -> textViewAtlasCode.setText(atlas_codes[i]));
        builder.show();
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
        Intent intent = new Intent(this, MapActivity.class);
        // If location is not loaded put some location on the map at the Balkan
        Bundle bundle = new Bundle();
        if (currentLocation.latitude == 0) {
            String database = SettingsManager.getDatabaseName();
            if (database.equals("https://biologer.org")) {
                currentLocation = new LatLng(44.0, 20.8);
            }
            if (database.equals("https://dev.biologer.org")) {
                currentLocation = new LatLng(44.0, 20.8);
            }
            if (database.equals("https://biologer.hr")) {
                currentLocation = new LatLng(45.5, 16.3);
            }
            if (database.equals("https://biologer.ba")) {
                currentLocation = new LatLng(44.3, 17.9);
            }
        }
        intent.putExtra("latlong", currentLocation);
        bundle.putDouble("accuracy", acc);
        intent.putExtras(bundle);
        startActivityForResult(intent, MAP);
    }

    public void takePhotoFromGallery() {
        Log.i(TAG, "Taking photo from the Gallery.");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        int GALLERY = 1;
        if (requestCode == GALLERY) {
            Log.d(TAG, "GALLERY requestCode sent");
            if (data != null) {
                // If a single image is selected we cen get it directly...
                if (data.getData() != null) {
                    Uri currentPhotoUri = data.getData();
                    Log.i(TAG, "You have selected this image from Gallery: " + currentPhotoUri);
                    resizeAndViewImage(currentPhotoUri);
                    // entryAddPic(resized_image); // Not sure if I need this...
                } else {
                    // If multiple images are selected we have a workaround...
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        int count_selected = clipData.getItemCount();
                        int count_free = countEmptyImageFrames();
                        Log.d(TAG, count_selected + " images selected from Gallery; " + count_free + " image placeholders are free...");

                        if (count_free < count_selected) {
                            Toast.makeText(this,
                                    getString(R.string.limit_photo1) + " " + count_selected + " " +
                                            getString(R.string.limit_photo2) + " " + count_free + " " +
                                            getString(R.string.limit_photo3), Toast.LENGTH_LONG).show();
                        }

                        for (int i = 0; i < Math.min(count_free, count_selected); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            resizeAndViewImage(uri);
                        }

                    } else {
                        Toast.makeText(this, getString(R.string.no_photo_selected),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

        } else if (requestCode == CAMERA) {
            Log.d(TAG, "CAMERA requestCode sent");
            if (data != null) {
                String image_uri_string = data.getStringExtra("image_string");
                entryAddPic(image_uri_string);
                if (image1 == null) {
                    image1 = image_uri_string;
                    Glide.with(this)
                            .load(image1)
                            .override(100, 100)
                            .into(imageViewPicture1);
                    list_new_images.add(image1);
                    frameLayoutPicture1.setVisibility(View.VISIBLE);
                } else if (image2 == null) {
                    image2 = image_uri_string;
                    Glide.with(this)
                            .load(image2)
                            .override(100, 100)
                            .into(imageViewPicture2);
                    list_new_images.add(image2);
                    frameLayoutPicture2.setVisibility(View.VISIBLE);
                } else if (image3 == null) {
                    image3 = image_uri_string;
                    Glide.with(this)
                            .load(image3)
                            .override(100, 100)
                            .into(imageViewPicture3);
                    list_new_images.add(image3);
                    frameLayoutPicture3.setVisibility(View.VISIBLE);
                    disablePhotoButtons(true);
                }
            }
            Toast.makeText(EntryActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }

        // Get data from Google MapActivity.java and save it as local variables
        if (requestCode == MAP) {
            Log.d(TAG, "MAP requestCode sent");
            locationManager.removeUpdates(locationListener);
            if (data != null) {
                currentLocation = data.getParcelableExtra("google_map_latlong");
                assert currentLocation != null;
                setLocationValues(currentLocation.latitude, currentLocation.longitude);
                acc = Double.parseDouble(Objects.requireNonNull(Objects.requireNonNull(data.getExtras()).getString("google_map_accuracy")));

                elev = Double.parseDouble(Objects.requireNonNull(data.getExtras().getString("google_map_elevation")));
            }
            assert data != null;
            if (Objects.equals(data.getExtras().getString("google_map_accuracy"), "0.0")) {
                textViewGPSAccuracy.setText(R.string.unknown);
                textViewMeters.setVisibility(View.GONE);
            } else {
                textViewGPSAccuracy.setText(String.format(Locale.ENGLISH, "%.0f", acc));
                textViewMeters.setVisibility(View.VISIBLE);
                setAccuracyColor();
            }
        }
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

    private int countEmptyImageFrames() {

        String[] images = getImagesArray();

        int count = 0;
        for (Object obj : images) {
            if ( obj == null ) count++;
        }

        return count;
    }

    private String[] getImagesArray() {
        String[] images = new String[3];
        images[0] = image1;
        images[1] = image2;
        images[2] = image3;

        return images;
    }

    private void entryAddPic(String string) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.parse(string);
        intent.setData(uri);
        this.sendBroadcast(intent);
    }

    public void showDeadComment() {
        if (checkBox_dead.isChecked()) {
            textViewDeathComment.setVisibility(View.VISIBLE);
        } else {
            textViewDeathComment.setVisibility(View.GONE);
            editTextDeathComment.setText("");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission to write external storage granted.");
                    getLocation(100, 2); // Ensure that EntryActivity ask for location permission.
                } else {
                    Log.d(TAG, "Not possible to get permission to write external storage.");
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission to take camera granted, taking photo now…");
                    takePhotoFromCamera();
                } else {
                    Log.d(TAG, "Not possible to get permission to use camera.");
                }
            }

            case REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation(100, 2);
                    Log.d(TAG, "Permission to request location granted.");
                } else {
                    Log.d(TAG, "Not possible to get permission to use camera.");
                }
            }
        }
    }

    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Log.d(TAG, "Could not show camera permission dialog.");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            takePhotoFromCamera();
        }
    }

    private void takePhotoFromCamera() {
        Intent camera = new Intent(this, CameraActivity.class);
        startActivityForResult(camera, CAMERA);
    }

    // Function used to retrieve the location
    private void getLocation(int time, int distance) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EntryActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, locationListener);
        }
    }

    private void setLocationValues(double latti, double longi) {
        latitude = String.format(Locale.ENGLISH, "%.4f", (latti));
        longitude = String.format(Locale.ENGLISH, "%.4f", (longi));
        textViewLatitude.setText(latitude);
        textViewLongitude.setText(longitude);
        layoutUnknownCoordinates.setVisibility(View.GONE);
        layoutCoordinates.setVisibility(View.VISIBLE);
    }

    // Show the message if the taxon is not chosen from the taxonomic list
    protected void buildAlertMessageInvalidTaxon() {
        final AlertDialog.Builder builder_taxon = new AlertDialog.Builder(EntryActivity.this);
        builder_taxon.setMessage(getString(R.string.invalid_taxon_name))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.save_anyway), (dialog, id) -> {
                    // Save custom taxon with no ID.
                    // Just send null ID and do the rest in entryChecker.
                    saveEntry2(null);
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
                    saveEntry3(getSelectedTaxon());
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

    @Override
    public void onBackPressed() {

        // If EntryActivity was canceled, delete unsaved images from internal memory.
        deleteUnsavedImages();

        Intent intent = new Intent(EntryActivity.this, LandingActivity.class);
        startActivity(intent);
        super.onBackPressed();
    }

    private void deleteUnsavedImages() {
        for( String image : list_new_images ) {
            if (image != null) {
                String filename = new File(image).getName();
                final File file = new File(getFilesDir(), filename);
                boolean b = file.delete();
                Log.d(TAG, "Deleting image " + image + " returned: " + b);
            }
        }
    }

    // Check for permissions and add them if required
    private void checkWriteStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No permission to write external storage");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d(TAG, "User does not allow to set write permission for this application.");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL);
                Log.d(TAG, "Requesting permission to write external storage.");
            }
        } else {
            Log.d(TAG, "Permission to write external storage was already granted");
        }
    }

    private int getGreenDaoDataLicense() {
        if (userDataList != null) {
            if (!userDataList.isEmpty()) {
                return userDataList.get(0).getData_license();
            }
            return 0;
        }
        return 0;
    }

    private int getGreenDaoImageLicense() {
        if (userDataList != null) {
            if (!userDataList.isEmpty()) {
                return userDataList.get(0).getImage_license();
            }
            return 0;
        }
        return 0;
    }

    private TaxonData getSelectedTaxon() {
        TaxonData taxon = App.get().getDaoSession().getTaxonDataDao().queryBuilder()
                .where(TaxonDataDao.Properties.LatinName.eq(getLatinName()))
                .limit(1)
                .unique();
        if (taxon != null) {
            Log.d(TAG, "Selected taxon latin name is: " + taxon.getLatinName() + ". Taxon ID: " + taxon.getId());
            return taxon;
        } else {
            return null;
        }
    }

    private String getLatinName() {
        String entered_taxon_name = acTextView.getText().toString();
        return entered_taxon_name.split(" \\(")[0];
    }

    // When screen is rotated activity is destroyed, thus images should be saved and opened again!
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("image1", image1);
        outState.putString("image2", image2);
        outState.putString("image3", image3);
        outState.putStringArrayList("list_new_images", list_new_images);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        Log.d(TAG, "Activity will be recreated. Saving the state!");
        super.onSaveInstanceState(outState);
    }

    private void fillObservationTypes() {

        long number_of_observation_types = App.get().getDaoSession().getObservationTypesDataDao().queryBuilder()
                .where(ObservationTypesDataDao.Properties.Locale.eq(locale_script))
                .count();

        Log.d(TAG, "Filling types of observations with " + locale_script + " script names. Total of " + number_of_observation_types + " entries.");

        for (int i = 0; i < number_of_observation_types; i++) {
            ObservationTypesData observation_type = App.get().getDaoSession().getObservationTypesDataDao().queryBuilder()
                    .where(ObservationTypesDataDao.Properties.Locale.eq(locale_script))
                    .list().get(i);
            String slug = observation_type.getSlug();
            int observation_id = observation_type.getObservationId().intValue();

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
                chip.setChecked(arrayContainsNumber(observation_type_ids, observation_id));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    String text = (String) compoundButton.getTag();
                    int id = compoundButton.getId();
                    if (compoundButton.isChecked()) {
                        Log.d(TAG, "Chip button \"" + text + "\" selected, ID: " + id);
                        observation_type_ids = insertIntoArray(observation_type_ids, id);
                    } else {
                        Log.d(TAG, "Chip button \"" + text + "\" deselected, ID: " + id);
                        observation_type_ids = removeFromArray(observation_type_ids, id);
                    }
                });
                observation_types.addView(chip);
            }

        }
    }

    public boolean arrayContainsNumber(final int[] array, final int key) {
        boolean value = ArrayUtils.contains(array, key);
        Log.d(TAG, "Array " + Arrays.toString(array) + " is compared against number " + key + " and returned " + value);
        return value;
    }

    private int[] insertIntoArray(int[] observation_type_ids, int new_id) {
        if (observation_type_ids == null) {
            int[] new_array = {new_id};
            Log.d(TAG, "The complete array of tag IDs looks like this: " + Arrays.toString(new_array));
            return new_array;
        } else {
            int len = observation_type_ids.length;
            int[] new_array = new int[len + 1];
            System.arraycopy(observation_type_ids, 0, new_array, 0, len);
            new_array[len] = new_id;
            Log.d(TAG, "The complete array of tag IDs looks like this: " + Arrays.toString(new_array));
            return new_array;
        }
    }

    private int[] removeFromArray(int[] observation_type_ids, int id) {
        int len = observation_type_ids.length;
        List<Integer> new_list = new ArrayList<>(len - 1);
        for (int observation_type_id : observation_type_ids) {
            if (observation_type_id != id) {
                new_list.add(observation_type_id);
            }
        }
        int[] new_array = new int[len - 1];
        for (int i = 0; i < len - 1; i++) {
            new_array[i] = new_list.get(i);
        }
        return new_array;
    }

    private int[] getArrayFromText(String string) {
        String[] strings = string.replace("[", "").replace("]", "").split(", ");
        int[] new_array = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            new_array[i] = Integer.parseInt(strings[i]);
        }
        return new_array;
    }

    private void resizeAndViewImage(Uri uri) {
        // Start another Activity to resize captured image.
        Intent resizeImage = new Intent(this, PreparePhotos.class);
        resizeImage.putExtra("image_uri", String.valueOf(uri));
        startService(resizeImage);
    }
}