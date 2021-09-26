package org.cimugbit.onetouchnotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.github.johnkil.print.PrintView;
import com.unnamed.b.atv.model.TreeNode;

// class must be public to get instantiated by the TreeView!
public class TreeNodeHolder extends TreeNode.BaseNodeViewHolder<TreeNodeHolder.NodeConfig> {

    static class NodeConfig {
        final int icon;
        public String name;
        final String extension;

        NodeConfig( String name ) {
            this.name = name;
            this.extension = "";
            this.icon = 0;
        }

        NodeConfig( String name, String extension, int icon ) {
            this.name = name;
            this.extension = extension;
            this.icon = icon;
        }
    }

    private View nodeView;
    private PrintView iconView;
    private TextView textView;
    private NodeConfig config;

    public TreeNodeHolder( Context context ) {
        super( context );
    }

    @Override
    public View createNodeView( TreeNode node, NodeConfig value ) {
        config = value;

        final LayoutInflater inflater = LayoutInflater.from( context );
        nodeView = inflater.inflate( R.layout.layout_tree_node, null, false );

        textView = (TextView) nodeView.findViewById( R.id.node_text );
        textView.setText( Util.decodeFileName( config.name, null ) );
        iconView = (PrintView) nodeView.findViewById( R.id.icon );
        if (config.icon != 0) {
            iconView.setIconText( context.getResources().getString( config.icon ) );
        }
        else {
            toggle( false );
        }

        return nodeView;
    }

    void rename( String newName ) {
        config.name = newName;
        textView.setText( Util.decodeFileName( config.name, null ) );
    }

    @Override
    public void toggle( boolean active ) {
        if (config.icon == 0) {
            iconView.setIconText( context.getResources().getString( active ? R.string.icon_folder_open : R.string.icon_folder ) );
        }
    }

    @Override
    public void toggleSelectionMode( boolean editModeEnabled ) {
        View wrapperView = (View) nodeView.getParent().getParent();
        if (editModeEnabled) {
            wrapperView.setAlpha( 0.5f );
        } else {
            wrapperView.setAlpha( 1.0f );
        }
    }
}
