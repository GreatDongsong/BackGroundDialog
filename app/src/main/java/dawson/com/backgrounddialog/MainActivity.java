package dawson.com.backgrounddialog;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import static dawson.com.backgrounddialog.BackGroundTaskManager.WORK_DURATION_KEY;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int MSG_UNCOLOR_START = 0;
    public static final int MSG_UNCOLOR_STOP = 1;
    public static final int MSG_COLOR_START = 2;
    public static final int MSG_COLOR_STOP = 3;


    private EditText mDelayEditText;
    private EditText mDeadlineEditText;
    private EditText mDurationTimeEditText;
    private RadioButton mWiFiConnectivityRadioButton;
    private RadioButton mAnyConnectivityRadioButton;
    private CheckBox mRequiresChargingCheckBox;
    private CheckBox mRequiresIdleCheckbox;


    private int mJobId = 0;

    // Handler for incoming messages from the service.
    private IncomingMessageHandler mHandler;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);

        // Set up UI.
        mDelayEditText = findViewById(R.id.delay_time);
        mDurationTimeEditText = findViewById(R.id.duration_time);
        mDeadlineEditText = findViewById(R.id.deadline_time);
        mWiFiConnectivityRadioButton = findViewById(R.id.checkbox_unmetered);
        mAnyConnectivityRadioButton = findViewById(R.id.checkbox_any);
        mRequiresChargingCheckBox = findViewById(R.id.checkbox_charging);
        mRequiresIdleCheckbox = findViewById(R.id.checkbox_idle);

        mHandler = new IncomingMessageHandler(this);

        BackGroundTaskManager.getInstance().init(this, mHandler);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BackGroundTaskManager.getInstance().stop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        BackGroundTaskManager.getInstance().start();
    }

    /**
     * Executed when user clicks on SCHEDULE JOB.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void scheduleJob(View v) {
        String delay = mDelayEditText.getText().toString();
        long minLatencyMillis = 0;
        if (!TextUtils.isEmpty(delay)) {
            minLatencyMillis = Long.valueOf(delay) * 1000;
        }
        String deadline = mDeadlineEditText.getText().toString();
        long maxExecutionDelayMillis = 0;
        if (!TextUtils.isEmpty(deadline)) {
            maxExecutionDelayMillis = Long.valueOf(deadline) * 1000;
        }
        boolean requiresUnmetered = mWiFiConnectivityRadioButton.isChecked();
        boolean requiresAnyConnectivity = mAnyConnectivityRadioButton.isChecked();
        boolean requiresIdleCheckboxChecked = mRequiresIdleCheckbox.isChecked();
        boolean requiresChargingCheckBoxChecked = mRequiresChargingCheckBox.isChecked();

        String workDuration = mDurationTimeEditText.getText().toString();

        BackGroundTaskManager.getInstance().scheduleJob(minLatencyMillis,
                maxExecutionDelayMillis, requiresUnmetered, requiresAnyConnectivity,
                requiresIdleCheckboxChecked, requiresChargingCheckBoxChecked, workDuration);
    }

    /**
     * Executed when user clicks on CANCEL ALL.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void cancelAllJobs(View v) {
        BackGroundTaskManager.getInstance().cancelAllJobs();
    }

    /**
     * Executed when user clicks on FINISH LAST TASK.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void finishJob(View v) {
        BackGroundTaskManager.getInstance().cancelFirstJob();
    }

    /**
     * A {@link Handler} allows you to send messages associated with a thread. A {@link Messenger}
     * uses this handler to communicate from {@link MyJobService}. It's also used to make
     * the start and stop views blink for a short period of time.
     */
    public static class IncomingMessageHandler extends Handler {

        // Prevent possible leaks with a weak reference.
        private WeakReference<MainActivity> mActivity;

        IncomingMessageHandler(MainActivity activity) {
            super(/* default looper */);
            this.mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mActivity.get();
            if (mainActivity == null) {
                // Activity is no longer available, exit.
                return;
            }
            View showStartView = mainActivity.findViewById(R.id.onstart_textview);
            View showStopView = mainActivity.findViewById(R.id.onstop_textview);
            Message m;
            switch (msg.what) {
                /*
                 * Receives callback from the service when a job has landed
                 * on the app. Turns on indicator and sends a message to turn it off after
                 * a second.
                 */
                case MSG_COLOR_START:
                    // Start received, turn on the indicator and show text.
                    showStartView.setBackgroundColor(getColor(R.color.start_received));
                    updateParamsTextView(msg.obj, "started");

                    // Send message to turn it off after a second.
                    m = Message.obtain(this, MSG_UNCOLOR_START);
                    sendMessageDelayed(m, 1000L);
                    break;
                /*
                 * Receives callback from the service when a job that previously landed on the
                 * app must stop executing. Turns on indicator and sends a message to turn it
                 * off after two seconds.
                 */
                case MSG_COLOR_STOP:
                    // Stop received, turn on the indicator and show text.
                    showStopView.setBackgroundColor(getColor(R.color.stop_received));
                    updateParamsTextView(msg.obj, "stopped");

                    // Send message to turn it off after a second.
                    m = obtainMessage(MSG_UNCOLOR_STOP);
                    sendMessageDelayed(m, 2000L);
                    break;
                case MSG_UNCOLOR_START:
                    showStartView.setBackgroundColor(getColor(R.color.none_received));
                    updateParamsTextView(null, "");
                    break;
                case MSG_UNCOLOR_STOP:
                    showStopView.setBackgroundColor(getColor(R.color.none_received));
                    updateParamsTextView(null, "");
                    break;
            }
        }

        private void updateParamsTextView(@Nullable Object jobId, String action) {
            TextView paramsTextView = mActivity.get().findViewById(R.id.task_params);
            if (jobId == null) {
                paramsTextView.setText("");
                return;
            }
            String jobIdText = String.valueOf(jobId);
            paramsTextView.setText(String.format("Job ID %s %s", jobIdText, action));
        }

        private int getColor(@ColorRes int color) {
            return mActivity.get().getResources().getColor(color);
        }
    }
}

