package com.example.togames.finalproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback, ConnectionCallbacks,
        OnConnectionFailedListener, View.OnClickListener,
        GoogleMap.OnMapLongClickListener {

    private static final int CLEAR_DAY = 0, CLEAR_NIGHT = 1, FEW_CLOUDS = 2, SCATTERED_CLOUDS = 3,
            BROKEN_CLOUDS = 4, SHOWER_RAIN = 5, RAIN = 6, THUNDERSTORM = 7, SNOW = 8, MIST = 9;

    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int WEATHER_REQUEST_INTERVAL = 60000;
    private static final int WEATHER_REQUEST_RATE =
            WEATHER_REQUEST_INTERVAL / LOCATION_REQUEST_INTERVAL;

    private String REQUESTING_LOCATION_UPDATES_KEY = "location";

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Address address;
    private boolean mRequestingLocationUpdates;
    private int locationResultCount = 0;
    private Marker mCurrLocationMarker;
    private FirebaseAuth auth;

    private ImageButton imageButton_maps_back;
    private ImageView imageView_weather, imageView_humidity;
    private TextView textView_locationText, textView_temp, textView_humidity;
    private FloatingActionButton floatingButton_location;
    private ConstraintLayout constraintLayout_weather;

    private Bitmap current_location_icon;
    private Drawable[] weatherIcons;

    private ArrayList<LatLng> markerPoints;
    private boolean isDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isDark = AppSettings.getInstance(this).isDarkTheme;
        setTheme(isDark ? R.style.NoTitleThemeDark : R.style.NoTitleTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        imageButton_maps_back = findViewById(R.id.imageButton_maps_back);
        imageView_weather = findViewById(R.id.imageView_weather);
        imageView_humidity = findViewById(R.id.imageView_humidity);
        textView_temp = findViewById(R.id.textView_temp);
        textView_humidity = findViewById(R.id.textView_humidity);
        textView_locationText = findViewById(R.id.textView_locationText);
        floatingButton_location = findViewById(R.id.floatingButton_location);
        constraintLayout_weather = findViewById(R.id.constraintLayout_weather);
        imageButton_maps_back.setOnClickListener(this);
        floatingButton_location.setOnClickListener(this);

        markerPoints = new ArrayList<>(3);

        // Load pre-saved data
        updateValuesFromBundle(savedInstanceState);

        // Initialize and set the elements of weather icons array
        initializeWeatherIcons();

        // Get Firebase Authenticator
        auth = FirebaseAuth.getInstance();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create Location Callback to receive new locations
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    // Update the GUI according to location
                    mLastLocation = location;
                    double latitude = mLastLocation.getLatitude();
                    double longitude = mLastLocation.getLongitude();
                    LatLng currentPosition = new LatLng(latitude, longitude);
                    drawCurrentLocationIcon(currentPosition);
                    if (locationResultCount == 0) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
                    }
                    Log.d("onLocationResult", "location received");
                }

                if (mLastLocation != null) {
                    double latitude = mLastLocation.getLatitude();
                    double longitude = mLastLocation.getLongitude();

                    setAddressText(latitude, longitude);

                    if (locationResultCount % WEATHER_REQUEST_RATE == 0) {
                        locationResultCount = 0;
                        // Request weather from OpenWeatherMap
                        String sUrl = "http://api.openweathermap.org/data/2.5/weather?"
                                + "lat=" + latitude + "&lon=" + longitude
                                + "&APPID=" + getString(R.string.open_weather_map_key);
                        WeatherTask weatherTask = new WeatherTask();
                        weatherTask.execute(sUrl);

                        Log.d("onLocationResult", "weather requested");
                    }
                    constraintLayout_weather.setVisibility(View.VISIBLE);
                    textView_locationText.setVisibility(View.VISIBLE);
                }

                locationResultCount++;
            }
        };
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of mRequestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }
    }

    private void initializeWeatherIcons() {
        weatherIcons = new Drawable[10];
        weatherIcons[CLEAR_DAY] = getResources().
                getDrawable(R.drawable.finalproject_weather_clear_day_icon);
        weatherIcons[CLEAR_NIGHT] = getResources().
                getDrawable(R.drawable.finalproject_weather_clear_night_icon);
        weatherIcons[FEW_CLOUDS] = getResources().
                getDrawable(R.drawable.finalproject_weather_few_clouds_icon);
        weatherIcons[SCATTERED_CLOUDS] = getResources().
                getDrawable(R.drawable.finalproject_weather_clouds_icon);
        weatherIcons[BROKEN_CLOUDS] = getResources().
                getDrawable(R.drawable.finalproject_weather_broken_clouds_icon);
        weatherIcons[SHOWER_RAIN] = getResources().
                getDrawable(R.drawable.finalproject_weather_rain_icon);
        weatherIcons[RAIN] = getResources().
                getDrawable(R.drawable.finalproject_weather_shower_rain_icon);
        weatherIcons[THUNDERSTORM] = getResources().
                getDrawable(R.drawable.finalproject_weather_thunderstorm_icon);
        weatherIcons[SNOW] = getResources().
                getDrawable(R.drawable.finalproject_weather_snow_icon);
        weatherIcons[MIST] = getResources().
                getDrawable(R.drawable.finalproject_weather_mist_icon);
    }

    public void drawCurrentLocationIcon(LatLng currentPosition) {
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        MarkerOptions markerOptions = new MarkerOptions().position(currentPosition).
                title(getString(R.string.current_position)).
                icon(BitmapDescriptorFactory.
                        fromBitmap(current_location_icon));

        mCurrLocationMarker = mMap.addMarker(markerOptions);
    }

    /*
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Log.v("TAG", "onMapReady is called");
        mMap = googleMap;
        if (isDark) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_dark_mode));
        }
        mMap.setOnMapLongClickListener(this);
        //mMap.setMyLocationEnabled(true);
        //mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set the current location icon
        current_location_icon = Bitmap.createScaledBitmap(((BitmapDrawable) (getResources().
                getDrawable(R.drawable.finalproject_current_location_icon))).
                getBitmap(), 90, 120, false);

        // Build and connect to Google Api for GPS location finding
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        //Log.v("TAG", "buildGoogleApiClient is called");
        mGoogleApiClient = new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Log.v("TAG", "onConnected is called");
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.v("TAG", "onConnectionSuspended is called");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.v("TAG", "onConnectionFailed is called");
    }

    private void createLocationRequest() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    Log.d("testPermission", "onFailure");
                    checkLocationPermission();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            createLocationRequest();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mRequestingLocationUpdates = true;
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);

        super.onSaveInstanceState(outState);
    }


    private void checkLocationPermission() {
        Log.d("testPermission", "checkLocationPermission");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("testPermission", "permissionNotGranted");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("testPermission", "showRequestPermissionRationale");

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
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        1);
                            }
                        })
                        .create()
                        .show();
            } else {
                Log.d("testPermission", "notShowRequestPermissionRationale");
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: { // Location permission
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // if granted
                    Toast.makeText(getApplicationContext(), R.string.location_permission_granted,
                            Toast.LENGTH_SHORT).show();
                    startLocationUpdates();
                } else { // if not granted
                    Toast.makeText(getApplicationContext(), R.string.location_permission_denied,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imageButton_maps_back) {
            // Finish current activity
            finish();
        } else if (id == R.id.floatingButton_location) {
            if (mLastLocation != null) {
                LatLng currentPosition =
                        new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            }
        }
    }

    public void setAddressText(double lat, double lon) {
        // Create a geocoder
        // Get and print the address line using latitude and longitude
        String result;
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            address = geocoder.getFromLocation(lat, lon, 1).get(0);
            result = address.getAddressLine(0);
        } catch (IOException e) {
            result = getString(R.string.address_not_found);
        }
        textView_locationText.setText(result);
    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        if (markerPoints.size() > 1) {
            markerPoints.clear();
            mMap.clear();
            if (mLastLocation != null) {
                LatLng currentPosition = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                drawCurrentLocationIcon(currentPosition);
            }
        }

        // Add new LatLng to the ArrayList
        markerPoints.add(latLng);
        // Create MarkerOptions
        MarkerOptions options = new MarkerOptions();
        // Set the position of the marker
        options.position(latLng);

        if (markerPoints.size() == 1) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            // Move the map camera
        } else if (markerPoints.size() == 2) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
        // Add new marker
        mMap.addMarker(options);

        // Check if start and end locations are captured
        if (markerPoints.size() >= 2) {
            LatLng origin = markerPoints.get(0);
            LatLng dest = markerPoints.get(1);

            mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

            // Get the URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            // Download json data from Google Directions API
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class WeatherTask extends AsyncTask<String, Integer, String[]> {

        @Override
        protected String[] doInBackground(String... url) {
            String[] data = new String[3];
            try {
                data = getDataAndParse(url[0]);
            } catch (Exception e) {
                Log.d("Weather Background", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            // Set temperature text
            String sTemp = result[0] + " °C";
            textView_temp.setText(sTemp);

            // Set humidity text
            String sHumidity = result[1]; // + " %";
            textView_humidity.setText(sHumidity);

            // Set weather drawable according to the received weather id
            Drawable tempDrawable = null;
            int id = Integer.parseInt(result[2]);
            if (id / 100 == 2) {
                tempDrawable = weatherIcons[THUNDERSTORM];
            } else if (id / 100 == 3) {
                tempDrawable = weatherIcons[SHOWER_RAIN];
            } else if (id / 100 == 5) {
                if (id < 511) {
                    tempDrawable = weatherIcons[RAIN];
                } else {
                    tempDrawable = weatherIcons[SHOWER_RAIN];
                }
            } else if (id / 100 == 6) {
                tempDrawable = weatherIcons[SNOW];
            } else if (id / 100 == 7) {
                tempDrawable = weatherIcons[MIST];
            } else if (id == 800) {
                tempDrawable = weatherIcons[CLEAR_DAY];
            } else if (id == 801) {
                tempDrawable = weatherIcons[FEW_CLOUDS];
            } else if (id == 802) {
                tempDrawable = weatherIcons[SCATTERED_CLOUDS];
            } else if (id == 803 || id == 804) {
                tempDrawable = weatherIcons[BROKEN_CLOUDS];
            }

            // Print the weather drawable on weather image view
            if (tempDrawable == null) {
                imageView_weather.setImageDrawable(weatherIcons[CLEAR_NIGHT]);
            } else {
                imageView_weather.setImageDrawable(tempDrawable);
            }

            // Show the humidity image view
            imageView_humidity.setVisibility(View.VISIBLE);
        }

        private String[] getDataAndParse(String sUrl) {
            // Connect to OpenWeatherMap Api via HttpUrlConnection,
            // get JSON for temperature, humidity and weather id.
            // Then parse and return the result.
            BufferedReader rd = null;
            HttpURLConnection connection = null;
            String[] result = new String[3];
            try {
                URL url = new URL(sUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String content = "", line;
                while ((line = rd.readLine()) != null) {
                    content += line + "\n";
                }
                JSONObject json = new JSONObject(content);
                // Parse JSON for temperature
                result[0] = Integer.toString(Math.round(Float.parseFloat(json.getJSONObject("main").
                        getString("temp")) - 273.15f));
                // Parse JSON for humidity
                result[1] = Integer.toString(Math.round(Float.parseFloat(json.getJSONObject("main").
                        getString("humidity"))));
                // Parse JSON for weather id
                result[2] = json.getJSONArray("weather").getJSONObject(0).
                        getString("id");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (rd != null) {
                    try {
                        // Close the buffered reader
                        rd.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                // Close the connection if it exists
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Download Background", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parse the data in a new thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            //MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.parseColor("#003663"));
                lineOptions.geodesic(true);
            }

            // Draw polyline on the map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Initialize the parameters
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String mode = "mode=walking";

        // Merge the parameters of the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Return the url of the web service
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    private String downloadUrl(String strUrl) throws IOException {
        // Connect to the url and download the content
        String data = "";
        InputStream iStream = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(strUrl);
            connection = (HttpURLConnection) url.openConnection();
            //connection.setRequestMethod("POST");
            //connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            iStream = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();

            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return data;
    }

}