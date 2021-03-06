package com.foobnix.tts;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Build;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class TTSControlsView extends FrameLayout {

    private ImageView ttsPlayPause;
    private DocumentController controller;

    public void setDC(DocumentController dc) {
        controller = dc;
    }

    private ImageView ttsDialog;

    Handler handler;

    public void addOnDialogRunnable(final Runnable run) {
        ttsDialog.setVisibility(View.VISIBLE);
        ttsDialog.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                run.run();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public TTSControlsView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.tts_mp3_line, this, false);
        addView(view);

        final ImageView ttsStop = (ImageView) view.findViewById(R.id.ttsStop);
        ttsPlayPause = (ImageView) view.findViewById(R.id.ttsPlay);

        final ImageView ttsNext = (ImageView) view.findViewById(R.id.ttsNext);
        final ImageView ttsPrev = (ImageView) view.findViewById(R.id.ttsPrev);

        ttsDialog = (ImageView) view.findViewById(R.id.ttsDialog);
        ttsDialog.setVisibility(View.GONE);

        int color = Color.parseColor(AppState.get().isDayNotInvert ? BookCSS.get().linkColorDay : BookCSS.get().linkColorNight);

        TintUtil.setTintImageWithAlpha(ttsStop, color);
        TintUtil.setTintImageWithAlpha(ttsPlayPause, color);
        TintUtil.setTintImageWithAlpha(ttsNext, color);
        TintUtil.setTintImageWithAlpha(ttsPrev, color);
        TintUtil.setTintImageWithAlpha(ttsDialog, color);

        ttsNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_NEXT, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    next.send();
                } catch (CanceledException e) {
                    LOG.d(e);
                }

            }
        });
        ttsPrev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PREV, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    next.send();
                } catch (CanceledException e) {
                    LOG.d(e);
                }

            }
        });

        ttsStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_STOP_DESTROY, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    next.send();
                } catch (CanceledException e) {
                    LOG.d(e);
                }
            }
        });

        ttsPlayPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (TTSEngine.get().isPlaying()) {
                    PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    try {
                        next.send();
                    } catch (CanceledException e) {
                        LOG.d(e);
                    }
                } else {
                    TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "", controller.getBookWidth(), controller.getBookHeight(), AppState.get().fontSizeSp, controller.getTitle());
                }
            }
        });
        handler = new Handler();
        seekMp3 = (SeekBar) view.findViewById(R.id.seekMp3);
        seekCurrent = (TextView) view.findViewById(R.id.seekCurrent);
        seekMax = (TextView) view.findViewById(R.id.seekMax);
        layoutMp3 = view.findViewById(R.id.layoutMp3);

        int tinColor = ColorUtils.setAlphaComponent(color, 230);

        seekMp3.getProgressDrawable().setColorFilter(tinColor, Mode.SRC_ATOP);
        if (Build.VERSION.SDK_INT >= 16) {
            seekMp3.getThumb().setColorFilter(tinColor, Mode.SRC_ATOP);
        }
        TintUtil.setTintText(seekCurrent, tinColor);
        TintUtil.setTintText(seekMax, tinColor);

        layoutMp3.setVisibility(View.GONE);
        initMp3();

    }

    public void initMp3() {
        if (TTSEngine.get().isMp3() && layoutMp3.getVisibility() == View.GONE) {
            layoutMp3.setVisibility(View.VISIBLE);

            seekMp3.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        LOG.d("Seek-onProgressChanged", progress);
                        TTSEngine.get().mp.seekTo(progress);
                    }
                }
            });

        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        if (ttsPlayPause != null) {
            update.run();
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(update, 250);
        }
    }

    Runnable update = new Runnable() {

        @Override
        public void run() {
            if (TTSEngine.get().isMp3()) {
                initMp3();
                if (TTSEngine.get().mp != null) {
                    seekCurrent.setText(TxtUtils.getMp3TimeString(TTSEngine.get().mp.getCurrentPosition()));
                    seekMax.setText(TxtUtils.getMp3TimeString(TTSEngine.get().mp.getDuration()));

                    seekMp3.setMax(TTSEngine.get().mp.getDuration());
                    seekMp3.setProgress(TTSEngine.get().mp.getCurrentPosition());
                }

            } else {
                layoutMp3.setVisibility(View.GONE);
            }

            LOG.d("TtsStatus-isPlaying", TTSEngine.get().isPlaying());
            ttsPlayPause.setImageResource(TTSEngine.get().isPlaying() ? R.drawable.glyphicons_175_pause : R.drawable.glyphicons_174_play);
        }
    };
    private View layoutMp3;
    private SeekBar seekMp3;
    private TextView seekCurrent;
    private TextView seekMax;

}
