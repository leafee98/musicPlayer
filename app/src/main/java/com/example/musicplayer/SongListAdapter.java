package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, artist, duration;
        View wholeView;

        ViewHolder(@NonNull View item) {
            super(item);
            this.title = item.findViewById(R.id.textView_title);
            this.artist = item.findViewById(R.id.textView_artist);
            this.duration = item.findViewById(R.id.textView_duration);
            this.wholeView = item;
        }
    }

    private List<Song> songs;
    private MainActivity main;

    SongListAdapter(MainActivity main, List<Song> songs) {
        this.main = main;
        this.songs = songs;
    }

    void shuffle() {
        long id = this.songs.get(main.playingPosition).getId();
        Collections.shuffle(this.songs);
        for (int i = 0; i < this.songs.size(); ++i)
            if (this.songs.get(i).getId().equals(id)) {
                main.playingPosition = i;
                break;
            }
        this.notifyItemRangeChanged(0, this.songs.size());
    }

    void sort() {
        long id = this.songs.get(main.playingPosition).getId();
        Collections.sort(this.songs, (Song a, Song b) -> a.getTitle().compareTo(b.getTitle()));
        for (int i = 0; i < this.songs.size(); ++i)
            if (this.songs.get(i).getId().equals(id)) {
                main.playingPosition = i;
                break;
            }
        this.notifyItemRangeChanged(0, this.songs.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_song_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = this.songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.duration.setText(song.getDurationStr());

        holder.wholeView.setOnLongClickListener((View v) -> {
            this.songs.remove(position);
            this.notifyItemRemoved(position);
            this.notifyItemRangeChanged(position, this.getItemCount());
            Toast.makeText(this.main, "removed a song.", Toast.LENGTH_LONG).show();
            return true;
        });

        holder.wholeView.setOnClickListener((View v) -> this.main.initPlayer(position));
    }

    @Override
    public int getItemCount() {
        return this.songs.size();
    }
}
