package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.databinding.FragmentLogoutBinding;
import org.biologer.biologer.network.UpdateTaxa;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.UserDb;

import java.util.List;

public class FragmentLogout extends Fragment {

    private static final String TAG = "Biologer.Logout";
    private FragmentLogoutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        binding = FragmentLogoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();

        if (view != null) {
            Activity activity = getActivity();
            if (activity != null) {

                List<UserDb> userDataList = App.get().getBoxStore().boxFor(UserDb.class).getAll();
                UserDb userData = userDataList.get(0);
                String username = userData.getUsername();
                String database_url = SettingsManager.getDatabaseName();
                String community_name = getBiologerCommunity(database_url);

                Log.i(TAG, "Current user: " + username);
                String database_text = getString(R.string.currently_logged) +
                        " " + community_name + " " +
                        getString(R.string.at) + " " +
                        database_url + " " + getString(R.string.as_user) + " " +
                        username + " (" +
                        userData.getEmail() + ").";
                binding.textViewUser.setText(database_text);

                binding.buttonLogout.setOnClickListener(v -> {
                    // If there are entries warn the user that the data will be lost!
                    Log.d(TAG, "There are " + App.get().getBoxStore().boxFor(EntryDb.class).count() + " entries in the list.");
                    if (!App.get().getBoxStore().boxFor(EntryDb.class).isEmpty()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(getString(R.string.there_are) + " " +
                                App.get().getBoxStore().boxFor(EntryDb.class).count() + " " +
                                getString(R.string.there_are2));
                        builder.setPositiveButton(getString(R.string.yes), (dialog, id) -> deleteDataAndLogout(activity));
                        builder.setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel());
                        builder.setCancelable(true);
                        final AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        // If download process is active, stop it first
                        if (UpdateTaxa.isInstanceCreated()) {
                            final Intent updateTaxa = new Intent(getActivity(), UpdateTaxa.class);
                            updateTaxa.setAction(UpdateTaxa.ACTION_STOP);
                            requireActivity().startService(updateTaxa);

                            final Handler handler = new Handler(Looper.getMainLooper());
                            final Runnable runnable = new Runnable() {
                                public void run() {
                                    // need to do tasks on the UI thread
                                    Log.d(TAG, "Waiting for downloading to finish...");
                                    if (UpdateTaxa.isInstanceCreated()) {
                                        handler.postDelayed(this, 2000);
                                    } else {
                                        deleteDataAndLogout(activity);
                                    }
                                }
                            };
                            handler.post(runnable);
                        } else {
                            deleteDataAndLogout(activity);
                        }
                    }
                });

                logoutEnableDisable(binding.buttonLogout, binding.textViewLogout);

            }
        }
    }

    private void logoutEnableDisable(MaterialButton button, TextView textView) {
        long entries = App.get().getBoxStore().boxFor(EntryDb.class).count();
        if (entries >= 1) {
            button.setEnabled(false);
            textView.setText(R.string.logout_disabled_note);
        } else {
            button.setEnabled(true);
            textView.setText(getString(R.string.logout_from_biologer));
        }
    }

    public String getBiologerCommunity(String database) {
        return switch (database) {
            case "https://biologer.ba" -> "Bosnia and Herzegovina Biologer Community";
            case "https://biologer.rs" -> "Serbian Biologer Community";
            case "https://biologer.me" -> "Montenegrin Biologer Community";
            case "https://birdloger.biologer.org" -> "Birdloger Community";
            case "https://dev.biologer.org" -> "Global Biologer Community";
            case "https://biologer.hr" -> "Croatian Biologer Community";
            default -> "";
        };
    }

    private void deleteDataAndLogout(Activity activity) {
        Log.d(TAG, "Deleting all user data upon logout.");

        App.get().deleteAllBoxes();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(activity, ActivityLogin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
