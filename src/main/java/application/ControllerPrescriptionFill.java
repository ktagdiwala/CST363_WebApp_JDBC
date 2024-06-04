package application;

import java.sql.*;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionFill {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * Patient requests form to fill prescription.
     */
    @GetMapping("/prescription/fill")
    public String getfillForm(Model model) {
        model.addAttribute("prescription", new PrescriptionView());
        return "prescription_fill";
    }

    // process data from prescription_fill form
    @PostMapping("/prescription/fill")
    public String processFillForm(PrescriptionView p, Model model) {

        try (Connection conn = getConnection()) {
            // Validate pharmacy name and address, get pharmacy id and phone
            String pharmacySql = "SELECT * FROM pharmacy WHERE name = ? AND address = ?";
            PreparedStatement pharmacyStmt = conn.prepareStatement(pharmacySql);
            pharmacyStmt.setString(1, p.getPharmacyName());
            pharmacyStmt.setString(2, p.getPharmacyAddress());
            ResultSet rsPharmacy = pharmacyStmt.executeQuery();

            // Runs if pharmacy information not found
            if (!rsPharmacy.next()) {
                model.addAttribute("message", "Pharmacy not found.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            // If pharmacy info found, update the PrescriptionView with this information
            p.setPharmacyID(rsPharmacy.getInt("pharmacy_id"));
            p.setPharmacyName(rsPharmacy.getString("name"));
            p.setPharmacyAddress(rsPharmacy.getString("address"));
            p.setPharmacyPhone(rsPharmacy.getString("phone_number"));

            // Find the patient information
            String patientSQL = "SELECT * FROM Patient WHERE last_name = ?";
            PreparedStatement patientStmt = conn.prepareStatement(patientSQL);
            patientStmt.setString(1, p.getPatientLastName());
            ResultSet rsPatient = patientStmt.executeQuery();

            //Runs if patient information not found
            if (!rsPatient.next()) {
                model.addAttribute("message", "Patient not found.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }
            p.setPatient_id(rsPatient.getInt("patient_id"));
            p.setPatientFirstName(rsPatient.getString("first_name"));
            p.setPatientLastName(rsPatient.getString("last_name"));


            // Find the prescription
            String prescriptionSql = "SELECT * FROM prescription WHERE rxid = ? AND patient_id = ?";
            PreparedStatement prescriptionStmt = conn.prepareStatement(prescriptionSql);
            prescriptionStmt.setInt(1, p.getRxid());
            prescriptionStmt.setInt(2, p.getPatient_id());
            ResultSet rsPrescription = prescriptionStmt.executeQuery();

            // Runs if prescription RXID is invalid
            if (!rsPrescription.next()) {
                model.addAttribute("message", "Prescription not found.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            // If prescription information found, store prescription information in variables
            p.setRxid(rsPrescription.getInt("rxid"));
            p.setRefills(rsPrescription.getInt("refills"));
            p.setDrug_id(rsPrescription.getInt("drug_id"));
            p.setQuantity(rsPrescription.getInt("quantity"));

            // Get doctor information using retrieved doctor_id from prescription table
            String doctorSql = "SELECT * FROM Doctor WHERE doctor_id = ?";
            PreparedStatement doctorStmt = conn.prepareStatement(doctorSql);
            doctorStmt.setInt(1, rsPrescription.getInt("doctor_id"));
            ResultSet rsDoctor = doctorStmt.executeQuery();

            // Runs if the prescription does not have an associated doctor
            if (!rsDoctor.next()) {
                model.addAttribute("message", "Doctor not found.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }
            p.setDoctor_id(rsDoctor.getInt("doctor_id"));
            p.setDoctorFirstName(rsDoctor.getString("first_name"));
            p.setDoctorLastName(rsDoctor.getString("last_name"));

            // Find drug information from pharmacy_inventory
            // using retrieved pharmacyId and drugID
            String inventorySQL = "SELECT * FROM pharmacy_inventory WHERE pharmacy_id = ? AND drug_id = ?";
            PreparedStatement inventoryStmt = conn.prepareStatement(inventorySQL);
            inventoryStmt.setInt(1, p.getPharmacyID());
            inventoryStmt.setInt(2, p.getDrug_id());
            ResultSet rsInventory = inventoryStmt.executeQuery();

            // Check if the pharmacy carries the specified drug
            if(!rsInventory.next()){
                model.addAttribute("message", "This pharmacy does not carry the drug in your prescription");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            // Runs if the pharmacy carries the prescription's drug
            String drugSQL = "SELECT * FROM drug WHERE drug_id = ?";
            PreparedStatement drugStmt = conn.prepareStatement(drugSQL);
            drugStmt.setInt(1, p.getDrug_id());
            ResultSet rsDrug = drugStmt.executeQuery();

            // Check to make sure a drug is returned from the drug table
            if(!rsDrug.next()){
                model.addAttribute("message", "This drug does not exist");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }
            p.setDrugName(rsDrug.getString("name"));
            int unitAmount = rsInventory.getInt("unit_amount");
            Double unitPrice = rsInventory.getDouble("unit_price");

            // Check if filled_prescriptions already has a record for this prescription
            String refillSQL = "SELECT * FROM filled_prescriptions WHERE rxid = ?";
            PreparedStatement refillStmt = conn.prepareStatement(refillSQL);
            refillStmt.setInt(1, p.getRxid());
            ResultSet rsRefill = refillStmt.executeQuery();
            p.setCost(((double) p.getQuantity() / unitAmount) * unitPrice);

            // Runs if the prescription has never been filled before
            if (!rsRefill.next()) {

                // Create a record for this prescription in filled_prescriptions
                String newRefillSQL = "INSERT INTO filled_prescriptions (rxid, pharmacy_id, date_filled, refill_cost) VALUES (?, ?, ?, ?)";
                PreparedStatement newRefillStmt = conn.prepareStatement(newRefillSQL);
                newRefillStmt.setInt(1, p.getRxid());
                newRefillStmt.setInt(2, p.getPharmacyID());
                newRefillStmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                newRefillStmt.setDouble(4, p.getCost());
                newRefillStmt.execute();

                // Displays the date filled as today
                p.setDateFilled(java.sql.Date.valueOf(LocalDate.now()).toString());
                // Set refills remaining as the allowed number of refills
                p.setRefillsRemaining(p.getRefills());

                // returns the first instance of a fill for this prescription
                model.addAttribute("message", "Prescription filled.");
                model.addAttribute("prescription", p);
                return "prescription_show";
            }

            int numFills = p.getRefills();
            // checks prescription
            while(rsRefill.next()){
                numFills--;
            }

            // Check if we have exceeded the number of allowed refills
            // the first fill is not considered a refill.
//            int numFills = rsRefill.getFetchSize();
            p.setRefillsRemaining(numFills-1);
            if (p.getRefillsRemaining() < 0) {
                model.addAttribute("message", "No refills left.");
                model.addAttribute("prescription", p);
                return "prescription_fill";
            }

            // Continues if the prescription has been filled at least one time
            // and does not exceed the number of allowed refills

            // Calculates the cost of the refill
            p.setCost(((double) p.getQuantity() / unitAmount) * unitPrice);

            // Create a record for this refill in filled_prescriptions
            String newRefillSQL = "INSERT INTO filled_prescriptions (rxid, pharmacy_id, date_filled, refill_cost) VALUES (?, ?, ?, ?)";
            PreparedStatement newRefillStmt = conn.prepareStatement(newRefillSQL);
            newRefillStmt.setInt(1, p.getRxid());
            newRefillStmt.setInt(2, p.getPharmacyID());
            newRefillStmt.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            newRefillStmt.setDouble(4, p.getCost());
            newRefillStmt.execute();

            // Displays the date filled as today
            p.setDateFilled(java.sql.Date.valueOf(LocalDate.now()).toString());

            // Show the new prescription with the most fill information
            model.addAttribute("message", "Prescription refilled.");
            model.addAttribute("prescription", p);
            return "prescription_show";

        } catch (SQLException e) {
            model.addAttribute("message", "An error occurred: " + e.getMessage());
            model.addAttribute("prescription", p);
            return "prescription_fill";
        }
        /*
         * valid pharmacy name and address, get pharmacy id and phone
         */
        // TODO

        // TODO find the patient information

        // TODO find the prescription


        /*
         * have we exceeded the number of allowed refills
         * the first fill is not considered a refill.
         */

        // TODO

        /*
         * get doctor information
         */
        // TODO

        /*
         * calculate cost of prescription
         */
        // TODO

        // TODO save updated prescription

        // show the updated prescription with the most recent fill information

    }

    private Connection getConnection() throws SQLException {
        Connection conn = jdbcTemplate.getDataSource().getConnection();
        return conn;
    }

}