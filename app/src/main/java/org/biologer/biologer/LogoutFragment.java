package org.biologer.biologer;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.AppCompatButton;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.biologer.biologer.model.UserData;

import java.util.List;

public class LogoutFragment extends Fragment {

    private static final String TAG = "Biologer.Logout";

    AppCompatButton btn_logout;
    TextView tv_username;
    TextView tv_email;
    TextView tv_database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_logout, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();

        if (view != null) {
            btn_logout = getActivity().findViewById(R.id.btn_logout);
            tv_database = getActivity().findViewById(R.id.currentDatabase);
            tv_username = getActivity().findViewById(R.id.tv_currentlyLogged_username);
            tv_email = getActivity().findViewById(R.id.tv_currentlyLogged_email);

            List<UserData> list = App.get().getDaoSession().getUserDataDao().loadAll();
            UserData ud = list.get(0);
            tv_database.setText(SettingsManager.getDatabaseName());
            tv_username.setText(ud.getUsername());
            tv_email.setText(ud.getEmail());

            btn_logout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = getActivity();
                    if(activity != null) {
                        LandingActivity.clearUserData(activity);
                    }
                    // Kill the app on logout, since new login request does not work on normal logout... :/
                    killApp();
/*
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
*/
                }
            });
        }
    }

    private void killApp() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.exit_application))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        System.exit(0);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
