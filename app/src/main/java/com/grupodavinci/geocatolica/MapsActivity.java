package com.grupodavinci.geocatolica;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.grupodavinci.geocatolica.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    ArrayList<String> arrayName = new ArrayList<String>();
    ArrayList<String> arrayLatitude = new ArrayList<String>();
    ArrayList<String> arrayLongitude = new ArrayList<String>();

    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            arrayName = extras.getStringArrayList("listName");
            arrayLatitude = extras.getStringArrayList("listLatitude");
            arrayLongitude = extras.getStringArrayList("listLongitude");
            Log.i(TAG, "DATA INIT: " + arrayName.toString());
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Log.i(TAG, "LOAD...");
        if (arrayName != null) {
            Log.i(TAG, "DATA: " + arrayName.toString());
            for(int i = 0; i < arrayName.size(); i ++){
                LatLng place = new LatLng(Double.valueOf(arrayLatitude.get(i)), Double.valueOf(arrayLongitude.get(i)));
                mMap.addMarker(new MarkerOptions().position(place).title(arrayName.get(i)));
                if (i == 0) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(place));
                    mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
                }
            }
        }

    }
}
