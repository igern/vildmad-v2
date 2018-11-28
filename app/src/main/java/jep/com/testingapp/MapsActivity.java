package jep.com.testingapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RequestQueue queue;
    private List<Plant> plants = new ArrayList<>();
    private SharedPreferences sp;
    private String plantUrl = "https://mysterious-fjord-16136.herokuapp.com/api/Plants/";

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
        requestPlants();
        if(!sp.getBoolean("logged", false)) {
            return;
        }
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
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
                                            plant = new Plant(jsonPlant.getString("_id"), jsonPlant.getString("username"), jsonPlant.getString("plantname"), Float.parseFloat(jsonPlant.getString("lat")),Float.parseFloat(jsonPlant.getString("lon")),jsonPlant.getString("description"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if(plant != null) {
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
        mMap.clear();
        for(Plant plant : plants) {
            LatLng plantPosition = new LatLng(plant.getLat(), plant.getLon());
            mMap.addMarker(new MarkerOptions().position(plantPosition).title(plant.getName()));
        }
    }
}
