package com.example.lowpowerevaluation;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Main activity.
 */
public class TouchDistActivity extends Activity {
    private static final String TAG = "TouchDistActivity";

    // The user is forced to repeat the phrase if the number of entered
    // letters is different from the number of letters in the phrase by
    // CANCEL_DIFF_THRESHOLD * length_phrase.
    private static final float CANCEL_DIFF_THRESHOLD_NORMALIZED = 0.2f;

    private static final int INPUT_MODE_TYPING = 1;

    private static final int TRIALS_PER_WARM_UP_BLOCK = 4;
    public static final int TRIALS_PER_BLOCK_PHRASE = 8;
    public static final int REP_NUM_PHRASE = 1;

    public static final String INPUT_FINGER_INDEX = "i";
    public static final String INPUT_FINGER_ONE_THUMB = "t";
    public static final String INPUT_FINGER_TWO_THUMBS = "tt";

    public static final String DEFAULT_KEYBOARD = "default_keyboard";
    public static final String INVISIBLE_TAP = "invisible_tap";

    public static final String DEFAULT_SCREEN_FREQ = "screen_on";
    public static final String SCREEN_OFF = "screen_off";
    public static final String WORDWISE_SCREEN_FREQ = "screen_off_wordwise";
    public static final String SCREEN_OFF_ARBITRARY = "screen_off_arbitrary";


    static class DataSet {
        private static final int T_20 = 0;
        private static final int T_40 = 1;
        private static final int T_80 = 2;
        private static final int T_160 = 3;
        private static final int TASK_TYPE_UNKNOWN = -1;

        private final int mResourceId;
        private final int mTrialsPerBlock;
        private final int mRepetitionTimes;
        private final int mTaskType;

        DataSet(final int resourceId, final int trialsPerBlock, final int repetitionTimes) {
            mResourceId = resourceId;
            switch (mResourceId) {
                case R.raw.t_20:
                    mTaskType = T_20;
                    break;
                case R.raw.t_40:
                    mTaskType = T_40;
                    break;
                case R.raw.t_80:
                    mTaskType = T_80;
                    break;
                case R.raw.t_160:
                    mTaskType = T_160;
                    break;
                default:
                    mTaskType = TASK_TYPE_UNKNOWN;
                    break;
            }
            mTrialsPerBlock = trialsPerBlock;
            mRepetitionTimes = repetitionTimes;
        }

        int getResourceId() {
            return mResourceId;
        }

        int getTrialNumPerBlock() {
            return mTrialsPerBlock;
        }

        int getRepetitionTimes() {
            return mRepetitionTimes;
        }

        int getTaskType() {
            return mTaskType;
        }
    }

    private DataSet mDataSet;

    private int mUserID = 1;
    private boolean mIsGestureExpert = true;
    private int mInputMode;
    private boolean mIsDataSetLoaded;
    private String mKeyboardType;

    TextView mTaskInfoTextView = null;
    TextView mStimulusTextView = null;
    MockEditText mInputEditText = null;

    private String mStimulus = null;

    private final StimulusGenerator mStimulusGenerator = new StimulusGenerator();

    private int mTrialNum = 0;
    private boolean mIsStimulusCancelled = false;
    private boolean mIsForcedCancel = false;

    // t-thumb, tt-two thumbs, i-index finger
    private String mInputFinger = "t";

    // variables for sensors
    private static final int BUFSIZE = 1024 * 1000;
    private StringBuffer mScreenSb = new StringBuffer(BUFSIZE);

    private StringBuffer mScreenDataLog = new StringBuffer(BUFSIZE);
    private StringBuffer mTouchPointsDataLog = new StringBuffer();
    private HandlerThread mHandlerThread;
    private DataLogger mTouchPointsLogger;
    private DataLogger mScreenStatusLogger;

    // variables for private command listener in EditText
    private TaskActionListener mListener;
    private static boolean mLogHeaderPrinted = false;
    private static boolean mScreenHeaderPrinted = false;
    private TextSpeaker mSpeaker;
    private boolean isScreenOn;
    private String mScreenSetting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, 1);
        }
        boolean permitted = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED);
        Log.d(TAG, "The permission is " + permitted);

        //setNavigationInvisible();
        //updateUI();
        registerScreenStatusBroadcastReceiver();
        isScreenOn = true;
        mLogHeaderPrinted = false;
        mScreenHeaderPrinted = false;

        mTaskInfoTextView = (TextView) findViewById(R.id.taskInfo);
        mStimulusTextView = (TextView) findViewById(R.id.stimulus);
        mInputEditText = (MockEditText) findViewById(R.id.inputEditText);
        mListener = mInputEditText;
        final Button nextButton = (Button) findViewById(R.id.nextButton);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDataSet = createDataSet(extras.getInt(StartMenu.DATASET));
            mUserID = extras.getInt(StartMenu.USERID);
            mKeyboardType = extras.getString(StartMenu.KEYBOARDTYPE);
            mInputFinger = extras.getString(StartMenu.INPUTFINGER);
            mIsGestureExpert = extras.getBoolean(StartMenu.ISGESTUREEXPERT);
            final boolean isGestureInput = extras.getBoolean(StartMenu.ISGESTUREINPUT);
            mScreenSetting = extras.getString(StartMenu.SCREENFREQ);

            mInputMode = INPUT_MODE_TYPING;
        }

        mTaskInfoTextView.setVisibility(View.VISIBLE);
        mStimulusTextView.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);

        mSpeaker = new TextSpeaker(this);
        mInputEditText.addTextChangedListener(new TextWatcher() {
            String prev = "";

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                //Log.i(TAG, "beforeTextChanged " + charSequence.toString() + " start: "+ start
                //        + " count:" + count + " before:" + after);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String txt = charSequence.toString();
                int prev_len = prev.length();
                int cur_len = txt.length();
                // While deleting case
                if (cur_len < prev_len) {
                    prev = txt;

                } else if (cur_len > 0 && txt.charAt(cur_len - 1) == ' ') {
                    // While committing a word
                    String word = txt.substring(prev_len);
                    mSpeaker.say(word);
                    Log.i(TAG, "Say with: " + word);
                    prev = txt;
                    if (mScreenSetting.equals(WORDWISE_SCREEN_FREQ)) {
                        if (!isScreenOn) {
                            turnOn();
                        }
                    }
                } else if (cur_len > 0) {
                    if (mScreenSetting.equals(WORDWISE_SCREEN_FREQ)) {
                        if (isScreenOn) {
                            turnOff();
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // force focus on the EditText
        mInputEditText.requestFocus();
        // always show ime
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputString = mInputEditText.getText().toString().trim();
                if (Math.abs(inputString.length() - mStimulus.length()) * 1.0
                        / mStimulus.length()
                        > CANCEL_DIFF_THRESHOLD_NORMALIZED) {
                    mIsForcedCancel = true;
                    mIsStimulusCancelled = true;
                }
                saveTouchPointsToBuffer(
                        mListener.onTaskFinished(mIsStimulusCancelled),
                        mIsStimulusCancelled,
                        SystemClock.uptimeMillis());
                saveScreenStatusToBuffer(SystemClock.uptimeMillis());

                saveTouchPointsBufferToFile();
                saveScreenDataToFile();

                mTrialNum ++;
                if (mIsStimulusCancelled) {
                    mTrialNum --;
                }
                showStimulus(mTrialNum);
            }
        });

        mHandlerThread =
                new HandlerThread("logging thread", Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        Handler mHandler = new Handler(mHandlerThread.getLooper());
        mTouchPointsLogger = new DataLogger(
                mHandler, getTouchLoggerFileName(mInputMode, mUserID),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!--" +
                        getString(R.string.touch_points_logger_file_head) +
                        "-->\n");
        mScreenStatusLogger = new DataLogger(
                mHandler, getScreenLoggerFileName(mInputMode, mUserID),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        loadStimulus();
    }


    /**
     * Turns the screen off and locks the device, provided that proper rights
     * are given.
     *
     */
    private void turnOff() {
        Log.i(TAG, "Turning off the screen ");

        DevicePolicyManager policyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(this,  AdminReceiver.class);
        boolean admin = policyManager.isAdminActive(adminReceiver);
        if (admin) {
            Log.i(TAG, "Going to sleep now.");
            policyManager.lockNow();
        } else {
            Log.i(TAG, "Not an admin");
            Toast.makeText(getApplicationContext(), R.string.device_admin_not_enabled,
                    Toast.LENGTH_LONG).show();
        }
        isScreenOn = false;
    }


    private void turnOn() {
        Log.i(TAG, "Turning on the screen ");
        PowerManager powerManager = ((PowerManager) getSystemService(Context.POWER_SERVICE));
        PowerManager.WakeLock wake =
                powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        wake.acquire();
        isScreenOn = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void setNavigationInvisible() {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // TODO: The system bars are visible. Make any desired
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.
                            // Hide both the navigation bar and the status bar.
                            // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
                            // a general rule, you should design your app to hide the status bar whenever you
                            // hide the navigation bar.
                            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
                            decorView.setSystemUiVisibility(uiOptions);

                        } else {
                            // TODO: The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                        }
                    }
                });
    }
    public void updateUI() {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });
    }

    private void registerScreenStatusBroadcastReceiver() {

        final IntentFilter theFilter = new IntentFilter();
        /** System Defined Broadcast */
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
        theFilter.addAction(Intent.ACTION_USER_PRESENT);

        BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (!strAction.equals(Intent.ACTION_SCREEN_OFF) && !strAction.equals(Intent.ACTION_SCREEN_ON))
                    return;
                if(strAction.equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.i(TAG,"NOW Screen " + "LOCKED");
                    isScreenOn = false;
                } else if (strAction.equals(Intent.ACTION_SCREEN_ON)) {
                    Log.i(TAG,"NOW Screen " + "UNLOCKED");
                    isScreenOn = true;
                }

                mScreenSb.append("\t<isScreenOn>")
                        .append(isScreenOn ? 1 : 0)
                        .append("</isScreenOn>\n")
                        .append("\t<time>")
                        .append(SystemClock.uptimeMillis())
                        .append("</time>\n");
            }
        };

        getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
    }

    /**
     * Returns the consumer friendly device name.
     */
    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    private static DataSet createDataSet(final int resId) {
        final int trialsPerBlock, repNum;
        switch (resId) {
            case R.raw.t_20:
            case R.raw.t_40:
            case R.raw.t_80:
            case R.raw.t_160:
                trialsPerBlock = TRIALS_PER_BLOCK_PHRASE;
                repNum = REP_NUM_PHRASE;
                break;
            default:
                trialsPerBlock = TRIALS_PER_BLOCK_PHRASE;
                repNum = REP_NUM_PHRASE;
        }
        return new DataSet(resId, trialsPerBlock, repNum);
    }

    private void loadStimulus() {
        mIsDataSetLoaded = false;
        Log.i(TAG, "Loading...loadStimulus..");
        OnTaskComplete onTaskComplete = new OnTaskComplete() {
            @Override
            public void onComplete(Bundle params) {
                if (params.getBoolean(KEY_RESULT)) {
                    Log.i(TAG, "Loading...data ");
                    showStimulus(mTrialNum);
                    mIsDataSetLoaded = true;
                } else {
                    Log.e(TAG, "loading dataset fails");
                }
            }
        };
        mStimulusGenerator.load(this, mDataSet, onTaskComplete);
    }

    /**
     * Display the next word or flip the repetition counter on the current word.
     */
    public void showStimulus(final int trialNum) {
        boolean startNewBlock = false;

        final int totalBlock = mStimulusGenerator.getBlockNum();
        final int trialPerBlock = mDataSet.getTrialNumPerBlock();

        final int trialInBlock, blockNum;
        if (trialNum < TRIALS_PER_WARM_UP_BLOCK) {
            if (trialNum % TRIALS_PER_WARM_UP_BLOCK == 0) {
                startNewBlock = true;
            }
            trialInBlock = trialNum;
            blockNum = 0;
        } else {
            trialInBlock = (trialNum - TRIALS_PER_WARM_UP_BLOCK) % trialPerBlock;
            blockNum = (trialNum - TRIALS_PER_WARM_UP_BLOCK) / trialPerBlock + 1;
        }
        if (trialNum == 0 ||
                (trialNum - TRIALS_PER_WARM_UP_BLOCK)  % trialPerBlock == 0) {
            startNewBlock = true;
        }

//        if (trialNum % trialPerBlock == 0) {
//            startNewBlock = true;
//        }

        final Resources res = getResources();
        if (mIsStimulusCancelled && mIsForcedCancel) {
            showDialog(res.getString(R.string.instructions_forced_cancel),
                    false /*clickOKToFinishTask*/);
        } else if (mIsStimulusCancelled) { // && !mIsForcedCancel
            showDialog(res.getString(R.string.instructions_button_cancel),
                    false /*clickOKToFinishTask*/);
        }
        else if (startNewBlock) {
            if (blockNum == 0) {
                final String instructions;
                if (mInputMode == INPUT_MODE_TYPING) {
                    instructions = res.getString(R.string.instruction_typing);
                } else {
                    instructions = res.getString(R.string.instruction_gesturing);
                }
                Log.d(TAG, "start. block_num=" + blockNum);
                showDialog(instructions, false /*clickOKToFinishTask*/);
            } else if (blockNum == 1) {
                Log.d(TAG, "Warm-up complete, block_num=" + blockNum);
                showDialog("Warm-up Complete, Block " + blockNum,
                        false /*clickOKToFinishTask*/);
            } else if (blockNum == totalBlock) {
                Log.d(TAG, "done, block_num=" + blockNum);
                mTouchPointsLogger.logString("</root>");
                mScreenStatusLogger.logString("</root>");
                showDialog("You've completed the session!", true /*clickOKToFinishTask*/);
                new BroadcastTask().execute(TCPTags.PHRASE_TEXT_KEY);
                return;
            } else {
                Log.d(TAG, "new block, block_num=" + blockNum);
                showDialog("Block " + blockNum, false /*clickOKToFinishTask*/);
            }
        }

        StringBuilder tvCountBuilder = new StringBuilder();
        if (trialNum < TRIALS_PER_WARM_UP_BLOCK) {
            tvCountBuilder.append(TRIALS_PER_WARM_UP_BLOCK - trialInBlock)
                    .append("/")
                    .append(TRIALS_PER_WARM_UP_BLOCK)
                    .append(" more phrases in this block.");
        } else {
            tvCountBuilder.append(trialPerBlock - trialInBlock)
                    .append("/")
                    .append(trialPerBlock)
                    .append(" more phrases in this block.");
        }

        mTaskInfoTextView.setText(tvCountBuilder);
        mInputEditText.setText("");

        // Show a new phrase/word
        mStimulus = mStimulusGenerator.getPhrase(blockNum, trialInBlock);
        mStimulusTextView.setText(mStimulus);
        // Send phrase content to PC.
        new BroadcastTask().execute(TCPTags.PHRASE_TEXT_KEY, mStimulus);

        // reset
        mIsStimulusCancelled = false;
        mIsForcedCancel = false;
    }

    private static String getInputModeString(final int inputMode) {
        switch (inputMode) {
            case INPUT_MODE_TYPING:
            default:
                return "Tap Input";
        }
    }

    private static String getInputFingerFullString(final String inputFinger) {
        if (inputFinger.equalsIgnoreCase("t")) {
            return "thumb";
        } else if (inputFinger.equalsIgnoreCase("i")) {
            return "index finger";
        } else if (inputFinger.equalsIgnoreCase("tt")) {
            return "two thumbs";
        }
        return "unknown finger";
    }

    private static String getKeyboardTypeString(final String keyboardType) {
        switch (keyboardType) {
            case INVISIBLE_TAP:
                return "invisible tap keyboard";
            default:
                return "default keyboard";
        }
    }

    private static String getFormattedDate() {
        final Date curDate = new Date();
        final SimpleDateFormat format =
                new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(curDate);
    }

    private static String getTouchLoggerFileName(final int inputMode,
                                                 final int userId) {
        return "user_" + userId + "Touch_PointsLogger" + getFormattedDate() + ".xml";
    }

    private static String getScreenLoggerFileName(final int inputMode,
                                                 final int userId) {
        return "user_" + userId + "Screen_Logger" + getFormattedDate() + ".xml";
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mTouchPointsLogger.closeLogFile();
        mScreenStatusLogger.closeLogFile();
        mHandlerThread.quit();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void saveTouchPointsToBuffer(StringBuffer touchPointInfo,
                                         final boolean isPhraseCanceled,
                                         final long upTime) {
        mTouchPointsDataLog.append(getPointLogDataPerTrialPrefix())
                .append(touchPointInfo)
                .append("\t<isCanceled>")
                .append(isPhraseCanceled ? 1 : 0)
                .append("</isCanceled>\n")
                .append("\t<editTextContent>")
                .append(mInputEditText.getText())
                .append("</editTextContent>\n")
                .append("\t<submitTime>")
                .append(upTime)
                .append("</submitTime>\n")
                .append("</trial>\n");
    }

    private void saveScreenStatusToBuffer(final long upTime) {
        mScreenDataLog.append(getScreenDataPerTrialPrefix())
                .append(mScreenSb)
                .append("\t<submitTime>")
                .append(upTime)
                .append("</submitTime>\n")
                .append("</trial>\n");
        mScreenSb.setLength(0);
    }

    private void saveTouchPointsBufferToFile() {
        StringBuffer dataLog;
        // swap buffers so new data goes to a clean buffer while we write out the existing one.
        synchronized (this) {
            dataLog = mTouchPointsDataLog;
            mTouchPointsDataLog = new StringBuffer(BUFSIZE);
        }
        mTouchPointsLogger.logString(dataLog.toString());
    }

    private void saveScreenDataToFile() {
        StringBuffer dataLog;
        synchronized (this) {
            dataLog = mScreenDataLog;
            mScreenDataLog = new StringBuffer(BUFSIZE);
        }
        mScreenStatusLogger.logString(dataLog.toString());
    }

    private String getPointLogDataPrefix() {
        return "<root>\n" +
                "<user>" +
                mUserID +
                "</user>\n" +
                "<inputFinger>" +
                getInputFingerFullString(mInputFinger) +
                "</inputFinger>\n" +
                "<keyboardType>" +
                getKeyboardTypeString(mKeyboardType) +
                "</keyboardType>\n" +
                "<taskType>" +
                mDataSet.getTaskType() +
                "</taskType>\n" +
                "<gestureExpert>" +
                (mIsGestureExpert ? 1 : 0) +
                "</gestureExpert>\n" +
                "<device>" +
                getDeviceName() +
                "</device>\n" +
                "<inputMode>" +
                getInputModeString(mInputMode) +
                "</inputMode>\n" +
                "<screenOption>" +
                mScreenSetting +
                "</screenOption>\n";

    }

    private String getScreenDataPerTrialPrefix() {
        final int blockNum;
        if (mTrialNum < TRIALS_PER_WARM_UP_BLOCK) {
            blockNum = 0;
        } else {
            blockNum = (mTrialNum - TRIALS_PER_WARM_UP_BLOCK) / mDataSet.getTrialNumPerBlock() + 1;
        }

        StringBuilder sb = new StringBuilder();
        // save user info to touchPointsLogger
        if (!mScreenHeaderPrinted) {
            sb.append(getPointLogDataPrefix());
            mScreenHeaderPrinted = true;
        }
        sb.append("<trial>\n")
                .append("\t<blockNum>")
                .append(blockNum)
                .append("</blockNum>\n")
                .append("\t<trialNum>")
                .append(mTrialNum)
                .append("</trialNum>\n")
                .append("\t<stimulus>")
                .append(mStimulus)
                .append("</stimulus>\n");
        return sb.toString();
    }

    private String getPointLogDataPerTrialPrefix() {
        final int blockNum = mTrialNum / mDataSet.getTrialNumPerBlock();
        StringBuilder sb = new StringBuilder();
        // save user info to touchPointsLogger
        if (!mLogHeaderPrinted) {
            sb.append(getPointLogDataPrefix());
            mLogHeaderPrinted = true;
        }
        sb.append("<trial>\n")
                .append("\t<blockNum>")
                .append(blockNum)
                .append("</blockNum>\n")
                .append("\t<trialNum>")
                .append(mTrialNum)
                .append("</trialNum>\n")
                .append("\t<stimulus>")
                .append(mStimulus)
                .append("</stimulus>\n");
        return sb.toString();
    }


    private void showDialog(final String msg, final boolean clickOKToFinishTask) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        MessageDialog newDialog = MessageDialog.newInstance(msg, clickOKToFinishTask);
        switch (mKeyboardType) {
            case INVISIBLE_TAP:
            default:
                newDialog.show(ft, "dialog");
                break;
        }
    }
}
