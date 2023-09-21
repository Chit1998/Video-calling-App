package com.stranger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stranger.R;
import com.stranger.databinding.ActivityConnectingBinding;

import java.util.HashMap;
import java.util.Objects;

public class ConnectingActivity extends AppCompatActivity {

    ActivityConnectingBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase db;
    boolean isOkay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        Glide.with(getApplicationContext())
                .load(getIntent().getStringExtra("profile"))
                .centerCrop()
                .into(binding.imageProfile);

        String username = auth.getUid();

        db.getReference()
                .child("profiles")
                .orderByChild("status")
                .equalTo(0).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getChildrenCount() > 0){
                            isOkay = true;
//                            Rooms available hai
                            Log.d("err","available");
                            for (DataSnapshot snap: snapshot.getChildren()){
                                db.getReference()
                                        .child("profiles")
                                        .child(snap.getKey())
                                        .child("incoming")
                                        .setValue(username);
                                db.getReference()
                                        .child("profiles")
                                        .child(snap.getKey())
                                        .child("status")
                                        .setValue(1);

                                startActivity(new Intent(getApplicationContext(), CallActivity.class)
                                        .putExtra("username",username)
                                        .putExtra("incoming",snap.child("incoming").getValue(String.class))
                                        .putExtra("createdBy",snap.child("createdBy").getValue(String.class))
                                        .putExtra("isAvailable",snap.child("isAvailable").getValue(Boolean.class)));


                            }
                        }else {
//                            Rooms not available
                            HashMap<String, Object> room = new HashMap<>();
                            room.put("incoming",username);
                            room.put("createdBy",username);
                            room.put("isAvailable",true);
                            room.put("status",0);

                            db.getReference().child("profiles")
                                    .child(username)
                                    .setValue(room)
                                    .addOnSuccessListener(unused -> {
                                        db.getReference().child("profiles")
                                                .child(username)
                                                .addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (snapshot.child("status").exists()){
                                                            if (Objects.equals(snapshot.child("status").getValue(Integer.class), 1)){
                                                                if (isOkay){
                                                                    return;
                                                                }
                                                                isOkay = true;
                                                                startActivity(new Intent(getApplicationContext(), CallActivity.class)
                                                                        .putExtra("username",username)
                                                                        .putExtra("incoming",snapshot.child("incoming").getValue(String.class))
                                                                        .putExtra("createdBy",snapshot.child("createdBy").getValue(String.class))
                                                                        .putExtra("isAvailable",snapshot.child("isAvailable").getValue(Boolean.class)));
                                                                finish();
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}