package com.searchgears.decompounder;

import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Ignore;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

@Ignore
public class DecompounderMain extends BaseTokenStreamFactoryTestCase {
    public void testDictionarySplits() throws Exception {
        Reader reader = new FileReader("/Users/jsprivat/searchgears/vlb/testinput");
        TokenStream stream = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        ((Tokenizer) stream).setReader(reader);
        stream = tokenFilterFactory("completenessCompoundWord",
                "dictionary", "de-dictionary.dic", "minSubwordSize", "3", "onlyLongestMatch", "true")
                .create(stream);
        stream.reset();
        while(stream.incrementToken()) {
            CharTermAttribute attre = stream.getAttribute(CharTermAttribute.class);
            System.out.println(attre);
        }
    }
}