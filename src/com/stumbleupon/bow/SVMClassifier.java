/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stumbleupon.bow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import utils.DataFileReader;
import ca.uwo.csd.ai.nlp.kernel.KernelManager;
import ca.uwo.csd.ai.nlp.kernel.LinearKernel;
import ca.uwo.csd.ai.nlp.libsvm.svm_model;
import ca.uwo.csd.ai.nlp.libsvm.svm_parameter;
import ca.uwo.csd.ai.nlp.libsvm.ex.Instance;
import ca.uwo.csd.ai.nlp.libsvm.ex.SVMPredictor;
import ca.uwo.csd.ai.nlp.libsvm.ex.SVMTrainer;

public class SVMClassifier {
    String traingFilePath,modelFilePath,predictionFilePath;

    public String getTraingFilePath() {
        return traingFilePath;
    }

    public void setTraingFilePath(String traingFilePath) {
        this.traingFilePath = traingFilePath;
    }

    public String getModelFilePath() {
        return modelFilePath;
    }

    public void setModelFilePath(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public String getPredictionFilePath() {
        return predictionFilePath;
    }

    public void setPredictionFilePath(String predictionFilePath) {
        this.predictionFilePath = predictionFilePath;
    }
    
      public static void testLinearKernel(String[] args) throws IOException, ClassNotFoundException {
        String trainFileName = args[0];
        String testFileName = args[1];
        String outputFileName = args[2];
        String outputFileName1=outputFileName+"1";
        String urlidfile=args[3];
        
        //Read training file
        Instance[] trainingInstances = DataFileReader.readDataFile(trainFileName);        
        
        //Register kernel function
        KernelManager.setCustomKernel(new LinearKernel());        
        
        //Setup parameters
        svm_parameter param = new svm_parameter();                
        
        //Train the model
        System.out.println("Training started...");
        svm_model model = SVMTrainer.train(trainingInstances, param);
        System.out.println("Training completed.");
                
        //Save the trained model
        //SVMTrainer.saveModel(model, "a1a.model");
        //model = SVMPredictor.load_model("a1a.model");
        
        //Read test file
        
        ca.uwo.csd.ai.nlp.libsvm.ex.Instance[] testingInstances = DataFileReader.readDataFile(testFileName);
        //Predict results
        double[] predictions = SVMPredictor.predict(testingInstances, model, true);
        writeOutputs(outputFileName1, predictions);
        //SVMTrainer.doCrossValidation(trainingInstances, param, 10, true);
        //SVMTrainer.doInOrderCrossValidation(trainingInstances, param, 10, true);
        mergeURLidpredictions(outputFileName1,urlidfile,outputFileName);
    }
    
    private static void mergeURLidpredictions(String outputFileName1,
			String urlidfile, String outputFileName) {
		File out=new File(outputFileName1);
		File url=new File(urlidfile);
		File out1=new File(outputFileName);
		try{
		BufferedReader br = new BufferedReader(new FileReader(out));
		BufferedReader br1 = new BufferedReader(new FileReader(url));
		BufferedWriter brout = new BufferedWriter(new FileWriter(out1));
		String urlid,result;
		while((urlid=br1.readLine())!=null &&(result=br.readLine())!=null)
		{
			brout.write(urlid+","+result);
			brout.newLine();
		}
		br1.close();
		br.close();
		brout.close();
		}
		catch(Exception e)
		{
			
		}
		
		
	}

	private static void writeOutputs(String outputFileName, double[] predictions) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        for (double p : predictions) {
            writer.write(String.format("%.0f\n", p));
        }
        writer.close();
    }
    
    private static void showUsage() {
        System.out.println("Demo training-file testing-file output-file");
    }
    
    private static boolean checkArgument(String[] args) {
        return args.length == 4;
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {        
        if (checkArgument(args)) {
            testLinearKernel(args);
        } else {
            showUsage();
        }
    }
}
