package jeff.yeyongquan.pers.myrecordactivity.Recording;


import android.app.Activity;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SimpleTimeZone;

import jeff.yeyongquan.pers.myrecordactivity.Recording.utils.MediaDirectoryUtils;


/**
 * Created by Yqquan on 2018/1/29.
 */

public class AmrRecord extends RecordingManagerI {


    private static final String  TAG = "ArmRecord";
    private MediaRecorder mMediaRecorder = null;

    //用于暂存一段录音文件
    protected File recFile = null;


    @Override
    protected File startRecordCreateFile() throws IOException {
        Log.d(TAG,"startRecordCreateFile");
        mMediaRecorder = new MediaRecorder();
        recFile = prepareRecorder(mMediaRecorder, true);
        new ObtainDecibelThread(this).start();

        return recFile;
    }

    @Override
    protected void stop() {
        Log.d(TAG,"stop");
        if ( mMediaRecorder!= null) {
            stopRecorder(mMediaRecorder, true);
            mMediaRecorder = null;


        }

    }

    @Override
    protected void pause() {
        Log.d(TAG,"pause");
        //stopRecorder(mMediaRecorder, true);
        //mMediaRecorder = null;
        stopRecord();
    }

    @Override
    protected void conTinue() {
        Log.d(TAG,"conTinue");
        //startVoiceRecord();
        try {
            startRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean mergeFile() {
        Log.d(TAG,"mergeFile");
        setmFile(getOutputVoiceFile(getmData()));
        afterMergeFile();
        return false;
    }

    @Override
    public Object getInternAudioRecord() {
        return mMediaRecorder;
    }



    /**
     * 录音准备工作 ，开始录音
     *
     * @param mr
     * @param start
     * @return
     */
    private File prepareRecorder(MediaRecorder mr, boolean start) {

        if (mr == null) return null;
        File file = null;
        try {

            file = MediaDirectoryUtils.getTempAmrFileName();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mr.setOutputFile(file.getAbsolutePath());
            mr.prepare();
            if (start) {
                mr.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }


    /**
     * 最后停止录音
     *
     * @param mr
     * @param release
     * @return
     */
    private boolean stopRecorder(MediaRecorder mr, boolean release) {
        boolean result = false;
        try {
            if (mr != null) {
                mr.stop();
                if (release) {
                    mr.release();
                }
                result = true;
            }

        } catch (Exception e) {

        }
        return result;
    }


    /**
     * 开始录音(内部调)
     *
     *
     */
    private void startVoiceRecord() {

        mMediaRecorder = new MediaRecorder();
        prepareRecorder(mMediaRecorder, true);

    }

    /**
     * 合并录音
     *
     * @param list
     * @return
     */
    private File getOutputVoiceFile(ArrayList<File> list) {



        // 创建音频文件,合并的文件放这里
        File resFile = MediaDirectoryUtils.getTempAmrFileName();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(resFile);
        } catch (IOException e) {
        }
        // list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
        for (int i = 0; i < list.size(); i++) {
            File file = list.get(i);
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] myByte = new byte[fileInputStream.available()];
                // 文件长度
                int length = myByte.length;
                // 头文件
                if (i == 0) {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 0, length);
                    }
                }
                // 之后的文件，去掉头文件就可以了
                else {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 6, length - 6);
                    }
                }
                fileOutputStream.flush();
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 结束后关闭流
        try {
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resFile;
    }


    /**
     * 监听录音声音频率大小
     */
    private class ObtainDecibelThread extends Thread {


        private RecordingManagerI  mManager ;

        public ObtainDecibelThread(RecordingManagerI mManager) {
            this.mManager = mManager;
        }


        //计算出的范围大概是0-150
        @Override
        public void run() {
            while (mManager.isRecordIng()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mMediaRecorder == null ) {
                    break;
                }
                try {
                    final double ratio = mMediaRecorder.getMaxAmplitude()/150;
                    if (ratio != 0) {
                        setVolume((int) ratio);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }



            }
        }

    }

    @Override
    public int getVolume() {
        //计算出的范围大概是0-150
        Log.d(TAG,"volume grade is "+ super.getVolume()/15);
        return  super.getVolume()/15;


    }
}
