 package com.application.saveyoursoul;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    private EditText contactName;
    private EditText contactNumber;
    private Button contactButton;
    private ListView contactListView;
    public static List<String> contactList;
    public static List<String> savedContactList;
    private ArrayAdapter<String> contactAdapter;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private static final int MY_PERMISSION_READ_PHONE_STATE = 1;
    private static final int MY_PERMISSION_ACCESS_LOCATION = 2;

    private Double longitude, latitude;
    private boolean isGPS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService(new Intent(getApplicationContext(), LockService.class));

        contactName = findViewById(R.id.contactName);
        contactNumber = findViewById(R.id.contactNumber);
        contactButton = findViewById(R.id.contactAdd);
        contactListView = findViewById(R.id.contactList);

        Database database = new Database(MainActivity.this);

        savedContactList = database.getContactList();
        contactList = database.getContacts();
        contactAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, savedContactList);
        contactListView.setAdapter(contactAdapter);

        new GPS(this).turnGPSOn(new GPS.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });

        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contactName.getText() == null || contactNumber.getText() == null) {
                    Toast.makeText(MainActivity.this, "Fields are Empty!!", Toast.LENGTH_SHORT).show();
                } else if(contactNumber.getText().toString().length()!=10){
                    Toast.makeText(MainActivity.this, "Contact number should be of 10 digit !!", Toast.LENGTH_SHORT).show();
                } else{
                    String name = contactName.getText().toString();
                    String phone = contactNumber.getText().toString();

                    if (savedContactList.contains("Contact List is empty!!")) {
                        contactAdapter.clear();
                        savedContactList.clear();
                    }
                    savedContactList.add(name + " \t " + phone);
                    contactList.add(phone);
                    contactListView.setAdapter(contactAdapter);
                    database.addOne(name, phone);

                    Toast.makeText(MainActivity.this, "Contact Added Successfully", Toast.LENGTH_SHORT).show();
                }
            }
        });
        contactListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder alertDialog= new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Are you Sure?").setMessage("Do you want to delete this contact?")
                        .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String string = (String) parent.getItemAtPosition(position);
                                String phone= "";

                                for(int i=string.length()-10;i<string.length();i++){
                                    char c=string.charAt(i);
                                    phone += c;
                                }
                                Log.i("phone", phone.toString());

                                savedContactList.remove(position);
                                contactList.remove(position);
                                contactListView.setAdapter(contactAdapter);
                                database.delete(phone);
                            }
                        })
                        .setPositiveButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();

                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        checkSmsPermission();
        checkPhonePermission();
        checkLocationPermission();

    }
    public void checkSmsPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }
    public void checkPhonePermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSION_READ_PHONE_STATE);
            }
        }
    }
    public void checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(MyApplication.getAppContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_ACCESS_LOCATION);
            }
        }
    }

}