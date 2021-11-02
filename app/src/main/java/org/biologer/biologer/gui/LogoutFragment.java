package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatButton;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.User;
import org.biologer.biologer.network.FetchTaxa;
import org.biologer.biologer.sql.UserData;

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
                AppCompatButton btn_logout = getActivity().findViewById(R.id.btn_logout);
                TextView tv_database = getActivity().findViewById(R.id.currentDatabase);
                TextView tv_username = getActivity().findViewById(R.id.tv_currentlyLogged_username);
                TextView tv_email = getActivity().findViewById(R.id.tv_currentlyLogged_email);

                List<UserData> list = App.get().getDaoSession().getUserDataDao().loadAll();
                UserData ud = list.get(0);
                String username = ud.getUsername();
                Log.i(TAG, "Current user: " + username);
                tv_database.setText(SettingsManager.getDatabaseName());
                tv_username.setText(username);
                tv_email.setText(ud.getEmail());

                btn_logout.setOnClickListener(v -> {
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
                });
            }
        }
    }

    private void deleteDataAndLogout(Activity activity) {
        Log.d(TAG, "Deleting all user data upon logout.");
        User.clearUserData(activity);

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
