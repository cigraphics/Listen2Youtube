package com.listen2youtube;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by khang on 25/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class Utils {
    private static final String TAG = "Utils";
    public interface OnCompleteListener {
        void onComplete(Object response);
    }

    public static boolean isOnline(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null)
            return false;
        return ni.isConnected();
    }

    public static void getLinkAsync(final String videoID, @NonNull final OnCompleteListener onCompleteListener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                try {
                    // Test google video host
                    URL url = new URL(Constants.SERVER + "?id=" + videoID + "&type=redirect");
                    HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
                    ucon.setInstanceFollowRedirects(true);
                    Log.e(TAG, "getLinkAsync - line 36: ucon.getResponseCode() " + ucon.getResponseCode());
                    if (ucon.getResponseCode() / 100 == 2)
                        result = Constants.SERVER + "?id=" + videoID + "&type=redirect";
                    else
                        result = Constants.SERVER + "?id=" + videoID;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onCompleteListener.onComplete(result);
            }
        }).start();

    }

    public static String getValidFileName(String fileName) {
        String newFileName = fileName.replaceAll("^[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");
        if(newFileName.length()==0)
            throw new IllegalStateException(
                    "File Name " + fileName + " results in a empty fileName!");
        return newFileName;
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        Log.e(TAG, "deleteRecursive - line 67: delete " + fileOrDirectory.getName());
        fileOrDirectory.delete();
    }
}
