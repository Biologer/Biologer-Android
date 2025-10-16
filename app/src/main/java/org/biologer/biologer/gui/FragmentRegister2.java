package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.biologer.biologer.R;
import org.biologer.biologer.databinding.FragmentRegister2Binding;

import java.util.Objects;

public class FragmentRegister2 extends Fragment {

    private static final String TAG = "Biologer.Register";
    private FragmentRegister2Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegister2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonNext.setOnClickListener(this::onNextClicked);
        binding.buttonNext.setEnabled(false);

        Activity activity = getActivity();
        if (activity != null) {
            binding.textInputEditTextEmail.setText(((ActivityLogin) activity).editTextUsername.getText().toString());
            binding.textInputEditTextPassword.setText(((ActivityLogin) activity).editTextPassword.getText().toString());

            ImageView imageViewShowPassword = activity.findViewById(R.id.show_password_icon_type);
            imageViewShowPassword.setOnClickListener(view1 -> {
                int inputType = binding.textInputEditTextPassword.getInputType();
                if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    imageViewShowPassword.setImageResource(R.drawable.eye_open);
                    binding.textInputEditTextPassword.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imageViewShowPassword.setContentDescription(getString(R.string.hide_password));
                } else {
                    imageViewShowPassword.setImageResource(R.drawable.eye_closed);
                    binding.textInputEditTextPassword.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imageViewShowPassword.setContentDescription(getString(R.string.show_password));
                }
                binding.textInputEditTextPassword.setSelection(
                        Objects.requireNonNull(binding.textInputEditTextPassword.getText()).length());
            });

            ImageView imageViewShowPasswordRepeat = activity.findViewById(R.id.show_password_icon_retype);
            imageViewShowPasswordRepeat.setOnClickListener(view2 -> {
                int inputType = binding.textInputEditTextPasswordRepeat.getInputType();
                if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    imageViewShowPasswordRepeat.setImageResource(R.drawable.eye_open);
                    binding.textInputEditTextPasswordRepeat.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imageViewShowPasswordRepeat.setContentDescription(getString(R.string.hide_password));
                } else {
                    imageViewShowPasswordRepeat.setImageResource(R.drawable.eye_closed);
                    binding.textInputEditTextPasswordRepeat.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imageViewShowPasswordRepeat.setContentDescription(getString(R.string.show_password));
                }
                binding.textInputEditTextPasswordRepeat.setSelection(
                        Objects.requireNonNull(binding.textInputEditTextPasswordRepeat.getText()).length());
            });
        }

        validateFields();

    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {
            enableButton();
            Activity activity = getActivity();
            if (activity != null) {

                binding.textInputEditTextEmail.setText(((ActivityLogin) activity).editTextUsername.getText().toString());
                binding.textInputEditTextPassword.setText(((ActivityLogin) activity).editTextPassword.getText().toString());
                binding.textInputEditTextPassword.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((ActivityLogin) activity).handler.removeCallbacks(((ActivityLogin) activity).runnable);
                        ((ActivityLogin) activity).runnable = () -> {
                            if (!(Objects.requireNonNull(binding.textInputEditTextPassword.getText()).length() > 8)) {
                                binding.textInputLayoutPassword.setError(getString(R.string.pass_short));
                            } else {
                                binding.textInputLayoutPassword.setError(null);

                                // If repeated password is not empty, check for password matching
                                if (Objects.requireNonNull(binding.textInputEditTextPasswordRepeat.getText()).length() != 0) {
                                    if (binding.textInputEditTextPassword.getText().toString().equals(binding.textInputEditTextPasswordRepeat.getText().toString())) {
                                        binding.textInputLayoutPasswordRepeat.setError(null);
                                    } else {
                                        binding.textInputLayoutPasswordRepeat.setError(getString(R.string.pass_not_match));
                                    }
                                }

                            }

                            enableButton();
                        };
                        ((ActivityLogin) activity).handler.postDelayed(((ActivityLogin) activity).runnable, 2000);
                    }
                });

                binding.textInputEditTextPasswordRepeat.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((ActivityLogin) activity).handler.removeCallbacks(((ActivityLogin) activity).runnable);
                        ((ActivityLogin) activity).runnable = () -> {
                            if (Objects.requireNonNull(binding.textInputEditTextPassword.getText()).toString()
                                    .equals(Objects.requireNonNull(binding.textInputEditTextPasswordRepeat.getText()).toString())) {
                                binding.textInputLayoutPasswordRepeat.setError(null);
                            } else {
                                binding.textInputLayoutPasswordRepeat.setError(getString(R.string.pass_not_match));
                            }

                            if (!(binding.textInputEditTextPassword.getText().length() > 8)) {
                                binding.textInputLayoutPassword.setError(getString(R.string.pass_short));
                            } else {
                                binding.textInputLayoutPassword.setError(null);
                            }

                            enableButton();
                        };
                        ((ActivityLogin) activity).handler.postDelayed(((ActivityLogin) activity).runnable, 2000);
                    }
                });

                binding.textInputEditTextEmail.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((ActivityLogin) activity).handler.removeCallbacks(((ActivityLogin) activity).runnable);
                        ((ActivityLogin) activity).runnable = () -> {
                            if (isValidEmail(Objects.requireNonNull(binding.textInputEditTextEmail.getText()).toString())) {
                                binding.textInputLayoutEmail.setError(null);
                            } else {
                                binding.textInputLayoutEmail.setError(activity.getString(R.string.invalid_email));
                            }
                            enableButton();
                        };
                        ((ActivityLogin) activity).handler.postDelayed(((ActivityLogin) activity).runnable, 2000);
                    }
                });
            }
        }

    }


    private void onNextClicked(View view) {
        ((ActivityLogin) requireActivity()).editTextUsername.setText(Objects.requireNonNull(binding.textInputEditTextEmail.getText()).toString());
        ((ActivityLogin) requireActivity()).editTextPassword.setText(Objects.requireNonNull(binding.textInputEditTextPassword.getText()).toString());

        Log.d(TAG, "Registering with email address: " + binding.textInputEditTextEmail.getText().toString());

        Fragment fragment = new FragmentRegister3();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.login_container, fragment);
        fragmentTransaction.addToBackStack("Register fragment 3");
        fragmentTransaction.commit();

    }

    private void validateFields() {
        String email = Objects.requireNonNull(binding.textInputEditTextEmail.getText()).toString();
        String password = Objects.requireNonNull(binding.textInputEditTextPassword.getText()).toString();
        String passwordRepeat = Objects.requireNonNull(binding.textInputEditTextPasswordRepeat.getText()).toString();

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.setError(null);
        } else if (isValidEmail(email)) {
            binding.textInputLayoutEmail.setError(null);
        } else {
            binding.textInputLayoutEmail.setError(getString(R.string.invalid_email));
        }

        if (password.isEmpty() || password.length() > 8) {
            binding.textInputLayoutPassword.setError(null);
        } else {
            binding.textInputLayoutPassword.setError(getString(R.string.pass_short));
        }

        if (!password.equals(passwordRepeat) && !passwordRepeat.isEmpty()) {
            binding.textInputLayoutPasswordRepeat.setError(getString(R.string.pass_not_match));
        }

        enableButton();
    }

    private void enableButton() {
        binding.buttonNext.setEnabled(
                Objects.requireNonNull(binding.textInputEditTextPassword.getText()).length() > 8 &&
                        binding.textInputEditTextPassword.getText().toString()
                                .equals(Objects.requireNonNull(binding.textInputEditTextPasswordRepeat.getText()).toString()) &&
                        isValidEmail(Objects.requireNonNull(binding.textInputEditTextEmail.getText()).toString()));
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }
}
