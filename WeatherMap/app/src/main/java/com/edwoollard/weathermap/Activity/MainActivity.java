package com.edwoollard.weathermap.Activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edwoollard.weathermap.Utils.Cache;
import com.edwoollard.weathermap.Utils.DownloadImageTask;
import com.edwoollard.weathermap.Utils.JSONParser;
import com.edwoollard.weathermap.R;
import com.edwoollard.weathermap.Model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final static String WEATHER_MAP_APP_ID = "4bcfe0ba8f12acbaab0881fa6810a549";
    private final static int FINE_LOCATION_PERMISSION_CODE = 1;
    private TextView currentCondition;
    private TextView temperature;
    private TextView windSpeed;
    private TextView windDirection;
    private ImageView currentConditionImage;
    private TextView lastUpdated;
    private FloatingActionButton fab;
    private String lastUpdatedDate;
    private LinearLayout weatherLayout;
    private TextView city;
    private TextView noPreviousWeather;
    private Location userLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
    public final static long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Define layout items
        fab = (FloatingActionButton) findViewById(R.id.fab);
        currentCondition = (TextView) findViewById(R.id.currentCondition);
        temperature = (TextView) findViewById(R.id.temperature);
        windSpeed = (TextView) findViewById(R.id.windSpeed);
        windDirection = (TextView) findViewById(R.id.windDirection);
        currentConditionImage = (ImageView) findViewById(R.id.currentConditionImage);
        lastUpdated = (TextView) findViewById(R.id.lastUpdated);
        weatherLayout = (LinearLayout) findViewById(R.id.weatherLayout);
        city = (TextView) findViewById(R.id.city);
        noPreviousWeather = (TextView) findViewById(R.id.noPreviousWeather);

        // Ensure the location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_CODE);
            }
        } else {
            // Location permission has been previously granted
            setupLocationFunctionality();
        }
    }

    /**
     * Sets up weather view using flow of checking for internet and whether to read cached data or fetch new using JSON call
     */
    public void updateWeatherView() {
        if (!isNetworkAvailable()) {
            boolean read = readCachedDataDate();
            if (read) {
                boolean twentyFourHoursOld = isDate24hOld(lastUpdatedDate);
                if (!twentyFourHoursOld) {
                    // If last updated date is less than 24 hours then display cached data
                    switchView(true);
                    readCachedData();
                } else {
                    // Show TextView explaining there is no previous data
                    switchView(false);
                }
            } else {
                // Show TextView explaining there is no previous data
                switchView(false);
            }
        } else {
            // Refresh the weather data of the user's location if they have network
            getCityName(userLocation, new OnGeocoderFinishedListener() {
                @Override
                public void onFinished(List<Address> results) {
                    switchView(true);
                    city.setText(results.get(0).getLocality());
                    setLastUpdated();
                    String url = "http://api.openweathermap.org/data/2.5/weather?q=" + results.get(0).getLocality() + "&units=metric&APPID=" + WEATHER_MAP_APP_ID;
                    new ConnectAsyncTask(MainActivity.this, url).execute();
                }
            });
        }
    }

    /**
     * Sets the last updated date using the current date and time
     */
    public void setLastUpdated() {
        lastUpdatedDate = dateFormat.format(new Date());
        lastUpdated.setText(getResources().getString(R.string.last_updated) + lastUpdatedDate);
    }

    /**
     * Switches the view between the current weather and no weather depending on if there is data
     * @param dataToDisplay - whether there is weather data to display or not
     */
    public void switchView(boolean dataToDisplay) {
        if (dataToDisplay) {
            weatherLayout.setVisibility(View.VISIBLE);
            city.setVisibility(View.VISIBLE);
            lastUpdated.setVisibility(View.VISIBLE);
            noPreviousWeather.setVisibility(View.GONE);
        } else {
            // Show TextView explaining there is no previous data
            weatherLayout.setVisibility(View.GONE);
            city.setVisibility(View.GONE);
            lastUpdated.setVisibility(View.GONE);
            noPreviousWeather.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Starts requesting the user's location and checks permission is granted
     */
    public void startRequestingLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_CODE);
            }
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    /**
     * Gets the last known location of the user and checks permission is granted
     * @return the last known location as a Location object
     */
    public Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_CODE);
            }
        }
        return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Checks whether the network is currently available or not
     * @return whether the network is available
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * AsyncTask used to connect and gain JSON result information about the current weather
     */
    private class ConnectAsyncTask extends AsyncTask<Void, Void, String> {

        private ProgressDialog progressDialog;
        private Context context;
        private String url;

        ConnectAsyncTask(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(getResources().getText(R.string.fetching_forecast));
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            constructWeatherView(result);
            progressDialog.hide();
        }
    }

    /**
     * Constructs the view holding the weather details from the JSON returned result
     * @param result - JSON result containing weather details
     */
    public void constructWeatherView(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);

            // Populate current condition TextView
            JSONArray jsonArrayWeather = jsonObject.getJSONArray("weather");
            JSONObject currentConditionObject = jsonArrayWeather.getJSONObject(0);
            String currentConditionValue = currentConditionObject.getString("main");
            currentCondition.setText(currentConditionValue);

            // Populate current condition ImageView
            String currentConditionImageValue = currentConditionObject.getString("icon");
            new DownloadImageTask(currentConditionImage)
                    .execute("http://openweathermap.org/img/w/" + currentConditionImageValue + ".png");

            // Populate temperature TextView
            JSONObject jsonObjectTemperature = jsonObject.getJSONObject("main");
            double temperatureValue = jsonObjectTemperature.getDouble("temp");
            temperature.setText(temperatureValue + getResources().getString(R.string.degrees_celsius_unit));

            // Populate wind speed TextView
            JSONObject jsonObjectWind = jsonObject.getJSONObject("wind");
            double windSpeedValue = jsonObjectWind.getDouble("speed");
            windSpeed.setText(windSpeedValue + getResources().getString(R.string.mph_unit));

            // Populate wind direction TextView
            double windDirectionValue = jsonObjectWind.getDouble("deg");
            String cardinalDirection = compassSection(windDirectionValue);
            windDirection.setText(cardinalDirection);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes in degrees and works out the cardinal direction the wind comes from
     * @param degrees - double holding the degree the wind is coming from
     * @return String detailing which cardinal direction the degree points towards
     */
    public String compassSection(double degrees) {
        String directions[] = {"North", "North East", "East", "South East", "South", "South West", "West", "North West"};
        // Calculates which cardinal direction the wind direction degrees provided fits under
        return directions[(int) Math.round(((degrees % 360) / 45)) % 8];
    }

    @Override
    protected void onStop() {
        super.onStop();
        writeCachedData();
    }

    /**
     * Constructs Weather object and writes it into the cache
     */
    public void writeCachedData() {
        // Get the bitmap from the current weather condition ImageView
        Bitmap bitmap = null;
        if (((BitmapDrawable) currentConditionImage.getDrawable()).getBitmap() != null) {
            bitmap = ((BitmapDrawable) currentConditionImage.getDrawable()).getBitmap();
        }

        // Remove units from temperature and wind speed
        if (!temperature.getText().toString().isEmpty()) {
            String temp = temperature.getText().toString();
            temp = temp.substring(0, temp.length() - 2);
            String windSp = windSpeed.getText().toString();
            windSp = windSp.substring(0, windSp.length() - 3);

            // Construct weather object to place into the cache
            Weather weather = new Weather(currentCondition.getText().toString(),
                    Double.valueOf(temp), Double.valueOf(windSp),
                    windDirection.getText().toString(), bitmap, lastUpdatedDate, city.getText().toString());

            // Save weather item to cache so it can be retrieved later with the key
            Cache.getInstance().getLru().put("weather", weather);
        } else {

        }
    }

    /**
     * Reads cached data into a Weather object to then repopulate the view
     */
    public void readCachedData() {
        // Construct weather object from the object placed in cache
        Weather weather = (Weather) Cache.getInstance().getLru().get("weather");

        if (weather != null) {
            // Update UI with relevant data from the cached object
            currentCondition.setText(weather.getCurrentCondition());
            temperature.setText(String.valueOf(weather.getTemperature()) + getResources().getString(R.string.degrees_celsius_unit));
            windSpeed.setText(String.valueOf(weather.getWindSpeed()) + getResources().getString(R.string.mph_unit));
            windDirection.setText(weather.getWindDirection());
            currentConditionImage.setImageBitmap(weather.getCurrentConditionImage());
            lastUpdated.setText(getResources().getString(R.string.last_updated) + weather.getLastUpdatedDate());
            city.setText(weather.getCity());
        }
    }

    /**
     * Reads the date that the last data was cached
     * @return whether cached data date was able to be read
     */
    public boolean readCachedDataDate() {
        // Construct weather object from the object placed in cache
        Weather weather = (Weather) Cache.getInstance().getLru().get("weather");

        if (weather != null) {
            lastUpdatedDate = weather.getLastUpdatedDate().toString();
            return true;
        }
        return false;
    }

    /**
     * Check if date is more or less than 24 hours old
     * @param stringDate - the date to check
     * @return whether the date is at least 24 hours old
     */
    public boolean isDate24hOld(String stringDate) {
        // Check if date is more or less than 24 hours old
        Date date = null;
        Date currentDate = new Date();
        try {
            date = dateFormat.parse(stringDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return Math.abs(date.getTime() - currentDate.getTime()) > MILLIS_PER_DAY;
    }

    /**
     * Gets the name of the closest city to the provided location
     * @param location - user's current location
     * @param listener - geocoder listener
     */
    public void getCityName(final Location location, final OnGeocoderFinishedListener listener) {
        new AsyncTask<Void, Integer, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... arg0) {
                Geocoder coder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
                List<Address> results = null;
                try {
                    results = coder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return results;
            }

            @Override
            protected void onPostExecute(List<Address> results) {
                if (results != null && listener != null) {
                    listener.onFinished(results);
                }
            }
        }.execute();
    }

    public abstract class OnGeocoderFinishedListener {
        public abstract void onFinished(List<Address> results);
    }

    /**
     * Sets up all of the app related functionality that relies on location
     */
    public void setupLocationFunctionality() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Auto-assign the user's last known location
        userLocation = getLastKnownLocation();

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider
                userLocation = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        startRequestingLocation();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNetworkAvailable()) {
                    // Show snackbar informing the user to connect to the internet if network is down
                    Snackbar.make(view, "There is no network available. Please connect to the internet and retry.", Snackbar.LENGTH_LONG).show();
                } else {
                    // Refresh the weather data of the user's location
                    getCityName(userLocation, new OnGeocoderFinishedListener() {
                        @Override
                        public void onFinished(List<Address> results) {
                            switchView(true);
                            city.setText(results.get(0).getLocality());
                            setLastUpdated();
                            String url = "http://api.openweathermap.org/data/2.5/weather?q=" + results.get(0).getLocality() + "&units=metric&APPID=" + WEATHER_MAP_APP_ID;
                            new ConnectAsyncTask(MainActivity.this, url).execute();
                        }
                    });
                }
            }
        });

        updateWeatherView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupLocationFunctionality();
                }
                return;
            }
        }
    }
}