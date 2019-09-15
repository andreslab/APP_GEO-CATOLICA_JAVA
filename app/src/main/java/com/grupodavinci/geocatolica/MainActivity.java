package com.grupodavinci.geocatolica;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.grupodavinci.geocatolica.R;
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

//-------------------------------------

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
/*import com.google.android.gms.location.LocationServices;


import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;*/
import android.os.Build;
/*import android.os.Bundle;
import android.support.annotation.NonNull;*/
import android.support.annotation.Nullable;
/*import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;*/
import android.widget.Toast;

//-------------------------------------


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    //OCULTAR (FALSE) Y MOSTRAR (TRUE) BARRA DE DESARROLLO
    private static final Boolean DEVELOPER = false;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    Context context = this;
    Button btnSave;
    Button btnDistance;
    Button btnMap;
    TextView user_location;

    Button btnOk;
    Button btnYes;
    Button btnNo;
    ProgressBar progressBar;
    LinearLayout bar_dev;
    private FusedLocationProviderClient mFusedLocationClient;

    private static final String TAG = "MyActivity";

    //CONTIENE TUS DATOS DE TU POSICION ACTUAL
    private Double _latittude;
    private Double _longitude;
    private String _namePlace;

    //FIREBASE
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    //CONTIENEN TODAS LAS POSICIONES (PUNTOS) DE LOS LUGARES GUARDADOS
    ArrayList<String> arrayName = new ArrayList<String>();
    ArrayList<String> arrayLatitude = new ArrayList<String>();
    ArrayList<String> arrayLongitude = new ArrayList<String>();

    //SONIDOS (SIN VOZ) CUANDO ESTAS CERCA DE UN PUNTO
    private MediaPlayer sound1;
    private MediaPlayer sound2;
    private MediaPlayer sound3;
    private MediaPlayer sound4;
    private MediaPlayer sound5;
    private MediaPlayer sound6;

    //AUDIO DEL TUTORIAL
    private MediaPlayer tutorial;

    //VARIABLES DE AUDIOS
    private MediaPlayer audio_resume_fayh;
    private MediaPlayer audio_intro_fayh;

    //CONTIENE EL NOMBRE DE  LA ULTIMA FACULTAD RASTREADA
    private String lastPoint;
    private Boolean lastPlayEqual;
    private action_ok actionBtnOk;


    //---------------------------------

    //CONFIGURACION DE GPS

    private Location location;
    //private TextView locationTv;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;

    //TIEMPO QUE TARDA EN REFRESCAR TU POSICION
    private static final long UPDATE_INTERVAL = 4000, FASTEST_INTERVAL = 4000; // = 4 seconds
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    //---------------------------------

    //DISTANCIA DEFAULT DEL RANGO QUE DETECTA PUNTOS CERCANOS
    double minDistanceMeters = 6.0; //rango de distance en metros

    enum action_ok {
        INFO,
        INFO_FAH,
        INFO_COME,
        INFO_BOM
    }

    //LA PRIMERA FUNCION QUE SE EJECUTA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //VINCULA CODIGO CON INTERFAZ
        btnSave = findViewById(R.id.btn_catch_gps);
        btnMap = findViewById(R.id.btn_map);
        btnDistance = findViewById(R.id.btn_distance);
        user_location = findViewById(R.id.user_location);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        progressBar = findViewById(R.id.progressBar);


        //EJECUTA 1 VEZ EL TURORIAL AL INSTALAR LA APLICACION
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            // <---- run your one time code here
            tutorial = MediaPlayer.create(this, R.raw.tutorial);
            tutorial.start();

            // mark first time has ran.
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTime", true);
            editor.commit();
        }


        bar_dev = findViewById(R.id.linearLayout2);
        if (!DEVELOPER){
            ViewGroup.LayoutParams params = bar_dev.getLayoutParams();
// Changes the height and width to the specified *pixels*
            params.height = 0;
            //params.width = 0;
            bar_dev.setLayoutParams(params);
        }

        btnOk = findViewById(R.id.btn_ok);
        btnYes = findViewById(R.id.btn_yes);
        btnNo = findViewById(R.id.btn_no);

        actionBtnOk = action_ok.INFO;
        lastPlayEqual = false;

        resetParams();


        //DEFINE LOS SONIDOS
        sound1 = MediaPlayer.create(this, R.raw.sound1);
        sound2 = MediaPlayer.create(this, R.raw.sound2);
        sound3 = MediaPlayer.create(this, R.raw.sound3);
        sound4 = MediaPlayer.create(this, R.raw.sound4);
        sound5 = MediaPlayer.create(this, R.raw.sound5);
        sound6 = MediaPlayer.create(this, R.raw.sound6);

        sound1.setLooping(true);
        //sound2.setLooping(true);

        //DEFINE LOS AUDIOS
        audio_resume_fayh = MediaPlayer.create(this, R.raw.audio_fayh);
        audio_intro_fayh = MediaPlayer.create(this, R.raw.intro_fayh);

        progressBar.setVisibility(View.GONE);


        //CONFIGURA LA ACCION DEL BOTON OK
        //REPRODUCE LOS AUDIOS DE LUGAR DONDE SE ENCUENTRE
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

                stopPlaying();

                /*sound1.pause();
                sound2.pause();*/

                if (actionBtnOk == action_ok.INFO_FAH){

                    stopPlaying();
                    audio_intro_fayh = MediaPlayer.create(getApplicationContext(), R.raw.intro_fayh);
                    audio_intro_fayh.start();
                }

                if (actionBtnOk == action_ok.INFO_BOM){
                    stopPlaying();
                    audio_intro_fayh = MediaPlayer.create(getApplicationContext(), R.raw.intro_fayh);
                    audio_intro_fayh.start();
                }
            }
        });

        //CONFIGURA LA ACCION DEL BOTON SI
        //REPRODUCE LOS AUDIOS DE INFORMACION
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                long[] pattern = {0, 100, 200, 300, 500};
                vibrator.vibrate(pattern, -1);

                stopPlaying();

                /*sound1.pause();
                sound2.pause();*/


                if (actionBtnOk == action_ok.INFO_FAH){

                    stopPlaying();
                    audio_resume_fayh = MediaPlayer.create(getApplicationContext(), R.raw.audio_fayh);
                    audio_resume_fayh.start();

                }

                if (actionBtnOk == action_ok.INFO_BOM){
                    stopPlaying();
                    audio_resume_fayh = MediaPlayer.create(getApplicationContext(), R.raw.audio_fayh);
                    audio_resume_fayh.start();
                }
            }
        });

        //CONFIGURA LA ACCION DEL BOTON NO
        //DETIENE AUDIOS QUE SE ESTEN EJECUTANDO Y PRODUCE UN PATRON VIBRACION
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                long[] pattern = {0, 500, 300, 200, 100};
                vibrator.vibrate(pattern, -1);

                stopPlaying();

                /*sound1.pause();
                sound2.pause();*/
            }
        });


        //BOTON QUE GUARDA NUEVA POSICION
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE); //to show
                showAlertToSaveName();
            }
        });

        //BOTON QUE LLEVA AL MAPA QUE CONTIENE LAS POSICIONES
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE); //to show
                readPlacesFirestore(true);
            }
        });

        //BOTON PARA DEFINIR DISTANCIA DE PUNTOS
        btnDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE); //to show
                showAlertToAssignRange();
            }
        });


        //EJECUTA LA ACTUALIZACION DEL GPS REPETITIVAMENTE
        // we add permissions we need to request location of the users
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();

        user_location.setText("D: "+ minDistanceMeters + " m" + " | PLACE : ");


        //CONSULTA DE BASE DE DATOS DE FIRESTORE
        readPlacesFirestore(false);
    }

    //RESETEA PARAMETROS DE POSICION
    private void resetParams(){
        _latittude = 0.0;
        _longitude = 0.0;
        _namePlace = "";
        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f);
        progressBar.setVisibility(View.GONE);
    }

    //ACTIVA EL BOTON DE GUARDAR NUEVA POSICION AL TENER INFORMACION GPS
    private void activeBtnSave(){
        btnSave.setEnabled(true);
        btnSave.setAlpha(1f);
    }

    //FUNCION PARA DETENER LOS SONIDOS
    private void stopPlayingSoundRepeat() {
        if (sound1 != null) {
            sound1.stop();
            sound1.release();
            sound1 = null;
        }
        if (tutorial != null){
            tutorial.stop();
            tutorial.release();
            tutorial = null;
        }
    }

    //FUNCION PARA DETENER LOS AUDIOS
    private void stopPlaying() {
        if (audio_resume_fayh != null) {
            audio_resume_fayh.stop();
            audio_resume_fayh.release();
            audio_resume_fayh = null;
        }

        if (audio_intro_fayh != null) {
            audio_intro_fayh.stop();
            audio_intro_fayh.release();
            audio_intro_fayh = null;
        }

        if (tutorial != null){
            tutorial.stop();
            tutorial.release();
            tutorial = null;
        }
    }

    //FUNCION PARA GUARDAR POSICION EN FIRESTORE
    private void savePlaceFirestore(Double lat, Double log, String namePlace){
        //ADD DATA FIRESTORE

        if (lat != 0.0 && log != 0.0 && lat !=null && log != null){
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
                            Log.i(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            resetParams();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error adding document", e);
                            resetParams();
                        }
                    });
        }else{
            Toast toast1 = Toast.makeText(getApplicationContext(), "GPS ERROR...", Toast.LENGTH_SHORT);
            //toast1.setGravity(Gravity.CENTER, , );
            toast1.show();
        }

    }

    //CONSULTA DE DATOS A LA BASE DE DATOS DE FIREBASE - FIRESTORE
    private void readPlacesFirestore(final Boolean goToMap){
        //READ DATA FIRESTORE
        db.collection("places")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG,  "LOADING DATA...");
                            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);


                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.i(TAG, document.getId() + " => " + document.getData());
                                arrayName.add(document.getData().get("name").toString());
                                arrayLatitude.add(document.getData().get("latitude").toString());
                                arrayLongitude.add(document.getData().get("longitude").toString());
                            }

                            intent.putExtra("listName", arrayName);
                            intent.putExtra("listLatitude", arrayLatitude);
                            intent.putExtra("listLongitude", arrayLongitude);
                            if (goToMap){startActivity(intent);}

                        } else {
                            Log.i(TAG, "Error getting documents.", task.getException());

                        }

                        progressBar.setVisibility(View.GONE);
                    }
                });
    }


    //VERIFICA LOS LUGARES GEOLOCALIZADOS QUE SE ENCUENTRAN A UNA DISTANCIA DETERMINADA
    private void checkPlaceInRange(Double minDistance_meters){
        Double minDistanceKm = minDistance_meters / 1000; //convertir metros a kilometros
        Pair<String, Double> data = detectNearstPlaces();

        //sound1.pause();

        if (data.second <= minDistanceKm) {
            //esta en el rango
            switch (data.first) {
                case "fayh":

                    if (audio_resume_fayh != null){
                        if (!audio_resume_fayh.isPlaying()) {
                            if (lastPoint == data.first) {
                                stopPlayingSoundRepeat();
                                sound1 = MediaPlayer.create(this, R.raw.sound1);
                                sound1.start();
                            } else {
                                stopPlayingSoundRepeat();
                                audio_intro_fayh = MediaPlayer.create(this, R.raw.intro_fayh);
                                audio_intro_fayh.start();
                            }
                        }
                        lastPoint = data.first;
                    }else{
                        audio_resume_fayh = MediaPlayer.create(this, R.raw.audio_fayh);
                    }
                    actionBtnOk = action_ok.INFO_FAH;
                    Log.i(TAG, "Code: fayh");

                case "come1":
                    Log.i(TAG, "Code come1");
                    actionBtnOk = action_ok.INFO_COME;

                case "cien":
                    Log.i(TAG, "Code: cien");

                    //test
                case "bom":

                    if (audio_resume_fayh != null) {
                        Log.i(TAG, "paso1");
                        if (!audio_resume_fayh.isPlaying()) {
                            Log.i(TAG, "paso2");
                            if (lastPoint == data.first) {
                                Log.i(TAG, "paso3");
                                stopPlayingSoundRepeat();
                                sound1 = MediaPlayer.create(this, R.raw.sound1);
                                sound1.start();
                            }else{
                                Log.i(TAG, "paso4");
                                stopPlayingSoundRepeat();
                                audio_intro_fayh = MediaPlayer.create(this, R.raw.intro_fayh);
                                audio_intro_fayh.start();
                            }
                        }
                        lastPoint = data.first;
                    }else{
                        audio_resume_fayh = MediaPlayer.create(this, R.raw.audio_fayh);
                    }
                    Log.i(TAG, "code: bom");
                    actionBtnOk = action_ok.INFO_BOM;
            }
        }else{
            actionBtnOk = action_ok.INFO;
        }
    }

    private void pauseAllSound(){

    }

    //DETECTA LOS LUGARES MAS CERCANOS
    private Pair<String, Double> detectNearstPlaces(){
            String namePlaceNearst = "";
            Double nearstDistance = 200.0; //distancia mas corta, valor random alto, medida en KM
        for (int i = 0; i < arrayName.size(); i++){

            Double endLatitude = Double.valueOf(arrayLatitude.get(i));
            Double endLongitude = Double.valueOf(arrayLongitude.get(i));
            Double distance = distanceBetweenTwoPoint(_latittude, _longitude, endLatitude, endLongitude);

            if (distance < nearstDistance){
                //la nueva distancia es mas pequena que la guardada
                nearstDistance = distance;
                namePlaceNearst = arrayName.get(i);
            }
        }

        return new Pair<String, Double>(namePlaceNearst, nearstDistance);
    }

    //COMPARA DOS LOCALIZACIONES GEOPOSICIONADAS Y DEVUELVE LA DISTANCIA ENTRE ELLAS
    public static double distanceBetweenTwoPoint(double startLat, double startLong,
                                  double endLat, double endLong){
        //KM
        return HaversineDistance.distance(startLat, startLong,
                endLat, endLong);
    }

    /*private void fetchLocation() {


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
                                user_location.setText("LAST PLACE : " + _namePlace.toUpperCase());

                            }
                        }
                    });

        }

    }*/

    //DEFINE EL RANGO DE DETECCION DE UN PUNTO GEOPOSICIONADO
    private void showAlertToAssignRange(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ingrese el rango de istancia en metros");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                minDistanceMeters = Double.valueOf(input.getText().toString());
                user_location.setText("D: "+ minDistanceMeters + " m" + " | PLACE : " + _namePlace.toUpperCase());
                progressBar.setVisibility(View.GONE);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressBar.setVisibility(View.GONE);
                dialog.cancel();
            }
        });

        builder.show();
    }


    //ALERTA PARA INGRESAR NUEVA POSICION EOGRAFICA
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
                //fetchLocation();
                savePlaceFirestore(_latittude, _longitude, _namePlace);
                user_location.setText("D: "+ minDistanceMeters + " m" + " | PLACE : " + _namePlace.toUpperCase());

            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetParams();
                dialog.cancel();
            }
        });

        builder.show();
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //abc
            }else{

            }
        }
    }*/


    // CATCH GPS TIMER

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    //CICLO DE VIDA DE PANTALLA: PANTALLA CARGANDO, PROCESOS INTERNOS (NO SE MUESTRA)
    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    //CICLO DE VIDA DE PANTALLA: PANTALLA APARECE PARA USUARIO
    @Override
    protected void onResume() {
        super.onResume();

        if (!checkPlayServices()) {
            user_location.setText("ERROR..");
            //locationTv.setText("You need to install Google Play Services to use the App properly");
        }
    }

    //CICLO DE VIDA DE PANTALLA: PANTALLA EN PAUSA (NO SE MUESTRA)
    @Override
    protected void onPause() {
        super.onPause();

        // stop location updates
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    //ENLACE CON SERVICIOS DE GOOGLE
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    //CALLBACK: SE EJECUTA CUANDO SE OBTIENE LA GEOPOSICION
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //VALIDA QUE EL USUARIO DIO LOS PERMISOS NECESARIOS
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            _latittude = location.getLatitude();
            _longitude = location.getLongitude();
            activeBtnSave();
            checkPlaceInRange(minDistanceMeters);
            Log.i(TAG, "Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
            //locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }

        //INICIA LA ACTUALIZACION AUTOMATICA DE LA GEOPOSICION
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    //CONEXION DE GPS SUSPENDIDA POR INTERNET
    @Override
    public void onConnectionSuspended(int i) {
    }

    //CONEXION DE GPS PERDIDA
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    //SI LA POSICION DE GPS CAMBIO
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            _latittude = location.getLatitude();
            _longitude = location.getLongitude();
            activeBtnSave();
            checkPlaceInRange(minDistanceMeters);
            Log.i(TAG, "Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
            //locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }
    }

    //VERIFICA SI TIENES LOS PERMISOS NECSARIOS Y SI NO LOS TIENES TE LOS PIDE
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }

                break;
        }
    }
}
