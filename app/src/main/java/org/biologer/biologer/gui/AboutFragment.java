package org.biologer.biologer.gui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;

import java.util.Objects;


public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();

        // Get the name of the database currently in use
        if (view != null) {
            TextView tv_database = Objects.requireNonNull(getActivity()).findViewById(R.id.currentDatabase);
            tv_database.setText(SettingsManager.getDatabaseName());
        }

        // Add onClick to the biologer.org link
        TextView address = (TextView) Objects.requireNonNull(getActivity()).findViewById(R.id.biologerorg_url);
        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://biologer.org"));
                Objects.requireNonNull(getActivity()).startActivity(i);
            }
        });
    }
}
