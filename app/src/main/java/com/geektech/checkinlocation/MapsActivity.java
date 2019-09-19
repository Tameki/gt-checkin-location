package com.geektech.checkinlocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView mDistanceTv;

    private FusedLocationProviderClient mFusedLocationProvider;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private DecimalFormat dfFormat = new DecimalFormat("#,##0.0");

    @Nullable
    private Location mLatestLocation;
    private ArrayList<Location> mPathLocations = new ArrayList<>();

    private boolean mIsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mDistanceTv = findViewById(R.id.distance);

        mFusedLocationProvider = LocationServices
                .getFusedLocationProviderClient(
                        this.getApplicationContext()
                );

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();

                mPathLocations.add(location);
                mLatestLocation = location;

                refreshMarker();
            }
        };

        initMap();
        initLocationRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeLocationUpdates();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000L);
        mLocationRequest.setFastestInterval(5000L);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void requestLocationPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Log.d("ololo", "Permission granted");
                        requestLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Log.d("ololo", "Permission denied");
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        Log.d("ololo", "Permission RationaleShouldBeShown");
                    }
                }).check();
    }


    private void requestLocationUpdates() {
        try {
            mFusedLocationProvider.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.getMainLooper()
            );
        } catch (Exception e) {

        }
    }

    private void removeLocationUpdates() {
        mFusedLocationProvider.removeLocationUpdates(mLocationCallback);
    }


    private void getLastLocation() {
        mFusedLocationProvider.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLatestLocation = task.getResult();
                            refreshMarker();
                        }
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLastLocation();
    }

    private void refreshMarker() {
        if (mLatestLocation != null) {
            LatLng latLng = new LatLng(
                    mLatestLocation.getLatitude(),
                    mLatestLocation.getLongitude()
            );

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Current location"));

            renderPath(mPathLocations);
            if (!mIsLoaded) {
                mIsLoaded = true;
                showMyLocation();
            }
        }
    }

    private void renderPath(ArrayList<Location> path) {
        if (mMap == null) return;
        if (path.size() < 2) return;

        PolylineOptions options = new PolylineOptions();

        options.color(Color.BLACK);
        options.width(5f);


        //start i = 0
        //loop: i++, i = 1
        //      i++, i = 2
        //end: loop ++, i = 3

        //start i = 3
        //loop: i++, i = 4
        //      i++, i = 5
        //end: loop ++, i = 6

        //------------------------------------

        //start i = 0
        //loop: i + 1 = 0 + 1
        //      i + 1 = 0 + 1
        //end: loop++, i = 1

        //start i = 10 // From
        //loop: i + 1 = 10 + 1 // To
        //      i + 1 = 10 + 1 // To
        //end: loop++, i = 11

        double distance = 0.0;

        for (int i = 0; i < path.size(); i++) {
            LatLng latLng = new LatLng(
                    path.get(i).getLatitude(),
                    path.get(i).getLongitude()
            );

            if (i < path.size() - 1) {
                LatLng latLngTo = new LatLng(
                        path.get(i + 1).getLatitude(),
                        path.get(i + 1).getLongitude()
                );

                distance += SphericalUtil.computeDistanceBetween(latLng, latLngTo);
            }

            options.add(latLng);
        }

        mDistanceTv.setText("Distance: " + dfFormat.format(distance) + "m");
        mMap.addPolyline(options);
    }

    //TODO: Add show my location button
    private void showMyLocation() {
        if (mLatestLocation != null) {
            LatLng latLng = new LatLng(
                    mLatestLocation.getLatitude(),
                    mLatestLocation.getLongitude()
            );
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }
    }
}
