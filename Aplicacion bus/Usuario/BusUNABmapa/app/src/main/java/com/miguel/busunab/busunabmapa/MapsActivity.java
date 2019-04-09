package com.miguel.busunab.busunabmapa;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private GoogleMap mMap;
    private Marker mMarker;
    private String responseString;
    private Coordenadas coordenadasArray = new Coordenadas();
    private List<Coordenada> coordenadasList = new ArrayList<>();
    private double latitud;
    private double longitud;
    private ProgressDialog progressDialog;
    private Handler h;
    private int delay;
    private Runnable getCoordenadasRunnable;
    public TextView textViewBusOff;
    //private String urlGet = "http://52.37.152.19:80/bus/obtener_coordenadas.php";
    private String urlGet = "http://200.69.124.143:80/busUnab/obtener_coordenadas.php";
    //private String urlGet = "http://52.37.152.19:80/busUnab/obtener_coordenadas.php";
    private TextView textViewBusDetenido;
    private Animation blink;
    private LinearLayout layoutSinSeñal;
    private GoogleApiClient mGoogleApiClient;
    private int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private double newlongitude;
    private ImageView image_cambio_mapa;
    private SharedPreferences preferenciasBusUNAB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }


        preferenciasBusUNAB = this.getSharedPreferences("busUNAB_preferencias",this.MODE_PRIVATE);

        if (mGoogleApiClient == null) {
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
        }


        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setTitle("Bus UNAB");
        myToolbar.setTitleTextAppearance(this, R.style.ActionBarTitleLight);
        setSupportActionBar(myToolbar);


        textViewBusOff = (TextView) findViewById(R.id.busFueradeServicio);
        textViewBusDetenido = (TextView) findViewById(R.id.busDetenido);
        layoutSinSeñal = (LinearLayout) findViewById(R.id.layoutSinSeñal);
        image_cambio_mapa = (ImageView) findViewById(R.id.image_cambio_mapa);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.informacion) {
            Intent intent = new Intent(this, Info.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /* public void loadCurrentPosition(GoogleMap googleMap) {

        mMap = googleMap;
        Location locationCt;
        LocationManager locationManagerCt = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        locationCt = locationManagerCt.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationCt = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        double latitudLocal = locationCt.getLatitude();
        double longitudLocal = locationCt.getLongitude();
        LatLng latLng = new LatLng(latitudLocal, longitudLocal);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0F));

    }*/

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = "";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\nLocation to show user location.";
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            message += "\nLocation to show user location.";
        }

        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
        } // else: We already have permissions, so handle as normal
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location_FINE_LOCATION = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                Boolean location_COARSE_LOCATION = perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (location_FINE_LOCATION && location_COARSE_LOCATION) {
                    // All Permissions Granted
                   // Toast.makeText(MapsActivity.this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
                } else if (location_FINE_LOCATION && location_COARSE_LOCATION) {
                    Toast.makeText(this, "Los permisos de ubicación son necesarios para obtener la ubicación del bus", Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(MapsActivity.this, "Los permisos de ubicación son necesarios para obtener la ubicación del bus.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        Bitmap unabIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_unab_circle);
        Bitmap csuIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_csu_circle);
        Bitmap parada = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_parada);

        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }

        }else {
            mMap.setMyLocationEnabled(true);
        }
        String registro = preferenciasBusUNAB.getString("tipo_mapa", "ningun registro");
        if (preferenciasBusUNAB.getString("tipo_mapa", "ningun registro").compareTo("normal")==0){
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }else if(preferenciasBusUNAB.getString("tipo_mapa", "ningun registro").compareTo("hibrido")==0){
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }else{
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }


        LatLng UNAB = new LatLng(7.116793, -73.104979);
        mMap.addMarker(new MarkerOptions()
                .position(UNAB)
                .icon(BitmapDescriptorFactory.fromBitmap(unabIcon))
                .title("Universidad Autónoma de Bucaramanga"));

        LatLng CSU = new LatLng(7.112369, -73.105542);
        mMap.addMarker(new MarkerOptions()
                .position(CSU)
                .icon(BitmapDescriptorFactory.fromBitmap(csuIcon))
                .title("Centro de servicios universitarios"));

        //PARADAS

        LatLng parqueadero = new LatLng(7.117637, -73.105635);
        mMap.addMarker(new MarkerOptions()
                .position(parqueadero)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Parqueadero"));

        LatLng biblioteca = new LatLng(7.115673, -73.104739);
        mMap.addMarker(new MarkerOptions()
                .position(biblioteca)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Biblioteca"));

        LatLng bahia = new LatLng(7.112903, -73.106049);
        mMap.addMarker(new MarkerOptions()
                .position(bahia)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Bahía"));

        LatLng garitaS = new LatLng(7.115140, -73.104287);
        mMap.addMarker(new MarkerOptions()
                .position(garitaS)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Garita seguridad"));

        LatLng capilla = new LatLng(7.109652, -73.105208);
        mMap.addMarker(new MarkerOptions()
                .position(capilla)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Capilla de los Misioneros de Yarumal"));

        LatLng metrolinea = new LatLng(7.110873, -73.106065);
        mMap.addMarker(new MarkerOptions()
                .position(metrolinea)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Parada metrolinea"));

        LatLng carrera39 = new LatLng(7.115093, -73.106303);
        mMap.addMarker(new MarkerOptions()
                .position(carrera39)
                .icon(BitmapDescriptorFactory.fromBitmap(parada))
                .title("Carrera 39"));

        //mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));

        GoogleMapOptions options = new GoogleMapOptions();

        options.rotateGesturesEnabled(true)
                .scrollGesturesEnabled(true)
                .zoomControlsEnabled(true)
                .zoomGesturesEnabled(true);


        image_cambio_mapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle(R.string.lista_mapas)
                        .setItems(R.array.lista_mapas, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int position) {
                                if (position==0){
                                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                    image_cambio_mapa.setImageResource(R.drawable.cambio_mapa);
                                    preferenciasBusUNAB.edit().putString("tipo_mapa", "normal").apply();
                                }else{
                                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                    image_cambio_mapa.setImageResource(R.drawable.cambio_mapa_blanco);
                                    preferenciasBusUNAB.edit().putString("tipo_mapa", "hibrido").apply();
                                }

                                // The 'which' argument contains the index position
                                // of the selected item
                            }
                        });

                builder.show();
            }

        });
    }

    public void cargarUbicacionBus(GoogleMap googleMap, double latitud, double longitud) {
        mMap = googleMap;
        LatLng busLocation = new LatLng(latitud, longitud);
        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_bus_unab);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busLocation, 17.5F));
        if (mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(busLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .title("Bus UNAB"));

        } else {
            busAnimation(mMarker, latitud, longitud);
            mMarker.setPosition(busLocation);
        }


    }

    // Add a marker in Sydney and move the camera


    private void getCoordenadas() {

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlGet)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                                final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                builder.setTitle(R.string.error).setIcon(R.drawable.warning)
                                        .setMessage(R.string.intentar);

                                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        textViewBusOff.setVisibility(View.GONE);
                                        textViewBusDetenido.setVisibility(View.GONE);
                                        textViewBusDetenido.clearAnimation();
                                        textViewBusOff.clearAnimation();

                                        layoutSinSeñal.setVisibility(View.VISIBLE);
                                        blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                                        layoutSinSeñal.startAnimation(blink);
                                        Toast.makeText(getApplicationContext(), R.string.recuperandoConexion, Toast.LENGTH_LONG).show();

                                    }
                                });
                                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                                    }
                                });
                                builder.show();
                            } else {
                                textViewBusOff.setVisibility(View.GONE);
                                textViewBusDetenido.setVisibility(View.GONE);
                                textViewBusDetenido.clearAnimation();
                                textViewBusOff.clearAnimation();

                                layoutSinSeñal.setVisibility(View.VISIBLE);
                                blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                                layoutSinSeñal.startAnimation(blink);
                                Toast.makeText(getApplicationContext(), R.string.recuperandoConexion, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {


                    responseString = response.body().string();
                    Gson gson = new Gson();
                    coordenadasArray = gson.fromJson(responseString, Coordenadas.class);
                    int coorSize = coordenadasArray.getCoordenadas().size();
                    for (int i = 0; i < coorSize; i++) {
                        coordenadasList.add(coordenadasArray.getCoordenadas().get(i));
                    }
                    latitud = coordenadasList.get(coordenadasList.size() - 1).getLatitud();
                    longitud = coordenadasList.get(coordenadasList.size() - 1).getLongitud();
                    String estado = coordenadasList.get(coordenadasList.size() - 1).getEstado();

                    if (estado.compareTo("disponible") == 0) {

                        MapsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textViewBusOff.setVisibility(View.GONE);
                                textViewBusDetenido.setVisibility(View.GONE);
                                layoutSinSeñal.setVisibility(View.GONE);

                                textViewBusDetenido.clearAnimation();
                                textViewBusOff.clearAnimation();
                                layoutSinSeñal.clearAnimation();

                                cargarUbicacionBus(mMap, latitud, longitud);

                            }
                        });

                    } else if (estado.compareTo("detenido") == 0) {
                        MapsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textViewBusDetenido.setVisibility(View.VISIBLE);
                                textViewBusOff.setVisibility(View.GONE);
                                layoutSinSeñal.setVisibility(View.GONE);

                                textViewBusOff.clearAnimation();
                                layoutSinSeñal.clearAnimation();
                                blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                                textViewBusDetenido.startAnimation(blink);
                                textViewBusDetenido.setTextColor(getResources().getColor(R.color.textBusPausado));

                                cargarUbicacionBus(mMap, latitud, longitud);
                            }
                        });

                    } else {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                textViewBusOff.setVisibility(View.VISIBLE);
                                textViewBusDetenido.setVisibility(View.GONE);
                                layoutSinSeñal.setVisibility(View.GONE);

                                textViewBusDetenido.clearAnimation();
                                layoutSinSeñal.clearAnimation();
                                blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                                textViewBusOff.startAnimation(blink);
                                textViewBusOff.setTextColor(Color.RED);
                            }
                        });
                    }

                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

            });

        } catch (Exception e) {

        }
    }

    private void handlerGetCoordenadas() {
        try {
            h = new Handler();
            delay = 3000; //milliseconds

            getCoordenadasRunnable = new Runnable() {
                @Override
                public void run() {
                    h.postDelayed(this, delay);
                    getCoordenadas();
                }
            };

            h.postDelayed(getCoordenadasRunnable, delay);

           /* h.postDelayed(new Runnable() {
                public void run() {
                    //do something
                    h.postDelayed(this, delay);
                    getCoordenadas();
                }
            }, delay);*/
        } catch (Exception e) {

        }
    }

    private void busAnimation(final Marker mMarker, double latitud, double longitud) {
        final LatLng startPosition = mMarker.getPosition();
        final LatLng finalPosition = new LatLng(latitud, longitud);
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 3000;
        final boolean hideMarker = false;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                        startPosition.longitude * (1 - t) + finalPosition.longitude * t);

                mMarker.setPosition(currentPosition);

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        mMarker.setVisible(false);
                    } else {
                        mMarker.setVisible(true);
                    }
                }
            }
        });
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.miguel.busunab.busunabmapa/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    protected void onResume() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(MapsActivity.this.getString(R.string.cargando));
        progressDialog.setCancelable(false);
        progressDialog.show();
        handlerGetCoordenadas();
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(getCoordenadasRunnable);
        if (progressDialog.isShowing()){
            progressDialog.dismiss();
        }
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.miguel.busunab.busunabmapa/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacks(getCoordenadasRunnable);
    }


    @Override
    public void onConnected(Bundle bundle) {
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
        Location locationCt = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (locationCt != null) {
            double latitudLocal = locationCt.getLatitude();
            double longitudLocal = locationCt.getLongitude();
            LatLng latLng = new LatLng(latitudLocal, longitudLocal);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0F));
        }

    }




    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

}
