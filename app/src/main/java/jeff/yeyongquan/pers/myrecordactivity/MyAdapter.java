package jeff.yeyongquan.pers.myrecordactivity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import jeff.yeyongquan.pers.myrecordactivity.MediaPlay.VoiceTimeUtils;


/**
 * Created by Yqquan on 2018/1/18.
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    ArrayList<File>  mList  ;
    int   clickNum = -1;
    Handler mHandler ;
    MainActivity context;

    Boolean enableClickItem = true ;

    public int getClickNum() {
        return clickNum;
    }

    public MyAdapter(ArrayList  list, Handler handler,MainActivity context,boolean enableClick) {
        mList = list;
        mHandler = handler;
        enableClickItem = enableClick;
        this.context = context;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout,parent,false);

        ViewHolder vh = new ViewHolder(view,mHandler/*,parent.getContext()*/,enableClickItem);
        return vh;
    }



    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = mList.get(position);
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            holder.tv.setText(file.getName()+" size :"+stream.available());
        } catch (IOException e) {
            e.printStackTrace();
        }


        MediaPlayer media =  new MediaPlayer();
        try {
            if(!file.exists()) {
                return;
            }

            FileInputStream fls = new FileInputStream(file);
            media.setDataSource(fls.getFD());
            //media.setDataSource(file.getAbsolutePath());

            media.setAudioStreamType(AudioManager.STREAM_MUSIC);
            media.prepare();
            media.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.stop();
                    mp.release();
                    return false;
                }
            });

            media.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){

                @Override
                public void onCompletion(MediaPlayer arg0) {
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
        holder.tv_duration.setText(voiceLength);



    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

      class ViewHolder extends  RecyclerView.ViewHolder     implements View.OnClickListener{
        private static final String TAG ="YYQ-->>" ;

        //VoiceManager manager ;

        TextView  tv ;
        TextView  tv_duration;
        Handler mHandler;

        public ViewHolder(View itemView,Handler handler/*,Context  context*/,boolean enable) {
            super(itemView);
            tv  = itemView.findViewById(R.id.item_text);
            tv_duration = itemView.findViewById(R.id.item_duration);
            if(enable) {
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Log.d(TAG," onLongClick   position :"+ getPosition());
                        Message msg = mHandler.obtainMessage(MainActivity.MSG_MUSIC_DOCUMENT_INFO);
                        msg.arg1  = getPosition();
                        mHandler.sendMessage(msg);
                        return true;
                    }
                });
            }
            mHandler = handler;

            //manager = VoiceManager.getInstance(context);


        }

        @Override
        public void onClick(View v) {

                Log.d(TAG," click num :"+this.getPosition());
                //manager.startPlay(mFile.getAbsolutePath());

                Message msg = mHandler.obtainMessage(MainActivity.MSG_START_PLAY);
               msg.arg1  = this.getPosition();
                mHandler.sendMessage(msg);
                //File file = mList.get(this.getPosition());
                //context.startPlayMusic(this.getPosition());


        }
    }
}
