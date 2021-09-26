package org.cimugbit.onetouchnotes;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

class Util {

    static String encodeFileName( String name ) {
        if (!name.isEmpty()) {
            try {
                return URLEncoder.encode( name, "UTF-8" );
            } catch (UnsupportedEncodingException e) {
                // fall through
            }
        }
        return null;
    }

    static String decodeFileName( String filename, String fallback ) {
        try {
            return URLDecoder.decode( filename, "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            return (fallback != null) ? fallback : filename;
        }
    }

    static String findFreeName( File directory, String name, String extension, boolean encode ) {
        int counter = 0;
        String testee = name;
        String filename = null;
        while (counter <= 1024) {
            filename = encodeFileName( testee );
            if ((filename != null) && !(new File( directory, filename + extension )).exists()) break;
            testee = name + " - " + ++counter;
        }
        return (counter > 1024) ? null : (encode ? filename : testee);
    }

    interface StringValidator {
        boolean validate( String value );
    }
    interface StringDialogCallback {
        void execute( String value );
    }
    static void showStringDialog( Activity activity, int title, String value, final StringDialogCallback action, final StringValidator validator ) {
        LayoutInflater inflater = activity.getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder( activity );
        final View view = inflater.inflate( R.layout.string_input_view, null );
        final EditText input = (EditText) view.findViewById( R.id.input_string );
        if (value != null) {
            input.setText( value );
        }
        builder.setView( view );
        builder.setTitle( activity.getResources().getString( title ) );
        builder.setNegativeButton( android.R.string.cancel, null );
        builder.setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialog, int id ) {
                action.execute( input.getText().toString() );
            }
        } );
        AlertDialog dialog = builder.create();
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE );
        }
        dialog.show();
        if (validator != null) {
            final Button okButton = dialog.getButton( AlertDialog.BUTTON_POSITIVE );
            if (okButton != null) {
                okButton.setEnabled( (value != null) && validator.validate( value ) );
                input.addTextChangedListener( new TextWatcher() {
                    @Override
                    public void beforeTextChanged( CharSequence s, int start, int count, int after ) {}

                    @Override
                    public void onTextChanged( CharSequence s, int start, int before, int count ) {}

                    @Override
                    public void afterTextChanged( Editable s ) {
                        okButton.setEnabled( validator.validate( input.getText().toString() ) );
                    }
                } );
            }
        }
    }

}
