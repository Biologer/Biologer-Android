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

import java.util.Objects;

public class RegisterFragment1 extends Fragment {

    private static final String TAG = "Biologer.Register";

    Button buttonNext;
    EditText editTextName, editTextSurname, editTextInstitution;
    TextInputLayout nameLayout, surnameLayout;
    TextView textViewDatabase;
    ImageView flag;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register1, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view != null) {

            Activity activity = getActivity();
            if (activity != null) {

                buttonNext = activity.findViewById(R.id.buttonRegisterNext1);
                buttonNext.setOnClickListener(this::onNextClicked);
                buttonNext.setEnabled(false);

                editTextName = activity.findViewById(R.id.editText_given_name);
                nameLayout = activity.findViewById(R.id.editTextLayout_given_name);
                editTextSurname = activity.findViewById(R.id.editText_family_name);
                surnameLayout = activity.findViewById(R.id.editTextLayout_family_name);
                editTextInstitution = activity.findViewById(R.id.editText_institution_name);

                textViewDatabase = activity.findViewById(R.id.register_database_text);
                String database = ((LoginActivity)activity).database_name;
                textViewDatabase.setText(getString(R.string.register_database_info, database));

                enableButton();

                // Change image of the state... don't confuse users
                flag = activity.findViewById(R.id.database_flag);
                if (database.equals("https://biologer.ba")) {
                    flag.setImageResource(R.drawable.flag_bosnia);
                }
                if (database.equals("https://biologer.rs")) {
                    flag.setImageResource(R.drawable.flag_serbia);
                }
                if (database.equals("https://birdloger.biologer.org")) {
                    flag.setImageResource(R.drawable.flag_serbia);
                }
                if (database.equals("https://dev.biologer.org")) {
                    flag.setImageResource(R.drawable.flag_serbia);
                }
                if (database.equals("https://biologer.hr")) {
                    flag.setImageResource(R.drawable.flag_croatia);
                }

                editTextName.addTextChangedListener(new TextWatcher() {
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
                            if (!(editTextName.getText().length() > 2)) {
                                nameLayout.setError(getString(R.string.name_is_obligatory));
                            } else {
                                nameLayout.setError(null);
                            }
                            enableButton();
                        };
                        ((LoginActivity)activity).handler.postDelayed(((LoginActivity)activity).runnable, 2000);
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
                        ((LoginActivity)activity).handler.removeCallbacks(((LoginActivity)activity).runnable);
                        ((LoginActivity)activity).runnable = () -> {
                            if (!(editTextSurname.getText().length() > 2)) {
                                surnameLayout.setError("Surname is obligatory");
                            } else {
                                surnameLayout.setError(null);
                            }
                            enableButton();
                        };
                        ((LoginActivity)activity).handler.postDelayed(((LoginActivity)activity).runnable, 2000);
                    }
                });

            }
        }

    }

    private void enableButton() {
        buttonNext.setEnabled(editTextSurname.getText().length() > 2 &&
                editTextName.getText().length() > 2);
    }

    private void onNextClicked(View view) {

            ((LoginActivity) requireActivity()).register_name = editTextName.getText().toString();
            ((LoginActivity) requireActivity()).register_surname = editTextSurname.getText().toString();
            ((LoginActivity) requireActivity()).register_institution = editTextInstitution.getText().toString();

            Log.d(TAG, "Registering as " +
                    editTextName.getText().toString() + " " +
                    editTextSurname.getText().toString() + " (from " +
                    editTextInstitution.getText().toString() + ").");

            Fragment fragment = new RegisterFragment2();
            FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.login_frame, fragment);
            fragmentTransaction.addToBackStack("Register fragment 2");
            fragmentTransaction.commit();
    }
}
