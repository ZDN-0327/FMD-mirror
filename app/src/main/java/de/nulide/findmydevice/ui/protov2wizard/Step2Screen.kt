package de.nulide.findmydevice.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.nulide.findmydevice.R
import de.nulide.findmydevice.ui.theme.AppTheme

@Composable
fun Step2Screen(
    numLocations: Int,
    numPictures: Int,
    onDeleteDataClicked: () -> Unit,
    onMigrateDataClicked: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row {
            Icon(
                painterResource(R.drawable.ic_check_circle),
                modifier = Modifier.padding(end = 16.dp),
                contentDescription = "",
            )
            Text(
                stringResource(R.string.proto_v2_wizard_step2_text_intro),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row {
            Icon(
                painterResource(R.drawable.ic_database),
                modifier = Modifier.padding(end = 16.dp),
                contentDescription = "",
            )
            Text(
                stringResource(R.string.proto_v2_wizard_step2_text_data),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row {
            Icon(
                painterResource(R.drawable.ic_mobiledata_arrows),
                modifier = Modifier.padding(end = 16.dp),
                contentDescription = "",
            )
            Column {
                Text(
                    stringResource(R.string.proto_v2_wizard_step2_text_data_migration),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.proto_v2_wizard_step2_text_num_locations, numLocations),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.proto_v2_wizard_step2_text_num_pictures, numPictures),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onDeleteDataClicked() },
        ) {
            Text(stringResource(R.string.proto_v2_wizard_step2_button_delete_data))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onMigrateDataClicked() },
        ) {
            Text(stringResource(R.string.proto_v2_wizard_step2_button_migrate_data))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            Step2Screen(50, 2, { }, {})
        }
    }
}
