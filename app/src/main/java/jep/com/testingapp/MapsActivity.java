package jep.com.testingapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,CountPlantAndPlayGame.sendChoosenPlant,CountPlantAndPlayGame.sendStopGame {

    BroadcastReceiver broadcastReceiver;
    private GoogleMap mMap;
    private RequestQueue queue;
    private List<Plant> plants = new ArrayList<>();
    private SharedPreferences sp;
    private String plantUrl = "https://mysterious-fjord-16136.herokuapp.com/api/Plants/";
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    public SeekBar seekBar;
    private Location loc_plant=new Location("my plant");
    private int distanceInMetersToPlant;
    private int fastestIntervalTime=5000;
    private int intervalTime=5000;
    private int activityState=0;

    boolean onPlaying=false;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        sp = getSharedPreferences("login", MODE_PRIVATE);
        queue = Volley.newRequestQueue(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setSeekbarInvisible();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Toast.makeText(MapsActivity.this,"location update", Toast.LENGTH_SHORT);

                for (Location location : locationResult.getLocations()) {
                    SearchPlantsAt5km(location);
                    trackPositionPlayer(location);
                    huntChoosenPlant(loc_plant,location);





                }
            };
        };
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("onn", "onReceive: ");
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };


    }
    private void handleUserActivity(int type, int confidence) {
        if(type!=activityState){
        activityState=type;
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        switch (type) {

            case DetectedActivity.RUNNING: {

                fastestIntervalTime=5000;
                intervalTime=10000;

                break;
            }
            case DetectedActivity.STILL: {
                fastestIntervalTime=60000;
                intervalTime=60000;

                break;
            }

            case DetectedActivity.WALKING: {
                fastestIntervalTime=15000;
                intervalTime=30000;


                break;
            }

        }
        createLocationRequest();
        }



        if (confidence > Constants.CONFIDENCE) {



        }
    }
    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof CountPlantAndPlayGame) {
            CountPlantAndPlayGame countPlantAndPlayGame = (CountPlantAndPlayGame) fragment;
            countPlantAndPlayGame.setOnPlantSelectedListener(this);
            countPlantAndPlayGame.setOnStopGameListener(this);
        }
    }
    public void onPlantSelected(ArrayList<String> choosenPlant) {
        startGame(choosenPlant);

    }

    @Override
    public void onStopGame(boolean stop) {
        StopGame(stop);
    }

    private void StopGame(Boolean b){
        onPlaying=b;
        setTvVisibleAndButtonChange();
        setSeekbarInvisible();
        loc_plant = new Location("my plant");
        if (!plants.isEmpty()) {


            showPlants(plants);


        }
    }
    private void setTvInvisibleAndBtChange(){
        if (MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment)!=null){
            View view =MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment).getView();
            TextView tv = view.findViewById(R.id.numberPlant);
            Button btPlay=view.findViewById(R.id.buttonPlay);
            Button btStop=view.findViewById(R.id.buttonStop);
            tv.setVisibility(View.INVISIBLE);
            btPlay.setVisibility(View.INVISIBLE);
            btStop.setVisibility(View.VISIBLE);

        }

    }
    private void startGame(ArrayList<String> choosenPlant){
        if (choosenPlant.size()>1){
            loc_plant.setLatitude(Double.valueOf(choosenPlant.get(1)));
            loc_plant.setLongitude(Double.valueOf(choosenPlant.get(2)));
            loc_plant.setTime(new Date().getTime());
            setSeekbarVisible();
            if (!plants.isEmpty()) {
                    mMap.clear();
                }
            }

            onPlaying=true;
            setTvInvisibleAndBtChange();
            startTracking();


        }


    private void onGameFinished(){

        AlertDialog.Builder builderChoosen = new AlertDialog.Builder(MapsActivity.this);
        AlertDialog alertDialogChoosen=builderChoosen.create();
        alertDialogChoosen.setTitle("YOU WIN!!!!! Thanks you for playing");
        alertDialogChoosen.show();
        onPlaying=false;
        setTvVisibleAndButtonChange();
        setSeekbarInvisible();

        loc_plant = new Location("my plant");
        if (!plants.isEmpty()) {

          showPlants(plants);



        }
        stopTracking();
    }
    private void setTvVisibleAndButtonChange(){
        if (MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment)!=null){
            View view =MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment).getView();
            TextView tv = view.findViewById(R.id.numberPlant);
            Button btPlay=view.findViewById(R.id.buttonPlay);
            Button btStop=view.findViewById(R.id.buttonStop);
            tv.setVisibility(View.VISIBLE);
            btPlay.setVisibility(View.VISIBLE);
            btStop.setVisibility(View.INVISIBLE);

        }

    }
    private void setSeekbarInvisible(){

        if (MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.distanceToPlant)!=null){
            View view =MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.distanceToPlant).getView().findViewById(R.id.seekBar);
            seekBar = view.findViewById(R.id.seekBar);
            seekBar.setVisibility(View.INVISIBLE);

        }

    }
    private void setSeekbarVisible(){

        if (MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.distanceToPlant)!=null){
            View view =MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.distanceToPlant).getView().findViewById(R.id.seekBar);
            seekBar = view.findViewById(R.id.seekBar);
            seekBar.setVisibility(View.VISIBLE);
            seekBar.setEnabled(false);

        }

    }
    private void trackPositionPlayer(Location location){
        LatLng mLocation=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mLocation));
    }
    private void huntChoosenPlant(Location loc_choosen_plant,Location actualLocation){

        if (loc_choosen_plant.getLongitude()!=0.0 && loc_choosen_plant.getLatitude()!=0.0){

            distanceInMetersToPlant=(int)actualLocation.distanceTo(loc_choosen_plant);
            if (distanceInMetersToPlant<=5000){
                seekBar.setProgress(distanceInMetersToPlant);
                if (distanceInMetersToPlant<=4){
                    onGameFinished();

                }
            }

            else{
                seekBar.setProgress(5000);
            }
        }

    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(intervalTime);
        mLocationRequest.setFastestInterval(fastestIntervalTime);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null);
            Toast.makeText(this, String.valueOf(intervalTime),
                    Toast.LENGTH_LONG).show();
        }

    }

    private void SearchPlantsAt5km(Location myLocation){

        ArrayList<ArrayList<String>> Allplants=new ArrayList<ArrayList<String>>();

        if (!plants.isEmpty()) {

            for (Plant plant : plants) {

                Location actualPlant = new Location("actual plant");
                actualPlant.setLatitude(plant.getLat());
                actualPlant.setLongitude(plant.getLon());
                actualPlant.setTime(new Date().getTime());

                if (actualPlant.distanceTo(myLocation) <= 5000) {
                    ArrayList<String> eachPlant=new ArrayList();
                    eachPlant.add(plant.getName());
                    eachPlant.add(String.valueOf(plant.getLat()));
                    eachPlant.add(String.valueOf(plant.getLon()));
                    Allplants.add(eachPlant);


                }else {
                    ArrayList<String> eachPlant=new ArrayList();
                    eachPlant.add(plant.getName());
                    eachPlant.add(String.valueOf(plant.getLat()));
                    eachPlant.add(String.valueOf(plant.getLon()));
                    Allplants.removeAll(eachPlant);


                }

            }
            if (MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment)!=null) {
                View view = MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment).getView().findViewById(R.id.numberPlant);
                TextView numberPlant = view.findViewById(R.id.numberPlant);
                Bundle b = new Bundle();
                if (!Allplants.isEmpty()) {
                    for (ArrayList<String> plant1 : Allplants) {
                        b.putStringArrayList("listPlants", plant1);
                        MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment).setArguments(b);

                        numberPlant.setText("Nb Plant<5km: " + String.valueOf(Allplants.size()));
                    }
                } else {
                    ArrayList<String> noPlant = new ArrayList<>();
                    noPlant.add("No plant here");
                    b.putStringArrayList("listPlants", noPlant);
                    MapsActivity.this.getSupportFragmentManager().findFragmentById(R.id.playGameFragment).setArguments(b);

                    numberPlant.setText("Nb Plant<5km: " + String.valueOf(Allplants.size()));
                }
            }






        }

    }
    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(true);
                onResume();
                createLocationRequest();

            } else {
                checkLocationPermission();


            }
        } else {

            mMap.setMyLocationEnabled(true);
            onResume();
            createLocationRequest();



        }

        if(!sp.getBoolean("logged", false)) {
            return;
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);


        LatLng location = new LatLng(48, 2);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,9));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                if (onPlaying == false) {


                    LayoutInflater description_plant = LayoutInflater.from(MapsActivity.this);
                    final View alertDialogView = description_plant.inflate(R.layout.dialog_plant_description, null);
                    AlertDialog.Builder adb = new AlertDialog.Builder(MapsActivity.this);
                    adb.setView(alertDialogView);
                    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            EditText name_plant = alertDialogView.findViewById(R.id.plant_name);
                            EditText description_plant = alertDialogView.findViewById(R.id.description);


                            JSONObject jsonPlant = new JSONObject();
                            try {
                                jsonPlant.put("username", sp.getString("user", "notLoggedIn"));
                                jsonPlant.put("plantname", name_plant.getText().toString());
                                jsonPlant.put("lat", latLng.latitude);
                                jsonPlant.put("lon", latLng.longitude);
                                jsonPlant.put("description", description_plant.getText().toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, plantUrl, jsonPlant,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject jsonPlant) {
                                            Plant plant = null;
                                            try {
                                                plant = new Plant(jsonPlant.getString("_id"), jsonPlant.getString("username"), jsonPlant.getString("plantname"), Float.parseFloat(jsonPlant.getString("lat")), Float.parseFloat(jsonPlant.getString("lon")), jsonPlant.getString("description"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            if (plant != null) {
                                                plants.add(plant);
                                                showPlants(plants);
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                }
                            });
                            queue.add(jsonObjectRequest);
                        }
                    });

                    adb.show();
                }
            }
        });


    }


    private void requestPlants() {
        JsonArrayRequest plantRequest = new JsonArrayRequest(plantUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonPlant = response.getJSONObject(i);
                        Plant plant = new Plant(jsonPlant.getString("_id"), jsonPlant.getString("username"),
                                jsonPlant.getString("plantname"), jsonPlant.getDouble("lat"),
                                jsonPlant.getDouble("lon"), jsonPlant.getString("description"));
                        plants.add(plant);

                    }
                    showPlants(plants);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(plantRequest);
    }

    private void showPlants(List<Plant> plants) {
        Drawable circleDrawable = getResources().getDrawable(R.drawable.ic_lotus_flower);
        final BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);
        mMap.clear();
        for(Plant plant : plants) {
            LatLng plantPosition = new LatLng(plant.getLat(), plant.getLon());
            mMap.addMarker(new MarkerOptions().position(plantPosition).title(plant.getName()).icon(markerIcon).snippet(plant.getDescription()));
        }

    }
    private void startTracking() {
        Intent intent1 = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        Log.i("intent1", intent1.toString());
        startService(intent1);
    }

    private void stopTracking() {
        Intent intent = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume(){
        super.onResume();

        if(mMap != null){//prevent crashing if the map doesn't exist yet (eg. on starting activity)
            mMap.clear();
            requestPlants();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {


                        mMap.setMyLocationEnabled(true);
                        onResume();
                        createLocationRequest();
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
