package com.tejas.transactionservice.utils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.tejas.transactionservice.models.TransactionLedgerRecord;

@Service
public class PdfStatementGenerator {
	public byte[] generatePdfStatement(String accountNumber, LocalDateTime from, LocalDateTime to, List<TransactionLedgerRecord> tx) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		try(PdfWriter writer = new PdfWriter(out);
	     PdfDocument pdfDoc = new PdfDocument(writer);
	     Document document = new Document(pdfDoc)) {
			
			document.add(new Paragraph("Account Statement")
					.setBold()
					.setFontSize(18));
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
			Paragraph header = new Paragraph(
				    "Transaction Report\n" +
				    "From: " + from.format(formatter) + "\n" +
				    "To:   " + to.format(formatter)
			);
			
			header.setTextAlignment(TextAlignment.CENTER);
			document.add(header);
			
		    document.add(new Paragraph("\n"));
		    
		    float[] columnWidths = {1, 2, 1, 1, 2, 1, 2};
		    Table table = new Table(UnitValue.createPercentArray(columnWidths))
		    		.useAllAvailableWidth();
		    
		    table.addCell("Date & Time");
		    table.addCell("Type");
		    table.addCell("Counterparty");
		    table.addCell("Amount");
		    table.addCell("Balance After");
		    table.addCell("Status");
		    table.addCell("Txn ID");
		    
		    for (TransactionLedgerRecord r: tx) {
		    	table.addCell(r.getCreatedAt().format(formatter));
		        table.addCell(r.getType().toString());
		        table.addCell(r.getCounterparty());
		        table.addCell(String.valueOf(r.getAmount()));
		        table.addCell(String.valueOf(r.getBalanceAfter()));
		        table.addCell(r.getStatus().replace("_", ""));
		        table.addCell(String.valueOf(r.getParentTransactionId()));
		    }
		    
		    document.add(table);
		    document.close();
		    return out.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return out.toByteArray();		
	}
}