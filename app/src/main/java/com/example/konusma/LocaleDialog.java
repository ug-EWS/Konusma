package com.example.konusma;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

public class LocaleDialog extends BottomSheetDialog {
    LocaleDialog(Context _context, ArrayList<Locale> _locales, Consumer<Locale> _listener) {
        super(_context, R.style.BottomSheetDialogTheme);
        setContentView(R.layout.locale_dialog);

        ImageView localeBack = findViewById(R.id.localeBack);
        RecyclerView localeRecycler = findViewById(R.id.localeRecycler);

        localeBack.setOnClickListener(v -> cancel());

        localeRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        localeRecycler.setAdapter(new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.list_item, parent, false)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                View view = holder.itemView;
                int pos = holder.getBindingAdapterPosition();
                TextView language = view.findViewById(R.id.language);
                TextView country = view.findViewById(R.id.country);
                Locale locale = _locales.get(pos);
                language.setText(locale.getDisplayLanguage());
                country.setText(locale.getDisplayCountry());
                view.setOnClickListener(v -> {
                    dismiss();
                    _listener.accept(locale);
                });
            }

            @Override
            public int getItemCount() {
                return _locales.size();
            }
        });

        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
