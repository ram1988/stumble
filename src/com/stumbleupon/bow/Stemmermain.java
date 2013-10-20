/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stumbleupon.bow;

import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;


public class Stemmermain {
    public static void main(String args[])
    {
       TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");
     
PorterStemmerTokenizerFactory pstf=new PorterStemmerTokenizerFactory(TOKENIZER_FACTORY);
System.out.println();
     
    
    }
}