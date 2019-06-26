package com.example.lowpowerevaluation;

import android.os.Bundle;

abstract class OnTaskComplete {

    // Key for values passed to onComplete(...) for StimulusGenerator
    static String KEY_RESULT = "result";

    public abstract void onComplete(Bundle params);
}
