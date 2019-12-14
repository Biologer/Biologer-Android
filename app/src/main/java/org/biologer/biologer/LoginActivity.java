package org.biologer.biologer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.biologer.biologer.model.LoginResponse;
import org.biologer.biologer.model.RetrofitClient;
import org.biologer.biologer.model.UserData;
import org.biologer.biologer.model.network.UserDataResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Login";

    EditText et_username;
    EditText et_password;
    TextView tv_wrongPass;
    TextView tv_wrongEmail;
    TextView tv_devDatabase;
    Button loginButton;
    // Get the value for KEY.DATABASE_NAME
    String key_database_name = SettingsManager.getDatabaseName();
    String database_name = key_database_name;

    /*
     Get the keys for client applications. Separate client key should be given for each Biologer server
     application. This workaround is used to hide secret client_key from the source code. The developers
     should put the key in ~/.gradle/gradle.properties.
      */
    String rsKey = BuildConfig.BiologerRS_Key;
    String hrKey = BuildConfig.BiologerHR_Key;

    Call <LoginResponse> login;

    //regex za email
    String regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
    // Initialise list for Database selection
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_username = (EditText) findViewById(R.id.et_username);
        et_password = (EditText) findViewById(R.id.et_password);
        tv_wrongPass = (TextView) findViewById(R.id.tv_wrongPass);
        tv_wrongEmail = (TextView) findViewById(R.id.tv_wrongEmail);
        tv_devDatabase = (TextView) findViewById(R.id.tv_devDatabase);

        // Fill in the data for database list
        spinner = (Spinner) findViewById(R.id.spinner_databases);
        ArrayAdapter<CharSequence> ourDatabases = ArrayAdapter.createFromResource(this, R.array.databases, android.R.layout.simple_spinner_item);
        ourDatabases.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(ourDatabases);
        int spinnerPosition = ourDatabases.getPosition(key_database_name);
        spinner.setSelection(spinnerPosition);

        // If item is chosen from the database list.
        spinner = (Spinner) findViewById(R.id.spinner_databases);
        spinner.setOnItemSelectedListener(new getDatabaseURL());

        // Android 4.4 (KitKat) compatibility: Set button listener programmatically.
        // Login button.
        loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogin(v);
            }
        });
        // Register link.
        TextView registerTextView = (TextView) findViewById(R.id.ctv_register);
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegister(v);
            }
        });
        // Forgot password link.
        TextView forgotPassTextView = (TextView) findViewById(R.id.ctv_forgotPass);
        forgotPassTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onForgotPass(v);
            }
        });
    }

    // Activity starting after user selects a Database from the list
    public class getDatabaseURL implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> getdatabase, View view, int pos,long id) {
            // Change the preference according to the user selection
            database_name = getdatabase.getItemAtPosition(pos).toString();
            SettingsManager.setDatabaseName(database_name);

            tv_devDatabase.setText("");
            if (SettingsManager.getDatabaseName().equals("https://dev.biologer.org")) {
                tv_devDatabase.setText(R.string.devDatabase);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }

    }

    // Run when user click the login button
    public void onLogin(View view) {
        loginButton.setEnabled(false);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(et_username.getText().toString());

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.logging_in));
        progressDialog.show();

        // On new login clear the previous error messages.
        tv_wrongPass.setText("");
        tv_wrongEmail.setText("");

        // Display error messages if email/password is mistyped.
        if (!(matcher.matches()))
        {
            tv_wrongEmail.setText(R.string.valid_email);
            loginButton.setEnabled(true);
            progressDialog.dismiss();
            return;
        }
        if (!(et_password.getText().length() > 3))
        {
            tv_wrongPass.setText(R.string.valid_pass);
            loginButton.setEnabled(true);
            progressDialog.dismiss();
            return;
        }

        // Change the call according to the database selected
        if (database_name.equals("https://biologer.hr")) {
            login = RetrofitClient.getService(database_name).login("password", "2", hrKey, "*", et_username.getText().toString(), et_password.getText().toString());
        } else {
            login = RetrofitClient.getService(database_name).login("password", "2", rsKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        Log.d(TAG, "Logging into " + database_name + " as user " + et_username.getText().toString());

        // Get the response from the call
        login.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> login, Response<LoginResponse> response) {
                if(response.isSuccessful()) {
                    String token = response.body().getAccessToken();
                    Log.d(TAG, "Token value is: " + token);
                    SettingsManager.setToken(token);
                    fillUserData();
                }
                else {
                    tv_wrongPass.setText(R.string.wrong_creds);
                    loginButton.setEnabled(true);
                    progressDialog.dismiss();
                    Log.d(TAG, String.valueOf(response.body()));
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> login, Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.cannot_connect_token), Toast.LENGTH_LONG).show();
                loginButton.setEnabled(true);
                progressDialog.dismiss();
                Log.e(TAG, "Cannot get response from the server (token)");
            }
        });

    }

    // Get email and name and store it
    private void fillUserData(){
        Call<UserDataResponse> serv = RetrofitClient.getService(database_name).getUserData();
        serv.enqueue(new Callback<UserDataResponse>() {
            @Override
            public void onResponse(Call<UserDataResponse> serv, Response<UserDataResponse> response) {
                String email = response.body().getData().getEmail();
                String name = response.body().getData().getFullName();
                int data_license = response.body().getData().getSettings().getDataLicense();
                int image_license = response.body().getData().getSettings().getImageLicense();
                UserData uData = new UserData(null, email, name, data_license, image_license);
                App.get().getDaoSession().getUserDataDao().insertOrReplace(uData);
                Intent intent = new Intent(LoginActivity.this, LandingActivity.class);
                startActivity(intent);
            }
            @Override
            public void onFailure(Call<UserDataResponse> serv, Throwable t) {
                Toast.makeText(LoginActivity.this, "Cannot get response from the server (userdata)", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Cannot get response from the server (userdata)");
            }
        });
    }

    public void onRegister(View view) {
        String url_register = SettingsManager.getDatabaseName() + "/register";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_register));
        startActivity(browserIntent);
    }

    public void onForgotPass(View view) {
        String url_reset_pass = SettingsManager.getDatabaseName() + "/password/reset";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_reset_pass));
        startActivity(browserIntent);
    }

}
