package jeff.yeyongquan.pers.myrecordactivity.Recording;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jeff.yeyongquan.pers.myrecordactivity.Recording.utils.MediaDirectoryUtils;
import jeff.yeyongquan.pers.myrecordactivity.Recording.utils.WavMergeUtil;


/**
 * Created by Yqquan on 2018/1/29.
 */

public class WavRecord extends RecordingManagerI {




    private AudioRecord recorder = null;
    //录音源
    //private static int audioSource = MediaRecorder.AudioSource.MIC;
    private static int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;

    //录音的采样频率
    private static int audioRate = 44100;
    //录音的声道，单声道
    //private static int audioChannel = AudioFormat.CHANNEL_IN_MONO;
    private static int audioChannel = AudioFormat.CHANNEL_IN_STEREO;
    //量化的深度
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //private static int audioFormat = AudioFormat.ENCODING_AAC_LC;
    //缓存的大小   缓存的大小会影响  record 。read 多久触发一次（暂时的理解） ，
    //private static int bufferSize = AudioRecord.getMinBufferSize(audioRate, audioChannel, audioFormat);
    private static int bufferSize = AudioRecord.getMinBufferSize(audioRate, audioChannel, audioFormat);

    //数字信号数组
    private byte[] noteArray;
    //PCM文件
    protected File pcmFile;
    //WAV文件
    protected File wavFile;
    //文件输出流
    private OutputStream os;

    //拼接wav的工具类
    private WavMergeUtil wavMergeUtil = new WavMergeUtil();


    private final String TAG   = "WavRecord";

    public WavRecord(){

    }



    @Override
    protected File startRecordCreateFile() throws IOException {

        createFile();//创建文件
        if (recorder != null) {
            Log.d(TAG," recorder is not null");
            recorder.stop();
            recorder.release();
        }

        recorder = new AudioRecord(audioSource, audioRate, audioChannel, audioFormat, bufferSize);


        recorder.startRecording();


        // 开启音频文件写入线程
        recordData();
        Log.d(TAG,"wav文件 名称 ："+wavFile.getName());
        return  wavFile;
    }

    @Override
    protected void stop() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            pcmFile.delete();
            pcmFile=null;
        }
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

    @Override
    public boolean mergeFile() {
       new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                File file = MediaDirectoryUtils.getTempCacheWavFileName();
                Log.d(TAG,"最终文件 名称 ："+file.getAbsolutePath());
                setmFile(file);
                try {

                    wavMergeUtil.mergeWav(getmData(), file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG," wav生成成功");

                afterMergeFile();

            }
        }, 100);

        return false;
    }

    @Override
    public Object getInternAudioRecord() {
        return recorder;
    }


    //创建文件夹,首先创建目录，然后创建对应的文件
    protected   File createFile() {


        File tempCachePcmFileName = MediaDirectoryUtils.getTempCachePcmFileName();
        if (!tempCachePcmFileName.getParentFile().exists()) {
            tempCachePcmFileName.getParentFile().mkdirs();//
        }
        pcmFile = tempCachePcmFileName;
        wavFile = MediaDirectoryUtils.getTempCacheWavFileName();
        if (pcmFile.exists()) {
            pcmFile.delete();
        }
        if (wavFile.exists()) {
            wavFile.delete();
        }
        try {
            pcmFile.createNewFile();
            wavFile.createNewFile();
        } catch (IOException e) {

        }
        return wavFile;
    }



    //记录数据
    public void recordData() {

        new Thread(new WriteThread()).start();
    }





    public int getCurrenttime() {
        return currenttime;
    }

    //读取录音数字数据线程
    class WriteThread implements Runnable {
        public void run() {
            writeData();
            convertWaveFile();
        }
    }




    //将数据写入文件夹,文件的写入没有做优化
    private void writeData() {
        noteArray = new byte[bufferSize];
        //建立文件输出流
        try {
            os = new BufferedOutputStream(new FileOutputStream(pcmFile));
        } catch (IOException e){

        }
        while (isRecording == true) {

            int recordSize = recorder.read(noteArray, 0, bufferSize);



            //这个可以用来做音量的变化  但是很不精确，buffersize 会影响record.read的频率  ，如果要控制 获取volume的周期，可以通过改变buffersize ,
            //但是不知道有没有别的影响
            //大概范围时25~38之间
            {
                long v = 0;
                // 将 buffer 内容取出，进行平方和运算
                for (int i = 0; i < recordSize; i++) {
                    v += noteArray[i] * noteArray[i];

                }
                //平方和除以数据总长度，得到音量大小。
                double mean = v / (double) recordSize;
                double volume = 10 * Math.log10(mean);
                setVolume((int) volume);
            }



            if (recordSize > 0) {
                try {
                    os.write(noteArray);
                } catch (IOException e) {

                }
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {

            }
        }
    }

    @Override
    protected int getVolume() {
        //大概范围时25~38之间
        if(super.getVolume()>=37){
            return 11;
        }else if(super.getVolume()<37&&super.getVolume()>=28){
            return super.getVolume()-26;
        }else if(super.getVolume()>=26){
            return 1;
        }else if(super.getVolume()>=25){
            return 0;
        }else {
            return 0;
        }

    }

    // 这里得到可播放的音频文件
    private void convertWaveFile() {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = audioRate;
        //int channels = 1; //单声道
        int channels = audioChannel == AudioFormat.CHANNEL_IN_STEREO?2:1;   //立体声

        long byteRate = 16 * audioRate * channels / 8;
        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream(pcmFile);
            out = new FileOutputStream(wavFile);
            totalAudioLen = in.getChannel().size();
            //由于不包括RIFF和WAV
            totalDataLen = totalAudioLen + 36;


            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk， FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的， */
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                     int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (1 * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

}
