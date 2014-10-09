package com.cenix.atc;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;


public abstract class AbstractATCNodeFactory extends NodeFactory<ATCNodeModel> {

    @Override
    public int getNrNodeViews() {
        return 0;
    }


    @Override
    public NodeView<ATCNodeModel> createNodeView(final int viewIndex,
                                                 final ATCNodeModel nodeModel) {
//        return new ACTNodeView(nodeModel);
        return null;
    }


    @Override
    public boolean hasDialog() {
        return true;
    }


    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ATCNodeDialog();
    }

}
