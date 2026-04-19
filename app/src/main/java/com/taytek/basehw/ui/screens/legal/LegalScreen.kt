package com.taytek.basehw.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taytek.basehw.R

enum class LegalType { PRIVACY_POLICY, TERMS_OF_USE, COMMUNITY_RULES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    type: LegalType,
    onNavigateBack: () -> Unit
) {
    val title = when (type) {
        LegalType.PRIVACY_POLICY -> stringResource(R.string.privacy_policy)
        LegalType.TERMS_OF_USE   -> stringResource(R.string.terms_of_use)
        LegalType.COMMUNITY_RULES -> stringResource(R.string.community_rules_title)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (type) {
                LegalType.PRIVACY_POLICY -> PrivacyPolicyContent()
                LegalType.TERMS_OF_USE   -> TermsOfUseContent()
                LegalType.COMMUNITY_RULES -> CommunityRulesContent()
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    LegalTitle(stringResource(R.string.privacy_policy))
    LegalSubtitle(stringResource(R.string.privacy_last_updated))
    LegalParagraph(stringResource(R.string.privacy_intro))
    
    LegalSection(stringResource(R.string.privacy_section_1_title))
    LegalParagraph(stringResource(R.string.privacy_section_1_text))

    LegalSection(stringResource(R.string.privacy_section_2_title))
    LegalParagraph(stringResource(R.string.privacy_section_2_text))

    LegalSection(stringResource(R.string.privacy_section_3_title))
    LegalParagraph(stringResource(R.string.privacy_section_3_text))

    LegalSection(stringResource(R.string.privacy_section_4_title))
    LegalParagraph(stringResource(R.string.privacy_section_4_text))

    LegalSection(stringResource(R.string.privacy_section_5_title))
    LegalParagraph(stringResource(R.string.privacy_section_5_text))

    LegalSection(stringResource(R.string.privacy_section_6_title))
    LegalParagraph(stringResource(R.string.privacy_section_6_text))

    LegalSection(stringResource(R.string.privacy_section_7_title))
    LegalParagraph(stringResource(R.string.privacy_section_7_text))
}

@Composable
private fun TermsOfUseContent() {
    LegalTitle(stringResource(R.string.terms_of_use))
    LegalSubtitle(stringResource(R.string.terms_last_updated))
    LegalParagraph(stringResource(R.string.terms_intro))
    
    LegalSection(stringResource(R.string.terms_section_1_title))
    LegalParagraph(stringResource(R.string.terms_section_1_text))

    LegalSection(stringResource(R.string.terms_section_2_title))
    LegalParagraph(stringResource(R.string.terms_section_2_text))

    LegalSection(stringResource(R.string.terms_section_3_title))
    LegalParagraph(stringResource(R.string.terms_section_3_text))

    LegalSection(stringResource(R.string.terms_section_4_title))
    LegalParagraph(stringResource(R.string.terms_section_4_text))

    LegalSection(stringResource(R.string.terms_section_5_title))
    LegalParagraph(stringResource(R.string.terms_section_5_text))
}

@Composable
private fun CommunityRulesContent() {
    LegalTitle(stringResource(R.string.community_rules_title))
    LegalParagraph(stringResource(R.string.community_rules_intro))
    Spacer(Modifier.height(8.dp))
    
    LegalParagraph(stringResource(R.string.rule_1))
    Spacer(Modifier.height(4.dp))
    LegalParagraph(stringResource(R.string.rule_2))
    Spacer(Modifier.height(4.dp))
    LegalParagraph(stringResource(R.string.rule_3))
    Spacer(Modifier.height(4.dp))
    LegalParagraph(stringResource(R.string.rule_4))
    Spacer(Modifier.height(4.dp))
    LegalParagraph(stringResource(R.string.rule_5))
    Spacer(Modifier.height(4.dp))
    LegalParagraph(stringResource(R.string.community_rules_inclusivity))
}



// ─── Shared components ────────────────────────────────────────────────────────

@Composable
private fun LegalTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun LegalSubtitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LegalSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun LegalParagraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
}
