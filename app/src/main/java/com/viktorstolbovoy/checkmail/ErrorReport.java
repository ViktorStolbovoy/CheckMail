package com.viktorstolbovoy.checkmail;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by Main User on 6/10/2016.
 */
public class ErrorReport extends DialogFragment {

    public static void showError(FragmentManager lm, String message) {
        DialogFragment dialog = new ErrorReport();
        dialog.show(lm, "ErrorReport");
        Bundle args = new Bundle();
        args.putString("text", message);
        dialog.setArguments(args);
    }

    private String mText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Error");
        builder.setMessage(getArguments().getString("text"));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // You don't have to do anything here if you just want it dismissed when clicked
            }
        });

        return builder.create();
    }
}