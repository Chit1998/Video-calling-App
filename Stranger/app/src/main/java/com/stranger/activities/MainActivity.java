package com.stranger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stranger.R;
import com.stranger.databinding.ActivityMainBinding;
import com.stranger.model.UserModel;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;

    ActivityMainBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference reference;
    ValueEventListener listener;
    long coins = 0;
    UserModel user;

    String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference().child("user");

        if (auth.getCurrentUser() != null){
            listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    user = snapshot.getValue(UserModel.class);
                    Log.d("TAG_MAIN", "onDataChange: "+user);
                    Glide.with(getApplicationContext())
                    .load(user.getPhotoUrl())
                    .centerCrop()
                    .into(binding.profileImage);

                    coins = Long.parseLong(user.getCoins());

                    binding.textViewCoins.setText("You have: "+coins);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            reference.child(Objects.requireNonNull(auth.getUid())).addValueEventListener(listener);
        }



        binding.findButton.setOnClickListener(v -> {
            if (isPermissionGranted()){
                if (coins > 5){
                    Toast.makeText(this, "Call finding...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), ConnectingActivity.class)
                            .putExtra("profile", user.getPhotoUrl()));
                }else {
                    Toast.makeText(this, "Insufficient coins balance...", Toast.LENGTH_SHORT).show();
                }
            }else {
                askPermission();
            }
        });
    }

    void askPermission(){
        ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE);
    }

    private boolean isPermissionGranted(){
        for (String permission: permissions){
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

}