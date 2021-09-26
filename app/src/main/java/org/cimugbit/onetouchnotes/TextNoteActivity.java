package org.cimugbit.onetouchnotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextNoteActivity extends ChildActivity {

    private EditText note;
    private boolean isCreate;
    private String name;
    private File directory;
    private String initialName;
    private String extractedName = null;
    private boolean isChanged = false;

    MenuItem saveMenuItem = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.note_text_view );

        note = (EditText) findViewById( R.id.note_text );
        name = initialName = getIntent().getStringExtra( TreeViewActivity.EXTRA_NAME );
        directory = (File) getIntent().getSerializableExtra( TreeViewActivity.EXTRA_DIRECTORY );
        isCreate = !Intent.ACTION_EDIT.equals( getIntent().getAction() );
        if (isCreate) {
            name = getResources().getString( R.string.menu_new_note );
        }

        if (savedInstanceState != null) {
            name = savedInstanceState.getString( "name" );
            extractedName = savedInstanceState.getString( "extractedName" );
            isChanged = savedInstanceState.getBoolean( "isChanged" );
        }
        else {
            if (!isCreate) {
                BufferedReader input = null;
                try {
                    StringBuilder builder = new StringBuilder();
                    input = new BufferedReader( new FileReader( new File( directory, name + ".txt" ) ) );
                    char[] buffer = new char[1024];
                    int numRead;
                    while ((numRead = input.read( buffer )) > 0) {
                        builder.append( buffer, 0, numRead );
                    }
                    input.close();
                    String text = builder.toString();
                    extractedName = extractName( text );
                    note.setText( text );
                }
                catch (IOException e) {
                    if (input != null) {
                        try {
                            input.close();
                        }
                        catch (IOException e1) {
                            // double error
                        }
                    }
                    Toast.makeText( this, getResources().getString( R.string.error_internal_error ), Toast.LENGTH_SHORT ).show();
                    finish();
                    return;
                }
                getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN );
            }
        }
        setTitle( Util.decodeFileName( name, null ) );

        note.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {}

            @Override
            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                isChanged = true;
            }

            @Override
            public void afterTextChanged( Editable s ) {}
        } );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        // the above call did save the state of the EditText, adding instance data now
        outState.putString( "name", name );
        outState.putString( "extractedName", extractedName );
        outState.putBoolean( "isChanged", isChanged );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate( R.menu.menu_note, menu );
        saveMenuItem = menu.findItem( R.id.action_save );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        saveMenuItem.setVisible( isModified() );
        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_save: {
                if (saveNote()) {
                    setTitle( Util.decodeFileName( name, null ) );
                }
                return true;
            }
            case R.id.action_cancel: {
                if (isModified()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder( this );
                    builder.setTitle( R.string.title_discard_modifications );
                    builder.setNegativeButton( android.R.string.no, null );
                    builder.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                            handleExit( false );
                            finish();
                        }
                    } );
                    builder.create().show();
                }
                else {
                    handleExit( false );
                    finish();
                }
                return true;
            }
            case android.R.id.home: {
                if (handleExit( isModified() )) {
                    super.onOptionsItemSelected( item );
                }
                return true;
            }
            case R.id.action_insert_date: {
                String text = DateUtils.formatDateTime( this, (new Date()).getTime(), DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR );
                note.getText().replace( note.getSelectionStart(), note.getSelectionEnd(), text );
                return true;
            }
            case R.id.action_insert_timestamp: {
                String text = DateUtils.formatDateTime( this, (new Date()).getTime(), DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR )
                        + " " + DateUtils.formatDateTime( this, (new Date()).getTime(), DateUtils.FORMAT_SHOW_TIME );
                note.getText().replace( note.getSelectionStart(), note.getSelectionEnd(), text );
                return true;
            }
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onBackPressed() {
        if (handleExit( isModified() )) {
            super.onBackPressed();
        }
    }

    private boolean handleExit( boolean saveNote ) {
        boolean success = true;
        if (saveNote) {
            success = saveNote();
        }
        if (success) {
            if ((extractedName != null) && (isCreate || !name.equals( initialName ))) {
                // communicate the new name to the calling activity
                Intent result = new Intent();
                result.putExtra( TreeViewActivity.EXTRA_NAME, name );
                setResult( RESULT_OK, result );
            }
        }
        return success;
    }

    private boolean isModified() {
        if (extractedName == null) {
            String newExtractedName = extractName( note.getText().toString() );
            return !newExtractedName.isEmpty();
        }
        else {
            return isChanged;
        }
    }

    private boolean saveNote() {
        // extractedName == null means that no file for the note exists
        // otherwise the filename is name + ".txt", and extractedName is
        // the name extracted from the version last saved
        String text = note.getText().toString();
        String newExtractedName = extractName( text );
        if ((extractedName == null) && (newExtractedName.isEmpty())) {
            return true;
        }
        String newName;
        File output;
        File oldFile = null;
        if ((extractedName != null) && (extractedName.equals( newExtractedName ) || newExtractedName.isEmpty())) {
            // save with previous name
            newName = name;
            output = new File( directory, newName + ".txt" );
        }
        else {
            // save with new name
            if (extractedName != null) {
                oldFile = new File( directory, name + ".txt" );
            }
            newName = Util.findFreeName( directory, newExtractedName, ".txt", true );
            if (newName != null) {
                output = new File( directory, newName + ".txt" );
            }
            else {
                Toast.makeText( this, getResources().getString( R.string.error_invalid_name ), Toast.LENGTH_SHORT ).show();
                return false;
            }
        }
        boolean success = false;
        try {
            // write data
            Log.i( getClass().getCanonicalName(), output.toString() );
            FileWriter writer = new FileWriter( output );
            writer.write( text );
            writer.close();
            success = true;
            // commit to internal state
            extractedName = newExtractedName;
            name = newName;
            isChanged = false;
            // cleanup after rename
            if (oldFile != null) {
                oldFile.delete();
            }
        }
        catch (IOException e) {
            Toast.makeText( this, getResources().getString( R.string.error_saving_note ), Toast.LENGTH_SHORT ).show();
        }
        return success;
    }

    private String extractName( String text ) {
        Matcher parser = Pattern.compile( "[^\r\n]+" ).matcher( text );
        String name = "";
        while (parser.find()) {
            name = parser.group().trim();
            if (!name.isEmpty()) {
                break;
            }
        }
        return name;
    }
}
