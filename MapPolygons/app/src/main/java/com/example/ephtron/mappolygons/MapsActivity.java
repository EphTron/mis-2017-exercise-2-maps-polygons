package com.example.ephtron.mappolygons;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;

import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    protected static final String TAG = "";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private SharedPreferences markerStorage;
    private Map markers = new TreeMap<>();
    private Map polygons = new TreeMap<>();
    private List currentPolygon = new ArrayList<>();
    private Integer markerId = 0;
    private Integer polygonMarkerId = 0;
    private Boolean polygonState;

    private Integer polygonId = 0;
    private GoogleMap mMap;
    private EditText markerText;
    private Button clearButton;
    private Button polygonButton;

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Create an instance of GoogleAPIClient.
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        markerText = (EditText) findViewById(R.id.marker_text);
        clearButton = (Button) findViewById(R.id.clear_markers);
        polygonButton = (Button) findViewById(R.id.polygon_button);
        polygonState = false;


        // Simple Surface and Centroid Test
        List test = new ArrayList<LatLng>();
        test.add(new LatLng(0, 0));
        test.add(new LatLng(10, 0));
        test.add(new LatLng(0, 10));
        double test_area = calculateSurfaceArea(test);
        System.out.println("Surface " + test_area);
        System.out.println("Centroid " + calculateCentroid(test, test_area));


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // https://developer.android.com/training/location/retrieve-current.html (4.5.17)
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        float zoom = 14;
        Toast.makeText(this, "Permission granted. Loading location.", Toast.LENGTH_LONG).show();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude())).title("Current Position"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude()), zoom));
        } else {
            Toast.makeText(this, R.string.no_location, Toast.LENGTH_LONG).show();
            LatLng currentLocation = new LatLng(50.97295863175768, 11.329278722405434);
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("B11"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add Markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

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
                if (polygonState) { // End
                    if (currentPolygon.size() > 2) {
                        polygonState = false;
                        polygonButton.setText("Start Polygon");
                        createPolygon();
                        currentPolygon.clear();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.not_enough_markers,
                                Toast.LENGTH_LONG).show();
                    }
                } else { // Start
                    polygonState = true;
                    polygonButton.setText("End Polygon");
                }
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (polygonState) {
                    createPolygonMarker(latLng);
                } else {
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

    private boolean createPolygonMarker(LatLng latLng) {
        currentPolygon.add(latLng);

        MarkerOptions polygonMarker = new MarkerOptions().position(latLng).title("Polygon_#"
                + polygonId + " PolyMarker_#" + polygonMarkerId);
        // Changing marker icon
        polygonMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        mMap.addMarker(polygonMarker);

        System.out.println("Added polygon marker:"
                + latLng.latitude
                + " "
                + latLng.longitude);
        polygonMarkerId++;
        return true;
    }

    private void createPolygon() {
        PolygonOptions polyOptions = new PolygonOptions();
        polyOptions.fillColor(0x7777ff88);
        for (int i = 0; i < currentPolygon.size(); i++) {
            LatLng pos = (LatLng) currentPolygon.get(i);
            polyOptions.add(pos);
        }
        Polygon polygon = mMap.addPolygon(polyOptions);
        polygons.put(polygonId, polygon);

        // double area = calculateSurfaceArea(currentPolygon);
        // http://stackoverflow.com/questions/19396295/android-calculate-the-area-of-polygon (4.5.17)
        // http://stackoverflow.com/questions/30395825/how-do-i-import-com-google-maps-android-sphericalutil-in-android-studio (4.5.17)
        double area = SphericalUtil.computeArea(polyOptions.getPoints());

        //LatLng polygonCenter = calculateCentroid(currentPolygon, area);
        LatLng polygonCenter = calculateCentroid2(currentPolygon);

        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        String polyMarkerText = "Area: " + df.format(area) + "m²";
        Toast.makeText(this, polyMarkerText, Toast.LENGTH_LONG).show();
        mMap.addMarker(new MarkerOptions().position(polygonCenter).title(polyMarkerText));

        polygonId++;
        polygonMarkerId = 0;
    }

    private void deleteMarkers() {
        SharedPreferences.Editor markerEditor = markerStorage.edit();
        markerEditor.clear();
        markerEditor.commit();
        mMap.clear();
        Toast.makeText(getApplicationContext(),
                R.string.cleared_markers,
                Toast.LENGTH_LONG).show();
    }

    private void setupMarkersOnMap() {
        markerStorage = getSharedPreferences(getString(R.string.marker_storage),
                Context.MODE_PRIVATE);
        String markerTag = "Marker_" + markerId;

        while (markerStorage.contains(markerTag + "_txt")) {
            String txt = markerStorage.getString(markerTag + "_txt", "Error");
            Double lat = toDouble(markerStorage.getLong(markerTag + "_lat", 0));
            Double lng = toDouble(markerStorage.getLong(markerTag + "_lng", 0));
            LatLng pos = new LatLng(lat, lng);
            markers.put(markerId, pos);

            markerId++;
            markerTag = "Marker_" + markerId;

            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(txt));
        }
    }

    private double toDouble(Long val) {
        return Double.longBitsToDouble(val);
    }

    // http://www.seas.upenn.edu/~sys502/extra_materials/Polygon%20Area%20and%20Centroid.pdf (3.5.17)
    private double calculateSurfaceArea(List<LatLng> markerList) {
        if (markerList.size() >= 3) {
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
    // not working properly on LatLng, therefore I chose another approach - see calculateCantroid2
    private LatLng calculateCentroid(List<LatLng> markerList, double a) {
        LatLng centroid = new LatLng(0, 0);
        if (markerList.size() >= 3) {
            double x = (1 / (6 * a)) * helpCentroidXCoordinate(markerList);
            double y = (1 / (6 * a)) * helpCentroidYCoordinate(markerList);
            centroid = new LatLng(x, y);
            return centroid;
        } else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_enough_markers,
                    Toast.LENGTH_LONG).show();
            return centroid;
        }
    }

    // http://www.androiddevelopersolutions.com/2015/02/android-calculate-center-of-polygon-in.html (4.5.17)
    private LatLng calculateCentroid2(List<LatLng> markerList) {
        double x = 0;
        double y = 0;
        for (int i = 0; i < markerList.size(); i++) {
            x += markerList.get(i).latitude;
            y += markerList.get(i).longitude;
        }
        x = (x / markerList.size());
        y = (y / markerList.size());
        LatLng centroid = new LatLng(x, y);
        return centroid;
    }

    private double helpCentroidXCoordinate(List<LatLng> markerList) {
        double x = 0;
        for (int i = 0; i < markerList.size() - 1; i++) {
            x += (markerList.get(i).latitude + markerList.get(i + 1).latitude)
                    * (markerList.get(i).latitude * markerList.get(i + 1).longitude
                    - markerList.get(i + 1).latitude * markerList.get(i).longitude);
        }
        return x;
    }

    private double helpCentroidYCoordinate(List<LatLng> markerList) {
        double y = 0;
        for (int i = 0; i < markerList.size() - 1; i++) {
            y += (markerList.get(i).longitude + markerList.get(i + 1).longitude)
                    * ((markerList.get(i).latitude * markerList.get(i + 1).longitude)
                    - (markerList.get(i + 1).latitude * markerList.get(i).longitude));
        }
        return y;
    }

    // http://stackoverflow.com/questions/41449652/activitycompat-requestpermissions (4.5.17)
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

}


