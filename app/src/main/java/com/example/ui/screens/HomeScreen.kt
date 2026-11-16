package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.WorkerEntity
import com.example.model.CarePlan
import com.example.model.CatalogData
import com.example.model.ServiceCategory
import com.example.model.ServiceItem
import com.example.ui.KaamWalaUiState
import com.example.ui.components.NetworkImageWithFallback
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.util.PlatformHelpers

@Composable
fun HomeScreen(
    state: KaamWalaUiState,
    onLogoTap: () -> Unit,
    onToggleLang: () -> Unit,
    onCityTap: () -> Unit,
    onSettingsTap: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (ServiceCategory) -> Unit,
    onToggleFemaleMode: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onOpenBigJob: () -> Unit,
    onOpenB2b: () -> Unit,
    onSubscribePlan: (CarePlan) -> Unit,
    onSimulateReferral: () -> Unit,
    onShowToast: (String) -> Unit,
    onOpenLiveVoice: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    val strings = CatalogData.strings(state.prefs.lang)
    val services = state.filteredServices
    val workers = state.filteredWorkers

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gridColumns = if (maxWidth >= 600.dp) 3 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. HERO HEADER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_home),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        extras.heroStart.copy(alpha = 0.92f),
                                        extras.heroEnd.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onLogoTap() }
                                    .testTag("brand_logo_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(extras.emeraldMint, MaterialTheme.colorScheme.primary)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Build,
                                        contentDescription = "KaamWala Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text(
                                        text = state.prefs.brand,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = strings.subTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HeroPillButton(
                                    text = strings.langBtn,
                                    onClick = onToggleLang,
                                    modifier = Modifier.testTag("language_toggle_button")
                                )
                                HeroPillButton(
                                    text = state.prefs.city,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    },
                                    onClick = onCityTap,
                                    modifier = Modifier.testTag("city_picker_button")
                                )
                                IconButton(
                                    onClick = onSettingsTap,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                                        .testTag("settings_button")
                                ) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "Staff Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = strings.heroTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            lineHeight = 25.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            HeroStatChip("4.9 Avg Rating")
                            HeroStatChip("12+ Hunar")
                            HeroStatChip("7-Day Warranty")
                        }

                        Spacer(Modifier.height(8.dp))

                        // Signed-in user bar + Profile + Sign Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val roleLabel = when {
                                state.isAdminUser -> " • Admin"
                                state.userProfile != null -> " • ${state.userProfile.role} (${state.userProfile.verificationStatus})"
                                else -> ""
                            }
                            Text(
                                text = (state.userProfile?.fullName?.ifBlank { null }
                                    ?: state.currentUserName.ifBlank { state.currentUserEmail.ifBlank { "Google Account Synced" } }) + roleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD1FAE5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HeroPillButton(
                                    text = if (state.userProfile == null) "Register" else "Profile",
                                    onClick = onOpenRegistration,
                                    modifier = Modifier.testTag("open_registration_pill")
                                )
                                HeroPillButton(
                                    text = "Sign Out",
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Sign Out",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    },
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("sign_out_button")
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Search Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Smart Fault Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            text = strings.searchPlaceholder,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extras.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = state.searchQuery,
                                        onValueChange = onSearchChange,
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("smart_search_input")
                                    )
                                }
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = extras.mutedText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // AI Ustad Live Voice CTA
                        Surface(
                            onClick = onOpenLiveVoice,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, extras.emeraldMint.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("live_voice_assistant_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(extras.emeraldMint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = "AI Ustad Live Voice",
                                            tint = extras.darkSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "AI Ustad Live Voice",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Urdu / English mein bol kar foran mashwara lein",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFD1FAE5)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = extras.emeraldMint
                                ) {
                                    Text(
                                        text = "Talk Live",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = extras.darkSurface,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. AI FAULT ADVISOR BOX
            item {
                AnimatedVisibility(visible = state.matchedAiRule != null) {
                    state.matchedAiRule?.let { rule ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = extras.emeraldTint,
                            border = BorderStroke(1.5.dp, extras.emeraldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Fault Diagnosis",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = rule.estimate,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tip: ${rule.tip}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.secondaryText
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { onOpenBooking(rule.serviceId) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text("Book Recommended Pro", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // 2B. ROLE REGISTRATION CTA (for unregistered users or pending workers)
            if (!state.isAdminUser && (state.userProfile == null || (state.userProfile.role == "WORKER" && state.userProfile.verificationStatus == "PENDING"))) {
                item {
                    val isPendingWorker = state.userProfile?.role == "WORKER"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPendingWorker) extras.amberBg else extras.emeraldTint,
                        border = BorderStroke(1.dp, if (isPendingWorker) extras.amberBorder else extras.emeraldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp)
                            .testTag("home_registration_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = if (isPendingWorker) "Karegar KYC Under Review" else "Registration Zaroori Hai",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPendingWorker) extras.amberText else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isPendingWorker) {
                                        "Admin verify karega to aapko live orders milna shuru ho jayein ge."
                                    } else {
                                        "Customer ya Karegar banne ke liye apna Phone, City & Role register karein."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.secondaryText
                                )
                            }
                            Button(
                                onClick = onOpenRegistration,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("home_register_cta_button")
                            ) {
                                Text(
                                    text = if (isPendingWorker) "View KYC" else "Register",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // 3. 90-DAY REMINDER BANNER
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = extras.amberBg,
                    border = BorderStroke(1.dp, extras.amberBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.remTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = extras.amberText
                            )
                            Text(
                                text = strings.remDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.amberText
                            )
                        }
                        FilledTonalButton(
                            onClick = { onOpenBooking("solar") },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = extras.amberBadgeBg,
                                contentColor = extras.amberText
                            ),
                            border = BorderStroke(1.dp, extras.amberAccent),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("reminder_book_solar_button")
                        ) {
                            Text(strings.remBtn, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 4. PROMO STRIP
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, extras.heroStart)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.promoTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "${strings.creditPrefix} ${PlatformHelpers.formatPkr(state.prefs.customerCredit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = extras.amberBadgeBg,
                            border = BorderStroke(1.dp, extras.amberAccent),
                            modifier = Modifier
                                .clickable {
                                    PlatformHelpers.copyToClipboard(context, "Promo Code", "FIRST200")
                                    onShowToast("Code FIRST200 copied!")
                                }
                                .testTag("promo_code_chip")
                        ) {
                            Text(
                                text = "FIRST200",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = extras.amberText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 5. CATEGORY CHIPS
            item {
                val chips = listOf(
                    Triple(ServiceCategory.ALL, strings.catAll, "cat_all"),
                    Triple(ServiceCategory.REPAIR, strings.catRepair, "cat_repair"),
                    Triple(ServiceCategory.BUILD, strings.catBuild, "cat_build"),
                    Triple(ServiceCategory.FEMALE, strings.catFemale, "cat_female"),
                    Triple(ServiceCategory.AUTO, strings.catAuto, "cat_auto")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { (cat, label, tag) ->
                        val active = state.selectedCategory == cat
                        FilterChip(
                            selected = active,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.testTag(tag)
                        )
                    }
                    FilterChip(
                        selected = state.femaleMode,
                        onClick = onToggleFemaleMode,
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text(strings.catSafe, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extras.roseAccent,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("family_safe_filter_chip")
                    )
                }
            }

            // 6. SERVICES GRID HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.secServices,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = strings.quickBook,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onOpenBooking("ac") }
                            .padding(vertical = 4.dp)
                            .testTag("quick_book_link")
                    )
                }
            }

            // 7. SERVICES GRID ROWS
            val serviceRows = services.chunked(gridColumns)
            if (serviceRows.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extras.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Koi matching service nahi mili", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Search clear karein ya 'All' category select karein.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.mutedText
                            )
                        }
                    }
                }
            } else {
                items(serviceRows) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        rowItems.forEach { svc ->
                            ServiceCard(
                                service = svc,
                                lang = state.prefs.lang,
                                onClick = { onOpenBooking(svc.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(gridColumns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 8. BIG JOB & B2B DUAL TILES
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        title = strings.bigJobTitle,
                        desc = strings.bigJobDesc,
                        icon = Icons.Filled.Construction,
                        gradient = listOf(extras.blueTileStart, extras.blueTileEnd),
                        onClick = onOpenBigJob,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("post_big_job_tile")
                    )
                    ActionTile(
                        title = strings.b2bTitle,
                        desc = strings.b2bDesc,
                        icon = Icons.Filled.Apartment,
                        gradient = listOf(extras.darkSurface, extras.darkSurfaceAlt),
                        onClick = onOpenB2b,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("b2b_contract_tile")
                    )
                }
            }

            // 9. MONTHLY CARE PLANS
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.secPlans, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Save 35%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(CatalogData.PLANS, key = { it.id }) { plan ->
                            PlanCard(plan = plan, onSubscribe = { onSubscribePlan(plan) })
                        }
                    }
                }
            }

            // 10. REFER & EARN CARD
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = extras.emeraldTint,
                    border = BorderStroke(1.dp, extras.emeraldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = strings.refTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = strings.refDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.secondaryText
                                )
                            }
                            Button(
                                onClick = {
                                    PlatformHelpers.copyToClipboard(context, "Referral Code", "REF200")
                                    PlatformHelpers.shareText(
                                        context,
                                        "KaamWala par ghar ke har kaam ke liye verified ustad book karein! Code REF200 use karke Rs. 200 discount payein."
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .testTag("share_referral_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (!state.prefs.liveCustomerMode) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onSimulateReferral,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .testTag("demo_referral_bonus_button")
                            ) {
                                Text("+ Test Friend Joined (+Rs. 200 Credit)", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 11. TOP HUNARMAND PROS
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.secPros, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${workers.size} Verified",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(workers, key = { it.id }) { worker ->
                            ProCard(worker = worker)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        modifier = modifier.heightIn(min = 34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HeroStatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFECFDF5),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ServiceCard(
    service: ServiceItem,
    lang: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalKaamWalaExtras.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extras.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag("service_card_${service.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                NetworkImageWithFallback(
                    url = service.imageUrl,
                    contentDescription = service.localizedName(lang),
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xAA0F172A))
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = extras.darkSurface.copy(alpha = 0.80f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                ) {
                    Text(
                        text = service.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = extras.emeraldTint,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = service.eta,
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.heroStart,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = service.localizedName(lang),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = service.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PlatformHelpers.formatPkr(service.fee),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Book",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun PlanCard(plan: CarePlan, onSubscribe: () -> Unit) {
    val extras = LocalKaamWalaExtras.current
    Box(
        modifier = Modifier
            .width(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(extras.darkSurface, extras.darkSurfaceAlt)))
            .padding(14.dp)
    ) {
        Column {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            ) {
                Text(
                    text = plan.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.emeraldMint,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(plan.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(plan.desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${PlatformHelpers.formatPkr(plan.price)}/mo",
                    style = MaterialTheme.typography.labelLarge,
                    color = extras.emeraldMint
                )
                FilledTonalButton(
                    onClick = onSubscribe,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .testTag("subscribe_plan_${plan.id}")
                ) {
                    Text("Activate", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ProCard(worker: WorkerEntity) {
    val extras = LocalKaamWalaExtras.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        modifier = Modifier.width(152.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NetworkImageWithFallback(
                url = worker.avatarUrl,
                contentDescription = worker.name,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = worker.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = worker.skill,
                style = MaterialTheme.typography.bodySmall,
                color = extras.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = extras.amberAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("${worker.rating} • ${worker.jobsCount}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = if (worker.femaleSafe) extras.roseAccent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = if (worker.femaleSafe) "Family-Safe" else "CNIC Verified",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (worker.femaleSafe) extras.roseAccent else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
