package org.biologer.biologer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.biologer.biologer.model.LoginResponse;
import org.biologer.biologer.model.RetrofitClient;
import org.biologer.biologer.model.greendao.UserData;
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
    String baKey = BuildConfig.BiologerBA_Key;

    Call <LoginResponse> login;

    //regex za email
    String regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
    // Initialise list for Database selection
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        tv_wrongPass = findViewById(R.id.tv_wrongPass);
        tv_wrongEmail = findViewById(R.id.tv_wrongEmail);
        tv_devDatabase = findViewById(R.id.tv_devDatabase);

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
                R.drawable.flag_bih,
                R.drawable.ic_hammer};
        ImageArrayAdapter ourDatabases = new ImageArrayAdapter(this, Icons, Databases);
        spinner.setAdapter(ourDatabases);
        spinner.setOnItemSelectedListener(new getDatabaseURL());

        // Android 4.4 (KitKat) compatibility: Set button listener programmatically.
        // Login button.
        loginButton = findViewById(R.id.btn_login);
        loginButton.setOnClickListener(v -> onLogin(v));
        // Register link.
        TextView registerTextView = findViewById(R.id.ctv_register);
        registerTextView.setOnClickListener(v -> onRegister(v));
        // Forgot password link.
        TextView forgotPassTextView = findViewById(R.id.ctv_forgotPass);
        forgotPassTextView.setOnClickListener(v -> onForgotPass(v));
    }

    public static class ImageArrayAdapter extends ArrayAdapter<Integer> {
        private Integer[] images;
        private String[] text;
        private Context context;

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

        public void onItemSelected(AdapterView<?> getdatabase, View view, int pos,long id) {
            // Change the preference according to the user selection
            long database_id = getdatabase.getItemIdAtPosition(pos);
            database_name = getdatabase.getItemAtPosition(pos).toString();
            if (database_id == 0) {database_name = "https://biologer.org";}
            if (database_id == 1) {database_name = "https://biologer.hr";}
            if (database_id == 2) {database_name = "https://biologer.ba";}
            if (database_id == 3) {database_name = "https://dev.biologer.org";}

            Log.i(TAG, "Database No. " + database_id + " selected: " + database_name);

            SettingsManager.setDatabaseName(database_name);

            String hint_text = getString(R.string.URL_address) + database_name;
            tv_devDatabase.setText(hint_text);
            tv_devDatabase.setTextColor(getResources().getColor(R.color.colorPrimary));
            if (database_name.equals("https://dev.biologer.org")) {
                tv_devDatabase.setTextColor(getResources().getColor(R.color.warningRed));
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
        }
        if (database_name.equals("https://biologer.ba")) {
            login = RetrofitClient.getService(database_name).login("password", "2", baKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        else {
            login = RetrofitClient.getService(database_name).login("password", "2", rsKey, "*", et_username.getText().toString(), et_password.getText().toString());
        }
        Log.d(TAG, "Logging into " + database_name + " as user " + et_username.getText().toString());

        // Get the response from the call
        login.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> login, @NonNull Response<LoginResponse> response) {
                if(response.isSuccessful()) {
                    LoginResponse login_data = response.body();
                    assert login_data != null;
                    String token = login_data.getAccessToken();
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
            public void onFailure(@NonNull Call<LoginResponse> login, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.cannot_connect_token), Toast.LENGTH_LONG).show();
                loginButton.setEnabled(true);
                progressDialog.dismiss();
                Log.e(TAG, "Cannot get response from the server (token)");
            }
        });

    }

    // Get email and name and store it
    private void fillUserData(){
        Call<UserDataResponse> service = RetrofitClient.getService(database_name).getUserData();
        service.enqueue(new Callback<UserDataResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserDataResponse> service, @NonNull Response<UserDataResponse> response) {
                assert response.body() != null;
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
            public void onFailure(@NonNull Call<UserDataResponse> service, @NonNull Throwable t) {
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
