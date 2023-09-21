package com.stranger.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stranger.R;
import com.stranger.databinding.ActivityCallBinding;
import com.stranger.helper.InterfaceJava;
import com.stranger.model.UserModel;

import org.jetbrains.annotations.NotNull;

import java.util.IllformedLocaleException;
import java.util.UUID;

public class CallActivity extends AppCompatActivity {

    ActivityCallBinding binding;
    String uniqueId = "";
    FirebaseAuth auth;
    String name = "", peerName = "", createdBy;
    boolean isPeerConnection = false;
    DatabaseReference reference;
    boolean isAudio = true;
    boolean isVideo = true;
    boolean pageExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference()
                .child("profiles");
//        uniqueId = auth.getUid();

        name = getIntent().getStringExtra("username");
        createdBy = getIntent().getStringExtra("createdBy");
        String incoming = getIntent().getStringExtra("incoming");

//        if (incoming.equalsIgnoreCase(peerName)){
            peerName = incoming;
//        }

        setUpWebView();

        binding.imageAudioCall.setOnClickListener(v -> {
            isAudio = !isAudio;
            callJavaScriptFun("javascript:toggleAudio(\""+isAudio+"\")");

            if (isAudio){
                binding.imageAudioCall.setImageResource(R.drawable.btn_unmute_normal);
            }else{
                binding.imageAudioCall.setImageResource(R.drawable.btn_mute_normal);
            }
        });

        binding.imageVideoCall.setOnClickListener(v -> {
            isVideo = !isVideo;
            callJavaScriptFun("javascript:toggleVideo(\""+isVideo+"\")");

            if (isVideo){
                binding.imageVideoCall.setImageResource(R.drawable.btn_video_normal);
            }else{
                binding.imageVideoCall.setImageResource(R.drawable.btn_video_muted);
            }
        });

        binding.imageCallEnd.setOnClickListener(v -> finish());

    }

    @SuppressLint("SetJavaScriptEnabled")
    void setUpWebView(){
        binding.webView.setWebChromeClient(new WebChromeClient(){
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onPermissionRequest(PermissionRequest request) {
//                super.onPermissionRequest(request);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }
        });

        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        binding.webView.addJavascriptInterface(new InterfaceJava(CallActivity.this), "Android");

        loadVideoCall();
    }

    public void loadVideoCall(){
        String filePath = "file:android_asset/call.html";
        binding.webView.loadUrl(filePath);

        binding.webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                initializePeer();
            }
        });
    }

    public void initializePeer(){
        uniqueId = getUniqueId();

        callJavaScriptFun("javascript:init(\""+uniqueId+"\")");

        if (createdBy.equalsIgnoreCase(name)){
            if(pageExit)
                return;

            reference.child(name).child("connId").setValue(uniqueId);
            reference.child(name).child("isAvailable").setValue(true);
            binding.controls.setVisibility(View.VISIBLE);

            FirebaseDatabase.getInstance().getReference()
                    .child("user")
                    .child(peerName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            UserModel user = snapshot.getValue(UserModel.class);

                            Glide.with(CallActivity.this).load(user.getPhotoUrl())
                                    .into(binding.imageProfile);
                            binding.textUserName.setText(user.getName());
                            binding.textCity.setText(user.getCity());

                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {
                            finish();
                        }
                    });

        }else {
            new Handler().postDelayed(()->{
                peerName = createdBy;
                FirebaseDatabase.getInstance().getReference()
                        .child("user")
                        .child(peerName)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                                UserModel user = snapshot.getValue(UserModel.class);

                                Glide.with(CallActivity.this).load(user.getPhotoUrl())
                                        .into(binding.imageProfile);
                                binding.textUserName.setText(user.getName());
                                binding.textCity.setText(user.getCity());

                            }

                            @Override
                            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                                finish();
                            }
                        });
                FirebaseDatabase.getInstance().getReference()
                        .child("profiles")
                        .child(peerName)
                        .child("connId")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.getValue() != null) {
                                    sendCallRequest();
                                }else{
                                    finish();
                                    return;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                finish();
                            }
                        });
            },1000);
        }
    }

    public void onPeerConnected(){
        isPeerConnection = true;
    }


    void sendCallRequest(){
        if (!isPeerConnection) {
            Toast.makeText(this, "You are not connected. Please check your internet."+new IllformedLocaleException().getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        listenConnId();
    }

    void listenConnId() {
        reference.child(peerName)
                .child("connId")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getValue() == null) {
                            finish();
                            return;
                        }
                        binding.controls.setVisibility(View.VISIBLE);
                        String connId = snapshot.getValue(String.class);
                        callJavaScriptFun("javascript:startCall(\""+connId+"\")");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        finish();
                    }
                });
    }

    void callJavaScriptFun(String fun){
        binding.webView.post(()->
            binding.webView.evaluateJavascript(fun, null)
        );
    }

    private String getUniqueId() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pageExit = true;
        reference.child(createdBy).setValue(null);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pageExit = true;
        reference.child(createdBy).setValue(null);
        finish();
    }
}