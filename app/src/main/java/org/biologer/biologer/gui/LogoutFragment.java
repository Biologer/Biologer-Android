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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.FetchTaxa;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.UserDb;

import java.util.List;

public class LogoutFragment extends Fragment {

    private static final String TAG = "Biologer.Logout";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_logout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();

        if (view != null) {
            Activity activity = getActivity();
            if (activity != null) {
                MaterialButton buttonLogout = getActivity().findViewById(R.id.btn_logout);
                TextView textViewUser = getActivity().findViewById(R.id.current_logged_in_text);
                TextView textViewLogout = getActivity().findViewById(R.id.logout_text);

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
                textViewUser.setText(database_text);

                buttonLogout.setOnClickListener(v -> {
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
                        if (FetchTaxa.isInstanceCreated()) {
                            final Intent fetchTaxa = new Intent(getActivity(), FetchTaxa.class);
                            fetchTaxa.setAction(FetchTaxa.ACTION_CANCEL);
                            requireActivity().startService(fetchTaxa);

                            final Handler handler = new Handler(Looper.getMainLooper());
                            final Runnable runnable = new Runnable() {
                                public void run() {
                                    // need to do tasks on the UI thread
                                    Log.d(TAG, "Waiting for downloading to finish...");
                                    if (FetchTaxa.isInstanceCreated()) {
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

                logoutEnableDisable(buttonLogout, textViewLogout);

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
        if (database.equals("https://biologer.ba")) {
            return "Bosnia and Herzegovina Biologer Community";
        }
        if (database.equals("https://biologer.rs")) {
            return "Serbian Biologer Community";
        }
        if (database.equals("https://biologer.me")) {
            return "Montenegrin Biologer Community";
        }
        if (database.equals("https://birdloger.biologer.org")) {
            return "Birdloger Community";
        }
        if (database.equals("https://dev.biologer.org")) {
            return "Global Biologer Community";
        }
        if (database.equals("https://biologer.hr")) {
            return "Croatian Biologer Community";
        } else {
            return "";
        }
    }

    private void deleteDataAndLogout(Activity activity) {
        Log.d(TAG, "Deleting all user data upon logout.");

        App.get().deleteAllBoxes();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
