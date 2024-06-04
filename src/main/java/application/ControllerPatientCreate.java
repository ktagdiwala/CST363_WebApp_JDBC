package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientCreate {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /*
     * Request blank patient registration form.
     */
    @GetMapping("/patient/new")
    public String getNewPatientForm(Model model) {
        // return blank form for new patient registration
        model.addAttribute("patient", new PatientView());
        System.out.println("Inside /patient/new... getNewPatientForm()");
        return "patient_register";
    }

    /*
     * Process data from the patient_register form
     */
    @PostMapping("/patient/new")
    public String createPatient(PatientView p, Model model) {

        try (Connection conn = getConnection()) {
            /*
             * validate doctor last name and find the doctor id
             */
            String doctorSql = "SELECT doctor_id FROM Doctor WHERE last_name = ?";
            PreparedStatement doctorStmt = conn.prepareStatement(doctorSql);
            doctorStmt.setString(1, p.getPrimaryName());
            ResultSet rs = doctorStmt.executeQuery();

            if (rs.next()) {
                int doctorId = rs.getInt("doctor_id");
                p.setDoctor_id(doctorId);
            } else {
                model.addAttribute("message", "Doctor not found.");
                model.addAttribute("patient", p);
                return "patient_register";
            }
            // TODO

            /*
             * insert to patient table
             */
            String patientSql = "INSERT INTO Patient (doctor_id, ssc_num, first_name, last_name, birth_date, street, city, state, zip_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement patientStmt = conn.prepareStatement(patientSql,
                    PreparedStatement.RETURN_GENERATED_KEYS);
            patientStmt.setInt(1, p.getDoctor_id());
            patientStmt.setInt(2, p.getSsn());
            patientStmt.setString(3, p.getFirst_name());
            patientStmt.setString(4, p.getLast_name());
            patientStmt.setDate(5, java.sql.Date.valueOf(p.getBirthdate()));
            patientStmt.setString(6, p.getStreet());
            patientStmt.setString(7, p.getCity());
            patientStmt.setString(8, p.getState());
            patientStmt.setString(9, p.getZipcode());
            patientStmt.executeUpdate();

            ResultSet generatedKeys = patientStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                p.setId(generatedKeys.getInt(1));
            }

            // display patient data and the generated patient ID,  and success message
            model.addAttribute("message", "Registration successful.");
            model.addAttribute("patient", p);
            return "patient_show";
        } catch (SQLException e) {
            model.addAttribute("message", "An error occurred: " + e.getMessage());
            model.addAttribute("patient", p);
            return "patient_register";
        }
        /*
         * on error
         * model.addAttribute("message", some error message);
         * model.addAttribute("patient", p);
         * return "patient_register";
         */
    }

    /*
     * Request blank form to search for patient by id and name
     */
    @GetMapping("/patient/edit")
    public String getSearchForm(Model model) {
        model.addAttribute("patient", new PatientView());
        return "patient_get";
    }

    /*
     * Perform search for patient by patient id and name.
     */
    @PostMapping("/patient/show")
    public String showPatient(PatientView p, Model model) {
        try (Connection conn = getConnection()) {
            // TODO   search for patient by id and name
            String sql = "SELECT p.*, d.last_name AS doctor_last_name FROM Patient p JOIN Doctor d ON p.doctor_id = d.doctor_id WHERE p.patient_id = ? AND p.last_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, p.getId());
            stmt.setString(2, p.getLast_name());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                p.setFirst_name(rs.getString("first_name"));
                p.setBirthdate(rs.getDate("birth_date").toString());
                p.setStreet(rs.getString("street"));
                p.setCity(rs.getString("city"));
                p.setState(rs.getString("state"));
                p.setZipcode(rs.getString("zip_code"));
                p.setPrimaryName(rs.getString("doctor_last_name"));

                model.addAttribute("patient", p);
                model.addAttribute("message", "Patient found.");
                return "patient_show";
            } else {
                model.addAttribute("message", "Patient not found.");
                model.addAttribute("patient", p);
                return "patient_get";
            }

        } catch (SQLException e) {
            model.addAttribute("message", "An error occurred: " + e.getMessage());
            model.addAttribute("patient", p);
            return "patient_get";
        }
    }

    // if found, return "patient_show", else return error message and "patient_get"


    /**
     * return JDBC Connection using jdbcTemplate in Spring Server
     */

    private Connection getConnection() throws SQLException {
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        return conn;
    }
}

