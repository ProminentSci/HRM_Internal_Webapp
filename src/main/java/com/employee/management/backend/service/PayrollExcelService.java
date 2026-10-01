package com.employee.management.backend.service;

import com.employee.management.backend.dto.PayrollEmployeeResponseDTO;
import com.employee.management.backend.dto.PayrollProcessResponseDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PayrollExcelService {

    public byte[] buildPayrollExcel(PayrollProcessResponseDTO payroll) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {
                    "Payroll ID",
                    "Employee ID",
                    "Employee Name",
                    "Requested Employee Name",
                    "Process Status",
                    "Message",
                    "DOJ",
                    "LOP",
                    "Final Salary",
                    "Basic Salary",
                    "Bonus",
                    "CTC",
                    "Monthly Gross Salary",
                    "Paid Leave Days",
                    "Unpaid Leave Days",
                    "Leave Deduction",
                    "Variable Pay",
                    "Net Salary",
                    "PAN Number",
                    "Account No",
                    "IFSC",
                    "UAN",
                    "PF",
                    "Credited/Non Credited",
                    "Month",
                    "Year"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (PayrollEmployeeResponseDTO employeePayroll : payroll.getEmployees()) {
                Row row = sheet.createRow(rowIndex++);
                writePayrollRow(row, employeePayroll);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate payroll excel", ex);
        }
    }

    private void writePayrollRow(Row row, PayrollEmployeeResponseDTO payroll) {
        writeLong(row, 0, payroll.getPayrollId());
        writeLong(row, 1, payroll.getEmployeeId());
        writeString(row, 2, payroll.getEmployeeName());
        writeString(row, 3, payroll.getRequestedEmployeeName());
        writeString(row, 4, payroll.getStatus());
        writeString(row, 5, payroll.getMessage());
        writeString(row, 6, payroll.getDateOfJoining());
        writeInteger(row, 7, payroll.getLop());
        writeDouble(row, 8, payroll.getSalary());
        writeDouble(row, 9, payroll.getBasicSalary());
        writeDouble(row, 10, payroll.getBonus());
        writeDouble(row, 11, payroll.getCtc());
        writeDouble(row, 12, payroll.getMonthlyGrossSalary());
        writeInteger(row, 13, payroll.getPaidLeaveDays());
        writeInteger(row, 14, payroll.getUnpaidLeaveDays());
        writeDouble(row, 15, payroll.getLeaveDeduction());
        writeDouble(row, 16, payroll.getVariablePay());
        writeDouble(row, 17, payroll.getNetSalary());
        writeString(row, 18, payroll.getPanNumber());
        writeString(row, 19, payroll.getAccountNumber());
        writeString(row, 20, payroll.getIfsc());
        writeString(row, 21, payroll.getUan());
        writeString(row, 22, payroll.getPf());
        writeString(row, 23, payroll.getCreditStatus());
        writeInteger(row, 24, payroll.getMonth());
        writeInteger(row, 25, payroll.getYear());
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeString(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void writeLong(Row row, int column, Long value) {
        if (value == null) {
            writeString(row, column, "");
        } else {
            row.createCell(column).setCellValue(value);
        }
    }

    private void writeInteger(Row row, int column, Integer value) {
        if (value == null) {
            writeString(row, column, "");
        } else {
            row.createCell(column).setCellValue(value);
        }
    }

    private void writeDouble(Row row, int column, Double value) {
        if (value == null) {
            writeString(row, column, "");
        } else {
            row.createCell(column).setCellValue(value);
        }
    }
}
