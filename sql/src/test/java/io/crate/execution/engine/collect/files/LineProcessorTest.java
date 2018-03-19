package io.crate.execution.engine.collect.files;

import io.crate.execution.dsl.phases.FileUriCollectPhase;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import static io.crate.execution.dsl.phases.FileUriCollectPhase.InputFormat.CSV;
import static io.crate.execution.dsl.phases.FileUriCollectPhase.InputFormat.JSON;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class LineProcessorTest {

    private LineProcessor subjectUnderTest;
    private URI uri;
    private FileUriCollectPhase.InputFormat inputFormat;
    private BufferedReader bufferedReader;



    @Before
    public void setup() {
        subjectUnderTest  = new LineProcessor();
    }

    @Test
    public void readFirstLine_givenFileExtensionIsCsv_thenReadsFile() throws URISyntaxException, IOException {
        givenURI();
        givenURIWithExtension("file.csv");
        givenBufferedReader();

        whenCalledWith(uri, inputFormat, bufferedReader);

        thenLineHasBeenRead();
    }

    @Test
    public void readFirstLine_givenFileFormatIsCsv_thenReadsFile() throws URISyntaxException, IOException {
        givenBufferedReader();
        givenInputFormatIs(CSV);

        whenCalledWith(uri, inputFormat, bufferedReader);

        thenLineHasBeenRead();
    }

    @Test
    public void readFirstLine_givenFileExtensionIsJson_thenReadsFile() throws URISyntaxException, IOException {
        givenURIWithExtension("file.json");
        givenBufferedReader();

        whenCalledWith(uri, inputFormat, bufferedReader);

        thenLineHasNotBeenRead();
    }

    @Test
    public void readFirstLine_givenFileFormatIsJson_thenReadsFile() throws URISyntaxException, IOException {
        givenBufferedReader();
        givenURI();
        givenInputFormatIs(JSON);

        whenCalledWith(uri, inputFormat, bufferedReader);

        thenLineHasNotBeenRead();
    }

    private void givenBufferedReader() {
        Reader reader = new StringReader("some/string");
        bufferedReader = new BufferedReader(reader);
    }

    private void givenURI() throws URISyntaxException {
        uri = new URI ("some.uri");
    }

    private void givenURIWithExtension(String fileName) throws URISyntaxException {
        uri = new URI (fileName);
    }

    private void givenInputFormatIs(FileUriCollectPhase.InputFormat format) {
        inputFormat = format;
    }

    private void whenCalledWith(URI uri, FileUriCollectPhase.InputFormat inputFormat, BufferedReader bufferedReader) throws IOException {
        subjectUnderTest.readFirstLine(uri, inputFormat, bufferedReader);
    }

    private void whenCalledWith(String line, FileUriCollectPhase.InputFormat inputFormat, URI uri) throws IOException {
        subjectUnderTest.process(line, inputFormat, uri);
    }


    private void thenLineHasBeenRead() throws IOException {
        assertThat(bufferedReader.readLine(), is(nullValue()));
    }

    private void thenLineHasNotBeenRead() throws IOException {
        assertThat(bufferedReader.readLine(), is("some/string"));
    }

}
