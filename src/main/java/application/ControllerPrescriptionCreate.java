package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionCreate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * Doctor requests blank form for new prescription.
     */
    @GetMapping("/prescription/new")
    public String getPrescriptionForm(Model model) {
        model.addAttribute("prescription", new PrescriptionView());
        return "prescription_create";
    }

    // process data entered on prescription_create form
    @PostMapping("/prescription")
    public String createPrescription(PrescriptionView p, Model model) {

        System.out.println("createPrescription " + p);
        try (Connection conn = getConnection()) {
            // Validate doctor name and id
            String doctorSql = "SELECT doctor_id FROM Doctor WHERE doctor_id = ? AND last_name = ?";
            PreparedStatement doctorStmt = conn.prepareStatement(doctorSql);
            doctorStmt.setInt(1, p.getDoctor_id());
            doctorStmt.setString(2, p.getDoctorLastName());
            ResultSet rsDoctor = doctorStmt.executeQuery();

            if (!rsDoctor.next()) {
                model.addAttribute("message", "Doctor not found.");
                model.addAttribute("prescription", p);
                return "prescription_create";
            }

            // Validate patient name and id
            String patientSql = "SELECT patient_id FROM Patient WHERE patient_id = ? AND last_name = ?";
            PreparedStatement patientStmt = conn.prepareStatement(patientSql);
            patientStmt.setInt(1, p.getPatient_id());
            patientStmt.setString(2, p.getPatientLastName());
            ResultSet rsPatient = patientStmt.executeQuery();

            if (!rsPatient.next()) {
                model.addAttribute("message", "Patient not found.");
                model.addAttribute("prescription", p);
                return "prescription_create";
            }

            // Validate drug name
            String drugSql = "SELECT * FROM Drug WHERE name = ?";
            PreparedStatement drugStmt = conn.prepareStatement(drugSql);
            drugStmt.setString(1, p.getDrugName());
            ResultSet rsDrug = drugStmt.executeQuery();

            if (!rsDrug.next()) {
                model.addAttribute("message", "Drug not found.");
                model.addAttribute("prescription", p);
                return "prescription_create";
            }

            p.setDrug_id(rsDrug.getInt("drug_id"));
            p.setDrugName(rsDrug.getString("name"));

            // Insert prescription
            String prescriptionSql = "INSERT INTO Prescription (patient_id, doctor_id, drug_id, date_prescribed, refills, quantity) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement prescriptionStmt = conn.prepareStatement(prescriptionSql, PreparedStatement.RETURN_GENERATED_KEYS);
            prescriptionStmt.setInt(1, p.getPatient_id());
            prescriptionStmt.setInt(2, p.getDoctor_id());
            prescriptionStmt.setInt(3, p.getDrug_id());
            prescriptionStmt.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
            prescriptionStmt.setInt(5, p.getRefills());
            prescriptionStmt.setInt(6, p.getQuantity());
            prescriptionStmt.executeUpdate();

            p.setRefillsRemaining(p.getRefills());

            ResultSet generatedKeys = prescriptionStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                p.setRxid(generatedKeys.getInt(1));
            }

            model.addAttribute("message", "Prescription created.");
            model.addAttribute("prescription", p);
            return "prescription_show";

        } catch (SQLException e) {
            model.addAttribute("message", "An error occurred: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_create";
        }
        /*
         * valid doctor name and id
         */
        //TODO

        /*
         * valid patient name and id
         */
        //TODO

        /*
         * valid drug name
         */
        //TODO

        /*
         * insert prescription
         */
        //TODO

    }

    private Connection getConnection() throws SQLException {
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        return conn;
    }

}
