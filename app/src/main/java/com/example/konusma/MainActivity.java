package com.example.konusma;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.transition.Scene;
import android.transition.Slide;
import android.transition.Transition;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private LinearLayout main, mainWindow, localeWindow, historyWindow, languageLayout, rateLayout, pitchLayout, bottomBarLayout, voiceLayout;
    private TextView rateText, pitchText, languageText, countryText, pCurrent, pDuration;
    private EditText editText;
    private ImageView optionsButton, expandButton, saveButton, localeBack, historyBack, clearButton;
    private FloatingActionButton playPauseButton;
    private SeekBar rateSeekBar, pitchSeekBar, pSeekBar;
    private RecyclerView localeRecycler, voicesList, historyRecycler;

    private TextToSpeech textToSpeech;

    private boolean localeWindowOpen, historyWindowOpen, expanded, isPlaying;

    private int currentVoiceIndex, playingEntryIndex;

    private float rate, pitch;

    private ArrayList<Locale> locales;
    private ArrayList<Voice> voices;
    private ArrayList<String> files;

    private File folder;
    private AlertDialog progressDialog;

    private MediaPlayer mediaPlayer;
    private Timer timer;

    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        isPlaying = false;
        playingEntryIndex = -1;
        locales = new ArrayList<>();
        voices = new ArrayList<>();
        files = new ArrayList<>();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                locales = new ArrayList<>(textToSpeech.getAvailableLanguages());
                locales.sort(Comparator.comparing(Locale::getDisplayLanguage));
                localeRecycler.getAdapter().notifyDataSetChanged();
                setLocale(textToSpeech.getDefaultVoice().getLocale());
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
                        showMessage("Dosya kaydedildi.");
                        files = getFiles();
                        historyRecycler.getAdapter().notifyDataSetChanged();

                }
                });
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                runOnUiThread(() -> {
                    String message;
                    switch (errorCode) {
                        case TextToSpeech.ERROR_NETWORK:
                            message = "İnternet bağlantısı yok.";
                            break;
                        case TextToSpeech.ERROR_INVALID_REQUEST:
                            message = "Girilen yazı kabul edilmedi.";
                            break;
                        case TextToSpeech.ERROR_OUTPUT:
                            message = "Çıktı hatası.";
                            break;
                        case TextToSpeech.ERROR_NETWORK_TIMEOUT:
                            message = "İşlem zaman aşımına uğradı.";
                            break;
                        case TextToSpeech.ERROR_NOT_INSTALLED_YET:
                            message = "Ses verileri henüz indirilmedi.";
                            break;
                        case TextToSpeech.ERROR_SERVICE:
                            message = "Servis hatası.";
                            break;
                        case TextToSpeech.ERROR_SYNTHESIS:
                            message = "Konuşma sesi sentezlenemedi.";
                            break;
                        default:
                            message = "Bir sorun oluştu.";
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
                showMessage("Bir sorun oluştu");
            }
        });

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        folder = getFolder();
        files = getFiles();
        initializeUi();
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (historyWindowOpen) openHistoryWindow(false);
                else if (localeWindowOpen) openLocaleWindow(false);
                else if (expanded) setOptionsExpanded(false);
                else finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insets2 = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (imeVisible) setOptionsExpanded(false);
            v.setPadding(insets1.left, insets1.top, insets1.right, Math.max(insets1.bottom, insets2.bottom));
            return insets;
        });
    }

    private void initializeUi() {
        languageLayout = findViewById(R.id.languageLayout);
        languageLayout.setOnClickListener(v -> openLocaleWindow(true));
        rateLayout = findViewById(R.id.rateLayout);
        pitchLayout = findViewById(R.id.pitchLayout);
        bottomBarLayout = findViewById(R.id.bottomBarLayout);
        rateText = findViewById(R.id.rateText);
        pitchText = findViewById(R.id.pitchText);
        editText = findViewById(R.id.editText);
        optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(v -> openHistoryWindow(true));
        optionsButton.setTooltipText("Kaydedilenler");
        expandButton = findViewById(R.id.expandButton);
        expandButton.setOnClickListener(v -> setOptionsExpanded(!expanded));
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (editText.getText().toString().isBlank()) showMessage("Kaydedilecek yazı yok.");
            else getSaveDialog().show();
        });
        playPauseButton = findViewById(R.id.playPauseButton);
        playPauseButton.setOnClickListener(v -> {
            if (textToSpeech.isSpeaking()) textToSpeech.stop();
            else if (editText.getText().toString().isBlank()) showMessage("Seslendirilecek yazı yok.");
            else textToSpeech.speak(editText.getText().toString(), TextToSpeech.QUEUE_ADD, null, "speak");
        });
        rateSeekBar = findViewById(R.id.rateSeekBar);
        rateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rate = (float) progress / 10;
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
                pitch = (float) progress / 10;
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
        localeWindow = findViewById(R.id.localeWindow);
        localeBack = findViewById(R.id.localeBack);
        localeBack.setOnClickListener(v -> openLocaleWindow(false));
        localeRecycler = findViewById(R.id.localeRecycler);
        localeRecycler.setLayoutManager(new LinearLayoutManager(this));
        localeRecycler.setAdapter(new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.list_item, parent, false)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                View view = holder.itemView;
                int pos = holder.getAdapterPosition();
                TextView language = view.findViewById(R.id.language);
                TextView country = view.findViewById(R.id.country);
                Locale locale = locales.get(pos);
                language.setText(locale.getDisplayLanguage());
                country.setText(locale.getDisplayCountry());
                view.setOnClickListener(v -> {
                    setLocale(locale);
                    openLocaleWindow(false);
                });
            }

            @Override
            public int getItemCount() {
                return locales.size();
            }
        });
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
                int pos = holder.getAdapterPosition();
                view.setText(String.valueOf(pos + 1));
                view.setBackgroundColor(pos == currentVoiceIndex ? Color.parseColor("#9e9e9e") : Color.WHITE);
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
        historyWindow = findViewById(R.id.historyWindow);
        historyBack = findViewById(R.id.historyBack);
        historyBack.setOnClickListener(v -> openHistoryWindow(false));
        historyRecycler = findViewById(R.id.historyRecycler);
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        historyRecycler.setAdapter(new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.history_list_item, parent, false)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                View view = holder.itemView;
                int pos = holder.getAdapterPosition();
                TextView title = view.findViewById(R.id.title);
                ImageView deleteButton = view.findViewById(R.id.deleteButton);
                ImageView shareButton = view.findViewById(R.id.shareButton);
                ImageView playPauseButton = view.findViewById(R.id.playPauseButton);
                LinearLayout mediaControls = view.findViewById(R.id.mediaControls);
                TextView current = view.findViewById(R.id.current);
                SeekBar seekBar = view.findViewById(R.id.seekBar);
                TextView duration = view.findViewById(R.id.duration);
                String fileName = files.get(pos);

                title.setText(fileName);
                deleteButton.setOnClickListener(v -> getDeleteDialog(pos).show());
                shareButton.setOnClickListener(v -> startActivity(
                        Intent.createChooser(new Intent(Intent.ACTION_SEND)
                        .setType("audio/mp3")
                        .putExtra(Intent.EXTRA_STREAM, files.get(pos)), "Uygulama seçin")));
                playPauseButton.setImageResource((playingEntryIndex == pos && isPlaying) ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                playPauseButton.setOnClickListener(v -> {
                    if (pos != playingEntryIndex) {
                        if (mediaPlayer != null) mediaPlayer.release();
                        mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setDataSource(MainActivity.this, Uri.fromFile(new File(folder, fileName)));
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
                        } catch (IOException e) {
                            showMessage("Dosya bulunamadı.");
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
                return files.size();
            }
        });
        main = findViewById(R.id.main);
        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v -> editText.setText(""));
        saveButton.setTooltipText("Temizle");
        progressDialog = getProgressDialog();
        mainWindow.setVisibility(View.VISIBLE);
        localeWindow.setVisibility(View.GONE);
        historyWindow.setVisibility(View.GONE);
        setOptionsExpanded(false);
    }

    private void setOptionsExpanded(boolean _expanded) {
        expanded = _expanded;
        languageLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        rateLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        pitchLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        voiceLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        bottomBarLayout.setBackgroundColor(expanded ? Color.parseColor("#dddddd") : Color.WHITE);
        expandButton.animate().rotation(expanded ? 0 : 180);
        getWindow().setNavigationBarColor(expanded ? Color.parseColor("#dddddd") : Color.WHITE);
        if (expanded) imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void openLocaleWindow(boolean open) {
        localeWindowOpen = open;
        imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        TransitionManager.beginDelayedTransition(main, new MaterialSharedAxis(MaterialSharedAxis.X, open));
        mainWindow.setVisibility(open ? View.GONE : View.VISIBLE);
        localeWindow.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    private void openHistoryWindow(boolean open) {
        historyWindowOpen = open;
        imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        TransitionManager.beginDelayedTransition(main, new MaterialSharedAxis(MaterialSharedAxis.X, open));
        mainWindow.setVisibility(open ? View.GONE : View.VISIBLE);
        historyWindow.setVisibility(open ? View.VISIBLE : View.GONE);
    }

    private AlertDialog getDeleteDialog(int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(files.get(index));
        builder.setMessage("Bu dosya silinsin mi?");
        builder.setPositiveButton("Silinsin", (dialog, which) -> {
            boolean success = new File(folder, files.get(index)).delete();
            files = getFiles();
            historyRecycler.getAdapter().notifyDataSetChanged();
            showMessage(success ? "Silindi." : "Silinemedi.");
        });
        builder.setNegativeButton("Hayır", null);
        return builder.create();
    }

    private AlertDialog getSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.save, null);
        EditText filename = view.findViewById(R.id.editTextText);
        filename.setHint(new SimpleDateFormat("ddMMyyyyHHmmss").format(Calendar.getInstance().getTime()));
        builder.setTitle("Dosya adı girin.");
        builder.setView(view);
        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            String text = editText.getText().toString();
            String name = filename.getText().toString();
            if (name.isBlank()) name = filename.getHint().toString();
            int i = 0;
            String newName = name;
            while (files.contains(newName.concat(".mp3"))) {
                i++;
                newName = String.format("%s (%d)", name, i);
            }
            name = newName;
            name = name.concat(".mp3");
            textToSpeech.synthesizeToFile(text, null, new File(folder, name), "save");
        });
        builder.setNegativeButton("İptal", null);
        return builder.create();
    }

    private AlertDialog getProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        builder.setTitle("Kaydediliyor...");
        builder.setView(progressBar);
        return builder.create();
    }

    private void showMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
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

    private File getFolder() {
        File mainFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File subfolder = new File(mainFolder, "Konuşma");
        if (!subfolder.exists()) subfolder.mkdirs();
        return subfolder;
    }

    private ArrayList<String> getFiles() {
       return new ArrayList<>(Arrays.asList(folder.list()));
    }

    private String getMS(int millis) {
        int minutes = millis / 60000;
        int seconds = (millis % 60000) / 1000;
        return String.valueOf(minutes).concat(":").concat(seconds < 10 ? "0" : "").concat(String.valueOf(seconds));
    }
}