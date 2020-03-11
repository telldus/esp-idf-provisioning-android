// Copyright 2018 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.espressif.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.espressif.AppConstants;
import com.espressif.provision.BuildConfig;
import com.espressif.provision.Provision;
import com.espressif.provision.R;

import java.util.HashMap;

public class EspMainActivity extends AppCompatActivity {

    private static final String TAG = "Espressif::" + EspMainActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION = 1;

    private String transportVersion, securityVersion;
    private String BASE_URL;
    private Button btnProvision, btnManageDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BASE_URL = getResources().getString(R.string.wifi_base_url);
        initViews();

        SharedPreferences sharedPreferences = getSharedPreferences(AppConstants.ESP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String wifiNamePrefix = sharedPreferences.getString(AppConstants.KEY_WIFI_NETWORK_NAME_PREFIX, "");
        String bleDevicePrefix = sharedPreferences.getString(AppConstants.KEY_BLE_DEVICE_NAME_PREFIX, "");

        if (TextUtils.isEmpty(wifiNamePrefix)) {

            wifiNamePrefix = getResources().getString(R.string.wifi_network_name_prefix);
            editor.putString(AppConstants.KEY_WIFI_NETWORK_NAME_PREFIX, wifiNamePrefix);
        }

        if (TextUtils.isEmpty(bleDevicePrefix)) {

            bleDevicePrefix = getResources().getString(R.string.ble_device_name_prefix);
            editor.putString(AppConstants.KEY_BLE_DEVICE_NAME_PREFIX, bleDevicePrefix);
        }

        editor.apply();

        if (BuildConfig.FLAVOR_security.equals("sec1")) {
            securityVersion = Provision.CONFIG_SECURITY_SECURITY1;
        } else {
            securityVersion = Provision.CONFIG_SECURITY_SECURITY0;
        }

        if (BuildConfig.FLAVOR_transport.equals("ble")) {
            transportVersion = Provision.CONFIG_TRANSPORT_BLE;
        } else {
            transportVersion = Provision.CONFIG_TRANSPORT_WIFI;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BuildConfig.FLAVOR_transport.equals("ble") && BLEProvisionLanding.isBleWorkDone) {

            if (BLEProvisionLanding.bleTransport != null) {
                BLEProvisionLanding.bleTransport.disconnect();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_app_info) {
            Intent intent = new Intent(this, AppInfoActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION) {

            Log.d(TAG, "REQUEST_LOCATION result received");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                if (isLocationEnabled()) {

                    HashMap<String, String> config = new HashMap<>();
                    config.put(Provision.CONFIG_TRANSPORT_KEY, transportVersion);
                    config.put(Provision.CONFIG_SECURITY_KEY, securityVersion);
                    config.put(Provision.CONFIG_BASE_URL_KEY, BASE_URL);

                    if (BuildConfig.FLAVOR_avs.equals("avs")) {

                        Provision.showProvisioningWithAmazonUI(EspMainActivity.this,
                                config);
                    } else {

                        Provision.showProvisioningUI(EspMainActivity.this, config);
                    }
                }
            }
        }
    }

    View.OnClickListener provisionBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vib.vibrate(HapticFeedbackConstants.VIRTUAL_KEY);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                if (!isLocationEnabled()) {
                    askForLocation();
                    return;
                }
            }

            HashMap<String, String> config = new HashMap<>();
            config.put(Provision.CONFIG_TRANSPORT_KEY, transportVersion);
            config.put(Provision.CONFIG_SECURITY_KEY, securityVersion);
            config.put(Provision.CONFIG_BASE_URL_KEY, BASE_URL);

            if (BuildConfig.FLAVOR_avs.equals("avs")) {

                Provision.showProvisioningWithAmazonUI(EspMainActivity.this,
                        config);
            } else {

                Provision.showProvisioningUI(EspMainActivity.this, config);
            }
        }
    };

    View.OnClickListener manageDeviceBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vib.vibrate(HapticFeedbackConstants.VIRTUAL_KEY);

            Intent goToManagePage = new Intent(getApplicationContext(), ScanLocalDevices.class);
            startActivity(goToManagePage);
        }
    };

    private void initViews() {

        btnProvision = findViewById(R.id.btn_start_provision);
        btnManageDevice = findViewById(R.id.manageButton);
        btnProvision.setOnClickListener(provisionBtnClickListener);
        btnManageDevice.setOnClickListener(manageDeviceBtnClickListener);
    }

    private void askForLocation() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(R.string.dialog_msg_for_gps);

        // Set up the buttons
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private boolean isLocationEnabled() {

        boolean gps_enabled = false;
        boolean network_enabled = false;
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Activity.LOCATION_SERVICE);

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        Log.d(TAG, "GPS Enabled : " + gps_enabled + " , Network Enabled : " + network_enabled);

        boolean result = gps_enabled || network_enabled;
        return result;
    }
}