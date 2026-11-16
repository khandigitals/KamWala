package com.example.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserProfileEntity
import com.example.model.CatalogData
import com.example.ui.components.KaamWalaDialogShape
import com.example.ui.components.KaamWalaInputShape
import com.example.ui.components.kaamWalaTextFieldColors
import com.example.ui.components.tactilePress
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.ui.theme.tabularMono

@Composable
fun RegistrationDialog(
    initialProfile: UserProfileEntity?,
    defaultName: String,
    defaultEmail: String,
    defaultCity: String,
    onDismiss: () -> Unit,
    onSaveProfile: (UserProfileEntity) -> Unit
) {
    val extras = LocalKaamWalaExtras.current

    var role by remember(initialProfile) {
        mutableStateOf(
            if (initialProfile?.role == "WORKER") "WORKER" else "CUSTOMER"
        )
    }
    var fullName by remember(initialProfile, defaultName) {
        mutableStateOf(initialProfile?.fullName?.ifBlank { null } ?: defaultName)
    }
    var phone by remember(initialProfile) {
        mutableStateOf(initialProfile?.phone.orEmpty())
    }
    var city by remember(initialProfile, defaultCity) {
        mutableStateOf(initialProfile?.city?.ifBlank { null } ?: defaultCity.ifBlank { "Lahore" })
    }
    val cityAreas = remember(city) { CatalogData.areasForCity(city) }
    var area by remember(initialProfile, cityAreas) {
        mutableStateOf(
            initialProfile?.area?.takeIf { it.isNotBlank() } ?: cityAreas.first()
        )
    }
    var areaExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var address by remember(initialProfile) { mutableStateOf(initialProfile?.address.orEmpty()) }

    val skillOptions = remember {
        CatalogData.SERVICES.map { it.nameEn }.distinct()
    }
    var skill by remember(initialProfile) {
        mutableStateOf(initialProfile?.skill?.takeIf { it.isNotBlank() } ?: skillOptions.first())
    }
    var skillExpanded by remember { mutableStateOf(false) }
    var experienceYears by remember(initialProfile) {
        mutableStateOf((initialProfile?.experienceYears ?: 2).toString())
    }
    var cnic by remember(initialProfile) { mutableStateOf(initialProfile?.cnic.orEmpty()) }
    var payoutAccount by remember(initialProfile) { mutableStateOf(initialProfile?.payoutAccount.orEmpty()) }
    var femaleSafe by remember(initialProfile) { mutableStateOf(initialProfile?.femaleSafe ?: false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val fieldColors = kaamWalaTextFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = KaamWalaDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 460.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (initialProfile == null) "KaamWala Registration" else "Update Your Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Customer ya Verified Karegar ke taur par register karein",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.mutedText
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close registration")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Role Switcher
                Text("Account Role Select Karein:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleOptionCard(
                        title = "Customer",
                        subtitle = "Service book karein",
                        selected = role == "CUSTOMER",
                        icon = Icons.Filled.Person,
                        onClick = { role = "CUSTOMER"; errorMsg = null },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_role_customer")
                    )
                    RoleOptionCard(
                        title = "Karegar / Pro",
                        subtitle = "Orders lein & kamayein",
                        selected = role == "WORKER",
                        icon = Icons.Filled.Engineering,
                        onClick = { role = "WORKER"; errorMsg = null },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_role_worker")
                    )
                }

                if (initialProfile != null && role == "WORKER") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (initialProfile.verificationStatus == "APPROVED") extras.emeraldTint else extras.amberBg,
                        border = BorderStroke(
                            1.dp,
                            if (initialProfile.verificationStatus == "APPROVED") extras.emeraldBorder else extras.amberBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = if (initialProfile.verificationStatus == "APPROVED") MaterialTheme.colorScheme.primary else extras.amberText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "KYC Status: ${initialProfile.verificationStatus}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (initialProfile.verificationStatus == "APPROVED") MaterialTheme.colorScheme.primary else extras.amberText
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMsg = null },
                    label = { Text("Poora Naam (Full Name)") },
                    placeholder = { Text("e.g. Hamza Khan") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text("WhatsApp / Mobile Number") },
                    placeholder = { Text("0300-1234567") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.tabularMono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone_input")
                )

                // City & Area Pickers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("City") },
                            shape = KaamWalaInputShape,
                            colors = fieldColors,
                            trailingIcon = { Text("▾", modifier = Modifier.padding(end = 8.dp)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(Modifier.matchParentSize().clickable { cityExpanded = true })
                        DropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                            CatalogData.CITIES.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = {
                                        city = c
                                        area = CatalogData.areasForCity(c).first()
                                        cityExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = area,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Area / Society") },
                            shape = KaamWalaInputShape,
                            colors = fieldColors,
                            trailingIcon = { Text("▾", modifier = Modifier.padding(end = 8.dp)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(Modifier.matchParentSize().clickable { areaExpanded = true })
                        DropdownMenu(expanded = areaExpanded, onDismissRequest = { areaExpanded = false }) {
                            cityAreas.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text(a) },
                                    onClick = { area = a; areaExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; errorMsg = null },
                    label = { Text(if (role == "WORKER") "Base Shop / Home Address" else "Ghar / Office Address") },
                    placeholder = { Text("House / Street / Block") },
                    singleLine = true,
                    shape = KaamWalaInputShape,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_address_input")
                )

                // Worker KYC Fields
                if (role == "WORKER") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = extras.amberBg,
                        border = BorderStroke(1.dp, extras.amberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Karegar KYC Zaroori Hai: Registration ke baad Admin verify kar ke aapka account approve karega (with Rs. 500 starter lead wallet).",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.amberText,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = skill,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Aapka Hunar (Primary Skill)") },
                            shape = KaamWalaInputShape,
                            colors = fieldColors,
                            trailingIcon = { Text("▾", modifier = Modifier.padding(end = 10.dp)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(Modifier.matchParentSize().clickable { skillExpanded = true })
                        DropdownMenu(expanded = skillExpanded, onDismissRequest = { skillExpanded = false }) {
                            skillOptions.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = { skill = s; skillExpanded = false }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = experienceYears,
                            onValueChange = { experienceYears = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Tajurba (Years)") },
                            singleLine = true,
                            shape = KaamWalaInputShape,
                            colors = fieldColors,
                            textStyle = LocalTextStyle.current.tabularMono(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f)
                        )
                        OutlinedTextField(
                            value = cnic,
                            onValueChange = { cnic = it; errorMsg = null },
                            label = { Text("CNIC Number") },
                            placeholder = { Text("35202-1234567-1") },
                            singleLine = true,
                            shape = KaamWalaInputShape,
                            colors = fieldColors,
                            textStyle = LocalTextStyle.current.tabularMono(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("reg_cnic_input")
                        )
                    }

                    OutlinedTextField(
                        value = payoutAccount,
                        onValueChange = { payoutAccount = it; errorMsg = null },
                        label = { Text("JazzCash / Easypaisa Payout Account") },
                        placeholder = { Text("JazzCash 0300-1234567") },
                        singleLine = true,
                        shape = KaamWalaInputShape,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_payout_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { femaleSafe = !femaleSafe }
                    ) {
                        Checkbox(checked = femaleSafe, onCheckedChange = { femaleSafe = it })
                        Text("Female / Family-Safe Verified Service Pro", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = extras.dangerText
                    )
                }
            }
        },
        confirmButton = {
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    val cleanDigits = phone.filter { it.isDigit() }
                    val cnicDigits = cnic.filter { it.isDigit() }
                    when {
                        fullName.trim().length < 2 -> errorMsg = "Apna poora naam likhein."
                        cleanDigits.length < 10 -> errorMsg = "Kam az kam 10 digits ka valid Mobile / WhatsApp number likhein."
                        address.trim().length < 4 -> errorMsg = "Apna ghar ya ilaaqe ka address likhein."
                        role == "WORKER" && cnicDigits.length < 5 -> errorMsg = "Karegar verification ke liye valid CNIC number likhein."
                        role == "WORKER" && payoutAccount.trim().length < 6 -> errorMsg = "Payment wasooli ke liye apna JazzCash / Easypaisa number likhein."
                        else -> {
                            onSaveProfile(
                                UserProfileEntity(
                                    userId = initialProfile?.userId.orEmpty(),
                                    role = role,
                                    fullName = fullName.trim(),
                                    phone = phone.trim(),
                                    email = defaultEmail,
                                    city = city,
                                    area = area,
                                    address = address.trim(),
                                    skill = if (role == "WORKER") skill else "",
                                    experienceYears = if (role == "WORKER") (experienceYears.toIntOrNull() ?: 1) else 0,
                                    cnic = if (role == "WORKER") cnic.trim() else "",
                                    payoutAccount = if (role == "WORKER") payoutAccount.trim() else "",
                                    femaleSafe = if (role == "WORKER") femaleSafe else false,
                                    verificationStatus = if (role == "WORKER") {
                                        initialProfile?.takeIf { it.role == "WORKER" }?.verificationStatus ?: "PENDING"
                                    } else {
                                        "APPROVED"
                                    }
                                )
                            )
                        }
                    }
                },
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .tactilePress(interactionSource)
                    .heightIn(min = 48.dp)
                    .testTag("registration_submit_button")
            ) {
                Text(if (role == "WORKER") "Submit Karegar KYC" else "Save Customer Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (initialProfile == null) "Later" else "Cancel")
            }
        }
    )
}

@Composable
private fun RoleOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalKaamWalaExtras.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else extras.inputFill,
        label = "role_bg"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else extras.inputBorder
        ),
        modifier = modifier.heightIn(min = 74.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) MaterialTheme.colorScheme.primary else extras.mutedText,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else extras.mutedText
            )
        }
    }
}
