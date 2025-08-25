package org.biologer.biologer.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputLayout;

import org.biologer.biologer.R;

import java.util.Arrays;
import java.util.List;

public class FragmentRegister1 extends Fragment {

    private static final String TAG = "Biologer.Register";

    Button buttonNext;
    EditText editTextName, editTextSurname, editTextInstitution;
    TextInputLayout nameLayout, surnameLayout;
    TextView textViewDatabaseText, textViewDatabaseInfo;
    ImageView flag, rightArrow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buttonNext = view.findViewById(R.id.buttonRegisterNext1);
        buttonNext.setOnClickListener(this::onNextClicked);
        buttonNext.setEnabled(false);

        editTextName = view.findViewById(R.id.editText_given_name);
        nameLayout = view.findViewById(R.id.editTextLayout_given_name);
        editTextSurname = view.findViewById(R.id.editText_family_name);
        surnameLayout = view.findViewById(R.id.editTextLayout_family_name);
        editTextInstitution = view.findViewById(R.id.editText_institution_name);

        textViewDatabaseText = view.findViewById(R.id.register_database_text);

        textViewDatabaseInfo = view.findViewById(R.id.register_database_description);

        enableButton();

        rightArrow = view.findViewById(R.id.register_right_arrow);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {


                rightArrow.setOnClickListener(view1 -> {
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

                    updateDatabaseView(activity, new_database);
                });

                updateDatabaseView(activity, ((ActivityLogin)activity).database_name);

                editTextName.addTextChangedListener(new TextWatcher() {
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
                            if (!(editTextName.getText().length() > 2)) {
                                nameLayout.setError(getString(R.string.name_is_obligatory));
                            } else {
                                nameLayout.setError(null);
                            }
                            enableButton();
                        };
                        ((ActivityLogin)activity).handler.postDelayed(((ActivityLogin)activity).runnable, 2000);
                    }
                });

                editTextSurname.addTextChangedListener(new TextWatcher() {
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
                            if (!(editTextSurname.getText().length() > 2)) {
                                surnameLayout.setError("Surname is obligatory");
                            } else {
                                surnameLayout.setError(null);
                            }
                            enableButton();
                        };
                        ((ActivityLogin)activity).handler.postDelayed(((ActivityLogin)activity).runnable, 2000);
                    }
                });

            }
        }

    }

    private void updateDatabaseView(Activity activity, String database) {

        textViewDatabaseText.setText(getString(R.string.register_database_info, database));

        flag = activity.findViewById(R.id.database_flag);

        if (database.equals("https://biologer.ba")) {
            flag.setImageResource(R.drawable.flag_bosnia);
            textViewDatabaseInfo.setText(R.string.community_bosnia);
        }
        if (database.equals("https://biologer.rs")) {
            flag.setImageResource(R.drawable.flag_serbia);
            textViewDatabaseInfo.setText(R.string.community_serbia);
        }
        if (database.equals("https://biologer.me")) {
            flag.setImageResource(R.drawable.flag_montenegro);
            textViewDatabaseInfo.setText(R.string.community_montenegro);
        }
        if (database.equals("https://dev.biologer.org")) {
            flag.setImageResource(R.drawable.flag_dev);
            textViewDatabaseInfo.setText(R.string.community_developers);
        }
        if (database.equals("https://biologer.hr")) {
            flag.setImageResource(R.drawable.flag_croatia);
            textViewDatabaseInfo.setText(R.string.community_croatia);
        }
    }

    private void enableButton() {
        buttonNext.setEnabled(editTextSurname.getText().length() > 2 &&
                editTextName.getText().length() > 2);
    }

    private void onNextClicked(View view) {

        ((ActivityLogin) requireActivity()).register_name = editTextName.getText().toString();
        ((ActivityLogin) requireActivity()).register_surname = editTextSurname.getText().toString();
        ((ActivityLogin) requireActivity()).register_institution = editTextInstitution.getText().toString();

        Log.d(TAG, "Registering as " +
                editTextName.getText().toString() + " " +
                editTextSurname.getText().toString() + " (from " +
                editTextInstitution.getText().toString() + ").");

        Fragment fragment = new FragmentRegister2();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.login_container, fragment);
        fragmentTransaction.addToBackStack("Register fragment 2");
        fragmentTransaction.commit();
    }
}
