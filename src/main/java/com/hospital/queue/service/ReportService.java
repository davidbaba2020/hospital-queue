package com.hospital.queue.service;

import com.hospital.queue.domain.entity.QueueEntry;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final QueueService   queueService;
    private final EmailService   emailService;

    // ─── Excel Report ─────────────────────────────────────────────────────────

    public byte[] generateExcelReport(LocalDateTime from, LocalDateTime to) {
        List<QueueEntry> entries = queueService.findByDateRange(from, to);
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Queue Report");

            // — Styles —
            XSSFCellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x1a,(byte)0x56,(byte)0x76}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            XSSFCellStyle altRow = wb.createCellStyle();
            altRow.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xf0,(byte)0xf7,(byte)0xff}, null));
            altRow.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            // — Title row —
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("🏥 Hospital Queue Management — Daily Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            // — Date range row —
            Row rangeRow = sheet.createRow(1);
            rangeRow.createCell(0).setCellValue(
                    "Period: " + from.format(DT_FMT) + " → " + to.format(DT_FMT));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            // — Blank row —
            sheet.createRow(2);

            // — Header row —
            String[] headers = {"#","Ticket","Patient","Patient No.","Department",
                                 "Doctor","Priority","Status","Registered At","Completed At"};
            Row hRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // — Data rows —
            int rowNum = 4;
            int idx    = 1;
            for (QueueEntry e : entries) {
                Row row = sheet.createRow(rowNum++);
                if (idx % 2 == 0) {
                    for (int c = 0; c < 10; c++) row.createCell(c).setCellStyle(altRow);
                }
                setCell(row, 0, String.valueOf(idx++));
                setCell(row, 1, e.getTicketNumber());
                setCell(row, 2, e.getPatient().getFullName());
                setCell(row, 3, e.getPatient().getPatientNumber());
                setCell(row, 4, e.getDepartment().getName());
                setCell(row, 5, e.getDoctor() != null ? e.getDoctor().getUser().getFullName() : "—");
                setCell(row, 6, e.getPriority().name());
                setCell(row, 7, e.getStatus().name());
                setCell(row, 8, e.getRegisteredAt() != null ? e.getRegisteredAt().format(DT_FMT) : "—");
                setCell(row, 9, e.getCompletedAt()  != null ? e.getCompletedAt().format(DT_FMT)  : "—");
            }

            // — Summary row —
            sheet.createRow(rowNum++);
            Row sumRow = sheet.createRow(rowNum);
            sumRow.createCell(0).setCellValue("Total Records: " + entries.size());

            // — Auto-size columns —
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            // — Freeze header —
            sheet.createFreezePane(0, 4);

            wb.write(out);
            log.info("Excel report generated: {} rows", entries.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    // ─── PDF Report ───────────────────────────────────────────────────────────

    public byte[] generatePdfReport(LocalDateTime from, LocalDateTime to) {
        List<QueueEntry> entries = queueService.findByDateRange(from, to);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            // Header/footer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    PdfContentByte cb = w.getDirectContent();
                    BaseFont bf;
                    try { bf = BaseFont.createFont(); } catch (Exception ex) { return; }
                    cb.beginText();
                    cb.setFontAndSize(bf, 8);
                    cb.showTextAligned(Element.ALIGN_CENTER,
                            "Hospital Queue Management System — Confidential",
                            d.getPageSize().getWidth() / 2, 20, 0);
                    cb.showTextAligned(Element.ALIGN_RIGHT,
                            "Page " + w.getPageNumber(),
                            d.getPageSize().getRight(36), 20, 0);
                    cb.endText();
                }
            });

            doc.open();

            // — Title —
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new java.awt.Color(0x1a, 0x56, 0x76));
            Paragraph title = new Paragraph("🏥 Hospital Queue Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            Font subFont = new Font(Font.HELVETICA, 10, Font.NORMAL, java.awt.Color.GRAY);
            Paragraph sub = new Paragraph("Period: " + from.format(DT_FMT) + " — " + to.format(DT_FMT)
                    + "   |   Generated: " + LocalDateTime.now().format(DT_FMT), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(16);
            doc.add(sub);

            // — Summary box —
            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(60);
            summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            summaryTable.setSpacingAfter(20);
            addSummaryCell(summaryTable, "Total Records", String.valueOf(entries.size()));
            long completed = entries.stream().filter(e -> e.getStatus().name().equals("COMPLETED")).count();
            addSummaryCell(summaryTable, "Completed", String.valueOf(completed));
            addSummaryCell(summaryTable, "Pending", String.valueOf(entries.size() - completed));
            doc.add(summaryTable);

            // — Data table —
            PdfPTable table = new PdfPTable(new float[]{0.5f,2f,3f,2f,2.5f,2f,1.5f,1.5f,2.5f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);

            String[] headers = {"#","Ticket","Patient","Dept","Doctor","Priority","Status","Registered","Completed"};
            java.awt.Color headerBg  = new java.awt.Color(0x1a, 0x56, 0x76);
            java.awt.Color altBg     = new java.awt.Color(0xf0, 0xf7, 0xff);
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, java.awt.Color.WHITE);
            Font dataFont   = new Font(Font.HELVETICA, 7, Font.NORMAL);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(java.awt.Color.WHITE);
                table.addCell(cell);
            }

            int i = 1;
            for (QueueEntry e : entries) {
                java.awt.Color bg = (i % 2 == 0) ? altBg : java.awt.Color.WHITE;
                addDataCell(table, String.valueOf(i++), dataFont, bg);
                addDataCell(table, e.getTicketNumber(), dataFont, bg);
                addDataCell(table, e.getPatient().getFullName(), dataFont, bg);
                addDataCell(table, e.getDepartment().getName(), dataFont, bg);
                addDataCell(table, e.getDoctor() != null ? e.getDoctor().getUser().getFullName() : "—", dataFont, bg);
                addDataCell(table, e.getPriority().name(), dataFont, bg);
                addDataCell(table, e.getStatus().name(), dataFont, bg);
                addDataCell(table, e.getRegisteredAt() != null ? e.getRegisteredAt().format(DT_FMT) : "—", dataFont, bg);
                addDataCell(table, e.getCompletedAt()  != null ? e.getCompletedAt().format(DT_FMT) : "—",  dataFont, bg);
            }
            doc.add(table);
            doc.close();

            log.info("PDF report generated: {} rows", entries.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    public void emailReport(String toEmail, String toName, String format,
                            LocalDateTime from, LocalDateTime to) {
        byte[] bytes;
        String filename;
        if ("excel".equalsIgnoreCase(format)) {
            bytes    = generateExcelReport(from, to);
            filename = "queue-report-" + LocalDate.now().format(D_FMT) + ".xlsx";
        } else {
            bytes    = generatePdfReport(from, to);
            filename = "queue-report-" + LocalDate.now().format(D_FMT) + ".pdf";
        }
        emailService.sendReportEmail(toEmail, toName, bytes, filename, format.toUpperCase() + " Report");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void setCell(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private void addSummaryCell(PdfPTable t, String label, String value) {
        Font lf = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.GRAY);
        Font vf = new Font(Font.HELVETICA, 14, Font.BOLD, new java.awt.Color(0x1a,0x56,0x76));
        PdfPCell cell = new PdfPCell();
        cell.addElement(new Phrase(label, lf));
        cell.addElement(new Phrase(value, vf));
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(new java.awt.Color(0xdd,0xee,0xff));
        t.addCell(cell);
    }

    private void addDataCell(PdfPTable t, String value, Font font, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setBorderColor(new java.awt.Color(0xdd,0xdd,0xdd));
        t.addCell(cell);
    }
}
