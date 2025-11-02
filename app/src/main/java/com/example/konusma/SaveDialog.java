package com.example.konusma;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.function.Consumer;

public class SaveDialog extends BottomSheetDialog {
    private ProjectManager projectManager;
    private HistoryManager historyManager;
    private ImageView cancelButton;
    private EditText fileName;
    private LinearLayout noProjectButton;
    private ImageView noProjectIcon;
    private LinearLayout newProjectButton;
    private LinearLayout saveButton;
    private RecyclerView recycler;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private int selection = -1;

    SaveDialog(Context _context, Consumer<String> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);

        sp = getContext().getSharedPreferences("Konusma", Context.MODE_PRIVATE);
        spe = sp.edit();

        setContentView(R.layout.save_dialog);

        cancelButton = findViewById(R.id.cancelButton);
        fileName = findViewById(R.id.editTextText);
        noProjectButton = findViewById(R.id.noProjectButton);
        noProjectIcon = findViewById(R.id.noProjectIcon);
        newProjectButton = findViewById(R.id.newPlaylistButton);
        saveButton = findViewById(R.id.saveButton);
        recycler = findViewById(R.id.recycler);

        cancelButton.setOnClickListener(v -> cancel());

        fileName.setHint(new SimpleDateFormat("ddMMyyyyHHmmss").format(Calendar.getInstance().getTime()));

        noProjectButton.setOnClickListener(v -> setSelection(-1));
        newProjectButton.setOnClickListener(v -> new ProjectDialog(getContext(), name -> {
            projectManager.createNewProject(name);
            adapter.notifyItemInserted(0);
            adapter.notifyItemRangeChanged(1, projectManager.getProjectCount() - 1);
            setSelection(0);
        }).show());
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ManagePlaylistsAdapter();
        saveButton.setOnClickListener(v -> {
            dismiss();
            String text = fileName.getText().toString();
            if (text.isBlank()) text = "";
            _listener.accept(text);
        });

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        setSelection(-1);
    }

    public void refreshData() {
        projectManager = new ProjectManager(sp.getString("projects", ""));
        historyManager = new HistoryManager(sp.getString("history", ""));
    }

    @Override
    public void show() {
        recycler.setAdapter(adapter);
        super.show();
    }

    public void save(HistoryEntry _entry) {
        if (selection == -1) {
            historyManager.addEntry(_entry);
            spe.putString("history", historyManager.toJsonString()).apply();
        } else {
            projectManager.getProjectAt(selection).addFile(_entry);
            spe.putString("projects", projectManager.toJsonString()).apply();
        }
    }

    private void setSelection(int _selection) {
        int oldSelection = selection;
        selection = _selection;
        if (oldSelection == -1) {
            noProjectIcon.setBackgroundResource(R.drawable.project_icon_unselected);
            noProjectIcon.setImageResource(R.drawable.baseline_done_24_invisible);
        } else adapter.notifyItemChanged(oldSelection);
        if (selection == -1) {
            noProjectIcon.setBackgroundResource(R.drawable.project_icon_selected);
            noProjectIcon.setImageResource(R.drawable.baseline_done_24_colored);
        } else adapter.notifyItemChanged(selection);
    }

    private class ManagePlaylistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.project_item, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            ProjectManager.Project project = projectManager.getProjectAt(pos);
            LinearLayout layout = view.findViewById(R.id.layout);
            ImageView icon = view.findViewById(R.id.playlistIcon);
            TextView title = view.findViewById(R.id.playlistTitle);
            TextView size = view.findViewById(R.id.playlistSize);
            ImageView options = view.findViewById(R.id.playlistOptions);

            icon.setBackgroundResource(selection == pos ?
                    R.drawable.project_icon_selected :
                    project.isProgress() ? R.drawable.project_icon_progress :
                            project.isComplete() ? R.drawable.project_icon_complete :
                                    R.drawable.project_icon_canceled);
            icon.setImageResource(selection == pos ? R.drawable.baseline_done_24_colored : R.drawable.baseline_build_24);
            title.setText(project.name);
            size.setText(project.isCanceled() ? "İptal edildi" : String.format("%d dosya", project.getFileCount()));

            layout.setAlpha(project.isProgress() ? 1 : 0.3F);

            layout.setOnClickListener(v -> {
                if (project.isProgress()) setSelection(pos);
                else Toast.makeText(getContext(),
                        project.isComplete() ?
                                "Tamamlanan projeye dosya eklenemez." :
                                "İptal edilen projeye dosya eklenemez", Toast.LENGTH_SHORT).show();
            });
            options.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return projectManager.getProjectCount();
        }
    }
}
