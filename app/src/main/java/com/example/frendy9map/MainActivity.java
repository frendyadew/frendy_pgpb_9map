package com.example.frendy9map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import android.view.View;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;
    private boolean isLocationTracking = false;
    private ImageButton locationButton;
    private ImageView popupImageView;
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Mapbox
        Mapbox.getInstance(this);
        setContentView(R.layout.activity_main);

        // Initialize MapView
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // Initialize popupImageView
        popupImageView = findViewById(R.id.popupImageView); // Initialize popupImageView with the ImageView in layout


        mapView.getMapAsync(map -> {
            this.mapboxMap = map;
            map.setStyle(new Style.Builder().fromJson(getStyleJson()), style -> {
                // Add markers with tooltips
                List<LatLng> locations = new ArrayList<>();
                locations.add(new LatLng(-7.788030240954288, 110.37853161680283)); // Warung Burjo
                locations.add(new LatLng(-7.790545258058598, 110.3754816203809)); // Mie Ayam Bakso
                locations.add(new LatLng(-7.791208921766778, 110.37049058641973)); // Sate Ayam Jos
                locations.add(new LatLng(-7.783975845816085, 110.369710635361)); // Mie Ayam Enak

                List<Feature> features = new ArrayList<>();
                for (LatLng location : locations) {
                    Feature feature = Feature.fromGeometry(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
                    features.add(feature);
                }

                GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source",
                        FeatureCollection.fromFeatures(features));
                style.addSource(geoJsonSource);

                style.addImage("marker-icon-id", BitmapUtils.getBitmapFromDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_location_on_24, null)));

                SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                        .withProperties(
                                PropertyFactory.iconImage("marker-icon-id"),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconSize(1.9f)
                        );

                mapboxMap.addOnMapClickListener(point -> {
                    // Iterate through defined locations
                    for (LatLng location : locations) {
                        // Check if the clicked point is near any of the defined marker locations
                        if (point.distanceTo(new LatLng(location.getLatitude(), location.getLongitude())) < 50) { // Adjust distance threshold as necessary
                            showImageForLocation(location);  // Call showImageForLocation when marker is clicked
                            return true; // Stop processing if a location matches
                        }
                    }
                    return false; // No marker matched the clicked location
                });
                style.addLayer(symbolLayer);

                map.setCameraPosition(new CameraPosition.Builder()
                        .target(locations.get(3)).zoom(7.0).build());

                // Zoom In and Zoom Out buttons
                Button zoomInButton = findViewById(R.id.btnZoomIn);
                Button zoomOutButton = findViewById(R.id.btnZoomOut);

                // Zoom in
                zoomInButton.setOnClickListener(v -> {
                    CameraPosition position = new CameraPosition.Builder()
                            .target(mapboxMap.getCameraPosition().target)
                            .zoom(mapboxMap.getCameraPosition().zoom + 1)
                            .build();
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 500);
                });

                // Zoom out
                zoomOutButton.setOnClickListener(v -> {
                    CameraPosition position = new CameraPosition.Builder()
                            .target(mapboxMap.getCameraPosition().target)
                            .zoom(mapboxMap.getCameraPosition().zoom - 1)
                            .build();
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 500);
                });

                // Enable location component
                checkLocationPermission(style, mapboxMap);

                // Location button
                locationButton = findViewById(R.id.geolocation_button);
                locationButton.setOnClickListener(v -> {
                    if (locationComponent != null) {
                        isLocationTracking = !isLocationTracking;
                        if (isLocationTracking) {
                            locationComponent.setCameraMode(CameraMode.TRACKING);
                            locationButton.setImageResource(R.drawable.baseline_my_location_24);
                        } else {
                            locationComponent.setCameraMode(CameraMode.NONE);
                            locationButton.setImageResource(R.drawable.baseline_share_location_24);
                        }
                    }
                });
            });
        });
    }

    private String getStyleJson() {
        String tmsUrl = "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}";
        return "{\n" +
                "  \"version\": 8,\n" +
                "  \"sources\": {\n" +
                "    \"tms-tiles\": {\n" +
                "      \"type\": \"raster\",\n" +
                "      \"tiles\": [\"" + tmsUrl + "\"],\n" +
                "      \"tileSize\": 256\n" +
                "    }\n" +
                "  },\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"tms-tiles\",\n" +
                "      \"type\": \"raster\",\n" +
                "      \"source\": \"tms-tiles\",\n" +
                "      \"minzoom\": 0,\n" +
                "      \"maxzoom\": 22\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private void showImageForLocation(LatLng location) {
        popupImageView.setVisibility(View.VISIBLE);

        if (location.equals(new LatLng(-7.788030240954288, 110.37853161680283))) {
            popupImageView.setImageResource(R.drawable.image_13); // Warung Burjo
        } else if (location.equals(new LatLng(-7.790545258058598, 110.3754816203809))) {
            popupImageView.setImageResource(R.drawable.image_14); // Mie Ayam Bakso
        } else if (location.equals(new LatLng(-7.791208921766778, 110.37049058641973))) {
            popupImageView.setImageResource(R.drawable.image_15); // Sate Ayam Jos
        } else if (location.equals(new LatLng(-7.783975845816085, 110.369710635361))) {
            popupImageView.setImageResource(R.drawable.image_16); // Mie Ayam Enak
        }
    }


    private void toggleLocationTracking() {
        if (locationComponent != null) {
            isLocationTracking = !isLocationTracking;
            if (isLocationTracking) {
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationButton.setImageResource(R.drawable.baseline_my_location_24);
            } else {
                locationComponent.setCameraMode(CameraMode.NONE);
                locationButton.setImageResource(R.drawable.baseline_share_location_24);
            }
        }
    }

    private void centerOnUserLocation() {
        if (locationComponent != null && locationComponent.getLastKnownLocation() != null) {
            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(locationComponent.getLastKnownLocation().getLatitude(),
                            locationComponent.getLastKnownLocation().getLongitude()), 15));
        }
    }


    private void checkLocationPermission(Style style, MapboxMap mapboxMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent(style, mapboxMap);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void enableLocationComponent(Style style, MapboxMap mapboxMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (mapboxMap != null) {
                locationComponent = mapboxMap.getLocationComponent();
                LocationComponentActivationOptions locationComponentActivationOptions =
                        LocationComponentActivationOptions.builder(this, style).build();

                locationComponent.activateLocationComponent(locationComponentActivationOptions);
                locationComponent.setLocationComponentEnabled(true);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.COMPASS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapView.getMapAsync(mapboxMap -> {
                    mapboxMap.getStyle(style -> enableLocationComponent(style, mapboxMap));
                });
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
