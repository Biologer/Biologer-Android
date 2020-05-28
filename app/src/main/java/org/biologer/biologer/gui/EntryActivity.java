package org.biologer.biologer.gui;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.sql.ObservationTypesData;
import org.biologer.biologer.sql.ObservationTypesDataDao;
import org.biologer.biologer.sql.Stage;
import org.biologer.biologer.sql.StageDao;
import org.biologer.biologer.sql.TaxonData;
import org.biologer.biologer.sql.TaxonDataDao;
import org.biologer.biologer.sql.UserData;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EntryActivity extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "Biologer.Entry";

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 1005;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1006;
    private static final int REQUEST_LOCATION = 1;

    private Uri currentPhotoUri;
    private LocationManager locationManager;
    private LocationListener locationListener;
    String latitude = "0", longitude = "0";
    private double elev = 0.0;
    private LatLng currentLocation = new LatLng(0.0, 0.0);
    private Double acc = 0.0;
    private int CAMERA = 2, MAP = 3;
    int GALLERY = 1;
    private TextView textViewGPS, textViewStage, textViewLatitude, textViewLongitude, textViewSex, textViewAtlasCode;
    private EditText editTextDeathComment, editTextComment, editTextSpecimensNo, editTextMalesNo,
            editTextFemalesNo, editTextHabitat, editTextFoundOn;
    AutoCompleteTextView acTextView;
    FrameLayout frameLayoutPicture1, frameLayoutPicture2, frameLayoutPicture3;
    ImageView imageViewPicture1, imageViewPicture1Del, imageViewPicture2, imageViewPicture2Del,
            imageViewPicture3, imageViewPicture3Del, imageViewMap, imageViewCamera, imageViewGallery;
    private CheckBox check_dead;
    ChipGroup observation_types;
    LinearLayout detailedEntry;
    RelativeLayout numberMalesFemales;
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

        checkWriteStoragePermission();

        // Get the system locale to translate names of the taxa
        locale_script = Localisation.getLocaleScript();

        /*
         * Get the view...
         */
        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(this);
        textViewLatitude = findViewById(R.id.tv_latitude);
        textViewLongitude = findViewById(R.id.tv_longitude);
        textViewGPS = findViewById(R.id.tv_gps);
        textViewStage = findViewById(R.id.text_view_stages);
        textViewStage.setOnClickListener(this);
        editTextDeathComment = findViewById(R.id.editText_death_comment);
        editTextComment = findViewById(R.id.editText_comment);
        editTextSpecimensNo = findViewById(R.id.editText_number_of_specimens);
        editTextMalesNo = findViewById(R.id.editText_number_of_males);
        editTextFemalesNo = findViewById(R.id.editText_number_of_females);
        editTextHabitat = findViewById(R.id.editText_habitat);
        editTextFoundOn = findViewById(R.id.editText_found_on);
        // In order not to use spinner to choose sex, we will put this into EditText
        textViewSex = findViewById(R.id.text_view_sex);
        textViewSex.setOnClickListener(this);
        textViewAtlasCode = findViewById(R.id.text_view_atlas_code);
        textViewAtlasCode.setOnClickListener(this);
        check_dead = findViewById(R.id.dead_specimen);
        check_dead.setOnClickListener(this);
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
        numberMalesFemales = findViewById(R.id.both_sexes_no);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("advanced_interface", false)) {
            detailedEntry.setVisibility(View.VISIBLE);
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
                    QueryBuilder<TaxonData> query = App.get().getDaoSession().getTaxonDataDao().queryBuilder();
                    if (locale_script.equals("sr")) {
                        if (preferences.getBoolean("english_names", false)) {
                            query.where(
                                    query.or(
                                            query.and(
                                                    TaxonDataDao.Properties.LatinName.like("%" + typed_name + "%"),
                                                    query.or(
                                                            query.and(
                                                                    TaxonDataDao.Properties.NativeName.isNotNull(),
                                                                    TaxonDataDao.Properties.Locale.eq(locale_script)),
                                                            query.and(
                                                                    TaxonDataDao.Properties.NativeName.isNull(),
                                                                    TaxonDataDao.Properties.Locale.eq("en")))),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq(locale_script)),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq("sr-Latn")),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq("en"))));
                        } else {
                            query.where(
                                    query.or(
                                            query.and(
                                                    TaxonDataDao.Properties.LatinName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq("en")),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq(locale_script)),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq("sr-Latn"))));
                        }
                    } else {
                        if (preferences.getBoolean("english_names", false)) {
                            query.where(
                                    query.or(
                                            query.and(
                                                    TaxonDataDao.Properties.LatinName.like("%" + typed_name + "%"),
                                                    query.or(
                                                            query.and(
                                                                    TaxonDataDao.Properties.NativeName.isNotNull(),
                                                                    TaxonDataDao.Properties.Locale.eq(locale_script)),
                                                            query.and(
                                                                    TaxonDataDao.Properties.NativeName.isNull(),
                                                                    TaxonDataDao.Properties.Locale.eq("en")))),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq(locale_script)),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq("en"))));
                        } else {
                            query.where(
                                    query.or(
                                            query.and(
                                                    TaxonDataDao.Properties.LatinName.like("%" + typed_name + "%"),
                                                    query.or(
                                                            query.and(
                                                                    TaxonDataDao.Properties.NativeName.isNotNull(),
                                                                    TaxonDataDao.Properties.Locale.eq(locale_script)),
                                                            query.and(
                                                                    TaxonDataDao.Properties.NativeName.isNull(),
                                                                    TaxonDataDao.Properties.Locale.eq("en")))),
                                            query.and(TaxonDataDao.Properties.NativeName.like("%" + typed_name + "%"),
                                                    TaxonDataDao.Properties.Locale.eq(locale_script))));
                        }
                    }

                    query.limit(10);
                    List<TaxonData> taxaList = query.list();

                    String[] taxaNames = new String[taxaList.size()];
                    for (int i = 0; i < taxaList.size(); i++) {

                        String latin_name = taxaList.get(i).getLatinName();

                        // Query fot the native name (stupid solution, but what can we do...)
                        QueryBuilder<TaxonData> query1 = App.get().getDaoSession().getTaxonDataDao().queryBuilder();
                        query1.where(
                                query1.and(TaxonDataDao.Properties.LatinName.eq(latin_name),
                                        TaxonDataDao.Properties.Locale.eq(locale_script)));

                        List<TaxonData> currentTaxon = query1.list();

                        if (currentTaxon.isEmpty()) {
                            taxaNames[i] = latin_name;
                        } else {
                            String native_name = currentTaxon.get(0).getNativeName();
                            if (native_name == null) {
                                taxaNames[i] = latin_name;
                            } else {
                                taxaNames[i] = latin_name + " (" + native_name + ")";
                            }
                        }
                    }

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
                        }
                        Log.d(TAG, "Taxon is selected from the list. Enabling Stages for this taxon.");
                        if (taxonData.getUseAtlasCode()) {
                            textViewAtlasCode.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Taxon uses the atlas code. Enabling Atlas code for this taxon.");
                        }
                    } else {
                        stages.setVisibility(View.GONE);
                        textViewAtlasCode.setVisibility(View.GONE);
                        Log.d(TAG, "Taxon is not selected from the list. Disabling Stages and Atlas Codes for this taxon.");
                    }
                    // Enable/disable Save button in Toolbar
                    if (acTextView.getText().toString().length() > 1) {
                        save_enabled = true;
                        Log.d(TAG, "Taxon is set to: " + acTextView.getText());
                        invalidateOptionsMenu();
                    } else {
                        save_enabled = false;
                        Log.d(TAG, "Taxon entry field is empty.");
                        invalidateOptionsMenu();
                    }
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
            public void onLocationChanged(Location location) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                setLocationValues(location.getLatitude(), location.getLongitude());
                elev = location.getAltitude();
                acc = (double) location.getAccuracy();
                textViewGPS.setText(String.format(Locale.ENGLISH, "%.0f", acc));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
                //buildAlertMessageNoGps();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Finally to start the gathering of data...
        startEntryActivity();
        fillObservationTypes();
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
        textViewLatitude.setText(String.format(Locale.ENGLISH, "%.4f", currentItem.getLattitude()));
        textViewLongitude.setText(String.format(Locale.ENGLISH, "%.4f", currentItem.getLongitude()));
        textViewGPS.setText(String.format(Locale.ENGLISH, "%.0f", currentItem.getAccuracy()));

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
            textViewSex.setText(getString(R.string.is_male));
        }
        if (currentItem.getSex().equals("female")) {
            Log.d(TAG, "Setting spinner selected item to female.");
            textViewSex.setText(getString(R.string.is_female));
        }

        // Get the atlas code.
        if (currentItem.getAtlas_code() != null) {
            Long code = currentItem.getAtlas_code();
            Log.d(TAG, "Setting the spinner to atlas code: " + code);
            textViewAtlasCode.setText(setAtlasCode(Math.toIntExact(code)));
        }

        if (currentItem.getDeadOrAlive().equals("true")) {
            // Specimen is a live
            check_dead.setChecked(false);
        } else {
            // Specimen is dead, Checkbox should be activated and Dead Comment shown
            check_dead.setChecked(true);
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

        if (image1 == null || image2 == null || image3 == null) {
            disablePhotoButtons(false);
        } else {
            disablePhotoButtons(true);
        }
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
            editTextSpecimensNo.setText(String.valueOf(currentItem.getNoSpecimens()));
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
            this.finish();
            return true;
        }
        if (id == R.id.action_save) {
            saveEntry1();
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // On click
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.editText_number_of_specimens:
                Log.d(TAG, "Text for number of individuals changed.");
                if (getSex().equals("both")) {
                    numberMalesFemales.setVisibility(View.GONE);
                }
                break;
            case R.id.editText_number_of_males:
                Log.d(TAG, "Text for number of males changed.");
                editTextSpecimensNo.setVisibility(View.GONE);
                break;
            case R.id.editText_number_of_females:
                Log.d(TAG, "Text for number of females changed.");
                editTextSpecimensNo.setVisibility(View.GONE);
                break;
            case R.id.text_view_atlas_code:
                getAtlasCodeForList();
                break;
            case R.id.text_view_stages:
                getStageForTaxon();
                break;
            case R.id.text_view_sex:
                getSexForList();
                break;
            case R.id.ib_pic1_del:
                Log.i(TAG, "Deleting image 1.");
                frameLayoutPicture1.setVisibility(View.GONE);
                disablePhotoButtons(false);
                image1 = null;
                break;
            case R.id.ib_pic1:
                Log.i(TAG, "Image 1 clicked. URL: " + image1);
                openInGallery(image1);
                break;
            case R.id.ib_pic2_del:
                frameLayoutPicture2.setVisibility(View.GONE);
                disablePhotoButtons(false);
                image2 = null;
                break;
            case R.id.ib_pic2:
                Log.i(TAG, "Image 2 clicked. URL: " + image2);
                openInGallery(image2);
                break;
            case R.id.ib_pic3_del:
                frameLayoutPicture3.setVisibility(View.GONE);
                disablePhotoButtons(false);
                image3 = null;
                break;
            case R.id.ib_pic3:
                Log.i(TAG, "Image 3 clicked. URL: " + image3);
                openInGallery(image3);
                break;
            case R.id.dead_specimen:
                showDeadComment();
                break;
            case R.id.iv_map:
                showMap();
                break;
            case R.id.image_view_take_photo_camera:
                takePhotoFromCamera();
                break;
            case R.id.image_view_take_photo_gallery:
                takePhotoFromGallery();
                break;
        }
    }

    private void openInGallery(String image) {
        Uri uri = Uri.parse(image);

        // Try to open image just to see if it still exist on the storage.
        ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");

            // If the image is there open it!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivity(intent);
            } else {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                startActivity(intent);
            }

            assert parcelFileDescriptor != null;
            parcelFileDescriptor.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
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
            Log.d(TAG, "The taxon with ID " + taxon.getTaxonId() + " selected. Checking coordinates and saving the entry!");
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
                TaxonData taxon_noId = new TaxonData(null, null, acTextView.getText().toString(), false, null, null, null);
                saveEntry3(taxon_noId);
            } else {
                Log.d(TAG, "Saving taxon with known ID: " + taxon.getTaxonId());
                saveEntry3(taxon);
            }
        }
    }

    /*
    /  PART 3: Check if both male and female entries should be created
    */
    private void saveEntry3(TaxonData taxon) {
        String all_specimens_number = editTextSpecimensNo.getText().toString();
        String males_number = editTextMalesNo.getText().toString();
        String females_number = editTextFemalesNo.getText().toString();
        if (getSex().equals("male")) {
            Log.d(TAG, "Only male individuals selected.");
            entrySaver(taxon, all_specimens_number, "male", 1);
        }
        if (getSex().equals("female")) {
            Log.d(TAG, "Only female individuals selected.");
            entrySaver(taxon, all_specimens_number, "female", 1);
        }
        if (getSex().equals("")) {
            Log.d(TAG, "No sex of individuals selected.");
            entrySaver(taxon, all_specimens_number, "", 1);
        }
        if (getSex().equals("both")) {
            Log.d(TAG, "Both male and female individuals selected.");
            if (!males_number.equals("") && !females_number.equals("")) {
                Log.d(TAG, "Creating two entries, since both males and females are selected.");
                entrySaver(taxon, males_number, "male", 1);
                entrySaver(taxon, females_number, "female", 2);
            }
            if (!males_number.equals("") && females_number.equals("")) {
                Log.d(TAG, "Creating one entry, since only " + males_number + " males selected.");
                entrySaver(taxon, males_number, "male", 1);
            }
            if (males_number.equals("") && !females_number.equals("")) {
                Log.d(TAG, "Creating one entry, since only " + females_number + " females selected.");
                entrySaver(taxon, females_number, "female", 1);
            }
            if (males_number.equals("") && females_number.equals("")) {
                Log.d(TAG, "No males and females number selected, saving a single entry.");
                entrySaver(taxon, all_specimens_number, "", 1);
            }
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
            Long taxon_id = taxon.getTaxonId();
            String taxon_name = taxon.getLatinName();
            String project_name = PreferenceManager.getDefaultSharedPreferences(this).getString("project_name", "0");
            getPhotoTag();
            observation_type_ids_string = Arrays.toString(observation_type_ids);
            Log.d(TAG, "Converting array of observation type IDs into string: " + observation_type_ids_string);

            // Get the data structure and save it into a database Entry
            Entry entry1 = new Entry(null, taxon_id, taxon_name, year, month, day, comment, numberOfSpecimens, sex, selectedStage, getAtlasCode(),
                    String.valueOf(!check_dead.isChecked()), deathComment, currentLocation.latitude, currentLocation.longitude, acc,
                    elev, "", image1, image2, image3, project_name, foundOn, String.valueOf(getGreenDaoDataLicense()),
                    getGreenDaoImageLicense(), time, habitat, observation_type_ids_string);
            App.get().getDaoSession().getEntryDao().insertOrReplace(entry1);
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else { // if the entry exist already
            currentItem.setTaxonId(taxon.getTaxonId());
            currentItem.setTaxonSuggestion(taxon.getLatinName());
            currentItem.setComment(comment);
            currentItem.setNoSpecimens(numberOfSpecimens);
            currentItem.setSex(sex);
            currentItem.setStage(selectedStage);
            currentItem.setAtlasCode(getAtlasCode());
            currentItem.setDeadOrAlive(String.valueOf(!check_dead.isChecked()));
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
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK);
            finish();
        }
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

    private String getSex() {
        String[] sex = {getString(R.string.unknown_sex_short), getString(R.string.is_male), getString(R.string.is_female), getString(R.string.both)};
        String sex_is = textViewSex.getText().toString();
        int sex_id = Arrays.asList(sex).indexOf(sex_is);
        if (sex_id == 1) {
            Log.d(TAG, "Sex from spinner index 1 selected with value " + sex_is);
            return "male";
        }
        if (sex_id == 2) {
            Log.d(TAG, "Sex from spinner index 2 selected with value " + sex_is);
            return "female";
        }
        if (sex_id == 3) {
            Log.d(TAG, "Sex from spinner index 3 selected with value " + sex_is);
            return "both";
        } else {
            return "";
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
                .where(StageDao.Properties.TaxonId.eq(taxonData.getTaxonId()))
                .list();
        return stageList.size() != 0;
    }

    private void getStageForTaxon() {
        TaxonData taxon = App.get().getDaoSession().getTaxonDataDao().queryBuilder()
                .where(TaxonDataDao.Properties.LatinName.eq(getLatinName()))
                .limit(1)
                .unique();
        stageList = App.get().getDaoSession().getStageDao().queryBuilder()
                .where(StageDao.Properties.TaxonId.eq(taxon.getTaxonId()))
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

    private void getSexForList() {
        final String[] sex = {getString(R.string.unknown_sex), getString(R.string.is_male), getString(R.string.is_female), getString(R.string.both_sexes)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(sex, (dialogInterface, i) -> {
            if (sex[i].equals(getString(R.string.unknown_sex))) {
                textViewSex.setText(getString(R.string.unknown_sex_short));
                Log.d(TAG, "No sex is selected.");
                numberMalesFemales.setVisibility(View.GONE);
                editTextSpecimensNo.setVisibility(View.VISIBLE);
            }
            if (sex[i].equals(getString(R.string.both_sexes))) {
                textViewSex.setText(getString(R.string.both));
                Log.d(TAG, "Selected sex for this entry is " + sex[i] + ".");
                numberMalesFemales.setVisibility(View.VISIBLE);
            } else {
                textViewSex.setText(sex[i]);
                Log.d(TAG, "Selected sex for this entry is " + sex[i] + ".");
                numberMalesFemales.setVisibility(View.GONE);
                editTextSpecimensNo.setVisibility(View.VISIBLE);
            }
        });
        builder.show();
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

    // Check for camera permission and run function takePhoto()
    private void takePhotoFromCamera() {
        Log.i(TAG, "Taking photo from Camera.");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Log.d(TAG, "Could not show camera permission dialog.");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            takePhoto();
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.i(TAG, "There is Camera software installed. All ready to take picture!");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            Uri photoUri = getPhotoUri(getImageFileName());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(takePictureIntent, CAMERA);
            // Update global variable to this URI
            currentPhotoUri = photoUri;
            Log.d(TAG, "The photo is: " + currentPhotoUri);
        } else {
            Log.d(TAG, "Take picture intent could not start for some reason.");
        }
    }

    private Bitmap resizeImage(Uri imageUri) {

        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(imageUri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap input_image = null;

        if (parcelFileDescriptor != null) {
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

            // Memory leak workaround = don’t load whole image for resizing, but use inSampleSize.
            int inSampleSize;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // just to get image dimensions, don’t load into memory
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            inSampleSize = getInSampleSize(options, 1024);

            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false; // now load the image into memory
            input_image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

            //  Finally close the FileDescriptor
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (input_image == null) {
            Log.e(TAG, "It looks like input image does not exist!");
            return null;
        }
        if (Math.max(input_image.getHeight(), input_image.getWidth()) == 1024) {
            Log.d(TAG, "The image fits perfectly! Returning image without resize");
            return input_image;
        }
        else {
            Log.d(TAG, "Resizing image prior to upload...");
            return resizeBitmap(input_image, 1024);
        }
    }

    private static int getInSampleSize(BitmapFactory.Options options, int max_dimensions) {
        int inSampleSize = 1;
        int larger_side = Math.max(options.outHeight, options.outWidth);

        if (larger_side > max_dimensions) {

            final int halfSide = larger_side / 2;

            while ((halfSide / inSampleSize) >= max_dimensions) {
                inSampleSize *= 2;
            }

        }
        Log.d(TAG, "Original image dimensions: " + options.outHeight + "×" + options.outWidth + " px. Resize factor value: " + inSampleSize);
        return inSampleSize;
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Log.i(TAG, "Resizing image of " + height + "×" + width + "px to a maximum of " + maxSize + "px.");

        if (height == width) {
            height = maxSize;
            width = maxSize;
        } if (height < width) {
            height = height * maxSize / width;
            width = maxSize;
        } else {
            width = width * maxSize /height;
            height = maxSize;
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    private Uri saveImage(Bitmap bitmap, @NonNull String fileName) {
        Uri photoUri;
        OutputStream fos = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String picturesDir = Environment.DIRECTORY_PICTURES + "/" + "Biologer";
            Log.i(TAG, "Saving image: " + fileName + " into a directory " + picturesDir);
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, picturesDir);
            photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                fos = resolver.openOutputStream(Objects.requireNonNull(photoUri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(fileName);
            } catch (IOException ex) {
                Log.e(TAG, "Could not create image file.");
            }
            try {
                fos = new FileOutputStream(Objects.requireNonNull(photoFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoUri = FileProvider.getUriForFile(this, "org.biologer.biologer.files", photoFile);
            } else {
                photoUri = Uri.fromFile(photoFile);
            }
            Log.i(TAG, "Saving image into: " + photoUri);

        }

        if (fos != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 87, fos);
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return photoUri;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        if (requestCode == GALLERY) {
            if (data != null) {
                // If a single image is selected we cen get it directly...
                if (data.getData() != null) {
                    currentPhotoUri = data.getData();
                    Log.i(TAG, "You have selected this image from Gallery: " + currentPhotoUri);
                    entryAddPic();
                    if (image1 == null) {
                        Bitmap bitmap = resizeImage(currentPhotoUri);
                        Uri uri1 = saveImage(bitmap, "resized1");
                        image1 = String.valueOf(uri1);
                        Glide.with(this)
                                .load(image1)
                                .override(100, 100)
                                .into(imageViewPicture1);
                        frameLayoutPicture1.setVisibility(View.VISIBLE);
                    } else if (image2 == null) {
                        Bitmap bitmap = resizeImage(currentPhotoUri);
                        Uri uri2 = saveImage(bitmap, "resized2");
                        image2 = String.valueOf(uri2);
                        Glide.with(this)
                                .load(image2)
                                .override(100, 100)
                                .into(imageViewPicture2);
                        frameLayoutPicture2.setVisibility(View.VISIBLE);
                    } else if (image3 == null) {
                        Bitmap bitmap = resizeImage(currentPhotoUri);
                        Uri uri3 = saveImage(bitmap, "resized3");
                        image3 = String.valueOf(uri3);
                        Glide.with(this)
                                .load(image3)
                                .override(100, 100)
                                .into(imageViewPicture3);
                        frameLayoutPicture3.setVisibility(View.VISIBLE);
                        imageViewGallery.setEnabled(false);
                        imageViewGallery.setImageAlpha(20);
                        imageViewCamera.setEnabled(false);
                        imageViewCamera.setImageAlpha(20);
                    }
                } else {
                    // If multiple images are selected we have a workaround...
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        ArrayList<Uri> arrayUri = new ArrayList<>();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            arrayUri.add(uri);
                        }

                        String[] images = new String[3];
                        images[0] = image1;
                        images[1] = image2;
                        images[2] = image3;

                        int j = 0;
                        for (int i = 0; i < 3; i++) {
                            if (images[i] == null) {
                                Log.i(TAG, "Image " + i + 1 + " is null.");
                                if (j < arrayUri.size()) {
                                    String uri = arrayUri.get(j).toString();
                                    j++;
                                    images[i] = uri;
                                }
                            } else {
                                Log.i(TAG, "Image " + i + 1 + " is already set to: " + images[i]);
                            }
                        }

                        // Send a message to the user if more than 3 photos are selected...
                        Log.i(TAG, "You have selected " + arrayUri.size() + " images; " + j + " image place holder exist in the Entry.");
                        if (j < arrayUri.size()) {
                            Toast.makeText(this,
                                    getString(R.string.limit_photo1) + " " + arrayUri.size() + " " +
                                            getString(R.string.limit_photo2) + " " + j + " " +
                                            getString(R.string.limit_photo3), Toast.LENGTH_LONG).show();
                        }

                        image1 = images[0];
                        image2 = images[1];
                        image3 = images[2];

                        if (image1 != null) {
                            Bitmap bitmap = resizeImage(Uri.parse(image1));
                            Uri uri1 = saveImage(bitmap, "resized1");
                            image1 = String.valueOf(uri1);
                            Glide.with(this)
                                    .load(image1)
                                    .override(100, 100)
                                    .into(imageViewPicture1);
                            frameLayoutPicture1.setVisibility(View.VISIBLE);
                        }
                        if (image2 != null) {
                            Bitmap bitmap = resizeImage(Uri.parse(image2));
                            Uri uri2 = saveImage(bitmap, "resized2");
                            image2 = String.valueOf(uri2);
                            Glide.with(this)
                                    .load(image2)
                                    .override(100, 100)
                                    .into(imageViewPicture2);
                            frameLayoutPicture2.setVisibility(View.VISIBLE);
                        }
                        if (image3 != null) {
                            Bitmap bitmap = resizeImage(Uri.parse(image3));
                            Uri uri3 = saveImage(bitmap, "resized3");
                            image3 = String.valueOf(uri3);
                            Glide.with(this)
                                    .load(image3)
                                    .override(100, 100)
                                    .into(imageViewPicture3);
                            frameLayoutPicture3.setVisibility(View.VISIBLE);
                        }
                        if (image1 != null && image2 != null && image3 != null) {
                            imageViewGallery.setEnabled(false);
                            imageViewGallery.setImageAlpha(20);
                            imageViewCamera.setEnabled(false);
                            imageViewCamera.setImageAlpha(20);
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.no_photo_selected),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

        } else if (requestCode == CAMERA) {
            entryAddPic();
            if (image1 == null) {
                Bitmap bitmap = resizeImage(currentPhotoUri);
                Uri uri1 = saveImage(bitmap, "resized1");
                image1 = String.valueOf(uri1);
                Glide.with(this)
                        .load(image1)
                        .override(100, 100)
                        .into(imageViewPicture1);
                frameLayoutPicture1.setVisibility(View.VISIBLE);
            } else if (image2 == null) {
                Bitmap bitmap = resizeImage(currentPhotoUri);
                Uri uri2 = saveImage(bitmap, "resized2");
                image2 = String.valueOf(uri2);
                Glide.with(this)
                        .load(image2)
                        .override(100, 100)
                        .into(imageViewPicture2);
                frameLayoutPicture2.setVisibility(View.VISIBLE);
            } else if (image3 == null) {
                Bitmap bitmap = resizeImage(currentPhotoUri);
                Uri uri3 = saveImage(bitmap, "resized3");
                image3 = String.valueOf(uri3);
                Glide.with(this)
                        .load(image3)
                        .override(100, 100)
                        .into(imageViewPicture3);
                frameLayoutPicture3.setVisibility(View.VISIBLE);
                disablePhotoButtons(true);
            }
            Toast.makeText(EntryActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }

        // Get data from Google MapActivity.java and save it as local variables
        if (requestCode == MAP) {
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
                textViewGPS.setText(R.string.not_available);
            } else {
                textViewGPS.setText(String.format(Locale.ENGLISH, "%.0f", acc));
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

    private Uri getPhotoUri(String fileName) {
        Uri photoUri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String picturesDir = Environment.DIRECTORY_PICTURES + "/" + "Biologer";
            Log.i(TAG, "Saving image: " + fileName + " into a directory " + picturesDir);
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, picturesDir);
            currentPhotoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            photoUri = currentPhotoUri;
            Log.i(TAG, "Image URI is: " + photoUri + ".");
        } else {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(fileName);
            } catch (IOException ex) {
                Log.e(TAG, "Could not create image file.");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    photoUri = FileProvider.getUriForFile(this, "org.biologer.biologer.files", photoFile);
                } else {
                    photoUri = Uri.fromFile(photoFile);
                }
            }
            Log.i(TAG, "Saving image into: " + photoUri);
        }
        return photoUri;
    }

    // This will create image on Android <= 9.0
    private File createImageFile(String fileName) throws IOException {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Biologer");

        if (!mediaStorageDir.exists()) {
            Log.d(TAG, "Media Storage directory does not exist");
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Media Storage directory should be created now...");
                return null;
            }
        }

        return File.createTempFile(fileName, ".jpg", mediaStorageDir);
    }

    // Set the filename for image taken through the Camera
    private String getImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "JPEG_" + timeStamp;
    }

    private void entryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(currentPhotoUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void showDeadComment() {
        if (check_dead.isChecked()) {
            editTextDeathComment.setVisibility(View.VISIBLE);
        } else {
            editTextDeathComment.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Not possible to get permission to write external storage.");
                } else {
                    finish();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    Log.d(TAG, "Not possible to get permission to use camera.");
                }
            }
        }
    }

    // Function used to retrieve the location
    private void getLocation(int time, int distance) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(EntryActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            // Sometimes there is a problem with first run of the program. So, request location again in 10 seconds...
            new Handler().postDelayed(() -> getLocation(100, 2), 10000);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, locationListener);
        }
    }

    private void setLocationValues(double latti, double longi) {
        latitude = String.format(Locale.ENGLISH, "%.4f", (latti));
        longitude = String.format(Locale.ENGLISH, "%.4f", (longi));
        textViewLatitude.setText(latitude);
        textViewLongitude.setText(longitude);
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
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> finish());
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
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> finish());
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
        Intent intent = new Intent(EntryActivity.this, LandingActivity.class);
        startActivity(intent);
        super.onBackPressed();
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
                if (arrayContainsNumber(observation_type_ids, observation_id)) {
                    chip.setChecked(true);
                } else {
                    chip.setChecked(false);
                }
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
}