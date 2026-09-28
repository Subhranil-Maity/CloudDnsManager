package com.subhranil.clouddnsmanager.email

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.subhranil.clouddnsmanager.email.activity.ActivityContent
import com.subhranil.clouddnsmanager.email.activity.ActivityDataState
import com.subhranil.clouddnsmanager.email.activity.ActivityState
import com.subhranil.clouddnsmanager.email.addresses.AddressListContent
import com.subhranil.clouddnsmanager.email.addresses.AddressListDataState
import com.subhranil.clouddnsmanager.email.addresses.AddressListState
import com.subhranil.clouddnsmanager.email.aliases.AliasListContent
import com.subhranil.clouddnsmanager.email.aliases.AliasListDataState
import com.subhranil.clouddnsmanager.email.aliases.AliasListState
import com.subhranil.clouddnsmanager.email.aliases.create.CreateAliasContent
import com.subhranil.clouddnsmanager.email.aliases.create.CreateAliasState
import com.subhranil.clouddnsmanager.email.aliases.create.DestinationOptionsState
import com.subhranil.clouddnsmanager.email.aliases.detail.AliasDetailDataState
import com.subhranil.clouddnsmanager.email.aliases.detail.AliasDetailScreenContent
import com.subhranil.clouddnsmanager.email.aliases.detail.AliasDetailState
import com.subhranil.clouddnsmanager.email.domain.toAliasDisplay
import com.subhranil.clouddnsmanager.email.home.EmailHomeContent
import com.subhranil.clouddnsmanager.email.home.EmailHomeDataState
import com.subhranil.clouddnsmanager.email.home.EmailHomeState
import com.subhranil.clouddnsmanager.email.home.EmailTab
import com.subhranil.clouddnsmanager.email.model.EmailRoutingSettings
import com.subhranil.clouddnsmanager.preview.PreviewTheme
import com.subhranil.clouddnsmanager.preview.SampleData
import com.subhranil.clouddnsmanager.preview.ScreenPreview

private val routingOn = EmailHomeDataState.Loaded(EmailRoutingSettings(enabled = true, status = "ready"))

/** The Email hub with the given tab selected and sample content in every tab. */
@Composable
private fun EmailHub(tab: EmailTab) {
    PreviewTheme {
        EmailHomeContent(
            state = EmailHomeState(zoneName = SampleData.ZONE, dataState = routingOn, selectedTab = tab),
            onAction = {},
        ) { selected ->
            when (selected) {
                EmailTab.Aliases -> AliasListContent(
                    state = AliasListState(
                        zoneName = SampleData.ZONE,
                        dataState = AliasListDataState.Loaded(SampleData.aliasRows, SampleData.catchAll),
                        lockedRuleIds = setOf("a1", "a4"),
                        notedRuleIds = setOf("a2", "a4"),
                    ),
                    onAction = {},
                )
                EmailTab.Addresses -> AddressListContent(
                    state = AddressListState(
                        zoneName = SampleData.ZONE,
                        dataState = AddressListDataState.Loaded(SampleData.addresses),
                        aliasesByDestination = SampleData.aliasesByDestination,
                        lockedAddressIds = setOf("d1"),
                        notes = mapOf("d2" to "Shared finance inbox"),
                    ),
                    onAction = {},
                )
                EmailTab.Activity -> ActivityContent(
                    state = ActivityState(alias = null, dataState = ActivityDataState.Loaded(SampleData.activity)),
                    onAction = {},
                )
            }
        }
    }
}

@PreviewTest
@ScreenPreview
@Composable
fun EmailAliasesPreview() = EmailHub(EmailTab.Aliases)

@PreviewTest
@ScreenPreview
@Composable
fun EmailAddressesPreview() = EmailHub(EmailTab.Addresses)

@PreviewTest
@ScreenPreview
@Composable
fun EmailActivityPreview() = EmailHub(EmailTab.Activity)

@PreviewTest
@ScreenPreview
@Composable
fun AliasDetailPreview() {
    val rule = SampleData.rules.first { it.id == "a2" }
    val display = rule.toAliasDisplay()
    PreviewTheme {
        AliasDetailScreenContent(
            state = AliasDetailState(
                zoneName = SampleData.ZONE,
                dataState = AliasDetailDataState.Loaded(rule, display),
                locked = true,
                note = "Used only for the Example Shop account.",
                verifiedDestinations = SampleData.addresses.filter { it.isVerified }.map { it.email },
            ),
            onAction = {},
            activityState = ActivityState(
                alias = display.address,
                dataState = ActivityDataState.Loaded(SampleData.aliasActivity),
            ),
            onActivityAction = {},
        )
    }
}

@PreviewTest
@ScreenPreview
@Composable
fun CreateAliasPreview() {
    PreviewTheme {
        CreateAliasContent(
            state = CreateAliasState(
                zoneName = SampleData.ZONE,
                localPart = "quiet-river-4821",
                name = "Online shopping",
                destinations = DestinationOptionsState.Loaded(
                    verified = SampleData.addresses.filter { it.isVerified }.map { it.email },
                    pendingCount = 1,
                ),
                selectedDestination = "alex@example.net",
            ),
            onAction = {},
        )
    }
}
