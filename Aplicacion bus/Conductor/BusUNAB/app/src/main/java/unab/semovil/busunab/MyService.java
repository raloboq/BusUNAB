package unab.semovil.busunab;

/**
 * Created by renel on 08/03/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

//import com.google.firebase.FirebaseApp;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyService extends Service
{
    private static final String TAG = "TESTRASTREO";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0;
    private String urlUpdate = "http://200.69.124.143:80/busUnab/actualizar_coordenadas.php";
    private MainActivity mainActivity = new MainActivity();
    private OkHttpClient client = new OkHttpClient();
    private String responseString;
    boolean Red;
    boolean servicio = false;
    boolean gps_enabled = false, network_enabled = false;
    private Animation blink;
    SharedPreferences.Editor editor;
    private boolean avisoInicio = false;
    //DatabaseReference myRef;
    SharedPreferences pref;
    double lat;
    double lng;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;


        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
             lat = location.getLatitude();
             lng = location.getLongitude();
            String elcarro = lat+","+lng;

            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

          //  myRef.child("rastreador").child("coordenadas").setValue(elcarro);
          //  myRef.child("rastreador").child("fecha").setValue(currentDateTimeString);
            Log.i("coordeandas","cambio lat "+lat+"  long"+lng);

            String jsoncoor = "{   \"lat\": \""+location.getLatitude()+"\",   \"long\": \"" + location.getLongitude()+"\"}";


            Log.i("Rene", "json " + jsoncoor);
            JsonObject coordenadas = new JsonObject();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            String format = simpleDateFormat.format(new Date());

            coordenadas.addProperty("id",1);
            coordenadas.addProperty("latitud",lat);
            coordenadas.addProperty("longitud",lng);
            coordenadas.addProperty("fecha",format);
            pref.getString("estado","");
            coordenadas.addProperty("estado",  pref.getString("estado","no disponible"));


            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, coordenadas.toString());
            try {
                actualizarCoordenadas(body);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // registrar(location.getLatitude(),location.getLongitude());
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
      //  FirebaseApp.initializeApp(this);
     //   FirebaseDatabase database = FirebaseDatabase.getInstance();
      //  myRef = database.getReference("reportes");
         pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();


        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }

       // lat = location.getLatitude();
       // lng = location.getLongitude();
        String elcarro = lat+","+lng;

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        //  myRef.child("rastreador").child("coordenadas").setValue(elcarro);
        //  myRef.child("rastreador").child("fecha").setValue(currentDateTimeString);
        Log.i("coordeandas","cambio lat "+lat+"  long"+lng);

       // String jsoncoor = "{   \"lat\": \""+location.getLatitude()+"\",   \"long\": \"" + location.getLongitude()+"\"}";


    //    Log.i("Rene", "json " + jsoncoor);
        JsonObject coordenadas = new JsonObject();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String format = simpleDateFormat.format(new Date());

        coordenadas.addProperty("id",1);
        coordenadas.addProperty("latitud",lat);
        coordenadas.addProperty("longitud",lng);
        coordenadas.addProperty("fecha",format);
        pref.getString("estado","");
        coordenadas.addProperty("estado",  "no disponible");


        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, coordenadas.toString());
        try {
            actualizarCoordenadas(body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void validarServicios(){

        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
              //  mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.GONE);
             //   mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.VISIBLE);
                validarRed();
                validarUbicacion();
                if (validarRed() && validarUbicacion()){
                    Toast.makeText(getApplicationContext(), R.string.encenderUbicacionEInternet, Toast.LENGTH_SHORT).show();

                }else if (validarUbicacion()){
                    Toast.makeText(getApplicationContext(), R.string.encenderUbicacion, Toast.LENGTH_SHORT).show();
                }else if (validarRed()){
                    Toast.makeText(getApplicationContext(), R.string.encenderInternet, Toast.LENGTH_SHORT).show();
                }

            }
        });


    }

    public boolean validarUbicacion(){
        LocationManager locationManager = null;
        servicio = false;
        gps_enabled = false;
        network_enabled = false;

        if(locationManager==null)
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){
            Log.i("error",ex.toString());
        }
        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){Log.i("error",ex.toString());}


        if (!gps_enabled && !network_enabled) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.sinUbicacion.findViewById(R.id.textSinUbicacion).setVisibility(View.VISIBLE);
                    blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                    mainActivity.sinUbicacion.setAnimation(blink);
                }});
            servicio = true;
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.sinUbicacion.findViewById(R.id.textSinUbicacion).setVisibility(View.GONE);
                    mainActivity.sinUbicacion.clearAnimation();
                }});
        }

        return servicio;
    }

    public boolean validarRed() {
        Red = false;
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        try {
            if (activeNetworkInfo == null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.sinInternet.findViewById(R.id.textSinInternet).setVisibility(View.VISIBLE);
                        blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                        mainActivity.sinInternet.setAnimation(blink);
                    }
                });
                Red = true;
            } else {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.sinInternet.findViewById(R.id.textSinInternet).setVisibility(View.GONE);
                        mainActivity.sinInternet.clearAnimation();
                    }
                });
            }
        }catch(Exception e){

        }

        return Red;
    }

    public void actualizarCoordenadas(RequestBody body) throws Exception {


        Request request = new Request.Builder()
                .url(urlUpdate)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("Miguel", "error");
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {

                        if (!validarUbicacion() || !validarRed()) {
                        //    mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.GONE);
                       //     mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.GONE);

                        } else {
//
                       //     mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.GONE);
                       //     mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.VISIBLE);
                            Toast.makeText(getApplicationContext(), R.string.errorUpdate, Toast.LENGTH_SHORT).show();

                        }

                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();
                avisoInicio = true;
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        if (avisoInicio)
//                            mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.VISIBLE);
//                        mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.GONE);
                        avisoInicio = false;
//                        if (mainActivity.progressDialog.isShowing())
//                            mainActivity.progressDialog.dismiss();
                    }
                });
                Log.i("Miguel", responseString);
            }
        });
    }

    /*public void registrar(double lati, double longi){


        Log.i("rta","entro" );

        final OkHttpClient client = new OkHttpClient();

        //public void run() throws Exception {
        Request request = new Request.Builder()
                .url("http://soydomi.com/domiapidev/domiapi/public/index.php/tax/registrarcoordenadas/"+"a/"+lati+"/"+longi)
                .build();

       // Log.i("registro","http://soydomi.com/domiapidev/domiapi/public/index.php/tax/registro/"+nombre.getText().toString().trim()+"/"+telefono.getText().toString()+"/");

        client.newCall(request).enqueue(new Callback() {


            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("rta","paila" + e );



            }



            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {

                if (!response.isSuccessful())
                {

                    throw new IOException("Unexpected code " + response);

                }


                String rtaa = response.body().string();

                if(rtaa.contains("exito")){


                }



            }


        });



    }*/
}
