package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.R;

import java.util.Objects;

public class RegisterFragment2 extends Fragment {

    private static final String TAG = "Biologer.Register";

    Button buttonNext;
    EditText editTextEmail, editTextPassword, editTextPasswordRepeat;
    TextInputLayout emailLayout, passwordLayout, passwordRepeatLayout;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register2, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {

                buttonNext = activity.findViewById(R.id.buttonRegisterNext2);
                buttonNext.setOnClickListener(this::onNextClicked);
                buttonNext.setEnabled(false);

                editTextEmail = activity.findViewById(R.id.editText_register_email);
                emailLayout = activity.findViewById(R.id.editTextLayout_register_email);
                editTextPassword = activity.findViewById(R.id.editText_register_password);
                passwordLayout = activity.findViewById(R.id.editTextLayout_register_password);
                editTextPasswordRepeat = activity.findViewById(R.id.editText_register_password_confirm);
                passwordRepeatLayout = activity.findViewById(R.id.editTextLayout_register_password_confirm);

                editTextEmail.setText(((LoginActivity)activity).et_username.getText().toString());
                editTextPassword.setText(((LoginActivity)activity).et_password.getText().toString());

                enableButton();

                editTextPassword.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((LoginActivity)activity).handler.removeCallbacks(((LoginActivity)activity).runnable);
                        ((LoginActivity)activity).runnable = () -> {
                            if (!(editTextPassword.getText().length() > 8)) {
                                passwordLayout.setError("Password too short");
                            } else {
                                passwordLayout.setError(null);

                                // If repeated password is not empty, check for password matching
                                if (editTextPasswordRepeat.getText().length() != 0) {
                                    if (editTextPassword.getText().toString().equals(editTextPasswordRepeat.getText().toString())) {
                                        passwordRepeatLayout.setError(null);
                                    } else {
                                        passwordRepeatLayout.setError("Passwords did not match");
                                    }
                                }

                            }

                            enableButton();
                        };
                        ((LoginActivity)activity).handler.postDelayed(((LoginActivity)activity).runnable, 2000);
                    }
                });

                editTextPasswordRepeat.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((LoginActivity)activity).handler.removeCallbacks(((LoginActivity)activity).runnable);
                        ((LoginActivity)activity).runnable = () -> {
                            if (editTextPassword.getText().toString().equals(editTextPasswordRepeat.getText().toString())) {
                                passwordRepeatLayout.setError(null);
                            } else {
                                passwordRepeatLayout.setError("Passwords did not match");
                            }
                            enableButton();
                        };
                        ((LoginActivity)activity).handler.postDelayed(((LoginActivity)activity).runnable, 2000);
                    }
                });

                editTextEmail.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((LoginActivity)activity).handler.removeCallbacks(((LoginActivity)activity).runnable);
                        ((LoginActivity)activity).runnable = () -> {
                            if (isValidEmail(editTextEmail.getText().toString())) {
                                emailLayout.setError(null);
                            } else {
                                emailLayout.setError(getString(R.string.invalid_email));
                            }
                            enableButton();
                        };
                        ((LoginActivity)activity).handler.postDelayed(((LoginActivity)activity).runnable, 2000);
                    }
                });

            }
            
        }
        
    }

    private void onNextClicked(View view) {
            ((LoginActivity) requireActivity()).et_username.setText(editTextEmail.getText().toString());
            ((LoginActivity) requireActivity()).et_password.setText(editTextPassword.getText().toString());

            Log.d(TAG, "Registering with email address: " + editTextEmail.getText().toString());

            Fragment fragment = new RegisterFragment3();
            FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.login_frame, fragment);
            fragmentTransaction.addToBackStack("Register fragment 3");
            fragmentTransaction.commit();

    }

    private void enableButton() {
        buttonNext.setEnabled(
                editTextPassword.getText().length() > 8 &&
                editTextPassword.getText().toString().equals(editTextPasswordRepeat.getText().toString()) &&
                isValidEmail(editTextEmail.getText().toString()));
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }
}
