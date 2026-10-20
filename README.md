# Greedy Course Scheduler

A Java Swing desktop application that generates academic class schedules using a greedy scheduling approach. The system organizes courses, sections, room assignments, and time slots based on student population and selected academic term.

## Overview

The **Greedy Course Scheduler** is an academic scheduling application designed for BSCS, BSIS, and BSIT programs. It helps generate class schedules by assigning lecture and laboratory sessions to available days, time slots, and rooms while checking for scheduling conflicts.

Users can enter student populations for each program and year level, select an academic term, generate schedules, and export the results to a CSV file.

## Features

- **Academic Term Selection** — Generate schedules for Term 1, Term 2, or Term 3.
- **Student Population Input** — Enter student counts for each program and year level.
- **Automatic Section Generation** — Creates sections based on a maximum target of 40 students per section.
- **Automated Course Scheduling** — Assigns lecture and laboratory sessions to available days, time slots, and rooms.
- **Room Assignment** — Considers designated lecture rooms and laboratory rooms.
- **Conflict Checking** — Checks room availability and prevents overlapping schedules for the same section.
- **Split Scheduling** — Attempts to divide courses with durations of four hours or longer into separate sessions.
- **CSV Export** — Exports course counts, generated sections, and schedules to a CSV file.

## Technologies Used

- **Java** — Core programming language
- **Java Swing** — Graphical user interface
- **Greedy Scheduling Approach** — Scheduling and resource-assignment strategy
- **CSV File Handling** — Importing course data and exporting generated schedules
- **Apache NetBeans IDE** — Development environment

## Getting Started

### Prerequisites

Before running the application, make sure you have:

- Java Development Kit (JDK) installed
- Apache NetBeans IDE
- The NetBeans `AbsoluteLayout` dependency required by the generated interface
- A CSV file containing course information in the required format

### Installation

1. Clone or download this repository.
2. Open the project in Apache NetBeans IDE.
3. Ensure that the required project dependencies are available.
4. Prepare your course data CSV file using the format described below.
5. Place the CSV file at the expected project path.
6. Build and run the application.

## Input Data: Use Your Own CSV File

You can use your **own curriculum and course data** instead of relying on a predefined dataset. This allows you to adapt the scheduler to your institution's academic programs and scheduling requirements.

### Required CSV Format

Your CSV file must contain the following column headers in the specified order:

| Column | Description | Example |
|---|---|---|
| `Program` | Academic program | BSCS |
| `CourseCode` | Unique course identifier | CS101 |
| `CourseTitle` | Course name | Introduction to Programming |
| `YearLevel` | Student year level (1–4) | 1 |
| `TermNumber` | Academic term (1–3) | 1 |
| `HasLab` | Indicates whether the course has a laboratory component (`true` or `false`) | true |
| `LecHours` | Lecture duration in hours | 2 |
| `LabHours` | Laboratory duration in hours | 3 |

### How to Prepare Your CSV File

1. Open Microsoft Excel, Google Sheets, or another spreadsheet application.
2. Create a new spreadsheet.
3. Enter the required column headers exactly as shown above.
4. Add your course information, with one course per row.
5. Save or download the spreadsheet as a **CSV file**.
6. Name the file exactly:

   `Courses-Per-Program-Per-Year.csv`

7. Place it in the following location relative to the project root:

   `src/Courses-Per-Program-Per-Year.csv`

8. Run the application and select the academic term you want to schedule.

**Important:** The current implementation expects the CSV file to use the exact filename and relative path shown above. It does not currently provide a file picker for selecting arbitrary CSV files. To use a different filename or location, update the file path in the source code.

### CSV Data Guidelines

- Use the exact column names and order specified in the table.
- Use numeric values for `YearLevel`, `TermNumber`, `LecHours`, and `LabHours`.
- Use `true` or `false` for `HasLab`.
- Ensure that lecture and laboratory hours match the courses you want to schedule.
- Use double quotes around course titles containing commas.
- Make sure the CSV file contains a header row followed by course records.

You can use fictional or sample course information to test the application before using actual curriculum data.

### Data Privacy

Do not include confidential student information, passwords, or other sensitive personal data in your CSV file. The application requires course information and student counts entered through its interface, not individual student records.

## How to Use

1. Launch the application.
2. Select the desired academic term.
3. Enter the student population for each year level under BSCS, BSIS, and BSIT.
4. Click **Display Schedule** to generate the schedule and display the results in the console.
5. Click **Export to CSV File** to generate a CSV file containing course counts, generated sections, and schedule details.

Exported schedules are saved as `schedule_term_1.csv`, `schedule_term_2.csv`, or `schedule_term_3.csv`, depending on the selected term.

## Output

The generated schedule includes:

- Course code and title
- Assigned day
- Start and end times
- Session duration
- Assigned room
- Session type (lecture or laboratory)
- Generated sections and course counts

## Notes and Limitations

- Scheduling results depend on the supplied course data, room availability, and implemented scheduling constraints.
- The scheduling algorithm uses randomized ordering when attempting to assign courses. Results may vary between runs.
- The application checks for room and section time conflicts but does not guarantee that every course can be scheduled.
- Schedules are currently displayed in the console rather than in the application's graphical table.
- The CSV import path and expected filename are fixed in the current implementation.
- The application is an academic project and may require further testing and refinement before use in a real institution.

## Future Improvements

- Add a graphical table for displaying generated schedules.
- Allow users to browse for and select their own CSV files.
- Improve input validation and error handling.
- Provide schedule previews and conflict summaries.
- Improve scheduling reliability for difficult course combinations.
- Add reporting and visualization features.

## Author

Developed as an academic programming project.
