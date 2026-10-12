package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.DTO.ExecutionSummaryResponse;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ------------------------------------------------------------------ CSV

    public byte[] toCsv(List<ExecutionSummaryResponse> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM para Excel
        sb.append("ID,Fecha,Estrategia,Base de datos,Tipo,Origen,Inicio,Fin,Duración (s),Resultado,Archivada\r\n");
        for (ExecutionSummaryResponse r : rows) {
            sb.append(r.id()).append(',')
                    .append(date(r.startTime())).append(',')
                    .append(text(r.strategyName())).append(',')
                    .append(text(r.databaseName())).append(',')
                    .append(r.backupType()).append(',')
                    .append(r.origin()).append(',')
                    .append(time(r.startTime())).append(',')
                    .append(time(r.endTime())).append(',')
                    .append(r.durationSeconds() == null ? "" : r.durationSeconds()).append(',')
                    .append(r.status()).append(',')
                    .append(r.archived() ? "Sí" : "No")
                    .append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Escapa comas, comillas y saltos de línea, y neutraliza las fórmulas de Excel. */
    private String text(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    // ------------------------------------------------------------------ PDF

    public byte[] toPdf(List<ExecutionSummaryResponse> rows, String subtitle) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 32, 28);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.NORMAL, Color.BLACK);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.NORMAL, Color.WHITE);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            Font bodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.NORMAL, Color.BLACK);

            doc.add(new Paragraph("Historial de respaldos", title));
            doc.add(new Paragraph("Generado el " + date(LocalDateTime.now()) + " a las "
                    + time(LocalDateTime.now()), small));
            if (subtitle != null && !subtitle.isBlank()) {
                doc.add(new Paragraph(subtitle, small));
            }
            doc.add(new Paragraph(summaryLine(rows), small));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{10, 22, 14, 12, 12, 9, 9, 10, 12});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String h : new String[]{"Fecha", "Estrategia", "Base de datos", "Tipo", "Origen",
                    "Inicio", "Fin", "Duración", "Resultado"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, head));
                cell.setBackgroundColor(new Color(33, 37, 41));
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (ExecutionSummaryResponse r : rows) {
                table.addCell(cell(date(r.startTime()), body, null));
                table.addCell(cell(r.strategyName(), body, null));
                table.addCell(cell(r.databaseName(), body, null));
                table.addCell(cell(String.valueOf(r.backupType()), body, null));
                table.addCell(cell(String.valueOf(r.origin()), body, null));
                table.addCell(cell(time(r.startTime()), body, null));
                table.addCell(cell(time(r.endTime()), body, null));
                table.addCell(cell(duration(r.durationSeconds()), body, null));
                PdfPCell result = cell(String.valueOf(r.status()), bodyBold, statusColor(r.status()));
                result.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(result);
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private PdfPCell cell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(4);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        return cell;
    }

    private Color statusColor(ExecutionStatus status) {
        return switch (status) {
            case EXITOSO -> new Color(198, 239, 206);
            case ERROR -> new Color(255, 199, 206);
            case ADVERTENCIA -> new Color(255, 235, 156);
            case OMITIDO -> new Color(217, 217, 217);
            case EN_EJECUCION -> new Color(189, 215, 238);
        };
    }

    private String summaryLine(List<ExecutionSummaryResponse> rows) {
        long ok = rows.stream().filter(r -> r.status() == ExecutionStatus.EXITOSO).count();
        long warn = rows.stream().filter(r -> r.status() == ExecutionStatus.ADVERTENCIA).count();
        long err = rows.stream().filter(r -> r.status() == ExecutionStatus.ERROR).count();
        long skipped = rows.stream().filter(r -> r.status() == ExecutionStatus.OMITIDO).count();
        return "Total: " + rows.size() + "   Exitosos: " + ok + "   Con advertencias: " + warn
                + "   Fallidos: " + err + "   Omitidos: " + skipped;
    }

    // ------------------------------------------------------------------ formato

    private String date(LocalDateTime t) {
        return t == null ? "" : t.format(DATE);
    }

    private String time(LocalDateTime t) {
        return t == null ? "" : t.format(TIME);
    }

    private String duration(Long seconds) {
        if (seconds == null) return "";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return h > 0 ? String.format("%dh %02dm", h, m) : String.format("%dm %02ds", m, s);
    }
}