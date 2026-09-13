package de.nulide.findmydevice.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.nulide.findmydevice.R
import de.nulide.findmydevice.ui.theme.AppTheme

@Composable
fun Step3Screen(
    onCloseClicked: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            modifier = Modifier.size(48.dp),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.proto_v2_wizard_step2_text_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(48.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onCloseClicked() },
        ) {
            Text(stringResource(R.string.proto_v2_wizard_step3_button_close))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            Step3Screen({})
        }
    }
}
