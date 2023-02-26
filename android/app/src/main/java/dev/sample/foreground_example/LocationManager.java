package dev.sample.foreground_example;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationManager {

    private static final String TAG = "SampleService_LM";
    private static final DateFormat DATAFORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    protected static Uri mLogFileUri;
    private Activity mMain;

    public LocationManager(Activity main) {
        mMain = main;
    }



    public void showLocationLogPicker() {

        if(mLogFileUri == null) {
            // 保存ファイル名
            String fileName = "LacationLog.txt";

            // ファイル保存のためのピッカーUI起動
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            mMain.startActivityForResult(intent, 12345);
        }
    }

    public void setLogFileUri( Uri uri){
        mLogFileUri = uri;
    }

    private static Location mLastLogLocation ;
    private static LocationCallback mLocationCallback;

    public static void setLocationLister(SampleService serv){

        if(mLocationCallback != null){
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);       // 位置情報更新間隔の希望
        locationRequest.setFastestInterval(5000); // 位置情報更新間隔の最速値
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // この位置情報要求の優先度

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(serv);
        if (ActivityCompat.checkSelfPermission(serv,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(serv,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    String str1 = " Latitude:" + location.getLatitude();
                    String str2 = " Longitude:" + location.getLongitude();

                    if(isNearPoint(mLastLogLocation,location) == false) {
                        writeLocationLog(location, serv);
                        mLastLogLocation = location;
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
    }

    private static boolean isNearPoint(Location l1, Location l2){
        if(l1 == null || l2 == null){
            return false;
        }

        double dx = l1.getLongitude() - l2.getLongitude();
        double dy = l1.getLatitude() - l2.getLatitude();

        double distance = Math.sqrt(dx*dx + dy*dy);
        return distance < 0.0001;
    }

    public static void writeLocationLog(Location location, SampleService serv) {

        Log.d(LocationManager.TAG, "lon=" + location.getLongitude() + ", lat=" + location.getLatitude() + ","+ location.getTime() );

        if(mLogFileUri == null){
            return;
        }

        // try-with-resources
        Date date = new Date(System.currentTimeMillis());

        String str =  DATAFORMAT.format(date) + "," +
                location.getLongitude() + "," +
                location.getLatitude() + "," +
                String.format("%.1f", location.getAltitude())+ "," +
                location.getAccuracy() + "," +
                location.getSpeed() + "," +
                location.getTime()
                ;
        try(OutputStream outputStream =
                    serv.getContentResolver().openOutputStream(mLogFileUri, "wa")) {
            if(outputStream != null){
                /// データを書き込み
                outputStream.write(str.getBytes());
                outputStream.write('\n');
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    };
}
