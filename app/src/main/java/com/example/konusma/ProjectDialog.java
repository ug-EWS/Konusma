package com.example.konusma;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.function.Consumer;

public class ProjectDialog extends BottomSheetDialog {
    private ImageView cancelButton;
    private LinearLayout addButton;
    private ImageView addIcon;
    private TextView addText;
    private TextView title;
    private EditText editText;

    ProjectDialog(Context _context, Consumer<String> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        initialize();
        title.setText("Yeni proje oluştur");
        addIcon.setImageResource(R.drawable.baseline_add_24);
        addText.setText("Oluştur");
        addButton.setOnClickListener(v -> {
            dismiss();
            String text = editText.getText().toString();
            if (text.isBlank()) text = editText.getHint().toString();
            _listener.accept(text);
        });
    }

    ProjectDialog(Context _context, String _name, Consumer<String> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        initialize();
        title.setText("Projeyi düzenle");
        addIcon.setImageResource(R.drawable.baseline_done_24);
        addText.setText("Uygula");
        editText.setText(_name);
        addButton.setOnClickListener(v -> {
            dismiss();
            String text = editText.getText().toString();
            if (text.isBlank()) text = editText.getHint().toString();
            _listener.accept(text);
        });
    }

    @Override
    public void show() {
        getContext().getSystemService(InputMethodManager.class).showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        super.show();
    }

    private void initialize() {
        setContentView(R.layout.add_project);
        cancelButton = findViewById(R.id.cancelButton);
        addButton = findViewById(R.id.addButton);
        addIcon = findViewById(R.id.addImage);
        addText = findViewById(R.id.addText);
        title = findViewById(R.id.title);
        editText = findViewById(R.id.editText);

        cancelButton.setOnClickListener(v -> cancel());

        setOnDismissListener(dialog ->
                getContext().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS));

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}