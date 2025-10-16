package org.biologer.biologer.gui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.biologer.biologer.R;
import org.biologer.biologer.databinding.ActivityViewImageBinding;

public class ActivityViewImage extends AppCompatActivity {

    private ActivityViewImageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Add a toolbar to the Activity
        addToolbar();

        // Get the image location from previous activity
        Intent intent = getIntent();
        String image_url = intent.getStringExtra("image");

        if (image_url != null) {
            binding.imageViewZoom.setImageURI(Uri.parse(image_url));
        }

    }

    private void addToolbar() {
        setSupportActionBar(binding.toolbar.toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(getString(R.string.image_view));
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }
    }

    // Process running after clicking the toolbar buttons (back and save)
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return true;
    }

}
