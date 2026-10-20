/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author marco
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;
import javax.swing.*;


public class Greedy extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Greedy.class.getName());
    

    public Greedy() {
        initComponents();
        ButtonGroup termGroup = new ButtonGroup();
        termGroup.add(rbTerm1);
        termGroup.add(rbTerm2);
        termGroup.add(rbTerm3);
        
        this.getContentPane().setBackground(new java.awt.Color(53, 64, 142));
        this.setSize(470, 400);
        this.setResizable(false);


    }

 
    static class Course {
        String code;
        String title;
        int yearLevel;
        int term;
        boolean hasLab;
        double lecHours;
        double labHours;

        Course(String code, String title, int yearLevel, int term, boolean hasLab, double lecHours, double labHours) {
            this.code = code;
            this.title = title;
            this.yearLevel = yearLevel;
            this.term = term;
            this.hasLab = hasLab;
            this.lecHours = lecHours;
            this.labHours = labHours;
        }
    }

    static class Section {
        String name; // for Sections
        String program;
        int year;

        Section(String program, int year, int idx) {
            this.program = program.toUpperCase();
            this.year = year;
            this.name = this.program + year + (char) ('A' + idx);
        }
    }
    // Models
    static class Booking {
        int day; // 0-5 ; Mon-Sat
        int start; // minutes from midnight
        int end;   // ending minutes
        String room;
        String sectionName;
        String courseCode;
        String type; // LEC or LAB

        Booking(int day, int start, int end, String room, String sectionName, String courseCode, String type) {
            this.day = day;
            this.start = start;
            this.end = end;
            this.room = room;
            this.sectionName = sectionName;
            this.courseCode = courseCode;
            this.type = type;
        }
    }

    static class ScheduleEntry {
        String courseCode;
        String courseTitle;
        String days; // Mon, Tue, etc. (we now store single day per entry)
        String time; // formatted string
        String durationStr; // formatted duration like "160m / 2.67h"
        String room; // room (single room per entry)
        String type; // LEC or LAB

        ScheduleEntry(String courseCode, String courseTitle, String days, String time, String durationStr, String room, String type) {
            this.courseCode = courseCode;
            this.courseTitle = courseTitle;
            this.days = days;
            this.time = time;
            this.durationStr = durationStr;
            this.room = room;
            this.type = type;
        }
    }

    // Constants
    private static final String[] LEC_ROOMS = {"Room 203", "Room 204", "V-Room"};
    private static final String[] LAB_ROOMS = {"COMLAB 1", "COMLAB 5", "COMLAB 6", "V-Room"};
    private static final String VROOM_NAME = "V-Room";

    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final int DAY_COUNT = 6;

    private static final int[] STARTS_120 = {420, 540, 660, 780, 900, 1020};
    private static final int[] STARTS_240 = {420, 540, 660, 780, 900, 1020};
    private static final int[] STARTS_160 = {420, 500, 580, 660, 740, 820, 900, 980};
    private static final int[] STARTS_80 = {420, 500, 580, 660, 740, 820, 900, 980, 1060};

    private static final int[][] SPLIT_PATTERNS = {{0, 3}, {1, 4}, {2, 5}};
    private static final String[] SPLIT_NAMES = {"Mon/Thu", "Tue/Fri", "Wed/Sat"};

    private static final Map<String, String> courseRoomMap = new HashMap<>();

    // courses per program per year are imported from csv file 
    private static Map<String, List<Course>> initCourses() {
        Map<String, List<Course>> map = new HashMap<>();
        String csvFile = "src/Courses-Per-Program-Per-Year.csv";
        String line;

        // robust split to avoid splitting on commas inside quotes
        String splitRegex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            // read header
            String header = br.readLine();
            if (header == null) {
                System.err.println("⚠️ Courses CSV is empty or missing.");
                return map;
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(splitRegex, -1);

                // Program,CourseCode,CourseTitle,YearLevel,TermNumber,HasLab,LecHours,LabHours
                if (parts.length < 8) continue;

                String program = parts[0].trim();
                String code = parts[1].trim();
                String title = parts[2].trim();
                
                int yearLevel;
                int term;
                try {
                    yearLevel = Integer.parseInt(parts[3].trim());
                } catch (NumberFormatException e) {
                    yearLevel = 1;
                }
                try {
                    term = Integer.parseInt(parts[4].trim());
                } catch (NumberFormatException e) {
                    term = 1;
                }
                boolean hasLab = Boolean.parseBoolean(parts[5].trim().toLowerCase());
                double lecHours = 0;
                double labHours = 0;
                try {
                    lecHours = Double.parseDouble(parts[6].trim());
                } catch (Exception ex) { lecHours = 0; }
                try {
                    labHours = Double.parseDouble(parts[7].trim());
                } catch (Exception ex) { labHours = 0; }

                Course c = new Course(code, title, yearLevel, term, hasLab, lecHours, labHours);
                map.computeIfAbsent(program, k -> new ArrayList<>()).add(c);
            }

        } catch (IOException e) {
            System.err.println("⚠️ Error reading CSV (" + csvFile + "): " + e.getMessage());
        }

        return map;
    }

    // Functions
    private static String fmtTime(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        String ampm = (h < 12) ? "AM" : "PM";
        int hr12 = h % 12;
        if (hr12 == 0) hr12 = 12;
        return String.format("%d:%02d %s", hr12, m, ampm);
    }

    private static boolean overlaps(int s1, int e1, int s2, int e2) {
        return s1 < e2 && s2 < e1;
    }

    private static boolean isRoomFree(List<Booking> roomBookings, String roomName, int day, int start, int end) {
        if (VROOM_NAME.equals(roomName)) return true;
        for (Booking b : roomBookings) {
            if (b.day == day && overlaps(b.start, b.end, start, end)) return false;
        }
        return true;
    }

    private static boolean isSectionFree(List<Booking> secBookings, int day, int start, int end) {
        for (Booking b : secBookings) {
            if (b.day == day && overlaps(b.start, b.end, start, end)) return false;
        }
        return true;
    }

    private static boolean tryPlaceStraight(String room, int day, int start, int dur, String sectionName,
                                           String courseCode, String type,
                                           Map<String, List<Booking>> roomMap,
                                           Map<String, List<Booking>> sectionMap) {
        int end = start + dur;
        List<Booking> rlist = roomMap.computeIfAbsent(room, k -> new ArrayList<>());
        List<Booking> slist = sectionMap.computeIfAbsent(sectionName, k -> new ArrayList<>());
        Booking b = new Booking(day, start, end, room, sectionName, courseCode, type);
        rlist.add(b);
        slist.add(b);
        return true;
    }

    private static List<Integer> buildRoomOrder(Section sec, boolean isLab, String[] rooms) {
        List<Integer> order = new ArrayList<>();
        int vIdx = -1;
        for (int i = 0; i < rooms.length; i++) {
            if (VROOM_NAME.equals(rooms[i])) vIdx = i;
        }

        if (isLab) {
            for (int i = 0; i < rooms.length; i++) {
                if (i == vIdx) continue;
                order.add(i);
            }
        } else {
            if (sec.year <= 2) {
                if (vIdx != -1) order.add(vIdx);
                for (int i = 0; i < rooms.length; i++) {
                    if (i == vIdx) continue;
                    order.add(i);
                }
            } else {
                for (int i = 0; i < rooms.length; i++) {
                    if (i == vIdx) continue;
                    order.add(i);
                }
            }
        }
        return order;
    }

    private static String formatDuration(int minutes) {
        double hours = minutes / 60.0;
        return String.format("%dm / %.2fh", minutes, hours);
    }

    // Scheduling logic
    private static boolean schedulePart(Section sec, Course c, String partType,
                                        Map<String, List<Booking>> roomMap,
                                        Map<String, List<Booking>> sectionMap,
                                        Map<String, List<ScheduleEntry>> outputMap,
                                        Random rand) {
        boolean isLab = partType.equals("LAB");
        double hours = isLab ? c.labHours : c.lecHours;
        int dur = (int) Math.round(hours * 60);
        if (dur <= 0) return true;

        String[] rooms = isLab ? LAB_ROOMS : LEC_ROOMS;

        int[] startsForThisDur;
        if (dur == 240) startsForThisDur = STARTS_240;
        else if (dur == 160) startsForThisDur = STARTS_160;
        else if (dur == 120) startsForThisDur = STARTS_120;
        else if (dur == 80) startsForThisDur = STARTS_80;
        else startsForThisDur = STARTS_120;

        List<Integer> dayList = new ArrayList<>();
        for (int d = 0; d < DAY_COUNT; d++) dayList.add(d);
        Collections.shuffle(dayList, rand);

        List<Integer> roomIndices = buildRoomOrder(sec, isLab, rooms);
        List<Integer> startList = new ArrayList<>();
        for (int st : startsForThisDur) startList.add(st);
        Collections.shuffle(startList, rand);

        // Force split for ALL courses with 4 hours or longer
        if (dur >= 240 && dur % 2 == 0) {
            int halfDur = dur / 2;
            int[] halfStarts;
            if (halfDur == 120) halfStarts = STARTS_120;
            else if (halfDur == 80) halfStarts = STARTS_80;
            else if (halfDur == 160) halfStarts = STARTS_160;
            else halfStarts = STARTS_120;

            List<Integer> halfStartList = new ArrayList<>();
            for (int st : halfStarts) halfStartList.add(st);
            Collections.shuffle(halfStartList, rand);

            for (int[] pat : SPLIT_PATTERNS) {
                int dayA = pat[0];
                int dayB = pat[1];

                for (int s : halfStartList) {
                    int e = s + halfDur;

                    for (int riA : roomIndices) {
                        for (int riB : roomIndices) {
                            String roomA = rooms[riA];
                            String roomB = rooms[riB];

                            if (isLab && (VROOM_NAME.equals(roomA) || VROOM_NAME.equals(roomB))) continue;

                            List<Booking> rlistA = roomMap.computeIfAbsent(roomA, k -> new ArrayList<>());
                            List<Booking> rlistB = roomMap.computeIfAbsent(roomB, k -> new ArrayList<>());
                            List<Booking> slist = sectionMap.computeIfAbsent(sec.name, k -> new ArrayList<>());

                            boolean ok = true;
                            if (!isRoomFree(rlistA, roomA, dayA, s, e)) ok = false;
                            if (!isRoomFree(rlistB, roomB, dayB, s, e)) ok = false;
                            if (!isSectionFree(slist, dayA, s, e)) ok = false;
                            if (!isSectionFree(slist, dayB, s, e)) ok = false;

                            if (ok) {
                                tryPlaceStraight(roomA, dayA, s, halfDur, sec.name, c.code, partType, roomMap, sectionMap);
                                tryPlaceStraight(roomB, dayB, s, halfDur, sec.name, c.code, partType, roomMap, sectionMap);

                                String timeStr = fmtTime(s) + " - " + fmtTime(e);
                                String durationStr = formatDuration(halfDur);
                                ScheduleEntry entryA = new ScheduleEntry(c.code, c.title, DAY_NAMES[dayA], timeStr, durationStr, roomA, partType);
                                ScheduleEntry entryB = new ScheduleEntry(c.code, c.title, DAY_NAMES[dayB], timeStr, durationStr, roomB, partType);
                                outputMap.computeIfAbsent(sec.name, k -> new ArrayList<>()).add(entryA);
                                outputMap.computeIfAbsent(sec.name, k -> new ArrayList<>()).add(entryB);
                                return true;
                            }
                        }
                    }
                }
            }

            // if can't split, fallback to single 4-hour block
        }

        // Try straight (only for shorter courses)
        if (dur < 240) {
            for (int d : dayList) {
                String dayLabel = DAY_NAMES[d];
                for (int ri : roomIndices) {
                    String room = rooms[ri];
                    for (int s : startList) {
                        int e = s + dur;
                        List<Booking> rlist = roomMap.computeIfAbsent(room, k -> new ArrayList<>());
                        List<Booking> slist = sectionMap.computeIfAbsent(sec.name, k -> new ArrayList<>());
                        if (isRoomFree(rlist, room, d, s, e) && isSectionFree(slist, d, s, e)) {
                            if (isLab && VROOM_NAME.equals(room)) continue;
                            if (!isLab && sec.year > 2 && VROOM_NAME.equals(room)) continue;

                            tryPlaceStraight(room, d, s, dur, sec.name, c.code, partType, roomMap, sectionMap);
                            String timeStr = fmtTime(s) + " - " + fmtTime(e);
                            String durationStr = formatDuration(dur);
                            ScheduleEntry entry = new ScheduleEntry(c.code, c.title, dayLabel, timeStr, durationStr, room, partType);
                            outputMap.computeIfAbsent(sec.name, k -> new ArrayList<>()).add(entry);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // Generated schedule exported to a CSV file
    private static void exportFullScheduleCSV(
            Map<String, Map<Integer, Integer>> courseCounts,
            List<Section> allSections,
            Map<String, List<ScheduleEntry>> sectionSchedules,
            String filename) {

        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            //Displays COURSE COUNTS
            pw.println("COURSE COUNTS");
            pw.println("Program,Year,Courses");
            for (String program : courseCounts.keySet()) {
                Map<Integer, Integer> yearMap = courseCounts.get(program);
                for (int year : new TreeSet<>(yearMap.keySet())) {
                    pw.printf("%s,%d,%d%n", program, year, yearMap.get(year));
                }
            }
            pw.println();

            //Displays GENERATED SECTIONS
            pw.println("GENERATED SECTIONS");
            pw.println("Section,Courses");
            for (Section sec : allSections) {
                int courseCount = courseCounts.getOrDefault(sec.program, new HashMap<>())
                    .getOrDefault(sec.year, 0);
                pw.printf("%s,%d%n", sec.name, courseCount);
}

            pw.println();

            //Displays generated SCHEDULES
            pw.println("SCHEDULES");
            pw.println("Section,Course Code,Course Title,Day,Time,Duration,Room,Type");
            for (Section sec : allSections) {
                List<ScheduleEntry> entries = sectionSchedules.getOrDefault(sec.name, Collections.emptyList());
                entries.sort(Comparator.comparing((ScheduleEntry e) -> e.courseCode).thenComparing(e -> e.days));
                for (ScheduleEntry e : entries) {
                    pw.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                            sec.name,
                            e.courseCode,
                            e.courseTitle.replace(",", " "), // remove commas
                            e.days,
                            e.time,
                            e.durationStr,
                            e.room,
                            e.type);
                }
            }

            System.out.println("\n✅ Full schedule exported to " + filename);

        } catch (IOException e) {
            System.err.println("❌ Error writing CSV: " + e.getMessage());
        }
    }

    //Data from user input in form (term number and student counts)
    private void generateSchedule(int selectedTerm, Map<String, int[]> studentCounts, boolean exportToCSV) {
        Map<String, List<Course>> courseMap = initCourses();
        int term = selectedTerm;

        //Displays COURSE COUNTS
        System.out.println("\n=== COURSE COUNTS FOR TERM " + term + " ===");

        Map<String, Map<Integer, Integer>> courseCounts = new HashMap<>();
        for (Map.Entry<String, List<Course>> entry : courseMap.entrySet()) {
            String program = entry.getKey();
            for (Course c : entry.getValue()) {
                if (c.term == term) {
                    courseCounts.putIfAbsent(program, new HashMap<>());
                    Map<Integer, Integer> yearMap = courseCounts.get(program);
                    yearMap.put(c.yearLevel, yearMap.getOrDefault(c.yearLevel, 0) + 1);
                }
            }
        }

        for (String program : courseCounts.keySet()) {
            Map<Integer, Integer> yearMap = courseCounts.get(program);
            for (int year : new TreeSet<>(yearMap.keySet())) {
                System.out.printf("%s Year %d - %d courses%n", program, year, yearMap.get(year));
            }
        }

        // GENERATED SECTIONS based on the user input from UI (number of students)
        List<Section> allSections = new ArrayList<>();

        for (String program : studentCounts.keySet()) {
            int[] counts = studentCounts.get(program); // index 0->year1 ... 3->year4
            for (int i = 0; i < counts.length; i++) {
                int students = counts[i];
                int needed = (int) Math.ceil(students / 40.0);
                for (int s = 0; s < needed; s++) {
                    allSections.add(new Section(program, i + 1, s));
                }
            }
        }

        System.out.println("\n=== GENERATED SECTIONS ===");
        for (Section s : allSections) {
            int count = 0;
            if (courseCounts.containsKey(s.program)) {
                count = courseCounts.get(s.program).getOrDefault(s.year, 0);
            }
            System.out.printf("%-7s - %d courses%n", s.name, count);
        }

        //Displays SCHEDULING
        Map<String, List<Booking>> roomBookings = new HashMap<>();
        Map<String, List<Booking>> sectionBookings = new HashMap<>();
        Map<String, List<ScheduleEntry>> sectionSchedules = new HashMap<>();

        Random rand = new Random();

        for (Section sec : allSections) {
            List<Course> list = courseMap.getOrDefault(sec.program, Collections.emptyList());
            for (Course c : list) {
                if (c.yearLevel == sec.year && c.term == term) {
                    if (c.lecHours > 0.01) {
                        boolean ok = schedulePart(sec, c, "LEC", roomBookings, sectionBookings, sectionSchedules, rand);
                        if (!ok) System.out.println("⚠ Could not place LEC for " + c.code + " in section " + sec.name);
                    }
                    if (c.labHours > 0.01) {
                        boolean ok2 = schedulePart(sec, c, "LAB", roomBookings, sectionBookings, sectionSchedules, rand);
                        if (!ok2) System.out.println("⚠ Could not place LAB for " + c.code + " in section " + sec.name);
                    }
                }
            }
        }

        // === PRINT FULL SCHEDULE PER SECTION ===
        for (Section sec : allSections) {
            System.out.println("\n====================");
            System.out.println("Schedule for " + sec.name);
            System.out.println("====================\n");
            System.out.printf("%-12s | %-47s | %-10s | %-20s | %-12s | %-12s | %-5s%n",
                    "CourseCode", "Course Title", "Days", "Time", "Duration", "Room", "Type");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            List<ScheduleEntry> entries = sectionSchedules.getOrDefault(sec.name, Collections.emptyList());
            entries.sort(Comparator.comparing((ScheduleEntry e) -> e.courseCode).thenComparing(e -> e.days));
            for (ScheduleEntry e : entries) {
                System.out.printf("%-12s | %-47s | %-10s | %-20s | %-12s | %-12s | %-5s%n",
                        e.courseCode, e.courseTitle, e.days, e.time, e.durationStr, e.room, e.type);
            }
            System.out.println();
        }

        // EXPORT CSV if the button is clicked
        if (exportToCSV) {
            String filename = "schedule_term_" + term + ".csv";
            exportFullScheduleCSV(courseCounts, allSections, sectionSchedules, filename);
        }
    }

    // UI helper: parse int from text field
    private int parseInt(javax.swing.JTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    // Called when user clicks display or export
    private void generateScheduleFromForm(boolean exportToCSV) {
        int selectedTerm = rbTerm1.isSelected() ? 1 :
                           rbTerm2.isSelected() ? 2 :
                           rbTerm3.isSelected() ? 3 : 0;

        if (selectedTerm == 0) {
            JOptionPane.showMessageDialog(this, "Please select a term number first!");
            return;
        }

        Map<String, int[]> studentCounts = new LinkedHashMap<>();
        studentCounts.put("BSCS", new int[]{
            parseInt(txtBSCSY1), parseInt(txtBSCSY2), parseInt(txtBSCSY3), parseInt(txtBSCSY4)
        });
        studentCounts.put("BSIS", new int[]{
            parseInt(txtBSISY1), parseInt(txtBSISY2), parseInt(txtBSISY3), parseInt(txtBSISY4)
        });
        studentCounts.put("BSIT", new int[]{
            parseInt(txtBSITY1), parseInt(txtBSITY2), parseInt(txtBSITY3), parseInt(txtBSITY4)
        });

        generateSchedule(selectedTerm, studentCounts, exportToCSV);
        if (!exportToCSV) {
            JOptionPane.showMessageDialog(this, "Schedule printed to console.");
        } else {
            JOptionPane.showMessageDialog(this, "Schedule exported to CSV.");
        }
    }

  

    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Greedy().setVisible(true));
    }

    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        rbTerm1 = new javax.swing.JRadioButton();
        rbTerm2 = new javax.swing.JRadioButton();
        rbTerm3 = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtBSCSY4 = new javax.swing.JTextField();
        txtBSCSY1 = new javax.swing.JTextField();
        txtBSCSY2 = new javax.swing.JTextField();
        txtBSCSY3 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        txtBSISY4 = new javax.swing.JTextField();
        txtBSISY1 = new javax.swing.JTextField();
        txtBSISY2 = new javax.swing.JTextField();
        txtBSISY3 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        txtBSITY4 = new javax.swing.JTextField();
        txtBSITY1 = new javax.swing.JTextField();
        txtBSITY2 = new javax.swing.JTextField();
        txtBSITY3 = new javax.swing.JTextField();
        btnExportCSVFile = new javax.swing.JButton();
        btnDisplaySchedule = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(53, 64, 142));
        setMinimumSize(new java.awt.Dimension(450, 350));
        setPreferredSize(new java.awt.Dimension(450, 350));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 212, 26));
        jLabel1.setText("GENERATE SCHEDULE");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Enter term number:");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 50, -1, -1));

        rbTerm1.setForeground(new java.awt.Color(255, 255, 255));
        rbTerm1.setText("Term 1");
        rbTerm1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbTerm1ActionPerformed(evt);
            }
        });
        getContentPane().add(rbTerm1, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 50, -1, -1));

        rbTerm2.setForeground(new java.awt.Color(255, 255, 255));
        rbTerm2.setText("Term 2");
        rbTerm2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbTerm2ActionPerformed(evt);
            }
        });
        getContentPane().add(rbTerm2, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 50, -1, -1));

        rbTerm3.setForeground(new java.awt.Color(255, 255, 255));
        rbTerm3.setText("Term 3");
        getContentPane().add(rbTerm3, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 50, -1, -1));

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Enter number of students:");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 80, -1, -1));

        jLabel4.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 212, 26));
        jLabel4.setText("BSIS");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 110, -1, -1));

        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("BSCS Year 1");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 140, -1, -1));

        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("BSCS Year 2");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, -1, -1));

        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("BSCS Year 3");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 200, -1, -1));

        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("BSCS Year 4");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 230, -1, -1));
        getContentPane().add(txtBSCSY4, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 230, 40, -1));
        getContentPane().add(txtBSCSY1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 140, 40, -1));
        getContentPane().add(txtBSCSY2, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 170, 40, -1));
        getContentPane().add(txtBSCSY3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 200, 40, -1));

        jLabel9.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 212, 26));
        jLabel9.setText("BSCS");
        getContentPane().add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 110, -1, -1));

        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("BSIS Year 1");
        getContentPane().add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 140, -1, -1));

        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("BSIS Year 2");
        getContentPane().add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 170, -1, -1));

        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("BSIS Year 3");
        getContentPane().add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 200, -1, -1));

        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("BSIS Year 4");
        getContentPane().add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 230, -1, -1));
        getContentPane().add(txtBSISY4, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 230, 40, -1));
        getContentPane().add(txtBSISY1, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 140, 40, -1));
        getContentPane().add(txtBSISY2, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 170, 40, -1));
        getContentPane().add(txtBSISY3, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 200, 40, -1));

        jLabel14.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 212, 26));
        jLabel14.setText("BSIT");
        getContentPane().add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 110, -1, -1));

        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("BSIT Year 1");
        getContentPane().add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 140, -1, -1));

        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("BSIT Year 2");
        getContentPane().add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 170, -1, -1));

        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("BSIT Year 3");
        getContentPane().add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 200, -1, -1));

        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setText("BSIT Year 4");
        getContentPane().add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 230, -1, -1));
        getContentPane().add(txtBSITY4, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 230, 40, -1));
        getContentPane().add(txtBSITY1, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 140, 40, -1));
        getContentPane().add(txtBSITY2, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 170, 40, -1));
        getContentPane().add(txtBSITY3, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 200, 40, -1));

        btnExportCSVFile.setBackground(new java.awt.Color(255, 212, 26));
        btnExportCSVFile.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        btnExportCSVFile.setForeground(new java.awt.Color(53, 64, 142));
        btnExportCSVFile.setText("EXPORT TO CSV FILE");
        btnExportCSVFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportCSVFileActionPerformed(evt);
            }
        });
        getContentPane().add(btnExportCSVFile, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 320, 170, -1));

        btnDisplaySchedule.setBackground(new java.awt.Color(255, 212, 26));
        btnDisplaySchedule.setFont(new java.awt.Font("Helvetica Neue", 1, 13)); // NOI18N
        btnDisplaySchedule.setForeground(new java.awt.Color(53, 64, 142));
        btnDisplaySchedule.setText("DISPLAY SCHEDULE");
        btnDisplaySchedule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDisplayScheduleActionPerformed(evt);
            }
        });
        getContentPane().add(btnDisplaySchedule, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 290, 170, -1));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    

    private void rbTerm1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbTerm1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbTerm1ActionPerformed

    private void rbTerm2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbTerm2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbTerm2ActionPerformed

    private void btnExportCSVFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportCSVFileActionPerformed
        generateScheduleFromForm(true); // true = export to CSV
    }//GEN-LAST:event_btnExportCSVFileActionPerformed

    private void btnDisplayScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDisplayScheduleActionPerformed
        generateScheduleFromForm(false); // false = only display schedule
    }//GEN-LAST:event_btnDisplayScheduleActionPerformed

    /**
     * @param args the command line arguments
     */
   

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDisplaySchedule;
    private javax.swing.JButton btnExportCSVFile;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JRadioButton rbTerm1;
    private javax.swing.JRadioButton rbTerm2;
    private javax.swing.JRadioButton rbTerm3;
    private javax.swing.JTextField txtBSCSY1;
    private javax.swing.JTextField txtBSCSY2;
    private javax.swing.JTextField txtBSCSY3;
    private javax.swing.JTextField txtBSCSY4;
    private javax.swing.JTextField txtBSISY1;
    private javax.swing.JTextField txtBSISY2;
    private javax.swing.JTextField txtBSISY3;
    private javax.swing.JTextField txtBSISY4;
    private javax.swing.JTextField txtBSITY1;
    private javax.swing.JTextField txtBSITY2;
    private javax.swing.JTextField txtBSITY3;
    private javax.swing.JTextField txtBSITY4;
    // End of variables declaration//GEN-END:variables
}
