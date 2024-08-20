This database and web application is designed for a pharmacy company to record and maintain information regarding prescriptions for patients. This database contains data about patients and doctors, prescriptions, prescription refills, drug prices, and a drug inventory. The database uses Java, Spring Boot, Mysql, and Maven. The goal is to provide accurate prescription management, including: creation, validation, filling, and tracking across different pharmacies. The design maintains referential integrity across doctors, patients, and drugs. The program also tracks prescription refills and pricing.  The entities and relationships in this database include:

- **Doctor:** Stores information about doctors, including SSN, last name, first name, specialty, and their first year of practice.

- **Patient:** Contains details such as ID, SSN, last name, first name, address, birthdate, and associated doctor.

- **Drug:** Stores a drug ID and drug name.

- **Pharmacy:** Details about pharmacies where prescriptions can be filled. Attributes include ID, name, address, and phone number.

- **Pharmacy Inventory:** Contains the cost of drugs (per unit) at different pharmacies.

- **Prescription:** Stores prescription details filled in by a doctor, including RXID, patient ID, doctor ID, drug ID, date prescribed, drug quantity, and refills allowed.

- **Filled Prescriptions:** Contains an entry for each time a prescription is filled. Each entry  records the prescription RXID, pharmacy ID, date filled, and refill cost.
