package com.andreslab.geocatolica;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    Context context = this;
    Button btnSave;
    Button btnMap;
    TextView user_location;

    Button btnOk;
    Button btnYes;
    Button btnNo;
    private FusedLocationProviderClient mFusedLocationClient;

    private static final String TAG = "MyActivity";

    private Double _latittude;
    private Double _longitude;
    private String _namePlace;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSave = findViewById(R.id.btn_catch_gps);
        btnMap = findViewById(R.id.btn_map);
        user_location = findViewById(R.id.user_location);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnOk = findViewById(R.id.btn_ok);
        btnYes = findViewById(R.id.btn_yes);
        btnNo = findViewById(R.id.btn_no);


        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                long[] pattern = {0, 1000, 1000};

                // The '0' here means to repeat indefinitely
                // '0' is actually the index at which the pattern keeps repeating from (the start)
                // To repeat the pattern from any other point, you could increase the index, e.g. '1'
                // -1  repeat once
                vibrator.vibrate(pattern, -1);
            }
        });

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                long[] pattern = {0, 100, 200, 300, 500};
                vibrator.vibrate(pattern, -1);
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                long[] pattern = {0, 500, 300, 200, 100};
                vibrator.vibrate(pattern, -1);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showAlertToSaveName();
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readPlacesFirestore();
            }
        });


    }

    private void savePlaceFirestore(Double lat, Double log, String namePlace){
        //ADD DATA FIRESTORE
        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("name", namePlace);
        user.put("latitude", lat);
        user.put("longitude", log);

// Add a new document with a generated ID
        db.collection("places")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        _latittude = 0.0;
                        _longitude = 0.0;
                        _namePlace = "";
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        _latittude = 0.0;
                        _longitude = 0.0;
                        _namePlace = "";
                    }
                });
    }

    private void readPlacesFirestore(){
        //READ DATA FIRESTORE
        db.collection("places")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }

                            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                            //intent.putExtra("places", ["","",""]);

                            startActivity(intent);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void fetchLocation() {


        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                new AlertDialog.Builder(this)
                        .setTitle("Required Location Permission")
                        .setMessage("You have to give this permission to acess this feature")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                Double latittude = location.getLatitude();
                                Double longitude = location.getLongitude();
                                _latittude = latittude;
                                _longitude = longitude;
                                savePlaceFirestore(_latittude, _longitude, _namePlace);
                                user_location.setText("LAST SAVE : " + _namePlace.toUpperCase());

                            }
                        }
                    });

        }

    }

    private void showAlertToSaveName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ingrese el nombre del lugar");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _namePlace = input.getText().toString();
                fetchLocation();

            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _latittude = 0.0;
                _longitude = 0.0;
                _namePlace = "";
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //abc
            }else{

            }
        }
    }
}
