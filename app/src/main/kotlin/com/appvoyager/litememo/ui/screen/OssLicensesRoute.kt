package com.appvoyager.litememo.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray

@Composable
fun OssLicensesRoute(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val licenses = remember(context) {
        val json = context.assets.open("oss_licenses.json")
            .bufferedReader()
            .use { it.readText() }
        parseLicenses(json)
    }

    OssLicensesScreen(
        licenses = licenses,
        onLicenseClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

private fun parseLicenses(json: String): List<OssLicense> {
    val array = JSONArray(json)
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        OssLicense(
            name = obj.getString("name"),
            license = obj.getString("license"),
            url = obj.getString("url")
        )
    }
}
