package com.example.iotcontroller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.analytics.AnalyticsCollector;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.PlayerView;

import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.iotcontroller.model.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VideoPlayerFragment extends Fragment {

    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private Button btnSelectVideo;
    private ArrayList<Video> videos;
    private BroadcastReceiver broadcastReceiver;

    private final ActivityResultLauncher<String> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri ->{
                if(uri != null)
                    startVideoPlaylist(uri);
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openPicker();
                } else {
                    Toast.makeText(getContext(), "Cần quyền truy cập để xem video", Toast.LENGTH_SHORT).show();
                }
            });

    public VideoPlayerFragment() {
        // Required empty public constructor
    }

    public static VideoPlayerFragment newInstance() {
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_player, container, false);

        playerView = rootView.findViewById(R.id.playerView);
        btnSelectVideo = rootView.findViewById(R.id.btnSelectVideo);
        videos = new ArrayList<>();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String actionName = intent.getStringExtra("action");
                Log.d("VIDEO_FRAGMENT", "Received Action: " + actionName);
                if ("SEEK_NEXT".equals(actionName)) {
                    seekToNext();
                } else if ("SEEK_PREV".equals(actionName)) {
                    seekToPrev();
                }
            }
        };

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                broadcastReceiver, new IntentFilter("COMMAND_VIDEO"));

        initializePlayer();

        btnSelectVideo.setOnClickListener(v ->{
            checkPermissionAndPick();
        });

        return rootView;
    }

    private void checkPermissionAndPick() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_VIDEO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        requestPermissionLauncher.launch(permission);
    }

    private ArrayList<Video> getAllVideos(){
        ArrayList<Video> videoList = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME
        };

        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try(Cursor cursor = requireContext().getContentResolver().query(collection,projection, null, null, sortOrder)) {
            if(cursor != null){
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                while (cursor.moveToNext()){
                    long id = cursor.getLong(idCol);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    videoList.add(new Video(id, uri, cursor.getString(1)));
                }
            }
        }

        return videoList;
    }

    private void openPicker(){
        videoPickerLauncher.launch("video/*");
    }

    private void initializePlayer(){
        exoPlayer = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(exoPlayer);
    }

    private void startVideoPlaylist(Uri selectedUri){
        Executors.newSingleThreadExecutor().execute(() -> {
            long selectedId = -1;
            try {
                String idString = DocumentsContract.getDocumentId(selectedUri);
                if (idString.contains(":")) {
                    selectedId = Long.parseLong(idString.split(":")[1]);
                } else {
                    selectedId = Long.parseLong(idString);
                }
            } catch (Exception e) {
                // Nếu không lấy được qua DocumentsContract, thử lấy ID cuối cùng của URI
                try {
                    selectedId = ContentUris.parseId(selectedUri);
                } catch (Exception ignored) {}
            }

            final long finalSelectedId = selectedId;
            videos = getAllVideos();

            getActivity().runOnUiThread(() ->{
                int startIndex = 0;

                for (int i = 0; i < videos.size(); i++) {
                    long currentVideoId = ContentUris.parseId(videos.get(i).getUri());
                    if (currentVideoId == finalSelectedId) {
                        startIndex = i;
                        Log.d("VIDEO_FRAGMENT", "StartIndex: " + startIndex);
                        break;
                    }
                }

                exoPlayer.clearMediaItems();
                for (Video video : videos) {
                    exoPlayer.addMediaItem(MediaItem.fromUri(video.getUri()));
                }

                exoPlayer.seekTo(startIndex, 0);
                exoPlayer.prepare();
                exoPlayer.play();

                btnSelectVideo.setVisibility(View.GONE);
            });
        });
    }

    public void seekToNext(){
        if(exoPlayer != null && exoPlayer.hasNextMediaItem())
            exoPlayer.seekToNext();
    }

    public void seekToPrev(){
        if(exoPlayer != null && exoPlayer.hasPreviousMediaItem())
            exoPlayer.seekToPrevious();
    }

    private void sendStateToService(boolean isVisible) {
        Intent intent = new Intent("VIDEO_COMMAND");
        intent.putExtra("IS_VISIBLE", isVisible);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        Log.d("VIDEO_FRAGMENT", "visibility state: " + isVisible);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            if (exoPlayer != null) exoPlayer.pause();
            sendStateToService(false);
        } else {
            sendStateToService(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sendStateToService(false);
        if(exoPlayer != null && exoPlayer.isPlaying())
            exoPlayer.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        sendStateToService(true);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
        if(exoPlayer != null){
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}