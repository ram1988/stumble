package com.stumbleupon.classifier;

import java.util.List;
import java.util.Random;

import com.stumbleupon.features.FeatureGenerator;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public abstract class Classifiers {
	
	public abstract void trainClassifier(List<List<Object>> features, String[] attributeNames) throws BuildModelException;
	public abstract EvalResult testClassifier(List<List<Object>> features) throws EvaluationException;
	
	public void evaluatebyCrossFold(int numFolds,Instances trainData,Classifier cls ) throws Exception {
		 for (int i = 0; i < 2; i++) {
		      // randomize data
		      int seed = i + 1;
		      Random rand = new Random(seed);
		      Instances randData = new Instances(trainData);
		      randData.randomize(rand);
		      if (randData.classAttribute().isNominal())
		        randData.stratify(numFolds);

		      Evaluation eval = new Evaluation(randData);
		      for (int n = 0; n < numFolds; n++) {
		        Instances train = randData.trainCV(numFolds, n);
		        Instances test = randData.testCV(numFolds, n);
		        // the above code is used by the StratifiedRemoveFolds filter, the
		        // code below by the Explorer/Experimenter:
		        // Instances train = randData.trainCV(folds, n, rand);

		        // build and evaluate classifier
		        Classifier clsCopy = Classifier.makeCopy(cls);
		        clsCopy.buildClassifier(train);
		        eval.evaluateModel(clsCopy, test);
		      }

		      // output evaluation
		      System.out.println();
		      System.out.println("=== Setup run " + (i+1) + " ===");
		      System.out.println("Classifier: " + cls.getClass().getName() );
		      System.out.println("Dataset: " + trainData.relationName());
		      System.out.println("Folds: " + numFolds);
		      System.out.println("Seed: " + seed);
		      System.out.println();
		      System.out.println(eval.toSummaryString("=== " + numFolds + "-fold Cross-validation run " + (i+1) + "===", false));
		    }
		  }
	}
	

