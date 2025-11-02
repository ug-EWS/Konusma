package com.example.konusma;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private LinearLayout main, mainWindow, languageLayout, rateLayout, pitchLayout, voiceLayout, optionsLayout, mainScreen, bottomNavigation;
    private TextView rateText, pitchText, languageText, countryText;
    private EditText editText;
    private ImageView optionsButton, expandButton, saveButton;
    private FloatingActionButton playPauseButton;
    private SeekBar rateSeekBar, pitchSeekBar;
    private RecyclerView voicesList;

    private TextToSpeech textToSpeech;

    private boolean expanded;

    private int currentVoiceIndex, theme;

    private float rate, pitch;

    private ArrayList<Locale> locales;
    private ArrayList<Voice> voices;

    private SaveDialog saveDialog;
    private AlertDialog progressDialog;
    private LocaleDialog localeDialog;

    private HistoryEntry generatedEntry;

    private InputMethodManager imm;

    private SharedPreferences sp;
    private SharedPreferences.Editor spe;

    private float[] rates = {0.33F, 0.4F, 0.5F, 0.57F, 0.66F, 0.8F, 1, 1.25F, 1.5F, 1.75F, 2, 2.5F, 3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        locales = new ArrayList<>();
        voices = new ArrayList<>();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Collator collator = Collator.getInstance();
                collator.setStrength(Collator.SECONDARY);
                locales = new ArrayList<>(textToSpeech.getAvailableLanguages());
                locales.sort(Comparator.comparing(Locale::getDisplayLanguage, collator::compare));
                setLocale(textToSpeech.getDefaultVoice().getLocale());
                localeDialog = new LocaleDialog(this, locales, this::setLocale);
            }
        });
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (utteranceId.equals("speak")) playPauseButton.setImageResource(R.drawable.baseline_stop_24);
                else if (utteranceId.equals("save")) progressDialog.show();
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                if (utteranceId.equals("speak")) playPauseButton.setImageResource(R.drawable.baseline_spatial_audio_off_24);
                else if (utteranceId.equals("save")) {
                        progressDialog.dismiss();
                        saveDialog.save(generatedEntry);
                        showMessage(R.string.file_saved);
                }
                });
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                runOnUiThread(() -> {
                    int message;
                    switch (errorCode) {
                        case TextToSpeech.ERROR_NETWORK:
                            message = R.string.no_internet_conn;
                            break;
                        case TextToSpeech.ERROR_INVALID_REQUEST:
                            message = R.string.inv_request;
                            break;
                        case TextToSpeech.ERROR_OUTPUT:
                            message = R.string.err_output;
                            break;
                        case TextToSpeech.ERROR_NETWORK_TIMEOUT:
                            message = R.string.err_timeout;
                            break;
                        case TextToSpeech.ERROR_NOT_INSTALLED_YET:
                            message = R.string.err_not_installed_yet;
                            break;
                        case TextToSpeech.ERROR_SERVICE:
                            message = R.string.err_service;
                            break;
                        case TextToSpeech.ERROR_SYNTHESIS:
                            message = R.string.err_synthesis;
                            break;
                        default:
                            message = R.string.err_generic;
                            break;
                }
                showMessage(message);
                if (utteranceId.equals("speak")) playPauseButton.setImageResource(R.drawable.baseline_spatial_audio_off_24);
                else progressDialog.dismiss();
                super.onError(utteranceId, errorCode);
                });
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                if (utteranceId.equals("speak")) playPauseButton.setImageResource(R.drawable.baseline_spatial_audio_off_24);
                super.onStop(utteranceId, interrupted);
            }

            @Override
            public void onError(String utteranceId) {
                showMessage(R.string.err_generic);
            }
        });

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        sp = getSharedPreferences("Konusma", MODE_PRIVATE);
        spe = sp.edit();

        theme = sp.getInt("theme", 0);

        initializeUi();

        saveDialog = new SaveDialog(this, fileName -> {
            String text = editText.getText().toString();

            String mainFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            mainFolder = mainFolder.concat("/Konuşma");
            File subfolder = new File(mainFolder);
            if (!subfolder.exists()) subfolder.mkdirs();

            String dateTime = new SimpleDateFormat("ddMMyyyyHHmmss").format(Calendar.getInstance().getTime());
            String filePath = fileName.isEmpty()
                    ? String.format("%s/%s.mp3", mainFolder, dateTime)
                    : String.format("%s/%s_%s.mp3", mainFolder, fileName, dateTime);

            if (fileName.isEmpty()) fileName = dateTime;
            fileName = fileName.concat(".mp3");
            generatedEntry = new HistoryEntry(fileName, filePath);

            textToSpeech.synthesizeToFile(text, null, new File(filePath), "save");
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (expanded) setOptionsExpanded(false);
                else finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        saveDialog.refreshData();
    }

    private void initializeUi() {
        optionsLayout = findViewById(R.id.optionsLayout);
        languageLayout = findViewById(R.id.languageLayout);
        languageLayout.setOnClickListener(v -> {
            if (localeDialog != null) localeDialog.show();
        });
        rateLayout = findViewById(R.id.rateLayout);
        pitchLayout = findViewById(R.id.pitchLayout);
        rateText = findViewById(R.id.rateText);
        pitchText = findViewById(R.id.pitchText);
        editText = findViewById(R.id.editText);
        optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(v -> getOptionsMenu().showMenu());
        expandButton = findViewById(R.id.expandButton);
        expandButton.setOnClickListener(v -> setOptionsExpanded(!expanded));
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (editText.getText().toString().isBlank()) showMessage(R.string.no_text_to_save);
            else saveDialog.show();
        });
        playPauseButton = findViewById(R.id.playPauseButton);
        playPauseButton.setOnClickListener(v -> {
            if (textToSpeech.isSpeaking()) textToSpeech.stop();
            else if (editText.getText().toString().isBlank()) showMessage(R.string.no_text_to_speak);
            else textToSpeech.speak(editText.getText().toString(), TextToSpeech.QUEUE_ADD, null, "speak");
        });
        rateSeekBar = findViewById(R.id.rateSeekBar);
        rateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rate = rates[progress];
                rateText.setText(String.valueOf(rate));
                textToSpeech.setSpeechRate(rate);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pitch = rates[progress];
                pitchText.setText(String.valueOf(pitch));
                textToSpeech.setPitch(pitch);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mainWindow = findViewById(R.id.mainWindow);
        languageText = findViewById(R.id.languageText);
        countryText = findViewById(R.id.countryText);
        voiceLayout = findViewById(R.id.voiceLayout);
        voicesList = findViewById(R.id.voicesList);
        voicesList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        voicesList.setAdapter(new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.voice_item, parent, false)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView view = (TextView) holder.itemView;
                int pos = holder.getBindingAdapterPosition();
                view.setText(String.valueOf(pos + 1));
                view.setBackgroundResource(pos == currentVoiceIndex ? R.drawable.voice_selected_bg : R.drawable.voice_unselected_bg);
                view.setTextColor(getColor(pos == currentVoiceIndex ? R.color.teal_200 : R.color.grey6));
                view.setOnClickListener(v -> {
                    currentVoiceIndex = pos;
                    textToSpeech.setVoice(voices.get(pos));
                    notifyDataSetChanged();
                });
            }

            @Override
            public int getItemCount() {
                return voices.size();
            }
        });
        main = findViewById(R.id.main);
        mainScreen = findViewById(R.id.mainScreen);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets insets3 = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(Math.max(insets1.left, insets3.left), insets1.top, Math.max(insets1.right, insets3.right), Math.max(insets1.bottom, insets2.bottom));
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (imeVisible) setOptionsExpanded(false);
            return insets;
        });

        progressDialog = getProgressDialog();
        mainWindow.setVisibility(View.VISIBLE);
        setOptionsExpanded(false);
    }

    private BottomSheetMenu getOptionsMenu() {
        BottomSheetMenu menu = new BottomSheetMenu(this);
        menu.addMenuItem(R.drawable.baseline_clear_24, R.string.clear_text, () ->
                editText.setText(""));
        menu.addMenuItem(R.drawable.baseline_folder_24, R.string.saved_files, () ->
            startActivity(new Intent(getApplicationContext(), HistoryActivity.class)));
        menu.addMenuItem(R.drawable.baseline_build_24, R.string.projects_center, () ->
            startActivity(new Intent(getApplicationContext(), ProjectsActivity.class)));
        menu.addMenuItem(R.drawable.baseline_color_lens_24, R.string.theme, () -> {
            BottomSheetMenu themeMenu = new BottomSheetMenu(this, theme == 0 ? 2 : theme - 1);
            themeMenu.addMenuItem(R.drawable.baseline_folder_24, R.string.light, () ->
                    setAppTheme(1));
            themeMenu.addMenuItem(R.drawable.baseline_folder_24, R.string.dark, () ->
                    setAppTheme(2));
            themeMenu.addMenuItem(R.drawable.baseline_folder_24, R.string.auto, () ->
                    setAppTheme(0));
            themeMenu.showMenu();
        }, getString(List.of(R.string.auto, R.string.light, R.string.dark).get(theme)));
        return menu;
    }

    private void setOptionsExpanded(boolean _expanded) {
        if (expanded != _expanded) {
            expanded = _expanded;
            expandButton.animate().rotation(expanded ? 0 : 180);
            TransitionManager.beginDelayedTransition(mainWindow);
            languageLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
            rateLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
            pitchLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
            voiceLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private AlertDialog getProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        builder.setTitle(R.string.saving);
        builder.setView(progressBar);
        return builder.create();
    }

    private void showMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void showMessage(int resId) {
        showMessage(getString(resId));
    }

    private void setLocale(Locale locale) {
        textToSpeech.setLanguage(locale);
        languageText.setText(locale.getDisplayLanguage());
        countryText.setText(locale.getDisplayCountry());
        Locale locale1 = textToSpeech.getVoice().getLocale();
        voices = new ArrayList<>(textToSpeech.getVoices());
        voices.removeIf(voice -> !voice.getLocale().equals(locale1));
        currentVoiceIndex = 0;
        textToSpeech.setVoice(voices.get(0));
        voicesList.getAdapter().notifyDataSetChanged();
    }

    private void setAppTheme(int _theme) {
        theme = _theme;
        spe.putInt("theme", theme).commit();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ((UiModeManager) getSystemService(UI_MODE_SERVICE)).setApplicationNightMode(theme);
        else AppCompatDelegate.setDefaultNightMode(theme == 0 ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : theme);
    }
}