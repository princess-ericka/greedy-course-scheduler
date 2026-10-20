## Input Data: Custom CSV Files

This application allows users to provide their own CSV file containing course information. You can customize the input data according to your institution's curriculum and scheduling requirements.

### Required CSV Format

Your CSV file must contain the following column headers in the specified order:

| Column | Description | Example |
|---|---|---|
| `Program` | Academic program | BSCS |
| `CourseCode` | Course identifier | CS101 |
| `CourseTitle` | Course name | Introduction to Programming |
| `YearLevel` | Year level (1–4) | 1 |
| `TermNumber` | Academic term (1–3) | 1 |
| `HasLab` | Whether the course has a laboratory component (`true` or `false`) | true |
| `LecHours` | Lecture duration in hours | 2 |
| `LabHours` | Laboratory duration in hours | 3 |

### How to Use Your Own CSV File

1. Create a CSV file using Microsoft Excel, Google Sheets, or another spreadsheet editor.
2. Add the required column headers exactly as shown above.
3. Enter your course information, ensuring that each row represents one course.
4. Save or download the file in CSV format.
5. Place the file at the following location within the project:

   `src/Courses-Per-Program-Per-Year.csv`

6. Run the application and select the academic term for which you want to generate a schedule.

**Important:** The current application expects the CSV file to have the exact filename and location shown above. If you use a different filename, you must update the file path in the source code.

### Sample Data

You may use your own curriculum data or create sample courses for testing. Ensure that the values match the required format and that the course hours are valid for your scheduling requirements.

### Data Privacy

Do not use confidential student information or other sensitive personal data in your CSV file. The application only needs course information and student counts entered through the interface.
