package jeff.yeyongquan.pers.myrecordactivity.Recording;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import cn.qssq666.audio.AudioManager;
import jeff.yeyongquan.pers.myrecordactivity.Recording.mp3.DataEncodeThread;
import jeff.yeyongquan.pers.myrecordactivity.Recording.mp3.PCMFormat;
import jeff.yeyongquan.pers.myrecordactivity.Recording.utils.MediaDirectoryUtils;


/**
 * Created by Yqquan on 2018/1/30.
 * mp3和amr ,wav不太一样
 * 这里的实现是在audioRecord中把数据流读出来，同时异步转码（这一步和wav是一样的）
 *
 * 但是暂时没有找到两个完整的MP3的文件拼接方法，但是发现MP3好像是没有头只有尾的，但是尾部的长度等信息我都不清楚。
 * 因此这里的做法是，一直把pcm流给转码为MP3,  最后在用MP3的流 去获取尾码。
 *
 *
 * 所以说，只要结束就等于最终文件生成了，并不需要merge这一步。
*/



public class MP3Record extends RecordingManagerI {

    //=======================AudioRecord Default Settings=======================
    //private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     */
    private static final int DEFAULT_SAMPLING_RATE = 44100;//模拟器仅支持从麦克风输入8kHz采样率
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 下面是对此的封装
     * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
     */
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    //======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
     */
    //private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32 * 6 ;  // 192 kbps

    /**
     * 自定义 每160帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 160;

    //==================================================================
    private AudioRecord mAudioRecord = null;
    private int mBufferSize;
    private short[] mPCMBuffer;
    private DataEncodeThread mEncodeThread;
    private boolean mIsRecording = false;
    private File mRecordFile;

    @Override
    protected File startRecordCreateFile() throws IOException {
        if(mEncodeThread==null||mEncodeThread.getEnableOverEncode()== DataEncodeThread.OverEncode2Mp3State.standby) {

            currenttime = 0;//从新清空
            mRecordFile = MediaDirectoryUtils.getTempMp3FileName();
        }
        start();

        //由于不需要拼接，因此 直接返回null即可
        return null;
    }

    @Override
    protected void stop() {


        mIsRecording = false;
    }

    @Override
    protected void pause() {
        stopRecord();

    }

    @Override
    protected void conTinue() {
        try {
            startRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //由于转码和拼接都是异步的， over之后最终文件直接一步创建完成
    @Override
    public boolean mergeFile() {
        mEncodeThread.setEnableOverEncode(DataEncodeThread.OverEncode2Mp3State.standby);


        setmFile(mRecordFile);

        afterMergeFile();
        return false;
    }

    @Override
    public Object getInternAudioRecord() {
        return null;
    }




    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    private void start() throws IOException {
        if (mIsRecording) {
            return;
        }
        mIsRecording = true; // 提早，防止init或startRecording被多次调用
        initAudioRecorder();
        mAudioRecord.startRecording();


        new Thread() {
            @Override
            public void run() {
                //设置线程权限
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (mIsRecording) {
                    int readSize = mAudioRecord.read(mPCMBuffer, 0, mBufferSize);


                    //这个可以用来做音量的变化  但是很不精确，buffersize 会影响record.read的频率  ，如果要控制 获取volume的周期，可以通过改变buffersize ,
                    //但是不知道有没有别的影响
                    //大概范围时-5 ~70
                    {
                        long v = 0;
                        // 将 buffer 内容取出，进行平方和运算
                        for (int i = 0; i < readSize; i++) {
                            v += mPCMBuffer[i] * mPCMBuffer[i];

                        }
                        // 平方和除以数据总长度，得到音量大小。
                        double mean = v / (double) readSize;
                        double volume = 10 * Math.log10(mean);
                        setVolume((int) volume);
                    }

                    if (readSize > 0) {
                        mEncodeThread.addTask(mPCMBuffer, readSize);
                    }
                }
                // release and finalize audioRecord
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
                if(mEncodeThread.getEnableOverEncode()== DataEncodeThread.OverEncode2Mp3State.standby) {
                    // stop the encoding thread and try to wait
                    // until the thread finishes its job
                    mEncodeThread.sendStopMessage();
                }

            }
        }.start();

    }

    @Override
    protected int getVolume() {
        //大概范围时-5 ~70

        if(super.getVolume()>65){
            return 10;
        }else if(super.getVolume()<=0){
            return 0;
        }else{
            return super.getVolume()/7;
        }

    }

    /**
     * Initialize audio recorder
     */
    private void initAudioRecorder() throws IOException {
        mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        /* Get number of samples. Calculate the buffer size
         * (round up to the factor of given frame size)
		 * 使能被整除，方便下面的周期性通知
		 * */
        int frameSize = mBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mBufferSize = frameSize * bytesPerFrame;
        }

		/* Setup audio recorder */
        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                mBufferSize);

        mPCMBuffer = new short[mBufferSize];
        /*
         * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate
		 * The bit rate is 32kbps
		 *
		 */


        // Create and run thread used to encode data
        // The thread will
        AudioManager audioManager = new AudioManager();
        audioManager.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        if(mEncodeThread==null||mEncodeThread.getEnableOverEncode()== DataEncodeThread.OverEncode2Mp3State.standby) {
            mEncodeThread = new DataEncodeThread(mRecordFile, mBufferSize, audioManager);
            mEncodeThread.start();
        }
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }





}
