package jeff.yeyongquan.pers.myrecordactivity.Recording;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jeff.yeyongquan.pers.myrecordactivity.Recording.utils.MediaDirectoryUtils;


/**
 * Created by Yqquan on 2018/1/29.
 */

public abstract class RecordingManagerI {
    public final static String FILE_DIRECTORY = Environment.getExternalStorageDirectory().getPath()+"/"+ MediaDirectoryUtils.ROOT_FOLDER+"/"+MediaDirectoryUtils.cacheStr;




    private int record_state = RecordState.RECORD_OVER_STOP;


    protected  OnRecordingCallback  recordingOverCallback  = null;

    //当前录音时长
    protected int currenttime = 0;

    //记录录音状态
    protected boolean isRecording = false;


    //最终录音文件
    protected File mFile = null;




    //当前音量值   不一定有效
    int volume  = 0;


    //用于拼接
    private ArrayList<File> mData = new ArrayList<>();


    private final Handler mHandler = new Handler();
    private Runnable mAddTimeRunnnable = new Runnable() {
        @Override
        public void run() {

            currenttime++;
            mHandler.postDelayed(this, 1000);//1秒一次
            if (recordingOverCallback != null) {
                recordingOverCallback.onSecondChnage(1000 * currenttime);
            }

        }
    };

    private Runnable  volumeChangeRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(this, 500);//1秒一次
            if (recordingOverCallback != null) {

                recordingOverCallback.onVolumeChange(getVolume());
            }
        }
    };

    private final String TAG = "RecordingManagerI";




    //开始录音
    public  void startRecord() throws IOException{

        switch (getRecordState()){
            case RecordState.RECORD_OVER_STOP:
                if(mData.size()>0){
                    mData.clear();
                }

                if(recordingOverCallback!=null) {
                    recordingOverCallback.onStart();
                }
                break;
            //不要响应pause状态下的 startRecord  会由于异步出现 先创建了新的文件 再 merge
            case RecordState.RECORD_RESUME:

                break;
            default:
                return;
        }



        isRecording = true;
        record_state = RecordState.REACORDING;
        File f = startRecordCreateFile();
        if(f!=null) {
            mData.add(f);
        }



        mHandler.postDelayed(mAddTimeRunnnable, 1000);
        mHandler.postDelayed(volumeChangeRunnable, 500);

        Log.d(TAG," mHandler.postDelayed(mAddTimeRunnnable, 1000);");

    };

    protected abstract File startRecordCreateFile() throws IOException;

    //停止录音   一段录音完成
    protected void  stopRecord(){
        isRecording = false;
        stop();
        record_state =RecordState.RECORD_OVER_STOP;
        mHandler.removeCallbacks(mAddTimeRunnnable);
        mHandler.removeCallbacks(volumeChangeRunnable);

    }


    protected abstract void stop();


    public void pauseRecord(){
        if(getRecordState()!= RecordState.REACORDING){
            return;
        }
        pause();
        record_state = RecordState.RECORD_PAUSE;
        if(recordingOverCallback!=null) {
            recordingOverCallback.onPause();
        }
    }

    //暂停录音
    protected abstract void pause();

    //继续录音
    public  void continueRecord(){
        if(getRecordState()!= RecordState.RECORD_PAUSE){
            return;
        }
        record_state = RecordState.RECORD_RESUME;
        conTinue();


        if(recordingOverCallback!=null) {
            recordingOverCallback.onResume();
        }
    };

    protected abstract void conTinue();

    //合并文件  完成拼接操作   注意要在里面调用 afterMergeFile
    public abstract boolean mergeFile();

    //一次录音完成
    public void overRecord(){

        switch (getRecordState()){
            case RecordState.RECORD_PAUSE:
                break;
            case RecordState.REACORDING:
                break;
            default:
                return;
        }

        stopRecord();


        //mergeFile一般是异步进行的， 注意  不要 先处理了afterMergeFile  才执行到异步的mergeFile
        //还是把afterMergeFile 放在子类中执行吧 。要不这异步问题有点难搞
        //afterMergeFile();
        mergeFile();

        currenttime = 0;

    };

    //是否正在录音
    public boolean isRecordIng() {
        return record_state == RecordState.REACORDING||record_state == RecordState.RECORD_PAUSE;
    }

    //合并后的处理   可能用于删除一些不需要的文件   注意 要在子类的mergeFile中调用该函数 ，注意异步问题
    protected  void  afterMergeFile(){
        for(File f: mData){
            if(f.exists()) {
                f.delete();
            }
        }
        mData.clear();

        if (recordingOverCallback != null&&getmFile()!=null) {
            recordingOverCallback.onOver(getmFile());
            setmFile(null);
        }
    };





    public void setRecordingOverCallback(OnRecordingCallback recordingOver) {
        this.recordingOverCallback = recordingOver;
    }


    public int getRecordState() {
        return record_state;
    }

    protected File getmFile() {
        return mFile;
    }

    protected void setmFile(File mFile) {
        this.mFile = mFile;
    }

    //得到需要拼接的文件的array list
    public ArrayList<File> getmData() {
        return mData;
    }

    // 当前录音时长
    public int getCurrenttime() {
        return currenttime;
    }

    /**
     * 获取内部管理器 通常返回的是 {@link AudioRecord }或 {@link MediaRecorder}
     *
     * @return
     */
    public abstract Object getInternAudioRecord();


    /**
     * 要注意每一种格式  返回的值的意义是不同的
     * 如同样是38  wav格式 已经很大了  但是MP3那是比较小的。
     * 后面最好就把音声分等级输出，而不要直接输出数值
     * @return
     */
    protected   int getVolume(){

        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    /**
     * RECORDING over  callback
     */
    public interface OnRecordingCallback {

        //这是一次录音的结束回调   一次里面可能有多段  回传最终生成的文件
        void onOver(File file);
        //开始录音的回调
        void onStart();
        //暂停
        void onPause();
        //继续录音
        void onResume();
        //录音ing 的秒回调
        void onSecondChnage(int duration);
        //录音音量回调
        void onVolumeChange(int volumeGrade);
    }


    // 录音当前说话音量分贝大小回调  能够得到当前mic的输入音量，但是不能不能边录边获取音量，  audioRecord 不能多处同时使用
    class   RecordingVolumeThread extends  Thread{
        final int SAMPLE_RATE_IN_HZ = 8000;
        final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord  mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                                                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                                                                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        RecordingManagerI manager ;
        private Object mLock;

        public RecordingVolumeThread(RecordingManagerI manager) {
            this.manager = manager;
            mLock = new Object();
        }

        @Override
        public void run() {
            Log.d(TAG, "开始计算 分贝值");
            mAudioRecord.startRecording();

            short[] buffer = new short[BUFFER_SIZE];
            while (manager.isRecordIng()) {
                //r是实际读取的数据长度，一般而言r会小于buffersize
                int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                long v = 0;
                // 将 buffer 内容取出，进行平方和运算
                for (int i = 0; i < buffer.length; i++) {
                    v += buffer[i] * buffer[i];
                }
                // 平方和除以数据总长度，得到音量大小。
                double mean = v / (double) r;
                double volume = 10 * Math.log10(mean);
                Log.d(TAG, "分贝值:" + volume);
                // 大概一秒十次
                synchronized (mLock) {
                    try {
                        mLock.wait(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
            Log.d(TAG, "结束计算 分贝值");
        }


    }

}
