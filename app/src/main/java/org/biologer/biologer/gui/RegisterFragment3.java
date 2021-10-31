package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.RegisterResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment3 extends Fragment {

    private static final String TAG = "Biologer.Register";

    Button buttonRegister;
    MaterialCheckBox checkBox;
    MaterialAutoCompleteTextView dataLicense, imageLicense;
    ProgressBar progressBar;
    TextView textViewError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register3, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {

                buttonRegister = activity.findViewById(R.id.buttonRegisterUser);
                buttonRegister.setOnClickListener(this::onRegisterClicked);
                buttonRegister.setEnabled(false);

                checkBox = activity.findViewById(R.id.checkBox_privacy_policy);
                dataLicense = activity.findViewById(R.id.autoComplete_register_dataLicense);
                imageLicense = activity.findViewById(R.id.autoComplete_register_imageLicense);
                textViewError = activity.findViewById(R.id.register_error_into_text);

                enableButton();

                String[] dataLicenses = {getString(R.string.license10), getString(R.string.license20), getString(R.string.license30), getString(R.string.license_timed), getString(R.string.license40)};
                String[] imageLicenses = {getString(R.string.license_image_10), getString(R.string.license_image_20), getString(R.string.license_image_30), getString(R.string.license_image_40)};
                ArrayAdapter<String> adapterDataLicense = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, dataLicenses);
                ArrayAdapter<String> adapterImageLicense = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, imageLicenses);
                dataLicense.setAdapter(adapterDataLicense);
                dataLicense.setText(getString(R.string.license10), false);
                imageLicense.setAdapter(adapterImageLicense);
                imageLicense.setText(getString(R.string.license_image_10), false);

                progressBar = activity.findViewById(R.id.progressBar_register);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> enableButton());
            }
        }
    }

    private void enableButton() {
        buttonRegister.setEnabled(checkBox.isChecked());
    }

    private int getDataLicense() {
        if (dataLicense.getText().toString().equals(getString(R.string.license10))) {
            return 10;
        }
        if (dataLicense.getText().toString().equals(getString(R.string.license20))) {
            return 20;
        }
        if (dataLicense.getText().toString().equals(getString(R.string.license30))) {
            return 30;
        }
        if (dataLicense.getText().toString().equals(getString(R.string.license_timed))) {
            return 35;
        }
        if (dataLicense.getText().toString().equals(getString(R.string.license40))) {
            return 40;
        }
        return 10;
    }

    private int getImageLicense() {
        if (imageLicense.getText().toString().equals(getString(R.string.license_image_10))) {
            return 10;
        }
        if (imageLicense.getText().toString().equals(getString(R.string.license_image_20))) {
            return 20;
        }
        if (imageLicense.getText().toString().equals(getString(R.string.license_image_30))) {
            return 30;
        }
        if (imageLicense.getText().toString().equals(getString(R.string.license_image_40))) {
            return 40;
        }
        return 10;
    }

    private void onRegisterClicked(View view) {
        buttonRegister.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String database = ((LoginActivity) requireActivity()).database_name;
        String clientKey = ((LoginActivity) requireActivity()).getClientKeyForDatabase(database);
        String clientId = ((LoginActivity) requireActivity()).getClientIdForDatabase(database);

        Call<RegisterResponse> registerResponseCall;
        registerResponseCall = RetrofitClient.getService(database).register(
                Integer.parseInt(clientId), clientKey,
                ((LoginActivity) requireActivity()).register_name,
                ((LoginActivity) requireActivity()).register_surname,
                getDataLicense(), getImageLicense(),
                ((LoginActivity) requireActivity()).register_institution,
                ((LoginActivity) requireActivity()).et_username.getText().toString(),
                ((LoginActivity) requireActivity()).et_password.getText().toString());
        registerResponseCall.enqueue(new Callback<RegisterResponse>() {

            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.code() == 422) {
                    Log.e(TAG, "Given email already exist/wrong name/surname/short password!");
                    textViewError.setText(getString(R.string.email_already_exist));
                    textViewError.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    registerResponseCall.cancel();
                    buttonRegister.setEnabled(true);
                }
                if (response.code() == 500) {
                    Log.e(TAG, "Error 500: Server could not send email to the given address.");
                    textViewError.setText(getString(R.string.email_domain_wrong));
                    textViewError.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    registerResponseCall.cancel();
                    buttonRegister.setEnabled(true);
                }
                if (response.code() == 404) {
                    Log.e(TAG, "Error 404: Server could not send email to the given address.");
                    textViewError.setText(getString(R.string.email_domain_wrong));
                    textViewError.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    registerResponseCall.cancel();
                    buttonRegister.setEnabled(true);
                }
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Log.d(TAG, "Successful registration!");

                        String token = response.body().getAccessToken();
                        String refresh_token = response.body().getRefreshToken();

                        SettingsManager.setAccessToken(token);
                        SettingsManager.setRefreshToken(refresh_token);
                        SettingsManager.setMailConfirmed(false);
                        long expire = response.body().getExpiresIn();
                        long expire_date = (System.currentTimeMillis()/1000) + expire;
                        SettingsManager.setTokenExpire(String.valueOf(expire_date));
                        Log.d(TAG, "Token will expire on timestamp: " + expire_date);

                        progressBar.setVisibility(View.GONE);
                        saveUserData();
                    } else {
                        Log.e(TAG, "Register response is null!");
                    }
                } else {
                    Log.e(TAG, "Something went wrong, response not successful!");
                }
                buttonRegister.setEnabled(true);
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure!" + t);
                registerResponseCall.cancel();
                progressBar.setVisibility(View.GONE);
                buttonRegister.setEnabled(true);
            }
        });
    }

    private void saveUserData() {
        String full_name =  ((LoginActivity) requireActivity()).register_name + " " +  ((LoginActivity) requireActivity()).register_surname;
        String email = ((LoginActivity) requireActivity()).et_username.getText().toString();
        UserData userData = new UserData(null, full_name, email, getDataLicense(), getImageLicense());
        App.get().getDaoSession().getUserDataDao().insertOrReplace(userData);
        displaySuccess();
    }

    private void displaySuccess() {
        // Show the message if registration was successful
        final AlertDialog.Builder builder_taxon = new AlertDialog.Builder(requireContext());
        builder_taxon.setMessage(getString(R.string.register_succesfull))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.OK), (dialog, id) -> {
                    List<Fragment> fragments = requireActivity().getSupportFragmentManager().getFragments();
                        if (fragments.size() != 0) {
                            for (Fragment fragment : fragments) {
                                requireActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                            }
                        }
                });
        final AlertDialog alert = builder_taxon.create();
        alert.show();
    }

}
