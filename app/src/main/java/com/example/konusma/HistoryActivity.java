package com.example.konusma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class HistoryActivity extends AppCompatActivity {
    private ImageView backButton;
    private TextView infoText;
    private RecyclerView recycler;

    private HistoryAdapter adapter;
    private ItemTouchHelper itemTouchHelper;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private HistoryManager historyManager;

    private MediaPlayer mediaPlayer;
    private Timer timer;

    private TextView pCurrent;
    private SeekBar pSeekBar;
    private boolean isPlaying;
    private int playingEntryIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        sp = getSharedPreferences("Konusma", MODE_PRIVATE);
        spe = sp.edit();

        historyManager = new HistoryManager(sp.getString("history", ""));

        isPlaying = false;
        playingEntryIndex = -1;

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        infoText = findViewById(R.id.infoText);

        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recycler.setAdapter(adapter);
        itemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recycler);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets insets3 = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(Math.max(insets1.left, insets3.left), insets1.top, Math.max(insets1.right, insets3.right), Math.max(insets1.bottom, insets2.bottom));
            return insets;
        });
    }

    @Override
    protected void onResume() {
        setNoItemsView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        spe.putString("history", historyManager.toJsonString()).commit();
        super.onPause();
    }

    public void shareResult(int index) {
        startActivity(
                Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                .setType("audio/mp3")
                                .putExtra(Intent.EXTRA_STREAM, historyManager.getVisibleEntryAt(index).path)
                        , getString(R.string.choose_an_app)));
    }

    private void deleteResult(int index) {
        new MiniDialog(this, historyManager.getVisibleEntryAt(index).name, () -> {
            historyManager.removeVisibleEntryAt(index);
            adapter.notifyItemRemoved(index);
            showMessage(R.string.deleted);
            setNoItemsView();
        }, () -> adapter.notifyItemChanged(index)).show();
    }

    private void setNoItemsView() {
        boolean isEmpty = historyManager.getVisibleEntriesCount() == 0;
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        infoText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private String getMS(int millis) {
        int minutes = millis / 60000;
        int seconds = (millis % 60000) / 1000;
        return String.valueOf(minutes).concat(":").concat(seconds < 10 ? "0" : "").concat(String.valueOf(seconds));
    }

    private void showMessage(int resId) {
        showMessage(getString(resId));
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.history_list_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            LinearLayout layout = view.findViewById(R.id.layout);
            TextView title = view.findViewById(R.id.title);
            ImageView playPauseButton = view.findViewById(R.id.playPauseButton);
            ImageView deleteButton = view.findViewById(R.id.deleteButton);
            ImageView shareButton = view.findViewById(R.id.shareButton);
            LinearLayout mediaControls = view.findViewById(R.id.mediaControls);
            TextView current = view.findViewById(R.id.current);
            SeekBar seekBar = view.findViewById(R.id.seekBar);
            TextView duration = view.findViewById(R.id.duration);
            HistoryEntry entry = historyManager.getVisibleEntryAt(pos);

            layout.setBackgroundResource(pos == playingEntryIndex ? R.drawable.active_back : 0);
            title.setText(entry.name);
            playPauseButton.setImageResource((playingEntryIndex == pos && isPlaying) ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
            playPauseButton.setOnClickListener(v -> {
                if (pos != playingEntryIndex) {
                    if (mediaPlayer != null) mediaPlayer.release();
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(HistoryActivity.this, Uri.fromFile(new File(entry.path)));
                        mediaPlayer.prepare();
                        int playing0 = playingEntryIndex;
                        playingEntryIndex = pos;
                        if (playing0 != -1) notifyItemChanged(playing0);
                        isPlaying = false;
                        mediaPlayer.setOnCompletionListener(mp -> {
                            int playing1 = playingEntryIndex;
                            playingEntryIndex = -1;
                            isPlaying = false;
                            notifyItemChanged(playing1);
                        });
                    } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
                        showMessage(R.string.file_not_found);
                    }
                }
                if (isPlaying) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    timer.cancel();
                } else {
                    mediaPlayer.start();
                    isPlaying = true;
                }
                notifyItemChanged(playingEntryIndex);
            });
            mediaControls.setVisibility(pos == playingEntryIndex ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(pos == playingEntryIndex ? View.VISIBLE : View.GONE);
            shareButton.setVisibility(pos == playingEntryIndex ? View.VISIBLE : View.GONE);
            deleteButton.setOnClickListener(v -> deleteResult(pos));
            shareButton.setOnClickListener(v -> shareResult(pos));
            if (pos == playingEntryIndex) {
                pCurrent = current;
                pSeekBar = seekBar;
                current.setText(getMS(mediaPlayer.getCurrentPosition()));
                duration.setText(getMS(mediaPlayer.getDuration()));
                seekBar.setMax(mediaPlayer.getDuration() / 1000);
                seekBar.setProgress(mediaPlayer.getCurrentPosition() / 1000);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) mediaPlayer.seekTo(progress * 1000);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            pCurrent.setText(getMS(mediaPlayer.getCurrentPosition()));
                            pSeekBar.setProgress(mediaPlayer.getCurrentPosition() / 1000);
                        });
                    }
                }, 0, 400);
            }
        }

        @Override
        public int getItemCount() {
            return historyManager.getVisibleEntriesCount();
        }

        @Override
        public boolean isSwipeEnabled() {
            return true;
        }

        @Override
        public void onSwipe(RecyclerView.ViewHolder myViewHolder, int i) {
            int position = myViewHolder.getBindingAdapterPosition();
            if (i == ItemTouchHelper.START) {
                shareResult(position);
                notifyItemChanged(position);
            } else deleteResult(position);
        }

        @Override
        public Context getContext() {
            return HistoryActivity.this;
        }
    }
}