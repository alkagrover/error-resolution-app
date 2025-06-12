package com.ai.hackathon.error_resolution_app.common;

import org.springframework.ai.document.Document;
import org.springframework.ai.model.Media;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.util.List;


public class CommonHelper {

    public static final String URL_PREFIX_FOR_SPRING = "/ai/spring/";

    public static final String OUTPUT_NEW_LINE = "\n\n<br><br>";
    public static final String HORIZONTAL_LINE = " --------------------------------------------------------- ";

    public static String surroundMessage(Class aClass, String input, String output){
        return surroundMessageSection( " HACKATHON " , aClass.getSimpleName() )+
                surroundMessageSection( " INPUT " , input )+
                surroundMessageSection( " OUTPUT " , output );
    }

    public static String surroundMessageSection(String title, String sectionData){
        return   HORIZONTAL_LINE + title + HORIZONTAL_LINE
                + OUTPUT_NEW_LINE
                + "<span style=\"white-space: pre-line\">"+sectionData+"</span>"
                + OUTPUT_NEW_LINE;
    }

    public static List<Document> getDocuments(String resourceUrl) {
        // Step 1: Initialize the document reader for PDFs
        PagePdfDocumentReader documentReader = new PagePdfDocumentReader(resourceUrl);
        // Step 2: Read and transform the document into a list of Document objects
        List<Document> documents = documentReader.read();
        return documents;
    }

    public static Media getImageDocument(String resourceUrl) throws IOException {

        Resource resource = new ClassPathResource(resourceUrl);
        return Media.builder().name("exception_pic").data(resource).mimeType(MimeType.valueOf("image/png")).build();

    }
}
