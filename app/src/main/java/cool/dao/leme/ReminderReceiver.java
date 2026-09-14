package cool.dao.leme;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String PREFS = "dao_leme";
    private static final int REQUEST_CODE = 2345;
    private static final String CHANNEL_ID = "daily_record";

    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        boolean alreadyRecorded = today.equals(prefs.getString("recorded_date", "")) && prefs.getBoolean("recorded", false);
        if (!alreadyRecorded) showNotification(context);
        updateSchedule(context, prefs.getBoolean("reminder_enabled", false), prefs.getString("reminder_time", "23:45"));
    }

    static void updateSchedule(Context context, boolean enabled, String time) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("reminder_enabled", enabled).putString("reminder_time", time).apply();
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = pendingIntent(context);
        alarms.cancel(pending);
        if (!enabled) return;
        String[] parts = time.split(":");
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        next.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DATE, 1);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
    }

    private static PendingIntent pendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, REQUEST_CODE, new Intent(context, ReminderReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void showNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "每日记录提醒", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new android.app.Notification.Builder(context, CHANNEL_ID) : new android.app.Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("今日记录提醒").setContentText("点击完成今天的记录").setAutoCancel(true).setContentIntent(content);
        manager.notify(REQUEST_CODE, builder.build());
    }
}
