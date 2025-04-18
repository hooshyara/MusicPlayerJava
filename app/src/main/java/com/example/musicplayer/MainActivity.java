package com.example.musicplayer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.databinding.ActivityMainBinding;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MusicAdapter.onMusicClickListener {
    private RecyclerView recyclerView;
    private ActivityMainBinding binding;
    private List<Music> musicList = Music.getList();
    private MediaPlayer mediaPlayer;
    private MusicState musicState = MusicState.STOPPED;
    private Timer timer;
    private boolean isDragging;
    private int cursor;
    private MusicAdapter musicAdapter;

    @Override
    public void onClick(Music music, int position) {
        timer.cancel();
        timer.purge();
        mediaPlayer.release();
        cursor = position;
        onMusicChange(musicList.get(cursor));
    }

    enum MusicState {
        PLAYING, PAUSE, STOPPED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Fresco.initialize(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        recyclerView = findViewById(R.id.rv_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        musicAdapter = new MusicAdapter(musicList, this);
        recyclerView.setAdapter(musicAdapter);
        onMusicChange(musicList.get(cursor));
        binding.playBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (musicState) {
                    case PLAYING:
                        mediaPlayer.pause();
                        musicState = MusicState.PAUSE;
                        binding.playBTN.setImageResource(R.drawable.ic_play_32dp);
                        break;
                    case PAUSE:
                    case STOPPED:
                        mediaPlayer.start();
                        musicState = MusicState.PLAYING;
                        binding.playBTN.setImageResource(R.drawable.ic_pause_24dp);

                        break;
                }
            }
        });
        binding.musicSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                binding.positionTv.setText(Music.convertMillisToString((long) value));
            }
        });

        binding.musicSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                isDragging = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                isDragging = true;
                mediaPlayer.seekTo((int) slider.getValue());

            }
        });

        binding.nextBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goNext();
            }
        });

        binding.prevBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPrev();
            }
        });
    }

    private void goPrev() {
        timer.cancel();
        timer.purge();
        mediaPlayer.release();
        if (cursor == 0) {
            cursor = musicList.size() - 1;
        } else {
            cursor--;
        }
        onMusicChange(musicList.get(cursor));
    }


    public void onMusicChange(Music music) {
        musicAdapter.notifyMusicChange(music);
        binding.musicSlider.setValue(0);
        mediaPlayer = MediaPlayer.create(this, music.getMusicFileResId());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isDragging) {
                                    binding.positionTv.setText(Music.convertMillisToString(mediaPlayer.getCurrentPosition()));
                                    binding.musicSlider.setValue(mediaPlayer.getCurrentPosition());
                                }

                            }
                        });
                    }
                }, 1000, 1000);
                binding.durationTv.setText(Music.convertMillisToString(mediaPlayer.getDuration()));
                binding.musicSlider.setValueTo(mediaPlayer.getDuration());
                musicState = MusicState.PLAYING;
                binding.playBTN.setImageResource(R.drawable.ic_pause_24dp);

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        goNext();
                    }
                });
            }
        });
        binding.coverIv.setActualImageResource(music.getCoverResId());
        binding.artistIv.setActualImageResource(music.getArtistResId());
        binding.artistTv.setText(music.getArtist());
        binding.musicNameTv.setText(music.getName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void goNext() {
        timer.cancel();
        timer.purge();
        mediaPlayer.release();
        if (cursor < musicList.size() - 1) {
            cursor++;
        } else {
            cursor = 0;
        }
        onMusicChange(musicList.get(cursor));
    }
}