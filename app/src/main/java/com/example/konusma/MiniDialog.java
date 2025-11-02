package com.example.konusma;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MiniDialog extends BottomSheetDialog {
    private ImageView cancelButton;
    private TextView title;
    private TextView fileName;
    private LinearLayout projectLayout;
    private TextView promptText;
    private LinearLayout button;
    private TextView buttonText;

    MiniDialog(Context _context, String _fileName, Runnable _listener, Runnable _onCancel) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();

        fileName.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);

        fileName.setText(_fileName);
        buttonText.setTextColor(getContext().getColor(R.color.red));
        button.setOnClickListener(v -> {
            dismiss();
            _listener.run();
        });

        setOnCancelListener(dialog -> _onCancel.run());
    }

    MiniDialog(Context _context, ProjectManager.Project _project, String _title, String _message, String _buttonText, int _buttonIcon, int _dangerLevel, Runnable _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        prepare();
        ImageView buttonImage = findViewById(R.id.buttonIcon);

        title.setText(_title);
        promptText.setText(_message);

        projectLayout.setVisibility(View.VISIBLE);
        ImageView icon = findViewById(R.id.playlistIcon);
        TextView name = findViewById(R.id.playlistTitle);
        TextView size = findViewById(R.id.playlistSize);

        icon.setBackgroundResource(_project.isComplete() ? R.drawable.project_icon_complete
                : _project.isProgress() ? R.drawable.project_icon_progress
                : R.drawable.project_icon_canceled);
        name.setText(_project.name);
        size.setText(_project.isCanceled() ? "İptal edildi" : String.format("%d dosya", _project.getFileCount()));

        buttonImage.setImageResource(_buttonIcon);
        buttonText.setText(_buttonText);

        if (_dangerLevel != 0) {
            int color = getContext().getColor(_dangerLevel > 0 ? R.color.red : R.color.green);
            buttonImage.setColorFilter(color);
            buttonText.setTextColor(color);
        }

        button.setOnClickListener(v -> {
            dismiss();
            _listener.run();
        });
    }

    private void prepare() {
        setContentView(R.layout.mini_dialog);

        cancelButton = findViewById(R.id.cancelButton);
        title = findViewById(R.id.title);
        fileName = findViewById(R.id.fileName);
        projectLayout = findViewById(R.id.projectLayout);
        promptText = findViewById(R.id.promptText);
        button = findViewById(R.id.deleteButton);
        buttonText = findViewById(R.id.buttonText);

        fileName.setVisibility(View.GONE);
        projectLayout.setVisibility(View.GONE);

        cancelButton.setOnClickListener(v -> cancel());

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

}
