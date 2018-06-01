package airfrance.maxworth.com.airfrancenearbypoc;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;



/**
 * Created by mars on 13/03/18.
 */

public class App extends Application {


    public static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }



    public static String getVerName(){
        try{
            PackageManager manager = app.getPackageManager();
            PackageInfo info = manager.getPackageInfo(app.getPackageName(), 0);
            return info.versionName;
        }catch (Exception e){

        }
        return "";
    }


}
