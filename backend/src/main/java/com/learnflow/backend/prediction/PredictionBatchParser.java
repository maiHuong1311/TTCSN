package com.learnflow.backend.prediction;

import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.prediction.dto.PredictionRequest;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PredictionBatchParser {
    private static final int MAX_ROWS = 1000;

    public List<PredictionRequest> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a non-empty CSV or XLSX file");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        try {
            List<Map<String, String>> rows = name.endsWith(".xlsx") ? readXlsx(file) : name.endsWith(".csv") ? readCsv(file) : throwType();
            if (rows.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "The batch file has no data rows");
            if (rows.size() > MAX_ROWS) throw new ApiException(HttpStatus.BAD_REQUEST, "A batch can contain at most 1000 rows");
            List<PredictionRequest> result = new ArrayList<>();
            for (int index = 0; index < rows.size(); index++) {
                try { result.add(toRequest(rows.get(index))); }
                catch (RuntimeException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid value at data row " + (index + 2) + ": " + exception.getMessage()); }
            }
            return result;
        } catch (ApiException exception) { throw exception; }
        catch (Exception exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read batch file: " + exception.getMessage()); }
    }

    private List<Map<String, String>> readCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String first = reader.readLine(); if (first == null) return List.of();
            String[] headers = first.replace("\uFEFF", "").split(",", -1);
            List<Map<String, String>> rows = new ArrayList<>(); String line;
            while ((line = reader.readLine()) != null) { if (!line.isBlank()) rows.add(row(headers, line.split(",", -1))); }
            return rows;
        }
    }

    private List<Map<String, String>> readXlsx(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); if (sheet.getPhysicalNumberOfRows() < 2) return List.of();
            DataFormatter formatter = new DataFormatter(Locale.ROOT); Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            String[] headers = new String[headerRow.getLastCellNum()];
            for (int c=0;c<headers.length;c++) headers[c]=formatter.formatCellValue(headerRow.getCell(c)).trim();
            List<Map<String,String>> rows = new ArrayList<>();
            for (int r=headerRow.getRowNum()+1;r<=sheet.getLastRowNum();r++) { Row source=sheet.getRow(r); if(source==null) continue; String[] values=new String[headers.length]; boolean any=false;
                for(int c=0;c<headers.length;c++){ values[c]=formatter.formatCellValue(source.getCell(c)).trim(); any|=!values[c].isBlank(); } if(any) rows.add(row(headers,values)); }
            return rows;
        }
    }

    private Map<String,String> row(String[] headers, String[] values) { Map<String,String> row=new HashMap<>(); for(int i=0;i<headers.length;i++) row.put(headers[i].trim(), i<values.length?values[i].trim():""); return row; }
    private List<Map<String,String>> throwType() { throw new ApiException(HttpStatus.BAD_REQUEST, "Only .csv and .xlsx files are supported"); }
    private String req(Map<String,String> r,String key){ String v=r.get(key); if(v==null||v.isBlank()) throw new IllegalArgumentException("missing " + key); return v.toLowerCase(Locale.ROOT).replace("_", ""); }
    private BigDecimal decimal(Map<String,String> r,String key){ return new BigDecimal(req(r,key)); }
    private Integer integer(Map<String,String> r,String key){ return decimal(r,key).intValueExact(); }
    private PredictionRequest toRequest(Map<String,String> r) {
        return new PredictionRequest(r.getOrDefault("patientCode", ""), integer(r,"age"), decimal(r,"bp"), req(r,"htn"), req(r,"dm"), req(r,"cad"), req(r,"appet"), req(r,"pe"), req(r,"ane"), decimal(r,"sg"), integer(r,"al"), integer(r,"su"), req(r,"rbc"), req(r,"pc"), req(r,"pcc"), req(r,"ba"), decimal(r,"bgr"), decimal(r,"bu"), decimal(r,"sc"), decimal(r,"sod"), decimal(r,"pot"), decimal(r,"hemo"), decimal(r,"pcv"), decimal(r,"wc"), decimal(r,"rc"));
    }
}
