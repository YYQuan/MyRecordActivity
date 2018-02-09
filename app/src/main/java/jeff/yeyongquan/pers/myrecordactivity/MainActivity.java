package jeff.yeyongquan.pers.myrecordactivity;

import android.app.Dialog;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jeff.yeyongquan.pers.myrecordactivity.MediaPlay.MediaPlayer;
import jeff.yeyongquan.pers.myrecordactivity.MediaPlay.VoiceTimeUtils;
import jeff.yeyongquan.pers.myrecordactivity.Recording.AmrRecord;
import jeff.yeyongquan.pers.myrecordactivity.Recording.MP3Record;
import jeff.yeyongquan.pers.myrecordactivity.Recording.RecordFactory;
import jeff.yeyongquan.pers.myrecordactivity.Recording.RecordingManagerI;
import jeff.yeyongquan.pers.myrecordactivity.Recording.WavRecord;
import jeff.yeyongquan.pers.myrecordactivity.Recording.utils.MediaDirectoryUtils;


public class MainActivity extends AppCompatActivity {


    @BindView(R.id.music_stop)
    ImageButton musicStop;
    @BindView(R.id.music_play)
    ImageButton musicPlay;
    @BindView(R.id.now_time)
    TextView nowTime;
    @BindView(R.id.process_seek_bar)
    SeekBar processSeekBar;
    @BindView(R.id.over_time)
    TextView overTime;
    @BindView(R.id.music_name)
    TextView musicName;
    @BindView(R.id.music_information)
    TextView musicInformation;

    public final static int MSG_START_PLAY = 1;
    public final static int MSG_START_MUSIC_STOP = 2;
    public final static int MSG_MUSIC_PROCESS_CHANGE = 3;
    public final static int MSG_MUSIC_TIME_INO = 4;
    public final static int MSG_MUSIC_DOCUMENT_INFO = 5;

    RecordingManagerI recordingManager;
    @BindView(R.id.v_volume)
    View vVolume;
    @BindView(R.id.sp_source)
    MaterialSpinner spSource;
    private MediaPlayer mediaManager = MediaPlayer.getInstance(this);

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_PLAY:
                    Log.d(TAG, "接收到start play的信息 : " + msg.arg1);
                    startPlayMusic(msg.arg1);
                    break;
                case MSG_START_MUSIC_STOP:
                    Log.d(TAG, "接收到music stop  的信息  ");
                    //stopMusic();
                    break;
                case MSG_MUSIC_PROCESS_CHANGE:
                    playProcessChange(msg.arg1);
                    break;
                case MSG_MUSIC_TIME_INO:
                    initPlayInfo(msg.arg1);
                    break;
                case MSG_MUSIC_DOCUMENT_INFO:
                    showFileInfo(mData.get(msg.arg1));
                    break;
                default:
                    break;

            }

        }
    };

    enum PlayState {
        PLAY,
        PAUSE,
        STOP,
    }


    private static final String TAG = "YYQ-->>";
    private static final String SAVE_PATH = "/YYQ_VoiceManager/audio";
    //private static final String SAVE_PATH =RecordingManagerI.FILE_DIRECTORY;
    RecordingManagerI.OnRecordingCallback callback;
    PlayState state = PlayState.STOP;
    PlayState musicState = PlayState.STOP;
    int isPlayNum = -1;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    private File mAudioDir;

    @BindView(R.id.text_recording)
    TextView textRecording;
    @BindView(R.id.recording_control)
    LinearLayout recordingControl;
    @BindView(R.id.music_control)
    LinearLayout musicControl;
    @BindView(R.id.recycleView)
    RelativeLayout recycleView;
    @BindView(R.id.back_btn)
    ImageButton backBtn;
    @BindView(R.id.information_btn)
    ImageButton informationBtn;
    @BindView(R.id.bottonMenu)
    RelativeLayout bottonMenu;
    @BindView(R.id.recording_btn)
    ImageButton recordingBtn;
    @BindView(R.id.recording_count_text)
    TextView recordingCountText;
    @BindView(R.id.recording_stop_btn)
    ImageButton recordingStopBtn;


    @OnClick(R.id.recording_btn)
    public void recordingBtnClick() {

        if(mediaManager.isPlaying()){
            return;
        }

        switch (state) {
            case PLAY:
                state = PlayState.PAUSE;
                //recordingBtn.setBackgroundResource(R.drawable.recording_btn_press);
                recordingBtn.setBackgroundResource(R.drawable.recording);

                //manager.pauseOrStartVoiceRecord();
                recordingManager.pauseRecord();
                break;
            case STOP:
                state = PlayState.PLAY;
                Log.d(TAG, "start recording");
                //开启录音

                //manager.startVoiceRecord(Environment.getExternalStorageDirectory().getPath() + SAVE_PATH);
                try {
                    recordingManager.startRecord();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //recordingBtn.setBackgroundResource(R.drawable.recording_pause_press);
                recordingBtn.setBackgroundResource(R.drawable.pause);


                break;

            case PAUSE:

                state = PlayState.PLAY;
                //recordingBtn.setBackgroundResource(R.drawable.recording_pause_press);
                recordingBtn.setBackgroundResource(R.drawable.pause);
                //manager.pauseOrStartVoiceRecord();
                recordingManager.continueRecord();
                break;

            default:
                break;
        }

    }

    @OnClick(R.id.recording_stop_btn)
    public void recoidingStopBtnClick() {
        if (state == PlayState.STOP) {
            return;
        }

        if (recordingManager != null) {

            recordingManager.overRecord();
        }
        state = PlayState.STOP;
        //recordingBtn.setBackgroundResource(R.drawable.recording_btn_press);
        recordingBtn.setBackgroundResource(R.drawable.recording);
        //updateData();
    }


    @OnClick(R.id.back_btn)
    public void backBtnClick() {
        Log.d(TAG, "BACK BTN   CLICK");
        for (File file : mData) {

            file.delete();
            notifySystemToScan(file.getAbsolutePath());
        }
        mData.clear();
        updateData(true);

    }


    private ArrayList<File> mData = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        //指定文件夹
        if (mAudioDir == null) {
            mAudioDir = new File(Environment.getExternalStorageDirectory() + SAVE_PATH);
            notifySystemToScan(mAudioDir.getAbsolutePath());
            Log.d(TAG, " path: " + mAudioDir.getAbsolutePath());
            MediaDirectoryUtils.setMediaManagerProvider(new MediaDirectoryUtils.MediaManagerProvider() {
                @Override
                public File getTempCacheWavFileName() {
                    return new File(mAudioDir, MediaDirectoryUtils.productSimpleFileName(".wav"));
                }

                @Override
                public File getTempAmrFileName() {
                    return new File(mAudioDir, MediaDirectoryUtils.productSimpleFileName(".amr"));
                }

                @Override
                public File getTempMp3FileName() {
                    return new File(mAudioDir, MediaDirectoryUtils.productSimpleFileName(".mp3"));
                }

                @Override
                public File getTempAACFileName() {
                    return new File(mAudioDir, MediaDirectoryUtils.productSimpleFileName(".aac"));
                }

                @Override
                public File getTempCachePcmFileName() {
                    return new File(mAudioDir, MediaDirectoryUtils.productSimpleFileName(".pcm"));
                }

                @Override
                public File getCachePath() {
                    return mAudioDir;
                }

                @Override
                public String productFileName(String postfix) {
                    Date date = new Date(System.currentTimeMillis()); //2016-01-28 12:02:28  14位年月日
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    return sdf.format(date) + MediaDirectoryUtils.getRandom(3) + postfix;
                }
            });

            //mAudioDir = new File(SAVE_PATH);
        }
        if (!mAudioDir.exists()) {
            //创建文件夹
            mAudioDir.mkdirs();
            Log.d(TAG, "创建文件夹");
        }

        if (mAudioDir.exists()) {
            Log.d(TAG, "文件夹已经存在");
            mData.clear();
            File[] files = mAudioDir.listFiles();
            for (File file : files) {

                if (file.getAbsolutePath().endsWith("amr") || file.getAbsolutePath().endsWith("wav") || file.getAbsolutePath().endsWith("mp3")) {
                    Log.d(TAG, "add file to mData " + file.getName());
                    mData.add(file);
                }
            }
        }

        spSource.setItems("wav","mp3","amr");
        spSource.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                switch (position){
                    case 0:
                        recordingManager = RecordFactory.getRecordManagerInstance(WavRecord.class);
                        recordingManager.setRecordingOverCallback(callback);
                        break;
                    case 1:
                        recordingManager = RecordFactory.getRecordManagerInstance(MP3Record.class);
                        recordingManager.setRecordingOverCallback(callback);
                        break;
                    case 2:
                        recordingManager = RecordFactory.getRecordManagerInstance(AmrRecord.class);
                        recordingManager.setRecordingOverCallback(callback);
                        break;
                    default:
                        break;
                }
            }
        });


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        MyAdapter adapter = new MyAdapter(mData, mHandler, this,true);
        recyclerView.setAdapter(adapter);


        callback = new RecordingManagerI.OnRecordingCallback() {


            @Override
            public void onOver(File file) {
                Log.d(TAG, "onOver   file name is :" + file.getName());
                //mFile = file;
                //tvFileName.setText(file.getName());
                //tvRecordDurationTime.setText("0");

                recordingCountText.setText("00:00:00");
                updateData(true);
                //需要通知系统有文件改变，要不pc读手机内存时会读不到没通知系统且没有被执行过的文件
                notifySystemToScan(file.getAbsolutePath());
                vVolume.setBackgroundResource(R.drawable.volume1);
                showBottomDialog(file);
            }

            @Override
            public void onStart() {
                Log.d(TAG, "onStart");
                //tvRecordDurationTime.setText("0");
                updateData(false);

            }

            @Override
            public void onPause() {
                Log.d(TAG, "onPause");
            }

            @Override
            public void onResume() {
                Log.d(TAG, "onResume");
            }

            @Override
            public void onSecondChnage(int duration) {
                Log.d(TAG, "  duration : " + duration);
                int inta = duration / 1000;
                if (inta < 60) {
                    if (inta % 60 > 9) {
                        recordingCountText.setText("00:00:" + inta % 60);
                    } else {
                        recordingCountText.setText("00:00:0" + inta % 60);
                    }
                } else if (inta / 60 < 10) {
                    if (inta % 60 > 9) {
                        recordingCountText.setText("00:0" + (inta / 60) + ":" + inta % 60);
                    } else {
                        recordingCountText.setText("00:0" + (inta / 60) + ":0" + inta % 60);
                    }

                } else {
                    if (inta % 60 > 9) {
                        recordingCountText.setText("00:" + (inta / 60) + ":" + inta % 60);
                    } else {
                        recordingCountText.setText("00:0" + (inta / 60) + ":0" + inta % 60);
                    }

                }




            }

            @Override
            public void onVolumeChange(int volumeGrade) {
                switch (volumeGrade) {
                    case 0:
                        vVolume.setBackgroundResource(R.drawable.volume1);
                        break;
                    case 1:
                        vVolume.setBackgroundResource(R.drawable.volume2);
                        break;
                    case 2:
                        vVolume.setBackgroundResource(R.drawable.volume3);
                        break;
                    case 3:
                        vVolume.setBackgroundResource(R.drawable.volume4);
                        break;
                    case 4:
                        vVolume.setBackgroundResource(R.drawable.volume5);
                        break;
                    case 5:
                        vVolume.setBackgroundResource(R.drawable.volume6);
                        break;
                    case 6:
                        vVolume.setBackgroundResource(R.drawable.volume7);
                        break;
                    case 7:
                        vVolume.setBackgroundResource(R.drawable.volume8);
                        break;
                    case 8:
                        vVolume.setBackgroundResource(R.drawable.volume9);
                        break;
                    case 9:
                        vVolume.setBackgroundResource(R.drawable.volume10);
                        break;
                    case 10:
                        vVolume.setBackgroundResource(R.drawable.volume11);
                        break;
                    default:
                        vVolume.setBackgroundResource(R.drawable.volume1);
                        break;
                }
            }
        };

        //manager = RecordFactory.getRecordManagerInstance(AmrRecord.class);
        recordingManager = RecordFactory.getRecordManagerInstance(WavRecord.class);


        recordingManager.setRecordingOverCallback(callback);

    }

    private void updateData(boolean enable) {
        if (mAudioDir.exists()) {
            Log.d(TAG, "文件夹已经存在");
            mData.clear();
            File[] files = mAudioDir.listFiles();
            for (File file : files) {

                if (file.getAbsolutePath().endsWith("amr") || file.getAbsolutePath().endsWith("wav") || file.getAbsolutePath().endsWith("mp3")) {
                    Log.d(TAG, "add file to mData " + file.getName());
                    mData.add(file);
                }
            }

        }
        MyAdapter adapter = new MyAdapter(mData, mHandler, this,enable);

        recyclerView.setAdapter(adapter);
    }

    //开始播录音
    public void startPlayMusic(int position) {
        if (mData.size() < position) {
            return;
        }
        File file = mData.get(position);
        if (file.exists()) {
            //manager.stopPlay();
            mediaManager.setSeekBarListener(processSeekBar);
            mediaManager.stopPlay();
            musicPlay.setBackgroundResource(R.drawable.recording_pause_press);
            isPlayNum = position;
            musicState = PlayState.PLAY;
            musicName.setText(file.getName());
            musicInformation.setText(file.getAbsolutePath());
            //manager.setSeekBarListener(processSeekBar);

            //播放的回调
            mediaManager.setVoicePlayListener(new MediaPlayer.VoicePlayCallBack() {
                @Override
                public void voiceTotalLength(long time, String strTime) {
                    Log.d(TAG, "duration  time is :" + time + "  strTime : " + strTime);
                    Message msg = mHandler.obtainMessage(MSG_MUSIC_TIME_INO);
                    msg.arg1 = (int) time;
                    mHandler.sendMessage(msg);
                    //initPlayInfo((int)time);

                }

                @Override
                public void playDoing(long time, String strTime) {
                    Log.d(TAG, "playDoing time " + time);
                    Message msg = mHandler.obtainMessage(MSG_MUSIC_PROCESS_CHANGE);
                    msg.arg1 = (int) time;
                    mHandler.sendMessage(msg);
                    //playProcessChange((int)time);
                }

                @Override
                public void playPause() {
                    Log.d(TAG, "  playPause ");
                }

                @Override
                public void playStart() {
                    Log.d(TAG, "  playStart ");
                }

                @Override
                public void playFinish() {
                    Message msg = mHandler.obtainMessage(MSG_START_MUSIC_STOP);
                    mHandler.sendMessage(msg);
                    stopMusic();
                    Log.d(TAG, "playFinish");
                }
            });
            //添加了播放回调之后，再开始播放
            mediaManager.startPlay(mData.get(position).getAbsolutePath());
        }
    }

    //暂停播录音
    private void pausePlayMusic() {
        if (isPlayNum >= 0) {
            //manager.continueOrPausePlay();
            //recordingManager.pauseRecord();
            mediaManager.continueOrPausePlay();
            musicPlay.setBackgroundResource(R.drawable.music_play_press);
            musicState = PlayState.PAUSE;

        }
    }

    //继续播录音
    private void continuePlayMusic() {
        if (isPlayNum >= 0) {
            //manager.continueOrPausePlay();
            mediaManager.continueOrPausePlay();

            musicPlay.setBackgroundResource(R.drawable.recording_pause_press);
            musicState = PlayState.PLAY;
        }
    }


    //停止播放录音

    /**
     * 如果这个函数放在handler下处理的话，就会有导致容易崩溃，而且 播放了录音之后，就不能回调当前录音的时长等，
     * 原因是用异步来做的话，会出现开始录音之后，把manager里的intervalMSG给移除了。
     * 开始录音之后就会调用VoicePlayCallback的playFinish ,然后再开始真正启动录音
     * 这是如果在playFinish里，用了个异步的stopMusic的话，就很可能出现启动了录音之后，才执行到了handler里的hangdleMessage
     * stopPlay里执行了  remoteMessage(INTERVAL) ，因此就出现了不能回调当前录音的时长等异常现象
     */
    void stopMusic() {
        if (isPlayNum >= 0) {
            //manager.stopPlay();
            mediaManager.stopPlay();
            musicPlay.setBackgroundResource(R.drawable.music_play_press);
            musicState = PlayState.STOP;
            nowTime.setText(timeToDisplayString(0));
            overTime.setText(timeToDisplayString(0));
            processSeekBar.setProgress(0);
        }
    }

    //music播放进度发生改变
    void playProcessChange(int time) {
        nowTime.setText(timeToDisplayString(time));
    }

    //初始事件
    void initPlayInfo(int time) {
        overTime.setText(timeToDisplayString(time));
    }

    //s时间转换成字符
    String timeToDisplayString(int time) {
        time %= 3600;
        int min = time / 60;
        int sec = time % 60;
        String str;
        if (min < 10 && sec < 10) {
            str = new String("0" + min + ":0" + sec);
        } else if (min < 10 && sec > 10) {
            str = new String("0" + min + ":" + sec);
        } else if (min > 10 && sec < 10) {
            str = new String("" + min + ":0" + sec);
        } else {
            str = new String("" + min + ":" + sec);
        }
        return str;
    }

    @OnClick(R.id.music_play)
    void musicPlayClick() {
        if(recordingManager.isRecordIng()){
            return;
        }
        if (isPlayNum >= 0) {
            switch (musicState) {
                case STOP:
                    startPlayMusic(isPlayNum);
                    Log.d(TAG, "MUSIC_STOP");
                    break;
                case PLAY:
                    pausePlayMusic();

                    break;
                case PAUSE:
                    continuePlayMusic();
                    break;
                default:
                    break;
            }
        }
    }

    @OnClick(R.id.music_stop)
    void musicStopClick() {
        stopMusic();
    }


    public void notifySystemToScan(String filePath) {

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);

        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        getApplication().sendBroadcast(intent);
        //this.sendBroadcast(intent);
    }

    private void showBottomDialog(final File file){

        final Dialog bottomDialog = new Dialog(this, R.style.BottomDialog);
        View contentView = LayoutInflater.from(this).inflate(R.layout.dialog_content_normal, null);

        bottomDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
        bottomDialog.setCanceledOnTouchOutside(true);
        bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        Button btn1 = contentView.findViewById(R.id.bt_rename);
        Button btn2 = contentView.findViewById(R.id.bt_delete);
        TextView tv  = contentView.findViewById(R.id.tv_record_file_name);

        tv.setText(file.getName());

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomDialog.dismiss();
                showRenameDialog(file);


            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                file.delete();
                notifySystemToScan(file.getPath());
                bottomDialog.dismiss();
                updateData(true);
            }
        });

        bottomDialog.show();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomDialog.dismiss();
            }
        },5000);


    }


    private void showRenameDialog(final File file){
        final Dialog dialog = new Dialog(MainActivity.this, R.style.BottomDialog);
        View content2View = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_rename_content_normal, null);
        Log.d(TAG,"---------------------------dddd------");
        dialog.setContentView(content2View);
        ViewGroup.LayoutParams layoutParams = content2View.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        content2View.setLayoutParams(layoutParams);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.setCanceledOnTouchOutside(true);
        //dialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        final EditText ed = content2View.findViewById(R.id.et_rename_edit);
        Button   bt3 = content2View.findViewById(R.id.bt_rename_sure);


        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file1;
                if(recordingManager instanceof WavRecord) {
                    file1  = new File(file.getParent(),ed.getText()+".wav");
                }else if(recordingManager instanceof MP3Record){
                    file1  = new File(file.getParent(),ed.getText()+".mp3");
                }else{
                    file1  = new File(file.getParent(),ed.getText()+".amr");
                }

                if(file1.exists()){
                    Toast.makeText(MainActivity.this, "file is exist", Toast.LENGTH_SHORT).show();
                }else{
                    file.renameTo(file1);
                    dialog.dismiss();
                    updateData(true);
                    notifySystemToScan(file.getPath());
                    notifySystemToScan(file1.getPath());
                }

            }
        });


        dialog.show();
    }

    private void showFileInfo(final File file){
        final Dialog dialog = new Dialog(this, R.style.BottomDialog);
        View contentView = LayoutInflater.from(this).inflate(R.layout.dialog_info_content_normal, null);

        dialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);

        TextView tvName = contentView.findViewById(R.id.tv_info_name);
        TextView tvFormat = contentView.findViewById(R.id.tv_info_format);
        TextView tvTime = contentView.findViewById(R.id.tv_info_time);
        TextView tvPath = contentView.findViewById(R.id.tv_info_path);
        TextView tvSize = contentView.findViewById(R.id.tv_info_size);
        Button btnDelete = contentView.findViewById(R.id.bt_info_delete);
        Button btnRename = contentView.findViewById(R.id.bt_info_rename);




        android.media.MediaPlayer media =  new android.media.MediaPlayer();
        try {
            if(!file.exists()) {
                return;
            }

            FileInputStream fls = new FileInputStream(file);
            media.setDataSource(fls.getFD());
            //media.setDataSource(file.getAbsolutePath());

            media.setAudioStreamType(AudioManager.STREAM_MUSIC);
            media.prepare();
            media.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                    mp.stop();
                    mp.release();
                    return false;
                }
            });

            media.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener(){

                @Override
                public void onCompletion(android.media.MediaPlayer arg0) {
                    arg0.stop();
                    arg0.release();

                }

            });
        } catch (MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        VoiceTimeUtils ts = VoiceTimeUtils.timeSpanSecond(media.getDuration()/1000);
        media.release();
        String voiceLength = String.format("%02d:%02d:%02d",
                ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond);

        tvName.setText(file.getName());
        tvPath.setText(file.getAbsolutePath());
        tvTime.setText(voiceLength);

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            tvSize.setText(stream.available()/1024+"kb");
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(recordingManager instanceof WavRecord) {
            tvFormat.setText(R.string.wav_format);
        }else if(recordingManager instanceof MP3Record){
            tvFormat.setText(R.string.mp3_format);
        }else{
            tvFormat.setText(R.string.amr_format);
        }


        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                file.delete();
                notifySystemToScan(file.getPath());
                updateData(true);
            }
        });

        btnRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRenameDialog(file);
                dialog.dismiss();
                updateData(true);
            }
        });

        dialog.show();
    }

}
