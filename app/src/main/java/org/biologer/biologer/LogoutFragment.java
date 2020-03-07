package org.biologer.biologer;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.biologer.biologer.model.UserData;

import java.util.List;
import java.util.Objects;

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
            AppCompatButton btn_logout = Objects.requireNonNull(getActivity()).findViewById(R.id.btn_logout);
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
                Activity activity = getActivity();
                if(activity != null) {
                    LandingActivity.clearUserData(activity);
                }
                // Kill the app on logout, since new login request does not work on normal logout... :/
                killApp();
            });
        }
    }

    private void killApp() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setMessage(getString(R.string.exit_application))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.OK), (dialog, id) -> System.exit(0));
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
