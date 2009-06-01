package com.timsu.astrid.utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.activities.LocaleEditAlerts;
import com.timsu.astrid.activities.TagListSubActivity;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;

/**
 * Receiver is activated when Locale conditions are triggered
 *
 * @author timsu
 *
 */
public class LocaleReceiver extends BroadcastReceiver {

    /** minimum amount of time between two notifications */
    private static final long MIN_NOTIFY_INTERVAL = 8*3600*1000L;

    /** hash map of times we've notified */
    private static final HashMap<Long, Long> notifyTimes =
        new HashMap<Long, Long>();

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
    	try {
	    	if (LocaleEditAlerts.ACTION_LOCALE_ALERT.equals(intent.getAction())) {
				final long tagId = intent.getLongExtra(LocaleEditAlerts.KEY_TAG_ID, 0);
				final String tagName = intent.getStringExtra(LocaleEditAlerts.KEY_TAG_NAME);
				if(tagId == 0) {
					Log.w("astrid-locale", "Invalid tag identifier in alert");
					return;
				}

				// check if we've already made a notification recently
				if(notifyTimes.containsKey(tagId)) {
				    if(System.currentTimeMillis() - notifyTimes.get(tagId) <
				            MIN_NOTIFY_INTERVAL)
				        return;
				}

				// find out if we have active tasks with this tag
				TaskController taskController = new TaskController(context);
				taskController.open();
				TagController tagController = new TagController(context);
				tagController.open();
				try {
					HashSet<TaskIdentifier> activeTasks = taskController.getActiveTaskIdentifiers();
					LinkedList<TaskIdentifier> tasks = tagController.getTaggedTasks(
							new TagIdentifier(tagId));
					int count = TagListSubActivity.countActiveTasks(activeTasks, tasks);
					if(count > 0) {
						Resources r = context.getResources();
						String reminder = r.getString(R.string.notif_tagNotification,
								r.getQuantityString(R.plurals.Ntasks, count, count), tagName);
						Notifications.showTagNotification(context, tagId, reminder);
						notifyTimes.put(tagId, System.currentTimeMillis());
					}
				} finally {
					taskController.close();
					tagController.close();
				}
	    	}
    	} catch (Exception e) {
    		Log.e("astrid-locale-rx", "Error receiving intent", e);
    	}
    }

}
