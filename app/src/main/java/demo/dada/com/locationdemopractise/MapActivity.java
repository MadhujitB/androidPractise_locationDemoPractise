package demo.dada.com.locationdemopractise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private Button btn_start, btn_stop;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Boolean mRequestingLocationUpdates;
    private GoogleMap mMap;
    private TextView textView_locationDisplay, textView_displayDestination;
    private ArrayList<LatLng> arrayList;
    private String displayAddress_Source, displayAddress_Destination, destinationSubLocality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        btn_start = findViewById(R.id.btn_startTracking);
        btn_start.setOnClickListener(this);
        btn_start.setVisibility(View.VISIBLE);

        btn_stop = findViewById(R.id.btn_stopTracking);
        btn_stop.setOnClickListener(this);
        btn_stop.setVisibility(View.INVISIBLE);

        textView_locationDisplay = findViewById(R.id.textView_locationDisplay);
        textView_displayDestination = findViewById(R.id.textView_displayDestination);
        textView_displayDestination.setVisibility(View.INVISIBLE);

        mRequestingLocationUpdates = false;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        arrayList = new ArrayList<>();

        displayAddress_Source = "";
        displayAddress_Destination = "";
        destinationSubLocality = "";


        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();


    }

    //Getting the current location of the
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                //Current co-ordinates of the current position
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Log.d(TAG, "Inside getLocation() \n Latitude : " + latitude + " Longitude: " + longitude);

                plotMap(latitude, longitude);
            }
        });
    }

    private void plotMap(double latitude, double longitude)
    {
        //This method will plot a marker and also display the address of the current location
        try {
            LatLng latLng = new LatLng(latitude, longitude);

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            List<Address> address = geocoder.getFromLocation(latitude, longitude, 1);


            //Getting the address of a particular location
            String addressLine = address.get(0).getAddressLine(0);
            String locality = address.get(0).getLocality();
            String subLocality = address.get(0).getSubLocality();
            String pinCode = address.get(0).getPostalCode();

            String[] addressLineSplit = addressLine.split(",");

            displayAddress_Source =   addressLineSplit[0] + ", " + addressLineSplit[1] + ", "
                                    + addressLineSplit[2] + ", " + addressLineSplit[3] + ", "
                                    + subLocality + ", " + locality + "-" + pinCode;

            mMap.addMarker(new MarkerOptions().position(latLng).title(subLocality)); //Displaying the marker to pin-point the location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)); //Moving camera to the point of location
            textView_locationDisplay.setText(displayAddress_Source); //Displaying the address of the current location
            Log.d(TAG, "Address Display: " + displayAddress_Source);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.btn_startTracking:
                btn_start.setVisibility(View.INVISIBLE);
                btn_stop.setVisibility(View.VISIBLE);
                startLocationUpdates();
                break;

            case R.id.btn_stopTracking:
                btn_start.setVisibility(View.INVISIBLE);
                btn_stop.setVisibility(View.INVISIBLE);

                String source = "SOURCE: \n" + displayAddress_Source;
                textView_locationDisplay.setText(source);

                textView_displayDestination.setText(displayAddress_Destination);
                textView_displayDestination.setVisibility(View.VISIBLE);

                mMap.addMarker(new MarkerOptions().position(arrayList.get(arrayList.size() - 1)).title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                stopLocationUpdates();
                break;
        }

    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Log.d(TAG,"Location Request Is Called");
    }

    private void createLocationCallback() {

        Log.d(TAG,"Location Callback Is Called");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();

                arrayList.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

                updateLocationUI();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    private void updateLocationUI() {
        Log.d(TAG, "Update Location Is Getting Called");
        if (mCurrentLocation != null) {

            Log.d(TAG, "Inside Update Location UI \n Latitude: " + mCurrentLocation.getLatitude() +
                    "Longitude: " + mCurrentLocation.getLongitude());
            setPolyLineTrace();
        }
        else
            Log.d(TAG, "Current Location is Null");

    }

    private void setPolyLineTrace()
    {

        try {

            //Displaying the polylines which will help in tracking the points
            PolylineOptions polylineOptions = new PolylineOptions().clickable(true);

            for(LatLng ll : arrayList) {
                polylineOptions.add(ll);
            }

            Polyline polyline = mMap.addPolyline(polylineOptions);

            polyline.setColor(ContextCompat.getColor(this, R.color.customColor_400));
            polyline.setWidth(15f);
            polyline.setEndCap(new RoundCap());

            LatLng latLng = arrayList.get(arrayList.size() - 1);
            double latitude = latLng.latitude;
            double longitude = latLng.longitude;

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            List<Address> address = geocoder.getFromLocation(latitude, longitude, 1);

            String addressLine = address.get(0).getAddressLine(0);
            String locality = address.get(0).getLocality();
            destinationSubLocality = address.get(0).getSubLocality();
            String pinCode = address.get(0).getPostalCode();

            String[] addressLineSplit = addressLine.split(",");

            displayAddress_Destination = "DESTINATION: \n " +
                    addressLineSplit[0] + ", " + addressLineSplit[1] + ", "
                    + addressLineSplit[2] + ", " + addressLineSplit[3] + ", "
                    + destinationSubLocality + ", " + locality + "-" + pinCode;


            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            Log.d(TAG, "Address Display: " + displayAddress_Destination);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }

    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.

        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        if (ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                });
    }


    private void stopLocationUpdates() {

        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                });
    }

    private void updateUI() {
        updateLocationUI();
    }

    private boolean checkPermissions() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
            mRequestingLocationUpdates = false;
        }
        else {
            mRequestingLocationUpdates = true;
            getLocation();
        }

        return mRequestingLocationUpdates;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case REQUEST_PERMISSIONS_REQUEST_CODE:

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    mRequestingLocationUpdates = true;
                    getLocation();
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!checkPermissions())
            checkPermissions();
        else
            getLocation();

        updateUI();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
