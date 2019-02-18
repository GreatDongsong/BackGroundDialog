package dawson.com.backgrounddialog;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.List;

public class BackGroundTaskManager {
    private int mJobId;
    private ComponentName componentName;
    private Context context;
    public static final String MESSENGER_INTENT_KEY = BuildConfig.APPLICATION_ID + ".MESSENGER_INTENT_KEY";
    public static final String WORK_DURATION_KEY = BuildConfig.APPLICATION_ID + ".WORK_DURATION_KEY";
    private Handler handler;

    private BackGroundTaskManager() {
    }

    public static BackGroundTaskManager getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        public static final BackGroundTaskManager instance = new BackGroundTaskManager();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void init(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        componentName = new ComponentName(context, MyJobService.class);
    }

    public void start() {
        // Start service and provide it a way to communicate with this class.
        Intent startServiceIntent = new Intent(context, MyJobService.class);
        Messenger messengerIncoming = new Messenger(handler);
        startServiceIntent.putExtra(MESSENGER_INTENT_KEY, messengerIncoming);
        context.startService(startServiceIntent);
    }

    public void stop() {
        // A service can be "started" and/or "bound". In this case, it's "started" by this Activity
        // and "bound" to the JobScheduler (also called "Scheduled" by the JobScheduler). This call
        // to stopService() won't prevent scheduled jobs to be processed. However, failing
        // to call stopService() would keep it alive indefinitely.
        context.stopService(new Intent(context, MyJobService.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void cancelAllJobs() {
        JobScheduler tm = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.cancelAll();
        Toast.makeText(context, R.string.all_jobs_cancelled, Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void scheduleJob(long minLatencyMillis,
                            long maxExecutionDelayMillis,
                            boolean requiresUnmetered,
                            boolean requiresAnyConnectivity,
                            boolean requiresIdleCheckboxChecked,
                            boolean requiresChargingCheckBoxChecked,
                            String workDuration) {
        JobInfo.Builder builder = new JobInfo.Builder(mJobId++, componentName);

        // Extras, work duration.
        PersistableBundle extras = new PersistableBundle();
        if (TextUtils.isEmpty(workDuration)) {
            workDuration = "1";
        }
        extras.putLong(WORK_DURATION_KEY, Long.valueOf(workDuration) * 1000);
        builder.setExtras(extras);

        // Schedule job
        Toast.makeText(context, "Scheduling job", Toast.LENGTH_SHORT).show();
        builder.setMinimumLatency(minLatencyMillis);
        builder.setOverrideDeadline(maxExecutionDelayMillis);
        if (requiresUnmetered) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        } else if (requiresAnyConnectivity) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        }
        builder.setRequiresDeviceIdle(requiresIdleCheckboxChecked);
        builder.setRequiresCharging(requiresChargingCheckBoxChecked);
        JobScheduler tm = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.schedule(builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void cancelFirstJob() {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = jobScheduler.getAllPendingJobs();
        if (allPendingJobs.size() > 0) {
            // Finish the last one
            int jobId = allPendingJobs.get(0).getId();
            jobScheduler.cancel(jobId);
            Toast.makeText(
                    context, String.format(context.getString(R.string.cancelled_job), jobId),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(
                    context, context.getString(R.string.no_jobs_to_cancel),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
