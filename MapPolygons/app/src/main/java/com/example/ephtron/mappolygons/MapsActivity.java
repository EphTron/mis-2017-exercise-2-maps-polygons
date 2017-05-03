package com.example.ephtron.mappolygons;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private SharedPreferences markerStorage;
    private Map markers = new TreeMap<>();
    private Map polygonMarkers = new TreeMap<>();
    private Integer markerId = 0;


    private GoogleMap mMap;
    private EditText markerText;
    private Button clearButton;
    private Button polygonButton;
    private Boolean polygonState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        markerText = (EditText) findViewById(R.id.marker_text);
        clearButton = (Button) findViewById(R.id.clear_markers);
        polygonButton = (Button) findViewById(R.id.polygon_button);
        polygonState = false;

        // Simple Surface and Centroid Test
        Map test = new TreeMap<>();
        test.put(1,new LatLng(0,0));
        test.put(2,new LatLng(10,0));
        test.put(3,new LatLng(0,10));
        System.out.println("Surface "+ calculateSurfaceArea(test));
        System.out.println("Centroid "+ calculateCentroid(test));
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
        setupMarkersOnMap();
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMarkers();
            }
        });

        polygonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (polygonState) {
                    polygonButton.setText("Start Polygon");
                    polygonState = false;
                } else {
                    polygonButton.setText("End Polygon");
                    polygonState = true;
                }
            }
        });

        // Add a marker in Sydney and move the camera
        LatLng worldOrigin = new LatLng(50.97295863175768, 11.329278722405434);
        createMarker(worldOrigin, "B11- Weimar");
        mMap.moveCamera(CameraUpdateFactory.newLatLng(worldOrigin));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                String text = markerText.getText().toString();
                if (createMarker(latLng, text) == true) {
                    Toast.makeText(getApplicationContext(),
                            R.string.marker_creation_success,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.marker_creation_failed,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean createMarker(LatLng latLng, String text) {
        SharedPreferences.Editor markerEditor = markerStorage.edit();

        if (!text.equals("")) {
            mMap.addMarker(new MarkerOptions().position(latLng).title(text));
            markers.put(markerId, latLng);

            markerEditor.putString("Marker_" + markerId + "_txt", text);
            markerEditor.putLong("Marker_" + markerId + "_lat",
                    Double.doubleToRawLongBits(latLng.latitude));
            markerEditor.putLong("Marker_" + markerId + "_lng",
                    Double.doubleToRawLongBits(latLng.longitude));
            markerEditor.commit();
            System.out.println("Added marker:"
                    + latLng.latitude
                    + " "
                    + latLng.longitude);

            markerText.setText("");
            markerId++;
            return true;
        } else {
            return false;
        }
    }

    private void deleteMarkers(){
        SharedPreferences.Editor markerEditor = markerStorage.edit();
        markerEditor.clear();
        markerEditor.commit();
        mMap.clear();
        Toast.makeText(getApplicationContext(),
                R.string.cleared_markers,
                Toast.LENGTH_LONG).show();

    }

    private void setupMarkersOnMap(){
        markerStorage = getSharedPreferences(getString(R.string.marker_storage),
                Context.MODE_PRIVATE);
        String markerTag = "Marker_"+markerId;

        while( markerStorage.contains(markerTag+"_txt")){
            String txt = markerStorage.getString(markerTag+"_txt","Error");
            Double lat = toDouble(markerStorage.getLong(markerTag + "_lat", 0));
            Double lng = toDouble(markerStorage.getLong(markerTag + "_lng", 0));
            LatLng pos = new LatLng(lat,lng);
            markers.put(markerId, pos);

            markerId++;
            markerTag = "Marker_"+markerId;

            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(txt));
        }
    }

    private double toDouble(Long val) {
        return Double.longBitsToDouble(val);
    }

    // http://www.seas.upenn.edu/~sys502/extra_materials/Polygon%20Area%20and%20Centroid.pdf (3.5.17)
    private double calculateSurfaceArea(Map markerMap){
        if (markerMap.size() >= 3) {
            List<LatLng> markerList = new ArrayList<>(markerMap.values());
            double surfaceArea = 0;
            for (int i = 0; i < markerList.size() - 1; i++) {
                surfaceArea += markerList.get(i).latitude * markerList.get(i + 1).longitude
                        - markerList.get(i + 1).latitude * markerList.get(i).longitude;
            }
            return 0.5 * surfaceArea;
        } else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_enough_markers,
                    Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    // http://www.seas.upenn.edu/~sys502/extra_materials/Polygon%20Area%20and%20Centroid.pdf (3.5.17)
    private LatLng calculateCentroid(Map markerMap){
        LatLng centroid = new LatLng(0,0);
        if (markerMap.size() >= 3) {
            List<LatLng> markerList = new ArrayList<>(markerMap.values());
            double a = calculateSurfaceArea(markerMap);
            double x = (1/(6*a)) * helpCentroidXCoordinate(markerList);
            double y = (1/(6*a)) * helpCentroidYCoordinate(markerList);
            centroid = new LatLng(x,y);
            return centroid;
        } else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_enough_markers,
                    Toast.LENGTH_LONG).show();
            return centroid;
        }
    }

    private double helpCentroidXCoordinate(List<LatLng> markerList) {
        double x = 0;
        for (int i = 0; i < markerList.size() - 1; i++) {
            x += (markerList.get(i).latitude + markerList.get(i + 1).latitude)
                    * (markerList.get(i).latitude * markerList.get(i + 1).longitude
                    - markerList.get(i+1).latitude * markerList.get(i).longitude);
        }
        return x;
    }

    private double helpCentroidYCoordinate(List<LatLng> markerList) {
        double y = 0;
        for (int i = 0; i < markerList.size() - 1; i++) {
            y += (markerList.get(i).longitude + markerList.get(i + 1).longitude)
                    * ((markerList.get(i).latitude * markerList.get(i + 1).longitude)
                    - (markerList.get(i+1).latitude * markerList.get(i).longitude));
        }
        return y;
    }
}

