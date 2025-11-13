package edu.dut.processor;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.*;

/**
 * PDF Converter using Docx4j library
 */
public class PDFConverter {
    
    public PDFConverter() {
        System.out.println("✓ PDF Converter initialized using Docx4j + Apache FOP");
    }
    
    /**
     * Convert Word document to PDF
     * @param inputFile Word file
     * @param outputFile PDF file
     * @throws Exception if conversion fails
     */
    public void convertToPDF(File inputFile, File outputFile) throws Exception {
        if (!inputFile.exists()) {
            throw new Exception("Input file does not exist: " + inputFile.getAbsolutePath());
        }
        
        OutputStream out = null;
        
        try {
            // Load Word document
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(inputFile);
            
            // Generate PDF directly
            out = new FileOutputStream(outputFile);
            Docx4J.toPDF(wordMLPackage, out);
            
            System.out.println("✓ Converted: " + inputFile.getName() + " → " + outputFile.getName());
            
        } catch (Exception e) {
            throw new Exception("Conversion failed: " + e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
    
    public void shutdown() {
        // No cleanup needed
    }
}