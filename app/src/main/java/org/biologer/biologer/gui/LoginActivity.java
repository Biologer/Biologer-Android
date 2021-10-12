package org.biologer.biologer.gui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.network.GetTaxaGroups;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.LoginResponse;
import org.biologer.biologer.network.JSON.TaxaResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;
import org.biologer.biologer.network.JSON.UserDataResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Login";

    int retry_login = 1;

    Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable;

    EditText et_username;
    EditText et_password;
    TextInputLayout til_username;
    TextInputLayout til_password;
    TextView tv_devDatabase;
    Button loginButton;
    String database_name;
    ProgressBar progressBar;
    TextView forgotPassTextView;

    String old_username;
    String old_password;

    /*
     Get the keys for client applications. Separate client key should be given for each Biologer server
     application. This workaround is used to hide secret client_key from the source code. The developers
     should put the key in ~/.gradle/gradle.properties.
      */
    String rsKey = BuildConfig.BiologerRS_Key;
    String hrKey = BuildConfig.BiologerHR_Key;
    String baKey = BuildConfig.BiologerBA_Key;
    String devKey = BuildConfig.BiologerDEV_Key;
    String birdKey = BuildConfig.Birdloger_Key;

    Call <LoginResponse> login;

    // Initialise list for Database selection
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        til_password = findViewById(R.id.layout_password);
        til_username = findViewById(R.id.layout_name);
        tv_devDatabase = findViewById(R.id.tv_devDatabase);

        database_name = SettingsManager.getDatabaseName();
        Log.d(TAG, "Database URL written in the settings is: " + database_name);

        // Just display the username in order to make app nice
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getString("email") != null) {
            Log.d(TAG, "Filling user data from register dialog.");
            et_username.setText(bundle.getString("email"));
            et_password.setText(bundle.getString("password"));
        } else {
            List<UserData> user = App.get().getDaoSession().getUserDataDao().loadAll();
            if (!user.isEmpty()) {
                Log.d(TAG, "There is user in the SQL database: " + user.get(0).getUsername());
                et_username.setText(user.get(0).getEmail());
                // Just display anything, no mather what...
                et_password.setText("random_string");
            }
        }

        // Fill in the data for database list
        spinner = findViewById(R.id.spinner_databases);
        String[] Databases = {
                getString(R.string.database_serbia),
                getString(R.string.database_croatia),
                getString(R.string.database_bih),
                getString(R.string.database_dev_version)};
        Integer[] Icons = {
                R.drawable.flag_serbia,
                R.drawable.flag_croatia,
                R.drawable.flag_bosnia,
                R.drawable.ic_hammer_developers};
        ImageArrayAdapter ourDatabases = new ImageArrayAdapter(this, Icons, Databases);
        spinner.setAdapter(ourDatabases);
        spinner.setSelection(getSpinnerIdFromUrl(database_name));
        spinner.setOnItemSelectedListener(new getDatabaseURL());

        // Android 4.4 (KitKat) compatibility: Set button listener programmatically.
        // Login button.
        loginButton = findViewById(R.id.btn_login);
        loginButton.setOnClickListener(this::onLogin);

        // Register link.
        // This is intro text which should contain a link to privacy policy to click on
        TextView registerTextView = findViewById(R.id.ctv_register);
        String register_string = getString(R.string.no_account1) + " " +
                getString(R.string.no_account2) + " " +
                getString(R.string.no_account3) + ".";
        // Hyperlink a part of the text
        SpannableString register_string_span = new SpannableString(register_string);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View textView) {
                onRegister(textView);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setFakeBoldText(true);
                ds.setUnderlineText(false);
            }
        };
        int start = register_string.indexOf(getString(R.string.no_account2));
        int end = register_string.lastIndexOf(getString(R.string.no_account2)) + getString(R.string.no_account2).length();
        register_string_span.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        registerTextView.setText(register_string_span);
        registerTextView.setMovementMethod(LinkMovementMethod.getInstance());
        registerTextView.setHighlightColor(Color.TRANSPARENT);

        // Forgot password link.
        forgotPassTextView = findViewById(R.id.ctv_forgotPass);
        forgotPassTextView.setOnClickListener(this::onForgotPass);

        progressBar = findViewById(R.id.login_progressBar);

        et_username.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "Username focus changed!");
            tv_devDatabase.setText("");
            if (!et_username.getText().toString().isEmpty()) {
                Log.d(TAG, "There is some text written as the username.");
                setEmailError();
                et_username.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        old_username = et_username.getText().toString();
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        setEmailError();
                        enableButton();

                        if (!old_username.equals(et_username.getText().toString())) {
                            if (SettingsManager.getAccessToken() != null) {
                                Log.d(TAG, "Username changed! Deleting Access Token.");
                                SettingsManager.deleteAccessToken();
                            }
                        }
                    }
                });
            }
        });

        et_password.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "Password focus changed!");
            setPasswordError();
            tv_devDatabase.setText("");
            if (!et_password.getText().toString().isEmpty()) {
                Log.d(TAG, "There is some text written as password.");
                et_password.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        old_password = et_password.getText().toString();
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        setPasswordError();
                        enableButton();

                        if (!old_password.equals(et_password.getText().toString())) {
                            if (SettingsManager.getAccessToken() != null) {
                                Log.d(TAG, "Password changed! Deleting Access Token.");
                                SettingsManager.deleteAccessToken();
                            }
                        }
                    }
                });
            }
        });

        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "Listening on password changes.");
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    setPasswordError();
                    enableButton();
                };
                handler.postDelayed(runnable, 1500);
            }
        });

        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "Listening on email changes.");
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    setEmailError();
                    enableButton();
                };
                handler.postDelayed(runnable, 1500);
            }
        });

        // Disable login button if there is no username and password
        if (et_username.getText().toString().equals("") && et_password.getText().toString().equals("")) {
            loginButton.setEnabled(false);
        }

    }

    private void setEmailError() {
        if (!(isValidEmail(et_username.getText().toString()))) {
            til_username.setError(getString(R.string.invalid_email));
        }
        else {
            til_username.setError(null);
        }
    }

    private void setPasswordError() {
        if (et_password.getText().length() < 8) {
            til_password.setError(getString(R.string.password_too_short));
        } else {
            til_password.setError(null);
        }
    }

    private void enableButton() {
        loginButton.setEnabled(et_password.getText().length() >= 8 && isValidEmail(et_username.getText().toString()));
    }

    private int getSpinnerIdFromUrl(String url) {
        if (url.equals("https://biologer.rs")) {return 0;}
        if (url.equals("https://biologer.hr")) {return 1;}
        if (url.equals("https://biologer.ba")) {return 2;}
        if (url.equals("https://birdloger.biologer.org")) {return 3;}
        return 4;
    }

    public static class ImageArrayAdapter extends ArrayAdapter<Integer> {
        private final Integer[] images;
        private final String[] text;
        private final Context context;

        ImageArrayAdapter(Context context, Integer[] images, String[] text) {
            super(context, R.layout.databases_and_icons, images);
            this.images = images;
            this.text = text;
            this.context = context;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getImageForPosition(position, parent);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return getImageForPosition(position, parent);
        }

        private View getImageForPosition(int position, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            View row = inflater.inflate(R.layout.databases_and_icons, parent, false);
            TextView textView = row.findViewById(R.id.database_name);
            textView.setText(text[position]);
            ImageView imageView = row.findViewById(R.id.database_icon);
            imageView.setImageResource(images[position]);
            return row;
        }
    }

    // Activity starting after user selects a Database from the list
    public class getDatabaseURL implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> database, View view, int pos,long id) {
            // Change the preference according to the user selection
            long database_id = database.getItemIdAtPosition(pos);
            String old_database = database_name;
            database_name = database.getItemAtPosition(pos).toString();
            if (database_id == 0) {database_name = "https://biologer.rs";}
            if (database_id == 1) {database_name = "https://biologer.hr";}
            if (database_id == 2) {database_name = "https://biologer.ba";}
            if (database_id == 3) {database_name = "https://birdloger.biologer.org";}
            if (database_id == 4) {database_name = "https://dev.biologer.org";}

            Log.i(TAG, "Database No. " + database_id + " selected: " + database_name);

            SettingsManager.setDatabaseName(database_name);

            if (!old_database.equals(database_name)) {
                Log.d(TAG, "User selected different database, so we should delete token.");
                SettingsManager.deleteAccessToken();
            }

            String hint_text = getString(R.string.URL_address) + " " + database_name;
            tv_devDatabase.setText(hint_text);
            if (database_name.equals("https://dev.biologer.org")) {
                tv_devDatabase.setTextColor(ContextCompat.getColor(LoginActivity.this, R.color.warningRed));
            } else {
                tv_devDatabase.setTextColor(ContextCompat.getColor(LoginActivity.this, R.color.colorPrimary));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }

    }

    // Run when user click the login button
    public void onLogin(View view) {
        loginButton.setEnabled(false);
        String token = SettingsManager.getAccessToken();

        // If there is no token, we should get one from the server
        if (token == null) {
            Log.d(TAG, "THERE IS NO TOKEN. Trying to log in with username and password.");
            getToken();
        }
        else {
            Log.d(TAG, "TOKEN: " + token);
            Log.d(TAG, "There is a token. Trying to log in without username/password.");
            logInTest();
        }
    }

    private void getToken() {

        displayProgressBar(true);

        // Change the call according to the database selected
        if (database_name.equals("https://biologer.rs")) {
            Log.d(TAG, "Serbian database selected.");
            login = RetrofitClient.getService(database_name).login("password", "2", rsKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        if (database_name.equals("https://biologer.hr")) {
            Log.d(TAG, "Croatian database selected.");
            login = RetrofitClient.getService(database_name).login("password", "2", hrKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        if (database_name.equals("https://biologer.ba")) {
            Log.d(TAG, "Bosnian database selected.");
            login = RetrofitClient.getService(database_name).login("password", "2", baKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        if (database_name.equals("https://birdloger.biologer.org")) {
            Log.d(TAG, "Birdloger database selected.");
            login = RetrofitClient.getService(database_name).login("password", "3", birdKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        if (database_name.equals("https://dev.biologer.org")) {
            Log.d(TAG, "Developmental database selected.");
            login = RetrofitClient.getService(database_name).login("password", "6", devKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }

        Log.d(TAG, "Logging into " + database_name + " as user " + et_username.getText().toString());

        // Get the response from the call
        Log.d(TAG, "Getting Token from server.");
        login.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> login, @NonNull Response<LoginResponse> response) {
                if(response.code() == 404) {
                    Log.e(TAG, "Error 404: This response is not implemented on a server.");
                }
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        String token = response.body().getAccessToken();
                        String refresh_token = response.body().getRefreshToken();
                        //Log.d(TAG, "Token value is: " + token);
                        SettingsManager.setAccessToken(token);
                        SettingsManager.setRefreshToken(refresh_token);
                        SettingsManager.setMailConfirmed(true);
                        long expire = response.body().getExpiresIn();
                        long expire_date = (System.currentTimeMillis()/1000) + expire;
                        SettingsManager.setTokenExpire(String.valueOf(expire_date));
                        Log.d(TAG, "Token will expire on timestamp: " + expire_date);
                        getUserData();
                    }
                }
                else {
                    retry_login ++;
                    if (retry_login >= 4) {
                        forgotPassTextView.setVisibility(View.VISIBLE);
                    }
                    til_password.setError(getString(R.string.wrong_creds));
                    til_username.setError(getString(R.string.wrong_creds));
                    displayProgressBar(false);
                    Log.d(TAG, "Unsuccessful response! The response body was: " + response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<LoginResponse> login, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.cannot_connect_token), Toast.LENGTH_LONG).show();
                displayProgressBar(false);
                Log.e(TAG, "Cannot get response from the server (token)");
            }
        });
    }

    private void getUserData() {
        Call<UserDataResponse> userData = RetrofitClient.getService(database_name).getUserData();
        userData.enqueue(new Callback<UserDataResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserDataResponse> call, @NonNull Response<UserDataResponse> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String email = response.body().getData().getEmail();
                    String name = response.body().getData().getFullName();
                    int data_license = response.body().getData().getSettings().getDataLicense();
                    int image_license = response.body().getData().getSettings().getImageLicense();
                    UserData user = new UserData(null, name, email, data_license, image_license);
                    App.get().getDaoSession().getUserDataDao().insertOrReplace(user);

                    Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
                    intent.putExtra("fromLoginScreen", true);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDataResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "User data could not be taken from server!");
            }
        });

        // Fetch Taxa groups for preferences
        final Intent getTaxaGroups = new Intent(this, GetTaxaGroups.class);
        startService(getTaxaGroups);
    }

    private void logInTest() {
        Log.d(TAG, "Logging in attempt.");
        displayProgressBar(true);
        Call<TaxaResponse> service = RetrofitClient.getService(database_name).getTaxa(1,1,0, false, null, true);
        service.enqueue(new Callback<TaxaResponse>() {
            @Override
            public void onResponse(@NonNull Call<TaxaResponse> service, @NonNull Response<TaxaResponse> response) {
                if (response.code() == 403) {
                    SettingsManager.setMailConfirmed(false);
                    displayProgressBar(false);
                    dialogConfirmMail(getString(R.string.confirm_email));
                }
                if (response.code() == 401) {
                    tv_devDatabase.setText(getString(R.string.check_database));
                    displayProgressBar(false);
                    dialogConfirmMail(getString(R.string.unauthorised));
                }
                else {
                    if (response.body() != null) {
                        SettingsManager.setMailConfirmed(true);
                        displayProgressBar(false);
                        Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
                        startActivity(intent);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<TaxaResponse> service, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.cannot_connect_server), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Cannot get response from the server (test taxa response)");
                displayProgressBar(false);
            }
        });
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private void displayProgressBar(Boolean value) {
        if (value) {
            loginButton.setEnabled(false);
            loginButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            loginButton.setEnabled(true);
        }
    }

    private void dialogConfirmMail(String message) {
        // Show the message if registration was successful
        final AlertDialog.Builder builder_taxon = new AlertDialog.Builder(LoginActivity.this);
        builder_taxon.setMessage(message)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.OK), (dialog, id) -> {
                    loginButton.setEnabled(true);
                    dialog.dismiss();
                });
        final AlertDialog alert = builder_taxon.create();
        alert.show();
    }

    public void onRegister(View view) {
        Intent registerIntent = new Intent(LoginActivity.this, Register.class);
        registerIntent.putExtra("database", database_name);
        registerIntent.putExtra("username", et_username.getText().toString());
        registerIntent.putExtra("password", et_password.getText().toString());
        startActivity(registerIntent);
    }

    public void onForgotPass(View view) {
        String url = SettingsManager.getDatabaseName() + "/password/reset";
        Intent defaultBrowser = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
        defaultBrowser.setData(Uri.parse(url));
        startActivity(defaultBrowser);
    }

}
