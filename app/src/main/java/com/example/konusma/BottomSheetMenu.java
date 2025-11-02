package com.example.konusma;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

public class BottomSheetMenu extends BottomSheetDialog {
    private ArrayList<MenuItem> items;
    private RecyclerView recyclerView;
    private LinearLayout sortLayout;
    private LinearLayout projectLayout;
    private LinearLayout space;
    private final int selection;

    BottomSheetMenu(Context _context) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        space.setVisibility(View.VISIBLE);
        selection = -1;
    }

    BottomSheetMenu(Context _context, int _selection) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        sortLayout.setVisibility(View.VISIBLE);
        selection = _selection;
        ImageView cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> cancel());
    }

    BottomSheetMenu(Context _context, ProjectManager.Project _project) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        projectLayout.setVisibility(View.VISIBLE);
        selection = -1;
        ImageView icon = findViewById(R.id.playlistIcon);
        TextView name = findViewById(R.id.playlistTitle);
        TextView size = findViewById(R.id.playlistSize);

        icon.setBackgroundResource(_project.isComplete() ? R.drawable.project_icon_complete
                : _project.isProgress() ? R.drawable.project_icon_progress
                : R.drawable.project_icon_canceled);
        name.setText(_project.name);
        size.setText(_project.isCanceled() ? "İptal edildi" : String.format("%d dosya", _project.getFileCount()));
    }


    private void prepare() {
        setContentView(R.layout.menu_dialog);
        recyclerView = findViewById(R.id.recycler);
        sortLayout = findViewById(R.id.sortLayout);
        projectLayout = findViewById(R.id.projectLayout);
        space = findViewById(R.id.space);
        sortLayout.setVisibility(View.GONE);
        projectLayout.setVisibility(View.GONE);
        space.setVisibility(View.GONE);

        items = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void addMenuItem(int icon, int title, Runnable listener) {
        items.add(new MenuItem(icon, title, "", listener, 0));
    }

    public void addMenuItem(int icon, int title, Runnable listener, int dangerLevel) {
        items.add(new MenuItem(icon, title, "", listener, dangerLevel));
    }

    public void addMenuItem(int icon, int title, Runnable listener, String description) {
        items.add(new MenuItem(icon, title, description, listener, 0));
    }

    public void showMenu() {
        recyclerView.setAdapter(new MenuAdapter());
        show();
    }

    private static class MenuItem {
        public int icon;
        public int title;
        public String description;
        public Runnable listener;
        public int dangerLevel;

        MenuItem(int _icon, int _title, String _description, Runnable _listener, int _dangerLevel) {
            icon = _icon;
            title = _title;
            description = _description;
            listener = _listener;
            dangerLevel = _dangerLevel;
        }
    }

    private class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(
                    getLayoutInflater().inflate(R.layout.menu_item, parent, false)
            ) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getBindingAdapterPosition();
            MenuItem item = items.get(pos);

            ImageView icon = view.findViewById(R.id.icon);
            TextView title = view.findViewById(R.id.title);
            TextView description = view.findViewById(R.id.description);

            icon.setImageResource(selection == -1 ? item.icon
                    : selection == pos ? R.drawable.baseline_radio_button_checked_24
                    : R.drawable.baseline_radio_button_unchecked_24);
            title.setText(item.title);
            description.setVisibility(item.description.isEmpty() ? View.GONE : View.VISIBLE);
            description.setText(item.description);

            if (item.dangerLevel != 0) {
                int color = getContext().getColor(item.dangerLevel > 0 ? R.color.red : R.color.green);
                icon.setColorFilter(color);
                title.setTextColor(color);
            }

            view.setOnClickListener(v -> {
                dismiss();
                item.listener.run();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
