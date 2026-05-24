package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

// Language dictionary
object Trans {
    var isArabic by mutableStateOf(false)

    fun t(en: String, ar: String): String = if (isArabic) ar else en

    // App constants
    val schoolName get() = t("Omdur English Kindergarten & Schools", "روضة ومدارس أمدر الإنجليزية")
    val subtitle get() = t("English Knowledge Schools", "مدارس المعرفة الإنجليزية")
    val abbreviation get() = t("OEKS", "أمدر")
    val tagline get() = t("Your Child's Future Starts Here", "مستقبل طفلك يبدأ من هنا")
    val home get() = t("Home", "الرئيسية")
    val payments get() = t("Payments", "المدفوعات")
    val aiHelp get() = t("AI Help", "المساعد الذكي")
    val alerts get() = t("Alerts", "التنبيهات")
    val enroll get() = t("Enroll", "التسجيل")
    
    // Roles
    val roleParent get() = t("Parent", "ولي الأمر")
    val roleTeacher get() = t("Teacher", "المعلم")
    val roleManager get() = t("Manager", "المدير")
}

// Data Classes for Parent View
data class Child(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val gradeEn: String,
    val gradeAr: String,
    val sectionEn: String,
    val sectionAr: String,
    val averageGrade: String,
    val dueSoonEn: String,
    val dueSoonAr: String,
    val dueSoonVal: Int,
    val alertCount: Int,
    val liveStatusEn: String,
    val liveStatusAr: String,
    val liveTime: String,
    val grades: List<SubjectGrade>,
    val paymentsPaid: Int,
    val paymentsRemaining: Int,
    val paymentsPercent: Float,
    val paymentsInstallmentCount: String
) {
    val name get() = Trans.t(nameEn, nameAr)
    val grade get() = Trans.t(gradeEn, gradeAr)
    val section get() = Trans.t(sectionEn, sectionAr)
    val dueSoon get() = Trans.t(dueSoonEn, dueSoonAr)
    val liveStatus get() = Trans.t(liveStatusEn, liveStatusAr)
}

data class SubjectGrade(
    val subjectEn: String,
    val subjectAr: String,
    val percentage: Int
) {
    val subject get() = Trans.t(subjectEn, subjectAr)
}

data class TimetableRow(
    val time: String,
    val subjectEn: String,
    val subjectAr: String,
    val isBreak: Boolean = false,
    val isCurrent: Boolean = false,
    val isUpcoming: Boolean = false,
    val teacherNameEn: String = "",
    val teacherNameAr: String = "",
    val color: Color
) {
    val subject get() = Trans.t(subjectEn, subjectAr)
    val teacherName get() = Trans.t(teacherNameEn, teacherNameAr)
}

data class TeacherModel(
    val initials: String,
    val nameEn: String,
    val nameAr: String,
    val subjectEn: String,
    val subjectAr: String,
    val avatarBg: Color
) {
    val name get() = Trans.t(nameEn, nameAr)
    val subject get() = Trans.t(subjectEn, subjectAr)
}

data class BoardSummary(
    val subjectEn: String,
    val subjectAr: String,
    val time: String,
    val imageAvailable: Boolean,
    val color: Color
) {
    val subject get() = Trans.t(subjectEn, subjectAr)
}

data class AlertNotification(
    val type: String, // "PAYMENT", "GRADE", "HOLIDAY", "REPORT"
    val textEn: String,
    val textAr: String,
    val timeEn: String,
    val timeAr: String
) {
    val text get() = Trans.t(textEn, textAr)
    val time get() = Trans.t(timeEn, timeAr)
}

data class ChatMessage(
    val isUser: Boolean,
    val text: String
)

// Teacher view structures
data class StudentModel(
    val nameEn: String,
    val nameAr: String,
    val gradeEn: String,
    val gradeAr: String,
    val percentage: Int,
    val isOnline: Boolean,
    val initials: String,
    val avatarBg: Color
) {
    val name get() = Trans.t(nameEn, nameAr)
    val grade get() = Trans.t(gradeEn, gradeAr)
}

data class GradebookRow(
    val nameEn: String,
    val nameAr: String,
    val cw: Int,
    val hw: Int,
    val quiz: Int,
    val test: Int,
    val avg: Int
) {
    val name get() = Trans.t(nameEn, nameAr)
}

// Manager structures
data class ApplicationModel(
    val id: String,
    val childEn: String,
    val childAr: String,
    val gradeEn: String,
    val gradeAr: String,
    val parentEn: String,
    val parentAr: String,
    val phone: String,
    val timeEn: String,
    val timeAr: String,
    var approved: Boolean = false,
    var fading: Boolean = false
) {
    val child get() = Trans.t(childEn, childAr)
    val grade get() = Trans.t(gradeEn, gradeAr)
    val parent get() = Trans.t(parentEn, parentAr)
    val time get() = Trans.t(timeEn, timeAr)
}

data class ActivityLog(
    val type: String, // "GREEN", "BLUE", "RED", "YELLOW"
    val textEn: String,
    val textAr: String,
    val timeEn: String,
    val timeAr: String
) {
    val text get() = Trans.t(textEn, textAr)
    val time get() = Trans.t(timeEn, timeAr)
}

// Enrollment structures
data class GradeCardModel(
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val fee: Int,
    val installment: Int,
    val teacherEn: String,
    val teacherAr: String,
    val seats: Int,
    val isLow: Boolean
) {
    val name get() = Trans.t(nameEn, nameAr)
    val teacher get() = Trans.t(teacherEn, teacherAr)
}

// Global Mock Repo
object MockRepo {
    val children = listOf(
        Child(
            id = "ahmad",
            nameEn = "Ahmad Al-Rashidi",
            nameAr = "أحمد الرشيدي",
            gradeEn = "Year 6",
            gradeAr = "الصف السادس",
            sectionEn = "Section B",
            sectionAr = "الفصل ب",
            averageGrade = "87%",
            dueSoonEn = "1,200 EGP",
            dueSoonAr = "١,٢٠٠ ج.م",
            dueSoonVal = 1200,
            alertCount = 3,
            liveStatusEn = "In Class: Science",
            liveStatusAr = "في الحصة: العلوم",
            liveTime = "11:00 AM – 11:45 AM",
            grades = listOf(
                SubjectGrade("Mathematics", "الرياضيات", 92),
                SubjectGrade("Science", "العلوم", 88),
                SubjectGrade("English", "اللغة الإنجليزية", 91),
                SubjectGrade("Arabic", "اللغة العربية", 78),
                SubjectGrade("History & Geo", "التاريخ والجغرافيا", 74),
                SubjectGrade("ICT", "الحاسب الآلي", 95),
                SubjectGrade("Art", "التربية الفنية", 82)
            ),
            paymentsPaid = 10800,
            paymentsRemaining = 7200,
            paymentsPercent = 0.6f,
            paymentsInstallmentCount = "6 of 10 months"
        ),
        Child(
            id = "sara",
            nameEn = "Sara Al-Rashidi",
            nameAr = "سارة الرشيدي",
            gradeEn = "Year 4",
            gradeAr = "الصف الرابع",
            sectionEn = "Section A",
            sectionAr = "الفصل أ",
            averageGrade = "94%",
            dueSoonEn = "1,000 EGP",
            dueSoonAr = "١,٠٠٠ ج.م",
            dueSoonVal = 1000,
            alertCount = 1,
            liveStatusEn = "In Class: English",
            liveStatusAr = "في الحصة: الإنجليزية",
            liveTime = "11:00 AM – 11:45 AM",
            grades = listOf(
                SubjectGrade("Mathematics", "الرياضيات", 95),
                SubjectGrade("Science", "العلوم", 92),
                SubjectGrade("English", "اللغة الإنجليزية", 96),
                SubjectGrade("Arabic", "اللغة العربية", 89),
                SubjectGrade("History & Geo", "التاريخ والجغرافيا", 85),
                SubjectGrade("ICT", "الحاسب الآلي", 98),
                SubjectGrade("Art", "التربية الفنية", 90)
            ),
            paymentsPaid = 8000,
            paymentsRemaining = 4000,
            paymentsPercent = 0.66f,
            paymentsInstallmentCount = "8 of 12 months"
        ),
        Child(
            id = "yousef",
            nameEn = "Yousef Al-Rashidi",
            nameAr = "يوسف الرشيدي",
            gradeEn = "KG 2",
            gradeAr = "الروضة الثانية",
            sectionEn = "Class C",
            sectionAr = "فصل ج",
            averageGrade = "91%",
            dueSoonEn = "708 EGP",
            dueSoonAr = "٧٠٨ ج.م",
            dueSoonVal = 708,
            alertCount = 2,
            liveStatusEn = "In Class: Play & Learn",
            liveStatusAr = "في الحصة: العب وتعلم",
            liveTime = "11:00 AM – 11:45 AM",
            grades = listOf(
                SubjectGrade("Math Play", "لعب الحساب", 90),
                SubjectGrade("Phonics", "الصوتيات", 94),
                SubjectGrade("Arabic Lettering", "الحروف العربية", 88),
                SubjectGrade("Fine Motor Skills", "المهارات الحركية", 93)
            ),
            paymentsPaid = 5664,
            paymentsRemaining = 2836,
            paymentsPercent = 0.66f,
            paymentsInstallmentCount = "8 of 12 months"
        )
    )

    val listTimetable = listOf(
        TimetableRow("8:00 AM", "Assembly", "طابور الصباح", isBreak = false, color = GrayState),
        TimetableRow("8:30 AM", "Mathematics", "الرياضيات", isBreak = false, color = GrayState),
        TimetableRow("9:15 AM", "English", "اللغة الإنجليزية", isBreak = false, color = GrayState),
        TimetableRow("10:00 AM", "Break", "فترة الاستراحة", isBreak = true, color = YellowState),
        TimetableRow("10:20 AM", "Arabic", "اللغة العربية", isBreak = false, color = GrayState),
        TimetableRow("11:00 AM", "Science NOW — Mr. Hassan", "العلوم الآن — أ/ حسن", isBreak = false, isCurrent = true, color = RoyalBlue),
        TimetableRow("11:45 AM", "ICT — Ms. Layla", "الحاسب — أ/ ليلى", isBreak = false, isUpcoming = true, color = GreenState),
        TimetableRow("12:30 PM", "Lunch Break", "استراحة الغداء", isBreak = true, color = YellowState),
        TimetableRow("1:00 PM", "History", "التاريخ والجغرافيا", isBreak = false, isUpcoming = true, color = GreenState),
        TimetableRow("1:45 PM", "Art", "التربية الفنية", isBreak = false, isUpcoming = true, color = GreenState),
        TimetableRow("2:30 PM", "Home Time", "الانصراف للمنزل", isBreak = true, color = YellowState)
    )

    val listTeachers = listOf(
        TeacherModel("MH", "Mr. Mohamed Hassan", "أ/ محمد حسن", "Science", "العلوم", RoyalBlue),
        TeacherModel("LS", "Ms. Layla Sayed", "أ/ ليلى سيد", "ICT & Mathematics", "الحاسب والرياضيات", CrimsonRed),
        TeacherModel("RK", "Mr. Rami Khaled", "أ/ رامي خالد", "English Language", "اللغة الإنجليزية", GreenState),
        TeacherModel("NF", "Ms. Nadia Fahmy", "أ/ نادية فهمي", "Arabic", "اللغة العربية", YellowState),
        TeacherModel("YA", "Mr. Youssef Amin", "أ/ يوسف أمين", "History & Geography", "التاريخ والجغرافيا", PurpleState)
    )

    val listBoards = listOf(
        BoardSummary("Mathematics", "الرياضيات", "8:30 AM", true, RoyalBlue),
        BoardSummary("English", "اللغة الإنجليزية", "9:15 AM", true, PurpleState),
        BoardSummary("Arabic", "اللغة العربية", "10:20 AM", true, OrangeText),
        BoardSummary("Science", "العلوم", "Uploading soon...", false, GrayState)
    )

    val listAlerts = listOf(
        AlertNotification("PAYMENT", "Payment Reminder: 1,200 EGP due June 1st for Ahmad", "تذكير بالدفع: ١,٢٠٠ ج.م مستحقة في ١ يونيو لأحمد", "Today · 8:00 AM", "اليوم · ٨:٠٠ ص"),
        AlertNotification("REPORT", "May report uploaded by Mr. Mohamed Hassan (Science)", "تم رفع تقرير مايو بواسطة أ/ محمد حسن (العلوم)", "Yesterday · 3:30 PM", "أمس · ٣:٣٠ م"),
        AlertNotification("GRADE", "Ahmad scored 95% in ICT Assessment — Excellent work!", "حصل أحمد على ٩٥٪ في اختبار الحاسب الآلي — عمل ممتاز!", "2 days ago", "قبل يومين"),
        AlertNotification("HOLIDAY", "School closed Thursday — National Holiday", "المدرسة مغلقة يوم الخميس — إجازة رسمية", "3 days ago", "قبل ٣ أيام")
    )

    // Teacher mock data
    val students = listOf(
        StudentModel("Ahmad Al-Rashidi", "أحمد الرشيدي", "Year 6B", "الصف السادس ب", 88, true, "AH", RoyalBlue),
        StudentModel("Sara Ramadan", "سارة رمضان", "Year 6B", "الصف السادس ب", 94, true, "SR", CrimsonRed),
        StudentModel("Karim Mostafa", "كريم مصطفى", "Year 6A", "الصف السادس أ", 58, false, "KM", YellowState),
        StudentModel("Lina Badr", "لينا بدر", "Year 5A", "الصف الخامس أ", 91, true, "LB", PurpleState)
    )

    val gradebook = listOf(
        GradebookRow("Ahmad H.", "أحمد ح.", 90, 85, 88, 89, 88),
        GradebookRow("Sara R.", "سارة ر.", 96, 92, 95, 93, 94),
        GradebookRow("Karim M.", "كريم م.", 55, 60, 58, 59, 58),
        GradebookRow("Lina B.", "لينا ب.", 93, 89, 91, 90, 91)
    )

    // Manager mock data
    val activityLogs = listOf(
        ActivityLog("GREEN", "New enrollment: Mariam Ahmed — Year 3", "طلب انتساب جديد: مريم أحمد — الصف الثالث", "10 min ago", "منذ ١٠ دقائق"),
        ActivityLog("BLUE", "5 teacher reports submitted for May 2026", "تم تقديم ٥ تقارير معلمين لشهر مايو ٢٠٢٦", "1 hour ago", "منذ ساعة واحدة"),
        ActivityLog("RED", "Karim Mostafa — Science grade below 60% — needs attention", "كريم مصطفى — درجة العلوم أقل من ٦٠٪ — يحتاج متابعة", "2 hours ago", "منذ ساعتين"),
        ActivityLog("YELLOW", "Payment received: Al-Rashidi family — 1,200 EGP", "تم استلام الدفعة: عائلة الرشيدي — ١,٢٠٠ ج.م", "Yesterday", "أمس")
    )

    val availableGrades = listOf(
        GradeCardModel("KG1", "KG 1", "الروضة الأولى", 8000, 667, "Ms. Rana Samy", "أ/ رنا سامي", 12, false),
        GradeCardModel("KG2", "KG 2", "الروضة الثانية", 8500, 708, "Ms. Heba Nour", "أ/ هبة نور", 3, true),
        GradeCardModel("Y3", "Year 3", "الصف الثالث", 12000, 1000, "Math+Eng+Sci+Arabic", "رياضيات+إنجليزي+علوم+عربي", 8, false),
        GradeCardModel("Y6", "Year 6", "الصف السادس", 18000, 1500, "Full IGCSE", "منهج كامبردج الكامل", 2, true),
        GradeCardModel("Y9", "Year 9", "الصف التاسع", 22000, 1833, "Advanced IGCSE · 8 subjects", "كامبريدج المتقدم · ٨ مواد", 5, false)
    )
}
