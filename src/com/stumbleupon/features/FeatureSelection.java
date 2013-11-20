package com.stumbleupon.features;

import weka.attributeSelection.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.*;

public class FeatureSelection {
	public int[] getSelectedIndices(Instances data, int numAttr) throws Exception {
		System.out.println("Analyzing attributes");
		AttributeSelection attrSel = new AttributeSelection();
		InfoGainAttributeEval eval = new InfoGainAttributeEval();
		//GainRatioAttributeEval eval = new GainRatioAttributeEval();
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
	
	public String[] getSelectedAttributes(Instances data, int numAttr) throws Exception {
		//get attribute names
		String[] attributeNames = new String[data.numAttributes()];
		for(int i = 0; i < data.numAttributes(); i++) {
			attributeNames[i] = data.attribute(i).name();
		}
		
		// select attributes
		AttributeSelection attrSel = new AttributeSelection();
		InfoGainAttributeEval eval = new InfoGainAttributeEval();
		Ranker rnkr = new Ranker();
		attrSel.setEvaluator(eval);
		attrSel.setSearch(rnkr);
		attrSel.setRanking(true);
		attrSel.SelectAttributes(data);
		int[] rankedIndices = attrSel.selectedAttributes();
		int[] selectedIndices = new int[numAttr + 1];
		String[] selectedAttributes = new String[selectedIndices.length];
		for(int i = 0; i < numAttr; i++) {
			selectedIndices[i] = rankedIndices[i];
			selectedAttributes[i] = attributeNames[selectedIndices[i]];
		}
		selectedIndices[numAttr] = data.classIndex();
		selectedAttributes[numAttr] = attributeNames[selectedIndices[numAttr]];
		return selectedAttributes;
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

		int[] indices = ftrSelection.getSelectedIndices(data, 10);
		System.out.println("selected attribute indices:" + Utils.arrayToString(indices));
		System.out.println("selected attributes:");
		for(int i : indices) {
			System.out.println(data.attribute(i).name());
		}
		
		String[] attributes = ftrSelection.getSelectedAttributes(data, 10);
		for(String s : attributes) {
			System.out.println(s);
		}
	}
}
