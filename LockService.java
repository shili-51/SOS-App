package com.application.saveyoursoul;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class LockService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID = "ChannelId1";
    NotificationManager manager;
    public static String Address="";
    FusedLocationProviderClient fusedLocationProviderClient;
    private static LocationRequest locationRequest;
    private static LocationCallback locationCallback;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float currX,currY,currZ,lastX,lastY,lastZ;
    boolean isfirst=true;
    private float xDiff,yDiff,zDiff;
    private final float threshold=5f;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(5 * 1000);

        locationCallback = new LocationCallback() {
            @Override

            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(MyApplication.getAppContext(), Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            String adminArea = addresses.get(0).getAdminArea();
                            String countryName = addresses.get(0).getCountryName();
                            String locality = addresses.get(0).getLocality();
                            String subLocality = addresses.get(0).getSubLocality();
                            String postalCode = addresses.get(0).getPostalCode();

                            if(subLocality!=null && subLocality.length()>0) {
                                Address +=subLocality+", ";
                                Log.i("subLocality", subLocality);
                            }
                            if(locality!=null && locality.length()>0) {
                                Address +=locality+", ";
                                Log.i("locality", locality);
                            }
                            if(adminArea!=null && adminArea.length()>0) {
                                Address +=adminArea+", ";
                                Log.i("adminArea", adminArea);
                            }
                            if(countryName!=null && countryName.length()>0) {
                                Address +=countryName+", ";
                                Log.i("countryName", countryName);
                            }
                            if(postalCode!=null && postalCode.length()>0) {
                                Address +=postalCode;
                                Log.i("postalcode", postalCode);
                            }
                            Log.i("ADDRESS,Location Callback",Address);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (fusedLocationProviderClient != null) {
                            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        final BroadcastReceiver mReceiver=new BroadcastReceiver() {
            int count=0;
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    count++;
                    Log.i("SCREEN", "Screen is OFF");
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    count++;
                    Log.i("SCREEN","Screen is ON");
                }
                if(count==4){
                    sendMessage();
                    count=0;
                }
            }
        };
        registerReceiver(mReceiver, filter);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        currX = event.values[0];
        currY = event.values[1];
        currZ = event.values[2];

        if(!isfirst){
            xDiff=Math.abs(lastX-currX);
            yDiff=Math.abs(lastY-currY);
            zDiff=Math.abs(lastZ-currZ);

            if((xDiff>threshold && yDiff>threshold) || (yDiff>threshold && zDiff>threshold) || (zDiff> threshold && xDiff>threshold)) {
                sendMessage();
                Log.i("SHAKING","PHONED SHAKED");
            }
        }
        lastX=currX;
        lastY=currY;
        lastZ=currZ;
        isfirst=false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class LocalBinder extends Binder {
        LockService getService() {
            return LockService.this;
        }
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel=new NotificationChannel(
                    CHANNEL_ID,"Foreground notification", NotificationManager.IMPORTANCE_DEFAULT);
            manager=getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification =
                new Notification.Builder(this,"ChannelId1" )
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .build();
        startForeground(1,notification);
    }

    public void checkLocation() {
        if (ActivityCompat.checkSelfPermission(MyApplication.getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Location", "permission required");
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                Log.i("Location", String.valueOf(location));
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MyApplication.getAppContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        String adminArea = addresses.get(0).getAdminArea();
                        String countryName = addresses.get(0).getCountryName();
                        String locality = addresses.get(0).getLocality();
                        String subLocality = addresses.get(0).getSubLocality();
                        String postalCode = addresses.get(0).getPostalCode();


                        if(subLocality!=null) {
                            Address +=subLocality+", ";
                            Log.i("subLocality", subLocality);
                        }
                        if(locality!=null) {
                            Address +=locality+", ";
                            Log.i("locality", locality);
                        }
                        if(adminArea!=null) {
                            Address +=adminArea+", ";
                            Log.i("adminArea", adminArea);
                        }
                        if(countryName!=null) {
                            Address +=countryName+", ";
                            Log.i("countryName", countryName);
                        }
                        if(postalCode!=null) {
                            Address +=postalCode;
                            Log.i("postalcode", postalCode);
                        }
                        Log.i("ADDRESS,checkLocation",Address);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (ActivityCompat.checkSelfPermission(MyApplication.getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            }
        });
    }
    public void sendMessage() {
        checkLocation();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String message = "Please Help I'm in Danger!!\n"+"ADDRESS: "+Address;
                Log.i("message",message);
                if (!MainActivity.contactList.isEmpty()) {
                    for (String phoneNumber : MainActivity.contactList) {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                    }
                }
            }
        }, 5000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        manager.deleteNotificationChannel("ChannelId1");
        stopForeground(true);
        stopSelf();
    }
}