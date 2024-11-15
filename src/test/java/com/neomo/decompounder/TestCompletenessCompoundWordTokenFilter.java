/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neomo.decompounder;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.tests.analysis.MockTokenizer;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.junit.Ignore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


public class TestCompletenessCompoundWordTokenFilter extends BaseTokenStreamTestCase {

    private static CharArraySet makeDictionary(String... dictionary) {
        return new CharArraySet(Arrays.asList(dictionary), true);
    }


    public void testDumbCompoundWordsSE() throws Exception {
        CharArraySet dict = makeDictionary("Bil", "Dörr", "Motor", "Tak", "Borr", "Slag", "Hammar",
                "Pelar", "Glas", "Ögon", "Fodral", "Bas", "Fiol", "Makare", "Gesäll",
                "Sko", "Vind", "Rute", "Torkare", "Blad");

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict,
                "Bildörr Bilmotor Biltak Slagborr Hammarborr Pelarborr Glasögonfodral Basfiolsfodral " +
                        "Basfiolsfodralmakaregesäll Skomakare Vindrutetorkare Vindrutetorkarblad abba");

        assertTokenStreamContents(tf, new String[]{"Bildörr", "Bil", "dörr", "Bilmotor",
                        "Bil", "motor", "Biltak", "Bil", "tak", "Slagborr", "Slag", "borr",
                        "Hammarborr", "Hammar", "borr", "Pelarborr", "Pelar", "borr",
                        "Glasögonfodral", "Glas", "ögon", "fodral", "Basfiolsfodral", "Bas",
                        "fiol", "fodral", "Basfiolsfodralmakaregesäll", "Bas", "fiol",
                        "fodral", "makare", "gesäll", "Skomakare", "Sko", "makare",
                        "Vindrutetorkare", "Vind", "rute", "torkare", "Vindrutetorkarblad",
                        "abba"}, new int[]{0, 0, 0, 8, 8, 8, 17,
                        17, 17, 24, 24, 24, 33, 33, 33, 44, 44, 44, 54, 54, 54, 54, 69, 69, 69,
                        69, 84, 84, 84, 84, 84, 84, 111, 111, 111, 121, 121, 121, 121, 137, 156},
                new int[]{7, 7, 7, 16, 16, 16, 23, 23, 23, 32,
                        32, 32, 43, 43, 43, 53, 53, 53, 68, 68, 68, 68, 83, 83, 83, 83, 110,
                        110, 110, 110, 110, 110, 120, 120, 120, 136, 136, 136, 136, 155, 160},
                new int[]{1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,
                        0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1,
                        1});
    }

    //This tests documents the desired behaviour. Currently the brute force algorithm looks for the first longest match
    // without looking ahead and assuring that the following characters are covered by dictionary entries too.
    @Ignore
    public void testLongerSubwordImpedesSplitDE() throws Exception {
        CharArraySet dict = makeDictionary("ratgeber", "ratgebers", "seite");

        CompletenessCompoundWordTokenFilter tf =
                completenessCompoundWordTokenFilter(dict,
                        "ratgeberseite ratgebersseite seitenratgeber");

        assertTokenStreamContents(tf, new String[]{"ratgeberseite","ratgeber", "seite",
                "ratgebersseite", "ratgebers", "seite",
                "seitenratgeber", "seite", "ratgeber"});
    }

    public void testCompoundDictionaryEntryIsDeduplicated() throws Exception {
        CharArraySet dict = makeDictionary("kindergarten");

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict,
                "ein kindergarten");

        assertTokenStreamContents(tf, new String[]{"ein", "kindergarten"},
                new int[]{0, 4},
                new int[]{3, 16},
                new int[]{1, 1});
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testGraphModeLeadsToSameTokens(boolean useGraphMode) throws Exception {
        CharArraySet dict = makeDictionary("schnitzelheimer", "risiko", "leben", "versicherung");

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict,
                "die risikolebensversicherung schnitzelheimer", useGraphMode);

        assertTokenStreamContents(tf, new String[]{"die", "risikolebensversicherung", "risiko", "leben", "versicherung",
                        "schnitzelheimer"},
                new int[]{0, 4, 4, 4, 29},
                new int[]{3, 28, 28, 28, 43}
        );
    }

    public void testGraphModeLeadsToTokensSpanningMultiplePositions() throws Exception {
        CharArraySet dict = makeDictionary("schnitzelheimer", "risiko", "leben", "versicherung");

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict,
                "die risikolebensversicherung schnitzelheimer", true);
        tf.reset();
        // use the following approach instead of assertTokenStreamContents because that method resets streams and
        // leads to wrong positionLength values (plus: sometimes does not set the token types)
        ArrayList<Integer> positionIncrements = new ArrayList<>();
        ArrayList<Integer> positionLengths = new ArrayList<>();
        while (tf.incrementToken()) {
            positionIncrements.add(tf.getAttribute(PositionIncrementAttribute.class).getPositionIncrement());
            positionLengths.add(tf.getAttribute(PositionLengthAttribute.class).getPositionLength());
        }
        assertEquals(List.of(1, 1, 0, 1, 1, 1), positionIncrements);
        assertEquals(List.of(1, 3, 1, 1, 1, 1), positionLengths);
    }

    public void testDumbCompoundWordsSELongestMatch() throws Exception {
        CharArraySet dict = makeDictionary("Bil", "Dörr", "Motor", "Tak", "Borr", "Slag", "Hammar",
                "Pelar", "Glas", "Ögon", "Fodral", "Bas", "Fiols", "Makare", "Gesäll",
                "Sko", "Vind", "Rute", "Torkare", "Blad", "Fiolsfodral");

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict,
                "Basfiolsfodralmakaregesäll");

        assertTokenStreamContents(tf, new String[]{"Basfiolsfodralmakaregesäll", "Bas",
                "fiolsfodral", "makare", "gesäll"}, new int[]{0, 0, 0,
                0, 0}, new int[]{26, 26, 26, 26, 26}, new int[]{1, 0, 0, 0, 0});
    }

    public void testTokenEndingWithWordComponentOfMinimumLength() throws Exception {
        CharArraySet dict = makeDictionary("ab", "cd", "ef");

        String inputText = "abcdef";

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict, inputText);

        assertTokenStreamContents(tf,
                new String[]{inputText, "ab", "cd", "ef"},
                new int[]{0, 0, 0, 0},
                new int[]{6, 6, 6, 6},
                new int[]{1, 0, 0, 0}
        );
    }

    public void testWordsAreOnlySplitWhenFullyCoveredByDictionary() throws Exception {
        CharArraySet dict = makeDictionary("sinnen");

        String inputText = "äbtissinnen";

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict, inputText);

        assertTokenStreamContents(tf,
                new String[]{inputText},
                new int[]{0},
                new int[]{11},
                new int[]{1}
        );
    }

    public void testFugenlautNotInDictionary() throws Exception {
        CharArraySet dict = makeDictionary("hase", "tasse");

        String inputText = "Hasentasse";

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict, inputText);

        assertTokenStreamContents(tf,
                new String[]{inputText, "Hase", "tasse"},
                new int[]{0, 0, 0},
                new int[]{10, 10, 10},
                new int[]{1, 0, 0}
        );
    }

    public void testSingleSubwordIsNotEmitted() throws Exception {
        CharArraySet dict = makeDictionary("äbtissin");

        String inputText = "äbtissinnen";

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict, inputText);

        assertTokenStreamContents(tf,
                new String[]{inputText}
        );
    }

    public void testSubwordsofSubwordsAreNotEmitted() throws IOException {
        CharArraySet dict = makeDictionary("aal", "räucherei", "che", "her");

        String inputText = "aalräucherei";

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict, inputText);

        assertTokenStreamContents(tf,
                new String[]{inputText, "aal", "räucherei"},
                new int[]{0, 0, 0},
                new int[]{12, 12, 12},
                new int[]{1, 0, 0}
        );

    }

    public void testWordComponentWithLessThanMinimumLength() throws Exception {
        CharArraySet dict = makeDictionary("abc", "d", "efg");

        String inputText = "abcdefg";

        CompletenessCompoundWordTokenFilter tf = completenessCompoundWordTokenFilter(dict, inputText);

        // since "d" is shorter than the minimum subword size, it should not be added to the token stream
        assertTokenStreamContents(tf,
                new String[]{inputText, "abc", "efg"},
                new int[]{0, 0, 0, 0},
                new int[]{7, 7, 7, 7},
                new int[]{1, 0, 0, 0}
        );
    }

    private CompletenessCompoundWordTokenFilter completenessCompoundWordTokenFilter(CharArraySet dict,
                                                                                    String inputText,
                                                                                    boolean useGraphMode)
            throws IOException {
        Tokenizer tokenizer = whitespaceMockTokenizer(inputText);
        return new CompletenessCompoundWordTokenFilter(
                tokenizer,
                dict,
                CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
                CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
                CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
                true,
                useGraphMode);
    }

    private CompletenessCompoundWordTokenFilter completenessCompoundWordTokenFilter(CharArraySet dict,
                                                                                    String inputText)
            throws IOException {
        return completenessCompoundWordTokenFilter(dict, inputText, false);
    }


    public void testReset() throws Exception {
        CharArraySet dict = makeDictionary("Rind", "Fleisch", "Draht", "Schere", "Gesetz",
                "Aufgabe", "Überwachung");

        MockTokenizer wsTokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        wsTokenizer.setEnableChecks(false); // we will reset in a strange place
        wsTokenizer.setReader(new StringReader("Rindfleischüberwachungsgesetz"));
        CompletenessCompoundWordTokenFilter tf = new CompletenessCompoundWordTokenFilter(
                wsTokenizer, dict,
                CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
                CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
                CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
                false,
                false
        );

        CharTermAttribute termAtt = tf.getAttribute(CharTermAttribute.class);
        tf.reset();
        assertTrue(tf.incrementToken());
        assertEquals("Rindfleischüberwachungsgesetz", termAtt.toString());
        assertTrue(tf.incrementToken());
        assertEquals("Rind", termAtt.toString());
        tf.end();
        tf.close();
        wsTokenizer.setReader(new StringReader("Rindfleischüberwachungsgesetz"));
        tf.reset();
        assertTrue(tf.incrementToken());
        assertEquals("Rindfleischüberwachungsgesetz", termAtt.toString());
    }

    public void testRetainMockAttribute() throws Exception {
        CharArraySet dict = makeDictionary("abc", "d", "efg");

        String inputText = "abcdefg";
        Tokenizer tokenizer = whitespaceMockTokenizer(inputText);
        TokenStream stream = new MockRetainAttributeFilter(tokenizer);
        stream = new CompletenessCompoundWordTokenFilter(
                stream, dict,
                CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE,
                CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE,
                CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE,
                false,
                false
        );
        MockRetainAttribute retAtt = stream.addAttribute(MockRetainAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            assertTrue("Custom attribute value was lost", retAtt.getRetain());
        }

    }

    public interface MockRetainAttribute extends Attribute {
        void setRetain(boolean attr);

        boolean getRetain();
    }

    public static final class MockRetainAttributeImpl extends AttributeImpl implements MockRetainAttribute {
        private boolean retain = false;

        @Override
        public void clear() {
            retain = false;
        }

        @Override
        public boolean getRetain() {
            return retain;
        }

        @Override
        public void setRetain(boolean retain) {
            this.retain = retain;
        }

        @Override
        public void copyTo(AttributeImpl target) {
            MockRetainAttribute t = (MockRetainAttribute) target;
            t.setRetain(retain);
        }

        @Override
        public void reflectWith(AttributeReflector reflector) {
            reflector.reflect(MockRetainAttribute.class, "retain", retain);
        }
    }

    private static class MockRetainAttributeFilter extends TokenFilter {

        MockRetainAttribute retainAtt = addAttribute(MockRetainAttribute.class);

        MockRetainAttributeFilter(TokenStream input) {
            super(input);
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (input.incrementToken()) {
                retainAtt.setRetain(true);
                return true;
            } else {
                return false;
            }
        }
    }

    // SOLR-2891
    // *CompoundWordTokenFilter blindly adds term length to offset, but this can take things out of bounds
    // wrt original text if a previous filter increases the length of the word (in this case ü -> ue)
    // so in this case we behave like WDF, and preserve any modified offsets
    public void testInvalidOffsets() throws Exception {
        final CharArraySet dict = makeDictionary("bank", "ueberfall");
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        builder.add("ü", "ue");
        final NormalizeCharMap normMap = builder.build();

        Analyzer analyzer = new Analyzer() {

            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
                TokenFilter filter = new CompletenessCompoundWordTokenFilter(tokenizer, dict);
                return new TokenStreamComponents(tokenizer, filter);
            }

            @Override
            protected Reader initReader(String fieldName, Reader reader) {
                return new MappingCharFilter(normMap, reader);
            }
        };

        assertAnalyzesTo(analyzer, "banküberfall",
                new String[]{"bankueberfall", "bank", "ueberfall"},
                new int[]{0, 0, 0},
                new int[]{12, 12, 12});
        analyzer.close();
    }

}
