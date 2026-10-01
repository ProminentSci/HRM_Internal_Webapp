package com.employee.management.backend.config;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.Entity.Project;
import com.employee.management.backend.Entity.ProjectMembership;
import com.employee.management.backend.repository.EmployeeRepository;
import com.employee.management.backend.repository.ProjectMembershipRepository;
import com.employee.management.backend.repository.ProjectRepository;
import com.employee.management.backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Seeds 30 sample employees (varied departments, designations, position levels, and bench/
// project status), plus the 3 projects they're assigned to with real PM/Team Lead reporting
// lines - so Team Structure, Attendance, and Reports have realistic data to test against on a
// fresh dev database. Safe to run on every restart, and resumable if interrupted partway
// through: each demo email and each project/membership is checked individually before creating
// it. Disable with app.demo-data.enabled=false.
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_EMAIL_DOMAIN = "@hrms-demo.local";
    private static final String DEMO_PASSWORD = "Employee@123";

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultClientProvider defaultClientProvider;
    private final boolean enabled;

    public DemoDataSeeder(EmployeeRepository employeeRepository,
                           EmployeeService employeeService,
                           ProjectRepository projectRepository,
                           ProjectMembershipRepository membershipRepository,
                           PasswordEncoder passwordEncoder,
                           DefaultClientProvider defaultClientProvider,
                           @Value("${app.demo-data.enabled:true}") boolean enabled) {
        this.employeeRepository = employeeRepository;
        this.employeeService = employeeService;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultClientProvider = defaultClientProvider;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        List<DemoEmployee> demoEmployees = buildDemoEmployees();
        List<Employee> savedEmployees = new ArrayList<>();
        int seeded = 0;

        for (int i = 0; i < demoEmployees.size(); i++) {
            String email = "demo" + (i + 1) + DEMO_EMAIL_DOMAIN;
            Employee existing = employeeRepository.findByEmail(email).orElse(null);
            if (existing != null) {
                backfillProjectManagerName(existing, demoEmployees.get(i));
                savedEmployees.add(existing);
                continue;
            }
            savedEmployees.add(employeeService.createEmployee(toEmployee(demoEmployees.get(i), email, i)));
            seeded++;
        }

        int projectRows = seedProjectsAndMemberships(demoEmployees, savedEmployees);

        if (seeded > 0 || projectRows > 0) {
            System.out.println("=================================================================");
            System.out.println("Demo data: seeded " + seeded + " employees and " + projectRows + " project assignments.");
            System.out.println("  Emails:   demo1" + DEMO_EMAIL_DOMAIN + " .. demo" + demoEmployees.size() + DEMO_EMAIL_DOMAIN);
            System.out.println("  Password: " + DEMO_PASSWORD);
            System.out.println("Disable future seeding with app.demo-data.enabled=false.");
            System.out.println("=================================================================");
        }
    }

    private Employee toEmployee(DemoEmployee d, String email, int index) {
        Employee employee = new Employee();
        employee.setFirstName(d.firstName);
        employee.setLastName(d.lastName);
        employee.setEmail(email);
        employee.setPhone(String.format("9%09d", 100000000 + index));
        employee.setGender(d.gender);
        employee.setRole("employee");
        employee.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        employee.setClient(defaultClientProvider.getOrCreate());

        JobDetails jobDetails = new JobDetails();
        jobDetails.setDepartment(d.department);
        jobDetails.setDesignation(d.designation);
        jobDetails.setPositionLevel(d.positionLevel);
        jobDetails.setEmployeeType(d.employeeType);
        jobDetails.setDateOfJoining(d.dateOfJoining);
        jobDetails.setEmployeeStatus(d.employeeStatus);
        jobDetails.setWorkLocation("Hyderabad");
        jobDetails.setWorkStatus(d.workStatus);
        if ("Project".equalsIgnoreCase(d.workStatus)) {
            jobDetails.setCurrentProjectName(d.currentProjectName);
            jobDetails.setCurrentProjectManager(d.projectManagerName);
            jobDetails.setCurrentProjectStartDate(d.dateOfJoining);
        }
        employee.setJobDetails(jobDetails);
        return employee;
    }

    // For demo employees seeded before this class knew about currentProjectManager (or before
    // the project-assignment data changed), backfill their Job Details on every restart so
    // already-existing rows stay in sync with the current demo data instead of only ever
    // reflecting whatever was true the day they were first created.
    private void backfillProjectManagerName(Employee employee, DemoEmployee d) {
        JobDetails jobDetails = employee.getJobDetails();
        if (jobDetails == null || !"Project".equalsIgnoreCase(d.workStatus)) {
            return;
        }
        if (!java.util.Objects.equals(d.projectManagerName, jobDetails.getCurrentProjectManager())) {
            jobDetails.setCurrentProjectManager(d.projectManagerName);
            employeeRepository.save(employee);
        }
    }

    // Creates the 3 demo projects (if missing) and a ProjectMembership row per assigned
    // employee (if missing), wiring up the same PM/team-lead reporting lines already implied
    // by each employee's currentProjectName/projectManagerName so Team Structure's hierarchy
    // view and the employee's own Job Details agree with each other.
    private int seedProjectsAndMemberships(List<DemoEmployee> demoEmployees, List<Employee> savedEmployees) {
        List<DemoProject> demoProjects = List.of(
            new DemoProject("Orion CRM Revamp", "Customer relationship management platform revamp for enterprise clients.", 0),
            new DemoProject("Atlas Payments Gateway", "Unified payments gateway integration across regions.", 1),
            new DemoProject("Zenith Onboarding Portal", "Self-service onboarding portal for new hires.", 2)
        );

        Map<String, Project> projectsByName = new LinkedHashMap<>();
        for (DemoProject dp : demoProjects) {
            Project project = projectRepository.findByName(dp.name).orElseGet(() -> {
                Project p = new Project();
                p.setName(dp.name);
                p.setDescription(dp.description);
                p.setStatus("Active");
                p.setProjectManager(savedEmployees.get(dp.managerIndex));
                return projectRepository.save(p);
            });
            projectsByName.put(dp.name, project);
        }

        int created = 0;
        for (int i = 0; i < demoEmployees.size(); i++) {
            DemoEmployee d = demoEmployees.get(i);
            // PMs are tracked on Project.projectManager itself, not as a membership row.
            if (!"Project".equalsIgnoreCase(d.workStatus) || d.currentProjectName == null
                    || "PROJECT_MANAGER".equals(d.positionLevel)) {
                continue;
            }

            Project project = projectsByName.get(d.currentProjectName);
            Employee employee = savedEmployees.get(i);
            if (project == null || membershipRepository.findByProjectIdAndEmployeeEmpId(project.getId(), employee.getEmpId()).isPresent()) {
                continue;
            }

            ProjectMembership membership = new ProjectMembership();
            membership.setProject(project);
            membership.setEmployee(employee);
            membership.setStartDate(d.dateOfJoining);
            if (d.teamLeadIndex != null) {
                membership.setTeamLead(savedEmployees.get(d.teamLeadIndex));
            }
            membershipRepository.save(membership);
            created++;
        }
        return created;
    }

    private record DemoProject(String name, String description, int managerIndex) {
    }

    private record DemoEmployee(String firstName, String lastName, String gender, String department,
                                 String designation, String positionLevel, String employeeType,
                                 String workStatus, String currentProjectName, String projectManagerName,
                                 Integer teamLeadIndex, String employeeStatus, String dateOfJoining) {
    }

    private List<DemoEmployee> buildDemoEmployees() {
        return List.of(
            // index 0-2: Project Managers (one per project)
            new DemoEmployee("Rahul", "Verma", "Male", "IT", "Project Manager", "PROJECT_MANAGER", "Full Time", "Project", "Orion CRM Revamp", null, null, "Active", "2021-03-15"),
            new DemoEmployee("Priya", "Nair", "Female", "IT", "Project Manager", "PROJECT_MANAGER", "Full Time", "Project", "Atlas Payments Gateway", null, null, "Active", "2021-06-01"),
            new DemoEmployee("Arjun", "Mehta", "Male", "Non IT", "Project Manager", "PROJECT_MANAGER", "Full Time", "Project", "Zenith Onboarding Portal", null, null, "Active", "2020-11-10"),

            // index 3-8: Team Leads (report directly to their project's PM - teamLeadIndex null)
            new DemoEmployee("Aditi", "Sharma", "Female", "IT", "Team Lead", "TEAM_LEAD", "Full Time", "Project", "Orion CRM Revamp", "Rahul Verma", null, "Active", "2021-09-20"),
            new DemoEmployee("Karan", "Malhotra", "Male", "IT", "Team Lead", "TEAM_LEAD", "Full Time", "Project", "Orion CRM Revamp", "Rahul Verma", null, "Active", "2022-01-05"),
            new DemoEmployee("Sneha", "Iyer", "Female", "IT", "Team Lead", "TEAM_LEAD", "Full Time", "Project", "Atlas Payments Gateway", "Priya Nair", null, "Active", "2021-12-01"),
            new DemoEmployee("Vikram", "Rao", "Male", "IT", "Team Lead", "TEAM_LEAD", "Full Time", "Project", "Atlas Payments Gateway", "Priya Nair", null, "Active", "2022-02-14"),
            new DemoEmployee("Neha", "Kapoor", "Female", "Non IT", "Team Lead", "TEAM_LEAD", "Full Time", "Project", "Zenith Onboarding Portal", "Arjun Mehta", null, "Active", "2021-07-19"),
            new DemoEmployee("Rohit", "Desai", "Male", "Non IT", "Team Lead", "TEAM_LEAD", "Full Time", "Bench", null, null, null, "Active", "2022-04-02"),

            // index 9-19: Employees (teamLeadIndex points at who they report to within their project)
            new DemoEmployee("Ananya", "Reddy", "Female", "IT", "Software Engineer", "EMPLOYEE", "Full Time", "Project", "Orion CRM Revamp", "Rahul Verma", 3, "Active", "2022-06-13"),
            new DemoEmployee("Siddharth", "Joshi", "Male", "IT", "Software Engineer", "EMPLOYEE", "Full Time", "Project", "Orion CRM Revamp", "Rahul Verma", 3, "Active", "2022-07-01"),
            new DemoEmployee("Meera", "Pillai", "Female", "IT", "Software Engineer", "EMPLOYEE", "Full Time", "Project", "Orion CRM Revamp", "Rahul Verma", 4, "Active", "2023-01-09"),
            new DemoEmployee("Aryan", "Chopra", "Male", "IT", "Senior Software Engineer", "EMPLOYEE", "Full Time", "Project", "Orion CRM Revamp", "Rahul Verma", 4, "Active", "2020-08-22"),
            new DemoEmployee("Ishita", "Bose", "Female", "IT", "QA Engineer", "EMPLOYEE", "Full Time", "Project", "Atlas Payments Gateway", "Priya Nair", 5, "Active", "2022-09-18"),
            new DemoEmployee("Devansh", "Saxena", "Male", "IT", "QA Engineer", "EMPLOYEE", "Full Time", "Project", "Atlas Payments Gateway", "Priya Nair", 5, "Active", "2023-02-27"),
            new DemoEmployee("Kavya", "Menon", "Female", "IT", "Software Engineer", "EMPLOYEE", "Full Time", "Project", "Atlas Payments Gateway", "Priya Nair", 6, "Active", "2023-03-06"),
            new DemoEmployee("Yash", "Agarwal", "Male", "IT", "Software Engineer", "EMPLOYEE", "Contract", "Project", "Atlas Payments Gateway", "Priya Nair", 6, "Active", "2023-05-15"),
            new DemoEmployee("Riya", "Bhatt", "Female", "Non IT", "Business Analyst", "EMPLOYEE", "Full Time", "Project", "Zenith Onboarding Portal", "Arjun Mehta", 7, "Active", "2022-10-03"),
            new DemoEmployee("Aditya", "Kulkarni", "Male", "Non IT", "Business Analyst", "EMPLOYEE", "Full Time", "Project", "Zenith Onboarding Portal", "Arjun Mehta", 7, "Active", "2022-11-21"),
            new DemoEmployee("Pooja", "Trivedi", "Female", "Non IT", "Operations Executive", "EMPLOYEE", "Full Time", "Project", "Zenith Onboarding Portal", "Arjun Mehta", 7, "Active", "2023-04-04"),

            // index 20-29: Bench (no project)
            new DemoEmployee("Manish", "Yadav", "Male", "HR", "HR Executive", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Active", "2021-05-11"),
            new DemoEmployee("Simran", "Kaur", "Female", "HR", "HR Executive", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Active", "2022-08-08"),
            new DemoEmployee("Farhan", "Sheikh", "Male", "HR", "Talent Acquisition Specialist", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Active", "2023-01-30"),
            new DemoEmployee("Divya", "Rajan", "Female", "Admin", "Admin Executive", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Active", "2021-10-25"),
            new DemoEmployee("Naveen", "Prakash", "Male", "Admin", "Facilities Coordinator", "EMPLOYEE", "Part Time", "Bench", null, null, null, "Active", "2022-12-12"),
            new DemoEmployee("Tanvi", "Shetty", "Female", "IT", "Software Engineer Intern", "EMPLOYEE", "Temporary", "Bench", null, null, null, "Active", "2023-06-19"),
            new DemoEmployee("Harsh", "Goel", "Male", "IT", "DevOps Engineer", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Active", "2022-03-08"),
            new DemoEmployee("Zoya", "Khan", "Female", "IT", "UI/UX Designer", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Active", "2023-07-17"),
            new DemoEmployee("Om", "Pandey", "Male", "Non IT", "Support Engineer", "EMPLOYEE", "Full Time", "Bench", null, null, null, "Inactive", "2020-02-14"),
            new DemoEmployee("Lakshmi", "Venkatesh", "Female", "Non IT", "Support Engineer", "EMPLOYEE", "Full Time", "Bench", null, null, null, "On Leave", "2021-01-04")
        );
    }
}
