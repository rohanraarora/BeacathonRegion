package in.codingninjas.beacathonregion;

import android.app.Application;

/**
 * Created by rohanarora on 22/12/16.
 */

public class MyApp extends Application {

    private static MyApp instance = null;
    public static MyApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
