package unab.semovil.busunab;

/**
 * Created by Rene on 7/28/14.
 */


import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class UpdateLocation extends Service implements
        LocationListener {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final String DEBUG_TAG = "updateLocation";
    private LocationManager mgr;
    private String best;
    private String responseString;
    private OkHttpClient client = new OkHttpClient();
    //private String urlSend = "http://52.37.152.19:80/busUnab/insertar_coordenadas.php";
    //private String urlUpdate = "http://52.37.152.19:80/busUnab/actualizar_coordenadas.php";

    private String urlSend = "http://200.69.124.143:80/busUnab/insertar_coordenadas.php";
    private String urlUpdate = "http://200.69.124.143:80/busUnab/actualizar_coordenadas.php";

    private boolean avisoInicio = false;
    private MainActivity mainActivity = new MainActivity();
    double longitude;
    double latitude;
    double newlongitude;
    double newlatitude;
    String USERID;
    String TASKID;
    String TASKPLACESID;
    //public JSONParser jsonParser;
    String params3;
    String paramsclient3;
    boolean Red;
    boolean servicio = false;
    boolean gps_enabled = false, network_enabled = false;
    SharedPreferences.Editor editor;
    private Animation blink;


    //private static OnnewServicesListener newservices;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {


                Location location = mgr.getLastKnownLocation(best);

//                Log.i("miguel",location.getLatitude()+" "+location.getLongitude());
                mServiceHandler.post(new MakeToast(trackLocation(location)));

                // Stop the service using the startId, so that we don't stop
                // the service in the middle of handling another job
                stopSelf(msg.arg1);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("error", e.toString());
            }
        }
    }

    @Override
    public void onCreate() {

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

        try {
            HandlerThread thread = new HandlerThread("ServiceStartArguments",
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            Log.d(DEBUG_TAG, ">>>onCreate()");
            // Get the HandlerThread's Looper and use it for our Handler
            mServiceLooper = thread.getLooper();
            mServiceHandler = new ServiceHandler(mServiceLooper);
            mgr = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            best = mgr.getBestProvider(criteria, true);

            mgr.requestLocationUpdates(best, 3000, 10, this);// 3 segundos
            USERID = pref.getString("USERID", ""); // getting String
            TASKID = pref.getString("TaskID", "");
            TASKPLACESID = pref.getString("TaskPlacesID", "");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("error", e.toString());
            // Crashlytics.logException(e);
        }
//        Log.i("shared",USERID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//      Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        try {
            Message msg = mServiceHandler.obtainMessage();
            String estado = intent.getStringExtra("trackingState");
            Log.i("trackingState", "updatelocation" + estado);
            if (estado.equals("waiting")) {
                Log.i("state", "waiting");
                mgr.removeUpdates(this);


                mgr.requestLocationUpdates(best, 5000, 10, this);
            }//3  min  onservice
            if (estado.equals("onservice")) {
                Log.i("state", "onservice");
                mgr.removeUpdates(this);
                mgr.requestLocationUpdates(best, 5000, 10, this);
            }

            msg.arg1 = startId;
            mServiceHandler.sendMessage(msg);
            Log.d(DEBUG_TAG, ">>>onStartCommand()");
        }
        catch (Exception e){
            Log.i("error",e.toString());
            e.printStackTrace();
        }
        // If we get killed, after returning from here, restart

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

        Log.d(DEBUG_TAG, ">>>onDestroy()");


    }

    //obtain current location, insert into database and make toast notification on screen
    private String trackLocation(Location location) throws Exception {

        String result = "Location currently unavailable.";
        if(location !=null && !validarRed()) {
            result = "Location: " + Double.toString(longitude) + ", " + Double.toString(latitude);

            newlongitude = location.getLongitude();
            newlatitude = location.getLatitude();
            editor.putString("ACTUALLATITUDE", newlatitude+"").commit(); // Storing string
            editor.putString("ACTUALLONGITUDE", newlongitude+"").commit();
            Log.i("Rene", "nuevas " + newlongitude + " " + newlatitude);
            String time2 = parseTime(location.getTime());
            mServiceHandler.post(new MakeToast("nuevas coordenadas= " + location.getLatitude() + " - " + location.getLongitude()));

            Log.i("Rene", "guardar");

            String jsoncoor = "{   \"lat\": \""+location.getLatitude()+"\",   \"long\": \"" + location.getLongitude()+"\"}";


            Log.i("Rene", "json " + jsoncoor);
            JsonObject coordenadas = new JsonObject();

            coordenadas.addProperty("id",1);
            coordenadas.addProperty("latitud",newlatitude);
            coordenadas.addProperty("longitud",newlongitude);
            coordenadas.addProperty("fecha",time2);
            coordenadas.addProperty("estado", "disponible");


            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, coordenadas.toString());
            actualizarCoordenadas(body);
        }
        else{
            validarServicios();
            Log.i("Miguel", "location null");
        }
        return result;
    }

    public void validarServicios(){

        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.GONE);
                mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.VISIBLE);
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
                                     mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.GONE);
                                     mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.GONE);

                                 } else {

                                     mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.GONE);
                                     mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.VISIBLE);
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
                                     mainActivity.imageViewRun.findViewById(R.id.imageViewRun).setVisibility(View.VISIBLE);
                                 mainActivity.imageViewCancel.findViewById(R.id.imageViewCancel).setVisibility(View.GONE);
                                 avisoInicio = false;
                                 if (mainActivity.progressDialog.isShowing())
                                 mainActivity.progressDialog.dismiss();
                             }
                         });
                         Log.i("Miguel", responseString);
                     }
                 });
             }


             public static double distFrom(double lat1, double lng1, double lat2, double lng2) {

                 double earthRadius = 6371; // km
                 lat1 = Math.toRadians(lat1);
                 lng1 = Math.toRadians(lng1);
                 lat2 = Math.toRadians(lat2);
                 lng2 = Math.toRadians(lng2);

                 double dlon = (lng2 - lng1);
                 double dlat = (lat2 - lat1);

                 double sinlat = Math.sin(dlat / 2);
                 double sinlon = Math.sin(dlon / 2);

                 double a = (sinlat * sinlat) + Math.cos(lat1) * Math.cos(lat2) * (sinlon * sinlon);
                 double c = 2 * Math.asin(Math.min(1.0, Math.sqrt(a)));

                 double distanceInMeters = earthRadius * c * 1000;
                 Log.i("Rene", "dist" + lat1 + " " + lng1 + " -  " + lat2 + " " + lng2 + " dist= " + distanceInMeters);

                 return (int) distanceInMeters;
             }

             private String parseTime(long t) {

                 String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                 Log.i("track", "time= " + currentDateandTime);

                 return currentDateandTime;
             }

             private class MakeToast implements Runnable {
                 String txt;

                 public MakeToast(String text) {
                     txt = text;
                 }

                 public void run() {

                     // Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();

                 }
             }

             @Override
             public void onLocationChanged(Location location) {
//		mHandler.post(new MakeToast(trackLocation(location)));


             }

             @Override
             public void onProviderDisabled(String provider) {
                 Log.w(DEBUG_TAG, ">>>provider disabled: " + provider);
             }


             @Override
             public void onProviderEnabled(String provider) {
                 Log.w(DEBUG_TAG, ">>>provider enabled: " + provider);
             }


             @Override
             public void onStatusChanged(String provider, int status, Bundle extras) {
                 Log.w(DEBUG_TAG, ">>>provider status changed: " + provider);
             }





   /* private class TrackUser extends AsyncTask<String, String, JSONObject> {

        //   private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected JSONObject doInBackground(String... args) {

           // userFunctions userFunction = new userFunctions();
           // JSONObject json4 = userFunction.trackUser(args[0],
           //         args[1], args[2], args[3], args[4], args[5], getApplicationContext());
           // if (json4 != null)
           //     Log.i("track", "json " + json4.toString());
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {

            if (json != null){
                try {
                    Log.i("nuevos", json.toString());
                    if (json.getString("servicios").equals("OK")) {
                        //   if(json.getString()=="")
                        //main.setbusy(true);



                        Log.i("nuevos", "nuevos");
                    }

                    Log.i("track", "todo bien");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
            else{

                Toast.makeText(getBaseContext(),"Error con la conexion al servidor", Toast.LENGTH_LONG);
                Log.i("red","error update location json null");

            }
        }

    }*/
         }
