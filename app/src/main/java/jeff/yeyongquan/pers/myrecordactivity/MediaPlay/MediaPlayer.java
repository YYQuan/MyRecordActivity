package jeff.yeyongquan.pers.myrecordactivity.MediaPlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;

import jeff.yeyongquan.pers.myrecordactivity.MainActivity;

/**
 * Created by Yqquan on 2018/2/1.
 */

public class MediaPlayer
{



    public final int MSG_TIME_INTERVAL = 100;

    public final int MEDIA_STATE_UNDEFINE = 200;
    public final int MEDIA_STATE_PLAY_STOP = 310;
    public final int MEDIA_STATE_PLAY_DOING = 320;
    public final int MEDIA_STATE_PLAY_PAUSE = 330;



    private static Context mContext = null;
    private SeekBar mSBPlayProgress;
    private int mSavedState, mDeviceState = MEDIA_STATE_UNDEFINE;
    private android.media.MediaPlayer mMediaPlayer = null;



    private static MediaPlayer  mediaPlayer ;
    private MediaPlayer(Context context) {

    }

    /**
     * 获取单例
     * @param context
     * @return
     */
    public static MediaPlayer getInstance(Context context){
        if(mediaPlayer == null){
            synchronized(MediaPlayer.class){
                if(mediaPlayer == null){
                    mediaPlayer = new MediaPlayer(context);
                    mContext = context;
                }
            }
        }
        return mediaPlayer;
    }

    /**
     * 播放音频文件位置
     */
    private String playFilePath;

    /**
     * 播放监听
     */
    private VoicePlayCallBack voicePlayCallBack;


    /**
     * 播放器结束监听
     */
    private android.media.MediaPlayer.OnCompletionListener mPlayCompetedListener = new android.media.MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(android.media.MediaPlayer mp) {
            mDeviceState = MEDIA_STATE_PLAY_STOP;
            mHandler.removeMessages(MSG_TIME_INTERVAL);
            mMediaPlayer.stop();
            mMediaPlayer.release();
            if (mSBPlayProgress != null) {
                mSBPlayProgress.setProgress(0);
            }
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playFinish();
            }
        }
    };

    /**
     * 播放或录音handler
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            VoiceTimeUtils ts;
            int current;
            try {
                switch (msg.what) {
                    //录音
                    case MSG_TIME_INTERVAL:
                            current = mMediaPlayer.getCurrentPosition();
                            if (mSBPlayProgress != null) {
                                mSBPlayProgress.setProgress(current);
                            }
                            ts = VoiceTimeUtils.timeSpanSecond(current / 1000);
                            //回调播放进度
                            if (voicePlayCallBack != null) {
                                voicePlayCallBack.playDoing(current / 1000, String.format("%02d:%02d:%02d",
                                        ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond));
                            }
                            mHandler.sendEmptyMessageDelayed(MSG_TIME_INTERVAL, 1000);

                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            }
        }
    };

    /**
     * 播放监听
     *
     * @param callBack
     */
    public void setVoicePlayListener(VoicePlayCallBack callBack) {
        voicePlayCallBack = callBack;
    }

    /**
     * 播放SeekBar监听
     *
     * @param seekBar
     */
    public void setSeekBarListener(SeekBar seekBar) {
        mSBPlayProgress = seekBar;
        if (mSBPlayProgress != null) {
            mSBPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mHandler.removeMessages(MSG_TIME_INTERVAL);
                    mSavedState = mDeviceState;
                    if (mSavedState == MEDIA_STATE_PLAY_DOING) {
                        pauseMedia(mMediaPlayer);
                    }
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mHandler.removeMessages(MSG_TIME_INTERVAL);
                    VoiceTimeUtils ts = VoiceTimeUtils.timeSpanSecond(progress / 1000);
                    //播放进度
                    if (voicePlayCallBack != null) {
                        voicePlayCallBack.playDoing(progress / 1000, String.format("%02d:%02d:%02d",
                                ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond));
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seektoMedia(mMediaPlayer, mSBPlayProgress.getProgress());

                    if (mSavedState == MEDIA_STATE_PLAY_DOING) {
                        playMedia(mMediaPlayer);
                        mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
                    }
                }
            });
        }
    }


    /**
     * 开始播放（外部调）
     *
     * @param filePath 音频存放文件夹
     */
    public void startPlay(String filePath) {
        if(mDeviceState==MEDIA_STATE_PLAY_DOING||mDeviceState==MEDIA_STATE_PLAY_PAUSE){
            return;
        }
        if (TextUtils.isEmpty(filePath)|| !new File(filePath).exists())
        {
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playFinish();
            }
            Toast.makeText(mContext,"文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }else {
            playFilePath = filePath;
            startPlay(true);
        }
    }


    /**
     * 开始播放（内部调）
     *
     * @param init
     */
    private void startPlay(boolean init) {
        try {


            stopMedia(mMediaPlayer, true);
            mMediaPlayer = null;


            mMediaPlayer = new android.media.MediaPlayer();

            //mMediaPlayer = android.media.MediaPlayer.create(mContext, Uri.fromFile(new File(playFilePath)));



            mMediaPlayer.setOnCompletionListener(mPlayCompetedListener);

            if (prepareMedia(mMediaPlayer, playFilePath)) {
                mDeviceState = MEDIA_STATE_PLAY_DOING;
                //总时间长度
                long totalTime = mMediaPlayer.getDuration() / 1000;
                Log.d("YYQ-->","duration " + totalTime);
                VoiceTimeUtils ts = VoiceTimeUtils.timeSpanSecond(totalTime);
                String voiceLength = String.format("%02d:%02d:%02d",
                        ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond);
                //播放进度回调
                if (voicePlayCallBack != null) {
                    voicePlayCallBack.voiceTotalLength(totalTime, voiceLength);
                    voicePlayCallBack.playStart();
                    Log.d("YYQ-->","duration " + totalTime);
                    voicePlayCallBack.playDoing(0, "00:00:00");
                }
                if (mSBPlayProgress != null) {
                    mSBPlayProgress.setMax(Math.max(1, mMediaPlayer.getDuration()));
                }

                if (init) {
                    if (mSBPlayProgress != null) {
                        mSBPlayProgress.setProgress(0);
                    }
                    seektoMedia(mMediaPlayer, 0);
                } else {
                    seektoMedia(mMediaPlayer, mSBPlayProgress.getProgress());
                }

                if (playMedia(mMediaPlayer)) {
                    mHandler.removeMessages(MSG_TIME_INTERVAL);
                    mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
                }

            }
        } catch (Exception e) {
            Log.e("播放出错了", e.getMessage());
        }
    }

    /**
     * 继续或暂停播放
     */
    public void continueOrPausePlay() {
        if (mDeviceState == MEDIA_STATE_PLAY_DOING) {
            mDeviceState = MEDIA_STATE_PLAY_PAUSE;
            pauseMedia(mMediaPlayer);
            //暂停
            if (voicePlayCallBack != null) {
                voicePlayCallBack.playPause();
            }
            mHandler.removeMessages(MSG_TIME_INTERVAL);
        } else if (mDeviceState == MEDIA_STATE_PLAY_PAUSE) {
            mDeviceState = MEDIA_STATE_PLAY_DOING;
            playMedia(mMediaPlayer);
            //播放中
            mHandler.removeMessages(MSG_TIME_INTERVAL);
            mHandler.sendEmptyMessage(MSG_TIME_INTERVAL);
        } else if (mDeviceState == MEDIA_STATE_PLAY_STOP) {
            //播放
            //if (!TextUtils.isEmpty(playFilePath)) {
            //    startPlay(false);
            //}
        }
    }


    /**
     * 停止播放
     */
    public void stopPlay() {
        if(mDeviceState==MEDIA_STATE_PLAY_STOP){
            return;
        }
        mHandler.removeMessages(MSG_TIME_INTERVAL);
        mDeviceState = MEDIA_STATE_PLAY_STOP;
        stopMedia(mMediaPlayer, true);
        mSBPlayProgress.setProgress(0);
        mMediaPlayer = null;
        if(voicePlayCallBack!=null) {
            voicePlayCallBack.playFinish();
        }
    }


    /**
     * 是否在播放中
     * @return
     */
    public boolean isPlaying(){
        return mDeviceState == MEDIA_STATE_PLAY_DOING||mDeviceState == MEDIA_STATE_PLAY_PAUSE;
    }


    /**
     * 播放录音准备工作
     *
     * @param mp
     * @param file
     * @return
     */
    private boolean prepareMedia(android.media.MediaPlayer mp, String file) {
        boolean result = false;
        try {
            mp.setDataSource(file);
            mp.prepare();
            result = true;
        } catch (Exception e) {
        }

        return result;
    }


    /**
     * 播放录音开始
     *
     * @param mp
     * @return
     */
    private boolean playMedia(android.media.MediaPlayer mp) {
        boolean result = false;
        try {
            if (mp != null) {
                mp.start();
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }


    /**
     * 拖动播放进度条
     *
     * @param mp
     * @param pos
     * @return
     */
    private boolean seektoMedia(android.media.MediaPlayer mp, int pos) {
        boolean result = false;
        try {
            if (mp != null && pos >= 0) {
                mp.seekTo(pos);
                result = true;
            }
        } catch (Exception e) {
        }
        return result;
    }


    /**
     * 停止播放
     *
     * @param mp
     * @param release
     * @return
     */
    private boolean stopMedia(android.media.MediaPlayer mp, boolean release) {
        boolean result = false;
        try {
            if (mp != null) {
                mp.stop();
                Log.d("yyq","stop mediaplay");
                if (release) {
                    mp.release();
                }
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }



    /**
     * 暂停播放
     *
     * @param mp
     * @return
     */
    private boolean pauseMedia(android.media.MediaPlayer mp) {
        boolean result = false;

        try {
            if (mp != null) {
                mp.pause();
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }




    /**
     * SD卡是否可用
     */
    public static boolean isSDCardAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static File recAudioDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }



    /**
     * 播放录音回调监听
     */
    public interface VoicePlayCallBack {

        /**
         * 音频长度
         * 指定的某个时间段，以秒为单位
         */
        void voiceTotalLength(long time, String strTime);

        /**
         * 播放中
         * 指定的某个时间段，以秒为单位
         */
        void playDoing(long time, String strTime);

        //播放暂停
        void playPause();

        //播放开始
        void playStart();

        //播放结束
        void playFinish();
    }
}
