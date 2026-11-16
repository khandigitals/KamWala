package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.components.FlagshipCard
import com.example.ui.components.FlagshipPrimaryButton
import com.example.ui.components.KaamWalaCardShape
import com.example.ui.components.NetworkImageWithFallback
import com.example.ui.components.tactilePress
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono
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
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // 1. HERO HEADER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
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
                                        extras.heroStart.copy(alpha = 0.94f),
                                        extras.heroEnd.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(extras.emeraldMint, MaterialTheme.colorScheme.primary)
                                            )
                                        )
                                        .border(1.dp, extras.frostedPillBorder, RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Build,
                                        contentDescription = "KaamWala Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
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
                                            tint = extras.emeraldMint,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    },
                                    onClick = onCityTap,
                                    modifier = Modifier.testTag("city_picker_button")
                                )
                                IconButton(
                                    onClick = onSettingsTap,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(extras.frostedPill)
                                        .border(1.dp, extras.frostedPillBorder, CircleShape)
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

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.heroTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            lineHeight = 27.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            HeroStatChip("4.9 Avg Rating")
                            HeroStatChip("12+ Hunar")
                            HeroStatChip("7-Day Warranty")
                        }

                        Spacer(Modifier.height(10.dp))

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
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
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

                        Spacer(Modifier.height(12.dp))

                        // Search Box (floating flagship card input)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, extras.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = extras.ambientShadow,
                                    spotColor = extras.ambientShadow
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Smart Fault Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(10.dp))
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
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
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
                                        modifier = Modifier.size(28.dp)
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
                        val voiceInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = onOpenLiveVoice,
                            interactionSource = voiceInteraction,
                            shape = RoundedCornerShape(16.dp),
                            color = extras.frostedPill,
                            border = BorderStroke(1.dp, extras.emeraldMint.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .tactilePress(voiceInteraction)
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("live_voice_assistant_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(extras.emeraldMint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = "AI Ustad Live Voice",
                                            tint = extras.darkSurface,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
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
                                    shape = RoundedCornerShape(999.dp),
                                    color = extras.emeraldMint
                                ) {
                                    Text(
                                        text = "Talk Live",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = extras.darkSurface,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
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
                        FlagshipCard(
                            containerColor = extras.emeraldTint,
                            borderColor = extras.emeraldBorder,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .padding(top = 14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Fault Diagnosis",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, extras.emeraldBorder)
                                    ) {
                                        Text(
                                            text = rule.estimate,
                                            style = MaterialTheme.typography.labelSmall.tabularMono(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Tip: ${rule.tip}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.secondaryText
                                )
                                Spacer(Modifier.height(10.dp))
                                FlagshipPrimaryButton(
                                    onClick = { onOpenBooking(rule.serviceId) },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Book Recommended Pro", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // 2B. ROLE REGISTRATION CTA
            if (!state.isAdminUser && (state.userProfile == null || (state.userProfile.role == "WORKER" && state.userProfile.verificationStatus == "PENDING"))) {
                item {
                    val isPendingWorker = state.userProfile?.role == "WORKER"
                    FlagshipCard(
                        containerColor = if (isPendingWorker) extras.amberBg else extras.emeraldTint,
                        borderColor = if (isPendingWorker) extras.amberBorder else extras.emeraldBorder,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(top = 12.dp)
                            .testTag("home_registration_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = if (isPendingWorker) "Karegar KYC Under Review" else "Registration Zaroori Hai",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPendingWorker) extras.amberText else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
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
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
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
                FlagshipCard(
                    containerColor = extras.amberBg,
                    borderColor = extras.amberBorder,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = strings.remTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = extras.amberText
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = strings.remDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.amberText
                            )
                        }
                        FilledTonalButton(
                            onClick = { onOpenBooking("solar") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = extras.amberBadgeBg,
                                contentColor = extras.amberText
                            ),
                            border = BorderStroke(1.dp, extras.amberAccent),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
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
                        .padding(horizontal = 18.dp)
                        .padding(top = 12.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = KaamWalaCardShape,
                            ambientColor = extras.ambientShadow,
                            spotColor = extras.ambientShadow
                        )
                        .clip(KaamWalaCardShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, extras.heroStart)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
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
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${strings.creditPrefix} ${PlatformHelpers.formatPkr(state.prefs.customerCredit)}",
                                style = MaterialTheme.typography.bodySmall.tabularMono(),
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
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
                                style = MaterialTheme.typography.labelMedium.tabularMono(),
                                fontWeight = FontWeight.Bold,
                                color = extras.amberText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { (cat, label, tag) ->
                        val active = state.selectedCategory == cat
                        val chipBg by animateColorAsState(
                            targetValue = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                            label = "cat_bg_$tag"
                        )
                        val chipFg by animateColorAsState(
                            targetValue = if (active) MaterialTheme.colorScheme.onSecondary else extras.secondaryText,
                            label = "cat_fg_$tag"
                        )
                        Surface(
                            onClick = { onSelectCategory(cat) },
                            shape = RoundedCornerShape(12.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.secondary else extras.cardBorder),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag(tag)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                    color = chipFg
                                )
                            }
                        }
                    }
                    val safeBg by animateColorAsState(
                        targetValue = if (state.femaleMode) extras.roseAccent else MaterialTheme.colorScheme.surface,
                        label = "safe_bg"
                    )
                    val safeFg by animateColorAsState(
                        targetValue = if (state.femaleMode) Color.White else extras.roseAccent,
                        label = "safe_fg"
                    )
                    Surface(
                        onClick = onToggleFemaleMode,
                        shape = RoundedCornerShape(12.dp),
                        color = safeBg,
                        border = BorderStroke(1.dp, if (state.femaleMode) extras.roseAccent else extras.roseBorder),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("family_safe_filter_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = safeFg,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = strings.catSafe,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = safeFg
                            )
                        }
                    }
                }
            }

            // 6. SERVICES GRID HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
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
                        fontWeight = FontWeight.Bold,
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
                    FlagshipCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
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
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.secPlans, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = extras.emeraldTint,
                            border = BorderStroke(1.dp, extras.emeraldBorder)
                        ) {
                            Text(
                                "Save 35%",
                                style = MaterialTheme.typography.labelSmall.tabularMono(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(CatalogData.PLANS, key = { it.id }) { plan ->
                            PlanCard(plan = plan, onSubscribe = { onSubscribePlan(plan) })
                        }
                    }
                }
            }

            // 10. REFER & EARN CARD
            item {
                FlagshipCard(
                    containerColor = MaterialTheme.colorScheme.surface,
                    borderColor = extras.emeraldBorder,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = strings.refTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = strings.refDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.secondaryText
                                )
                            }
                            FlagshipPrimaryButton(
                                onClick = {
                                    PlatformHelpers.copyToClipboard(context, "Referral Code", "REF200")
                                    PlatformHelpers.shareText(
                                        context,
                                        "KaamWala par ghar ke har kaam ke liye verified ustad book karein! Code REF200 use karke Rs. 200 discount payein."
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("share_referral_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Share", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (!state.prefs.liveCustomerMode) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onSimulateReferral,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
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
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.secPros, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${workers.size} Verified",
                            style = MaterialTheme.typography.labelMedium.tabularMono(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
    val extras = LocalKaamWalaExtras.current
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(999.dp),
        color = extras.frostedPill,
        border = BorderStroke(1.dp, extras.frostedPillBorder),
        modifier = modifier
            .tactilePress(interactionSource)
            .heightIn(min = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
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
    val extras = LocalKaamWalaExtras.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = extras.frostedPill,
        border = BorderStroke(1.dp, extras.frostedPillBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.tabularMono(),
            color = Color(0xFFECFDF5),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = KaamWalaCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        modifier = modifier
            .tactilePress(interactionSource)
            .shadow(
                elevation = 8.dp,
                shape = KaamWalaCardShape,
                ambientColor = extras.ambientShadow,
                spotColor = extras.ambientShadow
            )
            .testTag("service_card_${service.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
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
                    shape = RoundedCornerShape(8.dp),
                    color = extras.darkSurface.copy(alpha = 0.82f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = service.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = extras.emeraldTint,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                ) {
                    Text(
                        text = service.eta,
                        style = MaterialTheme.typography.labelSmall.tabularMono(),
                        color = extras.heroStart,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = service.localizedName(lang),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = service.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PlatformHelpers.formatPkr(service.fee),
                        style = MaterialTheme.typography.labelLarge.tabularMono(),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Book",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
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
    val extras = LocalKaamWalaExtras.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .tactilePress(interactionSource)
            .heightIn(min = 104.dp)
            .shadow(
                elevation = 8.dp,
                shape = KaamWalaCardShape,
                ambientColor = extras.ambientShadow,
                spotColor = extras.ambientShadow
            )
            .clip(KaamWalaCardShape)
            .background(Brush.linearGradient(gradient))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun PlanCard(plan: CarePlan, onSubscribe: () -> Unit) {
    val extras = LocalKaamWalaExtras.current
    Box(
        modifier = Modifier
            .width(220.dp)
            .shadow(
                elevation = 8.dp,
                shape = KaamWalaCardShape,
                ambientColor = extras.ambientShadow,
                spotColor = extras.ambientShadow
            )
            .clip(KaamWalaCardShape)
            .background(Brush.linearGradient(listOf(extras.darkSurface, extras.darkSurfaceAlt)))
            .border(1.dp, Color.White.copy(alpha = 0.08f), KaamWalaCardShape)
            .padding(16.dp)
    ) {
        Column {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            ) {
                Text(
                    text = plan.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.emeraldMint,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(plan.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(plan.desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${PlatformHelpers.formatPkr(plan.price)}/mo",
                    style = MaterialTheme.typography.labelLarge.tabularMono(),
                    fontWeight = FontWeight.ExtraBold,
                    color = extras.emeraldMint
                )
                FilledTonalButton(
                    onClick = onSubscribe,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
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
    FlagshipCard(
        shadowElevation = 6.dp,
        modifier = Modifier.width(158.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NetworkImageWithFallback(
                url = worker.avatarUrl,
                contentDescription = worker.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.width(3.dp))
                Text(
                    "${worker.rating} • ${worker.jobsCount}",
                    style = MaterialTheme.typography.labelSmall.tabularMono(),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(5.dp))
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
                    fontWeight = FontWeight.SemiBold,
                    color = if (worker.femaleSafe) extras.roseAccent else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
