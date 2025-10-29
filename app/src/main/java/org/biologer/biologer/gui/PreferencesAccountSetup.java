package org.biologer.biologer.gui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.sql.UserDb;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferencesAccountSetup extends PreferenceFragmentCompat {
    private static final String TAG = "Biologer.PreferencesA";
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable runnable;


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This is a workaround in order to change background color of the fragment
        getListView().setBackgroundResource(R.color.fragment_background);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_account);
        this.getPreferenceScreen();
        Log.d(TAG, "Starting fragment for account setup preferences.");

        Preference changeEmail = findPreference("change_email");
        if (changeEmail != null) {
            changeEmail.setOnPreferenceClickListener(preference -> {

                LinearLayout container = new LinearLayout(requireContext());
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                container.setOrientation(LinearLayout.VERTICAL);
                container.setLayoutParams(containerParams);
                container.setPadding(48, 12, 24, 24);

                final EditText email = new EditText(requireContext());
                email.setLayoutParams(containerParams);
                email.setHint(R.string.type_email);

                final EditText emailRetype = new EditText(requireContext());
                emailRetype.setLayoutParams(containerParams);
                emailRetype.setHint(R.string.retype_email);

                container.addView(email);
                container.addView(emailRetype);

                // Build the alert dialog
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

                UserDb userData = getUserData();
                String username = userData.getUsername();
                String email_address = userData.getEmail();

                builder.setTitle(R.string.change_email)
                        .setMessage(getString(R.string.change_email_msg1) + " " +
                                username + " (" + email_address + "). " +
                                getString(R.string.change_email_msg2))
                        .setView(container)
                        .setPositiveButton(R.string.change_email_button, (dialog, which) -> {
                            String new_email = email.getText().toString();
                            changeEmail(new_email);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                final android.app.AlertDialog alert = builder.create();
                alert.show();

                Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setEnabled(false);

                email.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        positiveButton.setEnabled(false);
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        handler.removeCallbacks(runnable);
                        runnable = () -> {
                            if (isValidEmail(email.getText().toString())) {
                                email.setError(null);
                                if (email.getText().toString().equals(emailRetype.getText().toString())) {
                                    email.setError(null);
                                    emailRetype.setError(null);
                                    positiveButton.setEnabled(true);
                                } else {
                                    email.setError(getString(R.string.email_not_match));
                                }
                            } else {
                                email.setError(getString(R.string.invalid_email));
                            }
                        };
                        handler.postDelayed(runnable, 1500);
                    }
                });

                emailRetype.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        positiveButton.setEnabled(false);
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        handler.removeCallbacks(runnable);
                        runnable = () -> {
                            if (isValidEmail(emailRetype.getText().toString())) {
                                emailRetype.setError(null);
                                if (email.getText().toString().equals(emailRetype.getText().toString())) {
                                    email.setError(null);
                                    emailRetype.setError(null);
                                    positiveButton.setEnabled(true);
                                } else {
                                    emailRetype.setError(getString(R.string.email_not_match));
                                }
                            } else {
                                emailRetype.setError(getString(R.string.invalid_email));
                            }
                        };
                        handler.postDelayed(runnable, 1500);
                    }
                });

                return true;
            });
        }

        Preference changePassword = findPreference("change_password");
        if (changePassword != null) {
            changePassword.setOnPreferenceClickListener(preference -> {

                LinearLayout container = new LinearLayout(requireContext());
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                container.setOrientation(LinearLayout.VERTICAL);
                container.setLayoutParams(containerParams);
                container.setPadding(48, 12, 24, 24);

                final EditText passEditText = new EditText(requireContext());
                passEditText.setLayoutParams(containerParams);
                passEditText.setHint(R.string.type_password);
                passEditText.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                final EditText passEditTextRetype = new EditText(requireContext());
                passEditTextRetype.setLayoutParams(containerParams);
                passEditTextRetype.setHint(R.string.retype_password);
                passEditTextRetype.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                RelativeLayout passContainer = new RelativeLayout(requireContext());
                RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                iconParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                iconParams.addRule(RelativeLayout.CENTER_VERTICAL);
                int marginEnd = (int) getResources().getDimension(R.dimen.edit_text_dialog_margin);
                iconParams.setMarginEnd(marginEnd + 16);
                ImageView passIcon = new ImageView(requireContext());
                passIcon.setLayoutParams(iconParams);
                passIcon.setContentDescription(getString(R.string.show_password));
                passIcon.setImageResource(R.drawable.eye_closed);
                passIcon.setClickable(true);
                passIcon.setFocusable(true);

                passContainer.addView(passEditText);
                passContainer.addView(passIcon);

                RelativeLayout passContainerRetype = new RelativeLayout(requireContext());
                ImageView passIconRetype = new ImageView(requireContext());
                passIconRetype.setLayoutParams(iconParams);
                passIconRetype.setContentDescription(getString(R.string.show_password));
                passIconRetype.setImageResource(R.drawable.eye_closed);
                passIconRetype.setClickable(true);
                passIconRetype.setFocusable(true);
                passContainerRetype.addView(passEditTextRetype);
                passContainerRetype.addView(passIconRetype);

                container.addView(passContainer);
                container.addView(passContainerRetype);

                passIcon.setOnClickListener(view -> {
                    int inputType = passEditText.getInputType();
                    if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        passIcon.setImageResource(R.drawable.eye_open);
                        passEditText.setInputType(InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        passIcon.setContentDescription(getString(R.string.hide_password));
                    } else {
                        passIcon.setImageResource(R.drawable.eye_closed);
                        passEditText.setInputType(InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        passIcon.setContentDescription(getString(R.string.show_password));
                    }
                    passEditText.setSelection(passEditText.getText().length());
                });

                passIconRetype.setOnClickListener(view -> {
                    int inputType = passEditTextRetype.getInputType();
                    if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                        passIconRetype.setImageResource(R.drawable.eye_open);
                        passEditTextRetype.setInputType(InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        passIconRetype.setContentDescription(getString(R.string.hide_password));
                    } else {
                        passIconRetype.setImageResource(R.drawable.eye_closed);
                        passEditTextRetype.setInputType(InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        passIconRetype.setContentDescription(getString(R.string.show_password));
                    }
                    passEditTextRetype.setSelection(passEditTextRetype.getText().length());
                });

                // Build the alert dialog
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

                builder.setTitle(R.string.change_password)
                        .setMessage(getString(R.string.change_pass_desc1) + " " +
                                getUserData().getUsername() + "." +
                                getString(R.string.change_pass_desc2))
                        .setView(container)
                        .setPositiveButton(R.string.change_email_button, (dialog, which) -> {
                            String new_password = passEditText.getText().toString();
                            changePassword(new_password);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                final android.app.AlertDialog alert = builder.create();
                alert.show();

                Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setEnabled(false);

                passEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        positiveButton.setEnabled(false);
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        handler.removeCallbacks(runnable);
                        runnable = () -> {
                            if (passEditText.getText().length() <= 8) {
                                passEditText.setError(getString(R.string.pass_short));
                                positiveButton.setEnabled(false);
                            } else {
                                if (passEditTextRetype.getText().toString()
                                        .equals(passEditText.getText().toString())) {
                                    passEditText.setError(null);
                                    passEditTextRetype.setError(null);
                                    positiveButton.setEnabled(true);
                                } else {
                                    if (passEditTextRetype.getText().length() >= 8) {
                                        passEditText.setError(getString(R.string.pass_not_match));
                                        positiveButton.setEnabled(false);
                                    }
                                }
                            }
                        };
                        handler.postDelayed(runnable, 1500);
                    }
                });

                passEditTextRetype.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        positiveButton.setEnabled(false);
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        handler.removeCallbacks(runnable);
                        runnable = () -> {
                            if (passEditTextRetype.getText().length() <= 8) {
                                passEditTextRetype.setError(getString(R.string.pass_short));
                                positiveButton.setEnabled(false);
                            } else {
                                if (passEditTextRetype.getText().toString()
                                        .equals(passEditText.getText().toString())) {
                                    passEditText.setError(null);
                                    passEditTextRetype.setError(null);
                                    positiveButton.setEnabled(true);
                                } else {
                                    if (passEditText.getText().length() >= 8) {
                                        passEditTextRetype.setError(getString(R.string.pass_not_match));
                                        positiveButton.setEnabled(false);
                                    }
                                }
                            }
                        };
                        handler.postDelayed(runnable, 1500);
                    }
                });

                return true;
            });

        }

        Preference deleteAccount = findPreference("delete_account");
        if (deleteAccount != null) {
            deleteAccount.setOnPreferenceClickListener(preference -> {

                final CheckBox checkboxDeleteData = new CheckBox(requireContext());
                checkboxDeleteData.setText(R.string.delete_field_observations);
                LinearLayout container = new LinearLayout(requireContext());
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                container.setLayoutParams(containerParams);
                container.setPadding(48, 12, 24, 24);
                container.addView(checkboxDeleteData);

                // Build the alert dialog
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

                List<UserDb> userDataList = App.get().getBoxStore().boxFor(UserDb.class).getAll();
                UserDb userData = userDataList.get(0);
                String username = userData.getUsername();
                String database = SettingsManager.getDatabaseName();

                builder.setTitle(R.string.delete_account_alert_title)
                        .setMessage(getString(R.string.delete_msg1) + username +
                                getString(R.string.delete_msg2) + database +
                                getString(R.string.delete_msg3) +
                                getString(R.string.delete_msg4))
                        .setView(container)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            boolean deleteData = checkboxDeleteData.isChecked();
                            deleteAccount(deleteData);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                final android.app.AlertDialog alert = builder.create();
                alert.setOnShowListener(new DialogInterface.OnShowListener() {
                    private static final int AUTO_DISMISS_MILLIS = 10000;
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        final Button defaultButton = alert.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                        defaultButton.setEnabled(false);
                        final CharSequence negativeButtonText = defaultButton.getText();
                        new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                            @Override
                            public void onTick(long l) {
                                defaultButton.setText(String.format(
                                        Locale.getDefault(), "%s (%d)",
                                        negativeButtonText,
                                        TimeUnit.MILLISECONDS.toSeconds(l) + 1
                                ));
                            }

                            @Override
                            public void onFinish() {
                                if (alert.isShowing()) {
                                    defaultButton.setEnabled(true);
                                    defaultButton.setText(negativeButtonText);
                                }
                            }
                        }.start();
                    }
                });
                alert.show();

                return true;
            });
        }

    }

    private void changePassword(String newPassword) {
        UserDb userData = getUserData();
        long user_id = userData.getUserId();
        Log.d(TAG, "This is the user ID: " + userData.getUserId() + " (email " + userData.getEmail() + ").");

        Call<ResponseBody> changePassword = RetrofitClient
                .getService(SettingsManager.getDatabaseName()).editUserPassword(user_id, newPassword);
        changePassword.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.password_changed)
                            .setMessage(getString(R.string.password_changed_desc1) +
                                    " " + SettingsManager.getDatabaseName() + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                final Intent startLandingActivity = new Intent(getActivity(), ActivityLanding.class);
                                startLandingActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(startLandingActivity);
                                dialog.dismiss();
                            });
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    int code = response.code();
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.error)
                            .setMessage(getString(R.string.error_pass_changed_desc1) + " " + code + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.error)
                        .setMessage(getString(R.string.error_pass_changed_desc2) + " " + t)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void changeEmail(String newEmail) {
        UserDb userData = getUserData();
        long user_id = userData.getUserId();
        Log.d(TAG, "This is the user ID: " + userData.getUserId() + " (email " + newEmail + ").");

        Call<ResponseBody> changeEmailAddress = RetrofitClient
                .getService(SettingsManager.getDatabaseName()).editUserEmail(user_id, newEmail);
        changeEmailAddress.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    SettingsManager.setMailConfirmed(false);
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.email_now_changed)
                            .setMessage(getString(R.string.email_changed_desc1) + " " +
                                    newEmail + ". " +
                                    getString(R.string.email_changed_desc2) + " " +
                                    getString(R.string.email_changed_desc3))
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                final Intent startLandingActivity = new Intent(getActivity(), ActivityLanding.class);
                                startLandingActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(startLandingActivity);
                                dialog.dismiss();
                            });
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    int code = response.code();
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    if (code == 422) {
                        builder.setTitle(R.string.error)
                                .setMessage(getString(R.string.email_changed_error_desc5) + " " + newEmail + " "
                                        + getString(R.string.email_changed_error_desc6) + " "
                                        + SettingsManager.getDatabaseName() + ".")
                                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                    } else {
                    builder.setTitle(R.string.error)
                            .setMessage(getString(R.string.email_changed_error_desc1) + " " + code + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                    }
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.error)
                        .setMessage(getString(R.string.email_changed_error_desc2) + " " + t)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void deleteAccount(boolean deleteData) {
        UserDb userData = getUserData();
        long user_id = userData.getUserId();
        Log.d(TAG, "This is the user ID: " + userData.getUserId());

        // Must send true/false as 0/1
        int delete;
        if (deleteData) {
            delete = 1;
        } else {
            delete = 0;
        }

        Call<ResponseBody> deleteUser = RetrofitClient
                .getService(SettingsManager.getDatabaseName()).deleteUser(user_id, delete);
        deleteUser.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Delete user preferences and database
                    ObjectBoxHelper.removeAllData(getContext());
                    SettingsManager.deleteSettings();

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.account_deleted)
                            .setMessage(getString(R.string.your_user_account_has_been_deleted_from)
                                    + " " + SettingsManager.getDatabaseName() + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                // Go back to login activity and exit the dialog
                                Intent intent = new Intent(getActivity(), ActivityLogin.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                dialog.dismiss();
                            });
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    int code = response.code();
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle(R.string.error)
                            .setMessage(getString(R.string.delete_account_error1) + " " + code + ".")
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.error)
                        .setMessage(getString(R.string.delete_account_error2) + " " + t)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private UserDb getUserData() {
        List<UserDb> userDataList = App.get().getBoxStore().boxFor(UserDb.class).getAll();
        return userDataList.get(0);
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

}
