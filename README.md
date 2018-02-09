 MyRecordActivity
一个录音demo

能够实现wav  MP3 amr 三种格式的录音 ，暂停录音，继续录音，以及播放。

主要参考：
https://github.com/qssq/recordutil

录音使用方式：

通过RecordingManagerI 这个抽象类来完成录音操作。
RecordingManagerI 的一下方法来完成整个录音
{
    //开始
    public  void startRecord()
    //继续
    public  void continueRecord()
    //暂停
    public void pauseRecord()
    //结束
    public void overRecord()
}

录音当中的各个过程的回调 通过OnRecordingCallback接口来回调
OnRecordingCallback
{
            //一次完成录音的结束
            @Override
            public void onOver(File file) {
            }
            //一次录音的开始
            @Override
            public void onStart() {
            }
            //暂停录音
            @Override
            public void onPause() {
            }
            //继续录音
            @Override
            public void onResume() {
            }
            //就是一个S定时器   其参数 duration代表当前录音总时长
            @Override
            public void onSecondChnage(int duration) {
            }
            //就是一个500ms定时器  其参数 volumeGrade 代表音量登记从 0~10  数值越大声音越大（很粗略）
            @Override
           public void onVolumeChange(int volumeGrade) {           
            }
}

实际用法：
 RecordingManagerI recordingManager = RecordFactory.getRecordManagerInstance(WavRecord.class);
 //recordingManager = RecordFactory.getRecordManagerInstance(MP3Record.class);
 //recordingManager = RecordFactory.getRecordManagerInstance(AmrRecord.class);
 recordingManager.setRecordingOverCallback(callback);
 
 
 播放的使用方法：
 使用MediaPlayer 来完成音频的播放
 
 MediaPlayer
 {
     /**
     * 开始播
     *
     * @param filePath 音频存放文件夹
     */
    public void startPlay(String filePath)
    /**
     * 继续或暂停播放
     */
    public void continueOrPausePlay()
    /**
     * 停止播放
     */
    public void stopPlay()
  
 }
 
 另外，需要传入一些callback 以便对播放过程进行监控
   /**
     * 播放SeekBar监听
     *
     * @param seekBar
     */
    public void setSeekBarListener(SeekBar seekBar)
    
     /**
     * 播放监听
     *
     * @param callBack
     */
    public void setVoicePlayListener(VoicePlayCallBack callBack)
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
 
  获取MediaPlayer的实例的方法：
  MediaPlayer mediaManager = MediaPlayer.getInstance(this);
 
 
 
 

  
  
  
  
  
 
 
 
 


