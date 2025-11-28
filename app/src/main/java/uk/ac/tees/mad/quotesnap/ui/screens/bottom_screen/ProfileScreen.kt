package uk.ac.tees.mad.quotesnap.ui.screens.bottom_screen


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.ac.tees.mad.quotesnap.data.models.userData.UserPreferences
import uk.ac.tees.mad.quotesnap.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPreferencesDialog by remember { mutableStateOf(false) }

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    var showExportDialog by remember {
        mutableStateOf(false)
    }

//    var exportedFile by remember {
//        mutableStateOf<File?>(null)
//    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.userProfile?.let { profile ->
                        // Profile Header with Avatar
                        ProfileHeader(profile.fullName, profile.email)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account Information Card
                        AccountInfoCard(
                            fullName = profile.fullName,
                            email = profile.email,
                            onEditClick = { showEditDialog = true }
                        )

                        // Preferences Card
                        PreferencesCard(
                            preferences = profile.preferences,
                            onPreferencesClick = { showPreferencesDialog = true }
                        )

                        // Actions Section
                        ActionsSection(
                            onLogoutClick = { showLogoutDialog = true },
                            onDeleteAccountClick = { showDeleteDialog = true },
                            onExportPostersClick = { showExportDialog = true }
                        )
                    }
                }
            }

            // Error Snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditDialog) {
        EditNameDialog(
            currentName = uiState.userProfile?.fullName ?: "",
            onDismiss = { showEditDialog = false },
            onSave = { newName ->
                viewModel.updateProfile(newName)
                showEditDialog = false
            }
        )
    }


    // Preferences Dialog
    if (showPreferencesDialog) {
        PreferencesDialog(
            preferences = uiState.userProfile?.preferences ?: UserPreferences(),
            onDismiss = { showPreferencesDialog = false },
            onSave = { newPreferences ->
                viewModel.updatePreferences(newPreferences)
                showPreferencesDialog = false
            }
        )
    }


    // log out Dialog
    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                // delete all the posters in the local db
                viewModel.deleteAllPosters()
                onLogout()  // Call the logout function
            }
        )

    }
    // Delete Account Dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteAccount(onSuccess = onLogout)
                showDeleteDialog = false
            }
        )
    }

    // export dialog
    if (showExportDialog) {
        ExportPostersDialog(
            onDismiss = {
                showExportDialog = false
//                exportedFile = null
            },
            onDownload = {
                viewModel.exportAllPosters(
                    context = context,
                    onSuccess = { file ->
                        viewModel.downloadZipFile(
                            context = context,
                            file = file,
                            onSuccess = {
                                showExportDialog = false
                                // Show success message

                                Toast.makeText(
                                    context,
                                    "Posters downloaded successfully to Downloads folder",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { error ->
                                // Show error message
                                showExportDialog = false
                                Toast.makeText(
                                    context,
                                    "Download failed: Try later $error",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },
                    onError = { error ->
                        // Show error message
                        showExportDialog = false
                        Toast.makeText(
                            context,
                            "✗ Export failed: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onShare = {
                viewModel.exportAllPosters(
                    context = context,
                    onSuccess = { file ->
                        viewModel.shareZipFile(context, file)
                        showExportDialog = false
                        Toast.makeText(
                            context,
                            "Opening share options...",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { error ->
                        showExportDialog = false
                        // Show error message
                        Toast.makeText(
                            context,
                            "Export failed: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

            }
        )
    }
}

@Composable
fun ExportPostersDialog(
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Export All Posters",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Export all your saved posters as a ZIP file.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Download Button
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download to Device")
                }

                // Share Button
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share via...")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ProfileHeader(fullName: String, email: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar with gradient background
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fullName.firstOrNull()?.uppercase() ?: "U",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = fullName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Logout",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to logout?\n\nYou'll need to sign in again to access your account.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}


@Composable
fun AccountInfoCard(
    fullName: String,
    email: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Name
            InfoRow(
                icon = Icons.Default.Person,
                label = "Name",
                value = fullName
            )

            // Email
            InfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = email
            )
        }
    }
}

@Composable
fun PreferencesCard(
    preferences: UserPreferences,
    onPreferencesClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onPreferencesClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Edit Preferences",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Theme
            InfoRow(
                icon = Icons.Default.Star,
                label = "Theme",
                value = preferences.theme.replaceFirstChar { it.uppercase() }
            )

            // Font Size
            InfoRow(
                icon = Icons.Default.Info,
                label = "Default Font Size",
                value = "${preferences.defaultFontSize.toInt()}sp"
            )

            // Auto Sync
            InfoRow(
                icon = Icons.Default.Info,
                label = "Auto Sync",
                value = if (preferences.autoSyncEnabled) "Enabled" else "Disabled"
            )
        }
    }
}

@Composable
fun ActionsSection(
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onExportPostersClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {


        // export postor button
        // Export Posters Button (NEW)
        OutlinedButton(
            onClick = onExportPostersClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Download/Share All Posters",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        // Logout Button
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Delete Account Button
        OutlinedButton(
            onClick = onDeleteAccountClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Name",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PreferencesDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (UserPreferences) -> Unit
) {
    var theme by remember { mutableStateOf(preferences.theme) }
    var fontSize by remember { mutableFloatStateOf(preferences.defaultFontSize) }
    var autoSync by remember { mutableStateOf(preferences.autoSyncEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Preferences",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Theme Selection
                Text(
                    "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = theme == "light",
                        onClick = { theme = "light" },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = theme == "dark",
                        onClick = { theme = "dark" },
                        label = { Text("Dark") }
                    )
                }

                // Font Size Slider
                Text(
                    "Default Font Size: ${fontSize.toInt()}sp",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 16f..40f,
                    steps = 11
                )

                // Auto Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Auto Sync",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        preferences.copy(
                            theme = theme,
                            defaultFontSize = fontSize,
                            autoSyncEnabled = autoSync
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Delete Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to delete your account? This will permanently delete:\n\n" +
                        "• Your profile information\n" +
                        "• All saved posters\n" +
                        "• Your preferences\n\n" +
                        "This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
//@Composable
//fun ProfileScreen(
//    modifier: Modifier = Modifier,
//    onLogout: () -> Unit = {}
//) {
//    //  DUMMY DATA FOR NOW
//    val userName = "John Doe"
//    val userEmail = "john.doe@example.com"
//    val posterCount = 12
//    val defaultQuoteStyle = "Minimal"
//    val isDarkTheme = false
//
//    var showLogoutDialog by remember { mutableStateOf(false) }
//    var showDeleteDialog by remember { mutableStateOf(false) }
//
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//            .verticalScroll(rememberScrollState())
//    ) {
//        // Profile Header with Gradient
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(220.dp)
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            MaterialTheme.colorScheme.primaryContainer,
//                            MaterialTheme.colorScheme.surface
//                        )
//                    )
//                )
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                // Avatar with Initials
//                Box(
//                    modifier = Modifier
//                        .size(100.dp)
//                        .clip(CircleShape)
//                        .background(MaterialTheme.colorScheme.primary),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = userName.split(" ").mapNotNull { it.firstOrNull() }.take(2)
//                            .joinToString(""),
//                        style = MaterialTheme.typography.headlineLarge,
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // User Name
//                Text(
//                    text = userName,
//                    style = MaterialTheme.typography.headlineSmall,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//
//                // User Email
//                Text(
//                    text = userEmail,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Stats Chip
//                Surface(
//                    shape = RoundedCornerShape(20.dp),
//                    color = MaterialTheme.colorScheme.secondaryContainer
//                ) {
//                    Text(
//                        text = "$posterCount Posters Created",
//                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onSecondaryContainer,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Profile Sections
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Preferences Section
//            SectionCard(title = "Preferences") {
//                PreferenceItem(
//                    icon = Icons.Default.Palette,
//                    title = "Default Quote Style",
//                    subtitle = defaultQuoteStyle,
//                    onClick = { /* TODO: Open style picker */ }
//                )
////                Divider(modifier = Modifier.padding(horizontal = 16.dp))
////                PreferenceItem(
////                    icon = Icons.Default.DarkMode,
////                    title = "Dark Mode",
////                    subtitle = if (isDarkTheme) "Enabled" else "Disabled",
////                    onClick = { /* TODO: Toggle theme */ },
////                    trailing = {
////                        Switch(
////                            checked = isDarkTheme,
////                            onCheckedChange = { /* TODO: Toggle theme */ }
////                        )
////                    }
////                )
////                Divider(modifier = Modifier.padding(horizontal = 16.dp))
////                PreferenceItem(
////                    icon = Icons.Default.Notifications,
////                    title = "Notifications",
////                    subtitle = "Manage notification settings",
////                    onClick = { /* TODO: Open notifications */ }
////                )
//            }
//
//            // Export Section
//            SectionCard(title = "Export & Share") {
//                PreferenceItem(
//                    icon = Icons.Default.Download,
//                    title = "Download All Posters",
//                    subtitle = "Save all posters as ZIP file",
//                    onClick = { /* TODO: Download all */ }
//                )
//                Divider(modifier = Modifier.padding(horizontal = 16.dp))
//                PreferenceItem(
//                    icon = Icons.Default.Share,
//                    title = "Share All Posters",
//                    subtitle = "Share collection with others",
//                    onClick = { /* TODO: Share all */ }
//                )
//            }
//
//            // Account Management Section
//            SectionCard(title = "Account") {
//                PreferenceItem(
//                    icon = Icons.Default.Edit,
//                    title = "Edit Profile",
//                    subtitle = "Update your information",
//                    onClick = { /* TODO: Edit profile */ }
//                )
//                Divider(modifier = Modifier.padding(horizontal = 16.dp))
////                PreferenceItem(
////                    icon = Icons.Default.Lock,
////                    title = "Change Password",
////                    subtitle = "Update your password",
////                    onClick = { /* TODO: Change password */ }
////                )
////                Divider(modifier = Modifier.padding(horizontal = 16.dp))
//                PreferenceItem(
//                    icon = Icons.Default.Logout,
//                    title = "Logout",
//                    subtitle = "Sign out of your account",
//                    onClick = { showLogoutDialog = true },
//                    iconTint = MaterialTheme.colorScheme.error
//                )
//                Divider(modifier = Modifier.padding(horizontal = 16.dp))
//                PreferenceItem(
//                    icon = Icons.Default.DeleteForever,
//                    title = "Delete Account",
//                    subtitle = "Permanently delete your account",
//                    onClick = { showDeleteDialog = true },
//                    iconTint = MaterialTheme.colorScheme.error
//                )
//            }
//
////            // About Section
////            SectionCard(title = "About") {
////                PreferenceItem(
////                    icon = Icons.Default.Info,
////                    title = "App Version",
////                    subtitle = "QuoteSnap v1.0.0",
////                    onClick = { }
////                )
////                Divider(modifier = Modifier.padding(horizontal = 16.dp))
////                PreferenceItem(
////                    icon = Icons.Default.Description,
////                    title = "Privacy Policy",
////                    subtitle = "View privacy policy",
////                    onClick = { /* TODO: Open privacy policy */ }
////                )
////                Divider(modifier = Modifier.padding(horizontal = 16.dp))
////                PreferenceItem(
////                    icon = Icons.Default.Gavel,
////                    title = "Terms of Service",
////                    subtitle = "View terms of service",
////                    onClick = { /* TODO: Open terms */ }
////                )
////            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//        }
//    }
//
//    // Logout Confirmation Dialog
//    if (showLogoutDialog) {
//        AlertDialog(
//            onDismissRequest = { showLogoutDialog = false },
//            icon = {
//                Icon(
//                    Icons.Default.Logout,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary
//                )
//            },
//            title = {
//                Text(
//                    "Logout",
//                    fontWeight = FontWeight.Bold
//                )
//            },
//            text = {
//                Text("Are you sure you want to logout?")
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        showLogoutDialog = false
//                        onLogout()
//                    }
//                ) {
//                    Text("Logout")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showLogoutDialog = false }) {
//                    Text("Cancel")
//                }
//            },
//            shape = RoundedCornerShape(24.dp)
//        )
//    }
//
//    // Delete Account Confirmation Dialog
//    if (showDeleteDialog) {
//        AlertDialog(
//            onDismissRequest = { showDeleteDialog = false },
//            icon = {
//                Icon(
//                    Icons.Default.DeleteForever,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.error
//                )
//            },
//            title = {
//                Text(
//                    "Delete Account?",
//                    fontWeight = FontWeight.Bold
//                )
//            },
//            text = {
//                Column {
//                    Text(
//                        "This will permanently delete your account and all your saved posters.",
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        "This action cannot be undone!",
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        showDeleteDialog = false
//                        // TODO: Delete account
//                    },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.error
//                    )
//                ) {
//                    Text("Delete")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showDeleteDialog = false }) {
//                    Text("Cancel")
//                }
//            },
//            shape = RoundedCornerShape(24.dp)
//        )
//    }
//}
//
//@Composable
//private fun SectionCard(
//    title: String,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = 2.dp
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 8.dp)
//        ) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
//            )
//            content()
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun PreferenceItem(
//    icon: ImageVector,
//    title: String,
//    subtitle: String,
//    onClick: () -> Unit,
//    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
//    trailing: @Composable (() -> Unit)? = null
//) {
//    Surface(
//        onClick = onClick,
//        modifier = Modifier.fillMaxWidth(),
//        color = MaterialTheme.colorScheme.surface
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = null,
//                tint = iconTint,
//                modifier = Modifier.size(24.dp)
//            )
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = title,
//                    style = MaterialTheme.typography.bodyLarge,
//                    fontWeight = FontWeight.Medium,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                Text(
//                    text = subtitle,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            if (trailing != null) {
//                trailing()
//            } else {
//                Icon(
//                    imageVector = Icons.Default.ChevronRight,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//    }
//}
//
//@Preview()
//@Composable
//private fun ProfileScreenPreview() {
//    ProfileScreen()
//}