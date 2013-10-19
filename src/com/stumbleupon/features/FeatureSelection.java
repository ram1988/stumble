package com.stumbleupon.features;

import weka.attributeSelection.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.*;

public class FeatureSelection {
	public int[] useLowLevel(Instances data, int numAttr) throws Exception {
		System.out.println("Analyzing attributes");
		AttributeSelection attrSel = new AttributeSelection();
		InfoGainAttributeEval eval = new InfoGainAttributeEval();
		Ranker rnkr = new Ranker();
		attrSel.setEvaluator(eval);
		attrSel.setSearch(rnkr);
		attrSel.setRanking(true);
		attrSel.SelectAttributes(data);
		int[] rankedIndices = attrSel.selectedAttributes();
		int[] selectedIndices = new int[numAttr + 1];
		for(int i = 0; i < numAttr; i++) {
			selectedIndices[i] = rankedIndices[i];
		}
		selectedIndices[numAttr] = data.classIndex();
		return selectedIndices;
	}
	
	public static void main(String[] args) throws Exception {
		// load data
		FeatureSelection ftrSelection = new FeatureSelection();
		System.out.println("Loading data");
		DataSource source = new DataSource("data/train_categoryFixed.arff");
		Instances data = source.getDataSet();
		if (data.classIndex() == -1) {
			data.setClassIndex(data.numAttributes() - 1);
		}

		int[] indices = ftrSelection.useLowLevel(data, 10);
		System.out.println("selected attribute indices:" + Utils.arrayToString(indices));
		System.out.println("selected attributes:");
		for(int i : indices) {
			System.out.println(data.attribute(i));
		}
	}
}
