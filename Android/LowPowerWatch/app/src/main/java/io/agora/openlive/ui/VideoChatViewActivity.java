package io.agora.openlive.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

import io.agora.openlive.R;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class VideoChatViewActivity extends AppCompatActivity {

    private static final String LOG_TAG = VideoChatViewActivity.class.getSimpleName();

    private static final boolean DBG = false;

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

    private RtcEngine mRtcEngine;// Tutorial Step 1
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1
        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) { // Tutorial Step 5
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) { // Tutorial Step 7
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat_view);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
            initAgoraEngineAndJoinChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        setupVideoProfile();         // Tutorial Step 2
        setupLocalVideo();           // Tutorial Step 3
        joinChannel();               // Tutorial Step 4
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA);
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
            case PERMISSION_REQ_ID_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showLongToast("No permission for " + Manifest.permission.CAMERA);
                    finish();
                }
                break;
            }
        }
    }

    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {

        leaveChannel();
        RtcEngine.destroy();

        super.onDestroy();

        mRtcEngine = null;
    }

    // Tutorial Step 8
    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }

    // Tutorial Step 6
    public void onEncCallClicked(View view) {
        finish();
    }

    // Tutorial Step 1
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.private_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
        mRtcEngine.setLogFile(Environment.getExternalStorageDirectory()
                + File.separator + getPackageName() + "/log/agora-rtc.log");

        int REQUEST_CODE_CONTACT = 101;
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //check permissions
        for (String str : permissions) {
            if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, REQUEST_CODE_CONTACT);
                return;
            }
        }

        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);

        mRtcEngine.setParameters("{\"che.audio.specify.codec\":\"G722\"}");

    }

    // Tutorial Step 2
    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
        // you can change it by yourself
        mRtcEngine.setVideoProfile(160, 120, 10, 120);

    }


    // Tutorial Step 3
    private void setupLocalVideo() {
        RelativeLayout container = (RelativeLayout) findViewById(R.id.local_video_view_container);

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));

    }

    // Tutorial Step 4
    private void joinChannel() {
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        //you can change channelname by yourself
        mRtcEngine.joinChannel(null, "yourchannel", "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
    }


    // Tutorial Step 5
    private void setupRemoteVideo(int uid) {
        RelativeLayout container = (RelativeLayout) findViewById(R.id.remote_video_view_container);
        if (container.getChildCount() >= 1) {
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        surfaceView.setZOrderMediaOverlay(true);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        surfaceView.setTag(uid); // for mark purpose
        container.invalidate();
    }

    // Tutorial Step 6
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    // Tutorial Step 7
    private void onRemoteUserLeft() {
        RelativeLayout container = (RelativeLayout) findViewById(R.id.remote_video_view_container);
        container.removeAllViews();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}
