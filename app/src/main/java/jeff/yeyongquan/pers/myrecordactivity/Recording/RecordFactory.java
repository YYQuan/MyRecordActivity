package jeff.yeyongquan.pers.myrecordactivity.Recording;

/**
 * Created by Yqquan on 2018/1/29.
 */

public class RecordFactory {

    private static RecordingManagerI  manager = null;
    private RecordFactory(){

    }

    public  static <T extends RecordingManagerI>T   getRecordManagerInstance(Class<T> clz){

       String classname = clz.getName();


        try{
           manager = (RecordingManagerI) Class.forName(classname).newInstance();
       } catch (IllegalAccessException e) {
           e.printStackTrace();
       } catch (InstantiationException e) {
           e.printStackTrace();
       } catch (ClassNotFoundException e) {
           e.printStackTrace();
       }


        return (T) manager;
    }


}
