package com.example.jcheon.slugloop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //how to implement: index from int array will correspond to index from stops array.
    //if a bus is in, for example, section 27, then the next stop will be stop 30 or 25,
    //depending on the direction.
    int[] stopIdCCW = {5, 9, 13, 16, 19, 21, 25, 30, 33, 41, 44, 48, 53, 59, 61, 999};
    int[] stopIDCW = {64, 61, 55, 46, 41, 36, 33, 30, 25, 18, 13, 9, 5};

    String[] stopsCCW = {
            "Lower Campus", "The Farm / Village", "East Remote Parking", "East Field House",
            "Bookstore, Cowell & Stevenson (North)", "Crown & Merrill College", "College 9 & 10 / Health Center",
            "Science Hill", "Kresge College", "Rachel Carson College & Porter",
            "Family Student Housing", "Oakes College (South)", "Arboretum (North)", "Tosca Terrace", "High & Western Dr",
            "Parking Lot"
    };

    String[] stopsCW = {"Main Entrance", "High & Western Dr", "Arboretum (South)", "Oakes College (North)",
            "Rachel Carson College & Porter", "Kerr Hall", "Kresge College", "Science Hill", "College 9 & 10 / Health Center",
            "Bookstore, Cowell & Stevenson (South)", "East Remote Parking", "The Farm / Village", "Lower Campus"
    };

    static Integer[] stopIdSpinner = {53, 55, 19, 18, 21, 25, 16, 13, 44, 61, 36, 33, 5, 64, 46, 48, 41, 33, 9, 59};

    private static final String LOG_TAG = "loop";

    static RequestQueue queue;

    public MainActivity() throws JSONException {
    }

    private class ListElement {
        ListElement(String tl, String subtl, String subtl2, String dtl, String subdtl) {
            title = tl;
            subtitle = subtl;
            subtitle2 = subtl2;
            miniute = dtl;
            subdetail = subdtl;
        }

        public String title;
        public String subtitle;
        public String subtitle2;
        public String miniute;
        public String subdetail;
    }

    private ArrayList<ListElement> aList;

    private class MyAdapter extends ArrayAdapter<ListElement> {

        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<ListElement> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout newView;

            ListElement item = getItem(position);

            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new RelativeLayout(getContext());
                LayoutInflater vi = (LayoutInflater)
                        getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                vi.inflate(resource, newView, true);
            } else {
                newView = (RelativeLayout) convertView;
            }

            // Fills in the view.
            TextView titleTextView = (TextView) newView.findViewById(R.id.title);
            TextView subtitleTextView = (TextView) newView.findViewById(R.id.subtitle);
            //TextView subtitle2TextView = (TextView) newView.findViewById(R.id.subtitle2);
            TextView detailTextView = (TextView) newView.findViewById(R.id.detail);
            TextView subdetailTextView = (TextView) newView.findViewById(R.id.subdetail);

            titleTextView.setText(item.title);
            subtitleTextView.setText(item.subtitle);
            //subtitle2TextView.setText(item.subtitle2);
            detailTextView.setText(item.miniute);
            subdetailTextView.setText(item.subdetail);

            // Set a listener for the whole list item.
            /*newView.setTag(item.url);
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Action to do when cell is clicked
                    String s = v.getTag().toString();
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, s, duration);
                    toast.show();

                    // Intent to go to next activity passing url
                    //Intent intent = new Intent(context, Main2Activity.class);
                    //intent.putExtra("url", v.getTag().toString());
                    //startActivity(intent);
                }
            });
            */
            return newView;
        }
    }

    private MyAdapter aa;
    private String[] arraySpinner;
    private int urlID = stopIdSpinner[0];

    private double lat;
    private double lon;

    static Spinner s;
    ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aList = new ArrayList<ListElement>();
        aa = new MyAdapter(this, R.layout.list_element, aList);
        ListView myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(aa);
        aa.notifyDataSetChanged();
        queue = Volley.newRequestQueue(this);
        getLoops();
        this.arraySpinner = new String[]{
                "Arboretum (North)", "Arboretum (South)", "Bookstore, Cowell & Stevenson (North)",
                "Bookstore, Cowell & Stevenson (South)", "Crown & Merrill College", "College 9 & 10 / Health Center",
                "East Field House", "East Remote Parking", "Family Student Housing", "High & Western Dr", "Kerr Hall",
                "Kresge College", "Lower Campus", "Main Entrance", "Oakes College (North)", "Oakes College (South)",
                "Rachel Carson College & Porter", "Science Hill", "The Farm / Village", "Tosca Terrace"
        };

        s = (Spinner) findViewById(R.id.spinner);
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Log.d(LOG_TAG, "POSITION  " + stopIdSpinner[position]);
                Log.d(LOG_TAG, "ID  " + id);
                urlID = stopIdSpinner[position];
                getLoops();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 20 seconds
                getLoops();
                handler.postDelayed(this, 20000);
            }
        }, 20000);  //the time is in milliseconds


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkLocationPermission()) {
            Log.d(LOG_TAG, "Checked");
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "in");
                //Request location updates:
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                Location location = locationManager.getLastKnownLocation(locationProvider);

                Log.d(LOG_TAG, "Location = " + location);

                if (location != null) {
                    // DO remember to check for null
                    lon = location.getLongitude();
                    lat = location.getLatitude();
                }
                getNearestStop(lat, lon);
            }
        }
    }

    public void getNearestStop(double lat, double lon) {
        String url = "http://107.178.220.22/api/location/" + lat + "/" + lon;
        final JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    int getLocId = 0;
                    int getLocIdIndex = 0;

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(LOG_TAG, "Received: " + response.toString());
                        // Ok, let's disassemble a bit the json object.
                        try {
                            JSONArray receivedList = response.getJSONArray("location");
                            if (receivedList.length() > 0) {
                                getLocId = Integer.parseInt(receivedList.getString(1));
                                Log.d(LOG_TAG, "ID OF LOCATION = " + receivedList.getString(1));
                                getLocIdIndex = Arrays.asList(stopIdSpinner).indexOf(getLocId);
                                Log.d(LOG_TAG, Integer.toString(getLocIdIndex));
                                if (getLocIdIndex < stopIdSpinner.length) {
                                    s.setSelection(getLocIdIndex);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d(LOG_TAG, error.toString());
                    }
                });
        queue.add(jsObjRequest);
        //s.setSelection(5);

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void clickRefresh(View v) {
        getLoops();
    }

    public void getLoops() {
        final String url = "http://107.178.220.22/api/exp/at/" + urlID;

        final JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(LOG_TAG, "Received: " + response.toString());
                        // Ok, let's disassemble a bit the json object.
                        try {
                            aList.clear();
                            JSONArray receivedList = response.getJSONArray("current");
                            if (receivedList.length() == 0) {
                                aList.add(new ListElement("No buses coming", "", "", "", ""));
                                Log.d(LOG_TAG, "no buses running");
                                aa.notifyDataSetChanged();
                            }
                            for (int i = 0; i < receivedList.length(); i++) {
                                JSONObject object = new JSONObject(receivedList.getString(i));
                                String typeLabel = object.getString("type");
                                int directionInt = object.getInt("direction");
                                int locationInt = object.getInt("location");
                                int durationInt = object.getInt("duration") / 60;

                                if (typeLabel.equals("LOOP_OUT_OF_SERVICE_AT_BARN_THEATER"))
                                    typeLabel = "LOOP";

                                typeLabel = typeLabel.replaceAll("_", " ");

                                String directionLabel = "";

                                //CW direction
                                if (directionInt == 1) {
                                    for (int k = 0; k < stopIDCW.length - 1; k++) {
                                        if (locationInt < 5) {
                                            directionLabel = "Coming From:  Main Entrance";
                                        } else if (stopIDCW[k] >= locationInt && stopIDCW[k + 1] < locationInt) {
                                            directionLabel = "Coming From:  " + stopsCW[k];
                                        }
                                    }
                                }

                                //CCW direction
                                if (directionInt == 2) {
                                    for (int k = 0; k < stopIdCCW.length - 1; k++) {
                                        if (locationInt < 5) {
                                            directionLabel = "Coming From:  Main Entrance";
                                        } else if (stopIdCCW[k] <= locationInt && stopIdCCW[k + 1] > locationInt) {
                                            directionLabel = "Coming From:  " + stopsCCW[k];
                                        }
                                    }
                                }

                                if (directionLabel.equals("Coming From:  Bookstore, Cowell & Stevenson (North)"))
                                    directionLabel = "Coming From:  Bookstore, Cowell & Stevenson";
                                else if (directionLabel.equals("Coming From:  Bookstore, Cowell & Stevenson (South)"))
                                    directionLabel = "Coming From:  Bookstore, Cowell & Stevenson";

                                aList.add(new ListElement(
                                        typeLabel, directionLabel, locationInt + "", durationInt + "", "min"
                                ));
                                Log.d(LOG_TAG, "Received: " + typeLabel + directionLabel + locationInt);
                                aa.notifyDataSetChanged();
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d(LOG_TAG, error.toString());
                    }
                });

        // In some cases, we don't want to cache the request.
        jsObjRequest.setShouldCache(false);
        queue.add(jsObjRequest);
    }
}
