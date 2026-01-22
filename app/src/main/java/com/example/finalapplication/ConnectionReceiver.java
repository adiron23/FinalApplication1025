package com.example.finalapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

public class ConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifiConn = false;
        boolean isMobileConn = false;

        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities != null) {
                    isWifiConn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    isMobileConn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            }
        }

         if (!isWifiConn && !isMobileConn) {
            Toast.makeText(context, "אין חיבור אינטרנט", Toast.LENGTH_LONG).show();
        }
    }
}