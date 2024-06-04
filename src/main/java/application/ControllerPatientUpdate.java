package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.DriverManager.getConnection;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientUpdate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     *  Display patient profile for patient id.
     */
    @GetMapping("/patient/edit/{id}")
    public String getUpdateForm(@PathVariable int id, Model model) {

        PatientView pv = new PatientView();
        try (Connection conn = getConnection()) {
            // TODO search for patient by id
            String sql = "SELECT p.*, d.last_name AS doctor_last_name FROM Patient p JOIN Doctor d ON p.doctor_id = d.doctor_id WHERE p.patient_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                pv.setId(rs.getInt("patient_id"));
                pv.setFirst_name(rs.getString("first_name"));
                pv.setLast_name(rs.getString("last_name"));
                pv.setBirthdate(rs.getDate("DOB").toString());
                pv.setStreet(rs.getString("street"));
                pv.setCity(rs.getString("city"));
                pv.setState(rs.getString("state"));
                pv.setZipcode(rs.getString("zipcode"));
                pv.setPrimaryName(rs.getString("doctor_last_name"));
                pv.setDoctor_id(rs.getInt("doctor_id"));

                model.addAttribute("patient", pv);
                return "patient_edit";
            } else {
                model.addAttribute("message", "Patient not found.");
                return "index"; // Patient not found
            }
        } catch (SQLException e) {
            model.addAttribute("message", "An error occurred: " + e.getMessage());
            return "index"; // On error, return to home page
        }

        //  if not found, return to home page using return "index";
        //  else create PatientView and add to model.
        // model.addAttribute("message", some message);
        // model.addAttribute("patient", pv
        // return editable form with patient data
    }


    /**
     * Process changes from patient_edit form
     * Primary doctor, street, city, state, zip can be changed
     * ssn, patient id, name, birthdate, ssn are read only in template.
     */
    @PostMapping("/patient/edit")
    public String updatePatient(PatientView p, Model model) {
        try (Connection conn = getConnection()) {
            // Validate doctor last name
            String doctorSql = "SELECT doctor_id FROM Doctor WHERE last_name = ?";
            PreparedStatement doctorStmt = conn.prepareStatement(doctorSql);
            doctorStmt.setString(1, p.getPrimaryName());
            ResultSet rs = doctorStmt.executeQuery();

            if (rs.next()) {
                p.setDoctor_id(rs.getInt("doctor_id"));
            } else {
                model.addAttribute("message", "Doctor not found.");
                model.addAttribute("patient", p);
                return "patient_edit";
            }

            // Update patient profile data in database
            String patientSql = "UPDATE Patient SET doctor_id = ?, street = ?, city = ?, state = ?, zipcode = ? WHERE patient_id = ?";
            PreparedStatement patientStmt = conn.prepareStatement(patientSql);
            patientStmt.setInt(1, p.getDoctor_id());
            patientStmt.setString(2, p.getStreet());
            patientStmt.setString(3, p.getCity());
            patientStmt.setString(4, p.getState());
            patientStmt.setString(5, p.getZipcode());
            patientStmt.setInt(6, p.getId());
            patientStmt.executeUpdate();

            model.addAttribute("message", "Patient profile updated successfully.");
            model.addAttribute("patient", p);
            return "patient_show";

        } catch (SQLException e) {
            model.addAttribute("message", "An error occurred: " + e.getMessage());
            model.addAttribute("patient", p);
            return "patient_edit";
        }

        // validate doctor last name

        // TODO

        // TODO update patient profile data in database

        // model.addAttribute("message", some message);
        // model.addAttribute("patient", p)
    }

    /*
     * return JDBC Connection using jdbcTemplate in Spring Server
     */
    private Connection getConnection() throws SQLException {
        return jdbcTemplate.getDataSource().getConnection();
    }

}
