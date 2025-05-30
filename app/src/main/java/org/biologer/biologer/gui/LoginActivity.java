package org.biologer.biologer.gui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.json.LoginResponse;
import org.biologer.biologer.network.json.UserDataResponse;
import org.biologer.biologer.network.json.UserDataSer;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserDb;

import java.util.Arrays;
import java.util.List;

import io.objectbox.Box;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Login";
    int retry_login = 1;
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable;
    EditText et_username, et_password;
    TextInputLayout til_username, til_password;
    TextView tv_devDatabase, forgotPassTextView;
    Button loginButton;
    public String database_name;
    ProgressBar progressBar;
    String old_username, old_password,
            register_name, register_surname, register_institution;
    static String[] allDatabases = {"https://biologer.rs",
            "https://biologer.hr",
            "https://biologer.ba",
            "https://biologer.me",
            "https://dev.biologer.org"};

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
        List<UserDb> user = App.get().getBoxStore().boxFor(UserDb.class).getAll();
        if (!user.isEmpty()) {
            Log.d(TAG, "There is user in the SQL database: " + user.get(0).getUsername());
            // Just set the user email so that is does not need to be written once again
            et_username.setText(user.get(0).getEmail());
        }

        // Fill in the data for database list
        spinner = findViewById(R.id.spinner_databases);
        String[] Databases = {
                getString(R.string.database_serbia),
                getString(R.string.database_croatia),
                getString(R.string.database_bih),
                getString(R.string.montenegro),
                getString(R.string.database_dev_version)};
        Integer[] Icons = {
                R.drawable.flag_serbia,
                R.drawable.flag_croatia,
                R.drawable.flag_bosnia,
                R.drawable.flag_montenegro,
                R.drawable.flag_dev};
        ImageArrayAdapter ourDatabases = new ImageArrayAdapter(this, Icons, Databases);
        spinner.setAdapter(ourDatabases);
        if (database_name != null) {
            spinner.setSelection(getSpinnerIdFromUrl(database_name));
        }
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
                        setEmailError();
                        enableButton();

                        if (!old_username.equals(et_username.getText().toString())) {
                            if (SettingsManager.getAccessToken() != null) {
                                Log.d(TAG, "Username changed! Deleting Access Token.");
                                SettingsManager.deleteAccessToken();
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
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
                        setPasswordError();
                        enableButton();

                        if (!old_password.equals(et_password.getText().toString())) {
                            if (SettingsManager.getAccessToken() != null) {
                                Log.d(TAG, "Password changed! Deleting Access Token.");
                                SettingsManager.deleteAccessToken();
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
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
                Log.d(TAG, "Listening on password changes.");
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    setPasswordError();
                    enableButton();
                };
                handler.postDelayed(runnable, 1500);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Listening on email changes.");
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    setEmailError();
                    enableButton();
                };
                handler.postDelayed(runnable, 1500);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageView imageViewShowPassword = findViewById(R.id.show_password_icon);
        imageViewShowPassword.setOnClickListener(view -> {
            int inputType = et_password.getInputType();
            if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                imageViewShowPassword.setImageResource(R.drawable.eye_open);
                et_password.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imageViewShowPassword.setContentDescription(getString(R.string.hide_password));
            } else {
                imageViewShowPassword.setImageResource(R.drawable.eye_closed);
                et_password.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imageViewShowPassword.setContentDescription(getString(R.string.show_password));
            }
            et_password.setSelection(et_password.getText().length());
        });

        // Disable login button if there is no username and password
        if (et_username.getText().toString().isEmpty() && et_password.getText().toString().isEmpty()) {
            loginButton.setEnabled(false);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.getString("refreshToken") != null) {
                Log.d(TAG, "It looks like login token is expired. We must use the password.");
                registerTextView.setText("");
                LinearLayout linearLayout = findViewById(R.id.linearlayout_databases);
                linearLayout.setVisibility(View.GONE);
                View view = findViewById(R.id.underline);
                view.setVisibility(View.GONE);
                TextView textView = findViewById(R.id.tv_refreshToken);
                textView.setText(R.string.refresh_token_text);
                textView.setVisibility(View.VISIBLE);
                et_password.requestFocus();
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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

    private void setEmailError() {
        if (!(isValidEmail(et_username.getText().toString()))) {
            til_username.setError(getString(R.string.invalid_email));
        }
        else {
            til_username.setError(null);
        }
    }

    private void setPasswordError() {
        int passwordLength = et_password.getText().length();

        if (passwordLength == 0) {
            til_password.setError(null);
        } else if (passwordLength < 8) {
            til_password.setError(getString(R.string.password_too_short));
        } else {
            til_password.setError(null);
        }
    }

    private void enableButton() {
        loginButton.setEnabled(et_password.getText().length() >= 8 && isValidEmail(et_username.getText().toString()));
    }

    public static int getSpinnerIdFromUrl(String url) {
        List<String> databasesList = Arrays.asList(allDatabases);
        return databasesList.indexOf(url);
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
            database_name = allDatabases[(int) database_id];

            Log.i(TAG, "Database No. " + database_id + " selected: " + database_name);

            SettingsManager.setDatabaseName(database_name);

            if (old_database != null) {
                if (!old_database.equals(database_name)) {
                Log.d(TAG, "User selected different database, so we should delete token.");
                SettingsManager.deleteAccessToken();
            }
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
        } else {
            Log.d(TAG, "There is a valid token. Trying to log in without username/password.");
            getUserData();
        }
    }

    private void getToken() {

        if (database_name == null) {
            Toast.makeText(this, R.string.the_database_is_not_selected, Toast.LENGTH_LONG).show();
        } else {

            displayProgressBar(true);

            login = RetrofitClient.getService(database_name).login("password", getClientIdForDatabase(database_name), getClientKeyForDatabase(database_name), "*", et_username.getText().toString(), et_password.getText().toString());

            Log.d(TAG, "Logging into " + database_name + " as user " + et_username.getText().toString());

            // Get the response from the call
            Log.d(TAG, "Getting Token from server.");
            login.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<LoginResponse> login, @NonNull Response<LoginResponse> response) {
                    if (response.code() == 404) {
                        Log.e(TAG, "Error 404: This response is not implemented on a server.");
                    }
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            String token = response.body().getAccessToken();
                            String refresh_token = response.body().getRefreshToken();
                            SettingsManager.setAccessToken(token);
                            SettingsManager.setRefreshToken(refresh_token);
                            long expire = response.body().getExpiresIn();
                            long expire_date = (System.currentTimeMillis() / 1000) + expire;
                            SettingsManager.setTokenExpire(String.valueOf(expire_date));
                            Log.d(TAG, "Token will expire on timestamp: " + expire_date);
                            getUserData();
                        }
                    } else {
                        retry_login++;
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

    }

    private void getUserData() {
        Call<UserDataResponse> userData = RetrofitClient.getService(database_name).getUserData();
        userData.enqueue(new Callback<>() {

            @Override
            public void onResponse(@NonNull Call<UserDataResponse> call, @NonNull Response<UserDataResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {

                        if (response.code() == 401) {
                            tv_devDatabase.setText(getString(R.string.check_database));
                            displayProgressBar(false);
                            dialogConfirmMail(getString(R.string.unauthorised));
                        }

                        UserDataSer userdata = response.body().getData();
                        if (userdata != null) {
                            String email = userdata.getEmail();
                            String name = userdata.getFullName();
                            int id = userdata.getId();
                            int data_license = userdata.getSettings().getDataLicense();
                            int image_license = userdata.getSettings().getImageLicense();

                            // Check if email is confirmed
                            if (userdata.isEmailVerified()) {
                                SettingsManager.setMailConfirmed(true);
                            }

                            // Write data in SQL
                            UserDb user = new UserDb(0, name, email, data_license, image_license, id);
                            Box<UserDb> userDataBox = App.get().getBoxStore().boxFor(UserDb.class);
                            userDataBox.removeAll();
                            userDataBox.put(user);
                        }

                        startLandingActivity();
                    } else {
                        Log.e(TAG, "User data (response.body().getData()) is null for successful response. Response code: " + response.code());
                        displayProgressBar(false);
                        Toast.makeText(LoginActivity.this, getString(R.string.error_fetching_user_data), Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Log.e(TAG, "Response body is null for successful login. Response code: " + response.code());
                    displayProgressBar(false);
                    // Optionally show an error message to the user
                    Toast.makeText(LoginActivity.this, getString(R.string.error_fetching_user_data1), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserDataResponse> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.cannot_connect_server), Toast.LENGTH_LONG).show();
                Log.e(TAG, "User data could not be taken from server!");
            }
        });
    }

    private void startLandingActivity() {
        Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("showHelpMessage", true);
        startActivity(intent);
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
        LinearLayout linearLayout = findViewById(R.id.login_layout);
        linearLayout.setVisibility(View.GONE);
        Fragment fragment = new RegisterFragment1();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.login_container, fragment);
        fragmentTransaction.addToBackStack("Register fragment 1");
        fragmentTransaction.commit();
    }

    public void onForgotPass(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_reset_email, null);
        builder.setView(dialogView);

        EditText editTextEmail = dialogView.findViewById(R.id.dialog_edit_text);

        builder.setTitle(getString(R.string.change_password_dialog));
        builder.setMessage(getString(R.string.change_pass_msg1) + ". " +
                getString(R.string.change_pass_msg2) + ".");
        builder.setPositiveButton(getString(R.string.send_email), null);
        builder.setNegativeButton(getString(R.string.cancel), (dialogInterface, id) -> dialogInterface.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);

        positiveButton.setOnClickListener(v -> {
            String emailText = editTextEmail.getText().toString();
            Call<ResponseBody> resetForgottenPassword = RetrofitClient.getService(database_name).resetForgottenPassword(emailText);
            resetForgottenPassword.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), getString(R.string.success_check_email), Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Link to change email is sent to " + editTextEmail.getText().toString() +
                                ". Code: " + response.code());
                        et_password.setText("");
                        til_password.setError(null);
                        til_username.setError(null);
                        dialog.dismiss();
                    } else {
                        Log.e(TAG, "Password change failed with okhttp code " + response.code());
                        if (response.code() == 400) {
                            Log.e(TAG, "Email address does not exist online!");
                            editTextEmail.setError(getString(R.string.no_account_with_this_email));
                            positiveButton.setEnabled(false);
                        } else {
                            String error = getString(R.string.failed_code) + response.code();
                            editTextEmail.setError(error);
                            positiveButton.setEnabled(false);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Toast.makeText(getApplicationContext(), "Error: " + t, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Password change error: " + t);
                    dialog.dismiss();
                }
            });
        });

        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    // Set the error if email is mistyped
                    if (isValidEmail(editTextEmail.getText().toString())) {
                        editTextEmail.setError(null);
                    } else {
                        editTextEmail.setError(getString(R.string.email_mistyped));
                    }
                    // Enable/disable the button
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (positiveButton != null) {
                        positiveButton.setEnabled(isValidEmail(editTextEmail.getText().toString()));
                    }
                };
                handler.postDelayed(runnable, 1500);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    /*
         Get the keys for client applications. Separate client key should be given for each Biologer server
         application. This workaround is used to hide secret client_key from the source code. The developers
         should put the key in ~/.gradle/gradle.properties.
    */
    public String getClientKeyForDatabase(String database) {
        return switch (database) {
            case "https://biologer.ba" -> BuildConfig.BiologerBA_Key;
            case "https://biologer.rs" -> BuildConfig.BiologerRS_Key;
            case "https://biologer.me" -> BuildConfig.BiologerME_Key;
            case "https://dev.biologer.org" -> BuildConfig.BiologerDEV_Key;
            case "https://biologer.hr" -> BuildConfig.BiologerHR_Key;
            default -> null;
        };
    }

    public String getClientIdForDatabase(String database) {
        return switch (database) {
            case "https://biologer.ba", "https://biologer.rs", "https://biologer.me",
                 "https://biologer.hr" -> "2";
            case "https://dev.biologer.org" -> "6";
            default -> null;
        };
    }

    private void backPressed() {
        Log.d(TAG, "Back button is pressed in login activity, closing the app!");
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.isEmpty()) {
            finishAffinity();
        } else {
            if (fragments.size() == 1) {
                LinearLayout linearLayout = findViewById(R.id.login_layout);
                linearLayout.setVisibility(View.VISIBLE);
            }
            getSupportFragmentManager().popBackStack();
        }
    }
}
