package com.example.model

enum class ServiceCategory(val key: String) {
    ALL("all"),
    REPAIR("repair"),
    BUILD("build"),
    FEMALE("female"),
    AUTO("auto")
}

data class RateCardItem(
    val label: String,
    val price: String
)

data class ServiceItem(
    val id: String,
    val category: ServiceCategory,
    val nameEn: String,
    val nameUr: String,
    val desc: String,
    val fee: Int,
    val eta: String,
    val badge: String,
    val femaleSafe: Boolean,
    val imageUrl: String,
    val rates: List<RateCardItem>
) {
    fun localizedName(lang: String): String = if (lang == "ur") nameUr else nameEn
}

data class CarePlan(
    val id: String,
    val tag: String,
    val title: String,
    val desc: String,
    val price: Int
)

data class HardwarePart(
    val name: String,
    val price: Int
)

data class ToolCatalogItem(
    val name: String,
    val price: Int,
    val dailyQist: Int
)

data class AiFaultRule(
    val keys: List<String>,
    val serviceId: String,
    val title: String,
    val estimate: String,
    val tip: String
)

data class AppStrings(
    val subTag: String,
    val heroTitle: String,
    val searchPlaceholder: String,
    val remTitle: String,
    val remDesc: String,
    val remBtn: String,
    val promoTitle: String,
    val creditPrefix: String,
    val catAll: String,
    val catRepair: String,
    val catBuild: String,
    val catFemale: String,
    val catAuto: String,
    val catSafe: String,
    val secServices: String,
    val quickBook: String,
    val bigJobTitle: String,
    val bigJobDesc: String,
    val b2bTitle: String,
    val b2bDesc: String,
    val secPlans: String,
    val refTitle: String,
    val refDesc: String,
    val secPros: String,
    val ordersHdr: String,
    val ordersSub: String,
    val workerHdr: String,
    val admHdr: String,
    val navHome: String,
    val navOrders: String,
    val navWorker: String,
    val navAdmin: String,
    val langBtn: String,
    val ordersNewOrder: String,
    val ordersCarePlans: String,
    val ordersBigJobs: String,
    val ordersServiceBookings: String,
    val ordersEmptyTitle: String,
    val ordersEmptyDesc: String,
    val ordersEmptyBtn: String,
    val bookBtnPrefix: String,
    val timeSlotLabel: String,
    val societyAreaLabel: String,
    val confirmBookingBtn: String,
    val workerFilterAll: String,
    val workerFilterPending: String,
    val workerFilterActive: String,
    val workerFilterDone: String
)

object CatalogData {
    val STRINGS_EN = AppStrings(
        subTag = "12+ Verified Home Skills",
        heroTitle = "Har Hunar aur Kaam ke liye Verified Ustad — 45 Min mein!",
        searchPlaceholder = "Search: 'solar', 'AC E1', 'salon', 'door lock', 'bike'...",
        remTitle = "90-Day AC & Solar Check Due!",
        remDesc = "15% OFF par seasonal service book karein",
        remBtn = "Book Solar",
        promoTitle = "Pehle Order par Rs. 200 OFF!",
        creditPrefix = "Referral Credit:",
        catAll = "All (12)",
        catRepair = "Repairs",
        catBuild = "Solar & Wood",
        catFemale = "Salon & Care",
        catAuto = "Auto & Errands",
        catSafe = "Family-Safe Only",
        secServices = "Services & Hunar",
        quickBook = "Quick Book ->",
        bigJobTitle = "Post Big Job (Bids)",
        bigJobDesc = "Solar, Paint, Wood — 3 quotes payein",
        b2bTitle = "B2B Office / Shop",
        b2bDesc = "Clinics, Cafes & Societies contracts",
        secPlans = "Monthly Care Plans",
        refTitle = "Refer & Earn Rs. 200",
        refDesc = "Doston ko invite karein, dono ko Rs. 200 credit milega!",
        secPros = "Top Hunarmand Pros",
        ordersHdr = "Meri Bookings",
        ordersSub = "Live Radar, In-App Chat, Custom Bids & Warranty Cards",
        workerHdr = "Partner Pro",
        admHdr = "Admin Command",
        navHome = "Home",
        navOrders = "Orders",
        navWorker = "Worker",
        navAdmin = "Admin",
        langBtn = "اردو",
        ordersNewOrder = "New Order",
        ordersCarePlans = "Active Monthly Care Plans",
        ordersBigJobs = "Big Jobs & Custom Bids",
        ordersServiceBookings = "Service Bookings",
        ordersEmptyTitle = "Abhi koi booking nahi hai",
        ordersEmptyDesc = "Home tab se AC, Solar, Plumbing ya Salon service 45 min mein book karein.",
        ordersEmptyBtn = "Book First Service",
        bookBtnPrefix = "Book",
        timeSlotLabel = "Time Slot",
        societyAreaLabel = "Society / Area",
        confirmBookingBtn = "Confirm Booking",
        workerFilterAll = "All",
        workerFilterPending = "Pending",
        workerFilterActive = "Active",
        workerFilterDone = "Done"
    )

    val STRINGS_UR = AppStrings(
        subTag = "12+ تصدیق شدہ گھریلو ہنر",
        heroTitle = "ہر ہنر اور کام کے لیے تصدیق شدہ کاریگر — 45 منٹ میں!",
        searchPlaceholder = "تلاش کریں: سولر، اے سی، سیلون، پلمبر، بائیک...",
        remTitle = "اے سی اور سولر سروس کا وقت ہو گیا!",
        remDesc = "15% ڈسکاؤنٹ پر سیزنل سروس بُک کریں",
        remBtn = "بُک کریں",
        promoTitle = "پہلے آرڈر پر 200 روپے کی بچت!",
        creditPrefix = "ریفرل کریڈٹ:",
        catAll = "تمام (12)",
        catRepair = "مرمت",
        catBuild = "سولر و لکڑی",
        catFemale = "خواتین کیئر",
        catAuto = "آٹو و ڈیلیوری",
        catSafe = "فیملی سیف",
        secServices = "سروسز اور ہنر",
        quickBook = "فوری بکنگ ←",
        bigJobTitle = "بڑا کام پوسٹ کریں",
        bigJobDesc = "پینٹ، سولر، وڈ ورک — ریٹس منگوائیں",
        b2bTitle = "آفس / دکان معاہدہ",
        b2bDesc = "کلینک، کیفے اور سوسائٹی پیکجز",
        secPlans = "ماہانہ کیئر پیکجز",
        refTitle = "دوستوں کو مدعو کریں — 200 روپے پائیں",
        refDesc = "کوڈ شیئر کریں اور دونوں کو 200 روپے کریڈٹ ملے گا!",
        secPros = "بہترین ہنرمند کاریگر",
        ordersHdr = "میری بکنگز",
        ordersSub = "لائیو ٹریکنگ، چیٹ، بڈنگ اور ڈیجیٹل وارنٹی کارڈ",
        workerHdr = "پارٹنر ایپ",
        admHdr = "ایڈمن پینل",
        navHome = "ہوم",
        navOrders = "آرڈرز",
        navWorker = "ورکر ایپ",
        navAdmin = "ایڈمن",
        langBtn = "EN",
        ordersNewOrder = "نیا آرڈر",
        ordersCarePlans = "فعال ماہانہ پلانز",
        ordersBigJobs = "بڑے کام اور بولیاں",
        ordersServiceBookings = "سروس بکنگز",
        ordersEmptyTitle = "ابھی کوئی بکنگ نہیں ہے",
        ordersEmptyDesc = "ہوم ٹیب سے اے سی، سولر، پلمبنگ یا سیلون سروس 45 منٹ میں بُک کریں۔",
        ordersEmptyBtn = "پہلی سروس بُک کریں",
        bookBtnPrefix = "بُک کریں:",
        timeSlotLabel = "وقت کا انتخاب",
        societyAreaLabel = "سوسائٹی / علاقہ",
        confirmBookingBtn = "بکنگ کنفرم کریں",
        workerFilterAll = "تمام",
        workerFilterPending = "نئی",
        workerFilterActive = "جاری",
        workerFilterDone = "مکمل"
    )

    fun strings(lang: String): AppStrings = if (lang == "ur") STRINGS_UR else STRINGS_EN

    val SERVICES = listOf(
        ServiceItem(
            id = "ac",
            category = ServiceCategory.REPAIR,
            nameEn = "AC & Cooling",
            nameUr = "اے سی اور کولنگ",
            desc = "Gas refill, master service, PCB",
            fee = 500,
            eta = "35 min",
            badge = "Popular",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Master Jet Service", "Rs. 2,000"),
                RateCardItem("Gas Refill", "Rs. 3,800"),
                RateCardItem("Capacitor Change", "Rs. 900")
            )
        ),
        ServiceItem(
            id = "elec",
            category = ServiceCategory.REPAIR,
            nameEn = "Electrician & UPS",
            nameUr = "الیکٹریشن اور یو پی ایس",
            desc = "UPS wiring, breaker, fans",
            fee = 400,
            eta = "30 min",
            badge = "Fastest",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1621905252507-b35492cc74b4?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("UPS / Inverter Setup", "Rs. 1,200"),
                RateCardItem("Ceiling Fan Install", "Rs. 600"),
                RateCardItem("Breaker Fix", "Rs. 500")
            )
        ),
        ServiceItem(
            id = "plumb",
            category = ServiceCategory.REPAIR,
            nameEn = "Plumber & Geyser",
            nameUr = "پلمبر اور گیزر",
            desc = "Leakage, motor, geyser, taps",
            fee = 400,
            eta = "40 min",
            badge = "Guaranteed",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1580401410158-1f0b0a406762?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Tap Replace", "Rs. 500"),
                RateCardItem("Water Motor Repair", "Rs. 1,000"),
                RateCardItem("Geyser Service", "Rs. 1,200")
            )
        ),
        ServiceItem(
            id = "solar",
            category = ServiceCategory.BUILD,
            nameEn = "Solar & Inverter",
            nameUr = "سولر اور انورٹر",
            desc = "Panel wash, hybrid setup, earthing",
            fee = 600,
            eta = "45 min",
            badge = "High Demand",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("10-Panel Pressure Wash", "Rs. 1,500"),
                RateCardItem("Inverter Fault Check", "Rs. 1,000"),
                RateCardItem("Net-Metering Check", "Rs. 1,200")
            )
        ),
        ServiceItem(
            id = "carp",
            category = ServiceCategory.BUILD,
            nameEn = "Carpenter & Locks",
            nameUr = "کارپینٹر اور تالے",
            desc = "Door locks, cabinets, bed repair",
            fee = 450,
            eta = "45 min",
            badge = "Skilled",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Door Lock Replace", "Rs. 600"),
                RateCardItem("Cabinet Hinge Fix", "Rs. 500"),
                RateCardItem("Bed Frame Fix", "Rs. 1,200")
            )
        ),
        ServiceItem(
            id = "paint",
            category = ServiceCategory.BUILD,
            nameEn = "Painter & Seepage",
            nameUr = "پینٹر اور سیم کا علاج",
            desc = "Room paint, seepage & polish",
            fee = 500,
            eta = "60 min",
            badge = "Pro Finish",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1562259949-e8e7689d7828?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("1 Wall Touch-up", "Rs. 1,800"),
                RateCardItem("Seepage Waterproofing", "Rs. 3,500"),
                RateCardItem("Door Deco Polish", "Rs. 2,200")
            )
        ),
        ServiceItem(
            id = "salon",
            category = ServiceCategory.FEMALE,
            nameEn = "Female Salon at Home",
            nameUr = "خواتین سیلون گھر پر",
            desc = "Facial, waxing, hair & makeup",
            fee = 500,
            eta = "45 min",
            badge = "Female Pro",
            femaleSafe = true,
            imageUrl = "https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Glow Facial + Cleansing", "Rs. 2,200"),
                RateCardItem("Full Arms/Legs Wax", "Rs. 1,800"),
                RateCardItem("Party Hair & Makeup", "Rs. 4,500")
            )
        ),
        ServiceItem(
            id = "clean",
            category = ServiceCategory.FEMALE,
            nameEn = "Female Home Care",
            nameUr = "گھریلو صفائی (فیمیل)",
            desc = "Female cleaner, sofa & kitchen",
            fee = 700,
            eta = "50 min",
            badge = "Family-Safe",
            femaleSafe = true,
            imageUrl = "https://images.unsplash.com/photo-1585421514284-efb74c2b69ba?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Female Deep Clean (3 hrs)", "Rs. 1,500"),
                RateCardItem("5-Seater Sofa Clean", "Rs. 1,800"),
                RateCardItem("Kitchen Degreasing", "Rs. 2,000")
            )
        ),
        ServiceItem(
            id = "tutor",
            category = ServiceCategory.FEMALE,
            nameEn = "Home Tutor & Quran",
            nameUr = "ہوم ٹیوٹر اور قرآن",
            desc = "Science, English & Nazra Quran",
            fee = 400,
            eta = "Demo",
            badge = "Verified",
            femaleSafe = true,
            imageUrl = "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Trial Assessment Class", "Rs. 400"),
                RateCardItem("Monthly Quran (Female/Male)", "Rs. 5,000"),
                RateCardItem("O-Level/Matric Monthly", "Rs. 8,000")
            )
        ),
        ServiceItem(
            id = "car",
            category = ServiceCategory.AUTO,
            nameEn = "Doorstep Car Wash",
            nameUr = "کار واش گھر پر",
            desc = "Foam wash, vacuum & polish",
            fee = 600,
            eta = "45 min",
            badge = "Trending",
            femaleSafe = true,
            imageUrl = "https://images.unsplash.com/photo-1601362840469-51e4d8d58785?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Sedan Exterior + Vacuum", "Rs. 600"),
                RateCardItem("SUV Detailing", "Rs. 1,200"),
                RateCardItem("Wax Polish", "Rs. 300")
            )
        ),
        ServiceItem(
            id = "mech",
            category = ServiceCategory.AUTO,
            nameEn = "Mobile Mechanic",
            nameUr = "موبائل مکینک (بائیک/کار)",
            desc = "Bike tuning, battery jump, puncture",
            fee = 400,
            eta = "25 min",
            badge = "Roadside",
            femaleSafe = false,
            imageUrl = "https://images.unsplash.com/photo-1615906655593-ad0386982a0f?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("70cc/125cc Bike Tuning", "Rs. 600"),
                RateCardItem("Car Battery Jump-Start", "Rs. 700"),
                RateCardItem("Doorstep Puncture Fix", "Rs. 400")
            )
        ),
        ServiceItem(
            id = "errand",
            category = ServiceCategory.AUTO,
            nameEn = "Pick & Drop / Errand",
            nameUr = "پک اینڈ ڈراپ / سامان",
            desc = "Bazar se samaan & documents",
            fee = 300,
            eta = "25 min",
            badge = "Instant",
            femaleSafe = true,
            imageUrl = "https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?auto=format&fit=crop&w=800&q=80",
            rates = listOf(
                RateCardItem("Within 3 km Errand", "Rs. 300"),
                RateCardItem("Grocery / Pharmacy Run", "Rs. 350"),
                RateCardItem("Document Drop", "Rs. 400")
            )
        )
    )

    val PLANS = listOf(
        CarePlan("p1", "BEST SELLER", "Monthly Car Care", "4 Foam Washes / Month", 1800),
        CarePlan("p2", "VIP HOME", "Home Care Club", "Free Visits + 10% Off", 499),
        CarePlan("p3", "SOLAR & AC", "Summer Care Duo", "2 AC + 2 Solar Washes", 3600)
    )

    val HARDWARE_PARTS = listOf(
        HardwarePart("AC Capacitor (35uF)", 750),
        HardwarePart("Brass Tap / Mixer", 650),
        HardwarePart("32A MCB Breaker", 850),
        HardwarePart("Smart Door Lock Cylinder", 1400),
        HardwarePart("AC Gas Top-Up (1kg)", 2400)
    )

    val TOOL_ITEMS = listOf(
        ToolCatalogItem("Bosch Hammer Drill", 4500, 100),
        ToolCatalogItem("High-Pressure Solar/AC Washer", 8000, 150),
        ToolCatalogItem("Digital Clamp Meter", 2500, 80)
    )

    val AI_RULES = listOf(
        AiFaultRule(listOf("e1", "e4", "f0", "ac", "cool", "gas", "ice", "split"), "ac", "AC Sensor / Gas / Capacitor Issue", "Rs. 900 – Rs. 3,800", "AC band kar dein aur stabilizer check karein."),
        AiFaultRule(listOf("solar", "panel", "inverter", "battery", "watt"), "solar", "Solar Panel Cleaning / Inverter Fault", "Rs. 1,000 – Rs. 2,500", "Dust se 25% bijli kam banti hai — pressure wash karwayein."),
        AiFaultRule(listOf("door", "lock", "wood", "cabinet", "hinge", "carpenter", "darwaza"), "carp", "Carpenter / Door Lock Repair", "Rs. 500 – Rs. 1,500", "Master carpenter locks & hinges sath le kar aayega."),
        AiFaultRule(listOf("paint", "seepage", "damp", "wall", "deewar", "rang"), "paint", "Wall Paint / Seepage Waterproofing", "Rs. 1,800 – Rs. 4,500", "Dampness check karke chemical coat lagaya jayega."),
        AiFaultRule(listOf("salon", "facial", "wax", "hair", "makeup", "beauty"), "salon", "Female Salon at Home (Verified)", "Rs. 1,800 – Rs. 4,500", "100% Female Beautician sealed kit ke saath aayegi."),
        AiFaultRule(listOf("bike", "puncture", "start", "battery", "mechanic", "tyre"), "mech", "Roadside / Doorstep Mechanic", "Rs. 400 – Rs. 1,000", "25 min mein mechanic jump-start & tools le kar pahunch jayega."),
        AiFaultRule(listOf("tutor", "quran", "study", "class", "math", "english"), "tutor", "Verified Home Tutor / Quran Teacher", "Rs. 400 Trial / Rs. 5k+ mo", "Male & Female verified teachers available hain."),
        AiFaultRule(listOf("trip", "breaker", "ups", "spark", "wire", "fan", "bijli"), "elec", "Short Circuit / Breaker / UPS Load", "Rs. 500 – Rs. 1,500", "Main breaker off rakhein taake appliance safe rahe."),
        AiFaultRule(listOf("leak", "tap", "pipe", "motor", "geyser", "pani", "sink"), "plumb", "Pipe Leakage / Motor / Geyser", "Rs. 500 – Rs. 1,800", "Main water valve band kar dein."),
        AiFaultRule(listOf("sofa", "clean", "safai", "maid", "kitchen"), "clean", "Deep Cleaning / Female Care", "Rs. 1,500 – Rs. 2,500", "Female-Safe verified pro available hai."),
        AiFaultRule(listOf("car", "wash", "gaari", "polish"), "car", "Doorstep Foam Wash & Vacuum", "Rs. 600 – Rs. 1,200", "45 min mein doorstep wash team pahunch jayegi.")
    )

    val AREAS_BY_CITY: Map<String, List<String>> = mapOf(
        "Lahore" to listOf(
            "Bahria Town",
            "DHA Phase 1-8",
            "Gulberg / Model Town",
            "Johar Town / Wapda Town",
            "Cantt / City Center"
        ),
        "Karachi" to listOf(
            "DHA Karachi / Clifton",
            "Gulshan-e-Iqbal",
            "Bahria Town Karachi",
            "PECHS / Tariq Road",
            "North Nazimabad"
        ),
        "Islamabad" to listOf(
            "F-6 / F-7 / F-8",
            "G-9 / G-10 / G-11",
            "DHA / Bahria Enclave",
            "E-11 / D-12",
            "Blue Area / Diplomatic"
        ),
        "Rawalpindi" to listOf(
            "Bahria Town Rwp",
            "Chaklala Scheme 3",
            "Saddar / Cantt",
            "Satellite Town",
            "Peshawar Road"
        ),
        "Faisalabad" to listOf(
            "Peoples Colony",
            "Madina Town",
            "Canal Road / Eden Valley",
            "D Ground",
            "Jaranwala Road"
        ),
        "Multan" to listOf(
            "DHA Multan",
            "Model Town Multan",
            "Cantt Multan",
            "Bosan Road",
            "Gulgasht Colony"
        )
    )

    fun areasForCity(city: String): List<String> =
        AREAS_BY_CITY.entries.firstOrNull { it.key.equals(city.trim(), ignoreCase = true) }?.value
            ?: AREAS_BY_CITY.getValue("Lahore")

    val AREA_OPTIONS = AREAS_BY_CITY.getValue("Lahore")

    val CITIES = listOf("Lahore", "Karachi", "Islamabad", "Rawalpindi", "Faisalabad", "Multan")

    val QUICK_CHAT_REPLIES = listOf(
        "Main gate par hoon",
        "10 min mein pahunch raha hoon",
        "Location confirm karein",
        "OTP share kar dein"
    )
}
