package com.listen2youtube;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by khang on 28/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class BroadcastDownloadComplete extends BroadcastReceiver {
    private static final String TAG = "DownloadComplete";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED")) {
            intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Bundle extras = intent.getExtras();
            Long downloaded_id = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloaded_id);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = manager.query(q);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                Log.e(TAG, "onReceive - line 27: status " + status + " reason  " + reason);
            }
            c.close();
        }
    }
}
