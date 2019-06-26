package com.example.lowpowerevaluation;

import com.example.lowpowerevaluation.TouchDistActivity.DataSet;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Generates words or phrases for the user to type.
 */
final class StimulusGenerator {
    // at most 6 blocks, with the first one being a warm-up
    private static final int BLOCK_MAX_NUM = 6;
    private final ArrayList<String> mPhrases = new ArrayList<>();
    private final Random mRandGen = new Random();
    private final ArrayList<ArrayList<String>> mBlockList = new ArrayList<>();
    private Context mContext;

    void load(final Context context, final DataSet dataSet,
              final OnTaskComplete onTaskComplete) {
        mContext = context;
        mBlockList.clear();
        mPhrases.clear();

        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            int phraseNum = 0;

            @Override
            protected Boolean doInBackground(Void... params) {
                InputStream inputStream;
                try {
                    // open the file for reading
                    inputStream = mContext.getResources().openRawResource(
                            dataSet.getResourceId());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
                try {
                    // if file available for reading
                    if (inputStream != null) {
                        // prepare the file for reading
                        InputStreamReader inputreader =
                                new InputStreamReader(inputStream);
                        BufferedReader buffreader =
                                new BufferedReader(inputreader);
                        String line;
                        // read every line of the file
                        while ((line = buffreader.readLine()) != null) {
                            mPhrases.add(line.trim());
                            phraseNum++;
                        }

                        // close the file again
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(OnTaskComplete.KEY_RESULT, result);
                createBlocks(phraseNum / dataSet.getTrialNumPerBlock(),
                        dataSet.getTrialNumPerBlock(),
                        dataSet.getRepetitionTimes());
                onTaskComplete.onComplete(bundle);
            }
        };
        // Execute the task in the background.
        task.execute();
    }

    private void createBlocks(final int blockNum, final int stimulusPerBlock,
                              final int repPerWord) {
        for (int i = 0; i < blockNum; i++) {
            ArrayList<String> curBlock = new ArrayList<>();
            for (int j = 0; j < stimulusPerBlock; j++) {
                String curStr = mPhrases.get(i * stimulusPerBlock + j);
                StringBuilder curStrBuilder = new StringBuilder(curStr);
                for (int k = 1; k < repPerWord; k++) {
                    curStrBuilder.append(" ").append(curStr);
                }
                curBlock.add(curStrBuilder.toString());
            }
            randomizedBlock(curBlock);
            mBlockList.add(curBlock);
        }
    }

    private static void randomizedBlock(final ArrayList<String> block) {
        for (int i = 0; i < block.size(); i++) {
            final Random rm = new Random();
            final int toSwitch = rm.nextInt(block.size());
            Collections.swap(block, i, toSwitch);
        }
    }

    public String getNext() {
        int i = mRandGen.nextInt(mPhrases.size());
        return mPhrases.get(i);
    }

    String getPhrase(int blockNum, int trialNum) {
        return(mBlockList.get(blockNum).get(trialNum));
    }

    int getBlockNum() {
        return Math.min(mBlockList.size(), BLOCK_MAX_NUM);
    }
}