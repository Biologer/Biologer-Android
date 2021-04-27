package org.biologer.biologer.gui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.RegisterResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.utility.StringUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Register extends FragmentActivity {

    private static final String TAG = "Biologer.Register";

    ImageView flag;
    TextView intro;
    String database;
    MaterialButton button;
    EditText name;
    EditText surname;
    EditText institution;
    MaterialAutoCompleteTextView data_license;
    MaterialAutoCompleteTextView image_license;
    EditText email;
    EditText password;
    EditText password_repeat;
    TextInputLayout emailLayout;
    TextInputLayout nameLayout;
    TextInputLayout surnameLayout;
    TextInputLayout passwordLayout;
    TextInputLayout passwordRepeatLayout;
    String clientKey;
    MaterialCheckBox checkBox;

    ProgressDialog progressDialog;

    Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.fragment_register);

        button = findViewById(R.id.buttonRegister);
        button.setOnClickListener(this::onRegister);
        button.setEnabled(false);

        name = findViewById(R.id.editText_name);
        nameLayout = findViewById(R.id.editTextLayout_name);
        surname = findViewById(R.id.editText_surname);
        surnameLayout = findViewById(R.id.editTextLayout_surname);
        institution = findViewById(R.id.editText_institution);
        email = findViewById(R.id.editText_email);
        emailLayout = findViewById(R.id.editTextLayout_email);
        password = findViewById(R.id.editText_password);
        passwordLayout = findViewById(R.id.editTextLayout_password);
        password_repeat = findViewById(R.id.editText_password2);
        passwordRepeatLayout = findViewById(R.id.editTextLayout_password2);
        checkBox = findViewById(R.id.checkBox_privacy);
        data_license = findViewById(R.id.autoCompleteDataLicense);
        image_license = findViewById(R.id.autoCompleteImageLicense);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            database = bundle.getString("database");
            email.setText(bundle.getString("username"));
            password.setText(bundle.getString("password"));
        } else {
            database = "https://biologer.org";
        }

        String locale_script = Localisation.getLocaleScript();

        progressDialog = new ProgressDialog(Register.this);

        // This is intro text which should contain a link to privacy policy to click on
        String intro_string = getString(R.string.register_intro1)
                + " " + StringUtil.replace(database, "https://", "") + " "
                + getString(R.string.register_intro2) + " "
                + getString(R.string.register_intro3) + " " + getString(R.string.register_intro4) + ".";
        // Hyperlink a part of the text
        SpannableString intro_string_span = new SpannableString(intro_string);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View textView) {
                String url = database + "/" + locale_script + "/pages/privacy-policy";
                Intent defaultBrowser = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
                defaultBrowser.setData(Uri.parse(url));
                startActivity(defaultBrowser);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        int start = intro_string.indexOf(getString(R.string.register_intro3));
        int end = intro_string.lastIndexOf(getString(R.string.register_intro3)) + getString(R.string.register_intro3).length();
        intro_string_span.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        intro = findViewById(R.id.register_message);
        intro.setText(intro_string_span);
        intro.setMovementMethod(LinkMovementMethod.getInstance());
        intro.setHighlightColor(Color.TRANSPARENT);

        String[] dataLicenses = {getString(R.string.license10), getString(R.string.license20), getString(R.string.license30), getString(R.string.license_timed), getString(R.string.license40)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, dataLicenses);
        data_license.setAdapter(adapter);
        data_license.setText(getString(R.string.license10), false);
        image_license.setAdapter(adapter);
        image_license.setText(getString(R.string.license10), false);

        // Change image of the state... don't confuse users
        flag = findViewById(R.id.flag);
        if (database.equals("https://biologer.ba")) {
            flag.setImageResource(R.drawable.flag_bih);
            clientKey = BuildConfig.BiologerBA_Key;
        }
        if (database.equals("https://biologer.org")) {
            flag.setImageResource(R.drawable.flag_serbia);
            clientKey = BuildConfig.BiologerRS_Key;
        }
        if (database.equals("https://dev.biologer.org")) {
            flag.setImageResource(R.drawable.flag_serbia);
            clientKey = BuildConfig.BiologerRS_Key;
        }
        if (database.equals("https://biologer.hr")) {
            flag.setImageResource(R.drawable.flag_croatia);
            clientKey = BuildConfig.BiologerHR_Key;
        }

        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    if (!(name.getText().length() > 2)) {
                        nameLayout.setError(getString(R.string.name_is_obligatory));
                    } else {
                        nameLayout.setError(null);
                    }
                    enableButton();
                };
                handler.postDelayed(runnable, 2000);
            }
        });

        surname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    if (!(surname.getText().length() > 2)) {
                        surnameLayout.setError("Surname is obligatory");
                    } else {
                        surnameLayout.setError(null);
                    }
                    enableButton();
                };
                handler.postDelayed(runnable, 2000);
            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    if (!(password.getText().length() > 8)) {
                        passwordLayout.setError("Password too short");
                        if(password_repeat.length() != 0) {
                            if (password.getText().toString().equals(password_repeat.getText().toString())) {
                                passwordRepeatLayout.setError(null);
                            } else {
                                passwordRepeatLayout.setError("Passwords did not match");
                            }
                        }
                    } else {
                        passwordLayout.setError(null);
                    }
                    enableButton();
                };
                handler.postDelayed(runnable, 2000);
            }
        });

        password_repeat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    if (password.getText().toString().equals(password_repeat.getText().toString())) {
                        passwordRepeatLayout.setError(null);
                    } else {
                        passwordRepeatLayout.setError("Passwords did not match");
                    }
                    enableButton();
                };
                handler.postDelayed(runnable, 2000);
            }
        });

        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);
                runnable = () -> {
                    if (isValidEmail(email.getText().toString())) {
                        emailLayout.setError(null);
                    } else {
                        emailLayout.setError(getString(R.string.invalid_email));
                    }
                    enableButton();
                };
                handler.postDelayed(runnable, 2000);
            }
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> enableButton());
    }

    private void enableButton() {
        button.setEnabled(surname.getText().length() > 2 &&
                name.getText().length() > 2 &&
                password.getText().length() > 8 &&
                password.getText().toString().equals(password_repeat.getText().toString()) &&
                isValidEmail(email.getText().toString()) &&
                checkBox.isChecked());
    }

    private void onRegister(View view) {
        button.setEnabled(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.registering));
        progressDialog.show();

        if (surname.getText().length() > 2 &&
                name.getText().length() > 2 &&
                password.getText().length() > 8 &&
                password.getText().toString().equals(password_repeat.getText().toString()) &&
                isValidEmail(email.getText().toString()) &&
                checkBox.isChecked()) {

            Call<RegisterResponse> registerResponseCall;
            registerResponseCall = RetrofitClient.getService(database).register(
                    2, clientKey,
                    name.getText().toString(), surname.getText().toString(),
                    getDataLicense(), getImageLicense(), institution.getText().toString(),
                    email.getText().toString(), password.getText().toString());
            registerResponseCall.enqueue(new Callback<RegisterResponse>() {

                @Override
                public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                    if (response.code() == 422) {
                        Log.e(TAG, "Given email already exist/wrong name/surname/short password!");
                        emailLayout.setError(getString(R.string.email_already_exist));
                        registerResponseCall.cancel();
                        button.setEnabled(true);
                        progressDialog.dismiss();
                    }
                    if (response.code() == 500) {
                        Log.e(TAG, "Error 500: Server could not send email to the given address.");
                        emailLayout.setError(getString(R.string.email_domain_wrong));
                        registerResponseCall.cancel();
                        button.setEnabled(true);
                        progressDialog.dismiss();
                    }
                    if (response.code() == 404) {
                        Log.e(TAG, "Error 404: Server could not send email to the given address.");
                        emailLayout.setError(getString(R.string.email_domain_wrong));
                        registerResponseCall.cancel();
                        button.setEnabled(true);
                        progressDialog.dismiss();
                    }
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            Log.d(TAG, "Successful registration!");
                            String token = response.body().getAccessToken();
                            String refresh_token = response.body().getRefreshToken();
                            Log.d(TAG, "Access token value is: " + token);
                            Log.d(TAG, "Refresh token value is: " + refresh_token);

                            SettingsManager.setAccessToken(token);
                            SettingsManager.setRefreshToken(refresh_token);
                            SettingsManager.setMailConfirmed(false);
                            long expire = response.body().getExpiresIn();
                            long expire_date = (System.currentTimeMillis()/1000) + expire;
                            SettingsManager.setTokenExpire(String.valueOf(expire_date));
                            Log.d(TAG, "Token will expire on timestamp: " + expire_date);

                            progressDialog.dismiss();
                            saveUserData();
                        } else {
                            Log.e(TAG, "Register response is null!");
                        }
                    } else {
                        Log.e(TAG, "Something went wrong, response not successful!");
                    }
                    button.setEnabled(true);
                }

                @Override
                public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Network failure!" + t);
                    registerResponseCall.cancel();
                    progressDialog.dismiss();
                    button.setEnabled(true);
                }
            });
        }
    }

    private int getDataLicense() {
        if (data_license.getText().toString().equals(getString(R.string.license10))) {
            return 10;
        }
        if (data_license.getText().toString().equals(getString(R.string.license20))) {
            return 20;
        }
        if (data_license.getText().toString().equals(getString(R.string.license30))) {
            return 30;
        }
        if (data_license.getText().toString().equals(getString(R.string.license_timed))) {
            return 35;
        }
        if (data_license.getText().toString().equals(getString(R.string.license40))) {
            return 40;
        }
        return 10;
    }

    private int getImageLicense() {
        if (image_license.getText().toString().equals(getString(R.string.license10))) {
            return 10;
        }
        if (image_license.getText().toString().equals(getString(R.string.license20))) {
            return 20;
        }
        if (image_license.getText().toString().equals(getString(R.string.license30))) {
            return 30;
        }
        if (image_license.getText().toString().equals(getString(R.string.license_timed))) {
            return 35;
        }
        if (image_license.getText().toString().equals(getString(R.string.license40))) {
            return 40;
        }
        return 10;
    }

    private void saveUserData() {
        String full_name = name.getText().toString() + " " + surname.getText().toString();
        UserData userData = new UserData(null, full_name, email.getText().toString(), getDataLicense(), getImageLicense());
        App.get().getDaoSession().getUserDataDao().insertOrReplace(userData);
        displaySuccess();
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private void displaySuccess() {
        // Show the message if registration was successful
        final AlertDialog.Builder builder_taxon = new AlertDialog.Builder(Register.this);
        builder_taxon.setMessage(getString(R.string.register_succesfull))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.OK), (dialog, id) -> {
                    Intent intent = new Intent(Register.this, LoginActivity.class);
                    intent.putExtra("email", email.getText().toString());
                    intent.putExtra("password", password.getText().toString());
                    startActivity(intent);
                    dialog.dismiss();
                });
        final AlertDialog alert = builder_taxon.create();
        alert.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent loginIntent = new Intent(Register.this, LoginActivity.class);
        startActivity(loginIntent);
    }
}
