package cool.dao.leme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("dao_leme", Context.MODE_PRIVATE);
        ReminderReceiver.updateSchedule(context, prefs.getBoolean("reminder_enabled", false), prefs.getString("reminder_time", "23:45"));
    }
}
