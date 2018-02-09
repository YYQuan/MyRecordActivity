package jeff.yeyongquan.pers.myrecordactivity.Recording;

/**
 * Created by Yqquan on 2018/1/29.
 */

public interface RecordState {


    int RECORD_RESUME = 6;  //回复录音状态   这个状态就是在暂停状态下，点击了继续录音。
    int RECORD_PAUSE = 5;   //暂停状态
    int RECORD_PLAYING=4;   //正在播放
    int RECORD_OVER_STOP=3; //录制完成了
    int REACORDING=1;       //录音中

}
