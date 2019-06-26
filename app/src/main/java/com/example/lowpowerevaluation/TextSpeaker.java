package com.example.lowpowerevaluation;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TextSpeaker implements TextToSpeech.OnInitListener
{
    private TextToSpeech tts;
    private Activity activity;

    private static HashMap DUMMY_PARAMS = new HashMap();
    static
    {
        DUMMY_PARAMS.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "theUtId");
    }
    private ReentrantLock waitForInitLock = new ReentrantLock();

    public TextSpeaker(Activity parentActivity)
    {
        activity = parentActivity;
        tts = new TextToSpeech(activity, this);
        //don't do speak until initing
        waitForInitLock.lock();
    }

    public void onInit(int version)
    {        //unlock it so that speech will happen
        waitForInitLock.unlock();
    }

    public void say(String say)
    {
        tts.speak(say, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void say(String say, TextToSpeech.OnUtteranceCompletedListener whenTextDone)
    {
        if (waitForInitLock.isLocked())
        {
            try
            {
                waitForInitLock.tryLock(180, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Log.e("speaker", "interruped");
            }
            //unlock it here so that it is never locked again
            waitForInitLock.unlock();
        }

        int result = tts.setOnUtteranceCompletedListener(whenTextDone);
        if (result == TextToSpeech.ERROR)
        {
            Log.e("speaker", "failed to add utterance listener");
        }
        //note: here pass in the dummy params so onUtteranceCompleted gets called
        tts.speak(say, TextToSpeech.QUEUE_FLUSH, DUMMY_PARAMS);
    }

    /**
     * make sure to call this at the end
     */
    public void done()
    {
        tts.shutdown();
    }
}
