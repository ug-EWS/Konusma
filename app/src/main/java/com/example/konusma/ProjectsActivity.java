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
import androidx.activity.OnBackPressedCallback;
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ProjectsActivity extends AppCompatActivity {
    private ImageView backButton, optionsButton;
    private TextView title, infoText;
    private RecyclerView projectManagerRecycler, projectRecycler;

    private ProjectManagerAdapter projectManagerAdapter;
    private ProjectAdapter projectAdapter;
    private ItemTouchHelper projectAdapterItemTouchHelper;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private HistoryManager historyManager;
    private ProjectManager projectManager;

    private int currentProjectIndex;
    private ProjectManager.Project currentProject;

    private boolean projectOpen = false;

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
        setContentView(R.layout.activity_projects);

        sp = getSharedPreferences("Konusma", MODE_PRIVATE);
        spe = sp.edit();

        playingEntryIndex = -1;
        isPlaying = false;

        historyManager = new HistoryManager(sp.getString("history", ""));
        projectManager = new ProjectManager(sp.getString("projects", ""));

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> back());

        title = findViewById(R.id.title);
        infoText = findViewById(R.id.infoText);

        optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(v -> getProjectMenu(currentProjectIndex).showMenu());

        projectManagerRecycler = findViewById(R.id.projectManagerRecycler);
        projectManagerRecycler.setLayoutManager(new LinearLayoutManager(this));
        projectManagerAdapter = new ProjectManagerAdapter();
        projectManagerRecycler.setAdapter(projectManagerAdapter);

        projectRecycler = findViewById(R.id.projectRecycler);
        projectRecycler.setLayoutManager(new LinearLayoutManager(this));

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                back();
            }
        };

        getOnBackPressedDispatcher().addCallback(callback);

        setViewMode(false);

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
        spe.putString("projects", projectManager.toJsonString()).commit();
        super.onPause();
    }

    public void shareResult(int index) {
        startActivity(
                Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                .setType("audio/mp3")
                                .putExtra(Intent.EXTRA_STREAM, currentProject.getFileAt(index).path)
                        , getString(R.string.choose_an_app)));
    }

    public void deleteResult(int index) {
        new MiniDialog(this, currentProject.getFileAt(index).name, () -> {
            currentProject.removeFileAt(index);
            projectAdapter.notifyItemRemoved(index);
            projectManagerAdapter.notifyItemChanged(currentProjectIndex);
            setNoItemsView();
        }, () -> projectAdapter.notifyItemChanged(index)).show();
    }

    private BottomSheetMenu getProjectMenu(int index) {
        ProjectManager.Project forProject = projectManager.getProjectAt(index);
        BottomSheetMenu menu = new BottomSheetMenu(this, forProject);
        if (forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_done_24, R.string.complete_project, () ->
                    new MiniDialog(this,
                    forProject,
                    getString(R.string.complete_project),
                    "Proje tamamlansın mı?",
                    "Tamamla",
                    R.drawable.baseline_done_24,
                    -1,
                    () -> {
                        forProject.completeProject();
                        projectManager.sort();
                        if (projectOpen) currentProjectIndex = projectManager.getIndexOf(currentProject);
                        projectManagerAdapter.notifyDataSetChanged();
                    }).show(), -1);
        if (forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_edit_24, R.string.edit_project, () ->
                new ProjectDialog(this, forProject.name, newName -> {
                    forProject.name = newName;
                    if (projectOpen) title.setText(newName);
                    projectManagerAdapter.notifyItemChanged(index);
                }).show());
        if (forProject.isComplete())
            menu.addMenuItem(R.drawable.baseline_swap_horiz_24, R.string.continue_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.continue_project),
                        "Projeye devam edilsin mi?",
                        "Devam et",
                            R.drawable.baseline_swap_horiz_24,
                            0,
                        () -> {
                            forProject.continueProject();
                            projectManager.sort();
                            if (projectOpen) currentProjectIndex = projectManager.getIndexOf(currentProject);
                            projectManagerAdapter.notifyDataSetChanged();
                        }).show());
        if (!forProject.isComplete())
            menu.addMenuItem(R.drawable.baseline_restart_alt_24, R.string.restart_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.restart_project),
                        "Proje yeniden başlatılsın mı?",
                        "Yeniden başlat",
                        R.drawable.baseline_restart_alt_24,
                        0,
                        () -> {
                            forProject.restartProject();
                            projectManager.sort();
                            if (projectOpen) currentProjectIndex = projectManager.getIndexOf(currentProject);
                            projectManagerAdapter.notifyDataSetChanged();
                        }).show());
        if (forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_close_24, R.string.cancel_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.cancel_project),
                        "Proje iptal edilsin mi? Projedeki dosyalar Oluşturulanlar bölümüne taşınacak.",
                        "Projeyi iptal et",
                        R.drawable.baseline_close_24,
                        1,
                        () -> {
                            ArrayList<HistoryEntry> entries = forProject.cancelProject();
                            for (HistoryEntry i : entries) historyManager.addEntry(i);
                            forProject.removeAllFiles();
                            spe.putString("history", historyManager.toJsonString()).apply();
                            projectManager.sort();
                            if (projectOpen) {
                                setNoItemsView();
                                currentProjectIndex = projectManager.getIndexOf(currentProject);
                            }
                            projectManagerAdapter.notifyDataSetChanged();
                        }).show(), 1);
        if (!forProject.isProgress())
            menu.addMenuItem(R.drawable.baseline_delete_forever_red_24, R.string.delete_project, () ->
                new MiniDialog(this,
                        forProject,
                        getString(R.string.delete_project),
                        "Proje silinsin mi?",
                        "Sil",
                        R.drawable.baseline_delete_forever_red_24,
                        1,
                        () -> {
                            projectManager.removeProject(index);
                            projectManagerAdapter.notifyItemRemoved(index);
                            if (projectOpen) setViewMode(false);
                        }).show(),1);
        return menu;
    }

    private void back() {
        if (projectOpen) {
            if (mediaPlayer != null) mediaPlayer.pause();
            playingEntryIndex = -1;
            isPlaying = false;
            currentProjectIndex = -1;
            setViewMode(false);
        }
        else finish();
    }

    private void openProject(int index) {
        currentProjectIndex = index;
        currentProject = projectManager.getProjectAt(index);
        setViewMode(true);
    }

    private void setViewMode(boolean _projectOpen) {
        projectOpen = _projectOpen;
        title.setText(projectOpen ? currentProject.name : getString(R.string.projects_center));
        optionsButton.setVisibility(projectOpen ? View.VISIBLE : View.GONE);
        setNoItemsView();
        if (projectOpen) {
            projectAdapter = new ProjectAdapter();
            projectRecycler.setAdapter(projectAdapter);
            projectAdapterItemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(projectAdapter));
            projectAdapterItemTouchHelper.attachToRecyclerView(projectRecycler);
        }
    }

    private void setNoItemsView() {
        boolean noItems = projectOpen ? currentProject.getFileCount() == 0 : projectManager.getProjectCount() == 0;
        infoText.setText(projectOpen ? currentProject.isCanceled() ? "Proje iptal edilmiş." : "Proje boş." : "Proje oluşturulmamış.");
        View view = noItems ? infoText : projectOpen ? projectRecycler : projectManagerRecycler;
        infoText.setVisibility(View.GONE);
        projectManagerRecycler.setVisibility(View.GONE);
        projectRecycler.setVisibility(View.GONE);
        view.setVisibility(View.VISIBLE);
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

    private class ProjectManagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.project_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            ProjectManager.Project project = projectManager.getProjectAt(pos);
            ImageView icon = view.findViewById(R.id.playlistIcon);
            TextView name = view.findViewById(R.id.playlistTitle);
            TextView size = view.findViewById(R.id.playlistSize);
            ImageView options = view.findViewById(R.id.playlistOptions);

            icon.setBackgroundResource(project.isComplete() ? R.drawable.project_icon_complete
                    : project.isProgress() ? R.drawable.project_icon_progress
                    : R.drawable.project_icon_canceled);

            name.setText(project.name);
            size.setText(project.isCanceled() ? "İptal edildi" : String.format("%d dosya", project.getFileCount()));

            view.setOnClickListener(v -> openProject(pos));
            options.setOnClickListener(v -> getProjectMenu(pos).showMenu());
        }

        @Override
        public int getItemCount() {
            return projectManager.getProjectCount();
        }
    }

    private class ProjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
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
            HistoryEntry entry = currentProject.getFileAt(pos);

            layout.setBackgroundResource(pos == playingEntryIndex ? R.drawable.active_back : 0);
            title.setText(entry.name);
            playPauseButton.setImageResource((playingEntryIndex == pos && isPlaying) ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
            playPauseButton.setOnClickListener(v -> {
                if (pos != playingEntryIndex) {
                    if (mediaPlayer != null) mediaPlayer.release();
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(ProjectsActivity.this, Uri.fromFile(new File(entry.path)));
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
            return currentProject.getFileCount();
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
            return ProjectsActivity.this;
        }
    }
}