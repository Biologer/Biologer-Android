package org.biologer.biologer.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationPhotoDownloader {
    private static final String TAG = "Biologer.NotyPhoto";


}