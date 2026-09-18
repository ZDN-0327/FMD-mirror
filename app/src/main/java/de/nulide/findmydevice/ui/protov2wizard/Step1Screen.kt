package de.nulide.findmydevice.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.nulide.findmydevice.R
import de.nulide.findmydevice.ui.theme.AppTheme
import de.nulide.findmydevice.utils.Utils

@Composable
fun Step1Screen(
    username: String,
    serverUrl: String,
    onNextStepClicked: (String) -> Unit,
) {
    val context = LocalContext.current
    val passwordState = rememberTextFieldState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.proto_v2_start_text),
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { Utils.openUrl(context, "https://fmd-foss.org/blog") },
        ) {
            Text(stringResource(R.string.proto_v2_wizard_step1_button_more_info))
        }

        Spacer(Modifier.height(12.dp))
        Row {
            Icon(
                painterResource(R.drawable.ic_warning),
                modifier = Modifier.padding(end = 16.dp),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                stringResource(R.string.proto_v2_wizard_step1_text_risk),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { Utils.openUrl(context, serverUrl) },
        ) {
            Text(stringResource(R.string.proto_v2_wizard_step1_button_open_web, serverUrl))
        }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.proto_v2_wizard_step1_text_password),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.Settings_FMD_Server_User_ID) + " " + username,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (LocalInspectionMode.current) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                state = passwordState,
                label = {
                    Text(
                        stringResource(R.string.password),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
            )
        } else {
            // Does not work in previews
            OutlinedSecureTextField(
                modifier = Modifier.fillMaxWidth(),
                state = passwordState,
                label = {
                    Text(
                        stringResource(R.string.password),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.proto_v2_wizard_step1_text_keep_app_open),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNextStepClicked(passwordState.text.toString()) },
        ) {
            Text(stringResource(R.string.proto_v2_start_button))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            Step1Screen("alice", "https://server.fmd-foss.org", {})
        }
    }
}
