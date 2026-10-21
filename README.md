# Decompounder

A Lucene/Solr token filter that splits compound words — as found in German and other
Germanic languages — into their parts, so that a document containing
`Risikolebensversicherung` can be found by searching for `Versicherung`.

```
Risikolebensversicherung → Risikolebensversicherung, Risiko, Leben, Versicherung
```

The filter is a fork of Lucene's `DictionaryCompoundWordTokenFilter` with one decisive
addition: **a split is only accepted if the dictionary parts cover the whole word.**
Lucene's original filter happily emits any dictionary word it finds somewhere inside a
token, which produces nonsense matches — `Äbtissinnen` would be indexed as `sinnen`
("to ponder"). This filter discards such partial splits and keeps the original token
untouched instead.

## Installation

Add the jar to Solr's classpath, e.g. by dropping it into `server/solr/lib` or a
`lib/` directory of your core, or by declaring it in `solrconfig.xml`:

```xml
<lib dir="${solr.install.dir}/contrib/decompounder/lib" regex=".*\.jar" />
```

The artifact is published to a private Maven repository:

```kotlin
repositories {
    maven("artifactregistry://europe-maven.pkg.dev/contentsurfacingservice/public")
}

dependencies {
    implementation("com.neomo:decompounder:1.2.6")
}
```

### Version compatibility

The filter links against Lucene's analysis API, so its version has to match the Lucene
version of your Solr installation.

| Decompounder | Lucene   | Solr   | Java |
|--------------|----------|--------|------|
| 1.2.6        | 9.12.3   | 9.10   | 17   |
| 1.2.5        | 9.12.3   | 9.10   | 21   |
| 1.2.4        | 10.3.1   | 10.0.0 | 21   |

## Usage in Solr

The filter registers itself via SPI under the name `completenessCompoundWord`, so no
class name is needed in `schema.xml`:

```xml
<fieldType name="text_de" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <tokenizer name="standard"/>
    <filter name="lowercase"/>
    <filter name="completenessCompoundWord"
            dictionary="lang/de-dictionary.dic"
            minWordSize="5"
            minSubwordSize="3"
            maxSubwordSize="15"
            onlyLongestMatch="true"
            useGraphMode="false"/>
  </analyzer>
</fieldType>
```

### Parameters

| Parameter          | Default  | Description                                                                    |
|--------------------|----------|--------------------------------------------------------------------------------|
| `dictionary`       | required | Resource path of the word list, resolved by Solr's `ResourceLoader`.             |
| `minWordSize`      | `5`      | Tokens shorter than this are passed through without any attempt to split them.   |
| `minSubwordSize`   | `2`      | Parts shorter than this are not looked up either, so they count as a gap.        |
| `maxSubwordSize`   | `15`     | Parts longer than this are not looked up in the dictionary.                       |
| `onlyLongestMatch` | `true`   | Keep only the longest dictionary match per position (see below).                  |
| `useGraphMode`     | `false`  | Place the parts on consecutive positions instead of stacking them (see below).    |

An unknown parameter makes the factory throw at startup rather than being ignored
silently. If the dictionary file turns out to be empty, the filter removes itself from
the chain and passes the stream through unchanged.

### Dictionary format

A plain text file with one word per line, UTF-8, comments starting with `#`. Lookups are
case-insensitive, so a lowercase word list is enough. There is a German list with roughly
106,000 word forms in
[`src/test/resources/com/neomo/decompounder/de-dictionary.dic`](src/test/resources/com/neomo/decompounder/de-dictionary.dic)
which also serves as the test fixture.

The dictionary should contain the *parts* you want to split on, not the compounds
themselves. Entries that happen to be compounds do no harm — a token matching one of them
as a whole is emitted once, not twice.

## How it works

### 1. Brute-force scan

Starting at the first character, the filter looks up every substring between
`minSubwordSize` and `maxSubwordSize` characters in the dictionary. With
`onlyLongestMatch=true` the longest hit at a position wins and the scan continues *after*
it; otherwise every hit is emitted and the scan advances one character at a time, which
also yields parts of parts.

### 2. Completeness check

This is what sets the filter apart from Lucene's. The parts found must account for the
whole token, except for up to **two characters per seam** — the budget for German linking
morphemes (*Fugenlaute*) such as `-s-`, `-n-`, `-es-` and `-en-`:

```
Hasentasse → Hase + tasse            accepted (the "n" sits in the seam)
Äbtissinnen → sinnen                 rejected ("Äbtis" is unaccounted for)
```

If the parts do not cover the token, all of them are dropped and only the original token
remains. A single part spanning the entire token (`Kindergarten`, when that word is in the
dictionary) is dropped as well, since it would merely duplicate the original.

### 3. Token positions

The original token is always emitted first and is never modified. All parts inherit the
original token's offsets rather than computing their own, so a preceding char filter that
changes the token's length (`ü` → `ue`) cannot push offsets out of bounds
([SOLR-2891](https://issues.apache.org/jira/browse/SOLR-2891)).

How the parts are positioned depends on `useGraphMode`:

**`useGraphMode=false`** (default, Lucene's behaviour) — all parts get a position
increment of `0` and are stacked onto the position of the original token:

```
position:  1     2
           die   risikolebensversicherung
                 risiko
                 leben
                 versicherung
```

Term queries work, but a phrase query for `"leben versicherung"` does not match, because
both terms sit on the same position.

**`useGraphMode=true`** — the parts occupy consecutive positions and the original token is
marked as spanning them via `positionLength`:

```
position:  1     2             3       4
           die   risikolebens… (len 3)
                 risiko        leben   versicherung
```

Phrase queries across the parts now match. Note that Lucene stores position increments
but *not* position lengths in the index, so when the filter runs in the index analyzer the
span information is lost: a phrase query combining the full compound with the word
following it can then fail to match. Using graph mode in the query analyzer avoids this.

## Building

Requires a JDK 17 toolchain; the Gradle wrapper is included.

```sh
./gradlew test       # run the test suite
./gradlew build      # build the jar
./gradlew publish    # publish to the Maven repository configured in build.gradle
```

The build resolves its version catalog from a private Artifact Registry, so publishing —
and a clean first build — need credentials for
`europe-maven.pkg.dev/contentsurfacingservice/public`.

[`DecompounderMain`](src/test/java/com/neomo/decompounder/DecompounderMain.java) is an
`@Ignore`d test that prints the splits of a given input against the full German
dictionary; handy for trying out words by hand.

## Known limitations

The scan is greedy and has no backtracking: it takes the longest match at each position
without checking whether the remainder can still be covered. A longer dictionary entry can
therefore block an otherwise valid split, which the completeness check then rejects
altogether. The ignored test `testLongerSubwordImpedesSplitDE` documents the intended
behaviour for this case.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Derived from Apache Lucene's
`org.apache.lucene.analysis.compound` package.
