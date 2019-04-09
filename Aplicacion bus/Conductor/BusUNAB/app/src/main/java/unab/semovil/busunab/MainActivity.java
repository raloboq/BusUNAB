package unab.semovil.busunab;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.fabric.sdk.android.Fabric;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {


    public PendingIntent tracking;
    public AlarmManager alarms;
    public long UPDATE_INTERVAL = 3000;
    public int START_DELAY = 0;
    private static final int REQUEST_COARSE_LOCATION = 112;
    private static final int REQUEST_FINE_LOCATION = 113;
    public String DEBUG_TAG = "LocationServiceActivity";
    public static ImageView imageViewRun;
    public static ImageView imageViewCancel;
    public static ImageView imageViewPause;
    public static TextView sinInternet;
    public static TextView sinUbicacion;
    public static ProgressDialog progressDialog;
    private Animation blink;
    private OkHttpClient client = new OkHttpClient();
    //private String urlGet = "http://52.37.152.19:80/bus/insertar_coordenadas.php";
    private String urlGet = "http://200.69.124.143:80/busUnab/obtener_coordenadas.php";
    //private String urlGet = "http://52.37.152.19:80/busUnab/obtener_coordenadas.php";
    //private String urlUpdate = "http://52.37.152.19:80/busUnab/actualizar_coordenadas.php";
    private String urlUpdate = "http://200.69.124.143:80/busUnab/actualizar_coordenadas.php";
    private String responseString;
    private Coordenadas coordenadasArray= new Coordenadas();;
    private List<Coordenada> coordenadasList = new ArrayList<>();;
    private Double latitud;
    private String estado;
    private Double longitud;
    private boolean servicioUbicacion = false;
    private boolean servicioRed = false;
    public static boolean active = false;
    private Handler h;
    private int delay;
    private Runnable getServiciosInternetUbicacion;

    SharedPreferences prefs = null;
    SharedPreferences.Editor editor;

    private Handler handlerGetEstado;
    private int delayGetEstado;
    private Runnable getServiciosEstadoBus;
    private int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(MainActivity.this);
        prefs = getSharedPreferences("firstLaunch", MODE_PRIVATE);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

      //  handlerGetEstadoBus();
        imageViewRun = (ImageView)findViewById(R.id.imageViewRun);
        imageViewCancel = (ImageView)findViewById(R.id.imageViewCancel);
        imageViewPause = (ImageView)findViewById(R.id.imageViewPause);
        sinInternet = (TextView)findViewById(R.id.textSinInternet);
        sinUbicacion = (TextView)findViewById(R.id.textSinUbicacion);

        final Button buttonIniciar = (Button)findViewById(R.id.btnIniciar);
        final Button buttonCancelar = (Button)findViewById(R.id.btnCancelar);
        final Button buttonPausar = (Button)findViewById(R.id.btnPausar);

        String estado = pref.getString("estado","");
        if(estado.equals("no disponible")){
            imageViewCancel.setVisibility(View.VISIBLE);
        }

       // start();

        final Intent i = new Intent(this, MyService.class);

        buttonIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*progressDialog.setTitle("Cargando");
                progressDialog.setMessage("Iniciando servicio...");
                progressDialog.show();*/
                //start();
                editor.putString("estado","disponible").commit();
                startService(i);
                imageViewRun.setVisibility(View.VISIBLE);
                imageViewCancel.setVisibility(View.GONE);
                imageViewPause.setVisibility(View.GONE);

            }
        });

        buttonCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*try {
                    progressDialog.setTitle("Cargando");
                    progressDialog.setMessage("Cancelando servicio...");
                    progressDialog.show();
                   // stop();
                    Toast.makeText(getApplicationContext(), R.string.cancelandoServicio, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                editor.putString("estado","no disponible").commit();
                stopService(i);
               imageViewRun.setVisibility(View.GONE);
                imageViewCancel.setVisibility(View.VISIBLE);
                imageViewPause.setVisibility(View.GONE);

            }
        });

        buttonPausar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*try {
                    progressDialog.setTitle("Cargando");
                    progressDialog.setMessage("Pausando servicio...");
                    progressDialog.show();
                  //  pause();
                    Toast.makeText(getApplicationContext(), R.string.pausandoServicio, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                editor.putString("estado","no disponible").commit();
                stopService(i);
                imageViewRun.setVisibility(View.GONE);
                imageViewCancel.setVisibility(View.GONE);
                imageViewPause.setVisibility(View.VISIBLE);
            }
        });
    }

    public void start() {
        Log.i("miguel", "empezo waiting");
        servicioUbicacion = validarUbicacion();
        servicioRed = validarRed();
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (!servicioUbicacion && !servicioRed) {
                    //  imageViewRun.setVisibility(View.VISIBLE);
                    imageViewCancel.setVisibility(View.GONE);
                    imageViewPause.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), R.string.iniciandoServicio, Toast.LENGTH_SHORT).show();
                    setRecurringAlarm(this);

                } else if (servicioUbicacion && servicioRed) {
                    Toast.makeText(getApplicationContext(), R.string.encenderUbicacionEInternet, Toast.LENGTH_SHORT).show();


                } else if (servicioUbicacion) {
                    Toast.makeText(getApplicationContext(), R.string.encenderUbicacion, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.encenderInternet, Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }

        }else {

            if (!servicioUbicacion && !servicioRed) {
                //  imageViewRun.setVisibility(View.VISIBLE);
                imageViewCancel.setVisibility(View.GONE);
                imageViewPause.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), R.string.iniciandoServicio, Toast.LENGTH_SHORT).show();
                setRecurringAlarm(this);

            } else if (servicioUbicacion && servicioRed) {
                Toast.makeText(getApplicationContext(), R.string.encenderUbicacionEInternet, Toast.LENGTH_SHORT).show();


            } else if (servicioUbicacion) {
                Toast.makeText(getApplicationContext(), R.string.encenderUbicacion, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.encenderInternet, Toast.LENGTH_SHORT).show();
            }
        }



    }

    public void stop() throws Exception {
        Log.i("miguel", "acabó");
        Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
        tracking = PendingIntent.getBroadcast(getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

            alarms.cancel(tracking);
            tracking.cancel();
            this.stopService(intent);
            getCoordenadasCancelado();



        Log.d(DEBUG_TAG, ">>>Stop tracking()");
    }

    public void pause() throws Exception {
        Log.i("miguel", "pausó");
        Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
        tracking = PendingIntent.getBroadcast(getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarms.cancel(tracking);
            tracking.cancel();
            this.stopService(intent);
            getCoordenadasPausa();

        Log.d(DEBUG_TAG, ">>>Stop tracking()");
    }



    public void setRecurringAlarm(Context context) {
        alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Log.i("Miguel", "set alarma");
        // get a Calendar object with current time
        Calendar cal = Calendar.getInstance();
        // add 5 minutes to the calendar object
        cal.add(Calendar.SECOND, START_DELAY);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("state", "waiting");


        tracking = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), UPDATE_INTERVAL, tracking);

    }


    public void registrarBusSinServicio(double latitud, double longitud)throws Exception  {

        JsonObject coordenadas = new JsonObject();
        //String time2 = DateFormat.getDateTimeInstance().format(new Date());
        String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        coordenadas.addProperty("id",1);
        coordenadas.addProperty("latitud", latitud);
        coordenadas.addProperty("longitud",longitud);
        coordenadas.addProperty("fecha", time2);
        coordenadas.addProperty("estado", "no disponible");

        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");


        RequestBody body = RequestBody.create(JSON, coordenadas.toString());


        Request request = new Request.Builder()
                .url(urlUpdate)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("Miguel", "error");

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.servicioCancelado, Toast.LENGTH_SHORT).show();
                        imageViewCancel.setVisibility(View.VISIBLE);
                        imageViewRun.setVisibility(View.GONE);
                        imageViewPause.setVisibility(View.GONE);
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                    }
                });
                Log.i("Miguel", responseString);

            }


        });
    }

    private void getCoordenadasPausa() {

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
                            Toast.makeText(getApplicationContext(), R.string.errorPause, Toast.LENGTH_LONG).show();
                            if(progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                        }
                    });
                    Log.i("Miguel", "error");
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

                    try {
                        registrarBusEnPausa(latitud, longitud);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
        }
    }

    private void getCoordenadasCancelado() {

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
                            Toast.makeText(getApplicationContext(), R.string.errorCancel, Toast.LENGTH_LONG).show();
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
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

                    try {
                        registrarBusSinServicio(latitud, longitud);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
        }
    }

    public void cargarEstadoBus()throws Exception {

        Request request = new Request.Builder()
                .url(urlGet)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();
                Gson gson = new Gson();
                coordenadasArray = gson.fromJson(responseString, Coordenadas.class);
                int coordSize = coordenadasArray.getCoordenadas().size();
                for (int i = 0; i < coordSize; i++) {
                    coordenadasList.add(coordenadasArray.getCoordenadas().get(i));
                }
                final String estado = coordenadasList.get(coordenadasList.size() - 1).getEstado();

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        if (estado.compareTo("disponible") == 0) {
                            imageViewRun.setVisibility(View.GONE);
                            imageViewCancel.setVisibility(View.GONE);
                            imageViewPause.setVisibility(View.GONE);
                        } else if (estado.compareTo("detenido") == 0) {
                            imageViewRun.setVisibility(View.GONE);
                            imageViewCancel.setVisibility(View.GONE);
                            imageViewPause.setVisibility(View.VISIBLE);
                        } else {
                            imageViewRun.setVisibility(View.GONE);
                            imageViewCancel.setVisibility(View.VISIBLE);
                            imageViewPause.setVisibility(View.GONE);
                        }
                    }
                });
            }

        });
    }

    public void registrarBusEnPausa(double latitud, double longitud )throws Exception   {

        JsonObject coordenadas = new JsonObject();
        //String time2 = DateFormat.getDateTimeInstance().format(new Date());
        String time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        coordenadas.addProperty("id",1);
        coordenadas.addProperty("latitud",latitud);
        coordenadas.addProperty("longitud",longitud);
        coordenadas.addProperty("fecha", time2);
        coordenadas.addProperty("estado", "detenido");

        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(JSON, coordenadas.toString());

        Request request = new Request.Builder()
                .url(urlUpdate)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.servicioPausado, Toast.LENGTH_SHORT).show();
                        imageViewPause.setVisibility(View.VISIBLE);
                        imageViewCancel.setVisibility(View.GONE);
                        imageViewRun.setVisibility(View.GONE);
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                    }
                });
                Log.i("Miguel", responseString);


            }

        });
    }

    public boolean validarUbicacion(){
        LocationManager locationManager = null;
        boolean servicio = false;
        boolean gps_enabled = false,network_enabled = false;
        if(locationManager==null)
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){}
        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){}

        if(!gps_enabled && !network_enabled){

            sinUbicacion.setVisibility(View.VISIBLE);
            blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
            sinUbicacion.startAnimation(blink);
            servicio = true;
        }else {
            sinUbicacion.setVisibility(View.GONE);
            sinUbicacion.clearAnimation();
        }
        return servicio;
    }

    public boolean validarRed() {
        boolean Red = false;
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null){
            sinInternet.setVisibility(View.VISIBLE);
            blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
            sinInternet.startAnimation(blink);
            Red = true;
        }else {
            sinInternet.setVisibility(View.GONE);
            sinInternet.clearAnimation();
        }
        return Red;
    }

    private void handlerGetServicios() {
        try {
            h = new Handler();
            delay = 60000; //milliseconds

            getServiciosInternetUbicacion = new Runnable() {
                @Override
                public void run() {
                    h.postDelayed(this, delay);
                    validarUbicacion();
                    validarRed();
                }
            };

            h.postDelayed(getServiciosInternetUbicacion, delay);

           /* h.postDelayed(new Runnable() {
                public void run() {
                    //do something
                    h.postDelayed(this, delay);
                    getCoordenadas();
                }
            }, delay);*/
        }catch (Exception e){

        }
    }

    private void handlerGetEstadoBus() {
        try {
            handlerGetEstado = new Handler();
            delayGetEstado = 10000; //milliseconds

            getServiciosEstadoBus = new Runnable() {
                @Override
                public void run() {
                    handlerGetEstado.postDelayed(this, delayGetEstado);
                    getEstadoBus();
                }
            };

            handlerGetEstado.postDelayed(getServiciosEstadoBus, delayGetEstado);

           /* h.postDelayed(new Runnable() {
                public void run() {
                    //do something
                    h.postDelayed(this, delay);
                    getCoordenadas();
                }
            }, delay);*/
        }catch (Exception e){

        }
    }

    private void getEstadoBus (){

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
                        Toast.makeText(getApplicationContext(), "Error al obtener el estado del servicio", Toast.LENGTH_SHORT).show();
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
                    estado = coordenadasArray.getCoordenadas().get(i).getEstado();
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                        if (estado.compareTo("disponible")==0){
                            imageViewRun.setVisibility(View.VISIBLE);
                            imageViewCancel.setVisibility(View.GONE);
                            imageViewPause.setVisibility(View.GONE);
                        }else if (estado.compareTo("no disponible")==0){
                            imageViewRun.setVisibility(View.GONE);
                            imageViewCancel.setVisibility(View.VISIBLE);
                            imageViewPause.setVisibility(View.GONE);
                        }else{
                            imageViewRun.setVisibility(View.GONE);
                            imageViewCancel.setVisibility(View.GONE);
                            imageViewPause.setVisibility(View.VISIBLE);

                        }
                    }

                });
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

            active = true;
          //  validarRed();
           // validarUbicacion();
           // handlerGetServicios();
            if (prefs.getBoolean("firstrun", true)) {
                imageViewRun.setVisibility(View.GONE);
                imageViewCancel.setVisibility(View.VISIBLE);
                imageViewPause.setVisibility(View.GONE);
                prefs.edit().putBoolean("firstrun", false).commit();
            }else {
                try {
                 //   cargarEstadoBus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        h.removeCallbacks(getServiciosInternetUbicacion);
//        h.removeCallbacks(getServiciosEstadoBus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
           // stop();
            active = false;
            imageViewRun.setVisibility(View.INVISIBLE);
            prefs.edit().putBoolean("firstrun", true).commit();
 //           h.removeCallbacks(getServiciosInternetUbicacion);
//            h.removeCallbacks(getServiciosEstadoBus);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.salir)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();


    }

    /*@Override
    protected void onPause() {

        sharedPref = getSharedPreferences("imagenes", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (this.isFinishing()) {
            imageViewRun.setVisibility(View.INVISIBLE);
            imageViewPause.setVisibility(View.INVISIBLE);
            imageViewCancel.setVisibility(View.VISIBLE);
        }
            editor.putInt("imagenRun", imageViewRun.getVisibility());
            editor.putInt("imagenPause", imageViewPause.getVisibility());
            editor.putInt("imagenCancel", imageViewCancel.getVisibility());
            editor.commit();

        super.onPause();
    }

    @Override
    protected void onResume() {

         sharedPref = getSharedPreferences("imagenes", Context.MODE_PRIVATE);

        imageViewRun.setVisibility(sharedPref.getInt("imagenRun", View.INVISIBLE));
        imageViewPause.setVisibility(sharedPref.getInt("imagenPause", View.INVISIBLE));
        imageViewCancel.setVisibility(sharedPref.getInt("imagenCancel", View.INVISIBLE));
        super.onResume();
    }*/
}
