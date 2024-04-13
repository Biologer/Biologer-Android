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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.App;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.ArrayHelper;
import org.biologer.biologer.adapters.FileManipulation;
import org.biologer.biologer.adapters.PreparePhotos;
import org.biologer.biologer.adapters.StageAndSexLocalization;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.ObservationTypesDb;
import org.biologer.biologer.sql.ObservationTypesDb_;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxaTranslationDb_;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonDb_;
import org.biologer.biologer.sql.UserDb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;

public class EntryActivity extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "Biologer.Entry";

    private LocationManager locationManager;
    private LocationListenerCompat locationListener;
    String latitude = "0", longitude = "0";
    private double elev = 0.0;
    private LatLng currentLocation = new LatLng(0.0, 0.0);
    private Double acc = 0.0;
    private TextInputLayout textViewAtlasCodeLayout, textViewSpecimensNo1, textViewSpecimensNo2, textViewDeathComment, textInputStages;
    private TextView textViewGPSAccuracy, textViewStage, textViewLatitude, textViewLongitude, textViewAtlasCode, textViewMeters;
    private EditText editTextDeathComment, editTextComment, editTextSpecimensNo1, editTextSpecimensNo2, editTextHabitat, editTextFoundOn;
    private MaterialCheckBox checkBox_males, checkBox_females, checkBox_dead;
    AutoCompleteTextView autoCompleteTextView_speciesName;
    FrameLayout frameLayoutPicture1, frameLayoutPicture2, frameLayoutPicture3;
    ImageView imageViewPicture1, imageViewPicture1Del, imageViewPicture2, imageViewPicture2Del,
            imageViewPicture3, imageViewPicture3Del, imageViewMap, imageViewCamera, imageViewGallery;
    ChipGroup observation_types;
    LinearLayout detailedEntry, layoutCoordinates, layoutUnknownCoordinates;
    private boolean save_enabled = false;
    private String image1, image2, image3;
    private Uri current_image;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<EntryDb> currentItem;
    private String locale_script = "en";
    Calendar calendar;
    SimpleDateFormat simpleDateFormat;
    // Get the data from the GreenDao database
    List<UserDb> userDataList = App.get().getBoxStore().boxFor(UserDb.class).getAll();
    String observation_type_ids_string;
    int[] observation_type_ids = null;
    ArrayList<String> list_new_images = new ArrayList<>();
    TaxonDb selectedTaxon = null;
    boolean locationFromTheMap = false;
    boolean taxonSelectedFromTheList = false;
    boolean callTagAutoChecked = false;
    Integer callTagIndexNumber = null;

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
        // This linear layout holds the stages. We will hide it before the taxon is not selected.
        textInputStages = findViewById(R.id.text_input_stages);
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
            list_new_images = savedInstanceState.getStringArrayList("list_new_images");
        }

        // Fill in the drop down menu with list of taxa
        TaxaListAdapter adapter = new TaxaListAdapter(this, R.layout.taxa_dropdown_list, new ArrayList<>());
        autoCompleteTextView_speciesName = findViewById(R.id.textview_list_of_taxa);
        autoCompleteTextView_speciesName.setAdapter(adapter);
        autoCompleteTextView_speciesName.setThreshold(2);

        autoCompleteTextView_speciesName.setOnItemClickListener((parent, view, position, id) -> {
            TaxonDb taxonDb = (TaxonDb) parent.getItemAtPosition(position);
            autoCompleteTextView_speciesName.setText(taxonDb.getLatinName());
            selectedTaxon = taxonDb;
            taxonSelectedFromTheList = true;
            showStagesAndAtlasCode(preferences);
        });

        // When user type taxon name...
        autoCompleteTextView_speciesName.addTextChangedListener(new TextWatcher() {
            final Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(runnable);

                final String typed_name = String.valueOf(s);

                runnable = () -> {
                    /*
                    Get the list of taxa from the GreenDao database
                    */

                    // Remove atlas code and stage for taxa not selected from the list
                    if (!taxonSelectedFromTheList) {
                        hideStagesAndAtlasCode();
                        selectedTaxon = null;
                    }
                    taxonSelectedFromTheList = false;

                    // This will bee the list of all IDs from all the queries
                    List<TaxonDb> allTaxaLists = new ArrayList<>();

                    // Query latin names
                    Box<TaxonDb> taxonDataBox = App.get().getBoxStore().boxFor(TaxonDb.class);
                    Query<TaxonDb> query_latin_name = taxonDataBox
                            .query(TaxonDb_.latinName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                            .build();
                    List<TaxonDb> latinNames = query_latin_name.find(0, 10);
                    query_latin_name.close();

                    for (int i = 0; i < latinNames.size(); i++) {
                        Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
                        Query<TaxaTranslationDb> query_taxon_translation = taxaTranslationDataBox
                                .query(TaxaTranslationDb_.taxonId.equal(latinNames.get(i).getId())
                                        .and(TaxaTranslationDb_.locale.equal(locale_script)))
                                .build();
                        List<TaxaTranslationDb> nativeNames = query_taxon_translation.find();
                        query_taxon_translation.close();

                        if (nativeNames.size() >= 1) {
                            String native_name = nativeNames.get(0).getNativeName();
                            if (native_name != null) {
                                TaxonDb taxon = latinNames.get(i);
                                taxon.setLatinName(taxon.getLatinName() + " (" + native_name + ")");
                                allTaxaLists.add(taxon);
                            }
                            else {
                                allTaxaLists.add(latinNames.get(i));

                            }
                        } else {
                            allTaxaLists.add(latinNames.get(i));
                        }
                    }

                    Box<TaxaTranslationDb> taxaTranslationDataBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
                    List<TaxaTranslationDb> nativeList;
                    // For Serbian language we should also search for Latin and Cyrillic names
                    if (locale_script.equals("sr")) {
                        if (preferences.getBoolean("english_names", false)) {
                            Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                                    .query(TaxaTranslationDb_.locale.equal("en")
                                            .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                            .or(TaxaTranslationDb_.locale.equal("sr")
                                                    .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                                            .or(TaxaTranslationDb_.locale.equal("sr-Latn")
                                                    .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                                    .build();
                            nativeList = nativeQuery.find(0, 10);
                            nativeQuery.close();
                        } else {
                            Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                                    .query(TaxaTranslationDb_.locale.equal("sr")
                                                    .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                            .or(TaxaTranslationDb_.locale.equal("sr-Latn")
                                                    .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                                    .build();
                            nativeList = nativeQuery.find(0, 10);
                            nativeQuery.close();
                        }
                    }

                    // For other languages it is more simple...
                    else {
                        if (preferences.getBoolean("english_names", false)) {
                            Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                                    .query(TaxaTranslationDb_.locale.equal("en")
                                            .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))
                                            .or(TaxaTranslationDb_.locale.equal(locale_script)
                                                    .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE))))
                                    .build();
                            nativeList = nativeQuery.find(0, 10);
                            nativeQuery.close();
                        } else {
                            Query<TaxaTranslationDb> nativeQuery = taxaTranslationDataBox
                                    .query(TaxaTranslationDb_.locale.equal(locale_script)
                                            .and(TaxaTranslationDb_.nativeName.contains(typed_name, QueryBuilder.StringOrder.CASE_INSENSITIVE)))
                                    .build();
                            nativeList = nativeQuery.find(0, 10);
                            nativeQuery.close();
                        }
                    }

                    for (int i = 0; i < nativeList.size(); i++) {
                        TaxaTranslationDb taxaTranslationData = nativeList.get(i);
                        // Don’t add taxa if already on the list
                        boolean duplicated = false;
                        for (int j = 0; j < allTaxaLists.size(); j++) {
                            if (allTaxaLists.get(j).getId() == taxaTranslationData.getTaxonId()) {
                                duplicated = true;
                            }
                        }
                        if (!duplicated) {
                            Box<TaxonDb> taxonDbBox = App.get().getBoxStore().boxFor(TaxonDb.class);
                            Query<TaxonDb> taxonDbQuery = taxonDbBox
                                    .query(TaxonDb_.id.equal(taxaTranslationData.getTaxonId()))
                                    .build();
                            TaxonDb taxon = taxonDbQuery.findFirst();
                            taxonDbQuery.close();

                            if (taxon != null) {
                                taxon.setLatinName(taxon.getLatinName() + " (" + taxaTranslationData.getNativeName() + ")");
                                allTaxaLists.add(taxon);
                            }
                        }
                    }

                    // Add the Query to the drop down list (adapter)
                    TaxaListAdapter adapter1 =
                            new TaxaListAdapter(EntryActivity.this, R.layout.taxa_dropdown_list, allTaxaLists);
                    autoCompleteTextView_speciesName.setAdapter(adapter1);
                    adapter1.notifyDataSetChanged();

                    // Enable/disable Save button in the Toolbar
                    if (autoCompleteTextView_speciesName.getText().toString().length() > 1) {
                        save_enabled = true;
                        Log.d(TAG, "Taxon is set to: " + autoCompleteTextView_speciesName.getText());
                    } else {
                        save_enabled = false;
                        Log.d(TAG, "Taxon entry field is empty.");
                    }
                    invalidateOptionsMenu();
                };
                handler.postDelayed(runnable, 300);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Activate the field for species name and show the keyboard.
        autoCompleteTextView_speciesName.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Define locationListener and locationManager in order to
        // to receive the Location.
        locationListener = new LocationListenerCompat() {
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
            public void onProviderEnabled(@NonNull String s) {
                Log.i(TAG, "Location provider is enabled.");
            }

            @Override
            public void onProviderDisabled(@NonNull String s) {
                Log.i(TAG, "Location provider is disabled.");
                if (!isFinishing()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(EntryActivity.this);
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

        // Broadcaster used for receiving resized images
        registerBroadcastReceiver();

        // Finally to start the gathering of data...
        startEntryActivity();
        fillObservationTypes();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "Back button is pressed!");
                backPressed();
            }
        });

    }

    private void showStagesAndAtlasCode(SharedPreferences preferences) {
        // Enable stage entry
        // Check if the taxon has stages. If not hide the stages dialog.
        String stages = selectedTaxon.getStages();
        Log.i(TAG, "This are the stages for " + selectedTaxon.getLatinName() + ": " + stages);

        if (stages != null) {
            Log.d(TAG, "Enabling Stages for this taxon.");
            textInputStages.setVisibility(View.VISIBLE);

            String[] all_stages = stages.split(";");

            // If user preferences are selected, the stage for taxa will be set to adult by default.
            // Step 1: Get the preferences
            if (preferences.getBoolean("adult_by_default", false)) {
                // If stage is already selected ignore this...
                if (textViewStage.getText().toString().equals("")) {
                    Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
                    Query<StageDb> query = stageBox
                            .query(StageDb_.name.equal("adult"))
                            .build();
                    StageDb stage = query.findFirst();
                    query.close();
                    if (stage != null) {
                        String s = String.valueOf(stage.getId());
                        if (Arrays.asList(all_stages).contains(s)) {
                            textViewStage.setText(getString(R.string.stage_adult));
                            textViewStage.setTag(stage.getId());
                        }
                    }
                }
            }
        }
        if (selectedTaxon.isUseAtlasCode()) {
            Log.d(TAG, "Enabling Atlas code for this taxon.");
            textViewAtlasCodeLayout.setVisibility(View.VISIBLE);
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

    public static class TaxaListAdapter extends ArrayAdapter<TaxonDb> {

        public TaxaListAdapter(@NonNull Context context, int resource, @NonNull List<TaxonDb> taxaLists) {
            super(context, resource, taxaLists);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable @org.jetbrains.annotations.Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder rowViewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.taxa_dropdown_list, parent, false);
                rowViewHolder = new ViewHolder();
                rowViewHolder.taxonNames = convertView.findViewById(R.id.textView_taxon_text);
                convertView.setTag(rowViewHolder);
            } else {
                rowViewHolder = (ViewHolder) convertView.getTag();
            }

            String taxon = Objects.requireNonNull(getItem(position)).getLatinName();

            rowViewHolder.taxonNames.setText(taxon);

            return convertView;
        }

        private static class ViewHolder {
            TextView taxonNames;
        }
    }

    private void setAccuracyColor() {
        if (acc != null) {
            if (acc <= 25) {
                textViewMeters.setTextColor(ContextCompat.getColor(this, R.color.checkBox_text));
                textViewGPSAccuracy.setTextColor(ContextCompat.getColor(this, R.color.checkBox_text));
            } else {
                textViewMeters.setTextColor(ContextCompat.getColor(this, R.color.warningRed));
                textViewGPSAccuracy.setTextColor(ContextCompat.getColor(this, R.color.warningRed));
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
                        Toast.makeText(EntryActivity.this, getString(R.string.image_resize_error), Toast.LENGTH_LONG).show();
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
            Box<ObservationTypesDb> observationTypesDataBox = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
            Query<ObservationTypesDb> query = observationTypesDataBox
                    .query(ObservationTypesDb_.slug.equal("observed"))
                    .build();
            List<ObservationTypesDb> observationTypesData = query.find();
            query.close();

            if (!observationTypesData.isEmpty()) {
                int id_for_observed_tag = (int)observationTypesData.get(0).getObservationId();
                Log.d(TAG, "Observed tag has ID: " + id_for_observed_tag);
                observation_type_ids = ArrayHelper.insertIntoArray(observation_type_ids, id_for_observed_tag);
            }

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

        long existing_entry_id = getIntent().getLongExtra("ENTRY_ID", 0);
        Box<EntryDb> entry = App.get().getBoxStore().boxFor(EntryDb.class);
        Query<EntryDb> query = entry
                .query(EntryDb_.id.equal(existing_entry_id))
                .build();
        currentItem = query.find();
        query.close();
        Log.i(TAG, "Opening existing entry with ID: " + existing_entry_id + ".");

        // Check if the taxa is selected from the list = it has an ID.
        if (currentItem.get(0) != null) {
            if (currentItem.get(0).getTaxonId() != 0) {
                taxonSelectedFromTheList = true;
            }
        }

        // Get the latitude, longitude, coordinate precision and elevation...
        currentLocation = new LatLng(currentItem.get(0).getLattitude(), currentItem.get(0).getLongitude());
        elev = currentItem.get(0).getElevation();
        acc = currentItem.get(0).getAccuracy();
        textViewLatitude.setText(String.format(Locale.ENGLISH, "%.4f", currentItem.get(0).getLattitude()));
        textViewLongitude.setText(String.format(Locale.ENGLISH, "%.4f", currentItem.get(0).getLongitude()));
        layoutUnknownCoordinates.setVisibility(View.GONE);
        layoutCoordinates.setVisibility(View.VISIBLE);
        textViewGPSAccuracy.setText(String.format(Locale.ENGLISH, "%.0f", currentItem.get(0).getAccuracy()));
        textViewMeters.setVisibility(View.VISIBLE);
        setAccuracyColor();

        if (currentItem.get(0).getTaxonId() != 0) {
            Box<TaxonDb> taxonDataBox = App.get().getBoxStore().boxFor(TaxonDb.class);
            Query<TaxonDb> query1 = taxonDataBox
                    .query(TaxonDb_.id.equal(currentItem.get(0).getTaxonId()))
                    .build();
            TaxonDb taxon = query1.findFirst();
            query1.close();

            // Get the atlas code
            boolean use_atlas_code;
            if (taxon != null) {
                use_atlas_code = taxon.isUseAtlasCode();
                if (use_atlas_code) {
                    Log.d(TAG, "There is an atlas code ID: " + currentItem.get(0).getAtlasCode());
                    textViewAtlasCodeLayout.setVisibility(View.VISIBLE);
                }

                // Get the taxa translation
                Box<TaxaTranslationDb> taxaTranslationDbBox = App.get().getBoxStore().boxFor(TaxaTranslationDb.class);
                Query<TaxaTranslationDb> query2 = taxaTranslationDbBox
                        .query(TaxaTranslationDb_.taxonId.equal(currentItem.get(0).getTaxonId())
                                .and(TaxaTranslationDb_.locale.equal(locale_script)))
                        .build();
                TaxaTranslationDb translationDb = query2.findFirst();
                query2.close();
                if (translationDb != null) {
                    taxon.setLatinName(taxon.getLatinName() + "(" + translationDb.getNativeName() + ")");
                }
                selectedTaxon = taxon;
            }
        }

        // Get the name of the taxon for this entry
        autoCompleteTextView_speciesName.setText(currentItem.get(0).getTaxonSuggestion());
        autoCompleteTextView_speciesName.dismissDropDown();

        // Get the name of the stage for the entry from the database
        if (currentItem.get(0).getStage() != null) {
            Log.d(TAG, "There is a stage already selected for this entry!");
            Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
            Query<StageDb> query2 = stageBox
                    .query(StageDb_.id.equal(currentItem.get(0).getStage()))
                    .build();
            long stage_id = query2.find().get(0).getId();
            query2.close();

            String stageName = StageAndSexLocalization.getStageLocaleFromID(this, stage_id);
            StageDb stage = new StageDb(stage_id, stageName);
            textViewStage.setTag(stage.getId());
            textViewStage.setText(stageName);
            textInputStages.setVisibility(View.VISIBLE);
        } else {
            if (isStageAvailable(currentItem.get(0).getTaxonId())) {
                textInputStages.setVisibility(View.VISIBLE);
            } else {
                textInputStages.setVisibility(View.GONE);
            }
        }

        // Get the selected sex. If not selected set spinner to default...
        Log.d(TAG, "Sex of individual from previous entry is " + currentItem.get(0).getSex());
        if (currentItem.get(0).getSex().equals("male")) {
            Log.d(TAG, "Setting spinner selected item to male.");
            checkBox_males.setChecked(true);
        }
        if (currentItem.get(0).getSex().equals("female")) {
            Log.d(TAG, "Setting spinner selected item to female.");
            checkBox_females.setChecked(true);
        }

        // Get the atlas code.
        if (currentItem.get(0).getAtlasCode() != null) {
            Long code = currentItem.get(0).getAtlasCode();
            Log.d(TAG, "Setting the spinner to atlas code: " + code);
            textViewAtlasCode.setText(setAtlasCode(code.intValue()));
        }

        if (currentItem.get(0).getDeadOrAlive().equals("true")) {
            // Specimen is a live
            checkBox_dead.setChecked(false);
        } else {
            // Specimen is dead, Checkbox should be activated and Dead Comment shown
            checkBox_dead.setChecked(true);
            showDeadComment();
        }

        // Get the images
        image1 = currentItem.get(0).getSlika1();
        if (image1 != null) {
            Glide.with(this)
                    .load(image1)
                    .override(100, 100)
                    .into(imageViewPicture1);
            frameLayoutPicture1.setVisibility(View.VISIBLE);
        }
        image2 = currentItem.get(0).getSlika2();
        if (image2 != null) {
            Glide.with(this)
                    .load(image2)
                    .override(100, 100)
                    .into(imageViewPicture2);
            frameLayoutPicture2.setVisibility(View.VISIBLE);
        }
        image3 = currentItem.get(0).getSlika3();
        if (image3 != null) {
            Glide.with(this)
                    .load(image3)
                    .override(100, 100)
                    .into(imageViewPicture3);
            frameLayoutPicture3.setVisibility(View.VISIBLE);
        }

        disablePhotoButtons(image1 != null && image2 != null && image3 != null);
        if (currentItem.get(0).getHabitat() != null) {
            editTextHabitat.setText(currentItem.get(0).getHabitat());
        }
        if (currentItem.get(0).getFoundOn() != null) {
            editTextFoundOn.setText(currentItem.get(0).getFoundOn());
        }

        // Get other values
        if (currentItem.get(0).getCauseOfDeath().length() != 0) {
            editTextDeathComment.setText(currentItem.get(0).getCauseOfDeath());
        }
        if (currentItem.get(0).getComment().length() != 0) {
            editTextComment.setText(currentItem.get(0).getComment());
        }
        if (currentItem.get(0).getNoSpecimens() != null) {
            editTextSpecimensNo1.setText(String.valueOf(currentItem.get(0).getNoSpecimens()));
        }

        // Load observation types and delete tag for photographed.
        observation_type_ids_string = currentItem.get(0).getObservationTypeIds();
        Log.d(TAG, "Loading observation types with IDs " + observation_type_ids_string);
        observation_type_ids = ArrayHelper.getArrayFromText(observation_type_ids_string);
        if (image1 != null || image2 != null || image3 != null) {
            Log.d(TAG, "Removing image tag just in case images got deleted.");
            Box<ObservationTypesDb> observationTypesDataBox = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
            Query<ObservationTypesDb> query1 = observationTypesDataBox
                    .query(ObservationTypesDb_.slug.equal("photographed"))
                    .build();
            int id_photo_tag = (int)query1.find().get(0).getObservationId();
            query1.close();
            observation_type_ids = ArrayHelper.removeFromArray(observation_type_ids, id_photo_tag);
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
        MenuItem item = menu.findItem(R.id.action_save);
        if (save_enabled) {
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
        if (id == R.id.action_save) {
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
                pickMultipleImages.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
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
        if (selectedTaxon == null) {
            Log.d(TAG, "The taxon does not exist in GreenDao database. Asking user to save it as is.");
            buildAlertMessageInvalidTaxon();
        } else {
            TaxonDb taxon = selectedTaxon;
            Log.d(TAG, "The taxon with ID " + taxon.getId() + " selected. Checking coordinates and saving the entry!");
            saveEntry2(taxon);
        }
    }

    /*
    /  PART 2: Check if the coordinates are OK
    */
    private void saveEntry2(TaxonDb taxon) {
        // If the location is not loaded, warn the user and
        // don’t send crappy data into the online database!
        if (currentLocation.latitude == 0) {
            buildAlertMessageNoCoordinates();
        } else {
            // If the location is not precise ask the user to
            // wait. But, not if the location is taken from the map
            // or if the user opened existing entry.
            if (acc <= 25 || locationFromTheMap || !isNewEntry()) {
                if (taxon == null) {
                    Log.d(TAG, "Saving taxon with unknown ID as simple text.");
                    TaxonDb taxon_noId = new TaxonDb(0, 0, autoCompleteTextView_speciesName.getText().toString(),
                            null, 0, null, false, false, null, null, null);
                    saveEntry3(taxon_noId);
                } else {
                    Log.d(TAG, "Saving taxon with known ID: " + taxon.getId());
                    saveEntry3(taxon);
                }
            } else {
                buildAlertMessageImpreciseCoordinates();
            }
        }
    }

    /*
    /  PART 3: Check if the location has been changed and ask the user to update location name
    */
    private void saveEntry3 (TaxonDb taxon) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String location_name = sharedPreferences.getString("location_name", null);
        if (location_name == null) {
            saveEntry4(taxon);
        } else {
            if (location_name.equals("")) {
                saveEntry4(taxon);
            } else {
                Location old_location = new Location("");
                if (SettingsManager.getPreviousLocationLong() == null) {
                    SettingsManager.setPreviousLocationLong(String.valueOf(currentLocation.longitude));
                    SettingsManager.setPreviousLocationLat(String.valueOf(currentLocation.latitude));
                    saveEntry4(taxon);
                } else {
                    old_location.setLongitude(Double.parseDouble(SettingsManager.getPreviousLocationLong()));
                    old_location.setLatitude(Double.parseDouble(SettingsManager.getPreviousLocationLat()));
                    Location current_location = new Location("");
                    current_location.setLongitude(currentLocation.longitude);
                    current_location.setLatitude(currentLocation.latitude);
                    double distance = old_location.distanceTo(current_location);

                    if (distance > 3000) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(getString(R.string.location_changed, location_name))
                                .setCancelable(false)
                                .setTitle("Location changed!")

                                // If a user choose to update location name
                                .setPositiveButton(getString(R.string.update_location_name), (dialog, id) -> {
                                    SettingsManager.setPreviousLocationLong(String.valueOf(currentLocation.longitude));
                                    SettingsManager.setPreviousLocationLat(String.valueOf(currentLocation.latitude));
                                    dialog.dismiss();

                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                                    builder1.setTitle("Location");
                                    final EditText input = new EditText(this);
                                    FrameLayout container = new FrameLayout(this);
                                    FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT);
                                    params.leftMargin = getResources().getDimensionPixelSize(R.dimen.edit_text_dialog_margin);
                                    params.rightMargin = getResources().getDimensionPixelSize(R.dimen.edit_text_dialog_margin);
                                    input.setLayoutParams(params);
                                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                                    container.addView(input);
                                    builder1.setView(container);
                                    builder1.setCancelable(false);
                                    builder1.setPositiveButton("OK", (dialog1, which) -> {
                                        String new_location_name = input.getText().toString();
                                        if (new_location_name.equals("")) {
                                            SettingsManager.setPreviousLocationLong(null);
                                            SettingsManager.setPreviousLocationLat(null);
                                        }
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString("location_name", new_location_name);
                                        editor.apply();
                                        dialog1.dismiss();
                                        saveEntry4(taxon);
                                    });
                                    builder1.show();
                                })

                                // If a user chooses to keep the current location name
                                .setNeutralButton(getString(R.string.keep_location_name), (dialog, id) -> {
                                    // Update location within the settings to the current one
                                    SettingsManager.setPreviousLocationLong(String.valueOf(currentLocation.longitude));
                                    SettingsManager.setPreviousLocationLat(String.valueOf(currentLocation.latitude));
                                    dialog.dismiss();
                                    saveEntry4(taxon);
                                })

                                // If the user chooses to remove location name
                                .setNegativeButton(getString(R.string.remove_location_name), (dialog, id) -> {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("location_name", null);
                                    editor.apply();
                                    SettingsManager.setPreviousLocationLong(null);
                                    SettingsManager.setPreviousLocationLat(null);
                                    dialog.dismiss();
                                    saveEntry4(taxon);
                                });
                        final AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        saveEntry4(taxon);
                    }
                }
            }
        }
    }

    /*
    /  PART 4: Check if both male and female entries should be created
    */
    private void saveEntry4(TaxonDb taxon) {
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
    private void entrySaver(TaxonDb taxon, String specimens, String sex, int entry_id) {
        String comment = editTextComment.getText().toString();
        Integer numberOfSpecimens = (!specimens.equals("")) ? Integer.valueOf(specimens) : null;
        Long selectedStage = (textViewStage.getTag() != null) ? Long.parseLong(textViewStage.getTag().toString()) : null;
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
            long taxon_id = taxon.getId();
            String taxon_name = taxon.getLatinName();
            String project_name = PreferenceManager.getDefaultSharedPreferences(this).getString("project_name", "0");
            String location = PreferenceManager.getDefaultSharedPreferences(this).getString("location_name", "");
            getPhotoTag();
            observation_type_ids_string = Arrays.toString(observation_type_ids);
            Log.d(TAG, "Converting array of observation type IDs into string: " + observation_type_ids_string);

            // Get the data structure and save it into a database Entry
            EntryDb entryDb1 = new EntryDb(0, taxon_id, taxon_name, year, month, day, comment, numberOfSpecimens, sex, selectedStage, getAtlasCode(),
                    String.valueOf(!checkBox_dead.isChecked()), deathComment,
                    Double.parseDouble(String.format(Locale.ENGLISH, "%.6f", currentLocation.latitude)),
                    Double.parseDouble(String.format(Locale.ENGLISH, "%.6f", currentLocation.longitude)),
                    Double.parseDouble(String.format(Locale.ENGLISH, "%.0f", acc)),
                    Double.parseDouble(String.format(Locale.ENGLISH, "%.0f", elev)),
                    location, image1, image2, image3, project_name, foundOn, String.valueOf(getGreenDaoDataLicense()),
                    getGreenDaoImageLicense(), time, habitat, observation_type_ids_string);
            Box<EntryDb> entry = App.get().getBoxStore().boxFor(EntryDb.class);
            entry.put(entryDb1);

            Intent intent = new Intent();
            long index_last = App.get().getBoxStore().boxFor(EntryDb.class).count() - 1;
            long new_entry_id = App.get().getBoxStore().boxFor(EntryDb.class).getAll().get((int) index_last).getId();
            Log.d(TAG, "Entry will be saved under ID " + new_entry_id);
            intent.putExtra("IS_NEW_ENTRY", isNewEntry());
            intent.putExtra("ENTRY_LIST_ID", new_entry_id);
            setResult(RESULT_OK, intent);
            finish();

        }

        // If the entry exist already
        else {
            currentItem.get(0).setTaxonId(taxon.getId());
            currentItem.get(0).setTaxonSuggestion(taxon.getLatinName());
            currentItem.get(0).setComment(comment);
            currentItem.get(0).setNoSpecimens(numberOfSpecimens);
            currentItem.get(0).setSex(sex);
            currentItem.get(0).setStage(selectedStage);
            currentItem.get(0).setAtlasCode(getAtlasCode());
            currentItem.get(0).setDeadOrAlive(String.valueOf(!checkBox_dead.isChecked()));
            currentItem.get(0).setCauseOfDeath(deathComment);
            currentItem.get(0).setLattitude(currentLocation.latitude);
            currentItem.get(0).setLongitude(currentLocation.longitude);
            currentItem.get(0).setElevation(elev);
            currentItem.get(0).setAccuracy(acc);
            currentItem.get(0).setSlika1(image1);
            currentItem.get(0).setSlika2(image2);
            currentItem.get(0).setSlika3(image3);
            currentItem.get(0).setHabitat(habitat);
            currentItem.get(0).setFoundOn(foundOn);
            getPhotoTag();
            currentItem.get(0).setObservationTypeIds(Arrays.toString(observation_type_ids));

            // Now just update the database with new data...
            Box<EntryDb> entry = App.get().getBoxStore().boxFor(EntryDb.class);
            entry.put(currentItem);

            Intent intent = new Intent();
            long current_entry_id = currentItem.get(0).getId();
            Log.d(TAG, "Entry will be saved under existing ID " + current_entry_id);
            intent.putExtra("IS_NEW_ENTRY", isNewEntry());
            intent.putExtra("ENTRY_LIST_ID", current_entry_id);
            setResult(RESULT_OK, intent);
            finish();

        }
        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();

    }

    private void getPhotoTag() {
        if (image1 != null || image2 != null || image3 != null) {
            int photo_tag_id;
            Box<ObservationTypesDb> observationTypesDataBox = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
            Query<ObservationTypesDb> observationTypesDataQuery = observationTypesDataBox
                    .query(ObservationTypesDb_.slug.equal("photographed"))
                    .build();
            List<ObservationTypesDb> observationTypesDbs = observationTypesDataQuery.find();
            if (!observationTypesDbs.isEmpty()) {
                photo_tag_id = (int) observationTypesDbs.get(0).getObservationId();
                Log.d(TAG, "Photographed tag has ID: " + photo_tag_id);
                observation_type_ids = ArrayHelper.insertIntoArray(observation_type_ids, photo_tag_id);
            } else {
                Toast.makeText(this, getString(R.string.observation_types_not_downloaded), Toast.LENGTH_LONG).show();
            }
            observationTypesDataQuery.close();
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
            Box<StageDb> stage = App.get().getBoxStore().boxFor(StageDb.class);
            Query<StageDb> query = stage
                    .query(StageDb_.id.equal(taxonID))
                    .build();
            int size = query.find().size();
            query.close();
            return size != 0;
        }
    }

    private void getStageForTaxon() {
        String stages = selectedTaxon.getStages();
        String[] all_stages_ids = stages.split(";");
        if (all_stages_ids.length != 0) {
            final String[] all_stages_names = new String[all_stages_ids.length + 1];
            all_stages_names[0] = getString(R.string.not_selected);
            for (int i = 0; i < all_stages_ids.length; i++) {
                Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
                Query<StageDb> query = stageBox
                        .query(StageDb_.id.equal(Long.parseLong(all_stages_ids[i])))
                        .build();
                StageDb stage = query.findFirst();
                query.close();
                if (stage != null) {
                    all_stages_names[i + 1] = StageAndSexLocalization.getStageLocale(this, stage.getName());
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(all_stages_names, (DialogInterface dialogInterface, int i) -> {
                textViewStage.setText(all_stages_names[i]);
                if (i == 0) {
                    textViewStage.setTag(null); // If no stage selected
                } else {
                    textViewStage.setTag(all_stages_ids[i - 1]);
                }
            });
            builder.show();
            Log.d(TAG, "Available stages for " + getLatinName() + " include: " + Arrays.toString(all_stages_names));

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

        // If user select atlas code 2, the "call" tag should also be checked.
        textViewAtlasCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Long atlas_code_id = getAtlasCode();
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
        bundle.putDouble("LATITUDE", currentLocation.latitude);
        bundle.putDouble("LONGITUDE", currentLocation.longitude);
        bundle.putDouble("ACCURACY", acc);
        bundle.putDouble("ELEVATION", elev);
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

    public void showDeadComment() {
        if (checkBox_dead.isChecked()) {
            textViewDeathComment.setVisibility(View.VISIBLE);
        } else {
            textViewDeathComment.setVisibility(View.GONE);
            editTextDeathComment.setText("");
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
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        Log.i(TAG, "Camera returned picture.");
                        resizeAndDisplayImage(current_image);
                    }
                }
            }
    );

    private void takePhotoFromCamera() {
        current_image = FileManipulation.newExternalDocumentFile(this, null, ".jpg");
        takePictureFromCamera.launch(current_image);
    }

    ActivityResultLauncher<PickVisualMediaRequest> pickMultipleImages = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(getEmptyImageSlots()), photoPicker -> {
                if (!photoPicker.isEmpty()) {
                    Log.d(TAG,getEmptyImageSlots() + " images allowed, " + photoPicker.size() + " selected.");
                    if (photoPicker.size() > getEmptyImageSlots()) {
                        Toast.makeText(this,
                                getString(R.string.limit_photo1) + " " + photoPicker.size() + " " +
                                        getString(R.string.limit_photo2) + " " + getEmptyImageSlots() + " " +
                                        getString(R.string.limit_photo3), Toast.LENGTH_LONG).show();
                    }

                    // TODO get location from image Exif data of the image, issue #27
                    Uri uri = photoPicker.get(photoPicker.size() - 1);
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

                    for (int i = 0; i < photoPicker.size(); i++) {
                        Log.d(TAG, "Getting and preparing image " + photoPicker.get(i) + ".");
                        resizeAndDisplayImage(photoPicker.get(i));
                    }
                }
                else {
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

    private final ActivityResultLauncher<Intent> openMap = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    locationManager.removeUpdates(locationListener);
                    if (result.getData() != null) {
                        currentLocation = getLatLongFromMap(result);
                        Log.d(TAG, "Map returned this result: " + currentLocation);
                        locationFromTheMap = true;
                        setLocationValues(currentLocation.latitude, currentLocation.longitude);
                        acc = getAccuracyFromMap(result);
                        elev = getElevationFromMap(result);
                    }

                    // Update the coordinate accuracy labels
                    if (acc == 0.0) {
                        textViewGPSAccuracy.setText(R.string.unknown);
                        textViewMeters.setVisibility(View.GONE);
                    } else {
                        textViewGPSAccuracy.setText(String.format(Locale.ENGLISH, "%.0f", acc));
                        textViewMeters.setVisibility(View.VISIBLE);
                        setAccuracyColor();
                    }
                }
            });

    private int getEmptyImageSlots() {
        int emptySlots = 0;
        if (image1 == null) {
            emptySlots = emptySlots + 1;
        } if (image2 == null) {
            emptySlots = emptySlots + 1;
        } if (image3 == null) {
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
            String[] perm = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            requestLocationPermissions.launch(perm);
        }
    }

    private void setLocationValues(double latitude_value, double longitude_value) {
        latitude = String.format(Locale.ENGLISH, "%.4f", (latitude_value));
        longitude = String.format(Locale.ENGLISH, "%.4f", (longitude_value));
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
                    saveEntry3(getSelectedTaxon(selectedTaxon.getId()));
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
        for (String image: list_new_images) {
            if (image != null) {
                String filename = new File(image).getName();
                final File file = new File(getFilesDir(), filename);
                boolean b = file.delete();
                Log.d(TAG, "Deleting image " + image + " returned: " + b);
            }
        }
    }

    private int getGreenDaoDataLicense() {
        if (userDataList != null) {
            if (!userDataList.isEmpty()) {
                return userDataList.get(0).getDataLicense();
            }
            return 0;
        }
        return 0;
    }

    private int getGreenDaoImageLicense() {
        if (userDataList != null) {
            if (!userDataList.isEmpty()) {
                return userDataList.get(0).getImageLicense();
            }
            return 0;
        }
        return 0;
    }

    private TaxonDb getSelectedTaxon(Long taxonID) {
        if (taxonID == null) {
            return null;
        } else {
            Box<TaxonDb> taxonDataBox = App.get().getBoxStore().boxFor(TaxonDb.class);
            Query<TaxonDb> query = taxonDataBox
                    .query(TaxonDb_.id.equal(taxonID))
                    .build();
            TaxonDb taxon = query.find(0, 1).get(0);
            query.close();
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

        Box<ObservationTypesDb> observationTypesDataBox = App.get().getBoxStore().boxFor(ObservationTypesDb.class);
        Query<ObservationTypesDb> query = observationTypesDataBox
                .query(ObservationTypesDb_.locale.equal(locale_script))
                .build();
        List<ObservationTypesDb> list = query.find();
        query.close();

        long number_of_observation_types = list.size();

        Log.d(TAG, "Filling types of observations with " + locale_script + " script names. Total of " + number_of_observation_types + " entries.");

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
                chip.setChecked(ArrayHelper.arrayContainsNumber(observation_type_ids, observation_id));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    String text = (String) compoundButton.getTag();
                    int id = compoundButton.getId();
                    if (compoundButton.isChecked()) {
                        Log.d(TAG, "Chip button \"" + text + "\" selected, ID: " + id);
                        observation_type_ids = ArrayHelper.insertIntoArray(observation_type_ids, id);
                    } else {
                        Log.d(TAG, "Chip button \"" + text + "\" deselected, ID: " + id);
                        observation_type_ids = ArrayHelper.removeFromArray(observation_type_ids, id);
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

}