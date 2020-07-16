package dev.ksick.mapreactions.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.SpeechBalloonOverlay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import dev.ksick.mapreactions.R;
import dev.ksick.mapreactions.model.Place;

public class MapFragment extends Fragment {

    public static final String ARG_NAME_PHRASE = "phrase";
    private final int PERMISSIONS_REQUEST_CODE = 1;

    private String phrase = null;

    private MapView mapView = null;
    private TextView textViewPhrase = null;
    private TextView textViewRoute = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() == null) {
            showError(getString(R.string.something_went_wrong_try_again));
            return;
        }

        phrase = getArguments().getString(ARG_NAME_PHRASE);

        // Initialize OSMDroid (Map)
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        mapView = view.findViewById(R.id.map_view);
        textViewPhrase = view.findViewById(R.id.tv_phrase);
        textViewRoute = view.findViewById(R.id.tv_route);

        view.findViewById(R.id.button_start_again).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        if (isStoragePermissionGranted()) {
            loadRouteAndInitMap();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private void loadRouteAndInitMap() {
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String encodedPhrase;
        try {
            encodedPhrase = URLEncoder.encode(phrase, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            showError(getString(R.string.phrase_invalid));
            return;
        }
        String url = "https://api.map-reactions.ksick.dev/v0-1/route?phrase=" + encodedPhrase;

        StringRequest routeRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        List<Place> route = new Gson().fromJson(
                                response,
                                new TypeToken<ArrayList<Place>>() {
                                }.getType()
                        );

                        if (route == null || route.isEmpty()) {
                            showError(getString(R.string.something_went_wrong_try_again));
                            return;
                        }

                        initMap(route);
                        showResultSummary(phrase, route);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage;
                        if (error.networkResponse == null) {
                            errorMessage = getString(R.string.something_went_wrong_try_again);
                        } else {
                            switch (error.networkResponse.statusCode) {
                                case 400:
                                    errorMessage = getString(R.string.phrase_invalid);
                                    break;
                                case 404:
                                    errorMessage = getString(R.string.route_not_found);
                                    break;
                                default:
                                    errorMessage = getString(R.string.something_went_wrong_try_again);
                            }
                        }
                        showError(errorMessage);
                    }
                });

        requestQueue.add(routeRequest);
    }

    private void initMap(List<Place> route) {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        ArrayList<SpeechBalloonOverlay> textOverlays = new ArrayList<>();

        int currentNumber = 1;
        for (Place place : route) {
            GeoPoint geoPoint = new GeoPoint(place.getLatitude(), place.getLongitude());

            waypoints.add(geoPoint);

            SpeechBalloonOverlay speechBalloonOverlay = new SpeechBalloonOverlay();
            speechBalloonOverlay.setTitle(currentNumber + " - " + place.getName());
            speechBalloonOverlay.setGeoPoint(geoPoint);

            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(getResources().getColor(R.color.colorPrimary));
            speechBalloonOverlay.setBackground(backgroundPaint);

            Paint foregroundPaint = new Paint();
            foregroundPaint.setColor(Color.WHITE);
            foregroundPaint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.cabin));
            foregroundPaint.setTextSize(56);
            speechBalloonOverlay.setForeground(foregroundPaint);

            speechBalloonOverlay.setMargin(24);

            textOverlays.add(speechBalloonOverlay);
            currentNumber = currentNumber + 1;
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        RoadManager roadManager = new OSRMRoadManager(getContext());
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road, getResources().getColor(R.color.colorPrimary), 8);
        mapView.getOverlays().add(roadOverlay);

        mapView.getOverlays().addAll(textOverlays);
        mapView.zoomToBoundingBox(roadOverlay.getBounds(), true, 100);
        mapView.invalidate();
    }

    private void showResultSummary(String phrase, List<Place> route) {
        textViewPhrase.setText(String.format("\"%s\"", phrase));
        StringBuilder routeStringBuilder = new StringBuilder("[");

        for (Place place : route) {
            if (routeStringBuilder.length() > 1) {
                routeStringBuilder.append(", ");
            }
            routeStringBuilder.append(place.getName());
        }

        routeStringBuilder.append("]");
        textViewRoute.setText(routeStringBuilder.toString());
    }

    private void showError(String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("Error")
                .setMessage(message)
                .show();
    }

    private boolean isStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }

        if (isStoragePermissionGranted()) {
            loadRouteAndInitMap();
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.storage_permission_necessary_title)
                    .setMessage(R.string.storage_permission_necessary_message)
                    .setPositiveButton(R.string.give_storage_permission, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActivity().finish();
                        }
                    })
                    .show();
        }
    }
}