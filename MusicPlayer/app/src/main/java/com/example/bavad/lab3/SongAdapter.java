package com.example.vbatuchenko.lab3;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInflater;

    public SongAdapter(Context c, ArrayList<Song> songs) {
        this.songs = songs;
        songInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout songLayout = (LinearLayout)songInflater.inflate
                (R.layout.song, parent, false);
        TextView songView = (TextView)songLayout.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLayout.findViewById(R.id.song_artist);
        Song currSong = songs.get(position);
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        songLayout.setTag(position);
        return songLayout;
    }
}
