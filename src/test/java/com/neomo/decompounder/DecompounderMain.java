package com.neomo.decompounder;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.junit.Ignore;

import java.io.Reader;
import java.io.StringReader;


public class DecompounderMain extends BaseTokenStreamFactoryTestCase {

    @Ignore
    public void testDictionarySplits() throws Exception {
        Reader reader = new StringReader("fuselfasel stängelkohlsalz unda finderhälftes kitafinder");
        TokenStream stream = whitespaceMockTokenizer(reader);
        stream = tokenFilterFactory("completenessCompoundWord",
                "dictionary", "de-dictionary.dic", "minSubwordSize", "3", "onlyLongestMatch", "true",
                "useGraphMode", "true")
                .create(stream);
        stream.reset();
        int position = 0;
        while(stream.incrementToken()) {
            CharTermAttribute termAttribute = stream.getAttribute(CharTermAttribute.class);
            position += stream.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
            int positionLength = stream.getAttribute(PositionLengthAttribute.class).getPositionLength();
            System.out.println(termAttribute + "|" +  position + "|" + positionLength);
        }
    }
}