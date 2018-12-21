package com.example.vbatuchenko.lab3;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private MusicController controller;
    private boolean isPaused = false;
    private boolean playbackPaused = false;
    private MenuItem shuffleItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        songView = findViewById(R.id.song_list);
        songList = new ArrayList<>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdapter = new SongAdapter(this, songList);
        songView.setAdapter(songAdapter);

        setController();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("paused", isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPaused) {
            setController();
            isPaused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService = null;

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        shuffleItem = menu.findItem(R.id.action_shuffle);
        if(musicService != null && musicService.isShuffle()){
            shuffleItem.setIcon(R.drawable.shuffle);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicService.setShuffle();
                if(musicService.isShuffle()){
                    shuffleItem.setIcon(R.drawable.shuffle);
                }
                else{
                    shuffleItem.setIcon(R.drawable.straight);
                }
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicService = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getPos();
        } else {
            return 0;
        }
    }

    @Override
    public int getDuration() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getDur();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying() {
        if (musicService != null && musicBound) {
            return musicService.isPlaying();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public void pause() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            playbackPaused = true;
            musicService.pausePlayer();
        }
    }

    @Override
    public void seekTo(int pos) {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            musicService.seek(pos);
        }
    }

    @Override
    public void start() {
        if (musicService != null && musicBound && !musicService.isPlaying()) {
            playbackPaused = false;
            musicService.go();
       }
    }


    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(songList);
            musicBound = true;
            if(shuffleItem != null && musicService.isShuffle()){
                shuffleItem.setIcon(R.drawable.shuffle);
            }
            if(musicService.isPlaying()){
                controller.show(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            do {
                long id = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);
                songList.add(new Song(id, title, artist));
            }
            while (musicCursor.moveToNext());

            musicCursor.close();
        }
    }

    public void songPicked(View view) {
        musicService.setSong(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    private void setController() {
        if(controller == null){
            controller = new MusicController(this);
            controller.setPrevNextListeners(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playNext();
                }
            }, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playPrev();
                }
            });
            controller.setMediaPlayer(this);
            controller.setAnchorView(findViewById(R.id.song_list));
            controller.setEnabled(true);
        }
    }

    private void playNext() {
        musicService.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev() {
        musicService.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
}