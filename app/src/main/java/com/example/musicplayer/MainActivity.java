package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button playerOrder, previous, playOrPause, next;
    private RecyclerView songList;
    private SeekBar seekBar;
    private TextView title, process, duration, artist;

    private List<Song> songs;
    private MediaPlayer mPlayer;
    private SongListAdapter adapter;

    private boolean sorted = true;
    int playingPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mPlayer = new MediaPlayer();

        this.requirePermission();
        this.assignView();
        this.setRecycler();
        this.assignAction();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void requirePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    private String dur2Str(long dur) {
        dur /= 1000;
        long sec = dur % 60;
        long min = dur / 60;
        return String.format(Locale.ENGLISH, "%02d:%02d", min, sec);
    }

    private void changePlayOrder() {
        if (sorted) {
            this.adapter.shuffle();
            this.playerOrder.setText(R.string.sort);
            sorted = false;
        } else {
            sorted = true;
            this.playerOrder.setText(R.string.shuffle);
            this.adapter.sort();
        }
    }

    private void previousSong() {
        initPlayer((this.playingPosition - 1 + this.songs.size()) % this.songs.size());
    }

    private void nextSong() {
        initPlayer((this.playingPosition + 1) % this.songs.size());
    }

    private void playOrPauseSong() {
        if (this.mPlayer.isPlaying()) {
            this.mPlayer.pause();
            this.playOrPause.setText(R.string.play);
        } else {
            this.mPlayer.start();
            this.playOrPause.setText(R.string.pause);
        }
    }

    void initPlayer(int position) {
        this.playingPosition = position;
        Song song = this.songs.get(position);
        this.playOrPause.setText(R.string.pause);
        this.initPlayStatus(song);
        this.initProcessUpdater();

        try {
            this.mPlayer.reset();
            this.mPlayer.setDataSource(song.getPath());
            this.mPlayer.prepare();
            this.mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "init media player error!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void initProcessUpdater() {
        new Thread( () -> {
            try {
                while (!Thread.interrupted()) {
                    Thread.sleep(250);
                    runOnUiThread(this::updatePlayingProgress);
                }
            } catch (InterruptedException e) {
                runOnUiThread(() ->
                    Toast.makeText(this,
                            "processUpdater has been interrupt, the position of playing may update no longer.",
                            Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void initPlayStatus(Song song) {
        this.title.setText(song.getTitle());
        this.artist.setText(song.getArtist());
        this.duration.setText(song.getDurationStr());
        this.process.setText(this.dur2Str(0L));

        this.seekBar.setMin(0);
        this.seekBar.setMax(song.getDuration().intValue());
    }

    private void setPlayerProgress(int process) {
        this.mPlayer.seekTo(process);
        this.process.setText(this.dur2Str(process));
    }

    private void updatePlayingProgress() {
        this.seekBar.setProgress(this.mPlayer.getCurrentPosition());
        this.process.setText(this.dur2Str(this.mPlayer.getCurrentPosition()));
    }

    private void assignAction() {
        this.playerOrder.setOnClickListener((View v) -> this.changePlayOrder());
        this.previous.setOnClickListener((View v) -> this.previousSong());
        this.playOrPause.setOnClickListener((View v) -> this.playOrPauseSong());
        this.next.setOnClickListener((View v) -> this.nextSong());

        this.setSeekBar();
    }

    private void assignView() {
        this.playerOrder = this.findViewById(R.id.button_playerOrder);
        this.previous = this.findViewById(R.id.button_previous);
        this.playOrPause = this.findViewById(R.id.button_play_pause);
        this.next = this.findViewById(R.id.button_next);

        this.duration = this.findViewById(R.id.textView_current_duration);
        this.process = this.findViewById(R.id.textView_current_process);
        this.title = this.findViewById(R.id.textView_current_title);
        this.artist = this.findViewById(R.id.textView_current_artist);
        this.seekBar = this.findViewById(R.id.seekBar_process);

        this.songList = this.findViewById(R.id.recycler_songList);
        this.songList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setSeekBar() {
        this.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setPlayerProgress(seekBar.getProgress());
            }
        });
    }

    private void setRecycler() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE;
        Cursor cursor = this.getContentResolver().query(uri, null, selection, null, sortOrder);
        this.songs = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            int id, title, artist, duration, path;
            id = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            duration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            path = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                Song song = new Song();
                song.setId(cursor.getLong(id));
                song.setTitle(cursor.getString(title));
                song.setArtist(cursor.getString(artist));
                song.setDuration(cursor.getLong(duration));
                song.setPath(cursor.getString(path));

                this.songs.add(song);
            } while (cursor.moveToNext());
            cursor.close();
        }

        Log.d(this.getClass().getName(), String.format("we get %d song(s).", this.songs.size()));

        this.adapter = new SongListAdapter(this, songs);
        this.songList.setAdapter(adapter);
    }
}
