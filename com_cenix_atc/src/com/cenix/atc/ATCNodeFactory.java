package com.cenix.atc;

public class ATCNodeFactory extends AbstractATCNodeFactory {

	@Override
	public ATCNodeModel createNodeModel() {
		return new ATCNodeModel(0,  1);
	}

}
