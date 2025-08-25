package org.biologer.biologer.gui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ortiz.touchview.TouchImageView;

import org.biologer.biologer.R;

public class ActivityViewImage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageview);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle(getString(R.string.image_view));
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        // Get the image location from previous activity
        Intent intent = getIntent();
        String image_url = intent.getStringExtra("image");

        TouchImageView imageView = findViewById(R.id.imageViewZoom);
        if (image_url != null) {
            imageView.setImageURI(Uri.parse(image_url));
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
