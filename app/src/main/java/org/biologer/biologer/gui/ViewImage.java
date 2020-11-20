package org.biologer.biologer.gui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.biologer.biologer.R;

public class ViewImage extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_imageview);

            // Add a toolbar to the Activity
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            ActionBar actionbar = getSupportActionBar();
            if (actionbar != null) {
                actionbar.setTitle("Image preview");
                actionbar.setDisplayHomeAsUpEnabled(true);
                actionbar.setDisplayShowHomeEnabled(true);
            }

            // Get the image location from previous activity
            Intent intent = getIntent();
            String image = intent.getStringExtra("image");

            ImageView imageview = (ImageView) findViewById(R.id.fullScreenImageView);
            imageview.setImageURI(Uri.parse(image));
            imageview.bringToFront();
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
