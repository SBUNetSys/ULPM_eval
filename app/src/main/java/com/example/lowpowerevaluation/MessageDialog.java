package com.example.lowpowerevaluation;

import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MessageDialog extends DialogFragment {
    private TextView mTextView;
    private String mText;
    private boolean mClickOKToFinish;
    private static String KEY_MSG = "msg";

    static MessageDialog newInstance(final String text,
                                     final boolean finishTaskAfterClickOK) {
        MessageDialog msgDialog = new MessageDialog();

        Bundle args = new Bundle();
        args.putString(KEY_MSG, text);
        msgDialog.setArguments(args);
        msgDialog.setIsFinishDialog(finishTaskAfterClickOK);
        return msgDialog;
    }

    private void setIsFinishDialog(final boolean isFinish) {
        mClickOKToFinish = isFinish;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mText = getArguments().getString(KEY_MSG);
        setStyle(DialogFragment.STYLE_NO_TITLE,
                android.R.style.Theme_DeviceDefault_Dialog);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view =
                inflater.inflate(R.layout.fragment_message, container);
        mTextView = (TextView) view.findViewById(R.id.message_content);
        mTextView.setText(mText);

        if (mText.length() < 20) {
            final Resources res = getResources();
            mTextView.setTextSize(
                    res.getDimensionPixelSize(R.dimen.dialog_text_size_big));
            mTextView.setWidth((int)Math.max(mTextView.getMinimumWidth(),
                    res.getDimension(R.dimen.dialog_width)));
            mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        }

        final Button butContinue =
                (Button) view.findViewById(R.id.message_button);
        butContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mClickOKToFinish) {
                    getActivity().finish();
                }
            }
        });
        return view;
    }
}
