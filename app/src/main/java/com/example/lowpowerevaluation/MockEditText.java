package com.example.lowpowerevaluation;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

public class MockEditText extends EditText implements TaskActionListener{

    private static final String TAG = "MockEditText";

    private final List<SimpleEntry<String, Bundle>> bundleList =
            new LinkedList<>();

    private static final String TOUCH_POINTS_ACTION = "TOUCH_POINTS";
    private static final String GESTURE_ACTION = "GESTURE";
    private static final String POINT_COUNT_KEY = "POINT_COUNT";
    private static final String POINTER_IDS_KEY = "POINTER_IDS";
    private static final String X_COORDINATES_KEY = "X_COORDINATES";
    private static final String Y_COORDINATES_KEY = "Y_COORDINATES";
    private static final String START_TIME_KEY = "START_TIME";
    private static final String TIMES_KEY = "TIMES";
    private static final String TYPED_WORD_KEY = "TYPED_WORD";
    private static final String TEXT_CONTENT_KEY = "TEXT_CONTENT_KEY";

    private static final String SUGGESTED_WORDS_ACTION = "SUGGESTED_WORDS";
    private static final String SUGGESTED_WORDS_KEY = "SUGGESTED_WORD";

    private static final String COMMITTED_WORD_ACTION = "COMMITTED_WORDS";
    private static final String COMMITTED_WORD_KEY = "COMMITTED_WORD";

    private static final String FUNCTIONAL_EVENT_ACTION = "FUNCTIONAL_EVENT";
    private static final String FUNCTIONAL_KEY = "FUNCTIONAL_KEY";

    private PowerManager.WakeLock mWakeLock;
    private WindowManager mWindowManager;

    // For power control.
    private static int btnNumber = 1;
    private Context mContext;

    public MockEditText(Context context) {
        super(context);
        mContext = context;
    }

    public MockEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

    }

    public MockEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    public boolean onPrivateIMECommand(String action, Bundle data) {
        SimpleEntry<String, Bundle> entry = new SimpleEntry<>(action, data);
        // Get swipe up and down gesture
        if (action.equals(GESTURE_ACTION)) {
            int pointCount = data.getInt(POINT_COUNT_KEY);
            int[] xCoordinates = data.getIntArray(X_COORDINATES_KEY);
            int[] yCoordinates = data.getIntArray(Y_COORDINATES_KEY);
            if (xCoordinates != null && yCoordinates != null) {
                int x1 = xCoordinates[0];
                int x2 = xCoordinates[pointCount - 1];
                int y1 = yCoordinates[0];
                int y2 = yCoordinates[pointCount - 1];
                // @link {https://stackoverflow.com/questions/13095494/}
                double rad = Math.atan2(y1 - y2, x2 - x1) + Math.PI;
                double angle = (rad * 180 / Math.PI + 180) % 360;
                if (angle >= 45 && angle < 135) {
                    Log.d(TAG, "gesture up");

                } else if (angle >= 225 && angle < 315) {
                    Log.d(TAG, "gesture down");
                    // Jian: this is the deprecated function, changing backlight
//                    ContentResolver cResolver = mContext.getApplicationContext().getContentResolver();
//                    if (btnNumber++ % 2 == 0) {
//                        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
//                    } else {
//                        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 100);
//                    }
                }
            }

        }

        // Add current edit text content
        if (action.equals(TOUCH_POINTS_ACTION)) {
            data.putString(TEXT_CONTENT_KEY, this.getText().toString());
        }
        synchronized (bundleList) {
            bundleList.add(entry);
        }
        return super.onPrivateIMECommand(action, data);
    }

    /**
     * Called when one phrase/word is inputted and "Next" button is pressed.
     */
    @Override
    public StringBuffer onTaskFinished(boolean isCanceled){
        return new StringBuffer(parseBundleList());
    }

    /**
     * Parses the bundle list. The bundle list will be cleared afterwards.
     */
    public String parseBundleList() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t<imeData>\n");
        for (AbstractMap.SimpleEntry<String, Bundle> entry : bundleList) {
            String action = entry.getKey();
            Bundle data = entry.getValue();

            switch (action) {
                case TOUCH_POINTS_ACTION:
                    sb.append(serializeTouchPoints(data));
                    break;
                case GESTURE_ACTION:
                    sb.append(serializeGesturePoints(data));
                    break;
                case SUGGESTED_WORDS_ACTION:
                    sb.append(serializeSuggestions(data));
                    break;
                case COMMITTED_WORD_ACTION:
                    sb.append(serializeCommittedWords(data));
                    break;
                case FUNCTIONAL_EVENT_ACTION:
                    sb.append(serializeFunctionalEvent(data));
                default:
                    break;
            }
        }
        sb.append("\t</imeData>\n");
        synchronized (bundleList) {
            bundleList.clear();
        }
        return sb.toString();
    }

    private StringBuilder serializeTouchPoints(final Bundle bundle) {
        int pointCount = bundle.getInt(POINT_COUNT_KEY);
        if (pointCount == 0)
            return new StringBuilder("");

        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<touchPoints>\n");
        sb.append("\t\t\t<pointCount>")
                .append(pointCount)
                .append("</pointCount>\n");
        // The int[] are fixed-length arrays, the actual data only come from
        // the first <pointCount> points.
        int[] pointerIds = bundle.getIntArray(POINTER_IDS_KEY);
        int[] xCoordinates = bundle.getIntArray(X_COORDINATES_KEY);
        int[] yCoordinates = bundle.getIntArray(Y_COORDINATES_KEY);
        long startTime = bundle.getLong(START_TIME_KEY);
        int[] times = bundle.getIntArray(TIMES_KEY);
        String typedWord = bundle.getString(TYPED_WORD_KEY);
        String editTextContent = bundle.getString(TEXT_CONTENT_KEY);

        ArrayList<Integer> xCoordinates_ = new ArrayList<>();
        ArrayList<Integer> yCoordinates_ = new ArrayList<>();
        ArrayList<Integer> pointerIds_ = new ArrayList<>();
        ArrayList<Integer> times_ = new ArrayList<>();
        if (xCoordinates != null && yCoordinates != null
                && pointerIds != null && times != null) {
            for (int i = 0; i < pointCount; i++) {
                xCoordinates_.add(xCoordinates[i]);
                yCoordinates_.add(yCoordinates[i]);
                pointerIds_.add(pointerIds[i]);
                times_.add(times[i]);
            }
            sb.append("\t\t\t<x>")
                    .append(xCoordinates_)
                    .append("</x>\n")
                    .append("\t\t\t<y>")
                    .append(yCoordinates_)
                    .append("</y>\n")
                    .append("\t\t\t<pointer>")
                    .append(pointerIds_)
                    .append("</pointer>\n")
                    .append("\t\t\t<startTime>")
                    .append(startTime)
                    .append("</startTime>\n")
                    .append("\t\t\t<time>")
                    .append(times_)
                    .append("</time>\n")
                    .append("\t\t\t<typedWord>")
                    .append(typedWord)
                    .append("</typedWord>\n")
                    .append("\t\t\t<currentOutput>")
                    .append(editTextContent)
                    .append("</currentOutput>\n");
        } else {
            Log.e(TAG, "Touch point arrays are NULL!");
        }
        sb.append("\t\t</touchPoints>\n");
        return sb;
    }

    private StringBuilder serializeGesturePoints(final Bundle bundle) {
        int pointCount = bundle.getInt(POINT_COUNT_KEY);
        if (pointCount == 0)
            return new StringBuilder("");

        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<gesturePoints>\n");
        sb.append("\t\t\t<pointCount>")
                .append(pointCount)
                .append("</pointCount>\n");
        // The int[] are fixed-length arrays, the actual data only come from
        // the first <pointCount> points.
        int[] pointerIds = bundle.getIntArray(POINTER_IDS_KEY);
        int[] xCoordinates = bundle.getIntArray(X_COORDINATES_KEY);
        int[] yCoordinates = bundle.getIntArray(Y_COORDINATES_KEY);
        long startTime = bundle.getLong(START_TIME_KEY);
        int[] times = bundle.getIntArray(TIMES_KEY);

        ArrayList<Integer> xCoordinates_ = new ArrayList<>();
        ArrayList<Integer> yCoordinates_ = new ArrayList<>();
        ArrayList<Integer> pointerIds_ = new ArrayList<>();
        ArrayList<Integer> times_ = new ArrayList<>();
        if (xCoordinates != null && yCoordinates != null
                && pointerIds != null && times != null) {
            for (int i = 0; i < pointCount; i++) {
                xCoordinates_.add(xCoordinates[i]);
                yCoordinates_.add(yCoordinates[i]);
                pointerIds_.add(pointerIds[i]);
                times_.add(times[i]);
            }
            sb.append("\t\t\t<x>")
                    .append(xCoordinates_)
                    .append("</x>\n")
                    .append("\t\t\t<y>")
                    .append(yCoordinates_)
                    .append("</y>\n")
                    .append("\t\t\t<pointer>")
                    .append(pointerIds_)
                    .append("</pointer>\n")
                    .append("\t\t\t<startTime>")
                    .append(startTime)
                    .append("</startTime>\n")
                    .append("\t\t\t<time>")
                    .append(times_)
                    .append("</time>\n");
        } else {
            Log.e(TAG, "Gesture point arrays are NULL!");
        }
        sb.append("\t\t</gesturePoints>\n");
        return sb;
    }

    private StringBuilder serializeSuggestions(final Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> suggestedWords =
                bundle.getStringArrayList(SUGGESTED_WORDS_KEY);
        sb.append("\t\t<suggestions>")
                .append(suggestedWords)
                .append("</suggestions>\n");
        return sb;
    }

    private StringBuilder serializeCommittedWords(final Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<committedWords>")
                .append(bundle.getString(COMMITTED_WORD_KEY))
                .append("</committedWords>\n");
        return sb;
    }

    private StringBuilder serializeFunctionalEvent(final Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<functionKey>")
                .append(bundle.getString(FUNCTIONAL_KEY))
                .append("</functionKey>\n");
        return sb;
    }
}
