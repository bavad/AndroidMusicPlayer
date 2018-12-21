package com.example.vbatuchenko.lab3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import java.net.URL;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener{

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int currSongPosition;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle = "";
    private String songArtist = "";
    private boolean shuffle = false;
    private Random rand;
    private AudioManager mAudioManager;
    SongProgress songProcess;

    @Override
    public void onCreate(){
        super.onCreate();

        currSongPosition = 0;
        player = new MediaPlayer();

        initMusicPlayer();

        rand=new Random();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public void setShuffle(){
        shuffle = !shuffle;
    }

    public boolean isShuffle(){
        return shuffle;
    }

    public void initMusicPlayer(){
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> songs){
        this.songs=songs;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(focusChange <= 0) {
            pausePlayer();
        }
        else{
            go();
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        player.reset();
        Song playSong = songs.get(currSongPosition);
        songTitle = playSong.getTitle();
        songArtist = playSong.getArtist();
        long currSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
        }

        player.prepareAsync();
    }

    public void setSong(int songIndex){
        currSongPosition=songIndex;
    }

    public int getPos(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int pos){
        player.seekTo(pos);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        currSongPosition--;
        if(currSongPosition < 0){
            currSongPosition = songs.size()-1;
        }

        playSong();
    }

    public void playNext() {
        if(shuffle){
            int newSongPosition = currSongPosition;
            while(newSongPosition == currSongPosition){
                newSongPosition = rand.nextInt(songs.size());
            }
            currSongPosition = newSongPosition;
        }
        else{
            currSongPosition++;
            if(currSongPosition == songs.size()){
                currSongPosition = 0;
            }
        }
        playSong();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        startMyOwnForeground(pendInt);
    }

    @Override
    public void onDestroy() {
        songProcess.cancel(true);
        stopForeground(true);
    }

    private void startMyOwnForeground(PendingIntent pendingIntent){
        String NOTIFICATION_CHANNEL_ID = "musicService";
        String channelName = "Music Service";
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_play_round)
                .setContentTitle(songArtist + " " + String.valueOf(milliSecondsToTimer(player.getCurrentPosition())))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentText(songTitle)
                .build();
        startForeground(2, notification);

        songProcess = new SongProgress(notificationBuilder, manager, pendingIntent);
        songProcess.execute();
    }

    private String milliSecondsToTimer(int milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        int hours = (milliseconds / (1000 * 60 * 60));
        int minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        if (seconds < 10) {
            secondsString = "0" + seconds;
        }   else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        return finalTimerString;
    }

    private class SongProgress extends AsyncTask<Void, Void, Void> {
        private NotificationCompat.Builder _builder;
        private NotificationManager _manager;
        private PendingIntent _pendingIntent;

        public SongProgress(NotificationCompat.Builder builder, NotificationManager manager, PendingIntent pendingIntent) {
            super();
            _builder = builder;
            _manager = manager;
            _pendingIntent = pendingIntent;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (player.getCurrentPosition() != player.getDuration()){
                Notification notification = _builder.setOngoing(true)
                        .setContentIntent(_pendingIntent)
                        .setSmallIcon(R.mipmap.ic_play_round)
                        .setContentTitle(songArtist + " " + String.valueOf(milliSecondsToTimer(player.getCurrentPosition())))
                        .setPriority(NotificationManager.IMPORTANCE_MIN)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setContentText(songTitle)
                        .build();
                startForeground(2, notification);

                if (isCancelled()){
                    break;
                }
            }

            --currSongPosition;

            return null;
        }
    }
}
