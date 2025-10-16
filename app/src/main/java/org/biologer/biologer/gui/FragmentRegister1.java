package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.biologer.biologer.R;
import org.biologer.biologer.databinding.FragmentRegister1Binding;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FragmentRegister1 extends Fragment {

    private static final String TAG = "Biologer.Register";
    private FragmentRegister1Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegister1Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonNext.setOnClickListener(this::onNextClicked);
        binding.buttonNext.setEnabled(false);
        enableButton();
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {

                binding.imageVireRightArrow.setOnClickListener(view1 -> {
                    String current_database = ((ActivityLogin) requireActivity()).database_name;
                    String[] databases = ActivityLogin.allDatabases;
                    List<String> databasesList = Arrays.asList(databases);
                    int index = databasesList.indexOf(current_database);
                    index++;
                    if (databasesList.size() == index) {
                        index = 0;
                    }
                    String new_database = databases[index];
                    Log.i(TAG, "User selected " + new_database + " as a database.");

                    ((ActivityLogin) requireActivity()).database_name = new_database;
                    int spinnerID = ActivityLogin.getSpinnerIdFromUrl(new_database);
                    ((ActivityLogin) requireActivity()).spinner.setSelection(spinnerID);

                    updateDatabaseView(new_database);
                });

                updateDatabaseView(((ActivityLogin)activity).database_name);

                binding.editTextName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((ActivityLogin)activity).handler.removeCallbacks(((ActivityLogin)activity).runnable);
                        ((ActivityLogin)activity).runnable = () -> {
                            if (!(Objects.requireNonNull(binding.editTextName.getText()).length() > 2)) {
                                binding.textInputLayoutName.setError(getString(R.string.name_is_obligatory));
                            } else {
                                binding.textInputLayoutName.setError(null);
                            }
                            enableButton();
                        };
                        ((ActivityLogin)activity).handler.postDelayed(((ActivityLogin)activity).runnable, 2000);
                    }
                });

                binding.editTextSurname.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        ((ActivityLogin)activity).handler.removeCallbacks(((ActivityLogin)activity).runnable);
                        ((ActivityLogin)activity).runnable = () -> {
                            if (!(Objects.requireNonNull(binding.editTextSurname.getText()).length() > 2)) {
                                binding.textInputLayoutSurname.setError("Surname is obligatory");
                            } else {
                                binding.textInputLayoutSurname.setError(null);
                            }
                            enableButton();
                        };
                        ((ActivityLogin)activity).handler.postDelayed(((ActivityLogin)activity).runnable, 2000);
                    }
                });

            }
        }

    }

    private void updateDatabaseView(String database) {

        binding.textViewDatabase.setText(getString(R.string.register_database_info, database));

        if (database.equals("https://biologer.ba")) {
            binding.imageViewFlag.setImageResource(R.drawable.flag_bosnia);
            binding.textViewDatabaseDesc.setText(R.string.community_bosnia);
        }
        if (database.equals("https://biologer.rs")) {
            binding.imageViewFlag.setImageResource(R.drawable.flag_serbia);
            binding.textViewDatabaseDesc.setText(R.string.community_serbia);
        }
        if (database.equals("https://biologer.me")) {
            binding.imageViewFlag.setImageResource(R.drawable.flag_montenegro);
            binding.textViewDatabaseDesc.setText(R.string.community_montenegro);
        }
        if (database.equals("https://dev.biologer.org")) {
            binding.imageViewFlag.setImageResource(R.drawable.flag_dev);
            binding.textViewDatabaseDesc.setText(R.string.community_developers);
        }
        if (database.equals("https://biologer.hr")) {
            binding.imageViewFlag.setImageResource(R.drawable.flag_croatia);
            binding.textViewDatabaseDesc.setText(R.string.community_croatia);
        }
    }

    private void enableButton() {
        binding.buttonNext.setEnabled(Objects.requireNonNull(binding.editTextSurname.getText()).length() > 2 &&
                Objects.requireNonNull(binding.editTextName.getText()).length() > 2);
    }

    private void onNextClicked(View view) {

        ((ActivityLogin) requireActivity()).register_name = Objects.requireNonNull(binding.editTextName.getText()).toString();
        ((ActivityLogin) requireActivity()).register_surname = Objects.requireNonNull(binding.editTextSurname.getText()).toString();
        ((ActivityLogin) requireActivity()).register_institution = Objects.requireNonNull(binding.editTextInstitution.getText()).toString();

        Log.d(TAG, "Registering as " +
                binding.editTextName.getText().toString() + " " +
                binding.editTextSurname.getText().toString() + " (from " +
                binding.editTextInstitution.getText().toString() + ").");

        Fragment fragment = new FragmentRegister2();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.login_container, fragment);
        fragmentTransaction.addToBackStack("Register fragment 2");
        fragmentTransaction.commit();
    }
}
