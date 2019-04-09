package unab.semovil.busunab;

/**
 * Created by Rene on 7/28/14.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String DEBUG_TAG = "AlarmReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Recurring alarm; requesting location tracking.");
        // start the service
        String estate = intent.getExtras().getString("state");
        Log.i("state","alarm "+estate);
        intent = new Intent(context, UpdateLocation.class);

        intent.putExtra("trackingState",estate);
        context.startService(intent);
    }
}