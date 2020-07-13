package com.example.maps_prakashrana_c0773839;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.sql.ClientInfoStatus;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity  implements OnMapReadyCallback,  GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private static final int REQUEST_CODE = 1;
    private static final int POLYGON_SIDES = 4;
    Polyline line;
    Polygon shape;
    List<Marker> markers = new ArrayList<>();
    List<Marker> distanceMarkers = new ArrayList<>();

    ArrayList<Polyline> polylines = new ArrayList<>();
    //location with location manager and listner
    LocationManager locationManager;
    LocationListener locationListener;
    private GoogleMap mMap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    public BitmapDescriptor createPureTextIcon(String text) {

        Paint textPaint = new Paint(); // Adapt to your needs

        textPaint.setTextSize(35);
        float textWidth = textPaint.measureText(text);
        float textHeight = textPaint.getTextSize();
        int width = (int) (textWidth);
        int height = (int) (textHeight);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        canvas.translate(0, height);

        // For development only:
        // Set a background in order to see the
        // full size and positioning of the bitmap.
        // Remove that for a fully transparent icon.

        canvas.drawText(text, 0, 0, textPaint);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(image);
        return icon;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnPolylineClickListener(this);
        mMap.setOnPolygonClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                setHomeMarker(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


        if (!hasLocationPermission()) {
            requestLocationPermission();
        } else {
            startUpdateLocations();
            // zoom to canada
            LatLng canadaCenterLatLong = new LatLng(56.1304,-106.3468);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(canadaCenterLatLong, 7));

        }



        //apply long gesture


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                System.out.println("marker Clicked"+marker.isInfoWindowShown());
                if(marker.isInfoWindowShown()){
                    marker.hideInfoWindow();
                }
                else{
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                if (markers.size() == POLYGON_SIDES) {
                    for(Polyline line: polylines){
                        line.remove();
                    }
                    polylines.clear();

                    shape.remove();
                    shape = null;

                    for(Marker currMarker: distanceMarkers){
                        currMarker.remove();
                    }
                    distanceMarkers.clear();

                    drawShape();
                }
            }
        });
    }

    private void startUpdateLocations() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                  return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (REQUEST_CODE == requestCode) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            }
        }
    }

    private void setMarker(LatLng latLng){

        Geocoder geoCoder = new Geocoder(this);
        Address address = null;

        try{
            List<Address> matches = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            address = (matches.isEmpty() ? null : matches.get(0));
        }
        catch (IOException e){
            e.printStackTrace();
        }

        String title = "";
        String snippet = "";

        ArrayList<String> titleComponents = new ArrayList<>();
        ArrayList<String> snippetComponents = new ArrayList<>();

        if(address != null){
            // get title
            if(address.getSubThoroughfare() != null)
            {
                titleComponents.add(address.getSubThoroughfare());

            }
            if(address.getThoroughfare() != null)
            {

                titleComponents.add(address.getThoroughfare());

            }
            if(address.getPostalCode() != null)
            {

                titleComponents.add(address.getPostalCode());

            }


            // get snippet

            if(address.getLocality() != null)
            {
                snippetComponents.add(address.getLocality());

            }
            if(address.getAdminArea() != null)
            {
                snippetComponents.add(address.getAdminArea());

            }

        }

        title = TextUtils.join(", ",titleComponents);
        title = (title.equals("") ? "  " : title);

        snippet = TextUtils.join(", ",snippetComponents);


        MarkerOptions options = new MarkerOptions().position(latLng)
                .draggable(true)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .snippet(snippet);


        // check if there are already the same number of markers, we clear the map
        if (markers.size() == POLYGON_SIDES) {
            clearMap();
        }

        Marker mm = mMap.addMarker(options);

        markers.add(mm);

        if (markers.size() == POLYGON_SIDES) {
            drawShape();
        }
    }

    private void drawShape (){
        PolygonOptions options = new PolygonOptions()
                .fillColor(Color.argb(35, 0, 1, 0))
                .strokeWidth(0);


        LatLng[] markersConvex = new LatLng[4];
        for (int i = 0; i < POLYGON_SIDES; i++) {
            markersConvex[i] = new LatLng(markers.get(i).getPosition().latitude,
                    markers.get(i).getPosition().longitude);
        }


        Vector<LatLng> sortedLatLong = ConvexHullJarvis.convexHull(markersConvex, POLYGON_SIDES);

        // add polygon as per convex hull lat long
        options.addAll(sortedLatLong);
        shape = mMap.addPolygon(options);
        shape.setClickable(true);

        // draw the polyline too
        LatLng[] polyLinePoints = new LatLng[sortedLatLong.size() + 1];
        int index = 0;
        for (LatLng x : sortedLatLong) {
            polyLinePoints[index] = x;

            index++;
            if (index == sortedLatLong.size()) {
                // at last add initial point
                polyLinePoints[index] = sortedLatLong.elementAt(0);
            }
        }

        for(int i =0 ; i<polyLinePoints.length -1 ; i++){

            LatLng[] tempArr = {polyLinePoints[i], polyLinePoints[i+1] };
            Polyline currentPolyline =  mMap.addPolyline(new PolylineOptions()
                    .clickable(true)
                    .add(tempArr)
                    .color(Color.RED));
            currentPolyline.setClickable(true);
            polylines.add(currentPolyline);
        }


    }

    private void clearMap() {

        // remove city markers
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        // remove polylines outer
        for(Polyline line: polylines){
            line.remove();
        }
        polylines.clear();

        // remove polygoon
        shape.remove();
        shape = null;

        // remove distance markers
        for (Marker marker : distanceMarkers) {
            marker.remove();
        }
        distanceMarkers.clear();

    }

    @Override
    public void onMapLongClick(LatLng latLng) {


    }

    @Override
    public void onMapClick(LatLng latLng) {
        System.out.println("long press");
        setMarker(latLng);

    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }



    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);

        LatLng center = LatLngBounds.builder().include(firstPoint).include(secondPoint).build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(createPureTextIcon(getDistanceOfPolyLine(polyline)));
        distanceMarkers.add(mMap.addMarker(options));
    }

    public String getDistanceOfPolyLine(Polyline polyline){
        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);


        double distance = distance(firstPoint.latitude,firstPoint.longitude,
                secondPoint.latitude,secondPoint.longitude);
        NumberFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(distance) + " KM";
    }

    public String getDistanceOfPolyLines(ArrayList<Polyline> polylines){

        double totalDistance = 0;
        for(Polyline polyline : polylines){
            List<LatLng> points = polyline.getPoints();
            LatLng firstPoint = points.remove(0);
            LatLng secondPoint = points.remove(0);


            double distance = distance(firstPoint.latitude,firstPoint.longitude,
                    secondPoint.latitude,secondPoint.longitude);
           totalDistance += distance;

        }
        NumberFormat formatter = new DecimalFormat("#0.0");

        return formatter.format(totalDistance) + " KM";
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for(LatLng point: polygon.getPoints()){
            builder.include(point);
        }
        LatLng center = builder.build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(createPureTextIcon(getDistanceOfPolyLines(polylines)));
        distanceMarkers.add(mMap.addMarker(options));

    }
}