package org.cimugbit.onetouchnotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class TreeViewActivity extends AppCompatActivity {

    final static String EXTRA_NAME = "org.cimugbit.onetouchnotes.EXTRA_NAME";
    final static String EXTRA_DIRECTORY = "org.cimugbit.onetouchnotes.EXTRA_DIRECTORY";

    final static int CREATE_NOTE = 1;
    final static int EDIT_NOTE = 2;

    private AndroidTreeView treeView;
    private TreeNode root;

    private TreeNode contextNode = null;
    private TreeNode cutNode = null;

    MenuItem pasteMenuItem = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_tree_view );

        Log.i( getClass().getCanonicalName(), "onCreate: " + this );
        // AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_AUTO );

        contextNode = null;
        cutNode = null;

        // alternativ: [ContextCompat.]getExternalFilesDirs() mit Auswahlmöglichkeit
        boolean haveExternalStorage = Environment.MEDIA_MOUNTED.equals( Environment.getExternalStorageState() );
        File rootDir = haveExternalStorage ? getExternalFilesDir( null ) : null;
        if (rootDir == null) rootDir = getFilesDir();

        root = new TreeNode( new TreeNodeHolder.NodeConfig( rootDir.getAbsolutePath() ) );
        populateNode( root );

        root.setSelectable( false );

        treeView = new AndroidTreeView( this, root );
        treeView.setDefaultAnimation( true );
        treeView.setDefaultContainerStyle( R.style.TreeNodeStyle );
        treeView.setDefaultViewHolder( TreeNodeHolder.class );
        treeView.setDefaultNodeClickListener( new TreeNode.TreeNodeClickListener() {
            @Override
            public void onClick( TreeNode node, Object value ) {
                TreeNodeHolder.NodeConfig config = (TreeNodeHolder.NodeConfig) value;
                if (config.icon != 0) {
                    // edit note
                    contextNode = node;
                    Intent intent = new Intent( TreeViewActivity.this, TextNoteActivity.class );
                    intent.setAction( Intent.ACTION_EDIT );
                    intent.putExtra( EXTRA_NAME, config.name );
                    intent.putExtra( EXTRA_DIRECTORY, getPath( node.getParent() ) );
                    startActivityForResult( intent, EDIT_NOTE );
                }
            }
        } );
        treeView.setDefaultNodeLongClickListener( new TreeNode.TreeNodeLongClickListener() {
            @Override
            public boolean onLongClick( TreeNode node, Object value ) {
                contextNode = node;
                if (node == cutNode) {
                    dimNode( cutNode, false );
                    cutNode = null;
                }
                // allow long click to propagate to cause context menu callback
                return false;
            }
        } );

        View uiView = treeView.getView();
        registerForContextMenu( uiView );
        ViewGroup containerView = (ViewGroup) findViewById( R.id.tree_container );
        containerView.addView( uiView );

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString( "treeState" );
            Log.i( getClass().getCanonicalName(), "restoreState: " + state );
            if (!TextUtils.isEmpty( state )) {
                treeView.restoreState( state );
            }
        }
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        outState.putString( "treeState", treeView.getSaveState() );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate( R.menu.menu_main, menu );
        pasteMenuItem = menu.findItem( R.id.action_paste );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        pasteMenuItem.setVisible( (cutNode != null) && (cutNode.getParent() != root) );
        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch (item.getItemId()) {
            case R.id.action_new_folder: {
                showCreateFolderDialog( root );
                return true;
            }
            case R.id.action_new_note: {
                showCreateNewNoteActivity( root );
                return true;
            }
            case R.id.action_paste: {
                moveNode( cutNode, root );
                dimNode( cutNode, false );
                cutNode = null;
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent( this, AboutActivity.class );
                startActivityForResult( intent, 0 );
                return true;
            }
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ) {
        if (contextNode != null) {
            super.onCreateContextMenu( menu, v, menuInfo );
            MenuInflater inflater = getMenuInflater();
            inflater.inflate( isFolderNode( contextNode ) ? R.menu.context_menu_folder : R.menu.context_menu_note, menu );
            if (!contextNode.isLeaf()) {
                menu.removeItem( R.id.action_delete );
            }
            if ((cutNode == null) || (cutNode.getParent() == contextNode)) {
                menu.removeItem( R.id.action_paste );
            }
        }

    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        switch (item.getItemId()) {
            case R.id.action_delete: {
                AlertDialog.Builder builder = new AlertDialog.Builder( this );
                builder.setTitle( getApplicationContext().getResources().getString( R.string.title_delete_confirm, Util.decodeFileName( getConfig( contextNode ).name, null ) ) );
                builder.setNegativeButton( android.R.string.no, null );
                builder.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialog, int id ) {
                        if (getPath( contextNode ).delete()) {
                            treeView.removeNode( contextNode );
                        }
                        else {
                            Toast.makeText( getApplicationContext(), getResources().getString( R.string.error_deleting_object ), Toast.LENGTH_SHORT ).show();
                        }
                    }
                } );
                builder.create().show();
                return true;
            }
            case R.id.action_new_folder: {
                showCreateFolderDialog( contextNode );
                return true;
            }
            case R.id.action_new_note: {
                showCreateNewNoteActivity( contextNode );
                return true;
            }
            case R.id.action_rename: {
                showRenameDialog();
                return true;
            }
            case R.id.action_cut: {
                if (cutNode != null) {
                    dimNode( cutNode, false );
                }
                cutNode = contextNode;
                dimNode( cutNode, true );
                return true;
            }
            case R.id.action_paste: {
                moveNode( cutNode, contextNode );
                dimNode( cutNode, false );
                cutNode = null;
                return true;
            }
        }
        return super.onContextItemSelected( item );
    }

    private static TreeNodeHolder.NodeConfig getConfig( TreeNode node ) {
        return (TreeNodeHolder.NodeConfig) node.getValue();
    }

    private static boolean isFolderNode( TreeNode node ) {
        return (getConfig( node ).icon == 0);
    }

    private static File getPath( TreeNode node ) {
        TreeNodeHolder.NodeConfig config = getConfig( node );
        TreeNode parent = node.getParent();
        return (parent == null) ?
                new File( config.name ) :
                new File( getPath( parent ), config.name + config.extension );
    }

    private void populateNode( TreeNode node ) {
        File directory = getPath( node );
        String[] names = directory.list();
        if (names != null) {
            Arrays.sort( names );
            List<String> files = new ArrayList<>();
            for ( String name : names ) {
                File path = new File( directory, name );
                if (path.isDirectory()) {
                    TreeNode folder = new TreeNode( new TreeNodeHolder.NodeConfig( name ) );
                    node.addChild( folder );
                    populateNode( folder );
                }
                else {
                    files.add( name );
                }
            }
            for ( String name : files ) {
                String basename = name;
                String extension = "";
                int split = name.lastIndexOf( "." );
                if (split >= 0) {
                    basename = name.substring( 0, split );
                    extension = name.substring( split );
                }
                TreeNode note = new TreeNode( new TreeNodeHolder.NodeConfig( basename, extension, R.string.icon_doc_text ) );
                node.addChild( note );
            }
        }
    }

    private void sortedInsert( TreeNode nodeToInsert, TreeNode parent ) {
        if ((nodeToInsert != null) && parent.isLeaf()) {
            treeView.addNode( parent, nodeToInsert );
            treeView.expandNode( parent );
        }
        else {
            TreeMap<String, TreeNode> folders = new TreeMap<>();
            TreeMap<String, TreeNode> notes = new TreeMap<>();
            for ( TreeNode child : new ArrayList<>( parent.getChildren() ) ) {
                TreeNodeHolder.NodeConfig config = (TreeNodeHolder.NodeConfig) child.getValue();
                ((config.icon == 0) ? folders : notes).put( config.name + config.extension, child );
                // we will remove the child views below, if necessary
                treeView.removeNode( child );
            }
            if (!parent.isExpanded()) {
                parent.getViewHolder().getNodeItemsView().removeAllViews();
            }
            if (nodeToInsert != null) {
                TreeNodeHolder.NodeConfig config = getConfig( nodeToInsert );
                ((config.icon == 0) ? folders : notes).put( config.name + config.extension, nodeToInsert );
            }
            for ( TreeNode child : folders.values() ) {
                treeView.addNode( parent, child );
            }
            for ( TreeNode child : notes.values() ) {
                treeView.addNode( parent, child );
            }
        }
    }

    private void createNode( TreeNode parent, String name, int icon ) {
        sortedInsert(
                (icon == 0) ?
                    new TreeNode( new TreeNodeHolder.NodeConfig( name ) ) :
                    new TreeNode( new TreeNodeHolder.NodeConfig( name, ".txt", icon ) ),
                parent
        );
    }

    private boolean moveNode( TreeNode node, TreeNode destination ) {
        File newPath = new File( getPath( destination ), getConfig( node ).name + getConfig( node ).extension );
        if (newPath.exists()) {
            Toast.makeText( this, getResources().getString( R.string.error_name_exists ), Toast.LENGTH_SHORT ).show();
        }
        else {
            if (getPath( node ).renameTo( newPath )) {
                // for correct unlinking of the child, the parent must be expanded
                TreeNode parent = node.getParent();
                boolean parentExpanded = parent.isExpanded();
                parent.setExpanded( true );
                treeView.removeNode( node );
                parent.setExpanded( parentExpanded );
                sortedInsert( node, destination );
                return true;
            }
            else {
                Toast.makeText( this, getResources().getString( R.string.error_pasting_object ), Toast.LENGTH_SHORT ).show();
            }
        }
        return false;
    }

    private void renameNode( TreeNode node, String name ) {
        TreeNodeHolder viewHolder = (TreeNodeHolder) node.getViewHolder();
        viewHolder.rename( name );
        sortedInsert( null, node.getParent() );
    }

    private void dimNode( TreeNode node, boolean dim ) {
        node.getViewHolder().toggleSelectionMode( dim );
    }

    private void showCreateFolderDialog( final TreeNode parentNode ) {
        Util.showStringDialog( this, R.string.title_create_folder, null, new Util.StringDialogCallback() {
            @Override
            public void execute( String value ) {
                String name = Util.encodeFileName( value.trim() );
                if (name != null) {
                    File path = new File( getPath( parentNode ), name );
                    if (path.mkdir()) {
                        createNode( parentNode, name, 0 );
                    }
                    else {
                        Toast.makeText( getApplicationContext(), getResources().getString( R.string.error_creating_folder ), Toast.LENGTH_SHORT ).show();
                    }
                }
                else {
                    Toast.makeText( getApplicationContext(), getResources().getString( R.string.error_internal_error ), Toast.LENGTH_SHORT ).show();
                }
            }
        }, new Util.StringValidator() {
            @Override
            public boolean validate( String value ) {
                String name = Util.encodeFileName( value.trim() );
                if (name != null) {
                    File path = new File( getPath( parentNode ), name );
                    return !path.exists();
                }
                return false;
            }
        } );
    }

    private void showRenameDialog() {
        // on decoding error, leave field blank
        String oldName = Util.decodeFileName( getConfig( contextNode ).name, "" );
        Util.showStringDialog( this, R.string.title_new_name, oldName, new Util.StringDialogCallback() {
            @Override
            public void execute( String value ) {
                String newName = Util.encodeFileName( value.trim() );
                if (newName != null) {
                    File path = new File( getPath( contextNode.getParent() ), newName + getConfig( contextNode ).extension );
                    if (getPath( contextNode ).renameTo( path )) {
                        renameNode( contextNode, newName );
                    }
                    else {
                        Toast.makeText( getApplicationContext(), getResources().getString( R.string.error_renaming_object ), Toast.LENGTH_SHORT ).show();
                    }
                }
                else {
                    Toast.makeText( getApplicationContext(), getResources().getString( R.string.error_internal_error ), Toast.LENGTH_SHORT ).show();
                }
            }
        }, new Util.StringValidator() {
            @Override
            public boolean validate( String value ) {
                String newName = Util.encodeFileName( value.trim() );
                if (newName != null) {
                    File path = new File( getPath( contextNode.getParent() ), newName + getConfig( contextNode ).extension );
                    return !path.exists();
                }
                return false;
            }
        } );
    }

    private void showCreateNewNoteActivity( final TreeNode parentNode ) {
        contextNode = parentNode;
        Intent intent = new Intent( TreeViewActivity.this, TextNoteActivity.class );
        intent.putExtra( EXTRA_DIRECTORY, getPath( parentNode ) );
        startActivityForResult( intent, CREATE_NOTE );
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        Log.i( getClass().getCanonicalName(), "onActivityResult: " + contextNode + " / " + resultCode + " (" + ((data != null) ? data.getStringExtra( EXTRA_NAME ) : "VOID") + ")" );
        // unless activity has been recreated anyway, adjust tree on changes
        if ((contextNode != null) && (data != null)) {
            switch (requestCode) {
                case EDIT_NOTE: {
                    String newName = data.getStringExtra( EXTRA_NAME );
                    if (newName != null) {
                        renameNode( contextNode, newName );
                    }
                    break;
                }
                case CREATE_NOTE: {
                    createNode( contextNode, data.getStringExtra( EXTRA_NAME ), R.string.icon_doc_text );
                    break;
                }
            }
        }
        super.onActivityResult( requestCode, resultCode, data );
    }

}
