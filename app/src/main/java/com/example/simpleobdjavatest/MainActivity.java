package com.example.simpleobdjavatest;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simpleobdjavatest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @SuppressLint("MissingPermission")
    @SuppressWarnings("all")
    private final BroadcastReceiver connectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (OBDBluetoothService.ACTION_OBD_STATE.equals(intent.getAction())) {
                int speed = intent.getIntExtra(OBDBluetoothService.EXTRA_OBD_SPEED, 0);

                if (binding != null) {
                    if (speed != 0 ) {
                        binding.testSpeed.setText(speed +"Km/h");
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i("MainActivity","Start OBD-II BluetoothService");
        Intent bsdIntent = new Intent(this, OBDBluetoothService.class);
        startService(bsdIntent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}