package com.example.lowpowerevaluation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

public class StartMenu extends Activity {
    public static String ISGESTUREINPUT = "isGestureInput";
    public static String USERID = "userId";
    public static String INPUTFINGER = "inputFinger";
    public static String ISGESTUREEXPERT = "isGesExpert";
    public static String DATASET = "dataSet";
    public static String KEYBOARDTYPE = "keyboardType";
    public static String SCREENFREQ = "screenFreq";

    private boolean mIsGestureInput;
    private String mInputFinger;
    private int mUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_menu);
        final EditText uerIdEditText = (EditText) findViewById(R.id.ET_UserId);
        final RadioGroup radioGroupInputType =
                (RadioGroup) findViewById(R.id.radioInputType);
        final Button butFirstMenu = (Button) findViewById(R.id.bnStart1);
        butFirstMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsGestureInput = getIsGestureInput(radioGroupInputType);
                mInputFinger = getInputFinger(radioGroupInputType);
                if (uerIdEditText.getText().toString().trim().length() == 0){
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "User ID must not be empty!", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    mUserId = Integer.parseInt(
                            uerIdEditText.getText().toString().trim());
                    createSecondMenu(R.layout.start_menu_2);
                }
            }
        });
    }

    private void createSecondMenu(final int layoutId) {
        setContentView(layoutId);
        final RadioGroup radioGroupDataSet =
                (RadioGroup) findViewById(R.id.radioDataSet);
        //final RadioGroup radioGroupIsGestureExpert =
    //                (RadioGroup) findViewById(R.id.radioGestureExpert);
        final RadioGroup radioGroupKeyboardType =
                (RadioGroup) findViewById(R.id.radioKeyboardType);
        final RadioGroup radioGroupScreenSetting =
                (RadioGroup) findViewById(R.id.radioScreenOffFreq);

        final Button butSecondMenu = (Button) findViewById(R.id.bnStart2);
        butSecondMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startOct =
                        new Intent(StartMenu.this, TouchDistActivity.class);
                startOct.putExtra(ISGESTUREINPUT, mIsGestureInput)
                        .putExtra(DATASET, getDataResId(radioGroupDataSet))
                        .putExtra(USERID, mUserId)
                        .putExtra(INPUTFINGER, mInputFinger)
                        //.putExtra(ISGESTUREEXPERT,
                        //        getIsGestureExpert(radioGroupIsGestureExpert))
                        .putExtra(KEYBOARDTYPE,
                                getKeyboardType(radioGroupKeyboardType))
                        .putExtra(SCREENFREQ,
                                getScreenSetting(radioGroupScreenSetting));
                startActivity(startOct);
            }
        });
    }

    private static boolean getIsGestureInput(final RadioGroup radioGroup) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            //case R.id.radioInputTypeIndexGesture:
            //case R.id.radioInputTypeOneThumbGesture:
            //    return true;
            //case R.id.radioInputTypeOneThumbTouch:
            case R.id.radioInputTypeTwoThumbTouch:
            //case R.id.radioInputTypeIndexTouch:
            default:
                return false;
        }
    }

    private static String getInputFinger(final RadioGroup radioGroup) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            //case R.id.radioInputTypeIndexGesture:
            //case R.id.radioInputTypeIndexTouch:
            //    return TouchDistActivity.INPUT_FINGER_INDEX;
            //case R.id.radioInputTypeOneThumbGesture:
            //case R.id.radioInputTypeOneThumbTouch:
            //    return TouchDistActivity.INPUT_FINGER_ONE_THUMB;
            case R.id.radioInputTypeTwoThumbTouch:
            default:
                return TouchDistActivity.INPUT_FINGER_TWO_THUMBS;
        }
    }

    private static String getKeyboardType(final RadioGroup radioGroup) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.radioKeyboardTypeInvisibleTap:
                return TouchDistActivity.INVISIBLE_TAP;
            default:
                return TouchDistActivity.DEFAULT_KEYBOARD;
        }
    }

    private static String getScreenSetting(final RadioGroup radioGroup) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.radioScreenByDefault:
                return TouchDistActivity.DEFAULT_SCREEN_FREQ;
            case R.id.radioScreenOff:
                return TouchDistActivity.SCREEN_OFF;
            case R.id.radioScreenOffArbitrary:
                return TouchDistActivity.SCREEN_OFF_ARBITRARY;
            default:
                return TouchDistActivity.WORDWISE_SCREEN_FREQ;
        }
    }


    private static int getDataResId(final RadioGroup radioGroup) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.radioDataSetT20:
                return R.raw.t_20;
            case R.id.radioDataSetT40:
                return R.raw.t_40;
            case R.id.radioDataSetT80:
                return R.raw.t_80;
            case R.id.radioDataSetT160:
                return R.raw.t_160;
            default:
                return R.raw.t_40;
        }
    }
}
