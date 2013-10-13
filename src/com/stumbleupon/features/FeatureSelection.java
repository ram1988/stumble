package com.stumbleupon.features;

import weka.attributeSelection.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.*;

public class FeatureSelection {
	protected static void useLowLevel(Instances data) throws Exception {
		System.out.println("Analyzing attributes");
		AttributeSelection attrSel = new AttributeSelection();
		InfoGainAttributeEval eval = new InfoGainAttributeEval();
		Ranker rnkr = new Ranker();
		attrSel.setEvaluator(eval);
		attrSel.setSearch(rnkr);
		attrSel.setRanking(true);
		attrSel.SelectAttributes(data);
		int[] indices = attrSel.selectedAttributes();
		System.out.println("selected attribute indices:" + Utils.arrayToString(indices));
		System.out.println("selected attributes:");
		for(int i : indices) {
			System.out.println(data.attribute(i));
		}
	}
	  
	public static void main(String[] args) throws Exception {
		// load data
		System.out.println("Loading data");
		DataSource source = new DataSource("data/train_categoryFixed.arff");
		Instances data = source.getDataSet();
		if (data.classIndex() == -1) {
			data.setClassIndex(data.numAttributes() - 1);
		}

		useLowLevel(data);
	}
}
