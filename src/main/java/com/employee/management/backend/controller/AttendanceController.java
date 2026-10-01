package com.employee.management.backend.controller;

import com.employee.management.backend.Entity.Attendance;
import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.Holiday;
import com.employee.management.backend.repository.AttendanceRepository;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.HolidayRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;

    public AttendanceController(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository,
                                 HolidayRepository holidayRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.holidayRepository = holidayRepository;
    }

    @GetMapping
    public List<AttendanceResponse> getAllAttendance(@RequestParam(required = false) Long employeeId,
                                                     @RequestParam(required = false) String month,
                                                     @RequestParam(required = false) Integer year,
                                                     @RequestParam(required = false) Integer monthNumber,
                                                     @RequestParam(required = false) String search) {
        String normalizedSearch = (search == null || search.trim().isEmpty()) ? null : search.trim();
        Long searchId = null;
        String searchName = null;
        if (normalizedSearch != null) {
            if (normalizedSearch.matches("\\d+")) {
                searchId = Long.parseLong(normalizedSearch);
            } else {
                searchName = normalizedSearch.toLowerCase();
            }
        }
        final Long finalSearchId = searchId;
        final String finalSearchName = searchName;

        // When a specific year+month is selected, build the report against the
        // month's actual working days (excluding weekends/holidays) so employees
        // who never checked in still show up as ABSENT instead of just vanishing.
        if (year != null && monthNumber != null) {
            return buildMonthlyAttendance(employeeId, finalSearchId, finalSearchName, year, monthNumber);
        }

        List<Attendance> attendanceList = attendanceRepository.findAll();

        return attendanceList.stream()
                .filter(record -> employeeId == null
                        || (record.getEmployee() != null && record.getEmployee().getEmpId().equals(employeeId)))
                // exact-ID search: never match "1" against 10, 11, 12...
                .filter(record -> finalSearchId == null
                        || (record.getEmployee() != null && finalSearchId.equals(record.getEmployee().getEmpId())))
                // name search stays a substring match, since that's expected for names
                .filter(record -> {
                    if (finalSearchName == null) return true;
                    if (record.getEmployee() == null) return false;
                    String fullName = String.format("%s %s",
                            record.getEmployee().getFirstName() == null ? "" : record.getEmployee().getFirstName(),
                            record.getEmployee().getLastName() == null ? "" : record.getEmployee().getLastName())
                            .trim().toLowerCase();
                    return fullName.contains(finalSearchName);
                })
                .filter(record -> {
                    if (month == null || month.isBlank()) return true;
                    if (record.getDate() == null) return false;
                    return record.getDate().startsWith(month);
                })
                .filter(record -> {
                    if (year == null && monthNumber == null) return true;
                    if (record.getDate() == null || record.getDate().length() < 7) return false;
                    try {
                        int recordYear = Integer.parseInt(record.getDate().substring(0, 4));
                        int recordMonth = Integer.parseInt(record.getDate().substring(5, 7));
                        boolean yearOk = year == null || recordYear == year;
                        boolean monthOk = monthNumber == null || recordMonth == monthNumber;
                        return yearOk && monthOk;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })
                .map(AttendanceResponse::fromAttendance)
                .toList();
    }

    /**
     * Builds one row per matched employee per working day of the given month
     * (Mon-Fri, minus holidays, minus days after today), using the real
     * attendance record when the employee checked in and a synthetic ABSENT
     * row otherwise.
     */
    private List<AttendanceResponse> buildMonthlyAttendance(Long employeeId, Long searchId, String searchName,
                                                              int year, int monthNumber) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, monthNumber);
        } catch (Exception ex) {
            return List.of();
        }

        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDate cappedLastDay = lastDay.isAfter(today) ? today : lastDay;
        if (cappedLastDay.isBefore(firstDay)) {
            return List.of();
        }

        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(emp -> employeeId == null || employeeId.equals(emp.getEmpId()))
                .filter(emp -> searchId == null || searchId.equals(emp.getEmpId()))
                .filter(emp -> {
                    if (searchName == null) return true;
                    String fullName = String.format("%s %s",
                            emp.getFirstName() == null ? "" : emp.getFirstName(),
                            emp.getLastName() == null ? "" : emp.getLastName()).trim().toLowerCase();
                    return fullName.contains(searchName);
                })
                .toList();

        if (employees.isEmpty()) {
            return List.of();
        }

        Set<LocalDate> holidayDates = holidayRepository.findByDateBetweenOrderByDateAsc(firstDay, cappedLastDay).stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());

        String monthPrefix = String.format("%04d-%02d", year, monthNumber);
        Map<Long, Map<String, Attendance>> attendanceByEmployee = attendanceRepository.findByDateStartingWith(monthPrefix).stream()
                .filter(record -> record.getEmployee() != null && record.getDate() != null)
                .collect(Collectors.groupingBy(
                        record -> record.getEmployee().getEmpId(),
                        Collectors.toMap(Attendance::getDate, r -> r, (a, b) -> a)));

        List<AttendanceResponse> result = new ArrayList<>();
        for (Employee employee : employees) {
            LocalDate joinDate = parseLocalDate(
                    employee.getJobDetails() != null ? employee.getJobDetails().getDateOfJoining() : null);
            Map<String, Attendance> employeeAttendance = attendanceByEmployee.getOrDefault(employee.getEmpId(), Map.of());

            for (LocalDate day = firstDay; !day.isAfter(cappedLastDay); day = day.plusDays(1)) {
                DayOfWeek dayOfWeek = day.getDayOfWeek();
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) continue;
                if (holidayDates.contains(day)) continue;
                if (joinDate != null && day.isBefore(joinDate)) continue;

                Attendance existing = employeeAttendance.get(day.toString());
                result.add(existing != null
                        ? AttendanceResponse.fromAttendance(existing)
                        : AttendanceResponse.absent(employee, day.toString()));
            }
        }

        result.sort(Comparator.comparing(AttendanceResponse::getDate)
                .thenComparing(r -> r.getEmployeeName() == null ? "" : r.getEmployeeName()));

        return result;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    @GetMapping("/employee/{empId}")
    public List<AttendanceResponse> getAttendanceForEmployee(@PathVariable Long empId) {
        return attendanceRepository.findByEmployeeEmpIdOrderByIdDesc(empId).stream()
                .map(AttendanceResponse::fromAttendance)
                .toList();
    }

    @GetMapping("/today/{empId}")
    public ResponseEntity<?> getTodayAttendance(@PathVariable Long empId) {
        String today = LocalDate.now().toString();
        return attendanceRepository.findByEmployeeEmpIdAndDate(empId, today)
                .map(att -> ResponseEntity.ok(AttendanceResponse.fromAttendance(att)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> body) {
        try {
            Long empId = Long.valueOf(body.get("employeeId").toString());
            String checkInTime = body.getOrDefault("checkInTime", LocalTime.now().toString()).toString();
            Employee employee = employeeRepository.findById(empId).orElse(null);
            if (employee == null) return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));

            String today = LocalDate.now().toString();
            Attendance attendance = attendanceRepository.findByEmployeeEmpIdAndDate(empId, today).orElse(null);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(today);
                attendance.setCheckInTime(checkInTime);
                attendance.setStatus("PRESENT");
                attendanceRepository.save(attendance);
            } else {
                attendance.setCheckInTime(checkInTime);
                attendance.setStatus("PRESENT");
                attendanceRepository.save(attendance);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Checked in successfully");
            resp.put("checkInTime", checkInTime);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(@RequestBody Map<String, Object> body) {
        try {
            Long empId = Long.valueOf(body.get("employeeId").toString());
            String checkOutTime = body.getOrDefault("checkOutTime", LocalTime.now().toString()).toString();

            String today = LocalDate.now().toString();
            Attendance attendance = attendanceRepository.findByEmployeeEmpIdAndDate(empId, today).orElse(null);
            if (attendance == null) return ResponseEntity.badRequest().body(Map.of("error", "No check-in record for today"));

            attendance.setCheckOutTime(checkOutTime);

            // compute total hours if checkIn exists
            if (attendance.getCheckInTime() != null && !attendance.getCheckInTime().isEmpty()) {
                try {
                    DateTimeFormatter[] formats = new DateTimeFormatter[] {
                            DateTimeFormatter.ofPattern("HH:mm:ss"),
                            DateTimeFormatter.ofPattern("HH:mm")
                    };
                    LocalTime in = null;
                    LocalTime out = null;
                    for (DateTimeFormatter f : formats) {
                        try { in = LocalTime.parse(attendance.getCheckInTime(), f); break; } catch (Exception e) { }
                    }
                    for (DateTimeFormatter f : formats) {
                        try { out = LocalTime.parse(checkOutTime, f); break; } catch (Exception e) { }
                    }
                    if (in != null && out != null) {
                        if (!out.isAfter(in)) {
                            return ResponseEntity.badRequest().body(Map.of("error", "Check-out time must be after check-in time"));
                        }
                        long minutes = ChronoUnit.MINUTES.between(in, out);
                        double hours = Math.round((minutes / 60.0) * 100.0) / 100.0;
                        attendance.setTotalHours(hours);
                    }
                } catch (Exception e) {
                    // ignore parsing errors
                }
            }

            attendanceRepository.save(attendance);

            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Checked out successfully");
            resp.put("checkOutTime", checkOutTime);
            resp.put("totalHours", attendance.getTotalHours());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
        }
    }

    public static class AttendanceResponse {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String date;
        private String checkInTime;
        private String checkOutTime;
        private Double totalHours;
        private String status;
        private String remarks;

        public AttendanceResponse() {
        }

        public static AttendanceResponse fromAttendance(Attendance attendance) {
            AttendanceResponse response = new AttendanceResponse();
            response.id = attendance.getId();
            response.employeeId = attendance.getEmployee() != null ? attendance.getEmployee().getEmpId() : null;
            if (attendance.getEmployee() != null) {
                response.employeeName = String.format("%s %s",
                        attendance.getEmployee().getFirstName() == null ? "" : attendance.getEmployee().getFirstName(),
                        attendance.getEmployee().getLastName() == null ? "" : attendance.getEmployee().getLastName()).trim();
            }
            response.date = attendance.getDate();
            response.checkInTime = attendance.getCheckInTime();
            response.checkOutTime = attendance.getCheckOutTime();
            response.totalHours = attendance.getTotalHours();
            response.status = attendance.getStatus();
            response.remarks = attendance.getRemarks();
            return response;
        }

        public static AttendanceResponse absent(Employee employee, String date) {
            AttendanceResponse response = new AttendanceResponse();
            response.employeeId = employee.getEmpId();
            response.employeeName = String.format("%s %s",
                    employee.getFirstName() == null ? "" : employee.getFirstName(),
                    employee.getLastName() == null ? "" : employee.getLastName()).trim();
            response.date = date;
            response.status = "ABSENT";
            return response;
        }

        public Long getId() {
            return id;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public String getDate() {
            return date;
        }

        public String getCheckInTime() {
            return checkInTime;
        }

        public String getCheckOutTime() {
            return checkOutTime;
        }

        public Double getTotalHours() {
            return totalHours;
        }

        public String getStatus() {
            return status;
        }

        public String getRemarks() {
            return remarks;
        }
    }
}
